/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import org.apache.commons.math3.util.OpenIntToDoubleHashMap;
import org.apache.commons.math3.util.OpenIntToDoubleHashMap.Iterator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.table.DataRow;


/**
 * This is an {@link Aggregator} for the {@link LeastAggregationFunction}. If the least value is not
 * unique, the first value from the nominal mapping will be used.
 * 
 * @author Sebastian Land
 */
public class LeastAggregator implements Aggregator {

	private Attribute sourceAttribute;
	private double[] frequencies;
	private double missingFrequency;
	private OpenIntToDoubleHashMap frequenciesMap;
	private final int mappingSize;

	public LeastAggregator(AggregationFunction function) {
		this.sourceAttribute = function.getSourceAttribute();
		mappingSize = sourceAttribute.getMapping().size();
		if (mappingSize > AggregationFunction.MAX_MAPPING_SIZE) {
			frequenciesMap = new OpenIntToDoubleHashMap();
		} else {
			frequencies = new double[mappingSize];
		}
	}

	@Override
	public void count(Example example) {
		count(example, 1d);
	}

	@Override
	public void count(Example example, double weight) {
		double value = example.getValue(sourceAttribute);
		if (Double.isNaN(value)) {
			missingFrequency += weight;
			return;
		}
		int intValue = (int) value;
		if (frequencies != null) {
			frequencies[intValue] += weight;
			return;
		}
		double oldValue = frequenciesMap.get(intValue);
		boolean wasPresent = !Double.isNaN(oldValue);
		double newValue = wasPresent ? oldValue + weight : weight;
		if (newValue == 0) {
			frequenciesMap.remove(intValue);
		} else {
			frequenciesMap.put(intValue, newValue);
		}
		if (!wasPresent) {
			checkMapFillStatus();
		}
	}

	@Override
	public void set(Attribute attribute, DataRow row) {
		if (frequenciesMap != null && frequenciesMap.size() == 0 && !handlesZeroMapEntries()) {
			row.set(attribute, Double.NaN);
			return;
		}
		int resultIndex = -1;
		double resultFrequency = getStartFrequency();
		if (frequenciesMap != null && handlesZeroMapEntries() && frequenciesMap.size() < mappingSize) {
			resultIndex = handleZeroMapEntries();
		} else if (frequenciesMap != null) {
			// finds the smallest index among those with the same frequency
			Iterator iterator = frequenciesMap.iterator();
			while (iterator.hasNext()) {
				iterator.advance();
				double frequency = iterator.value();
				int index = iterator.key();
				if (testCriteria(frequency, resultFrequency) || frequency == resultFrequency && index < resultIndex) {
					resultIndex = index;
					resultFrequency = frequency;
				}
			}
		} else {
			for (int i = 0; i < frequencies.length; i++) {
				double frequency = frequencies[i];
				if (testCriteria(frequency, resultFrequency)) {
					resultIndex = i;
					resultFrequency = frequency;
					// cannot get smaller than this
					if (frequency == absoluteExtremum()) {
						break;
					}
				}
			}
		}

		// if any counter was greater 0, set result to maximum
		if (resultIndex == -1 || handlesMissings() && testCriteria(missingFrequency, resultFrequency)) {
			row.set(attribute, Double.NaN);
		} else {
			row.set(attribute, attribute.getMapping().mapString(sourceAttribute.getMapping().mapIndex(resultIndex)));
		}
	}

	/**
	 * Returns the absolute extremum when to stop the search
	 *
	 * @return always {@code 0}
	 * @since 9.7
	 */
	protected int absoluteExtremum() {
		return 0;
	}

	/**
	 * Returns the start frequency to test against
	 *
	 * @return {@link Double#POSITIVE_INFINITY} by default
	 * @see #testCriteria(double, double)
	 * @since 9.7
	 */
	protected double getStartFrequency() {
		return Double.POSITIVE_INFINITY;
	}

	/**
	 * Returns whether this aggregator takes care of zero occurrence entries
	 *
	 * @return always {@code true} by default
	 * @since 9.7
	 */
	protected boolean handlesZeroMapEntries() {
		return true;
	}

	/**
	 * Whether this aggregator considers missing value as a possible outcome.
	 *
	 * @return always {@code false} by default
	 * @since 9.7
	 */
	protected boolean handlesMissings() {
		return false;
	}

	/**
	 * Handles zero occurrence (non-)entries
	 *
	 * @return the first mapping index that has zero occurrence
	 * @since 9.7
	 */
	protected int handleZeroMapEntries() {
		// find first index of frequency 0; if the map is actually used, this probably always the case
		for (int i = 0; i < mappingSize; i++) {
			if (!frequenciesMap.containsKey(i)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Tests the least criteria.
	 *
	 * @param frequency    the frequency to check
	 * @param resultFrequency the frequency to check against
	 * @return if the criteria is fulfilled
	 * @since 9.7
	 */
	protected boolean testCriteria(double frequency, double resultFrequency) {
		return frequency < resultFrequency;
	}

	/**
	 * Checks if the {@link #frequenciesMap} is getting too big (threshold: map's size is a third of {@link
	 * #mappingSize}). If the map is too big, it will be replaced with an array again.
	 * <p>
	 * <strong>Note:</strong> The {@link #frequenciesMap} must not be {@code null} when this is called!
	 *
	 * @since 9.7
	 */
	private void checkMapFillStatus() {
		if (frequenciesMap.size() * AggregationFunction.MAP_FILL_RATIO > mappingSize) {
			frequencies = new double[mappingSize];
			Iterator iterator = frequenciesMap.iterator();
			while (iterator.hasNext()) {
				iterator.advance();
				frequencies[iterator.key()] = iterator.value();
			}
			frequenciesMap = null;
		}
	}
}
