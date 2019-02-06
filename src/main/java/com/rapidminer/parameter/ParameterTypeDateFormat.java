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

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;


/**
 * A ParameterType for DateFormats. Also holds constants for date(/time) formats and static methods to create checked
 * {@link SimpleDateFormat SimpleDateFormats}.
 *
 * @author Simon Fischer, Jan Czogalla
 */
public class ParameterTypeDateFormat extends ParameterTypeStringCategory {

	private static final long serialVersionUID = 1L;
	public static final String INVALID_DATE_FORMAT = "invalid_date_format";
	public static final String INVALID_DATE_FORMAT_PARAMETER = INVALID_DATE_FORMAT + "_parameter";

	private transient InputPort inPort;

	private ParameterTypeAttribute attributeParameter;

	public static final String PARAMETER_DATE_FORMAT = "date_format";

	public static final String TIME_FORMAT_H_MM_A = "h:mm a";
	public static final String DATE_FORMAT_MM_DD_YYYY = "MM/dd/yyyy";
	public static final String DATE_FORMAT_DD_DOT_MM_DOT_YYYY = "dd.MM.yyyy";
	public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
	public static final String DATE_TIME_FORMAT_MM_DD_YYYY_H_MM_A = DATE_FORMAT_MM_DD_YYYY + " " + TIME_FORMAT_H_MM_A;
	public static final String DATE_TIME_FORMAT_YYYY_MM_DD_HH_MM_SS = DATE_FORMAT_YYYY_MM_DD + " HH:mm:ss";
	public static final String DATE_TIME_FORMAT_ISO8601_UTC_MS =  "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	public static final String DATE_TIME_FORMAT_M_D_YY_H_MM_A = "M/d/yy " + TIME_FORMAT_H_MM_A;
	public static final String[] PREDEFINED_DATE_FORMATS = new String[]{"", DATE_FORMAT_MM_DD_YYYY, DATE_FORMAT_DD_DOT_MM_DOT_YYYY, DATE_TIME_FORMAT_MM_DD_YYYY_H_MM_A,
			"yyyy/MM/dd", "dd/MM/yyyy", "dd/MM/yyyy HH:mm", "yyyy.MM.dd G 'at' HH:mm:ss z", "EEE, MMM d, ''yy", TIME_FORMAT_H_MM_A,
			"hh 'o''clock' a, zzzz", "K:mm a, z", "yyyy.MMMMM.dd GGG hh:mm aaa", "EEE, d MMM yyyy HH:mm:ss Z",
			"yyMMddHHmmssZ", DATE_TIME_FORMAT_ISO8601_UTC_MS, DATE_TIME_FORMAT_YYYY_MM_DD_HH_MM_SS, DATE_TIME_FORMAT_M_D_YY_H_MM_A, DATE_FORMAT_YYYY_MM_DD};

	/**
	 * Simple constructor with key {@value PARAMETER_DATE_FORMAT} and default description.
	 *
	 * @since 8.2
	 */
	public ParameterTypeDateFormat() {
		this(null, null);
	}

	/**
	 * Simple constructor with default key and description and example set meta data available.
	 *
	 * @since 8.2
	 */
	public ParameterTypeDateFormat(ParameterTypeAttribute attributeParameter, InputPort inPort) {
		this(attributeParameter, PARAMETER_DATE_FORMAT, "The parse format of the date values, for example \"yyyy/MM/dd\".", inPort, false);
	}

	/**
	 * This is the constructor for date format if no example set meta data is available.
	 */
	public ParameterTypeDateFormat(String key, String description, boolean expert) {
		this(null, key, description, null, expert);
	}

	/**
	 * This is the constructor for date format if no example set meta data is available.
	 */
	public ParameterTypeDateFormat(String key, String description, String defaultValue, boolean expert) {
		this(null, key, description, defaultValue, null, expert);
	}

	/**
	 * This is the constructor for parameter types of operators which transform an example set.
	 */
	public ParameterTypeDateFormat(ParameterTypeAttribute attributeParameter, String key, String description,
			InputPort inPort, boolean expert) {
		this(attributeParameter, key, description, "", inPort, expert);
	}

