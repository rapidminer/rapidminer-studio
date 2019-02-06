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
package com.rapidminer.tools;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * A parser class for dates to be utilized inside date handling operators.
 * 
 * @author Sebastian Land
 * @deprecated since 8.2; please use {@link ParameterTypeDateFormat}.
 */
@Deprecated
public class DateParser extends SimpleDateFormat {

	private static final long serialVersionUID = -950183600865410299L;

	public static final String PARAMETER_DATE_FORMAT = ParameterTypeDateFormat.PARAMETER_DATE_FORMAT;

	public static final String DEFAULT_DATE_FORMAT = ParameterTypeDateFormat.DATE_FORMAT_YYYY_MM_DD;

	public static final String DEFAULT_DATE_TIME_FORMAT = ParameterTypeDateFormat.DATE_TIME_FORMAT_YYYY_MM_DD_HH_MM_SS;

	public DateParser(String dateFormat) {
		super(dateFormat);
	}

	/**
	 * @deprecated since 8.2; please use {@link ParameterTypeDateFormat#createCheckedDateFormat(Operator, String, Locale, boolean)}
	 * or one of its convenience methods.
	 */
	@Deprecated
	public static DateParser getInstance(ParameterHandler handler) throws UndefinedParameterError {
		String dateFormat = handler.getParameterAsString(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT);
		return new DateParser(dateFormat);
	}

	/**
	 * @deprecated since 8.2; please use one of {@link ParameterTypeDateFormat ParameterTypeDateFormats} constructors
	 * to add a {@value ParameterTypeDateFormat#PARAMETER_DATE_FORMAT} parameter to your operator.
	 */
	@Deprecated
	public static List<ParameterType> getParameterTypes(ParameterHandler handler) {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterTypeDateFormat type = new ParameterTypeDateFormat();
		type.setDescription("The format pattern of date values.");
		type.setDefaultValue(ParameterTypeDateFormat.DATE_FORMAT_YYYY_MM_DD);
		types.add(type);
		return types;
	}
}
