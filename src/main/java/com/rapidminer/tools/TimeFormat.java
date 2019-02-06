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

import java.text.DecimalFormat;
import java.text.NumberFormat;


/**
 * <p>
 * This class provides some utility functions useful to format elapsed time (usually given in
 * milliseconds).
 * </p>
 * 
 * @author Christian Bockermann
 */
public class TimeFormat {

	/* Milliseconds of one second */
	public final static long SEC_MS = 1000;

	/* Milliseconds of one minute */
	public final static long MIN_MS = 60 * SEC_MS;

	/* Milliseconds of one hour */
	public final static long HOUR_MS = 60 * MIN_MS;

	/* Milliseconds of one day */
	public final static long DAY_MS = 24 * HOUR_MS;

	/* Milliseconds of a week */
	public final static long WEEK_MS = 7 * DAY_MS;

	// do not use months and years since the number of days vary!!!

	/* The number format for formatting the seconds */
	private NumberFormat numberFormat = null;

	/**
	 * Creates a new TimeFormat instance.
	 */
	public TimeFormat() {
		this(new DecimalFormat("0"));
	}

	/**
	 * Create a new time formatter object which uses the given NumberFormat to display the number of
	 * seconds. This can be used to support a fine granulated display of seconds.
	 */
	public TimeFormat(NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
	}

	/**
	 * <p>
	 * This method takes the given number of milliseconds, <code>time</code>, and creates a new
	 * String containing a description of the time by means of days, hours, minutes and seconds. If
	 * <code>time</code> is less than any of the mentioned properties, then this field will not be
	 * printed, e.g.
	 * <ul>
	 * <li>calling <code>format( 1000 )</code> will result in the string <code>&quot;1s&quot;</code>
	 * </li>
	 * 
	 * <li>calling <code>format( 90000 * 1000 )</code>, i.e. milliseconds of one day + 1 hour, will
	 * result in <code>&quot;1 day 1h&quot;</code>.</li>
	 * </ul>
	 * </p>
	 * 
	 * @param timeInMilliseconds
	 *            The time as an amount of milliseconds.
	 * @return The time formatted as printable string.
	 */
	public String format(long timeInMilliseconds) {
		StringBuffer result = new StringBuffer();

		// weeks
		if (timeInMilliseconds > WEEK_MS) {
			long weeks = timeInMilliseconds / WEEK_MS;
			result.append(numberFormat.format(weeks) + " Week");
			if (weeks > 1) {
				result.append("s");
			}
			timeInMilliseconds -= weeks * WEEK_MS;
			result.append(" ");
		}

		// days
		boolean showHours = false;
		if (timeInMilliseconds > DAY_MS) {
			long days = timeInMilliseconds / DAY_MS;
			result.append(numberFormat.format(days) + " Day");
			if (days > 1) {
				result.append("s");
			}
			timeInMilliseconds -= days * DAY_MS;
			result.append(" ");
			showHours = true;
		}

		// hours
		boolean showMinutes = false;
		if (timeInMilliseconds > HOUR_MS) {
			long hours = timeInMilliseconds / HOUR_MS;
			result.append(appendLeadingZeros(numberFormat.format(hours)) + ":");
			timeInMilliseconds -= hours * HOUR_MS;
			showMinutes = true;
		} else {
			if (showHours) {
				result.append("00:");
				showMinutes = true;
			}
		}

		// minutes
		boolean showSeconds = false;
		if (timeInMilliseconds > MIN_MS) {
			long minutes = timeInMilliseconds / MIN_MS;
			if (showMinutes) {
				result.append(appendLeadingZeros(numberFormat.format(minutes)) + ":");
			} else {
				result.append(numberFormat.format(minutes) + ":");
			}
			timeInMilliseconds -= minutes * MIN_MS;
			showSeconds = true;
		} else {
			if (showMinutes) {
				result.append("00:");
				showSeconds = true;
			}
		}

		// seconds
		boolean showUnit = result.length() == 0;
		if (timeInMilliseconds > SEC_MS) {
			long seconds = timeInMilliseconds / SEC_MS;
			if (showSeconds) {
				result.append(appendLeadingZeros(numberFormat.format(seconds)));
			} else {
				result.append(numberFormat.format(seconds));
			}
		} else {
			if (showSeconds) {
				result.append("00");
			}
		}
		if ((showUnit) && (result.length() > 0)) {
			result.append(" s");
		}

		if (result.length() == 0) {
			result.append("0 s");
		}
		return result.toString();
	}

	private String appendLeadingZeros(String number) {
		if (number.length() < 2) {
			return "0" + number;
		} else {
			return number;
		}
	}
}
