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
 * This is an {@link Aggregator} for the {@link SumAggregationFunction}
 * 
 * @author Sebastian Land
 */
public class ProductAggregator extends NumericalAggregator {

	private double product = 1d;

	public ProductAggregator(AggregationFunction function) {
		super(function);
	}

	@Override
	public void count(double value, double weight) {
		product *= Math.pow(value, weight);
	}

	@Override
	public void count(double value) {
		product *= value;
	}

	@Override
	public double getValue() {
		return product;
	}

	@Override
	public void setValue(double value) {
		this.product = value;
	}

}
