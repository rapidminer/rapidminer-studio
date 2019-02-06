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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
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
 * Generates a random example set for testing purposes. The data represents a team profit example
 * set.
 *
 * @author Ingo Mierswa
 */
public class TeamProfitExampleSetGenerator extends AbstractExampleSource {

	/** The parameter name for &quot;The number of generated examples.&quot; */
	public static final String PARAMETER_NUMBER_EXAMPLES = "number_examples";

	private static String[] ATTRIBUTE_NAMES = { "size", "leader", "number of qualified employees", "leader changed",
			"average years of experience", "structure" };

	private static int[] VALUE_TYPES = { Ontology.INTEGER, Ontology.NOMINAL, Ontology.INTEGER, Ontology.BINOMINAL,
			Ontology.INTEGER, Ontology.BINOMINAL };

	private static String[][] POSSIBLE_VALUES = { null,
			{ "Mr. Brown", "Mr. Miller", "Mrs. Smith", "Mrs. Hanson", "Mrs. Green", "Mr. Chang" }, null, { "yes", "no" },
			null, { "flat", "hierachical" } };

	/** @since 9.2.0 */
	private static final ExampleSetMetaData DEFAULT_META_DATA;
	static {
		ExampleSetMetaData emd = new ExampleSetMetaData();
		emd.addAttribute(new AttributeMetaData("label", Attributes.LABEL_NAME, "good", "bad"));
		emd.addAttribute(new AttributeMetaData("teamID", Ontology.NOMINAL));
		// "size", "leader", "number of qualified employees", "leader changed",
		// "average years of experience", "structure"
		emd.addAttribute(new AttributeMetaData("size", null, Ontology.INTEGER, new Range(5, 20)));
		emd.addAttribute(new AttributeMetaData("leader", null, POSSIBLE_VALUES[1]));
		emd.addAttribute(new AttributeMetaData("number of qualified employees", null, Ontology.INTEGER, new Range(1, 10)));
		emd.addAttribute(new AttributeMetaData("leader changed", null, POSSIBLE_VALUES[3]));
		emd.addAttribute(new AttributeMetaData("average years of experience", null, Ontology.INTEGER, new Range(1, 10)));
		emd.addAttribute(new AttributeMetaData("structure", null, POSSIBLE_VALUES[5]));
		DEFAULT_META_DATA = emd;
	}

	public TeamProfitExampleSetGenerator(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		// init
		int numberOfExamples = getParameterAsInt(PARAMETER_NUMBER_EXAMPLES);

		// create table
		List<Attribute> attributes = new ArrayList<Attribute>();
		Attribute id = AttributeFactory.createAttribute("teamID", Ontology.NOMINAL);
		attributes.add(id);
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
		Attribute label = AttributeFactory.createAttribute("label", Ontology.BINOMINAL);
		label.getMapping().mapString("good");
		label.getMapping().mapString("bad");
		attributes.add(label);

		ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(numberOfExamples);

		// create data
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);

		// init operator progress
		getProgress().setTotal(numberOfExamples);

		for (int n = 0; n < numberOfExamples; n++) {
			double[] values = new double[ATTRIBUTE_NAMES.length + 2];
			values[0] = attributes.get(0).getMapping().mapString("team_" + n);
			// "size", "leader", "number of qualified employees", "leader changed",
			// "average years of experience", "structure"
			values[1] = random.nextIntInRange(5, 20);
			values[2] = random.nextInt(POSSIBLE_VALUES[1].length);
			values[3] = Math.round(random.nextDouble() * (values[1] - 1) + 1);
			values[4] = random.nextInt(POSSIBLE_VALUES[3].length);
			values[5] = random.nextIntInRange(1, 10);
			values[6] = random.nextInt(POSSIBLE_VALUES[5].length);

			values[7] = label.getMapping().mapString("bad");
			if (values[1] > 18) {
				if (random.nextDouble() > 0.05) {
					values[7] = label.getMapping().mapString("good");
				}
			} else if (values[1] > 15) {
				if (random.nextDouble() > 0.1) {
					values[7] = label.getMapping().mapString("good");
				}
			} else if (values[4] == 1) {
				if (random.nextDouble() > 0.1) {
					values[7] = label.getMapping().mapString("good");
				}
			}
			builder.addRow(values);

			getProgress().step();
		}

		getProgress().complete();

		// create example set and return it
		return builder.withRole(label, Attributes.LABEL_NAME).build();
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

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		ExampleSetMetaData emd = getDefaultMetaData();
		emd.setNumberOfExamples(getParameterAsInt(PARAMETER_NUMBER_EXAMPLES));
		return emd;
	}

	/** @since 9.2.0 */
	@Override
	protected ExampleSetMetaData getDefaultMetaData() {
		return DEFAULT_META_DATA.clone();
	}
}
