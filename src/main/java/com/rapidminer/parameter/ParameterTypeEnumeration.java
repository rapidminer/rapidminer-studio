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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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
		String[] list;
		if (value != null) {
			list = transformString2Enumeration(value);
		} else {
			list = transformString2Enumeration(getDefaultValueAsString());
		}
		for (String string : list) {
			element.appendChild(type.getXML(type.getKey(), string, false, doc));
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

	/** @return the changed value after all entries were notified */
	@Override
	public String notifyOperatorRenaming(String oldOperatorName, String newOperatorName, String parameterValue) {
		return notifyOperatorRenamingReplacing((t, v) -> t.notifyOperatorRenaming(oldOperatorName, newOperatorName, v), parameterValue);
	}

	/** @return the changed value after all entries were notified */
	@Override
	public String notifyOperatorReplacing(String oldName, Operator oldOp, String newName, Operator newOp, String parameterValue) {
		return notifyOperatorRenamingReplacing((t, v) -> t.notifyOperatorReplacing(oldName, oldOp, newName, newOp, v), parameterValue);
	}

	/** @since 9.3 */
	private String notifyOperatorRenamingReplacing(BiFunction<ParameterType, String, String> replacer, String parameterValue) {
		return transformEnumeration2String(Arrays.stream(transformString2Enumeration(parameterValue))
				.map(v -> replacer.apply(type, v))
				.collect(Collectors.toList()));
	}

	/**
	 * Transforms the given list into an enumeration parameter string. Will escape all occurrences of
	 * {@link #SEPERATOR_CHAR the separator char} and join them with that same separator.
	 *
	 * @param list
	 * 		the list of individual parameter values
	 * @return the enumeration parameter string
	 */
	public static String transformEnumeration2String(List<String> list) {
		return list.stream().filter(Objects::nonNull).map(string -> Tools.escape(string, ESCAPE_CHAR, SPECIAL_CHARACTERS))
				.collect(Collectors.joining(Character.toString(SEPERATOR_CHAR)));
	}

	/**
	 * Transforms a parameter value into a list of strings representing each entry.
	 * Inverse method to {@link #transformEnumeration2String(List)}.
	 * @since 9.3
	 */
	public static List<String> transformString2List(String parameterValue) {
		if (parameterValue == null || "".equals(parameterValue)) {
			return Collections.emptyList();
		}
		return Tools.unescape(parameterValue, ESCAPE_CHAR, SPECIAL_CHARACTERS, SEPERATOR_CHAR);
	}

	/**  Same as {@link #transformString2List(String)}, but returns an array representation */
	public static String[] transformString2Enumeration(String parameterValue) {
		return transformString2List(parameterValue).toArray(new String[0]);
	}

	@Override
	public String substituteMacros(String parameterValue, MacroHandler mh) throws UndefinedParameterError {
		if (!parameterValue.contains("%{")) {
			return parameterValue;
		}
		List<String> list = transformString2List(parameterValue);
		ParameterType valueType = getValueType();
		for (int i = 0; i < list.size(); i++) {
			String value = list.get(i);
			list.set(i, valueType.substituteMacros(value, mh));
		}
		return transformEnumeration2String(list);
	}

	@Override
	public String substitutePredefinedMacros(String parameterValue, Operator operator) throws UndefinedParameterError {
		if (!parameterValue.contains("%{")) {
			return parameterValue;
		}
		List<String> list = transformString2List(parameterValue);
		ParameterType valueType = getValueType();
		for (int i = 0; i < list.size(); i++) {
			String value = list.get(i);
			list.set(i, valueType.substitutePredefinedMacros(value, operator));
		}
		return transformEnumeration2String(list);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code true} if the inner parameter is sensitive; {@code false} otherwise
	 */
	@Override
	public boolean isSensitive() {
		return getValueType() != null && getValueType().isSensitive();
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
