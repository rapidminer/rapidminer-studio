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

import java.util.List;


/**
 * This function first behaves like {@link SumAggregationFunction}, but it delivers fractions of the
 * total sum instead of absolute values. E.g. {@link SumAggregationFunction} delivers [6, 10, 4]
 * this function would deliver [0.3, 0.5, 0.2]
 * 
 * @author Marius Helf
 * 
 */
public class SumFractionalAggregationFunction extends SumAggregationFunction {

	public static final String FUNCTION_SUM_FRACTIONAL = "fractional_sum";

	public SumFractionalAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDisctinct) {
		super(sourceAttribute, ignoreMissings, countOnlyDisctinct, FUNCTION_SUM_FRACTIONAL, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE);
	}

	public SumFractionalAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDisctinct,
			String functionName, String separatorOpen, String separatorClose) {
		super(sourceAttribute, ignoreMissings, countOnlyDisctinct, functionName, separatorOpen, separatorClose);
	}

	@Override
	public void postProcessing(List<Aggregator> allAggregators) {
		double totalSum = 0;

		// calculate total sum
		for (Aggregator aggregator : allAggregators) {
			double value = ((SumAggregator) aggregator).getValue();
			if (value < 0 || Double.isNaN(value)) {
				totalSum = Double.NaN;
				break;
			}
			totalSum += value;
		}

		// devide by total sum
		for (Aggregator aggregator : allAggregators) {
			SumAggregator sumAggregator = (SumAggregator) aggregator;
			sumAggregator.setValue(sumAggregator.getValue() / totalSum);
		}
	}
}
