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
package com.rapidminer.example;

import com.rapidminer.tools.LogService;

import java.util.logging.Level;


/**
 * Attribute statistics object for weighted numerical attributes.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class WeightedNumericalStatistics implements Statistics {

	private static final long serialVersionUID = -6283236022093847887L;

	private double sum = 0.0d;

	private double squaredSum = 0.0d;

	private double totalWeight = 0.0d;

	private double count = 0.0d;

	public WeightedNumericalStatistics() {}

	/** Clone constructor. */
	private WeightedNumericalStatistics(WeightedNumericalStatistics other) {
		this.sum = other.sum;
		this.squaredSum = other.squaredSum;
		this.totalWeight = other.totalWeight;
		this.count = other.count;
	}

	@Override
	public Object clone() {
		return new WeightedNumericalStatistics(this);
	}

	@Override
	public void startCounting(Attribute attribute) {
		this.sum = 0.0d;
		this.squaredSum = 0.0d;
		this.totalWeight = 0;
		this.count = 0;
	}

	@Override
	public void count(double value, double weight) {
		if (Double.isNaN(weight)) {
			weight = 1.0d;
		}
		if (!Double.isNaN(value)) {
			sum += (weight * value);
			squaredSum += weight * value * value;
			totalWeight += weight;
			count++;
		}
	}

	@Override
	public boolean handleStatistics(String name) {
		return AVERAGE_WEIGHTED.equals(name) || VARIANCE_WEIGHTED.equals(name) || SUM_WEIGHTED.equals(name);
	}

	@Override
	public double getStatistics(Attribute attribute, String name, String parameter) {
		if (AVERAGE_WEIGHTED.equals(name)) {
			return this.sum / this.totalWeight;
		} else if (VARIANCE_WEIGHTED.equals(name)) {
			if (count <= 1) {
				return 0;
			}
			return (squaredSum - (sum * sum) / totalWeight) / (((count - 1) / count) * totalWeight);
		} else if (SUM_WEIGHTED.equals(name)) {
			return this.sum;
		} else {
			// LogService.getGlobal().log("Cannot calculate statistics, unknown type: " + name,
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.example.WeightedNumericalStatistics.calculating_statistics_unknown_type_error", name);
			return Double.NaN;
		}
	}
}
