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

import java.util.function.Predicate;

import org.w3c.dom.Element;

import com.rapidminer.operator.Operator;
import com.rapidminer.tools.XMLException;

/**
 * This parameter type allows to select either Operator Values or Parameters for logging purposes.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class ParameterTypeValue extends ParameterTypeSingle {

	public static class OperatorValueSelection {

		private String operatorName;
		private String valueParameterName;
		private boolean isValue;

		public OperatorValueSelection() {}

		public OperatorValueSelection(String operatorName, boolean isValue, String valueOrParameterName) {
			this.operatorName = operatorName;
			this.valueParameterName = valueOrParameterName;
			this.isValue = isValue;
		}

		public String getOperator() {
			return operatorName;
		}

		public String getParameterName() {
			if (!isValue) {
				return valueParameterName;
			}
			return null;
		}

		public String getValueName() {
			if (isValue) {
				return valueParameterName;
			}
			return null;
		}

		public boolean isValue() {
			return isValue;
		}
	}

	private static final long serialVersionUID = -5863628921324775010L;

	// only one character allowed
	private static final String ESCAPE_CHAR = "\\";
	private static final String ESCAPE_CHAR_REGEX = "\\\\";
	// only one character allowed
	private static final String SEPERATOR_CHAR_REGEX = "\\.";
	private static final String SEPERATOR_CHAR = ".";

	public ParameterTypeValue(Element element) throws XMLException {
		super(element);
		setOptional(false);
	}

	public ParameterTypeValue(String key, String description) {
		super(key, description);
		setOptional(false);
	}

	/** Returns null. */
	@Override
	public Object getDefaultValue() {
		return null;
	}

	/** Does nothing. */
	@Override
	public void setDefaultValue(Object defaultValue) {}

	@Override
	public String getRange() {
		return "values";
	}

	/** Returns false. */
	@Override
	public boolean isNumerical() {
		return false;
	}

	/** @return the updated selection value if the originally associated operator was the one that was renamed */
	@Override
	public String notifyOperatorRenaming(String oldOperatorName, String newOperatorName, String parameterValue) {
		return notifyOperatorRenamingReplacing(oldOperatorName, newOperatorName, s -> true, parameterValue);
	}

	/** @return the updated selection value if the new operator still fits the value/parameter; unspecified value/parameter otherwise */
	@Override
	public String notifyOperatorReplacing(String oldName, Operator oldOp, String newName, Operator newOp, String parameterValue) {
		// only set new value if the corresponding new operator has that value/parameter; otherwise set the value/parameter to null
		Predicate<OperatorValueSelection> validation = s -> s.isValue && newOp.getValue(s.valueParameterName) != null
				|| !s.isValue && newOp.getParameters().getKeys().contains(s.valueParameterName);
		return notifyOperatorRenamingReplacing(oldName, newName, validation, parameterValue);
	}

	/** @since 9.3 */
	private String notifyOperatorRenamingReplacing(String oldName, String newName, Predicate<OperatorValueSelection> validation, String parameterValue) {
		OperatorValueSelection selection = transformString2OperatorValueSelection(parameterValue);
		if (selection != null && selection.getOperator().equals(oldName)) {
			if (!validation.test(selection)) {
				selection.valueParameterName = null;
			}
			selection.operatorName = newName;
			return transformOperatorValueSelection2String(selection);
		}
		return parameterValue;
	}

	public static OperatorValueSelection transformString2OperatorValueSelection(String parameterValue) {
		String[] unescaped = parameterValue.split("(?<=[^" + ESCAPE_CHAR_REGEX + "])" + SEPERATOR_CHAR_REGEX, -1);
		for (int i = 0; i < unescaped.length; i++) {
			unescaped[i] = unescape(unescaped[i]);
		}
		OperatorValueSelection selection = new OperatorValueSelection();
		if (unescaped.length == 4) {
			selection.operatorName = unescaped[1];
			selection.valueParameterName = unescaped[3];
			selection.isValue = unescaped[2].equals("value");
			return selection;
		}
		return null;
	}

	private static String unescape(String escapedString) {
		escapedString = escapedString.replace(ESCAPE_CHAR + SEPERATOR_CHAR, SEPERATOR_CHAR);
		escapedString = escapedString.replace(ESCAPE_CHAR + ESCAPE_CHAR, ESCAPE_CHAR);
		return escapedString;
	}

	public static String transformOperatorValueSelection2String(OperatorValueSelection selection) {
		String operator = selection.operatorName != null ? selection.getOperator().replace(ESCAPE_CHAR,
				ESCAPE_CHAR + ESCAPE_CHAR) : "";
		operator = operator.replace(SEPERATOR_CHAR, ESCAPE_CHAR + SEPERATOR_CHAR);
		String value = selection.valueParameterName != null ? selection.valueParameterName.replace(ESCAPE_CHAR, ESCAPE_CHAR
				+ ESCAPE_CHAR) : "";
		value = value.replace(SEPERATOR_CHAR, ESCAPE_CHAR + SEPERATOR_CHAR);
		return "operator" + SEPERATOR_CHAR + operator + SEPERATOR_CHAR + (selection.isValue ? "value" : "parameter")
				+ SEPERATOR_CHAR + value;
	}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {}
}
