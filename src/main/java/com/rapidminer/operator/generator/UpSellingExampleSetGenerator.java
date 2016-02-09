/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.container.Range;


/**
 * Generates a random example set for testing purposes. The data represents an up-selling example
 * set.
 *
 * @author Ingo Mierswa
 */
public class UpSellingExampleSetGenerator extends AbstractExampleSource {

	/** The parameter name for &quot;The number of generated examples.&quot; */
	public static final String PARAMETER_NUMBER_EXAMPLES = "number_examples";

	private static String[] ATTRIBUTE_NAMES = { "name", "age", "lifestyle", "zip code", "family status", "car", "sports",
	"earnings" };

	private static int[] VALUE_TYPES = { Ontology.NOMINAL, Ontology.INTEGER, Ontology.NOMINAL, Ontology.INTEGER,
		Ontology.NOMINAL, Ontology.NOMINAL, Ontology.NOMINAL, Ontology.INTEGER };

	private static String[][] POSSIBLE_VALUES = { null, null, { "healthy", "active", "cozily" }, null,
		{ "married", "single" }, { "practical", "expensive" }, { "soccer", "badminton", "athletics" }, null };

	public UpSellingExampleSetGenerator(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		// init
		int numberOfExamples = getParameterAsInt(PARAMETER_NUMBER_EXAMPLES);

		// create table
		List<Attribute> attributes = new ArrayList<Attribute>();
		for (int m = 0; m < ATTRIBUTE_NAMES.length; m++) {
			Attribute current = AttributeFactory.createAttribute(ATTRIBUTE_NAMES[m], VALUE_TYPES[m]);
			String[] possibleValues = POSSIBLE_VALUES[m];
			if (possibleValues != null) {
				for (int v = 0; v < possibleValues.length; v++) {
					current.getMapping().mapString(possibleValues[v]);
				}
			}
			attributes.add(current);
		}
		Attribute label = AttributeFactory.createAttribute("label", Ontology.NOMINAL);
		label.getMapping().mapString("product_1");
		label.getMapping().mapString("product_2");
		label.getMapping().mapString("product_3");
		attributes.add(label);

		MemoryExampleTable table = new MemoryExampleTable(attributes);

		// create data
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);

		// init operator progress
		getProgress().setTotal(numberOfExamples);

		for (int n = 0; n < numberOfExamples; n++) {
			double[] values = new double[ATTRIBUTE_NAMES.length + 1];
			values[0] = attributes.get(0).getMapping().mapString(random.nextString(8));
			// "name", "age", "lifestyle", "zip code", "family status", "car", "sports", "earnings"
			values[1] = random.nextIntInRange(15, 70);
			values[2] = random.nextInt(POSSIBLE_VALUES[2].length);
			values[3] = random.nextIntInRange(10000, 100000);
			values[4] = random.nextInt(POSSIBLE_VALUES[4].length);
			values[5] = random.nextInt(POSSIBLE_VALUES[5].length);
			values[6] = random.nextInt(POSSIBLE_VALUES[6].length);
			values[7] = random.nextIntInRange(20000, 150000);

			values[8] = label.getMapping().mapString("product_1");
			if (values[1] > 65) {
				if (random.nextDouble() > 0.05) {
					values[8] = label.getMapping().mapString("product_2");
				}
			} else if (values[1] > 60) {
				if (random.nextDouble() > 0.1) {
					values[8] = label.getMapping().mapString("product_2");
				}
			} else if (values[1] > 55) {
				if (random.nextDouble() > 0.2) {
					values[8] = label.getMapping().mapString("product_2");
				}
			} else if (values[3] < 15000) {
				if (random.nextDouble() > 0.1) {
					values[8] = label.getMapping().mapString("product_3");
				}
			} else if (values[7] > 140000) {
				values[8] = label.getMapping().mapString("product_3");
			}
			table.addDataRow(new DoubleArrayDataRow(values));

			getProgress().step();
		}

		getProgress().complete();

		// create example set and return it
		return table.createExampleSet(label);
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		ExampleSetMetaData emd = new ExampleSetMetaData();
		emd.addAttribute(new AttributeMetaData("label", Attributes.LABEL_NAME, "product_1", "product_2", "product_3"));
		emd.addAttribute(new AttributeMetaData("name", Ontology.NOMINAL));
		emd.addAttribute(new AttributeMetaData("age", null, Ontology.INTEGER, new Range(15, 70)));
		emd.addAttribute(new AttributeMetaData("lifestyle", null, POSSIBLE_VALUES[2]));
		emd.addAttribute(new AttributeMetaData("zip code", null, Ontology.INTEGER, new Range(10000, 100000)));
		emd.addAttribute(new AttributeMetaData("familiy status", null, POSSIBLE_VALUES[4]));
		emd.addAttribute(new AttributeMetaData("car", null, POSSIBLE_VALUES[5]));
		emd.addAttribute(new AttributeMetaData("sports", null, POSSIBLE_VALUES[6]));
		emd.addAttribute(new AttributeMetaData("earnings", null, Ontology.INTEGER, new Range(20000, 150000)));

		emd.setNumberOfExamples(getParameterAsInt(PARAMETER_NUMBER_EXAMPLES));
		return emd;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_EXAMPLES, "The number of generated examples.", 1,
				Integer.MAX_VALUE, 100);
		type.setExpert(false);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
