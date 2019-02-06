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


import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


/**
 * This is an {@link Aggregator} for numerical mode aggregations.
 *
 * @author Marco Boeck
 * @since 9.2.0
 */
public class NumericalModeAggregator extends NumericalAggregator {

	private Map<Double, Double> frequenciesMap;


	public NumericalModeAggregator() {
		super(null);
		frequenciesMap = new HashMap<>();
	}

	@Override
	public void count(double value) {
		frequenciesMap.merge(value, 1d, Double::sum);
	}

	@Override
	public void count(double value, double weight) {
		frequenciesMap.merge(value, weight, Double::sum);
	}

	@Override
	public double getValue() {
		if (frequenciesMap.isEmpty()) {
			return Double.NaN;
		}

		return Collections.max(frequenciesMap.entrySet(), Comparator.comparingDouble(Map.Entry::getValue)).getKey();
	}

	@Override
	public void setValue(double value) {
		if (frequenciesMap.isEmpty()) {
			return;
		}

		frequenciesMap.put(Collections.max(frequenciesMap.entrySet(), Comparator.comparingDouble(Map.Entry::getValue)).getKey(), value);
	}
}
