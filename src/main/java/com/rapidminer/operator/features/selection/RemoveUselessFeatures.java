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
package com.rapidminer.operator.features.selection;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * Removes useless attribute from the example set. Useless attributes are
 * <ul>
 * <li>nominal attributes which has the same value for more than <code>p</code> percent of all
 * examples.</li>
 * <li>numerical attributes which standard deviation is less or equal to a given deviation threshold
 * <code>t</code>.</li>
 * </ul>
 * 
 * @author Ingo Mierswa
 */
public class RemoveUselessFeatures extends AbstractFeatureSelection {

	/**
	 * The parameter name for &quot;Removes all numerical attributes with standard deviation less or
	 * equal to this threshold.&quot;
	 */
	public static final String PARAMETER_NUMERICAL_MIN_DEVIATION = "numerical_min_deviation";

	/**
	 * The parameter name for &quot;Removes all nominal attributes which provides more than the
	 * given amount of only one value.&quot;
	 */
	public static final String PARAMETER_NOMINAL_SINGLE_VALUE_UPPER = "nominal_useless_above";

	/**
	 * The parameter name for &quot;Removes all nominal attributes which provides less than the
	 * given amount of at least one value (-1: remove attributes with values occuring only
	 * once).&quot;
	 */
	public static final String PARAMETER_NOMINAL_SINGLE_VALUE_LOWER = "nominal_useless_below";

	private static final String PARAMETER_REMOVE_ID_LIKE = "nominal_remove_id_like";

	public RemoveUselessFeatures(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		metaData.attributesAreSubset();
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		exampleSet.recalculateAllAttributeStatistics();

		double numericalMinDeviation = getParameterAsDouble(PARAMETER_NUMERICAL_MIN_DEVIATION);
		double nominalSingleValueUpper = getParameterAsDouble(PARAMETER_NOMINAL_SINGLE_VALUE_UPPER);
		double nominalSingleValueLower = getParameterAsDouble(PARAMETER_NOMINAL_SINGLE_VALUE_LOWER);

		if (getParameterAsBoolean(PARAMETER_REMOVE_ID_LIKE)) {
			nominalSingleValueLower = 1.0d / exampleSet.size();
		}

		Iterator<Attribute> i = exampleSet.getAttributes().iterator();
		while (i.hasNext()) {
			checkForStop();
			Attribute attribute = i.next();

			if (attribute.isNominal()) {
				Collection<String> values = attribute.getMapping().getValues();
				double[] valueCounts = new double[values.size()];
				int n = 0;
				for (String value : values) {
					checkForStop();
					valueCounts[n] = exampleSet.getStatistics(attribute, Statistics.COUNT, value);
					n++;
				}

				if (exampleSet.getStatistics(attribute, Statistics.UNKNOWN) / exampleSet.size() >= nominalSingleValueUpper) {
					i.remove();
					continue;
				}

				// check for single values which dominates other values and
				// calculate maximum
				double maximumValueCount = Double.NEGATIVE_INFINITY;
				boolean removed = false;
				for (n = 0; n < valueCounts.length; n++) {
					double percent = valueCounts[n] / exampleSet.size();
					maximumValueCount = Math.max(maximumValueCount, percent);
					if (percent >= nominalSingleValueUpper) {
						i.remove();
						removed = true;
						break;
					}
				}
				if (removed) {
					continue;
				}
				// check if the maximum is below lower bound to remove widely
				// spreaded attributes
				if (maximumValueCount <= nominalSingleValueLower) {
					i.remove();
					continue;
				}
			} else if (attribute.isNumerical()) {
				if (exampleSet.getStatistics(attribute, Statistics.UNKNOWN) / exampleSet.size() >= nominalSingleValueUpper) {
					i.remove();
					continue;
				}

				// remove numerical attribute with low deviation
				if (Math.sqrt(exampleSet.getStatistics(attribute, Statistics.VARIANCE)) <= numericalMinDeviation) {
					i.remove();
				}
			} else {
				// do nothing for data attributes
				log("Attribute '" + attribute.getName() + "' is not numerical and not nominal, do nothing...");
			}
			checkForStop();
		}

		if (exampleSet.getAttributes().size() <= 0) {
			logWarning("Example set does not not have any attribute after removing the useless attributes!");
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeDouble(PARAMETER_NUMERICAL_MIN_DEVIATION,
				"Removes all numerical attributes with standard deviation less or equal to this threshold.", 0.0d,
				Double.POSITIVE_INFINITY, 0.0d);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(
				PARAMETER_NOMINAL_SINGLE_VALUE_UPPER,
				"Removes all nominal attributes which most frequent value is contained in more than this fraction of all examples.",
				0.0d, 1.0d, 1.0d);
		type.setExpert(false);
		types.add(type);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_REMOVE_ID_LIKE,
				"If checked, nominal attributes which values appear only once in the complete exampleset are removed.",
				false, false));

		type = new ParameterTypeDouble(
				PARAMETER_NOMINAL_SINGLE_VALUE_LOWER,
				"Removes all nominal attributes which most frequent value is contained in less than this fraction of all examples.",
				0d, 1.0d, 0d);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_REMOVE_ID_LIKE, false, false));
		type.setExpert(false);
		types.add(type);
		return types;
	}

}
