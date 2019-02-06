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
package com.rapidminer.tools.math.function.aggregation;

import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.tools.Ontology;

import java.lang.reflect.InvocationTargetException;


/**
 * Superclass for aggregation functions providing some generic functions.
 * 
 * In comparison to the more specialized functions available in the
 * {@link com.rapidminer.operator.preprocessing.transformation.aggregation.AggregationFunction}
 * these functions have a more broader use, but are limited to numerical values.
 * 
 * @author Tobias Malbrecht
 * 
 */
public abstract class AbstractAggregationFunction implements AggregationFunction {

	@SuppressWarnings("unchecked")
	public static final Class<? extends AggregationFunction>[] KNOWN_AGGREGATION_FUNCTIONS = new Class[] {
			AverageFunction.class, VarianceFunction.class, StandardDeviationFunction.class, CountFunction.class,
			MinFunction.class, MaxFunction.class, SumFunction.class, ModeFunction.class, MedianFunction.class,
			ProductFunction.class };

	public static final String[] KNOWN_AGGREGATION_FUNCTION_NAMES = { "average", "variance", "standard_deviation", "count",
			"minimum", "maximum", "sum", "mode", "median", "product" };

	public enum AggregationFunctionType {
		average, variance, standard_deviation, count, minimum, maximum, sum, mode, median, product,
	}

	public static final int AVERAGE = 0;

	public static final int VARIANCE = 1;

	public static final int STANDARD_DEVIATION = 2;

	public static final int COUNT = 3;

	public static final int MINIMUM = 4;

	public static final int MAXIMUM = 5;

	public static final int SUM = 6;

	public static final int MODE = 7;

	public static final int MEDIAN = 8;

	public static final int PRODUCT = 9;

	public static final boolean DEFAULT_IGNORE_MISSINGS = true;

	protected boolean ignoreMissings = DEFAULT_IGNORE_MISSINGS;

	protected boolean foundMissing = false;

	@SuppressWarnings("unchecked")
	public static AggregationFunction createAggregationFunction(String functionName, boolean ignoreMissings)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException,
			InvocationTargetException {
		int typeIndex = -1;
		for (int i = 0; i < KNOWN_AGGREGATION_FUNCTION_NAMES.length; i++) {
			if (KNOWN_AGGREGATION_FUNCTION_NAMES[i].equals(functionName)) {
				typeIndex = i;
				break;
			}
		}
		Class<? extends AggregationFunction> clazz = null;
		if (typeIndex < 0) {
			clazz = (Class<? extends AggregationFunction>) Class.forName(functionName);
		} else {
			clazz = KNOWN_AGGREGATION_FUNCTIONS[typeIndex];
		}
		return clazz.getConstructor(Boolean.class).newInstance(ignoreMissings);
	}

	public static AggregationFunction createAggregationFunction(String functionName) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
		return createAggregationFunction(functionName, true);
	}

	public static AggregationFunction createAggregationFunction(int typeIndex, boolean ignoreMissings)
			throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		if (typeIndex >= 0 && typeIndex < KNOWN_AGGREGATION_FUNCTION_NAMES.length) {
			Class<? extends AggregationFunction> clazz = KNOWN_AGGREGATION_FUNCTIONS[typeIndex];
			return clazz.getConstructor(new Class[] { Boolean.class }).newInstance(ignoreMissings);
		} else {
			throw new InstantiationException();
		}
	}

	public static AggregationFunction createAggregationFunction(int typeIndex)
			throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		return createAggregationFunction(typeIndex, true);
	}

	public AbstractAggregationFunction() {
		this(true);
	}

	public AbstractAggregationFunction(Boolean ignoreMissings) {
		this.ignoreMissings = ignoreMissings;
		this.foundMissing = false;
		reset();
	}

	/**
	 * Reset the counters.
	 */
	protected abstract void reset();

	/**
	 * Resets the counters and computes the aggregation function solely based on the given values.
	 */
	@Override
	public synchronized double calculate(double[] values) {
		reset();
		for (int i = 0; i < values.length; i++) {
			update(values[i]);
		}
		return getValue();
	}

	/**
	 * Resets the counters and computes the aggregation function solely based on the given values
	 * and the given weights.
	 */
	@Override
	public synchronized double calculate(double[] values, double[] weights) {
		reset();
		if (values.length != weights.length) {
			return Double.NaN;
		}
		for (int i = 0; i < values.length; i++) {
			update(values[i], weights[i]);
		}
		return getValue();
	}

	/**
	 * Standard behavior is to return true for all numerical attributes
	 */
	@Override
	public boolean supportsAttribute(AttributeMetaData amd) {
		return amd.isNumerical();
	}

	/**
	 * Standard behavior is to return true for all numerical value types.
	 */
	@Override
	public boolean supportsValueType(int valueType) {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NUMERICAL);
	}

	/**
	 * Standard behaviour is to return inputType, i.e. same output type as input type.
	 */
	@Override
	public int getValueTypeOfResult(int inputType) {
		return inputType;
	}
}
