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
package com.rapidminer.tools.config;

import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeSingle;
import com.rapidminer.tools.XMLException;

import org.w3c.dom.Element;


/**
 * A {@link ParameterType} which offers a selection field for {@link Configurable}s.
 * 
 * @author Dominik Halfkann
 * 
 */
public class ParameterTypeConfigurable extends ParameterTypeSingle {

	private static final long serialVersionUID = 1047207381362696112L;
	private String typeId;

	public ParameterTypeConfigurable(Element element) throws XMLException {
		super(element);
	}

	public ParameterTypeConfigurable(String key, String description, String typeId) {
		super(key, description);
		if (!ConfigurationManager.getInstance().hasTypeId(typeId)) {
			throw new IllegalArgumentException("Unknown configurable type: " + typeId);
		}
		this.typeId = typeId;
		setExpert(false);
	}

	public String getTypeId() {
		return typeId;
	}

	@Override
	public Object getDefaultValue() {
		return null;
	}

	@Override
	public String getRange() {
		return null;
	}

	@Override
	public boolean isNumerical() {
		return false;
	}

	@Override
	public void setDefaultValue(Object defaultValue) {}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {}

}
