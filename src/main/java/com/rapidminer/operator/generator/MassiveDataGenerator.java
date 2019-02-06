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
package com.rapidminer.operator.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleSparseArrayDataRow;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSetBuilder.DataManagement;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDReal;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.container.Range;


/**
 * Generates huge amounts of data in either sparse or dense format. This operator can be used to
 * check if huge amounts of data can be handled by RapidMiner for a given process setup without
 * creating the correct format / writing special purpose input operators.
 *
 * @author Ingo Mierswa
 */
public class MassiveDataGenerator extends AbstractExampleSource {

	/** The parameter name for &quot;The number of generated examples.&quot; */
	public static final String PARAMETER_NUMBER_EXAMPLES = "number_examples";

	/** The parameter name for &quot;The number of attributes.&quot; */
	public static final String PARAMETER_NUMBER_ATTRIBUTES = "number_attributes";

	/** The parameter name for &quot;The fraction of default attributes.&quot; */
	public static final String PARAMETER_SPARSE_FRACTION = "sparse_fraction";

	/**
	 * The parameter name for &quot;Indicates if the example should be internally represented in a
	 * sparse format.&quot;
	 */
	public static final String PARAMETER_SPARSE_REPRESENTATION = "sparse_representation";

	private static final int MAX_METADATA_ATTRIBUTES = 20;

	private static final int OPERATOR_PROGRESS_STEPS = 1_000_000;

	public MassiveDataGenerator(OperatorDescription description) {
		super(description);
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		ExampleSetMetaData emd = new ExampleSetMetaData();
		emd.addAttribute(new AttributeMetaData("label", Ontology.NOMINAL, Attributes.LABEL_NAME));
		emd.setNumberOfExamples(getParameterAsInt(PARAMETER_NUMBER_EXAMPLES));
		int desiredNumberOfAttributes = getParameterAsInt(PARAMETER_NUMBER_ATTRIBUTES);
		double mean = getParameterAsDouble(PARAMETER_SPARSE_FRACTION);
		IntStream attributeIndices;
		if (desiredNumberOfAttributes <= MAX_METADATA_ATTRIBUTES) {
			// all attributes
			attributeIndices = IntStream.range(1, desiredNumberOfAttributes);
		} else {
			// first ten and last ten
			attributeIndices = IntStream.concat(IntStream.rangeClosed(1, MAX_METADATA_ATTRIBUTES/2),
					IntStream.rangeClosed(desiredNumberOfAttributes - MAX_METADATA_ATTRIBUTES/2 + 1, desiredNumberOfAttributes));
		}
		attributeIndices.mapToObj(i -> new AttributeMetaData("att" + i, null, Ontology.REAL, new Range(0, 1)))
				.peek(amd -> amd.setMean(new MDReal(mean))).forEach(emd::addAttribute);
		return emd;
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {

		// init
		int numberOfExamples = getParameterAsInt(PARAMETER_NUMBER_EXAMPLES);
		int numberOfAttributes = getParameterAsInt(PARAMETER_NUMBER_ATTRIBUTES);
		double sparseFraction = getParameterAsDouble(PARAMETER_SPARSE_FRACTION);
		boolean sparseRepresentation = getParameterAsBoolean(PARAMETER_SPARSE_REPRESENTATION);
		getProgress().setTotal(100);
		int progressCounter = 0;

		// create table
		List<Attribute> attributes = new ArrayList<>();
		for (int m = 0; m < numberOfAttributes; m++) {
			attributes.add(AttributeFactory.createAttribute("att" + (m + 1), Ontology.REAL));
			if (++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				getProgress()
						.setCompleted((int) (100.0 * progressCounter / ((1.0 + numberOfExamples) * numberOfAttributes)));
			}
		}
		Attribute label = AttributeFactory.createAttribute("label", Ontology.NOMINAL);
		label.getMapping().mapString("positive");
		label.getMapping().mapString("negative");
		attributes.add(label);
		ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(numberOfExamples);

		if (sparseRepresentation && !Boolean
				.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT))) {
			builder.withOptimizationHint(DataManagement.MEMORY_OPTIMIZED);
			sparseRepresentation = false;
		}

		// create data
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		progressCounter = 0;
		for (int n = 0; n < numberOfExamples; n++) {
			int counter = 0;
			if (sparseRepresentation) {
				DoubleSparseArrayDataRow dataRow = new DoubleSparseArrayDataRow(numberOfAttributes + 1);
				for (int i = 0; i < numberOfAttributes; i++) {
					double value = random.nextDouble() > sparseFraction ? 1.0d : 0.0d;
					dataRow.set(attributes.get(i), value);
					if (value == 0.0d) {
						counter++;
					}
					if (++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
						getProgress().setCompleted((int) (100.0 * (numberOfAttributes * (n + 1.0) + i + 1.0)
								/ ((1.0 + numberOfExamples) * numberOfAttributes)));
					}
				}
				if (counter < sparseFraction * numberOfAttributes) {
					dataRow.set(label, label.getMapping().mapString("positive"));
				} else {
					dataRow.set(label, label.getMapping().mapString("negative"));
				}
				dataRow.trim();
				builder.addDataRow(dataRow);
			} else {
				double[] dataRow = new double[numberOfAttributes + 1];
				for (int i = 0; i < numberOfAttributes; i++) {
					double value = random.nextDouble() > sparseFraction ? 1.0d : 0.0d;
					dataRow[i] = value;
					if (value == 0.0d) {
						counter++;
					}
					if (++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
						getProgress().setCompleted((int) (100.0 * (numberOfAttributes * (n + 1.0) + i + 1.0)
								/ ((1.0 + numberOfExamples) * numberOfAttributes)));
					}
				}
				if (counter < sparseFraction * numberOfAttributes) {
					dataRow[dataRow.length - 1] = label.getMapping().mapString("positive");
				} else {
					dataRow[dataRow.length - 1] = label.getMapping().mapString("negative");
				}
				builder.addRow(dataRow);
			}
		}

		// create example set and return it
		ExampleSet result = builder.withRole(label, Attributes.LABEL_NAME).build();

		getProgress().complete();

		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_EXAMPLES, "The number of generated examples.", 0,
				Integer.MAX_VALUE, 10000);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_NUMBER_ATTRIBUTES, "The number of attributes.", 0, Integer.MAX_VALUE, 10000);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeDouble(PARAMETER_SPARSE_FRACTION, "The fraction of default attributes.", 0.0d, 1.0d,
				0.99d));
		types.add(new ParameterTypeBoolean(PARAMETER_SPARSE_REPRESENTATION,
				"Indicates if the example should be internally represented in a sparse format.", true));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
