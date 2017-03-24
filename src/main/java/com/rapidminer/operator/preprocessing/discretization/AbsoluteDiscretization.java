/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.operator.preprocessing.discretization;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SortedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator discretizes all numeric attributes in the dataset into nominal attributes. This
 * discretization is performed by binning examples into bins of same size. The specified number of
 * equally sized bins is created and the numerical values are simply sorted into those bins, so that
 * all bins contain the same number of examples. Skips all special attributes including the label.
 *
 * @author Sebastian Land
 */
public class AbsoluteDiscretization extends AbstractDiscretizationOperator {

	static {
		registerDiscretizationOperator(AbsoluteDiscretization.class);
	}

	/** Indicates the number of used bins. */
	public static final String PARAMETER_SIZE_OF_BINS = "size_of_bins";

	/** Indicates if long range names should be used. */
	public static final String PARAMETER_RANGE_NAME_TYPE = "range_name_type";

	public static final String PARAMETER_SORTING_DIRECTION = "sorting_direction";

	public static final String PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS = "automatic_number_of_digits";

	public static final String PARAMETER_NUMBER_OF_DIGITS = "number_of_digits";

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public AbsoluteDiscretization(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd)
			throws UndefinedParameterError {
		AttributeMetaData newAMD = new AttributeMetaData(amd.getName(), Ontology.NOMINAL, amd.getRole());
		Set<String> valueSet = new TreeSet<String>();
		newAMD.setValueSet(valueSet, SetRelation.SUPERSET);
		if (getParameterAsInt(PARAMETER_RANGE_NAME_TYPE) == DiscretizationModel.RANGE_NAME_SHORT) {
			for (int i = 0; i < (int) Math.ceil(((double) emd.getNumberOfExamples().getNumber())
					/ getParameterAsInt(PARAMETER_SIZE_OF_BINS)); i++) {
				valueSet.add("range" + (i + 1));
			}
			switch (emd.getNumberOfExamples().getRelation()) {
				case AT_LEAST:
					newAMD.setValueSet(valueSet, SetRelation.SUPERSET);
					break;
				case AT_MOST:
					newAMD.setValueSet(valueSet, SetRelation.SUBSET);
					break;
				case EQUAL:
					newAMD.setValueSet(valueSet, SetRelation.EQUAL);
					break;
				case UNKNOWN:
					newAMD.setValueSet(valueSet, SetRelation.UNKNOWN);
					break;
			}
		}
		return Collections.singletonList(newAMD);
	}

	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {
		DiscretizationModel model = new DiscretizationModel(exampleSet);

		exampleSet.recalculateAllAttributeStatistics();
		// calculating number of bins
		int sizeOfBins = getParameterAsInt(PARAMETER_SIZE_OF_BINS);
		int numberOfBins = exampleSet.size() / sizeOfBins;
		int numberOfExamples = exampleSet.size();
		// add one bin if a remainder exists
		if (numberOfBins * sizeOfBins < numberOfExamples) {
			numberOfBins++;
		}
		HashMap<Attribute, double[]> ranges = new HashMap<Attribute, double[]>();

		int sortingDirection = getParameterAsInt(PARAMETER_SORTING_DIRECTION);

		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) { // skip nominal and date attributes
				ExampleSet sortedSet = new SortedExampleSet(exampleSet, attribute, sortingDirection);
				double[] binRange = new double[numberOfBins];
				for (int i = 0; i < numberOfBins - 1; i++) {
					int offset = (i + 1) * sizeOfBins - 1;
					double infimum = sortedSet.getExample(offset).getValue(attribute);
					offset++;
					double supremum = sortedSet.getExample(offset).getValue(attribute);
					// if targets equal values: Search for next different value
					while (infimum == supremum && offset < numberOfExamples) {
						supremum = sortedSet.getExample(offset).getValue(attribute);
						offset++;
					}
					if (sortingDirection == SortedExampleSet.DECREASING) {
						binRange[numberOfBins - 2 - i] = (infimum + supremum) / 2d;
					} else {
						binRange[i] = (infimum + supremum) / 2d;
					}
				}
				binRange[numberOfBins - 1] = Double.POSITIVE_INFINITY;
				ranges.put(attribute, binRange);
			}
		}

		// determine number of digits
		int numberOfDigits = -1;
		if (getParameterAsBoolean(PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS) == false) {
			numberOfDigits = getParameterAsInt(PARAMETER_NUMBER_OF_DIGITS);
		}

		model.setRanges(ranges, "range", getParameterAsInt(PARAMETER_RANGE_NAME_TYPE), numberOfDigits);
		return (model);
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return DiscretizationModel.class;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeInt(PARAMETER_SIZE_OF_BINS,
				"Defines the number of examples which should be used for each bin.", 1, Integer.MAX_VALUE, false);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeCategory(PARAMETER_SORTING_DIRECTION,
				"Indicates if the values should be sorted in increasing or decreasing order.",
				SortedExampleSet.SORTING_DIRECTIONS, SortedExampleSet.DECREASING));

		types.add(new ParameterTypeCategory(PARAMETER_RANGE_NAME_TYPE,
				"Indicates if long range names including the limits should be used.", DiscretizationModel.RANGE_NAME_TYPES,
				DiscretizationModel.RANGE_NAME_LONG));

		type = new ParameterTypeBoolean(PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS,
				"Indicates if the number of digits should be automatically determined for the range names.", true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_RANGE_NAME_TYPE,
				DiscretizationModel.RANGE_NAME_TYPES, false, DiscretizationModel.RANGE_NAME_INTERVAL));
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_NUMBER_OF_DIGITS,
				"The minimum number of digits used for the interval names (-1: determine minimal number automatically).",
				-1, Integer.MAX_VALUE, -1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS, false,
				false));
		types.add(type);

		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				AbsoluteDiscretization.class, attributeSelector);
	}

	@Override
	public boolean writesIntoExistingData() {
		if (getCompatibilityLevel().isAbove(VERSION_MAY_WRITE_INTO_DATA)) {
			return super.writesIntoExistingData();
		} else {
			// old version: true only if original output port is connected
			return isOriginalOutputConnected() && super.writesIntoExistingData();
		}
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}
}
