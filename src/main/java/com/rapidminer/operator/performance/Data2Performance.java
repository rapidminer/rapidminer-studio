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
package com.rapidminer.operator.performance;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ParameterConditionedPrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.EqualTypeCondition;


/**
 * This operator can be used to derive a specific value of a given example set and provide it as a
 * performance value which can be used for optimization purposes.
 *
 * @author Ingo Mierswa
 */
public class Data2Performance extends AbstractExampleSetEvaluator {

	public static final String PARAMETER_PERFORMANCE_TYPE = "performance_type";

	public static final String PARAMETER_ATTRIBUTE_VALUE = "attribute_value";

	public static final String PARAMETER_STATISTICS = "statistics";

	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	public static final String PARAMETER_EXAMPLE_INDEX = "example_index";

	public static final String PARAMETER_OPTIMIZATION_DIRECTION = "optimization_direction";

	public static final String[] OPTIMIZATION_DIRECTIONS = new String[] { "minimize", "maximize" };

	public static final int OPTIMIZATION_DIRECTION_MINIMIZE = 0;

	public static final int OPTIMIZATION_DIRECTION_MAXIMIZE = 1;

	public static final String[] MACRO_TYPES = new String[] { "number_of_examples", "number_of_attributes", "data_value",
			"statistics" };

	public static final int MACRO_TYPE_EXAMPLES = 0;
	public static final int MACRO_TYPE_ATTRIBUTES = 1;
	public static final int MACRO_TYPE_DATA = 2;
	public static final int MACRO_TYPE_STATISTICS = 3;

	public static final String[] STATISTICS_TYPES = new String[] { "average", "min", "max", "count" };

	public static final int STATISTICS_TYPE_AVERAGE = 0;
	public static final int STATISTICS_TYPE_MIN = 1;
	public static final int STATISTICS_TYPE_MAX = 2;
	public static final int STATISTICS_TYPE_COUNT = 3;

	private double performanceValue = Double.NaN;

	public Data2Performance(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(
				new ParameterConditionedPrecondition(getExampleSetInputPort(), new AttributeSetPrecondition(
						getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(this,
								PARAMETER_ATTRIBUTE_NAME)), this, PARAMETER_PERFORMANCE_TYPE, MACRO_TYPES[MACRO_TYPE_DATA]));
		getExampleSetInputPort().addPrecondition(
				new ParameterConditionedPrecondition(getExampleSetInputPort(), new AttributeSetPrecondition(
						getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(this,
								PARAMETER_ATTRIBUTE_NAME)), this, PARAMETER_PERFORMANCE_TYPE,
						MACRO_TYPES[MACRO_TYPE_STATISTICS]));

		addValue(new ValueDouble("performance", "The last calculated performance.") {

			@Override
			public double getDoubleValue() {
				return performanceValue;
			}
		});
	}

