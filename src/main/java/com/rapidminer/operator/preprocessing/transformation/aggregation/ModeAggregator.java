/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.table.DataRow;

import java.util.HashMap;
import java.util.Map.Entry;


/**
 * This is an {@link Aggregator} for the {@link ModeAggregationFunction}. If the mode is not unique,
 * the first value from the nominal mapping will be used.
 * 
 * @author Sebastian Land
 */
public class ModeAggregator implements Aggregator {

	private Attribute sourceAttribute;
	private double[] frequencies;
	private HashMap<Double, Double> frequenciesMap;

	public ModeAggregator(AggregationFunction function) {
		this.sourceAttribute = function.getSourceAttribute();
		if (sourceAttribute.isNominal()) {
			frequencies = new double[sourceAttribute.getMapping().size()];
		} else {
			frequenciesMap = new HashMap<Double, Double>();
		}
	}

	@Override
	public void count(Example example) {
		double value = example.getValue(sourceAttribute);
		if (!Double.isNaN(value)) {
			if (frequencies != null) {
				frequencies[(int) value]++;
			} else {
				Double frequency = frequenciesMap.get(value);
				if (frequency == null) {
					frequenciesMap.put(value, 1d);
				} else {
					frequenciesMap.put(value, frequency + 1d);
				}
			}
		}
	}

	@Override
	public void count(Example example, double weight) {
		double value = example.getValue(sourceAttribute);
		if (!Double.isNaN(value)) {
			if (frequencies != null) {
				frequencies[(int) value] += weight;
			} else {
				Double frequency = frequenciesMap.get(value);
				if (frequency == null) {
					frequenciesMap.put(value, weight);
				} else {
					frequenciesMap.put(value, frequency + weight);
				}
			}
		}
	}

	@Override
	public void set(Attribute attribute, DataRow row) {
		double minValue = -1;
		double minFrequency = Double.NEGATIVE_INFINITY;

		if (frequencies != null) {
			for (int i = 0; i < frequencies.length; i++) {
				if (frequencies[i] > minFrequency) {
					minValue = i;
					minFrequency = frequencies[i];
				}
			}
		} else {
			for (Entry<Double, Double> entry : frequenciesMap.entrySet()) {
				double frequency = entry.getValue();
				if (frequency > minFrequency) {
					minValue = entry.getKey();
					minFrequency = frequency;
				}

			}
		}
		// if any counter was greater 0, set result to maximum
		if (minValue > -1) {
			row.set(attribute, minValue);
		} else {
			row.set(attribute, Double.NaN);
		}
	}
}
