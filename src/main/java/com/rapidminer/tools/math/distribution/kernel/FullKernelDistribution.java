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
package com.rapidminer.tools.math.distribution.kernel;

import com.rapidminer.tools.math.distribution.NormalDistribution;

import java.util.HashMap;
import java.util.Map;


/**
 * An updatable estimated kernel density distribution. Uses one kernel for each value.
 * 
 * @author Tobias Malbrecht
 */
public class FullKernelDistribution extends KernelDistribution {

	public static final long serialVersionUID = -3205432422815818L;

	private boolean heuristicBandwidthSelection;

	private double bandwidth = DEFAULT_BANDWIDTH;

	private boolean recentlyUpdated;

	private HashMap<Double, Double> valueWeightMap;

	private double weightSum;

	private double minValue;

	private double maxValue;

	private static final double STANDARD_NORMAL_LOWER_BOUND = NormalDistribution.getLowerBound(0, 1);

	private static final double STANDARD_NORMAL_UPPER_BOUND = NormalDistribution.getUpperBound(0, 1);

	public FullKernelDistribution() {
		super();
		valueWeightMap = new HashMap<Double, Double>();
		weightSum = 0;
		minValue = Double.POSITIVE_INFINITY;
		maxValue = Double.NEGATIVE_INFINITY;
		heuristicBandwidthSelection = true;
		recentlyUpdated = false;
	}

	public FullKernelDistribution(double bandwidth) {
		super();
		this.bandwidth = bandwidth;
		valueWeightMap = new HashMap<Double, Double>();
		weightSum = 0;
		minValue = Double.POSITIVE_INFINITY;
		maxValue = Double.NEGATIVE_INFINITY;
		heuristicBandwidthSelection = false;
		recentlyUpdated = false;
	}

	@Override
	public void update(double value, double weight) {
		if (!Double.isNaN(value) && !Double.isNaN(weight)) {
			Double totalValueWeight = valueWeightMap.get(value);
			if (totalValueWeight != null) {
				totalValueWeight += weight;
			} else {
				totalValueWeight = new Double(weight);
			}
			valueWeightMap.put(value, totalValueWeight);
			weightSum += weight;
			if (value < minValue) {
				minValue = value;
			}
			if (value > maxValue) {
				maxValue = value;
			}
			recentlyUpdated = true;
		}
	}

	@Override
	public void update(double value) {
		update(value, 1.0d);
	}

	@Override
	public String getAttributeName() {
		return null;
	}

	@Override
	public int getNumberOfParameters() {
		return 0;
	}

	@Override
	public String getParameterName(int index) {
		return null;
	}

	@Override
	public double getParameterValue(int index) {
		return Double.NaN;
	}

	private void updateBandwidth() {
		if (heuristicBandwidthSelection && recentlyUpdated) {
			bandwidth = (maxValue - minValue) / Math.sqrt(weightSum);
			recentlyUpdated = false;
		}
	}

	@Override
	public double getUpperBound() {
		updateBandwidth();
		return NormalDistribution.getUpperBound(maxValue, bandwidth);
	}

	@Override
	public double getLowerBound() {
		updateBandwidth();
		return NormalDistribution.getLowerBound(minValue, bandwidth);
	}

	@Override
	public double getTotalWeight() {
		return weightSum;
	}

	@Override
	public double getProbability(double value) {
		updateBandwidth();
		double probability = 0;
		for (Map.Entry<Double, Double> entry : valueWeightMap.entrySet()) {
			double scaledValue = (value - entry.getKey().doubleValue()) / bandwidth;
			if (scaledValue < STANDARD_NORMAL_LOWER_BOUND || scaledValue > STANDARD_NORMAL_UPPER_BOUND) {
				continue;
			}
			probability += NormalDistribution.getProbability(0, 1, scaledValue) * entry.getValue().doubleValue();
		}
		probability /= bandwidth;
		if (probability == 0) {
			return Double.MIN_VALUE;
		}
		return probability / weightSum;
	}
}
