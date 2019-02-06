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
 * This is an abstract class for all ParameterTypes that are a combination of several other
 * {@link ParameterType}s. In fact it doesn't do anything...
 * 
 * @author Sebastian Land
 * 
 */
public abstract class CombinedParameterType extends ParameterType {

	private static final long serialVersionUID = 1674072082952288334L;

	public CombinedParameterType(String key, String description, ParameterType... types) {
		super(key, description);
	}

	public CombinedParameterType(Element element) throws XMLException {
		super(element);
	}
}
