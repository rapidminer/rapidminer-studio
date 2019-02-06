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

import com.rapidminer.MacroHandler;
import com.rapidminer.operator.ports.InputPort;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * This is the parameter type that can be used to define any number of filters which can be applied
 * on an example set to only show matching examples.
 * 
 * @author Marco Boeck
 * 
 */
public class ParameterTypeFilter extends ParameterType {

	private static final long serialVersionUID = 7719440206276258005L;

	private InputPort inPort;

	/**
	 * Creates a new {@link ParameterTypeFilter} instance.
	 * 
	 * @param key
	 * @param description
	 * @param inPort
	 * @param optional
	 */
	public ParameterTypeFilter(final String key, String description, InputPort inPort, boolean optional) {
		super(key, description);

		setOptional(optional);
		this.inPort = inPort;
	}

	/**
	 * Returns the {@link InputPort} where the data to apply the filter on is connected to.
	 * 
	 * @return
	 */
	public InputPort getInputPort() {
		return this.inPort;
	}

	@Override
	public Element getXML(String key, String value, boolean hideDefault, Document doc) {
		return null;
	}

	@Override
	public String getRange() {
		return null;
	}

	@Override
	public Object getDefaultValue() {
		return null;
	}

	@Override
	public void setDefaultValue(Object defaultValue) {}

	@Override
	public boolean isNumerical() {
		return false;
	}

	@Override
	public String getXML(String indent, String key, String value, boolean hideDefault) {
		return "";
	}

	@Override
	public String substituteMacros(String parameterValue, MacroHandler mh) {
		return parameterValue;
	}

}
