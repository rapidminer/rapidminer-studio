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

import java.util.logging.Level;

import com.rapidminer.tools.LogService;


/**
 * Attribute statistics object for numerical attributes.
 *
 * @author Ingo Mierswa
 */
public class NumericalStatistics implements Statistics {

	private static final long serialVersionUID = -6283236022093847887L;

	private double sum = 0.0d;

	private double squaredSum = 0.0d;

	private int valueCounter = 0;

	public NumericalStatistics() {}

	/** Clone constructor. */
	private NumericalStatistics(NumericalStatistics other) {
		this.sum = other.sum;
		this.squaredSum = other.squaredSum;
		this.valueCounter = other.valueCounter;
	}

	@Override
	public Object clone() {
		return new NumericalStatistics(this);
	}

	@Override
	public void startCounting(Attribute attribute) {
		this.sum = 0.0d;
		this.squaredSum = 0.0d;
		this.valueCounter = 0;
	}

	@Override
	public void count(double value, double weight) {
		if (!Double.isNaN(value)) {
			sum += value;
			squaredSum += value * value;
			valueCounter++;
		}
	}

	@Override
	public boolean handleStatistics(String name) {
		return AVERAGE.equals(name) || VARIANCE.equals(name) || SUM.equals(name);
	}

	@Override
	public double getStatistics(Attribute attribute, String name, String parameter) {
		if (AVERAGE.equals(name)) {
			return this.sum / this.valueCounter;
		} else if (VARIANCE.equals(name)) {
			if (valueCounter <= 1) {
				return 0;
			}
			double variance = (squaredSum - sum * sum / valueCounter) / (valueCounter - 1);
			if (variance < 0) {
				return 0;
			}
			return variance;
		} else if (SUM.equals(name)) {
			return this.sum;
		} else {

			// LogService.getGlobal().log("Cannot calculate statistics, unknown type: " + name,
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.example.NumericalStatistics.calculating_statistics_unknown_type_error", name);
			return Double.NaN;
		}
	}
}
