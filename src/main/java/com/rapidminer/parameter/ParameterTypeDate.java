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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Element;

import com.michaelbaranov.microba.calendar.DatePicker;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.XMLException;


/**
 * A parameter type for Dates. It is represented by a {@link DatePicker} element in the GUI. The
 * date is internally stored as string. To get a {@link Date} object use
 * {@link ParameterTypeDate#getParameterAsDate(String, Operator)}.
 *
 * @author Nils Woehler
 *
 */
public class ParameterTypeDate extends ParameterTypeSingle {

	private static final long serialVersionUID = 1L;

	private Date defaultValue = null;

	public static final ThreadLocal<SimpleDateFormat> DATE_FORMAT;

	static {
		// ThreadLocale because this is static and used by other threads
		// and DateFormats are NOT threadsafe
		DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {

			SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss Z");

			@Override
			protected SimpleDateFormat initialValue() {
				return format;
			}
		};
	}

	/**
	 * @param element
	 * @throws XMLException
	 */
	public ParameterTypeDate(Element element) throws XMLException {
		super(element);
	}

	public ParameterTypeDate(String key, String description) {
		super(key, description);
	}

	public ParameterTypeDate(String key, String description, Date defaultValue) {
		super(key, description);
		setDefaultValue(defaultValue);
	}

	public ParameterTypeDate(String key, String description, boolean optional, boolean expert) {
		super(key, description);
		setOptional(optional);
		setExpert(expert);
		setDefaultValue(null);
	}

	public ParameterTypeDate(String key, String description, Date defaultValue, boolean optional, boolean expert) {
		super(key, description);
		setOptional(optional);
		setExpert(expert);
		setDefaultValue(defaultValue);
	}

	@Override
	public String getRange() {
		return null; // no range here as showRange() returns null
	}

	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = (Date) defaultValue;
	}

	@Override
	public boolean isNumerical() {
		return false;
	}

	@Override
	public boolean showRange() {
		return false;
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

	@Override
	public String toString(Object value) {
		if (value == null) {
			return "";
		}
		String ret = null;
		if (value instanceof Date) {
			ret = DATE_FORMAT.get().format(value);
		} else {
			ret = String.valueOf(value);
		}
		return ret;
	}

	@Override
	public String toXMLString(Object value) {
		if (value == null) {
			return "";
		}
		String ret = null;
		if (value instanceof Date) {
			ret = DATE_FORMAT.get().format(value);
		} else {
			ret = String.valueOf(value);
		}
		return ret;
	}

	public static Date getParameterAsDate(String key, Operator operator) throws UndefinedParameterError, UserError {
		String value = operator.getParameter(key);
		if (value == null || value.trim().isEmpty()) {
			throw new UndefinedParameterError(key, operator);
		}
		try {
			return ParameterTypeDate.DATE_FORMAT.get().parse(value);
		} catch (ParseException e) {
			throw new UserError(operator, "wrong_date_format", value, key.replace('_', ' '));
		}
	}

	/**
	 * Can be used to check whether the current string is a valid date string for this parameter
	 * type.
	 */
	public static boolean isValidDate(String dateString) {
		try {
			return dateString != null && DATE_FORMAT.get().parse(dateString) != null;
		} catch (ParseException e) {
			return false;
		}
	}

}
