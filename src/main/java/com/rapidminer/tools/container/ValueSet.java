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
package com.rapidminer.tools.container;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;


/**
 * This class represents a set of (weighted) double values. Might be e.g. used to obtain all
 * distinct values or to efficiently store a large number of values out of a relatively small
 * domain.
 * 
 * @author Tobias Malbrecht
 */
public class ValueSet implements Iterable<Double> {

	private HashMap<Double, Double> valueMap;

	public ValueSet() {
		valueMap = new HashMap<Double, Double>();
	}

	/** Add a value with a weight to the set. */
	public void add(double value, double weight) {
		if (valueMap.containsKey(value)) {
			valueMap.put(value, valueMap.get(value) + weight);
		} else {
			valueMap.put(value, weight);
		}
	}

	/** Add a value to the set. */
	public void add(double value) {
		add(value, 1.0d);
	}

	/** Returns whether the set contains the given value. */
	public boolean contains(double value) {
		return valueMap.containsKey(value);
	}

	/** Returns an iterator over the values. */
	@Override
	public Iterator<Double> iterator() {
		return valueMap.keySet().iterator();
	}

	/**
	 * Returns an iterator over entries each consisting of a value and its weight.
	 */
	public Iterator<Entry<Double, Double>> weightedValuesIterator() {
		return valueMap.entrySet().iterator();
	}

	/** Returns the number of values in the set. */
	public int size() {
		return valueMap.size();
	}

	/** Returns the most common of the values in the set. */
	public double getMode() {
		double mode = Double.NaN;
		double maxWeight = Double.NEGATIVE_INFINITY;
		for (Entry<Double, Double> entry : valueMap.entrySet()) {
			double weight = entry.getValue();
			if (weight > maxWeight) {
				maxWeight = weight;
				mode = entry.getKey();
			}
		}
		return mode;
	}
}
