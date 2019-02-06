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

import com.rapidminer.tools.XMLException;

import org.w3c.dom.Element;


/**
 * Helper class for GUI purposes. This parameter type should hold information about other parameter
 * values, e.g. for the definition of the parameters for a parameter optimization. Parameters of
 * this type are never optional.
 * 
 * @author Ingo Mierswa
 */
public class ParameterTypeParameterValue extends ParameterTypeSingle {

	private static final long serialVersionUID = 5248919176004016189L;

	public ParameterTypeParameterValue(Element element) throws XMLException {
		super(element);
	}

	public ParameterTypeParameterValue(String key, String description) {
		super(key, description);
		setOptional(false);
	}

	/** Returns null. */
	@Override
	public Object getDefaultValue() {
		return null;
	}

	/** Does nothing. */
	@Override
	public void setDefaultValue(Object defaultValue) {}

	@Override
	public String getRange() {
		return "parameter values";
	}

	/** Returns false. */
	@Override
	public boolean isNumerical() {
		return false;
	}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {}
}
