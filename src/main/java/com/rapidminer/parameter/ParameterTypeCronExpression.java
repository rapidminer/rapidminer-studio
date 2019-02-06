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
 * @author Nils Woehler
 * 
 */
public class ParameterTypeCronExpression extends ParameterTypeString {

	private static final long serialVersionUID = 1L;

	public ParameterTypeCronExpression(Element element) throws XMLException {
		super(element);
	}

	public ParameterTypeCronExpression(String key, String description, boolean optional, boolean expert) {
		super(key, description, optional, expert);
	}

	public ParameterTypeCronExpression(String key, String description, boolean optional) {
		super(key, description, optional);
	}

	public ParameterTypeCronExpression(String key, String description) {
		super(key, description, true);
	}

	public ParameterTypeCronExpression(String key, String description, String defaultValue, boolean expert) {
		super(key, description, defaultValue, expert);
	}

	public ParameterTypeCronExpression(String key, String description, String defaultValue) {
		super(key, description, defaultValue);
	}

}
