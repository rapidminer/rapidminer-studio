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
package com.rapidminer.operator.preprocessing.filter.attributes;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.I18N;


/**
 * This class implements a condition for the AttributeFilter operator. It provides the possibility
 * to check if all values of a numerical attribute match a condition. This conditions might be
 * specified by != or <>, =, <, <=, >, >= followed by a value. For example like this: "> 6.5" would
 * keep all attributes having only values greater 6.5. This single conditions might be combined by
 * || or && but not mixed. Example: "> 6.5 && < 11" would keep all attributes containing only values
 * between 6.5 and 11. Whitespaces (that do not disrupt relational operators, && or ||) will be
 * ignored, so it will make no difference if the condition is for example ">6.5&&<11" or
 * " > 6.5 && < 11 "
 *
 * @author Sebastian Land, Ingo Mierswa, Marcel Seifert
 */
public class NumericValueAttributeFilter extends AbstractAttributeFilterCondition {

	/**
	 * Condition Operators
	 */
	private static final String CONDITION_OPERATOR_NOT_EQUAL = "<>";
	private static final String CONDITION_OPERATOR_NOT_EQUAL_2 = "!=";
	private static final String CONDITION_OPERATOR_LESS_OR_EQUAL = "<=";
	private static final String CONDITION_OPERATOR_LESS = "<";
	private static final String CONDITION_OPERATOR_GREATER_OR_EQUAL = ">=";
	private static final String CONDITION_OPERATOR_GREATER = ">";
	private static final String CONDITION_OPERATOR_EQUAL = "=";

	public static String PARAMETER_NUMERIC_CONDITION = "numeric_condition";

	private static final String[] CONDITION_OPERATORS = { CONDITION_OPERATOR_NOT_EQUAL, CONDITION_OPERATOR_NOT_EQUAL_2,
			CONDITION_OPERATOR_LESS_OR_EQUAL, CONDITION_OPERATOR_LESS, CONDITION_OPERATOR_GREATER_OR_EQUAL,
			CONDITION_OPERATOR_GREATER, CONDITION_OPERATOR_EQUAL };

	private Attribute lastCheckedAttribute = null;

	private ArrayList<Condition> conditions;

	private boolean keep = true;

	private boolean conjunctiveMode;

	private static class Condition {

		private int condition;

		private double value;

		public Condition(String condition, String value) {
			this.value = Double.parseDouble(value);
			if (condition.equals(CONDITION_OPERATOR_NOT_EQUAL) || condition.equals(CONDITION_OPERATOR_NOT_EQUAL_2)) {
				this.condition = 1;
			} else if (condition.equals(CONDITION_OPERATOR_LESS_OR_EQUAL)) {
				this.condition = 2;
			} else if (condition.equals(CONDITION_OPERATOR_LESS)) {
				this.condition = 3;
			} else if (condition.equals(CONDITION_OPERATOR_GREATER_OR_EQUAL)) {
				this.condition = 4;
			} else if (condition.equals(CONDITION_OPERATOR_GREATER)) {
				this.condition = 5;
			} else if (condition.equals(CONDITION_OPERATOR_EQUAL)) {
				this.condition = 0;
			}
		}

		public boolean check(double value) {
			if (Double.isNaN(value)) {
				return true;
			}

			switch (condition) {
				case 0:
					return value == this.value;
				case 1:
					return value != this.value;
				case 2:
					return value <= this.value;
				case 3:
					return value < this.value;
				case 4:
					return value >= this.value;
				case 5:
					return value > this.value;
			}
			return false;
		}
	}

	@Override
	public void init(ParameterHandler parameterHandler) throws UserError, ConditionCreationException {
		String conditionString = parameterHandler.getParameterAsString(PARAMETER_NUMERIC_CONDITION);
		Operator operator = null;
		if (parameterHandler instanceof Operator) {
			operator = (Operator) parameterHandler;
		}

		if (conditionString == null || conditionString.length() == 0) {
			throw new UserError(operator, "cannot_parse_expression", StringEscapeUtils.escapeHtml(conditionString),
					I18N.getErrorMessage("numeric_value_filter.no_parameter_string"));
		}
		// testing if not allowed combination of and and or
		if (conditionString.contains("||") && conditionString.contains("&&")) {
			throw new UserError(operator, "cannot_parse_expression", StringEscapeUtils.escapeHtml(conditionString),
					I18N.getErrorMessage("numeric_value_filter.and_combined_with_or"));
		}

		this.conjunctiveMode = conditionString.contains("&&");

		conditions = new ArrayList<>();
		boolean conditionFound;
		for (String conditionSubString : conditionString.split("[|&]{2}")) {

			conditionFound = false;
			for (String conditionOperator : CONDITION_OPERATORS) {
				if (conditionSubString.trim().startsWith(conditionOperator)) {
					conditionFound = true;
					// Quotation needed, to prevent conditionOperator to be interpreted as a regex
					String number = conditionSubString.replaceFirst("\\Q" + conditionOperator + "\\E", "").trim();

					// check if number is valid
					try {
						Double.parseDouble(number);
						conditions.add(new Condition(conditionOperator, number));
						break;
					} catch (NullPointerException | NumberFormatException e) {
						throw new UserError(operator, "cannot_parse_expression",
								StringEscapeUtils.escapeHtml(conditionString), I18N.getErrorMessage(
										"numeric_value_filter.invalid_syntax", new Object()));
					}

				}
			}
			if (!conditionFound) {
				throw new UserError(operator, "cannot_parse_expression", StringEscapeUtils.escapeHtml(conditionString),
						I18N.getErrorMessage("numeric_value_filter.missing_relational_operator"));
			}
		}
	}

	@Override
	public MetaDataInfo isFilteredOutMetaData(AttributeMetaData attribute, ParameterHandler handler) {
		// TODO: If some infos over the value range are available: Use them to decide if possible
		return MetaDataInfo.UNKNOWN;
	}

	@Override
	public boolean isNeedingScan() {
		return true;
	}

	/**
	 * Don't remove any attribute without checking values
	 */
	@Override
	public ScanResult beforeScanCheck(Attribute attribute) throws UserError {
		return ScanResult.UNCHECKED;
	}

	@Override
	public ScanResult check(Attribute attribute, Example example) {
		if (lastCheckedAttribute != attribute) {
			keep = true;
		}
		if (attribute.isNumerical()) {
			boolean exampleResult;
			double checkValue = example.getValue(attribute);

			if (conjunctiveMode) {
				exampleResult = true;
				for (Condition condition : conditions) {
					exampleResult &= condition.check(checkValue);
				}
			} else {
				exampleResult = false;
				for (Condition condition : conditions) {
					exampleResult |= condition.check(checkValue);
				}
			}
			keep &= exampleResult;
		}
		if (!keep && attribute.isNumerical()) {
			return ScanResult.REMOVE;
		} else {
			return ScanResult.UNCHECKED;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler operator, InputPort inPort, int... valueTypes) {
		LinkedList<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeString(PARAMETER_NUMERIC_CONDITION, "Parameter string for the condition, e.g. '>= 5'",
				true, false));
		return types;
	}

}
