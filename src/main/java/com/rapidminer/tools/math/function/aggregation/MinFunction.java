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
package com.rapidminer.tools.math.function.aggregation;

import com.rapidminer.example.Attribute;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.tools.Ontology;


/**
 * Calculates the minimum.
 *
 * @author Tobias Malbrecht, Ingo Mierswa
 *
 */
public class MinFunction extends AbstractAggregationFunction {

	private double minValue;

	public MinFunction() {
		this(DEFAULT_IGNORE_MISSINGS);
	}

	public MinFunction(Boolean ignoreMissings) {
		super(ignoreMissings);
	}

	@Override
	public String getName() {
		return "minimum";
	}

	@Override
	protected void reset() {
		foundMissing = false;
		minValue = Double.POSITIVE_INFINITY;
	}

	@Override
	public void update(double value, double weight) {
		update(value);
	}

	@Override
	public void update(double value) {
		if (Double.isNaN(value)) {
			foundMissing = true;
			return;
		}
		if (value < minValue) {
			minValue = value;
		}
	}

	@Override
	public double getValue() {
		if (foundMissing && !ignoreMissings) {
			return Double.NaN;
		}
		return minValue;
	}

	@Override
	public boolean supportsAttribute(Attribute attribute) {
		return attribute.isNumerical() || Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME);
	}

	@Override
	public boolean supportsAttribute(AttributeMetaData amd) {
		return amd.isNumerical() || Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), Ontology.DATE_TIME);
	}

	@Override
	public boolean supportsValueType(int valueType) {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NUMERICAL)
				|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE_TIME);
	}
}
