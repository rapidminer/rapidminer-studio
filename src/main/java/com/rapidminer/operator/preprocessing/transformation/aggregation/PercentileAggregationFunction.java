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
package com.rapidminer.operator.preprocessing.transformation.aggregation;

import com.rapidminer.example.Attribute;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Ontology;


/**
 * Percentile Aggregation Function can be used from the Aggregate Operator
 *
 * @author Andreas Timm
 * @since 9.1.0
 */
public class PercentileAggregationFunction extends NumericalAggregationFunction {

	public static final String FUNCTION_PERCENTILE = "percentile";

	private double percentileValue = -1;

	public PercentileAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDistinct) {
		super(sourceAttribute, ignoreMissings, countOnlyDistinct, FUNCTION_PERCENTILE, FUNCTION_SEPARATOR_OPEN, FUNCTION_SEPARATOR_CLOSE);
	}

	@Override
	public Aggregator createAggregator() {
		final PercentileAggregator percentileAggregator = new PercentileAggregator(this);
		percentileAggregator.setPercentile(percentileValue);
		return percentileAggregator;
	}

	@Override
	public boolean isCompatible() {
		return getSourceAttribute().isNumerical();
	}

	/**
	 * Set the percentile. Must be between 0 and 100.
	 *
	 * @param value
	 * 		the percentage value, between [0, 100]
	 * @return this aggregation function instance
	 * @since 9.2.0
	 */
	public PercentileAggregationFunction setPercentile(double value) {
		this.percentileValue = value;
		return this;
	}

	@Override
	protected int getTargetValueType(int sourceValueType) {
		return Ontology.REAL;
	}

	@Override
	protected boolean matches(String aggregationFunctionName) {
		return aggregationFunctionName != null && aggregationFunctionName.startsWith(FUNCTION_PERCENTILE)
				&& aggregationFunctionName.indexOf('(') >= 0 && aggregationFunctionName.indexOf(')') >= 0;
	}

	@Override
	protected AggregationFunctionMetaDataProvider createMetaDataProvider(String aggregationFunctionName) {
		return new DefaultAggregationFunctionMetaDataProvider(aggregationFunctionName,
				aggregationFunctionName, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE, new int[]{Ontology.NUMERICAL}, Ontology.REAL);
	}

	@Override
	protected PercentileAggregationFunction newInstance(String aggregationFunctionName, Attribute sourceAttribute,
														boolean ignoreMissings, boolean countOnlyDistinct) throws UserError {
		PercentileAggregationFunction paf = new PercentileAggregationFunction(sourceAttribute, ignoreMissings, countOnlyDistinct);
		paf.setPercentile(parseAndCheckValue(aggregationFunctionName));
		String suffix = FUNCTION_SEPARATOR_OPEN + sourceAttribute.getName() + FUNCTION_SEPARATOR_CLOSE;
		paf.targetAttribute.setName(aggregationFunctionName + suffix);
		return paf;
	}

	/**
	 * Read the percentile value from the input and check if the value is > 0 and <= 100
	 *
	 * @param aggregationFunctionName
	 * 		some input that needs to have the format 'percentile (23)' or 'percentile (23.5)'
	 * @return the percentile as double, which is 23.5 for the input 'percentile (23.5)'
	 * @throws UserError
	 * 		in case the customized aggregation function is used wrongly
	 */
	private double parseAndCheckValue(String aggregationFunctionName) throws UserError {
		aggregationFunctionName = String.valueOf(aggregationFunctionName);
		int leftBracket = aggregationFunctionName.indexOf('(') + 1;
		int rightBracket = aggregationFunctionName.indexOf(')');
		if (aggregationFunctionName.startsWith(FUNCTION_PERCENTILE) && leftBracket < rightBracket) {
			final double aDouble;
			final String doubleString = aggregationFunctionName.substring(leftBracket, rightBracket);
			try {
				aDouble = Double.parseDouble(doubleString);
			} catch (NumberFormatException nfe) {
				throw new UserError(null, nfe, "aggregation.percentile.numberformat", doubleString);
			}
			if (aDouble <= 0 || aDouble > 100) {
				throw new UserError(null, "aggregation.percentile.outofbounds", aDouble);
			}
			return aDouble;
		}
		throw new UserError(null, "aggregation.percentile.notparsable", FUNCTION_NAME_PERCENTILE, aggregationFunctionName);
	}
}
