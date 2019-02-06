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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.table.DataRow;

import java.util.HashSet;


/**
 * This is an {@link Aggregator} for the {@link CountIncludingMissingsAggregationFunction} It counts
 * all examples, if not ignoreMissing is true. Anyway, if only distinct values are counted, Missings
 * are ignored.
 * 
 * @author Sebastian Land
 */
public class CountIncludingMissingsAggregator implements Aggregator {

	private Attribute sourceAttribute;
	private double count = 0;
	private boolean ignoreMissings;

	private boolean isCountingOnlyDistinct = false;
	private HashSet<Double> valuesOccured = null;

	public CountIncludingMissingsAggregator(AggregationFunction function) {
		this.sourceAttribute = function.getSourceAttribute();
		ignoreMissings = function.isIgnoringMissings();
		this.isCountingOnlyDistinct = function.isCountingOnlyDistinct();
		if (isCountingOnlyDistinct) {
			valuesOccured = new HashSet<Double>();
		}

	}

	@Override
	public void count(Example example) {
		if (!ignoreMissings && !isCountingOnlyDistinct) {
			count++;
		} else {
			double value = example.getValue(sourceAttribute);
			if (!Double.isNaN(value)) {
				if (!isCountingOnlyDistinct || valuesOccured.add(value)) {
					count++;
				}
			}
		}
	}

	@Override
	public void count(Example example, double weight) {
		if (!ignoreMissings && !isCountingOnlyDistinct) {
			count += weight;
		} else {
			double value = example.getValue(sourceAttribute);
			if (!Double.isNaN(value)) {
				if (!isCountingOnlyDistinct || valuesOccured.add(value)) {
					count += weight;
				}
			}
		}
	}

	@Override
	public void set(Attribute attribute, DataRow row) {
		row.set(attribute, count);
	}

	public double getCount() {
		return count;
	}

	public void setCount(double count) {
		this.count = count;
	}
}
