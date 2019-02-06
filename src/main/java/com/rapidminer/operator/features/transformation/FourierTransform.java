/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package com.rapidminer.operator.features.transformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowReader;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.OperatorInsertionQuickFix;
import com.rapidminer.operator.preprocessing.filter.MissingValueReplenishment;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.math.Complex;
import com.rapidminer.tools.math.FastFourierTransform;
import com.rapidminer.tools.math.Peak;
import com.rapidminer.tools.math.SpectrumFilter;
import com.rapidminer.tools.math.WindowFunction;
import com.rapidminer.tools.math.container.Range;


/**
 * Creates a new example set consisting of the result of a fourier transformation for each attribute
 * of the input example set.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.0; see time series extension for new version
 */
@Deprecated
public class FourierTransform extends AbstractFeatureTransformation {

	public FourierTransform(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		for (AttributeMetaData amd : metaData.getAllAttributes()) {
			if (!amd.isSpecial()) {
				if (amd.containsMissingValues() == MetaDataInfo.YES) {
					getExampleSetInputPort().addError(
							new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(), Collections
									.singletonList(new OperatorInsertionQuickFix("insert_missing_value_replenishment",
									new String[0], 1, getExampleSetInputPort()) {

								@Override
								public Operator createOperator() throws OperatorCreationException {
									return OperatorService.createOperator(MissingValueReplenishment.class);
								}
							}), "exampleset.contains_missings", getName()));
					break;
				}
			}
		}

		// create transformed meta data
		List<AttributeMetaData> newAttributes = new LinkedList<AttributeMetaData>();
		for (AttributeMetaData amd : metaData.getAllAttributes()) {
			if (!amd.isSpecial() && amd.isNumerical()) {
				AttributeMetaData newAttribute = new AttributeMetaData("fft(" + amd.getName() + ")", Ontology.REAL);
				newAttribute.setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
						SetRelation.UNKNOWN);
				newAttributes.add(newAttribute);
			}
		}

		metaData.clearRegular();
		metaData.clear();
		AttributeMetaData frequency = new AttributeMetaData("frequency", Ontology.REAL);
		metaData.addAttribute(frequency);
		metaData.addAllAttributes(newAttributes);
		if (metaData.getNumberOfExamples().isKnown()) {
			metaData.setNumberOfExamples(FastFourierTransform.getGreatestPowerOf2LessThan(metaData.getNumberOfExamples()
					.getValue()) / 2);
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this);

		// collect data for new example table
		int numberOfNewExamples = FastFourierTransform.getGreatestPowerOf2LessThan(exampleSet.size()) / 2;
		List<Attribute> allAttributes = new ArrayList<>();

		// create frequency attribute (for frequency)
		Attribute frequencyAttribute = AttributeFactory.createAttribute("frequency", Ontology.REAL);
		allAttributes.add(frequencyAttribute);

		Attributes attributes = exampleSet.getAttributes();
		Attribute[] regularAttributes = attributes.createRegularAttributeArray();
		for (Attribute current : regularAttributes) {
			if (current.isNumerical()) {
				// create new attribute
				Attribute newAttribute = AttributeFactory.createAttribute("fft(" + current.getName() + ")", Ontology.REAL);
				allAttributes.add(newAttribute);
			}
		}

		ExampleSet resultSet = ExampleSets.from(allAttributes).withBlankSize(numberOfNewExamples)
				.withColumnFiller(frequencyAttribute,
						k -> FastFourierTransform.convertFrequency(k++, numberOfNewExamples, exampleSet.size()))
				.build();

		// create FFT values
		Attribute label = attributes.getLabel();

		// add FFT values
		FastFourierTransform fft = new FastFourierTransform(WindowFunction.BLACKMAN_HARRIS);
		SpectrumFilter filter = new SpectrumFilter(SpectrumFilter.NONE);
		int index = 1;
		for (Attribute current : regularAttributes) {
			if (current.isNumerical()) {
				Complex[] result = fft.getFourierTransform(exampleSet, label, current);
				Peak[] spectrum = filter.filter(result, exampleSet.size());
				// fill table with values
				fillTable(resultSet.getExampleTable(), allAttributes.get(index++), spectrum);
			}
		}

		return resultSet;
	}

	/**
	 * Fills the table with the length of the given complex numbers in the column of the attribute.
	 */
	private void fillTable(ExampleTable table, Attribute attribute, Peak[] values) throws OperatorException {
		DataRowReader reader = table.getDataRowReader();
		int k = 0;
		while (reader.hasNext()) {
			DataRow dataRow = reader.next();
			dataRow.set(attribute, values[k++].getMagnitude());
			checkForStop();
		}
	}

	/**
	 * This constructs a new table and hence doesn't modify existing example set
	 */
	@Override
	public boolean writesIntoExistingData() {
		return false;
	}
}
