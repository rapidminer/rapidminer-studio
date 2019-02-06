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
package com.rapidminer.operator.preprocessing.filter;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;


/**
 * @author Sebastian Land
 * 
 */
public abstract class AbstractDateDataProcessing extends AbstractDataProcessing {

	public static final String[] TIME_UNITS = { "millisecond", "second", "minute", "hour", "day", "week", "month",
			"quarter", "half year", "year" };

	public static final int MILLISECOND = 0;
	public static final int SECOND = 1;
	public static final int MINUTE = 2;
	public static final int HOUR = 3;
	public static final int DAY = 4;
	public static final int WEEK = 5;
	public static final int MONTH = 6;
	public static final int QUARTER = 7;
	public static final int HALF_YEAR = 8;
	public static final int YEAR = 9;

	public static final String[] PARAMETERS_RELATIVE_TO = { "millisecond_relative_to", "second_relative_to",
			"minute_relative_to", "hour_relative_to", "day_relative_to", "week_relative_to", "month_relative_to",
			"quarter_relative_to", "half_year_relative_to", "year_relative_to" };

	public static final String[][] RELATIVE_TO_MODES = { { "second", "epoch" }, { "minute", "hour", "day", "epoch" },
			{ "hour", "day", "epoch" }, { "day", "epoch" }, { "week", "month", "year", "epoch" },
			{ "month", "year", "epoch" }, { "quarter", "year", "epoch" }, { "year", "epoch" }, { "year", "epoch" },
			{ "epoch", "era" } };

	public static final int MILLISECOND_RELATIVE_TO_SECOND = 0;
	public static final int MILLISECOND_RELATIVE_TO_EPOCH = 1;

	public static final int SECOND_RELATIVE_TO_MINUTE = 0;
	public static final int SECOND_RELATIVE_TO_HOUR = 1;
	public static final int SECOND_RELATIVE_TO_DAY = 2;
	public static final int SECOND_RELATIVE_TO_EPOCH = 3;

	public static final int MINUTE_RELATIVE_TO_HOUR = 0;
	public static final int MINUTE_RELATIVE_TO_DAY = 1;
	public static final int MINUTE_RELATIVE_TO_EPOCH = 2;

	public static final int HOUR_RELATIVE_TO_DAY = 0;
	public static final int HOUR_RELATIVE_TO_EPOCH = 1;

	public static final int DAY_RELATIVE_TO_WEEK = 0;
	public static final int DAY_RELATIVE_TO_MONTH = 1;
	public static final int DAY_RELATIVE_TO_YEAR = 2;
	public static final int DAY_RELATIVE_TO_EPOCH = 3;

	public static final int WEEK_RELATIVE_TO_MONTH = 0;
	public static final int WEEK_RELATIVE_TO_YEAR = 1;
	public static final int WEEK_RELATIVE_TO_EPOCH = 2;

	public static final int MONTH_RELATIVE_TO_QUARTER = 0;
	public static final int MONTH_RELATIVE_TO_YEAR = 1;
	public static final int MONTH_RELATIVE_TO_EPOCH = 2;

	public static final int QUARTER_RELATIVE_TO_YEAR = 0;
	public static final int QUARTER_RELATIVE_TO_EPOCH = 1;

	public static final int HALF_YEAR_RELATIVE_TO_YEAR = 0;
	public static final int HALF_YEAR_RELATIVE_TO_EPOCH = 1;

	public static final int YEAR_RELATIVE_TO_EPOCH = 0;
	public static final int YEAR_RELATIVE_TO_ERA = 1;

	public static final int[] RELATIVE_TO_DEFAULTS = { MILLISECOND_RELATIVE_TO_SECOND, SECOND_RELATIVE_TO_MINUTE,
			MINUTE_RELATIVE_TO_HOUR, HOUR_RELATIVE_TO_DAY, DAY_RELATIVE_TO_MONTH, WEEK_RELATIVE_TO_YEAR,
			MONTH_RELATIVE_TO_YEAR, QUARTER_RELATIVE_TO_YEAR, HALF_YEAR_RELATIVE_TO_YEAR, YEAR_RELATIVE_TO_ERA };

	public static List<Locale> availableLocales = new ArrayList<Locale>();

	public static String[] availableLocaleNames;

	public static int defaultLocale;

	static {
		Locale[] availableLocaleArray = Locale.getAvailableLocales();

		for (Locale l : availableLocaleArray) {
			availableLocales.add(l);
		}

		Collections.sort(availableLocales, new Comparator<Locale>() {

			@Override
			public int compare(Locale o1, Locale o2) {
				return o1.getDisplayName().compareTo(o2.getDisplayName());
			}
		});

		availableLocaleNames = new String[availableLocales.size()];
		defaultLocale = -1;
		for (int i = 0; i < availableLocales.size(); i++) {
			Locale currentLocale = availableLocales.get(i);
			availableLocaleNames[i] = currentLocale.getDisplayName();
			if (currentLocale.equals(Locale.US)) {
				defaultLocale = i;
			}
		}
		if (defaultLocale < 0) {
			defaultLocale = 0;
		}
	}

	public AbstractDateDataProcessing(OperatorDescription description) {
		super(description);

	}
}
