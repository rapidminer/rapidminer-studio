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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.table.DataRow;


/**
 * @author Marius Helf
 * 
 */
public class ConcatAggregator implements Aggregator {

	private final ConcatAggregationFunction function;
	private final Collection<String> values;

	public ConcatAggregator(ConcatAggregationFunction concatAggregationFunction) {
		this.function = concatAggregationFunction;
		this.values = function.isCountingOnlyDistinct() ? new LinkedHashSet<>() : new ArrayList<>();
	}

	@Override
	public void count(Example example) {
		Attribute sourceAttribute = function.getSourceAttribute();
		double value = example.getValue(sourceAttribute);
		if (!Double.isNaN(value)) {
			values.add(sourceAttribute.getMapping().mapIndex((int) value));
		}
	}

	@Override
	public void count(Example example, double weight) {
		count(example);
	}

	@Override
	public void set(Attribute attribute, DataRow row) {
		final double idx;
		if (values.isEmpty() && !function.isIgnoringMissings()) {
			idx = Double.NaN;
		} else {
			idx = attribute.getMapping().mapString(String.join(function.getSeparator(), values));
		}
		attribute.setValue(row, idx);
	}

}
