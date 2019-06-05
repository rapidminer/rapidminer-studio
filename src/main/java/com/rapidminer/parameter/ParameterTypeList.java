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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
 * A parameter type for parameter lists. Operators ask for the list of the specified values with
 * {@link com.rapidminer.operator.Operator#getParameterList(String)}. Please note that in principle
 * arbitrary parameter types can be used for the list values. Internally, however, all values are
 * transformed to strings. Therefore, operators retrieving values from non-string lists (for example
 * for a parameter type category) have to transform the values themself, e.g. with the following
 * code:<br/>
 * <br/>
 *
 * <code>int index = ((ParameterTypeCategory)((ParameterTypeList)getParameters().getParameterType(PARAMETER_LIST)).getValueType()).getIndex(pair[1]);</code>
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class ParameterTypeList extends CombinedParameterType {

	private static final long serialVersionUID = -6101604413822993455L;

	private static final String ELEMENT_KEY_TYPE = "KeyType";

	private static final String ELEMENT_VALUE_TYPE = "ValueType";
	private static final String ELEMENT_DEFAULT_ENTRIES = "DefaultEntries";
	private static final String ELEMENT_ENTRY = "Entry";
	private static final String ATTRIBUTE_KEY = "key";
	private static final String ATTRIBUTE_VALUE = "value";

	private List<String[]> defaultList = new LinkedList<>();

	private final ParameterType valueType;
	private final ParameterType keyType;

	public ParameterTypeList(Element element) throws XMLException {
		super(element);

		valueType = ParameterType.createType(XMLTools.getChildElement(element, ELEMENT_VALUE_TYPE, true));
		keyType = ParameterType.createType(XMLTools.getChildElement(element, ELEMENT_KEY_TYPE, true));

		// now default values
		Element defaultEntriesElement = XMLTools.getChildElement(element, ELEMENT_DEFAULT_ENTRIES, true);
		for (Element entryElement : XMLTools.getChildElements(defaultEntriesElement, ELEMENT_ENTRY)) {
			defaultList.add(new String[] { entryElement.getAttribute(ATTRIBUTE_KEY),
					entryElement.getAttribute(ATTRIBUTE_VALUE) });
		}
	}

	@Deprecated
	/**
	 * This constructor is deprecated, because it does not provide enough information for user guidance
	 */
	public ParameterTypeList(String key, String description, ParameterType valueType) {
		this(key, description, valueType, new LinkedList<String[]>());
	}

	@Deprecated
	/**
	 * This constructor is deprecated, because it does not provide enough information for user guidance
	 */
	public ParameterTypeList(String key, String description, ParameterType valueType, List<String[]> defaultList) {
		super(key, description);
		this.defaultList = defaultList;
		this.valueType = valueType;
		this.keyType = new ParameterTypeString(key, description);
		if (valueType.getDescription() == null) {
			valueType.setDescription(description);
		}
	}

	public ParameterTypeList(String key, String description, ParameterType keyType, ParameterType valueType, boolean expert) {
		this(key, description, keyType, valueType, new LinkedList<String[]>());
		setExpert(expert);
	}

	public ParameterTypeList(String key, String description, ParameterType keyType, ParameterType valueType) {
		this(key, description, keyType, valueType, new LinkedList<String[]>());
	}

	public ParameterTypeList(String key, String description, ParameterType keyType, ParameterType valueType,
			List<String[]> defaultList, boolean expert) {
		this(key, description, keyType, valueType, defaultList);
		setExpert(expert);
	}

	public ParameterTypeList(String key, String description, ParameterType keyType, ParameterType valueType,
			List<String[]> defaultList) {
		super(key, description, keyType, valueType);
		this.defaultList = defaultList;
		this.valueType = valueType;
		this.keyType = keyType;
	}

	public ParameterType getValueType() {
		return valueType;
	}

	public ParameterType getKeyType() {
		return keyType;
	}

	@Override
	public List<String[]> getDefaultValue() {
		return defaultList;
	}

	@SuppressWarnings("unchecked")
	@Override
	// TODO: Introduce Typing??
	public void setDefaultValue(Object defaultValue) {
		this.defaultList = (List<String[]>) defaultValue;
	}

	/** Returns false. */
	@Override
	public boolean isNumerical() {
		return false;
	}

	@Override
	public Element getXML(String key, String value, boolean hideDefault, Document doc) {
		Element element = doc.createElement("list");
		element.setAttribute("key", key);
		List<String[]> list = null;
		if (value != null) {
			list = transformString2List(value);
		} else {
			list = getDefaultValue();
		}
		if (list != null) {
			for (Object object : list) {
				Object[] entry = (Object[]) object;
				element.appendChild(valueType.getXML((String) entry[0], entry[1].toString(), false, doc));
			}
		}
		return element;
	}

	/** @deprecated Replaced by DOM. */
	@Override
	@Deprecated
	public String getXML(String indent, String key, String value, boolean hideDefault) {
		StringBuffer result = new StringBuffer();
		result.append(indent + "<list key=\"" + key + "\">" + Tools.getLineSeparator());

		if (value != null) {
			List<String[]> list = Parameters.transformString2List(value);
			Iterator<String[]> i = list.iterator();
			while (i.hasNext()) {
				Object[] current = i.next();
				result.append(valueType.getXML(indent + "  ", (String) current[0], current[1].toString(), false));
			}
		} else {
			List<String[]> defaultValue = getDefaultValue();
			if (defaultValue != null) {
				List<String[]> defaultList = defaultValue;
				Iterator<String[]> i = defaultList.iterator();
				while (i.hasNext()) {
					Object[] current = i.next();
					result.append(valueType.getXML(indent + "  ", (String) current[0], current[1].toString(), false));
				}
			}
		}
		result.append(indent + "</list>" + Tools.getLineSeparator());
		return result.toString();
	}

	@Override
	public String getRange() {
		return "list";
	}

	@SuppressWarnings("unchecked")
	@Override
	public String toString(Object value) {
		if (value instanceof String) {
			return transformList2String(transformString2List(value.toString()));
		} else {
			return transformList2String((List<String[]>) value);
		}
	}

	public static String transformList2String(List<String[]> parameterList) {
		return parameterList.stream().map(vals -> Arrays.stream(vals)
				.collect(Collectors.joining(Character.toString(Parameters.PAIR_SEPARATOR))))
				.collect(Collectors.joining(Character.toString(Parameters.RECORD_SEPARATOR)));
	}

	public static List<String[]> transformString2List(String listString) {
		return Arrays.stream(listString.split(Character.toString(Parameters.RECORD_SEPARATOR)))
				.filter(record -> record.length() > 0)
				.map(record -> record.split(Character.toString(Parameters.PAIR_SEPARATOR)))
				.filter(pair -> pair.length == 2 && !pair[0].isEmpty() && !pair[1].isEmpty())
				.collect(Collectors.toList());
	}

	/** @return the changed value after all pairs were notified */
	@Override
	public String notifyOperatorRenaming(String oldOperatorName, String newOperatorName, String parameterValue) {
		return notifyOperatorRenamingReplacing((t, v) -> t.notifyOperatorRenaming(oldOperatorName, newOperatorName, v), parameterValue);
	}

	/** @return the changed value after all pairs were notified */
	@Override
	public String notifyOperatorReplacing(String oldName, Operator oldOp, String newName, Operator newOp, String parameterValue) {
		return notifyOperatorRenamingReplacing((t, v) -> t.notifyOperatorReplacing(oldName, oldOp, newName, newOp, v), parameterValue);
	}

	/** @since 9.3 */
	private String notifyOperatorRenamingReplacing(BiFunction<ParameterType, String, String> replacer, String parameterValue) {
		List<String[]> list = transformString2List(parameterValue);
		for (String[] pair : list) {
			pair[0] = replacer.apply(keyType, pair[0]);
			pair[1] = replacer.apply(valueType, pair[1]);
		}
		return transformList2String(list);
	}

	@Override
	public String substituteMacros(String parameterValue, MacroHandler mh) throws UndefinedParameterError {
		if (parameterValue.indexOf("%{") == -1) {
			return parameterValue;
		}
		List<String[]> list = transformString2List(parameterValue);
		List<String[]> result = new LinkedList<>();
		for (String[] entry : list) {
			result.add(new String[] { getKeyType().substituteMacros(entry[0], mh),
					getValueType().substituteMacros(entry[1], mh) });
		}
		return transformList2String(result);
	}

	@Override
	public String substitutePredefinedMacros(String parameterValue, Operator operator) throws UndefinedParameterError {
		if (parameterValue.indexOf("%{") == -1) {
			return parameterValue;
		}
		List<String[]> list = transformString2List(parameterValue);
		List<String[]> result = new LinkedList<>();
		for (String[] entry : list) {
			result.add(new String[] { getKeyType().substitutePredefinedMacros(entry[0], operator),
					getValueType().substitutePredefinedMacros(entry[1], operator) });
		}
		return transformList2String(result);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code true} if either the key parameter or the value parameter or both are
	 *         sensitive; {@code false} otherwise
	 */
	@Override
	public boolean isSensitive() {
		boolean keySensitive = getKeyType() != null ? getKeyType().isSensitive() : false;
		boolean valueSensitive = getValueType() != null ? getValueType().isSensitive() : false;
		return keySensitive || valueSensitive;
	}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {
		Element keyTypeElement = XMLTools.addTag(typeElement, ELEMENT_KEY_TYPE);
		keyType.writeDefinitionToXML(keyTypeElement);

		Element valueTypeElement = XMLTools.addTag(typeElement, ELEMENT_VALUE_TYPE);
		valueType.writeDefinitionToXML(valueTypeElement);

		// now default list
		Element defaultEntriesElement = XMLTools.addTag(typeElement, ELEMENT_DEFAULT_ENTRIES);
		for (String[] pair : defaultList) {
			Element defaultEntryElement = XMLTools.addTag(defaultEntriesElement, ELEMENT_ENTRY);
			defaultEntryElement.setAttribute(ATTRIBUTE_KEY, pair[0]);
			defaultEntryElement.setAttribute(ATTRIBUTE_VALUE, pair[1]);
		}
	}

}
