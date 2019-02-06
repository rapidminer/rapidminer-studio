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
package com.rapidminer.gui.new_plotter.utility;

/**
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class ContinuousSizeProvider implements SizeProvider {

	private boolean logarithmic;
	private double maxScalingFactor;
	private double minScalingFactor;
	private double minValue;
	private double maxValue;

	public ContinuousSizeProvider(double minValue, double maxValue, double minSize, double maxSize, boolean logarithmic) {
		// sanity checks
		if (minValue > maxValue) {
			throw new IllegalArgumentException("minValue > maxValue not allowed");
		}
		if (logarithmic && minValue <= 0) {
			throw new IllegalArgumentException("minValue <= 0 not allowed for logarithmic");
		}

		this.minValue = minValue;
		this.maxValue = maxValue;
		// this.minScalingFactor = minSize;
		// this.maxScalingFactor = maxSize;
		this.minScalingFactor = minSize * minSize;
		this.maxScalingFactor = maxSize * maxSize;
		this.logarithmic = logarithmic;
	}

	@Override
	public double getScalingFactorForValue(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return Double.NaN;
		}

		// map value to [0,1]
		double fraction;
		if (minValue == maxValue) {
			fraction = 0.5;
		} else {
			if (logarithmic) {
				fraction = (Math.log(value) - Math.log(minValue)) / (Math.log(maxValue) - Math.log(minValue));
			} else {
				fraction = (value - minValue) / (maxValue - minValue);
			}
		}
		// return minScalingFactor + (maxScalingFactor-minScalingFactor)*fraction;
		return Math.sqrt(minScalingFactor + (maxScalingFactor - minScalingFactor) * fraction);
	}

	@Override
	public boolean supportsCategoricalValues() {
		return false;
	}

	@Override
	public boolean supportsNumericalValues() {
		return true;
	}

	@Override
	public SizeProvider clone() {
		return new ContinuousSizeProvider(minValue, maxValue, minScalingFactor, maxScalingFactor, logarithmic);
	}

	@Override
	public double getMinScalingFactor() {
		return minScalingFactor;
	}

	@Override
	public double getMaxScalingFactor() {
		return maxScalingFactor;
	}
}
