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
 * An abstract superclass for all numerical parameter types.
 *
 * @author Simon Fischer, Ingo Mierswa
 */
public abstract class ParameterTypeNumber extends ParameterTypeSingle {

	private static final long serialVersionUID = 1733078666760192282L;

	public ParameterTypeNumber(Element element) throws XMLException {
		super(element);
	}

	public ParameterTypeNumber(String key, String description) {
		super(key, description);
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

	public abstract double getMinValue();

	public abstract double getMaxValue();

}
