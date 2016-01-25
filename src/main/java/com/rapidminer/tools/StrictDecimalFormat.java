/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.tools;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeChar;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * @author Tobias Malbrecht
 */
public class StrictDecimalFormat extends DecimalFormat {

	private static final long serialVersionUID = -1597219600920760784L;

	public static final String PARAMETER_PARSE_NUMBERS = "parse_numbers";

	public static final String PARAMETER_DECIMAL_CHARACTER = "decimal_character";

	public static final String PARAMETER_GROUPED_DIGITS = "grouped_digits";

	public static final String PARAMETER_GROUPING_CHARACTER = "grouping_character";

	{
		setParseIntegerOnly(false);
		setGroupingSize(3);
	}

	public static final char DEFAULT_DECIMAL_CHARACTER = '.';

	public static final char DEFAULT_GROUPING_CHARACTER = ',';

	public StrictDecimalFormat() {
		super();
	}

	public StrictDecimalFormat(char decimalSeparator) {
		this();
		DecimalFormatSymbols symbols = getDecimalFormatSymbols();
		symbols.setDecimalSeparator(decimalSeparator);
		setDecimalFormatSymbols(symbols);
		setGroupingUsed(false);
	}

	public StrictDecimalFormat(char decimalSeparator, char groupingSeparator) {
		this();
		DecimalFormatSymbols symbols = getDecimalFormatSymbols();
		symbols.setDecimalSeparator(decimalSeparator);
		symbols.setGroupingSeparator(groupingSeparator);
		setDecimalFormatSymbols(symbols);
		setGroupingUsed(true);
	}

	@Override
	public Number parse(String source) throws ParseException {
		ParsePosition parsePosition = new ParsePosition(0);
		Number result = parse(source, parsePosition);

		/**
		 * throw an error if a parse error has occured somewhere in the source string, not only at
		 * the beginning as in {@link NumberFormat}
		 */
		if (parsePosition.getIndex() < source.length()) {
			throw new ParseException("Unparseable number: \"" + source + "\"", parsePosition.getIndex());
		}
		return result;
	}

	public void setDecimalSeparator(char decimalSeparator) {
		DecimalFormatSymbols symbols = getDecimalFormatSymbols();
		symbols.setDecimalSeparator(decimalSeparator);
		setDecimalFormatSymbols(symbols);
	}

	public void setGroupingSeparator(char groupingSeparator) {
		DecimalFormatSymbols symbols = getDecimalFormatSymbols();
		symbols.setGroupingSeparator(groupingSeparator);
		setDecimalFormatSymbols(symbols);
	}

	public static StrictDecimalFormat getInstance(ParameterHandler handler) throws UndefinedParameterError {
		return getInstance(handler, false);
	}

	public static StrictDecimalFormat getInstance(ParameterHandler handler, boolean optional) throws UndefinedParameterError {
		if (optional) {
			if (!handler.getParameterAsBoolean(PARAMETER_PARSE_NUMBERS)) {
				return null;
			}
		}
		char decimalCharacter = handler.getParameterAsChar(PARAMETER_DECIMAL_CHARACTER);
		char groupingCharacter = handler.getParameterAsChar(PARAMETER_GROUPING_CHARACTER);
		if (handler.getParameterAsBoolean(PARAMETER_GROUPED_DIGITS)) {
			return new StrictDecimalFormat(decimalCharacter, groupingCharacter);
		} else {
			return new StrictDecimalFormat(decimalCharacter);
		}
	}

	public static List<ParameterType> getParameterTypes(ParameterHandler handler) {
		return getParameterTypes(handler, false);
	}

	public static List<ParameterType> getParameterTypes(ParameterHandler handler, boolean optional) {
		List<ParameterType> types = new LinkedList<>();
		ParameterType type;
		if (optional) {
			type = new ParameterTypeBoolean(PARAMETER_PARSE_NUMBERS, "Specifies whether numbers are parsed.", true, false);
			types.add(type);
		}
		type = new ParameterTypeChar(PARAMETER_DECIMAL_CHARACTER, "The decimal character.", DEFAULT_DECIMAL_CHARACTER, false);
		if (optional) {
			type.registerDependencyCondition(new BooleanParameterCondition(handler, PARAMETER_PARSE_NUMBERS, false, true));
		}
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_GROUPED_DIGITS, "Parse grouped digits.", false, false);
		if (optional) {
			type.registerDependencyCondition(new BooleanParameterCondition(handler, PARAMETER_PARSE_NUMBERS, false, true));
		}
		types.add(type);
		type = new ParameterTypeChar(PARAMETER_GROUPING_CHARACTER, "The grouping character.", DEFAULT_GROUPING_CHARACTER,
				false);
		if (optional) {
			type.registerDependencyCondition(new BooleanParameterCondition(handler, PARAMETER_PARSE_NUMBERS, false, true));
		}
		type.registerDependencyCondition(new BooleanParameterCondition(handler, PARAMETER_GROUPED_DIGITS, false, true));
		types.add(type);
		return types;
	}

}
