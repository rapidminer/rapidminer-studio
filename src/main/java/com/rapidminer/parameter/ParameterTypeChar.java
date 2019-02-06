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
package com.rapidminer.parameter;

import org.w3c.dom.Element;

import com.rapidminer.tools.XMLException;


/**
 * A parameter type for char values. Operators ask for the value with
 * {@link com.rapidminer.operator.Operator#getParameterAsChar(String)}.
 *
 * @author Tobias Malbrecht
 */
public class ParameterTypeChar extends ParameterTypeSingle {

	private static final long serialVersionUID = 6451584265725535856L;

	private static final String ATTRIBUTE_DEFAULT = null;

	private char defaultValue = '\0';

	public ParameterTypeChar(Element element) throws XMLException {
		super(element);

		defaultValue = element.getAttribute(ATTRIBUTE_DEFAULT).charAt(0);
	}

	public ParameterTypeChar(String key, String description, boolean optional, boolean expert) {
		this(key, description, optional);
		setExpert(expert);
	}

	public ParameterTypeChar(String key, String description, boolean optional) {
		super(key, description);
		this.defaultValue = '\0';
		setOptional(optional);
	}

	public ParameterTypeChar(String key, String description) {
		this(key, description, true);
	}

	public ParameterTypeChar(String key, String description, char defaultValue, boolean expert) {
		this(key, description, defaultValue);
		setExpert(expert);
	}

	public ParameterTypeChar(String key, String description, char defaultValue) {
		this(key, description);
		this.defaultValue = defaultValue;
	}

	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue.toString().charAt(0);
	}

	/** Returns false. */
	@Override
	public boolean isNumerical() {
		return false;
	}

	@Override
	public String getRange() {
		return "char" + (defaultValue != '\0' ? "; default: '" + defaultValue + "'" : "");
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return always {@code false}
	 */
	@Override
	public boolean isSensitive() {
		return false;
	}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {
		typeElement.setAttribute(ATTRIBUTE_DEFAULT, String.valueOf(defaultValue));
	}
}
