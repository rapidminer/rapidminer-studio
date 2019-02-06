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
package com.rapidminer.tools.parameter;

import com.rapidminer.parameter.ParameterType;


/**
 * Value and Definition of a Parameter. Two types of parameters can be defined: Implicit ones and
 * Defined ones. The defined one have a type definition, while the implicit ones only have a key and
 * value. Only defined parameters can be edited using the GUI and will be saved in the config files.
 *
 * @author Sebastian Land
 */
public class Parameter {

	ParameterType type = null;
	String group;
	String value;
	private ParameterScope scope = new ParameterScope();

	/**
	 * This creates a new implicit Parameter.
	 */
	public Parameter(String value) {

	}

	/**
	 * This creates a new defined Parameter with a default scope and an undefined value.
	 *
	 * The group of the Parameter is set automatically by the second segment of the dot separated
	 * key. For setting the group explicitly, please use {@link #Parameter(ParameterType, String)}.
	 *
	 * @param type
	 */
	public Parameter(ParameterType type) {
		this.type = type;
		this.value = type.getDefaultValueAsString();
		if (value == null) {
			value = "";
		}

		String[] parts = type.getKey().split("\\.");
		if ("rapidminer".equals(parts[0])) {
			this.group = parts[1];
		} else {
			this.group = "system";
		}
	}

	public Parameter(ParameterType type, String group) {
		this.type = type;
		this.group = group;
		this.value = type.getDefaultValueAsString();
		if (value == null) {
			value = "";
		}

	}

	/**
	 * Sets the value of this parameter. If the given value is null, it is automatically converted
	 * to an empty String.
	 */
	public void setValue(String value) {
		if (value == null) {
			throw new NullPointerException();
		} else {
			this.value = value;
		}
	}

	/**
	 * This method returns whether this parameter is a defined type. With a description, valid value
	 * range and name. Only defined types can be edited in the GUI, all others can only be set
	 * directly using system properties.
	 */
	public boolean isDefined() {
		return type != null;
	}

	/**
	 * This returns the scope of this parameter if it is a defined parameter.
	 */
	public ParameterScope getScope() {
		return scope;
	}

	/**
	 * Returns the actual value or an empty String if no values has been set.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * This returns the group of this parameter.
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * Returns the type of this parameter.
	 */
	public ParameterType getType() {
		return type;
	}

	/**
	 * This converts this parameter from an implicit Parameter into a defined one.
	 */
	public void setType(ParameterType type) {
		this.type = type;
		String[] parts = type.getKey().split("\\.");
		if ("rapidminer".equals(parts[0])) {
			this.group = parts[1];
		} else {
			this.group = "system";
		}
	}

	/**
	 * This sets the scope to the given one.
	 */
	public void setScope(ParameterScope scope) {
		this.scope = scope;
	}

	/**
	 * This method allows to set the group explicitly.
	 */
	public void setGroup(String group) {
		this.group = group;
	}
}
