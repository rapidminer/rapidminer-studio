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

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.MacroHandler;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.container.Pair;


/**
 * This is a parameter type that contains a number of subtypes. But unlike the
 * {@link ParameterTypeList} or {@link ParameterTypeEnumeration}, these subtypes are treated as one
 * value. In the gui these subtypes are shown beside each other.
 *
 * @author Sebastian Land
 */
public class ParameterTypeTupel extends CombinedParameterType {

	private static final long serialVersionUID = 7292052301201204321L;

	private static final String ELEMENT_CHILD_TYPE = "ChildType";
	private static final String ELEMENT_CHILD_TYPES = "ChildTypes";
	private static final String ELEMENT_DEFAULT_ENTRIES = "DefaultEntries";
	private static final String ELEMENT_DEFAULT_ENTRY = "Entry";
	private static final String ATTRIBUTE_IS_NULL = "is-null";

	// // only one character allowed
	// private static final String ESCAPE_CHAR = "\\";
	// private static final String ESCAPE_CHAR_REGEX = "\\\\";
	// // only one character allowed
	// private static final String SEPERATOR_CHAR_REGEX = "\\.";
	private static final char ESCAPE_CHAR = '\\';
	private static final char XML_SEPERATOR_CHAR = '.';
	private static final char[] XML_SPECIAL_CHARACTERS = new char[] { XML_SEPERATOR_CHAR };
	private static final char INTERNAL_SEPERATOR_CHAR = '.'; // Parameters.PAIR_SEPARATOR; //'.';
	private static final char[] INTERNAL_SPECIAL_CHARACTERS = new char[] { INTERNAL_SEPERATOR_CHAR };

	private Object[] defaultValues = null;

	private ParameterType[] types;

	public ParameterTypeTupel(Element element) throws XMLException {
		super(element);

		Element childTypesElement = XMLTools.getChildElement(element, ELEMENT_CHILD_TYPES, true);
		Collection<Element> childTypeElements = XMLTools.getChildElements(childTypesElement, ELEMENT_CHILD_TYPE);
		types = new ParameterType[childTypeElements.size()];
		int i = 0;
		for (Element childTypeElement : childTypeElements) {
			types[i] = ParameterType.createType(childTypeElement);
			i++;
		}

		// now default values
		Element defaultEntriesElement = XMLTools.getChildElement(element, ELEMENT_DEFAULT_ENTRIES, true);

		Collection<Element> defaultEntryElements = XMLTools.getChildElements(defaultEntriesElement, ELEMENT_DEFAULT_ENTRY);
		defaultValues = new Object[defaultEntryElements.size()];
		i = 0;
		for (Element entryElement : defaultEntryElements) {
			if (entryElement.hasAttribute(ATTRIBUTE_IS_NULL)
					&& Boolean.valueOf(entryElement.getAttribute(ATTRIBUTE_IS_NULL))) {
				defaultValues[i] = null;
			} else {
				defaultValues[i] = entryElement.getTextContent();
			}
			i++;
		}
	}

	public ParameterTypeTupel(String key, String description, ParameterType... parameterTypes) {
		super(key, description, parameterTypes);
		this.types = parameterTypes;
	}

	@Override
	public Object getDefaultValue() {
		if (defaultValues == null) {
			String[] defaultValues = new String[types.length];
			for (int i = 0; i < types.length; i++) {
				defaultValues[i] = types[i].getDefaultValue() == null ? "" : types[i].getDefaultValue() + "";
			}
			return ParameterTypeTupel.transformTupel2String(defaultValues);
		} else {
			String[] defStrings = new String[defaultValues.length];
			for (int i = 0; i < defaultValues.length; i++) {
				defStrings[i] = defaultValues[i] + "";
			}
			return ParameterTypeTupel.transformTupel2String(defStrings);
		}
	}

	@Override
	public String getDefaultValueAsString() {
		String[] defaultStrings = new String[types.length];
		for (int i = 0; i < types.length; i++) {
			Object defaultValue = types[i].getDefaultValue();
			// if (defaultValue == null)
			// return null;
			defaultStrings[i] = types[i].toString(defaultValue);
		}
		return ParameterTypeTupel.transformTupel2String(defaultStrings);
	}

	@Override
	public String getRange() {
		return "tupel";
	}

