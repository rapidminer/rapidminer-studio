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
import com.rapidminer.tools.Ontology;

import java.util.List;


/**
 * This function first behaves like {@link CountAggregationFunction}, but it delivers percentages of
 * the total count instead of absolute values. E.g. {@link SumAggregationFunction} delivers [2, 5,
 * 3] this function would deliver [20, 50, 30]
 * 
 * @author Marco Boeck
 * 
 */
public abstract class AbstractCountRatioAggregationFunction extends CountAggregationFunction {

	public static final String FUNCTION_COUNT_PERCENTAGE = "percentage_count";

	public AbstractCountRatioAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings,
			boolean countOnlyDisctinct, String functionName) {
		super(sourceAttribute, ignoreMissings, countOnlyDisctinct, functionName, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE);
	}

	public AbstractCountRatioAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings,
			boolean countOnlyDisctinct, String functionName, String separatorOpen, String separatorClose) {
		super(sourceAttribute, ignoreMissings, countOnlyDisctinct, functionName, separatorOpen, separatorClose);
	}

	public abstract double getRatioFactor();

	@Override
	public Aggregator createAggregator() {
		return new CountIncludingMissingsAggregator(this);
	}

	@Override
	public void postProcessing(List<Aggregator> allAggregators) {
		double totalCount = 0;

		// calculate total count
		for (Aggregator aggregator : allAggregators) {
			if(aggregator == null){
				continue;
			}
			double value = ((CountIncludingMissingsAggregator) aggregator).getCount();
			if (Double.isNaN(value)) {
				totalCount = Double.NaN;
				break;
			}
			totalCount += value;
		}

		// divide by total count
		for (Aggregator aggregator : allAggregators) {
			if(aggregator == null){
				continue;
			}
			CountIncludingMissingsAggregator countAggregator = (CountIncludingMissingsAggregator) aggregator;
			countAggregator.setCount((countAggregator.getCount() / totalCount) * getRatioFactor());
		}
	}

	@Override
	protected int getTargetValueType(int sourceValueType) {
		return Ontology.REAL;
	}
}
