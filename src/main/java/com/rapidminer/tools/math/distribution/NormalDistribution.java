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
package com.rapidminer.tools.math.distribution;

import com.rapidminer.tools.Tools;


/**
 * This class represents a gaussian normal distribution.
 * 
 * @author Tobias Malbrecht, Sebastian Land <<<<<<< NormalDistribution.java
 * @version $Id: NormalDistribution.java,v 1.3.2.3 2009-04-08 14:40:23 tobiasmalbrecht Exp $ =======
 *          >>>>>>> 1.3.2.2
 */
public class NormalDistribution extends ContinuousDistribution {

	private static final long serialVersionUID = -1819042904676198636L;

	private static final double BOUND_FACTOR = 5;

	private static final double SQRT_FACTOR = Math.sqrt(2 * Math.PI);

	protected double mean;

	protected double standardDeviation;

	public NormalDistribution(double mean, double standardDeviation) {
		this.mean = mean;
		this.standardDeviation = standardDeviation;
	}

	@Override
	public String getAttributeName() {
		return null;
	}

	public static double getProbability(double mean, double standardDeviation, double value) {
		double base = (value - mean) / standardDeviation;
		return Math.exp(-0.5 * (base * base)) / (standardDeviation * SQRT_FACTOR);
	}

	public static double getLogProbability(double mean, double standardDeviation, double value) {
		double base = (value - mean) / standardDeviation;
		return -Math.log(standardDeviation * SQRT_FACTOR) - 0.5 * (base * base);
	}

	public static final double getLowerBound(double mean, double standardDeviation) {
		return mean - BOUND_FACTOR * standardDeviation;
	}

	public static final double getUpperBound(double mean, double standardDeviation) {
		return mean + BOUND_FACTOR * standardDeviation;
	}

	@Override
	public double getProbability(double value) {
		return getProbability(mean, standardDeviation, value);
	}

	public double getMean() {
		return mean;
	}

	public double getStandardDeviation() {
		return standardDeviation;
	}

	public double getVariance() {
		return standardDeviation * standardDeviation;
	}

	@Override
	public double getLowerBound() {
		return getLowerBound(mean, standardDeviation);
	}

	@Override
	public double getUpperBound() {
		return getUpperBound(mean, standardDeviation);
	}

	@Override
	public String toString() {
		return ("Normal distribution --> mean: " + Tools.formatNumber(mean) + ", standard deviation: " + Tools
				.formatNumber(standardDeviation));
	}

	@Override
	public int getNumberOfParameters() {
		return 2;
	}

	@Override
	public String getParameterName(int index) {
		if (index == 0) {
			return "mean";
		}
		return "standard deviation";
	}

	@Override
	public double getParameterValue(int index) {
		if (index == 0) {
			return mean;
		}
		return standardDeviation;
	}
}
