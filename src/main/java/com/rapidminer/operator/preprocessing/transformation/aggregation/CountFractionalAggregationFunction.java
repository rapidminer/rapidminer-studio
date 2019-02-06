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


/**
 * This function first behaves like {@link CountAggregationFunction}, but it delivers fractions of
 * the total count instead of absolute values. E.g. {@link SumAggregationFunction} delivers [20, 50,
 * 30] this function would deliver [0.2, 0.5, 0.3]
 * 
 * @author Marco Boeck
 * 
 */
public class CountFractionalAggregationFunction extends AbstractCountRatioAggregationFunction {

	public static final String FUNCTION_COUNT_FRACTIONAL = "fractional_count";

	public CountFractionalAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDisctinct) {
		super(sourceAttribute, ignoreMissings, countOnlyDisctinct, FUNCTION_COUNT_FRACTIONAL);
	}

	public CountFractionalAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDisctinct,
			String functionName, String separatorOpen, String separatorClose) {
		super(sourceAttribute, ignoreMissings, countOnlyDisctinct, functionName, separatorOpen, separatorClose);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.operator.preprocessing.transformation.aggregation.
	 * AbstractCountRatioAggregationFunction#getRatioFactor()
	 */
	@Override
	public double getRatioFactor() {
		return 1.0;
	}

}
