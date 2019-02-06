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

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.table.NumericalAttribute;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.WrapperOperatorRuntimeException;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.tools.Ontology;


/**
 * Concatenates the values of the selected attributes (converted to string using
 * {@link Attribute#getAsString(double, int, boolean) Attribute.getAsString}) separated by '|' by default.
 * Supports any attribute type but does not allow for sequential updates.
 *
 * @since 9.0.0
 * @author Jan Czogalla
 */
public class ConcatenationFunction implements AggregationFunction {

	public static final String CONCATENATION_NAME = "concatenation";
	
	private final List<Attribute> attributes = new ArrayList<>();
	private Attribute targetAttribute;
	private String separator = "|";

	public ConcatenationFunction() {
	}

	public ConcatenationFunction(Boolean ignoreMissings) {
	}

	@Override
	public String getName() {
		return CONCATENATION_NAME;
	}

	/** Unsupported operation. */
	@Override
	public void update(double value, double weight) {
		throw new UnsupportedOperationException();
	}

	/** Unsupported operation. */
	@Override
	public void update(double value) {
		throw new UnsupportedOperationException();
	}

	/** Unsupported operation. */
	@Override
	public double getValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double calculate(double[] values) {
		return calculate(values, null);
	}

	@Override
	public double calculate(double[] values, double[] weights) {
		if (targetAttribute == null) {
			throw new WrapperOperatorRuntimeException(new UserError(null, 136));
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			Attribute attribute = attributes.get(i);
			builder.append(attribute.getAsString(values[i], NumericalAttribute.DEFAULT_NUMBER_OF_DIGITS, false));
			if (i < values.length - 1) {
				builder.append(separator);
			}
		}
		return targetAttribute.getMapping().mapString(builder.toString());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Also stores the attribute to allow query for nominal values.
	 */
	@Override
	public boolean supportsAttribute(Attribute attribute) {
		attributes.add(attribute);
		return true;
	}

	@Override
	public boolean supportsAttribute(AttributeMetaData amd) {
		return true;
	}

	@Override
	public boolean supportsValueType(int valueType) {
		return true;
	}

	@Override
	public int getValueTypeOfResult(int inputType) {
		return Ontology.NOMINAL;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Expects a nominal attribute.
	 */
	@Override
	public void setTargetAttribute(Attribute attribute) {
		if (!attribute.isNominal()) {
			throw new WrapperOperatorRuntimeException(new UserError(null, 120, attribute.getName(), attribute.getValueType(), Ontology.NOMINAL));
		}
		targetAttribute = attribute;
	}

	/**
	 * The separator string that should be used between attribute values.
	 *
	 * @param separator
	 * 		the separator string
	 */
	public void setSeparator(String separator) {
		if (separator != null) {
			this.separator = separator;
		}
	}
}
