/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.MacroHandler;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;


/**
 * This is a parameter type that will present a list of values. This would have been made a better
 * list than {@link ParameterTypeList} itself, which is more a mapping list with a pair of values.
 * This one only has one inner type.
 *
 * @author Sebastian Land
 */
public class ParameterTypeEnumeration extends CombinedParameterType {

	private static final long serialVersionUID = -3677952200700007724L;

	private static final String ELEMENT_CHILD_TYPE = "ChildType";
	private static final String ELEMENT_DEFAULT_VALUE = "Default";

	private static final char ESCAPE_CHAR = '\\';
	private static final char SEPERATOR_CHAR = ','; // Parameters.RECORD_SEPARATOR; //
	private static final char[] SPECIAL_CHARACTERS = new char[] { SEPERATOR_CHAR };

	private Object defaultValue;

	private ParameterType type;

	public ParameterTypeEnumeration(Element element) throws XMLException {
		super(element);

		type = ParameterType.createType(XMLTools.getChildElement(element, ELEMENT_CHILD_TYPE, true));
		Element defaultValueElement = XMLTools.getChildElement(element, ELEMENT_DEFAULT_VALUE, false);
		if (defaultValueElement != null) {
			defaultValue = defaultValueElement.getTextContent();
		}
	}

	public ParameterTypeEnumeration(String key, String description, ParameterType parameterType) {
		super(key, description, parameterType);
		this.type = parameterType;
	}

	public ParameterTypeEnumeration(String key, String description, ParameterType parameterType, boolean expert) {
		this(key, description, parameterType);
		setExpert(expert);
	}

	@Override
	public Element getXML(String key, String value, boolean hideDefault, Document doc) {
		Element element = doc.createElement("enumeration");
		element.setAttribute("key", key);
		String[] list = null;
		if (value != null) {
			list = transformString2Enumeration(value);
		} else {
			list = transformString2Enumeration(getDefaultValueAsString());
		}
		if (list != null) {
			for (String string : list) {
				element.appendChild(type.getXML(type.getKey(), string, false, doc));
			}
		}
		return element;
	}

	@Override
	@Deprecated
	public String getXML(String indent, String key, String value, boolean hideDefault) {
		return "";
	}

	@Override
	public boolean isNumerical() {
		return false;
	}

	@Override
	public String getRange() {
		return "enumeration";
	}

	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}

	@Override
	public String getDefaultValueAsString() {
		if (defaultValue == null) {
			return null;
		}
		return getValueType().toString(defaultValue);
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public ParameterType getValueType() {
		return type;
	}

	@Override
	public String notifyOperatorRenaming(String oldOperatorName, String newOperatorName, String parameterValue) {
		String[] enumeratedValues = transformString2Enumeration(parameterValue);
		for (int i = 0; i < enumeratedValues.length; i++) {
			enumeratedValues[i] = type.notifyOperatorRenaming(oldOperatorName, newOperatorName, enumeratedValues[i]);
		}
		return transformEnumeration2String(Arrays.asList(enumeratedValues));

	}

	public static String transformEnumeration2String(List<String> list) {
		StringBuilder builder = new StringBuilder();
		boolean isFirst = true;
		for (String string : list) {
			if (!isFirst) {
				builder.append(SEPERATOR_CHAR);
			}
			if (string != null) {
				builder.append(Tools.escape(string, ESCAPE_CHAR, SPECIAL_CHARACTERS));
			}
			isFirst = false;
		}
		return builder.toString();
	}

	public static String[] transformString2Enumeration(String parameterValue) {
		if (parameterValue == null || "".equals(parameterValue)) {
			return new String[0];
		}
		List<String> split = Tools.unescape(parameterValue, ESCAPE_CHAR, SPECIAL_CHARACTERS, SEPERATOR_CHAR);
		return split.toArray(new String[split.size()]);
	}

	@Override
	public String substituteMacros(String parameterValue, MacroHandler mh) throws UndefinedParameterError {
		if (parameterValue.indexOf("%{") == -1) {
			return parameterValue;
		}
		String[] list = transformString2Enumeration(parameterValue);
		String[] result = new String[list.length];
		for (int i = 0; i < list.length; i++) {
			result[i] = getValueType().substituteMacros(list[i], mh);
		}
		return transformEnumeration2String(Arrays.asList(result));
	}

	@Override
	public String substitutePredefinedMacros(String parameterValue, Operator operator) throws UndefinedParameterError {
		if (parameterValue.indexOf("%{") == -1) {
			return parameterValue;
		}
		String[] list = transformString2Enumeration(parameterValue);
		String[] result = new String[list.length];
		for (int i = 0; i < list.length; i++) {
			result[i] = getValueType().substitutePredefinedMacros(list[i], operator);
		}
		return transformEnumeration2String(Arrays.asList(result));
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code true} if the inner parameter is sensitive; {@code false} otherwise
	 */
	@Override
	public boolean isSensitive() {
		return getValueType() != null ? getValueType().isSensitive() : false;
	}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {
		Element childTypeElement = XMLTools.addTag(typeElement, ELEMENT_CHILD_TYPE);
		type.writeDefinitionToXML(childTypeElement);

		if (defaultValue instanceof String) {
			XMLTools.addTag(typeElement, ELEMENT_DEFAULT_VALUE, defaultValue.toString());
		}

	}
}
