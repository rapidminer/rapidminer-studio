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
 * @author Tobias Malbrecht, Sebastian Land
 */
public class EmpiricalNormalDistribution extends NormalDistribution implements EmpiricalDistribution,
		Comparable<EmpiricalNormalDistribution> {

	private static final long serialVersionUID = -1819042904676198636L;

	protected boolean recentlyUpdated;

	protected double sum;

	protected double squaredSum;

	protected double totalWeightSum;

	public EmpiricalNormalDistribution() {
		super(Double.NaN, Double.MIN_VALUE);
		sum = 0;
		squaredSum = 0;
		totalWeightSum = 0;
		recentlyUpdated = false;
	}

	@Override
	public void update(double value, double weight) {
		sum += weight * value;
		squaredSum += weight * value * value;
		totalWeightSum += weight;
		recentlyUpdated = true;
	}

	@Override
	public void update(double value) {
		sum += value;
		squaredSum += value * value;
		totalWeightSum += 1.0d;
		recentlyUpdated = true;
	}

	public void update(EmpiricalNormalDistribution distribution) {
		this.sum += distribution.sum;
		this.squaredSum += distribution.squaredSum;
		this.totalWeightSum += distribution.totalWeightSum;
		recentlyUpdated = true;
	}

	@Override
	public String getAttributeName() {
		return null;
	}

	protected void updateDistributionProperties() {
		if (recentlyUpdated) {
			mean = sum / totalWeightSum;
			standardDeviation = totalWeightSum > 1 ? Math.sqrt((squaredSum - sum * sum / totalWeightSum)
					/ (totalWeightSum - 1)) : Double.MIN_VALUE;
			recentlyUpdated = false;
		}
	}

	@Override
	public double getProbability(double value) {
		updateDistributionProperties();
		return getProbability(mean, standardDeviation, value);
	}

	@Override
	public double getMean() {
		updateDistributionProperties();
		return mean;
	}

	@Override
	public double getStandardDeviation() {
		updateDistributionProperties();
		return standardDeviation;
	}

	@Override
	public double getVariance() {
		updateDistributionProperties();
		return standardDeviation * standardDeviation;
	}

	@Override
	public double getLowerBound() {
		updateDistributionProperties();
		return getLowerBound(mean, standardDeviation);
	}

	@Override
	public double getUpperBound() {
		updateDistributionProperties();
		return getUpperBound(mean, standardDeviation);
	}

	@Override
	public double getTotalWeight() {
		return totalWeightSum;
	}

	@Override
	public String toString() {
		updateDistributionProperties();
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
		updateDistributionProperties();
		if (index == 0) {
			return mean;
		}
		return standardDeviation;
	}

	@Override
	public int compareTo(EmpiricalNormalDistribution otherDistribution) {
		return Double.compare(getMean(), otherDistribution.getMean());
	}
}
