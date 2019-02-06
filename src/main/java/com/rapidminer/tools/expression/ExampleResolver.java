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
package com.rapidminer.tools.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.expression.FunctionInput.Category;


/**
 * A {@link Resolver} for {@link Example}s from an {@link ExampleSet}. Should be constructed with an
 * ExampleSet or its meta data ({@link ExampleSetMetaData}). To evaluate an {@link Expression} for
 * all the {@link Example}s call {{@link #bind(Example)} before evaluating the expression.
 * {@link #bind(Example)} can be used in parallel.
 *
 * @author Gisa Schaefer
 * @since 6.5.0
 */
public class ExampleResolver implements Resolver {

	/**
	 * A thread local {@link Example} in order to allow to split the evaluation of
	 * {@link Expression}s over more than one thread.
	 */
	private final ThreadLocal<Example> exampleThreadLocal = new ThreadLocal<Example>();

	private final ExampleSetMetaData metaData;

	public static final String KEY_ATTRIBUTES = I18N.getGUIMessage("gui.dialog.function_input.regular_attributes");
	public static final String KEY_SPECIAL_ATTRIBUTES = I18N.getGUIMessage("gui.dialog.function_input.special_attributes");

	/**
	 * Creates an {@link ExampleResolver} that can bind {@link Example}s that have the same meta
	 * data as the given exampleSet.
	 *
	 * @param exampleSet
	 *            the {@link ExampleSet} from which this resolver should bind examples, cannot be
	 *            {@code null}
	 */
	public ExampleResolver(ExampleSet exampleSet) {
		if (exampleSet == null) {
			throw new IllegalArgumentException("exampleSet must not be null");
		}
		this.metaData = new ExampleSetMetaData(exampleSet, false, false);
	}

	/**
	 * Creates an {@link ExampleResolver} that can bind {@link Example}s that have the given meta
	 * data.
	 *
	 * @param metaData
	 *            the meta data for which the resolver is used, cannot be {@code null}
	 */
	public ExampleResolver(ExampleSetMetaData metaData) {
		if (metaData == null) {
			throw new IllegalArgumentException("metaData must not be null");
		}
		this.metaData = metaData;
	}

	/**
	 * Sets the current Example to example. This Example must have the same meta data as what this
	 * {@link ExampleResolver} was constructed with. The current example is thread local so this
	 * method can be called in parallel.
	 * <p>
	 * Don't forget to call {@link #unbind()} once the expression parser function has been evaluated
	 * to avoid memory leaks.
	 *
	 * @param example
	 *            an example with the same meta data as the resolver was constructed with
	 */
	public void bind(Example example) {
		exampleThreadLocal.set(example);
	}

	/**
	 * Removes the binding of the current example for this {@link ExampleResolver}. Make sure to
	 * call this method only after the expression parser function has been evaluated. Otherwise this
	 * might result in an undefined state.
	 */
	public void unbind() {
		exampleThreadLocal.remove();
	}

	/**
	 * Adds a new {@link AttributeMetaData} object to the current {@link ExampleSetMetaData}.
	 *
	 * @param amd
	 *            the new attribute meta data to add
	 */
	public void addAttributeMetaData(AttributeMetaData amd) {
		this.metaData.addAttribute(amd);
	}

	@Override
	public Collection<FunctionInput> getAllVariables() {
		Collection<AttributeMetaData> metaDataAttributes = metaData.getAllAttributes();
		List<FunctionInput> functionInputs = new ArrayList<>(metaDataAttributes.size());

		for (AttributeMetaData amd : metaDataAttributes) {
			if (amd.isSpecial()) {
				functionInputs.add(new FunctionInput(Category.DYNAMIC, KEY_SPECIAL_ATTRIBUTES, amd.getName(),
				        amd.getValueType(), amd.getRole()));
			} else {
				functionInputs
				        .add(new FunctionInput(Category.DYNAMIC, KEY_ATTRIBUTES, amd.getName(), amd.getValueType(), null));
			}
		}

		return functionInputs;
	}

	@Override
	public ExpressionType getVariableType(String variableName) {
		AttributeMetaData attributeMetaData = metaData.getAttributeByName(variableName);
		if (attributeMetaData == null) {
			return null;
		}
		int ontologyValueType = attributeMetaData.getValueType();
		return ExpressionType.getExpressionType(ontologyValueType);
	}

	@Override
	public String getStringValue(String variableName) {
		if (!(getVariableType(variableName) == ExpressionType.STRING)) {
			throw new IllegalStateException("the variable " + variableName + " does not have a String value");
		}
		Example example = getNonNullExample();
		Attribute attribute = example.getAttributes().get(variableName);
		if (Double.isNaN(example.getValue(attribute))) {
			return null;
		} else {
			return example.getNominalValue(attribute);
		}
	}

	/**
	 * Gets the thread local example and checks that it is not {@code null}.
	 */
	private Example getNonNullExample() {
		Example example = exampleThreadLocal.get();
		if (example == null) {
			throw new IllegalStateException("no example was bound");
		}
		return example;
	}

	@Override
	public double getDoubleValue(String variableName) {
		if (!(getVariableType(variableName) == ExpressionType.DOUBLE
		        || getVariableType(variableName) == ExpressionType.INTEGER)) {
			throw new IllegalStateException("the variable " + variableName + " does not have a double value");
		}
		Example example = getNonNullExample();
		Attribute attribute = example.getAttributes().get(variableName);
		if (getVariableType(variableName) == ExpressionType.INTEGER) {
			return Math.floor(example.getNumericalValue(attribute));
		} else {
			return example.getNumericalValue(attribute);
		}
	}

	@Override
	public boolean getBooleanValue(String variableName) {
		throw new IllegalStateException("Examples never have boolean values");
	}

	@Override
	public Date getDateValue(String variableName) {
		if (!(getVariableType(variableName) == ExpressionType.DATE)) {
			throw new IllegalStateException("the variable " + variableName + " does not have a date value");
		}
		Example example = getNonNullExample();
		Attribute attribute = example.getAttributes().get(variableName);
		if (Double.isNaN(example.getValue(attribute))) {
			return null;
		} else {
			return example.getDateValue(attribute);
		}
	}

}
