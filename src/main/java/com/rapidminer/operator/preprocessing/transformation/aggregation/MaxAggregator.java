/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
 * This is an {@link Aggregator} for the {@link MaxAggregationFunction}
 * 
 * @author Sebastian Land
 */
public class MaxAggregator extends NumericalAggregator {

	private double max = Double.NEGATIVE_INFINITY;
	private boolean hasValue = false;

	public MaxAggregator(AggregationFunction function) {
		super(function);
	}

	@Override
	public void count(double value) {
		hasValue = true;
		if (max < value) {  // NaN would always return false: Implicit NaN check
			max = value;
		}
	}

	@Override
	protected void count(double value, double weight) {
		hasValue = true;
		if (max < value) {  // NaN would always return false: Implicit NaN check
			max = value;
		}
	}

	@Override
	public double getValue() {
		if (hasValue) {
			return max;
		} else {
			return Double.NaN;
		}
	}

	@Override
	public void setValue(double value) {
		this.max = value;
	}
}
