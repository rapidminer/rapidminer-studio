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
package com.rapidminer.operator.nio;

import java.util.regex.Pattern;

import com.rapidminer.core.io.data.ColumnMetaData;


/**
 * Detects if SimpleDateFormat pattern is a Date, Time or DateTime format
 *
 * @author Jan Czogalla, Jonas Wilms-Pfau
 * @since 9.1
 */
public final class DateTimeTypeGuesser {

	private static final int DATE = 1;
	private static final int TIME = 2;

	// date indicators as defined by SimpleDateFormat javadoc
	private static final Pattern DATE_PATTERN = Pattern.compile(".*[yYMwWDdFEu].*");
	// time indicators as defined by SimpleDateFormat javadoc
	private static final Pattern TIME_PATTERN = Pattern.compile(".*[aHkKhmsS].*");
	// replacement match for escaped parts of simple date format
	private static final String REPLACE_PATTERN = "'.*?'";
	// replacement match for escaped single quotes  ' asdas '' asdsda '
	private static final String REPLACE_SINGLE_QUOTES = "''";

	private DateTimeTypeGuesser(){
		throw new AssertionError("Utility class");
	}

	/**
	 * Detects the ColumnType for a date pattern
	 *
	 * @param pattern a SimpleDateFormat pattern
	 * @return the ColumnType.DATE, ColumnType.TIME or ColumnType.DATETIME
	 */
	public static ColumnMetaData.ColumnType patternToColumnType(String pattern) {
		// first remove escaped single quotes ''
		pattern = pattern.replaceAll(REPLACE_SINGLE_QUOTES, "");
		// remove escaped strings
		pattern = pattern.replaceAll(REPLACE_PATTERN, "");
		int result = (DATE_PATTERN.matcher(pattern).matches() ? DATE : 0)
				+ (TIME_PATTERN.matcher(pattern).matches() ? TIME : 0);
		switch (result) {
			case DATE: return ColumnMetaData.ColumnType.DATE;
			case TIME: return ColumnMetaData.ColumnType.TIME;
			default:  return ColumnMetaData.ColumnType.DATETIME;
		}
	}
}
