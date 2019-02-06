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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;


/**
 * Aggregator for Percentile aggregation, using apache commons math lib implementation
 *
 * @author Andreas Timm
 * @since 9.1.0
 */
public class PercentileAggregator extends NumericalAggregator {

	private double percentile;
	private List<Double> elements = new ArrayList<>();

	public PercentileAggregator(AggregationFunction function) {
		super(function);
	}

	/**
	 * Set the percentile to be calculated, is checked during execution
	 *
	 * @param value
	 * 		> 0 and value <= 100
	 */
	public void setPercentile(double value) {
		this.percentile = value;
	}

	@Override
	public void count(double value) {
		elements.add(value);
	}

	@Override
	public void count(double value, double weight) {
		count(value);
	}

	@Override
	protected double getValue() {
		Percentile percentileCalc = new Percentile();
		percentileCalc.setData(ArrayUtils.toPrimitive(elements.toArray(new Double[0])));
		return percentileCalc.evaluate(percentile);
	}
}
