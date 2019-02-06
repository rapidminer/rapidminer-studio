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
 * A parameter type for boolean parameters. Operators ask for the boolean value with
 * {@link com.rapidminer.operator.Operator#getParameterAsBoolean(String)}.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class ParameterTypeBoolean extends ParameterTypeSingle {

	private static final long serialVersionUID = 6524969076774489545L;

	private static final String ATTRIBUTE_DEFAULT = "default";

	private boolean defaultValue = false;

	public ParameterTypeBoolean(Element element) throws XMLException {
		super(element);

		this.defaultValue = Boolean.valueOf(element.getAttribute(ATTRIBUTE_DEFAULT));
	}

	public ParameterTypeBoolean(String key, String description, boolean defaultValue, boolean expert) {
		this(key, description, defaultValue);
		setExpert(expert);
	}

	public ParameterTypeBoolean(String key, String description, boolean defaultValue) {
		super(key, description);
		this.defaultValue = defaultValue;
	}

	public boolean getDefault() {
		return defaultValue;
	}

	@Override
	public Object getDefaultValue() {
		return Boolean.valueOf(defaultValue);
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = (Boolean) defaultValue;
	}

	/** Returns false. */
	@Override
	public boolean isNumerical() {
		return false;
	}

	@Override
	public String getRange() {
		return "boolean; default: " + defaultValue;
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
		typeElement.setAttribute(ATTRIBUTE_DEFAULT, defaultValue + "");
	}
}
