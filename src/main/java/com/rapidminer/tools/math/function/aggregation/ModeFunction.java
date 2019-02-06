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

import java.util.HashMap;

import com.rapidminer.example.Attribute;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;


/**
 * Calculates the mode of some values.
 *
 * @author Tobias Malbrecht
 *
 */
public class ModeFunction extends AbstractAggregationFunction {

	private HashMap<Double, Double> valueWeightMap;

	private double maximumValueWeight;

	private double valueMaximumWeight;

	public ModeFunction() {
		this(DEFAULT_IGNORE_MISSINGS);
	}

	public ModeFunction(Boolean ignoreMissings) {
		super(ignoreMissings);
		valueWeightMap = new HashMap<Double, Double>();
		maximumValueWeight = Double.NEGATIVE_INFINITY;
		valueMaximumWeight = Double.NaN;
	}

	@Override
	public String getName() {
		return "mode";
	}

	@Override
	protected void reset() {
		foundMissing = false;
		maximumValueWeight = 0;
		valueMaximumWeight = Double.NaN;
		if (valueWeightMap != null) {
			valueWeightMap.clear();
		}
	}

	@Override
	public void update(double value, double weight) {
		if (Double.isNaN(value)) {
			foundMissing = true;
			return;
		}
		Double totalValueWeight = valueWeightMap.get(value);
		if (totalValueWeight != null) {
			totalValueWeight += weight;
		} else {
			totalValueWeight = new Double(weight);
		}
		valueWeightMap.put(value, totalValueWeight);
		if (totalValueWeight.doubleValue() > maximumValueWeight) {
			maximumValueWeight = totalValueWeight;
			valueMaximumWeight = value;
		}
	}

	@Override
	public void update(double value) {
		if (Double.isNaN(value)) {
			foundMissing = true;
			return;
		}
		Double totalValueWeight = valueWeightMap.get(value);
		if (totalValueWeight != null) {
			totalValueWeight++;
		} else {
			totalValueWeight = new Double(1);
		}
		valueWeightMap.put(value, totalValueWeight);
		if (totalValueWeight > maximumValueWeight) {
			maximumValueWeight = totalValueWeight;
			valueMaximumWeight = value;
		}
	}

	@Override
	public double getValue() {
		if (foundMissing && !ignoreMissings) {
			return Double.NaN;
		}
		return valueMaximumWeight;
	}

	@Override
	public boolean supportsAttribute(Attribute attribute) {
		return true;
	}

	@Override
	public boolean supportsAttribute(AttributeMetaData amd) {
		return true;
	}

	@Override
	public boolean supportsValueType(int valueType) {
		return true;
	}
}