	@Override
	public Element getXML(String key, String value, boolean hideDefault, Document doc) {
		Element element = doc.createElement("parameter");
		element.setAttribute("key", key);
		String[] tupel;
		if (value == null) {
			tupel = transformString2Tupel((String) getDefaultValue());
		} else {
			tupel = transformString2Tupel(value);
		}
		StringBuilder valueString = new StringBuilder();
		boolean first = true;
		for (String part : tupel) {
			if (!first) {
				valueString.append(XML_SEPERATOR_CHAR);
			} else {
				first = false;
			}
			if (part == null) {
				part = "";
			}
			valueString.append(Tools.escape(part, ESCAPE_CHAR, XML_SPECIAL_CHARACTERS));
		}
		element.setAttribute("value", valueString.toString());
		return element;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getXML(String indent, String key, String value, boolean hideDefault) {
		StringBuffer result = new StringBuffer();
		String valueString = value;
		if (value == null) {
			valueString = transformTupel2String((Pair<String, String>) getDefaultValue());
		}

		result.append(indent + "<parameter key=\"" + key + "\" value=\"" + valueString + "\" />" + Tools.getLineSeparator());
		return result.toString();
	}

	@Override
	public boolean isNumerical() {
		return false;
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		this.defaultValues = (Object[]) defaultValue;
	}

	public ParameterType getFirstParameterType() {
		return types[0];
	}

	public ParameterType getSecondParameterType() {
		return types[1];
	}

	public ParameterType[] getParameterTypes() {
		return types;
	}

	public static String[] transformString2Tupel(String parameterValue) {
		if (parameterValue == null || parameterValue.isEmpty()) {
			return new String[2];
		}
		List<String> split = Tools.unescape(parameterValue, ESCAPE_CHAR, INTERNAL_SPECIAL_CHARACTERS,
				INTERNAL_SEPERATOR_CHAR);
		while (split.size() < 2) {
			split.add(null);
		}
		return split.toArray(new String[0]);
	}

	public static String transformTupel2String(String firstValue, String secondValue) {
		return Tools.escape(firstValue, ESCAPE_CHAR, INTERNAL_SPECIAL_CHARACTERS) + INTERNAL_SEPERATOR_CHAR
				+ Tools.escape(secondValue, ESCAPE_CHAR, INTERNAL_SPECIAL_CHARACTERS);
	}

	public static String transformTupel2String(Pair<String, String> pair) {
		return transformTupel2String(pair.getFirst(), pair.getSecond());
	}

	public static String transformTupel2String(String[] tupel) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < tupel.length; i++) {
			if (i > 0) {
				builder.append(INTERNAL_SEPERATOR_CHAR);
			}
			if (tupel[i] != null) {
				builder.append(Tools.escape(tupel[i], ESCAPE_CHAR, INTERNAL_SPECIAL_CHARACTERS));
			}
		}
		return builder.toString();
	}

	/** @return the changed value after all tupel entries were notified */
	@Override
	public String notifyOperatorRenaming(String oldOperatorName, String newOperatorName, String parameterValue) {
		return notifyOperatorRenamingReplacing((t, v) -> t.notifyOperatorRenaming(oldOperatorName, newOperatorName, v), parameterValue);
	}

	/** @return the changed value after all tupel entries were notified */
	@Override
	public String notifyOperatorReplacing(String oldName, Operator oldOp, String newName, Operator newOp, String parameterValue) {
		return notifyOperatorRenamingReplacing((t, v) -> t.notifyOperatorReplacing(oldName, oldOp, newName, newOp, v), parameterValue);
	}

	/** @since 9.3 */
	private String notifyOperatorRenamingReplacing(BiFunction<ParameterType, String, String> replacer, String parameterValue) {
		String[] tupel = transformString2Tupel(parameterValue);
		for (int i = 0; i < types.length; i++) {
			tupel[i] = replacer.apply(types[i], tupel[i]);
		}
		return transformTupel2String(tupel);
	}

	@Override
	public String transformNewValue(String value) {
		List<String> split = Tools.unescape(value, ESCAPE_CHAR, XML_SPECIAL_CHARACTERS, XML_SEPERATOR_CHAR);
		StringBuilder internalEncoded = new StringBuilder();
		boolean first = true;
		for (String part : split) {
			if (!first) {
				internalEncoded.append(INTERNAL_SEPERATOR_CHAR);
			} else {
				first = false;
			}
			internalEncoded.append(Tools.escape(part, ESCAPE_CHAR, INTERNAL_SPECIAL_CHARACTERS));
		}
		return internalEncoded.toString();
	}

	public static String escapeForInternalRepresentation(String string) {
		return Tools.escape(string, ESCAPE_CHAR, INTERNAL_SPECIAL_CHARACTERS);
	}

	@Override
	public String substituteMacros(String parameterValue, MacroHandler mh) throws UndefinedParameterError {
		if (parameterValue.indexOf("%{") == -1) {
			return parameterValue;
		}
		String[] tupel = transformString2Tupel(parameterValue);
		String[] result = new String[tupel.length];
		for (int i = 0; i < tupel.length; i++) {
			result[i] = types[i].substituteMacros(tupel[i], mh);
		}
		return transformTupel2String(result);
	}

	@Override
	public String substitutePredefinedMacros(String parameterValue, Operator operator) throws UndefinedParameterError {
		if (parameterValue.indexOf("%{") == -1) {
			return parameterValue;
		}
		String[] tuple = transformString2Tupel(parameterValue);
		String[] result = new String[tuple.length];
		for (int i = 0; i < tuple.length; i++) {
			result[i] = types[i].substitutePredefinedMacros(tuple[i], operator);
		}
		return transformTupel2String(result);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code true} if any of the parameter types are sensitive; {@code false} otherwise
	 */
	@Override
	public boolean isSensitive() {
		for (ParameterType type : getParameterTypes()) {
			if (type.isSensitive()) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {

		Element childTypesElement = XMLTools.addTag(typeElement, ELEMENT_CHILD_TYPES);
		for (ParameterType type : types) {
			Element childTypeElement = XMLTools.addTag(childTypesElement, ELEMENT_CHILD_TYPE);
			type.writeDefinitionToXML(childTypeElement);
		}

		// now default list
		Element defaultEntriesElement = XMLTools.addTag(typeElement, ELEMENT_DEFAULT_ENTRIES);
		for (Object defaultValue : defaultValues) {
			Element defaultEntryElement = XMLTools.addTag(defaultEntriesElement, ELEMENT_DEFAULT_ENTRY);
			if (defaultValue != null && defaultValue instanceof String) {
				defaultEntryElement.setTextContent(defaultValue.toString());
			} else {
				defaultEntriesElement.setAttribute(ATTRIBUTE_IS_NULL, "true");
			}
		}
	}
}
