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


/**
 * @author Marius Helf
 * 
 */
public class ConcatAggregator implements Aggregator {

	private ConcatAggregationFunction function;
	boolean first = true;
	private StringBuilder concatenation = new StringBuilder();

	public ConcatAggregator(ConcatAggregationFunction concatAggregationFunction) {
		this.function = concatAggregationFunction;
	}

	@Override
	public void count(Example example) {

		Attribute sourceAttribute = function.getSourceAttribute();
		double value = example.getValue(sourceAttribute);
		if (!Double.isNaN(value)) {
			if (first) {
				first = false;
			} else {
				concatenation.append(function.getSeparator());
			}
			String nominalValue = sourceAttribute.getMapping().mapIndex((int) value);
			concatenation.append(nominalValue);
		}
	}

	@Override
	public void count(Example example, double weight) {
		count(example);
	}

	@Override
	public void set(Attribute attribute, DataRow row) {
		int idx = attribute.getMapping().mapString(concatenation.toString());
		attribute.setValue(row, idx);
	}

}
