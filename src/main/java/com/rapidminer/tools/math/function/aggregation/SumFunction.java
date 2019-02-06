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

import com.rapidminer.example.Attribute;


/**
 * Calculates the sum.
 * 
 * @author Tobias Malbrecht, Ingo Mierswa
 * 
 */
public class SumFunction extends AbstractAggregationFunction {

	private double valueSum;

	public SumFunction() {
		this(DEFAULT_IGNORE_MISSINGS);
	}

	public SumFunction(Boolean ignoreMissings) {
		super(ignoreMissings);
	}

	@Override
	public String getName() {
		return "sum";
	}

	@Override
	protected void reset() {
		foundMissing = false;
		valueSum = 0.0d;
	}

	@Override
	public void update(double value, double weight) {
		if (Double.isNaN(value)) {
			foundMissing = true;
			return;
		}
		valueSum += value * weight;
	}

	@Override
	public void update(double value) {
		if (Double.isNaN(value)) {
			foundMissing = true;
			return;
		}
		valueSum += value;
	}

	@Override
	public double getValue() {
		if (foundMissing && !ignoreMissings) {
			return Double.NaN;
		}
		return valueSum;
	}

	@Override
	public boolean supportsAttribute(Attribute attribute) {
		return attribute.isNumerical();
	}
}
