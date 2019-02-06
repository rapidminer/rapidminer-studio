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


/**
 * Generates a random example set for testing purposes. The data represents a direct mailing example
 * set.
 *
 * @author Ingo Mierswa
 */
public class ChurnReductionExampleSetGenerator extends AbstractExampleSource {

	/** The parameter name for &quot;The number of generated examples.&quot; */
	public static final String PARAMETER_NUMBER_EXAMPLES = "number_examples";

	public static final int NEW_CREDIT_IDX = 0;
	public static final int NOTHING_IDX = 1;
	public static final int END_CREDIT_IDX = 2;
	public static final int COLLECT_INFO_IDX = 3;
	public static final int ADD_CREDIT_IDX = 4;
	private static String[] POSSIBLE_VALUES = { "New Credit", "Nothing", "End Credit", "Collect Information",
			"Additional Credit" };

	/** the index of the label attribute */
	private static final int LABEL_ATTR_IDX = 5;

	/** @since 9.2.0 */
	private static final ExampleSetMetaData DEFAULT_META_DATA;
	static {
		ExampleSetMetaData emd = new ExampleSetMetaData();
		emd.addAttribute(new AttributeMetaData("label", Attributes.LABEL_NAME, "ok", "terminate"));
		for (int i = 1; i < 6; i++) {
			emd.addAttribute(new AttributeMetaData("Year " + i, null, POSSIBLE_VALUES));
		}
		DEFAULT_META_DATA = emd;
	}

	public ChurnReductionExampleSetGenerator(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		// init
		int numberOfExamples = getParameterAsInt(PARAMETER_NUMBER_EXAMPLES);

		// create table
		List<Attribute> attributes = new ArrayList<Attribute>();
		for (int m = 0; m < LABEL_ATTR_IDX; m++) {
			Attribute current = AttributeFactory.createAttribute("Year " + (m + 1), Ontology.NOMINAL);
			for (int v = 0; v < POSSIBLE_VALUES.length; v++) {
				current.getMapping().mapString(POSSIBLE_VALUES[v]);
			}
			attributes.add(current);
		}
		Attribute label = AttributeFactory.createAttribute("label", Ontology.NOMINAL);
		int okValue = label.getMapping().mapString("ok");
		int terminateValue = label.getMapping().mapString("terminate");
		attributes.add(label);

		ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(numberOfExamples);

		// create data
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);

		// init operator progress
		getProgress().setTotal(numberOfExamples);

		for (int n = 0; n < numberOfExamples; n++) {
			double[] values = new double[6];
			for (int i = 0; i < LABEL_ATTR_IDX; i++) {
				values[i] = random.nextInt(POSSIBLE_VALUES.length);
			}
			values[LABEL_ATTR_IDX] = okValue;

			// "New Credit", "Nothing", "End Credit", "Collect Information", "Additional Credit"
			if (values[0] == NEW_CREDIT_IDX && values[1] == NOTHING_IDX) {
				values[LABEL_ATTR_IDX] = terminateValue;
			} else if (values[2] == ADD_CREDIT_IDX && values[4] == NOTHING_IDX) {
				values[LABEL_ATTR_IDX] = terminateValue;
			} else if (values[4] == 5) { // this cannot happen (5 is no valid value idx). Remove?
				values[LABEL_ATTR_IDX] = terminateValue;
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
