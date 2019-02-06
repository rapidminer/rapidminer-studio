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


/**
 * This is an {@link Aggregator} for the {@link LeastAggregationFunction}. If the least value is not
 * unique, the first value from the nominal mapping will be used.
 * 
 * @author Sebastian Land
 */
public class LeastAggregator implements Aggregator {

	private Attribute sourceAttribute;
	private double[] frequencies;

	public LeastAggregator(AggregationFunction function) {
		this.sourceAttribute = function.getSourceAttribute();
		frequencies = new double[sourceAttribute.getMapping().size()];
	}

	@Override
	public void count(Example example) {
		count(example, 1d);
	}

	@Override
	public void count(Example example, double weight) {
		double value = example.getValue(sourceAttribute);
		if (!Double.isNaN(value)) {
			frequencies[(int) value] += weight;
		}
	}

	@Override
	public void set(Attribute attribute, DataRow row) {
		int minIndex = -1;
		double minFrequency = Double.POSITIVE_INFINITY;
		for (int i = 0; i < frequencies.length; i++) {
			if (frequencies[i] < minFrequency) {
				minIndex = i;
				minFrequency = frequencies[i];
			}
		}
		// if any counter was greater 0, set result to maximum
		if (minIndex > -1) {
			row.set(attribute, attribute.getMapping().mapString(sourceAttribute.getMapping().mapIndex(minIndex)));
		} else {
			row.set(attribute, Double.NaN);
		}
	}
}
