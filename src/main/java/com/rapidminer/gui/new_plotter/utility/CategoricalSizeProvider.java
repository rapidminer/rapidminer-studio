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

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class CategoricalSizeProvider implements SizeProvider {

	// maps categories (identified by their value, not by their index)
	// to scaling factors
	private Map<Double, Double> sizeMap;
	private double minScalingFactor;
	private double maxScalingFactor;

	public CategoricalSizeProvider(List<Double> categoryList, double minSize, double maxSize) {
		this.sizeMap = createSizeMapping(categoryList, minSize, maxSize);
		updateMinMaxScalingFactor();
	}

	public CategoricalSizeProvider(Map<Double, Double> sizeMap) {
		this.sizeMap = sizeMap;
		updateMinMaxScalingFactor();
	}

	private void updateMinMaxScalingFactor() {
		minScalingFactor = Double.POSITIVE_INFINITY;
		maxScalingFactor = Double.NEGATIVE_INFINITY;
		for (Double factor : sizeMap.values()) {
			if (factor < minScalingFactor) {
				minScalingFactor = factor;
			}
			if (factor > maxScalingFactor) {
				maxScalingFactor = factor;
			}
		}
	}

	private Map<Double, Double> createSizeMapping(List<Double> categoryList, double minSize, double maxSize) {
		int categoryCount = categoryList.size();
		Map<Double, Double> sizeMap = new HashMap<Double, Double>();
		if (categoryCount > 0) {
			ContinuousSizeProvider sizeProvider = new ContinuousSizeProvider(0, categoryCount - 1, minSize, maxSize, false);
			int idx = 0;
			for (Double category : categoryList) {
				sizeMap.put(category, sizeProvider.getScalingFactorForValue(idx));
				++idx;
			}
		}
		return sizeMap;
	}

	@Override
	public double getScalingFactorForValue(double value) {
		return sizeMap.get(value);
	}

	@Override
	public boolean supportsCategoricalValues() {
		return true;
	}

	@Override
	public boolean supportsNumericalValues() {
		return false;
	}

	@Override
	public CategoricalSizeProvider clone() {
		Map<Double, Double> clonedSizeMap = new HashMap<Double, Double>();
		clonedSizeMap.putAll(sizeMap);
		return new CategoricalSizeProvider(clonedSizeMap);
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