	/**
	 * This is the constructor for parameter types of operators which transform an example set.
	 */
	public ParameterTypeDateFormat(ParameterTypeAttribute attributeParameter, String key, String description,
			String defaultValue, InputPort inPort, boolean expert) {
		super(key, description, PREDEFINED_DATE_FORMATS, defaultValue, true);
		setExpert(expert);
		this.inPort = inPort;
		this.attributeParameter = attributeParameter;
	}

	public InputPort getInputPort() {
		return inPort;
	}

	/**
	 * This method returns the referenced attribute parameter or null if non exists.
	 */
	public ParameterTypeAttribute getAttributeParameterType() {
		return attributeParameter;
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

	/**
	 * Convenience method, same as {@link #createCheckedDateFormat(Operator, String, Locale, boolean) createCheckedDateFormat(op, pattern, null, false)}.
	 *
	 * @throws UserError
	 * 		iff the pattern is not valid
	 * @since 8.2
	 */
	public static SimpleDateFormat createCheckedDateFormat(Operator op, String pattern) throws UserError {
		return createCheckedDateFormat(op, pattern, null, false);
	}

	/**
	 * Convenience method, same as {@link #createCheckedDateFormat(Operator, String, Locale, boolean) createCheckedDateFormat(null, pattern, loc, false)}.
	 *
	 * @throws UserError
	 * 		iff the pattern is not valid
	 * @since 8.2
	 */
	public static SimpleDateFormat createCheckedDateFormat(String pattern, Locale loc) throws UserError {
		return createCheckedDateFormat(null, pattern, loc, false);
	}

	/**
	 * Convenience method, same as {@link #createCheckedDateFormat(Operator, String, Locale, boolean) createCheckedDateFormat(op, null, loc, isSetup)}.
	 *
	 * @throws UserError
	 * 		iff the pattern is not valid and {@code isSetup} is {@code false}
	 * @since 8.2
	 */
	public static SimpleDateFormat createCheckedDateFormat(Operator op, Locale loc, boolean isSetup) throws UserError {
		return createCheckedDateFormat(op, null, loc, isSetup);
	}

	/**
	 * Creates a {@link SimpleDateFormat} from the given parameters if possible. This method returns {@code null} if no
	 * operator was specified <strong>and</strong> {@code pattern} is {@code null} <em>or</em> if an error occurs during setup.
	 * <p>
	 * A date format pattern string is then extracted from the {@link Operator Operator's} parameter if {@code pattern} is {@code null},
	 * otherwise the provided {@code pattern} is used. Depending on {@code isSetup} either a {@link SimpleProcessSetupError}
	 * is added to the given operator, or a {@link UserError} is thrown <strong>iff</strong> the format pattern is not valid.
	 *
	 * @param op
	 * 		the operator to check for the parameter {@value #PARAMETER_DATE_FORMAT}; can be {@code null}
	 * @param pattern
	 * 		the pattern to check for validity; can be {@code null}
	 * @param loc
	 * 		the locale; if {@code null}, the default locale will be used
	 * @param inSetup
	 * 		whether the error should be a setup error (building process) or user error (running process)
	 * @return a {@link SimpleDateFormat} instance or {@code null}
	 * @throws UserError
	 * 		iff the pattern is not valid and {@code isSetup} is {@code false}
	 * @since 8.2
	 */
	public static SimpleDateFormat createCheckedDateFormat(Operator op, String pattern, Locale loc, boolean inSetup) throws UserError {
		if (op == null && pattern == null) {
			// insufficient information => ignore
			return null;
		}
		String dateFormat = "";
		try {
			if (pattern != null) {
				dateFormat = pattern;
			} else {
				dateFormat = op.getParameter(PARAMETER_DATE_FORMAT);
			}
			return loc == null ? new SimpleDateFormat(dateFormat) : new SimpleDateFormat(dateFormat, loc);
		} catch (UndefinedParameterError e) {
			// parameter not defined => ignore
		} catch (IllegalArgumentException e) {
			Object[] arguments = {dateFormat, e.getMessage(), PARAMETER_DATE_FORMAT.replaceAll("_", " ")};
			if (!inSetup) {
				throw new UserError(op, INVALID_DATE_FORMAT, arguments);
			}
			if (op != null) {
				op.addError(new SimpleProcessSetupError(Severity.ERROR, op.getPortOwner(),
						Collections.singletonList(new ParameterSettingQuickFix(op, PARAMETER_DATE_FORMAT)),
						INVALID_DATE_FORMAT_PARAMETER, arguments));
			}
		}
		return null;
	}
}
