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

import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.XMLException;


/**
 * A parameter type for categories. These are several Strings and one of these is the default value.
 * Additionally users can define other strings than these given in as pre-defined categories.
 * Operators ask for the defined String with the method
 * {@link com.rapidminer.operator.Operator#getParameterAsString(String)}.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class ParameterTypeStringCategory extends ParameterTypeSingle {

	private static final long serialVersionUID = 1620216625117563601L;

	protected static final String ELEMENT_DEFAULT = "default";

	protected static final String ELEMENT_VALUES = "Values";

	protected static final String ELEMENT_VALUE = "Value";

	protected static final String ATTRIBUTE_IS_EDITABLE = "is-editable";

	private String defaultValue = null;

	private String[] categories = new String[0];

	private boolean editable = true;

	public ParameterTypeStringCategory(Element element) throws XMLException {
		super(element);

		editable = Boolean.valueOf(element.getAttribute(ATTRIBUTE_IS_EDITABLE));
		defaultValue = XMLTools.getTagContents(element, ELEMENT_DEFAULT);
		if (defaultValue == null) {
			setOptional(false);
		}
		categories = XMLTools.getChildTagsContentAsStringArray(XMLTools.getChildElement(element, ELEMENT_VALUES, true),
				ELEMENT_VALUE);
	}

	public ParameterTypeStringCategory(String key, String description, String[] categories) {
		this(key, description, categories, null);
	}

	public ParameterTypeStringCategory(String key, String description, String[] categories, String defaultValue) {
		this(key, description, categories, defaultValue, true);
	}

	public ParameterTypeStringCategory(String key, String description, String[] categories, String defaultValue,
			boolean editable) {
		super(key, description);
		this.categories = categories;
		this.defaultValue = defaultValue;
		this.editable = editable;
		setOptional(defaultValue != null);
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public boolean isEditable() {
		return editable;
	}

	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = (String) defaultValue;
	}

	@Override
	public String toString(Object value) {
		return (String) value;
	}

	public String[] getValues() {
		return categories;
	}

	/** Returns false. */
	@Override
	public boolean isNumerical() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code false} if {@link #isEditable()} returns {@code false}; otherwise {@code true}
	 */
	@Override
	public boolean isSensitive() {
		return isEditable();
	}

	@Override
	public String getRange() {
		StringBuffer values = new StringBuffer();
		for (int i = 0; i < categories.length; i++) {
			if (i > 0) {
				values.append(", ");
			}
			values.append(categories[i]);
		}
		values.append(defaultValue != null ? "; default: '" + defaultValue + "'" : "");
		return values.toString();
	}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {
		typeElement.setAttribute(ATTRIBUTE_IS_EDITABLE, editable + "");
		if (defaultValue != null) {
			XMLTools.addTag(typeElement, ELEMENT_DEFAULT, defaultValue + "");
		}

		Element valuesElement = XMLTools.addTag(typeElement, ELEMENT_VALUES);
		for (String category : categories) {
			XMLTools.addTag(valuesElement, ELEMENT_VALUE, category);
		}
	}
}
