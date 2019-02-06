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

/**
 * This is an {@link Aggregator} for the {@link VarianceAggregationFunction}
 * 
 * @author Sebastian Land
 */
public class VarianceAggregator extends NumericalAggregator {

	private double valueSum = 0d;
	private double squaredValueSum = 0d;
	private double totalWeightSum = 0d;
	private double count = 0;

	public VarianceAggregator(AggregationFunction function) {
		super(function);
	}

	@Override
	public void count(double value) {
		valueSum += value;
		squaredValueSum += value * value;
		totalWeightSum++;
		count++;
	}

	@Override
	public void count(double value, double weight) {
		valueSum += weight * value;
		squaredValueSum += weight * value * value;
		totalWeightSum += weight;
		count++;
	}

	@Override
	public double getValue() {
		if (count > 0) {
			return (squaredValueSum - valueSum * valueSum / totalWeightSum) / ((count - 1) / count * totalWeightSum);
		} else {
			return Double.NaN;
		}
	}
}