	@Override
	public PerformanceVector evaluate(ExampleSet exampleSet) throws OperatorException {
		performanceValue = Double.NaN;
		switch (getParameterAsInt(PARAMETER_PERFORMANCE_TYPE)) {
			case MACRO_TYPE_ATTRIBUTES:
				performanceValue = exampleSet.getAttributes().size();
				break;
			case MACRO_TYPE_EXAMPLES:
				performanceValue = exampleSet.size();
				break;
			case MACRO_TYPE_DATA:
				int exampleIndex = getParameterAsInt(PARAMETER_EXAMPLE_INDEX);
				if (exampleIndex == 0) {
					throw new UserError(this, 207, new Object[] { "0", PARAMETER_EXAMPLE_INDEX,
							"only positive or negative indices are allowed" });
				}

				if (exampleIndex < 0) {
					exampleIndex = exampleSet.size() + exampleIndex;
				} else {
					exampleIndex--;
				}

				if (exampleIndex >= exampleSet.size()) {
					throw new UserError(this, 110, exampleIndex);
				}

				Attribute attribute = exampleSet.getAttributes().get(getParameter(PARAMETER_ATTRIBUTE_NAME));
				if (attribute == null) {
					throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME,
							getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
				}

				Example example = exampleSet.getExample(exampleIndex);
				performanceValue = example.getValue(attribute);
				break;
			case MACRO_TYPE_STATISTICS:
				attribute = exampleSet.getAttributes().get(getParameter(PARAMETER_ATTRIBUTE_NAME));
				if (attribute == null) {
					throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME,
							getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
				}

				exampleSet.recalculateAttributeStatistics(attribute);

				int statisticsType = getParameterAsInt(PARAMETER_STATISTICS);
				switch (statisticsType) {
					case STATISTICS_TYPE_AVERAGE:
						if (attribute.isNominal()) {
							performanceValue = exampleSet.getStatistics(attribute, Statistics.MODE);
						} else {
							performanceValue = exampleSet.getStatistics(attribute, Statistics.AVERAGE);
						}
						break;
					case STATISTICS_TYPE_MAX:
						performanceValue = exampleSet.getStatistics(attribute, Statistics.MAXIMUM);
						break;
					case STATISTICS_TYPE_MIN:
						performanceValue = exampleSet.getStatistics(attribute, Statistics.MINIMUM);
						break;
					case STATISTICS_TYPE_COUNT:
						if (attribute.isNominal()) {
							String attributeValue = getParameterAsString(PARAMETER_ATTRIBUTE_VALUE);
							int index = attribute.getMapping().getIndex(attributeValue);
							if (index < 0) {
								throw new UserError(this, 143, attributeValue, attribute.getName());
							}
							performanceValue = exampleSet.getStatistics(attribute, Statistics.COUNT, attributeValue);
						} else {
							throw new UserError(this, 119, attribute.getName(), getName());
						}
						break;
				}
				break;
		}

		// define macro
		PerformanceVector result = new PerformanceVector();
		EstimatedPerformance performance = new EstimatedPerformance("Data Based Performance", performanceValue, 1,
				getParameterAsInt(PARAMETER_OPTIMIZATION_DIRECTION) == OPTIMIZATION_DIRECTION_MINIMIZE);
		result.addCriterion(performance);

		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeCategory(PARAMETER_PERFORMANCE_TYPE,
				"Indicates the way how the macro should be defined.", MACRO_TYPES, MACRO_TYPE_EXAMPLES);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_STATISTICS,
				"The statistics of the specified attribute which should be used as macro value.", STATISTICS_TYPES,
				STATISTICS_TYPE_AVERAGE);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_PERFORMANCE_TYPE, MACRO_TYPES, true,
				MACRO_TYPE_STATISTICS));
		types.add(type);

		type = new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME,
				"The name of the attribute from which the data should be derived.", getExampleSetInputPort(), true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_PERFORMANCE_TYPE, MACRO_TYPES, true,
				MACRO_TYPE_DATA, MACRO_TYPE_STATISTICS));
		types.add(type);

		type = new ParameterTypeString(PARAMETER_ATTRIBUTE_VALUE, "The value of the attribute which should be counted.",
				true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_PERFORMANCE_TYPE, MACRO_TYPES, true,
				MACRO_TYPE_STATISTICS));
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_STATISTICS, STATISTICS_TYPES, true,
				STATISTICS_TYPE_COUNT));
		types.add(type);

		type = new ParameterTypeInt(
				PARAMETER_EXAMPLE_INDEX,
				"The index of the example from which the data should be derived. Negative indices are counted from the end of the data set. Positive counting starts with 1, negative counting with -1.",
				-Integer.MAX_VALUE, Integer.MAX_VALUE, true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_PERFORMANCE_TYPE, MACRO_TYPES, true,
				MACRO_TYPE_DATA));
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_OPTIMIZATION_DIRECTION,
				"Indicates if the performance value should be minimized or maximized.", OPTIMIZATION_DIRECTIONS,
				OPTIMIZATION_DIRECTION_MAXIMIZE);
		type.setExpert(false);
		types.add(type);

		return types;
	}
}
