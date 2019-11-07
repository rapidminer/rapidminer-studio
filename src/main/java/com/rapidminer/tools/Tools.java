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

import static com.rapidminer.tools.FunctionWithThrowable.suppress;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.AccessControlException;
import java.security.AccessController;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.rapidminer.RapidMiner;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.internal.ProcessEmbeddingOperator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.security.PluginSandboxPolicy;
import com.rapidminer.tools.io.Encoding;
import com.rapidminer.tools.parameter.ParameterChangeListener;
import com.rapidminer.tools.plugin.Plugin;


/**
 * Tools for RapidMiner.
 *
 * @author Simon Fischer, Ingo Mierswa, Marco Boeck
 */
public class Tools {

	/** Units for sizes in bytes. */
	private static final String[] MEMORY_UNITS = { "b", "kB", "MB", "GB", "TB" };

	/** The line separator depending on the operating system. */
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	/** Number smaller than this value are considered as zero. */
	private static final double IS_ZERO = 1E-6;

	/** Used for formatting values in the {@link #formatTime(Date)} method. */
	private static final TimeFormat DURATION_TIME_FORMAT = new TimeFormat();

	// ThreadLocal because DateFormat is NOT threadsafe and creating a new DateFormat is
	// EXTREMELY expensive
	/** Used for formatting values in the {@link #formatTime(Date)} method. */
	private static final ThreadLocal<DateFormat> TIME_FORMAT = ThreadLocal.withInitial(() -> {
		// clone because getDateInstance uses an internal pool which can return the same
		// instance for multiple threads
		return (DateFormat) DateFormat.getTimeInstance(DateFormat.LONG, Locale.getDefault()).clone();
	});

	// ThreadLocal because DateFormat is NOT threadsafe and creating a new DateFormat is
	// EXTREMELY expensive
	/** Used for formatting values in the {@link #formatDate(Date)} method. */
	private static final ThreadLocal<DateFormat> DATE_FORMAT = ThreadLocal.withInitial(() -> {
		// clone because getDateInstance uses an internal pool which can return the same
		// instance for multiple threads
		return (DateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).clone();
	});

	// ThreadLocal because DateFormat is NOT threadsafe and creating a new DateFormat is
	// EXTREMELY expensive
	/** Used for formatting values in the {@link #formatDateTime(Date)} method. */
	public static final ThreadLocal<DateFormat> DATE_TIME_FORMAT = ThreadLocal.withInitial(() -> {
		// clone because getDateInstance uses an internal pool which can return the same
		// instance for multiple threads
		return (DateFormat) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG, Locale.getDefault())
				.clone();
	});

	private static Locale FORMAT_LOCALE = Locale.US;

	/** Used for formatting values in the {@link #formatNumber(double)} method. */
	private static NumberFormat NUMBER_FORMAT;

	/** Used for formatting values in the {@link #formatNumber(double)} method. */
	private static NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(FORMAT_LOCALE);

	/** Used for formatting values in the {@link #formatPercent(double)} method. */
	private static NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance(FORMAT_LOCALE);

	/** Used for determining the symbols used in decimal formats. */
	public static DecimalFormatSymbols FORMAT_SYMBOLS = new DecimalFormatSymbols(FORMAT_LOCALE);

	/** this is close to the smallest machine epsilon where n + epsilon = n for double precision */
	private static final double SMALLEST_MACHINE_EPSILON = 1.11E-16;

	/** the double value below which numbers are displayed as integer */
	private static double epsilonDisplayValue;

	/** if a date should be created from NaN, this is returned */
	private static final String MISSING_DATE = "Missing";

	/** if a time should be created from NaN, this is returned */
	private static final String MISSING_TIME = "Missing";

	/** the current settings value of number of fraction digits shown */
	private static int numberOfFractionDigits;

	private static final List<ResourceSource> ALL_RESOURCE_SOURCES = Collections.synchronizedList(new LinkedList<>());
	private static final Map<String, ResourceSource> PLUGIN_RESOURCE_SOURCES = Collections.synchronizedMap(new HashMap<>());

	public static final String RESOURCE_PREFIX = "com/rapidminer/resources/";

	static {
		ALL_RESOURCE_SOURCES.add(new ResourceSource(Tools.class.getClassLoader()));

		// init parameters
		int numberDigits = 3;
		try {
			String numberDigitsString = ParameterService
					.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_NUMBERS);
			numberDigits = Integer.parseInt(numberDigitsString);
		} catch (NumberFormatException e) {
		}
		numberOfFractionDigits = numberDigits;
		epsilonDisplayValue = Math.min(SMALLEST_MACHINE_EPSILON, 1.0 / Math.pow(10, numberOfFractionDigits));
		NUMBER_FORMAT = new DecimalFormat(getDecimalFormatPattern(numberDigits),
				DecimalFormatSymbols.getInstance(FORMAT_LOCALE));

		// add listener to be notified of changes for static parameters
		ParameterService.registerParameterChangeListener(new ParameterChangeListener() {

			@Override
			public void informParameterSaved() {
				// ignore
			}

			@Override
			public void informParameterChanged(String key, String value) {
				if (RapidMiner.PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_NUMBERS.equals(key)) {
					int numberDigits = 3;
					try {
						String numberDigitsString = ParameterService
								.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_NUMBERS);
						numberDigits = Integer.parseInt(numberDigitsString);
					} catch (NumberFormatException e) {
					}
					numberOfFractionDigits = numberDigits;
					epsilonDisplayValue = Math.min(SMALLEST_MACHINE_EPSILON, 1.0 / Math.pow(10, numberOfFractionDigits));
					NUMBER_FORMAT = new DecimalFormat(getDecimalFormatPattern(numberDigits),
							DecimalFormatSymbols.getInstance(FORMAT_LOCALE));
				}
			}
		});
	}

	public static String[] availableTimeZoneNames;

	public static final int SYSTEM_TIME_ZONE = 0;

	static {
		String[] allTimeZoneNames = TimeZone.getAvailableIDs();
		Arrays.sort(allTimeZoneNames);

		availableTimeZoneNames = new String[allTimeZoneNames.length + 1];
		availableTimeZoneNames[SYSTEM_TIME_ZONE] = "SYSTEM";
		System.arraycopy(allTimeZoneNames, 0, availableTimeZoneNames, 1, allTimeZoneNames.length);
	}

	public static void setFormatLocale(Locale locale) {
		FORMAT_LOCALE = locale;

		int numberDigits = 3;
		try {
			String numberDigitsString = ParameterService
					.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_NUMBERS);
			numberDigits = Integer.parseInt(numberDigitsString);
		} catch (NumberFormatException e) {
		}
		NUMBER_FORMAT = new DecimalFormat(getDecimalFormatPattern(numberDigits),
				DecimalFormatSymbols.getInstance(FORMAT_LOCALE));
		INTEGER_FORMAT = NumberFormat.getIntegerInstance(locale);
		PERCENT_FORMAT = NumberFormat.getPercentInstance(locale);
		FORMAT_SYMBOLS = new DecimalFormatSymbols(locale);
	}

	public static Locale getFormatLocale() {
		return FORMAT_LOCALE;
	}

	public static String[] getAllTimeZones() {
		return availableTimeZoneNames;
	}

	public static TimeZone getTimeZone(int index) {
		if (index == SYSTEM_TIME_ZONE) {
			return TimeZone.getDefault();
		} else {
			return TimeZone.getTimeZone(availableTimeZoneNames[index]);
		}
	}

	public static TimeZone getPreferredTimeZone() {
		return getTimeZone(getPreferredTimeZoneIndex());
	}

	public static int getPreferredTimeZoneIndex() {
		String timeZoneString = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_TIME_ZONE);
		int preferredTimeZone = SYSTEM_TIME_ZONE;
		try {
			if (timeZoneString != null) {
				preferredTimeZone = Integer.parseInt(timeZoneString);
			}
		} catch (NumberFormatException e) {
			int index = 0;
			boolean found = false;
			for (String id : availableTimeZoneNames) {
				if (id.equals(timeZoneString)) {
					found = true;
					break;
				}
				index++;
			}
			if (found) {
				preferredTimeZone = index;
			}
		}

		return preferredTimeZone;
	}

	public static Calendar getPreferredCalendar() {
		return Calendar.getInstance(getPreferredTimeZone(), Locale.getDefault());
	}

	/**
	 * Formats the value according to the given valueType. The value must be an object of type
	 * String for nominal values, an object of type Date for date_time values or of type Double for
	 * numerical values.
	 *
	 * @param value
	 * @param valueType
	 * @return value as string
	 */
	public static String format(Object value, int valueType) {
		if (value == null) {
			return "?";
		}
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NOMINAL)) {
			return (String) value;
		}
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NUMERICAL)) {
			return formatIntegerIfPossible((Double) value);
		}
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE)) {
			return formatDate((Date) value);
		}
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.TIME)) {
			return formatTime((Date) value);
		}
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE_TIME)) {
			return formatDateTime((Date) value);
		}
		return "?";
	}

	/**
	 * Returns a formatted string of the given number (percent format with two fraction digits).
	 */
	public static String formatPercent(double value) {
		if (Double.isNaN(value)) {
			return "?";
		}
		String percentDigitsString = ParameterService
				.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_PERCENT);
		int percentDigits = 2;
		try {
			if (percentDigitsString != null) {
				percentDigits = Integer.parseInt(percentDigitsString);
			}
		} catch (NumberFormatException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.Tools.bad_integer_for_property");

		}
		PERCENT_FORMAT.setMaximumFractionDigits(percentDigits);
		PERCENT_FORMAT.setMinimumFractionDigits(percentDigits);
		return PERCENT_FORMAT.format(value);
	}

	/**
	 * Returns a formatted string of the given number (number format with usually three fraction
	 * digits).
	 */
	public static String formatNumber(double value) {
		if (Double.isNaN(value)) {
			return "?";
		}
		// TODO: read property for grouping characters
		return formatNumber(value, numberOfFractionDigits, false);
	}

	/**
	 * Returns a formatted string of the given number (uses the property
	 * rapidminer.gui.fractiondigits.numbers if the given number of digits is smaller than 0
	 * (usually 3)).
	 */
	public static String formatNumber(double value, int numberOfDigits) {
		// TODO: read property for grouping characters
		return formatNumber(value, numberOfDigits, false);
	}

	/**
	 * Returns a formatted string of the given number (uses the property
	 * rapidminer.gui.fractiondigits.numbers if the given number of digits is smaller than 0
	 * (usually 3)).
	 */
	public static String formatNumber(double value, int numberOfDigits, boolean groupingCharacters) {
		if (Double.isNaN(value)) {
			return "?";
		}
		int numberDigits = numberOfDigits;
		if (numberDigits < 0) {
			numberDigits = numberOfFractionDigits;
		}
		NUMBER_FORMAT.setMinimumFractionDigits(numberDigits);
		NUMBER_FORMAT.setMaximumFractionDigits(numberDigits);
		NUMBER_FORMAT.setGroupingUsed(groupingCharacters);
		return NUMBER_FORMAT.format(value);
	}

	/**
	 * Returns a number string with no fraction digits if possible. Otherwise the default number of
	 * digits will be returned.
	 */
	public static String formatIntegerIfPossible(double value) {
		return formatIntegerIfPossible(value, numberOfFractionDigits);
	}

	/**
	 * Returns a number string with no fraction digits if possible. Otherwise the given number of
	 * digits will be returned.
	 */
	public static String formatIntegerIfPossible(double value, int numberOfDigits) {
		// TODO: read property for grouping characters
		return formatIntegerIfPossible(value, numberOfDigits, false);
	}

	/**
	 * Returns a number string with no fraction digits if possible. Otherwise the given number of
	 * digits will be returned.
	 */
	public static String formatIntegerIfPossible(double value, int numberOfDigits, boolean groupingCharacter) {
		if (Double.isNaN(value)) {
			return "?";
		}
		if (Double.isInfinite(value)) {
			if (value < 0) {
				return "-" + FORMAT_SYMBOLS.getInfinity();
			} else {
				return FORMAT_SYMBOLS.getInfinity();
			}
		}

		long longValue = Math.round(value);
		if (Math.abs(longValue - value) < epsilonDisplayValue) {
			INTEGER_FORMAT.setGroupingUsed(groupingCharacter);
			return INTEGER_FORMAT.format(value);
		}

		return formatNumber(value, numberOfDigits, groupingCharacter);
	}

	/** Format date as a short time string. */
	public static String formatTime(Date date) {
		TIME_FORMAT.get().setTimeZone(getPreferredTimeZone());
		return TIME_FORMAT.get().format(date);
	}

	/**
	 * Format double value as a short time string. If value is NaN, returns {@value #MISSING_TIME}.
	 *
	 * @param value
	 * 		the value to be formatted as time
	 * @return a short time string or {@value #MISSING_TIME} if value was NaN
	 * @since 6.1.1
	 */
	public static String createTimeAndFormat(double value) {
		if (Double.isNaN(value)) {
			return MISSING_TIME;
		} else {
			TIME_FORMAT.get().setTimeZone(getPreferredTimeZone());
			return TIME_FORMAT.get().format(new Date((long) value));
		}
	}

	/**
	 * Format date as a short date string.
	 */
	public static String formatDate(Date date) {
		DATE_FORMAT.get().setTimeZone(getPreferredTimeZone());
		return DATE_FORMAT.get().format(date);
	}

	/**
	 * Format double value as a short date string. If value is NaN, returns {@value #MISSING_DATE}.
	 *
	 * @param value
	 * 		the value to be formatted as a date
	 * @return a short date string or {@value #MISSING_DATE} if value was NaN
	 * @since 6.1.1
	 */
	public static String createDateAndFormat(double value) {
		if (Double.isNaN(value)) {
			return MISSING_DATE;
		} else {
			DATE_FORMAT.get().setTimeZone(getPreferredTimeZone());
			return DATE_FORMAT.get().format(new Date((long) value));
		}
	}

	/** Format date as a short time string. */
	public static String formatDateTime(Date date) {
		DATE_TIME_FORMAT.get().setTimeZone(getPreferredTimeZone());
		return DATE_TIME_FORMAT.get().format(date);
	}

	/**
	 * Format double value as a short datetime string. If value is NaN, returns
	 * {@value #MISSING_DATE}.
	 *
	 * @param value
	 * 		the value to be formatted as datetime
	 * @return a short datetime string or {@value #MISSING_DATE} if value was NaN
	 * @since 6.1.1
	 */
	public static String createDateTimeAndFormat(double value) {
		if (Double.isNaN(value)) {
			return MISSING_DATE;
		} else {
			DATE_TIME_FORMAT.get().setTimeZone(getPreferredTimeZone());
			return DATE_TIME_FORMAT.get().format(new Date((long) value));
		}
	}

	public static String formatDateTime(Date date, String pattern) {
		SimpleDateFormat format = new SimpleDateFormat(pattern);
		format.setTimeZone(getPreferredTimeZone());
		return format.format(date);
	}

	/** Format the given amount of milliseconds as a human readable string. */
	public static String formatDuration(long milliseconds) {
		return DURATION_TIME_FORMAT.format(milliseconds);
	}

	/** Returns the name for an ordinal number. */
	public static String ordinalNumber(int n) {
		if (n % 10 == 1 && n % 100 != 11) {
			return n + "st";
		}
		if (n % 10 == 2 && n % 100 != 12) {
			return n + "nd";
		}
		if (n % 10 == 3 && n % 100 != 13) {
			return n + "rd";
		}
		return n + "th";
	}

	/**
	 * Returns <code>true</code> if the difference between both numbers is smaller than IS_ZERO or
	 * both are Double.NaN. If either d1 or d2 is Double.NaN it will return <code>false</code>.
	 */
	public static boolean isEqual(double d1, double d2) {
		// NaN handling
		if (Double.isNaN(d1) && Double.isNaN(d2)) {
			return true;
		}
		if (Double.isNaN(d1) || Double.isNaN(d2)) {
			return false;
		}
		// normal handling
		return Math.abs(d1 - d2) < IS_ZERO;
	}

	/** Returns {@link #isEqual(double, double)} for d and 0. */
	public static boolean isZero(double d) {
		return isEqual(d, 0.0d);
	}

	/** Returns not {@link #isEqual(double, double)}. */
	public static boolean isNotEqual(double d1, double d2) {
		return !isEqual(d1, d2);
	}

	/**
	 * Returns <code>false</code> if either d1 or d2 is Double.NaN. Otherwis returns
	 * <code>true</code> if the d1 is greater than d2.
	 */
	public static boolean isGreater(double d1, double d2) {
		if (Double.isNaN(d1) || Double.isNaN(d2)) {
			return false;
		}
		return Double.compare(d1, d2) > 0;
	}

	/**
	 * Returns <code>false</code> if either d1 or d2 is Double.NaN. Returns <code>true</code> if the
	 * d1 is greater than d2 or both are equal.
	 */
	public static boolean isGreaterEqual(double d1, double d2) {
		if (Double.isNaN(d1) || Double.isNaN(d2)) {
			return false;
		}
		return Double.compare(d1, d2) > 0 || isEqual(d1, d2);
	}

	/**
	 * Returns <code>false</code> if either d1 or d2 is Double.NaN. Returns <code>true</code> if the
	 * d1 is less than d2.
	 */
	public static boolean isLess(double d1, double d2) {
		if (Double.isNaN(d1) || Double.isNaN(d2)) {
			return false;
		}
		return Double.compare(d1, d2) < 0;
	}

	/**
	 * Returns <code>false</code> if either d1 or d2 is Double.NaN. Returns <code>true</code> if the
	 * d1 is less than d2 or both are equal.
	 */
	public static boolean isLessEqual(double d1, double d2) {
		if (Double.isNaN(d1) || Double.isNaN(d2)) {
			return false;
		}
		return Double.compare(d1, d2) < 0 || isEqual(d1, d2);
	}

	/**
	 * Returns <code>true</code> if date d1 is equal to date d2. Returns <code>false</code> if
	 * either d1 or d2 is <code>null</code> or dates are not equal.
	 */
	public static boolean isEqual(Date d1, Date d2) {
		if (d1 == d2) {
			return true;
		}
		if (d1 == null || d2 == null) {
			return false;
		}
		return d1.compareTo(d2) == 0;
	}

	/** Returns no {@link #isEqual(Date, Date)}. */
	public static boolean isNotEqual(Date d1, Date d2) {
		return !isEqual(d1, d2);
	}

	/**
	 * Returns <code>true</code> if the date d1 is greater than date d2. Returns <code>false</code>
	 * if either d1 or d2 are <code>null</code>.
	 */
	public static boolean isGreater(Date d1, Date d2) {
		if (d1 == null || d2 == null) {
			return false;
		}
		return d1.compareTo(d2) > 0;
	}

	/** Returns true if the date d1 is greater than date d1 or both are equal */
	public static boolean isGreaterEqual(Date d1, Date d2) {
		return isEqual(d1, d2) || d1 != null && d1.compareTo(d2) > 0;
	}

	/**
	 * Returns <code>true</code> if the date d1 is less than date d2. Returns <code>false</code> if
	 * either d1 or d2 are <code>null</code>.
	 */
	public static boolean isLess(Date d1, Date d2) {
		if (d1 == null || d2 == null) {
			return false;
		}
		return d1.compareTo(d2) < 0;
	}

	/** Returns true if the date d1 is less than date d1 or both are equal. */
	public static boolean isLessEqual(Date d1, Date d2) {
		return isEqual(d1, d2) || d1 != null && d1.compareTo(d2) < 0;
	}

	// ====================================

	/** Returns the correct line separator for the current operating system. */
	public static String getLineSeparator() {
		return LINE_SEPARATOR;
	}

	/**
	 * Returns the correct line separator for the current operating system concatenated for the
	 * given number of times.
	 */
	public static String getLineSeparators(int number) {
		if (number < 0) {
			number = 0;
		}
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < number; i++) {
			result.append(LINE_SEPARATOR);
		}
		return result.toString();
	}

	/**
	 * Replaces all possible line feed character combinations by &quot;\n&quot;. This might be
	 * important for GUI purposes like tool tip texts which do not support carriage return
	 * combinations.
	 */
	public static String transformAllLineSeparators(String text) {
		Pattern crlf = Pattern.compile("(\r\n|\r|\n|\n\r)");
		Matcher m = crlf.matcher(text);
		if (m.find()) {
			text = m.replaceAll("\n");
		}
		return text;
	}

	/**
	 * Removes all possible line feed character combinations. This might be important for GUI
	 * purposes like tool tip texts which do not support carriage return combinations.
	 */
	public static String removeAllLineSeparators(String text) {
		Pattern crlf = Pattern.compile("(\r\n|\r|\n|\n\r)");
		Matcher m = crlf.matcher(text);
		if (m.find()) {
			text = m.replaceAll(" ");
		}
		return text;
	}

	/**
	 * Returns the class name of the given class without the package information.
	 *
	 * @deprecated Call c.getSimpleName() directly.
	 */
	@Deprecated
	public static String classNameWOPackage(Class<?> c) {
		return c.getSimpleName();
	}

	/**
	 * Clones a {@link List} of {@link Operator}s including connections.
	 *
	 * @param operators
	 * 		List of operators.
	 * @return Cloned list of operators.
	 */
	public static List<Operator> cloneOperators(List<Operator> operators) {
		List<Operator> clonedOperators = new ArrayList<>(operators.size());
		Map<Operator, Operator> originalToClone = new HashMap<>(operators.size());

		for (Operator operator : operators) {
			// clone operator
			Operator clone = operator.cloneOperator(operator.getName(), false);
			clonedOperators.add(clone);
			// create mapping from original to cloned operator
			originalToClone.put(operator, clone);
		}

		for (Operator operator : operators) {
			// adjust connections
			cloneOperatorConnections(operator, originalToClone);
		}

		for (Operator operator : operators) {
			// unlock ports
			operator.getInputPorts().unlockPortExtenders();
			operator.getOutputPorts().unlockPortExtenders();
		}

		return clonedOperators;
	}

	private static void cloneOperatorConnections(Operator operator, Map<Operator, Operator> originalToClone) {
		for (OutputPort originalSource : operator.getOutputPorts().getAllPorts()) {
			if (originalSource.isConnected()) {
				// look up cloned source operator
				Operator clonedSourceOperator = originalToClone.get(operator);
				if (clonedSourceOperator == null) {
					// the source operator was not cloned, most likely not part of the copied
					// selection of operators ... ignore
					continue;
				}
				OutputPort clonedSource = clonedSourceOperator.getOutputPorts().getPortByName(originalSource.getName());
				if (clonedSource == null) {
					throw new RuntimeException("Error during clone: incomplete mapping.");
				}

				// look up cloned destination operator
				InputPort originalDestination = originalSource.getDestination();
				Operator clonedDestOperator = originalToClone.get(originalDestination.getPorts().getOwner().getOperator());
				if (clonedDestOperator == null) {
					// the destination operator was not cloned, most likely not part of the copied
					// selection of operators ... ignore
					continue;
				}
				InputPort clonedDestination = clonedDestOperator.getInputPorts()
						.getPortByName(originalDestination.getName());
				if (clonedDestination == null) {
					throw new RuntimeException("Error during clone: incomplete mapping.");
				}

				// connect cloned source to cloned destination
				clonedSource.connectTo(clonedDestination);
			}
		}
	}

	// ====================================

	/**
	 * Reads the output of the reader and delivers it as string.
	 */
	public static String readOutput(BufferedReader in) throws IOException {
		StringBuffer output = new StringBuffer();
		String line = null;
		while ((line = in.readLine()) != null) {
			output.append(line);
			output.append(Tools.getLineSeparator());
		}
		return output.toString();
	}

	/**
	 * Creates a file relative to the given parent if name is not an absolute file name. Returns
	 * null if name is null.
	 */
	public static File getFile(File parent, String name) {
		if (name == null) {
			return null;
		}
		File file = new File(name);
		if (file.isAbsolute()) {
			return file;
		} else {
			return new File(parent, name);
		}
	}

	/**
	 * This method checks if the given file is a Zip file containing one entry (in case of file
	 * extension .zip). If this is the case, a reader based on a ZipInputStream for this entry is
	 * returned. Otherwise, this method checks if the file has the extension .gz. If this applies, a
	 * gzipped stream reader is returned. Otherwise, this method just returns a BufferedReader for
	 * the given file (file was not zipped at all).
	 */
	public static BufferedReader getReader(File file, Charset encoding) throws IOException {
		// handle zip files if necessary
		if (file.getAbsolutePath().endsWith(".zip")) {
			try (ZipFile zipFile = new ZipFile(file)) {
				if (zipFile.size() == 0) {
					throw new IOException("Input of Zip file failed: the file archive does not contain any entries.");
				}
				if (zipFile.size() > 1) {
					throw new IOException("Input of Zip file failed: the file archive contains more than one entry.");
				}
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
				InputStream zipIn = zipFile.getInputStream(entries.nextElement());
				return new BufferedReader(new InputStreamReader(zipIn, encoding));
			}
		} else if (file.getAbsolutePath().endsWith(".gz")) {
			return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), encoding));
		} else {
			return new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
		}
	}

	/**
	 * This method tries to identify the encoding if a GUI is running and a process is defined. In
	 * this case, the encoding is taken from the process. Otherwise, the method tries to identify
	 * the encoding via the property {@link RapidMiner#PROPERTY_RAPIDMINER_GENERAL_DEFAULT_ENCODING}
	 * . If this is not possible, this method just returns the default system encoding.
	 */
	public static Charset getDefaultEncoding() {
		Charset result = null;

		if (RapidMiner.getExecutionMode().hasMainFrame()) {
			MainFrame mainFrame = RapidMinerGUI.getMainFrame();
			if (mainFrame != null) {
				com.rapidminer.Process process = mainFrame.getProcess();
				if (process != null) {
					Operator rootOperator = process.getRootOperator();
					if (rootOperator != null) {
						try {
							result = Encoding.getEncoding(rootOperator);
						} catch (UndefinedParameterError e) {
							result = Charset.defaultCharset();
						} catch (UserError e) {
							result = Charset.defaultCharset();
						}
					}
				}
			}
		}

		// try property setting
		if (result == null) {
			String encoding = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEFAULT_ENCODING);
			if (encoding != null && encoding.trim().length() > 0) {
				if (RapidMiner.SYSTEM_ENCODING_NAME.equals(encoding)) {
					result = Charset.defaultCharset();
				} else {
					result = Charset.forName(encoding);
				}
			}
		}

		// still not found? try default charset
		if (result == null) {
			result = Charset.defaultCharset();
		}

		return result;
	}

	/** Returns the relative path of the first file resolved against the second. */
	public static String getRelativePath(File firstFile, File secondFile) throws IOException {
		String canonicalFirstPath = firstFile.getCanonicalPath();
		String canonicalSecondPath = secondFile.getCanonicalPath();

		int minLength = Math.min(canonicalFirstPath.length(), canonicalSecondPath.length());
		int index = 0;
		for (index = 0; index < minLength; index++) {
			if (canonicalFirstPath.charAt(index) != canonicalSecondPath.charAt(index)) {
				break;
			}
		}

		String relPath = canonicalFirstPath;
		int lastSeparatorIndex = canonicalFirstPath.substring(0, index).lastIndexOf(File.separator);
		if (lastSeparatorIndex != -1) {
			String absRest = canonicalSecondPath.substring(lastSeparatorIndex + 1);
			StringBuffer relPathBuffer = new StringBuffer();
			while (absRest.indexOf(File.separator) >= 0) {
				relPathBuffer.append(".." + File.separator);
				absRest = absRest.substring(absRest.indexOf(File.separator) + 1);
			}
			relPathBuffer.append(canonicalFirstPath.substring(lastSeparatorIndex + 1));
			relPath = relPathBuffer.toString();
		}
		return relPath;
	}

	/**
	 * Waits for the required threads to die first. Then waits for the process to die and writes log messages.
	 * Terminates if exit value is not 0. Terminates if the RapidMiner process execution was stopped by the user.
	 *
	 * @param operator
	 * 		The current operator that will be checked for RapidMiner process execution break.
	 * @param process
	 * 		The process that should finish
	 * @param name
	 * 		Processname for logoutput.
	 * @param threadsToBeFinishedFirst
	 * 		The required {@link Thread Threads} to be joined before waiting for the process. Commonly used to read all outputs in those before waiting for the process itself.
	 * @throws OperatorException
	 * 		If the program execution failed the error will be thrown via an UserError.
	 * @author Andreas Timm
	 * @since 8.2
	 */
	public static void waitForDependentProcess(final Operator operator, final Process process, final String name, Thread... threadsToBeFinishedFirst)
			throws OperatorException {
		boolean allThreadsFinished = false;
		while (!allThreadsFinished) {
			allThreadsFinished = true;
			for (Thread t : threadsToBeFinishedFirst) {
				if (!t.isAlive()) {
					continue;
				}
				allThreadsFinished = false;
				try {
					t.join(200);
				} catch (InterruptedException e) {
					try {
						operator.checkForStop();
					} catch (ProcessStoppedException e1) {
						LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.Tools.terminating_process", name);
						process.destroy();
						throw e1;
					}
					Thread.currentThread().interrupt();
				}
			}
		}

		waitForProcess(operator, process, name);
	}

	/**
	 * Waits for process to die and writes log messages. Terminates if exit value is not 0.
	 */
	public static void waitForProcess(final Operator operator, final Process process, final String name)
			throws OperatorException {
		int exitValue = -1;
		try {

			// if operator was provided, start an observer thread
			// that check if the operator was stopped
			if (operator != null) {
				Thread observerThread = new Thread(operator.getName() + "-stop-observer") {

					@Override
					public void run() {
						Integer exitValue = null;
						while (exitValue == null) {
							try {
								Thread.sleep(500);
								exitValue = process.exitValue();
							} catch (IllegalThreadStateException | InterruptedException e) {
								try {
									operator.checkForStop();
								} catch (ProcessStoppedException e1) {
									LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.Tools.terminating_process",
											name);
									process.destroy();
									try {
										exitValue = process.waitFor();
									} catch (InterruptedException e2) {
										// in case of another interrupt, set exit value to error
										exitValue = -1;
									}
								}
							}
						}
					}
				};
				observerThread.setDaemon(true);
				observerThread.start();
			}
			LogService.getRoot().log(Level.ALL, "com.rapidminer.tools.Tools.waiting_for_process", name);
			exitValue = process.waitFor();
		} catch (InterruptedException e) {

			// if process was stopped because user aborted it, re-throw exception
			if (operator != null) {
				operator.checkForStop();
			}

			// if process was stopped because of an error, set exit value to -1
			exitValue = -1;
		}
		if (exitValue == 0) {
			LogService.getRoot().log(Level.FINE, "com.rapidminer.tools.Tools.process_terminated_successfully", name);
		} else {
			throw new UserError(operator, 306, new Object[]{name, exitValue});
		}
	}

	/**
	 * @deprecated Use {@link MailUtilities#sendEmail(String, String, String)} instead
	 */
	@Deprecated
	public static void sendEmail(String address, String subject, String content) {
		MailUtilities.sendEmail(address, subject, content);
	}

	/**
	 * Removes the given resource source. If the resource source was not registered before, does nothing.
	 *
	 * @param source
	 * 		the resource source to remove
	 * @since 9.0.0
	 */
	public static void removeResourceSource(ResourceSource source) {
		ALL_RESOURCE_SOURCES.remove(source);
	}

	/**
	 * Sets a resource source for a plugin. Note that for the same plugin, only the last set resource source will be used!
	 *
	 * @param pluginKey
	 * 		the plugin key
	 * @param source
	 * 		the source
	 * @since 9.0.0
	 */
	public static void setResourceSourceForPlugin(String pluginKey, ResourceSource source) {
		PLUGIN_RESOURCE_SOURCES.put(pluginKey, source);
	}

	/**
	 * Removes the resource source of the given plugin. If the plugin resource source was not registered before, does
	 * nothing.
	 *
	 * @param pluginKey
	 * 		* 		the plugin key for which the resource source should be removed
	 * @since 9.0.0
	 */
	public static void removeResourceSourceForPlugin(String pluginKey) {
		PLUGIN_RESOURCE_SOURCES.remove(pluginKey);
	}

	/** Adds a new resource source. Might be used by plugins etc. */
	public static void addResourceSource(ResourceSource source) {
		ALL_RESOURCE_SOURCES.add(source);
	}

	/** Adds a new resource source before the others. Might be used by plugins etc. */
	public static void prependResourceSource(ResourceSource source) {
		ALL_RESOURCE_SOURCES.add(0, source);
	}

	public static URL getResource(ClassLoader loader, String name) {
		return getResource(loader, RESOURCE_PREFIX, name);
	}

	public static URL getResource(ClassLoader loader, String prefix, String name) {
		return loader.getResource(prefix + name);
	}

	/**
	 * Returns the desired resource. Tries first to find a resource in the core RapidMiner resources
	 * directory. If no resource with the given name is found, it is tried to load with help of the
	 * ResourceSource which might have been added by plugins. Please note that resource names are
	 * only allowed to use '/' as separator instead of File.separator!
	 */
	public static URL getResource(String name) {
		synchronized (ALL_RESOURCE_SOURCES) {
			for (ResourceSource source : ALL_RESOURCE_SOURCES) {
				URL url = source.getResource(name);
				if (url != null) {
					return url;
				}
			}
		}
		synchronized (PLUGIN_RESOURCE_SOURCES) {
			for (ResourceSource pluginSource : PLUGIN_RESOURCE_SOURCES.values()) {
				URL url = pluginSource.getResource(name);
				if (url != null) {
					return url;
				}
			}
		}

		URL resourceURL = getResource(Plugin.getMajorClassLoader(), name);
		if (resourceURL != null) {
			return resourceURL;
		} else {
			return null;
		}
	}

	/**
	 * Return an input stream of the desired resource. Tries first to find a resource in the core
	 * RapidMiner resources directory. If no resource with the given name is found, it is tried to
	 * load with help of the ResourceSource which might have been added by plugins. Please note that
	 * resource names are only allowed to use '/' as separator instead of File.separator!
	 *
	 * @throws IOException
	 * 		if stream cannot be opened
	 * @throws RepositoryException
	 * 		if resource cannot be found
	 */
	public static InputStream getResourceInputStream(String name) throws IOException, RepositoryException {
		URL resourceURL = Tools.getResource(name);
		if (resourceURL == null) {
			throw new RepositoryException("Missing resource " + name);
		}
		return resourceURL.openStream();
	}

	/**
	 * Tries to load the given text file from the resources. If it fails, returns an empty string and logs it. This is
	 * necessary for extensions because {@link #getResource(String)} only looks in {@code
	 * resources/com/rapidminer/resources}.
	 *
	 * @param resourcePath the path, e.g. "com/rapidminer/extension/resources/folder/script.js". The path is treated as
	 *                     an absolute path. If the path contains a version number (which is quite common for HTML
	 *                     resources), it & anything behind it will be stripped as it is not a valid filename. Example:
	 *                     "/com/test/myFile.js?v=4.7.0" will become "/com/test/myFile.js".
	 * @return the stream, never {@code null}. Must be closed by the caller!
	 * @throws FileNotFoundException if the resource cannot be found
	 * @throws IOException           if accessing the resource fails
	 * @since 9.5.0
	 */
	public static InputStream openStreamFromResources(String resourcePath) throws IOException {
		if (resourcePath.startsWith("/")) {
			resourcePath = resourcePath.substring(1);
		}
		// in HTML files, it's quite common to reference a version. That is an invalid file name, so drop that.
		if (resourcePath.contains("?v=")) {
			resourcePath = resourcePath.substring(0, resourcePath.indexOf("?v="));
		}
		URL scriptResource = Plugin.getMajorClassLoader().getResource(resourcePath);
		if (scriptResource == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.Tools.resource_not_found", resourcePath);
			throw new FileNotFoundException("Could not find resource '" + resourcePath + "'");
		}
		return scriptResource.openStream();
	}

	public static String readTextFile(InputStream in) throws IOException {
		return readTextFile(new InputStreamReader(in, "UTF-8"));
	}

	/**
	 * Reads a text file into a single string. Process files created with RapidMiner 5.2.008 or
	 * earlier will be read with the system encoding (for compatibility reasons); all other files
	 * will be read with UTF-8 encoding.
	 */
	public static String readTextFile(File file) throws IOException {
		// due to a bug in pre-5.2.009, process files were stored in System encoding instead of
		// UTF-8. So we have to check the process version, and if it's less than 5.2.009 we have
		// to retrieve the file again with System encoding.
		// If anything goes wrong while parsing the version number, we continue with the old
		// method. If something goes wrong, the file is either not utf-8 encoded (so the old
		// method will probably work), or it is not a valid process file (which will also be
		// detected by the old method).
		boolean useFallback = false;
		try (FileInputStream inStream = new FileInputStream(file)) {

			try {
				DocumentBuilder documentBuilder = XMLTools.createDocumentBuilder();
				Document processXmlDocument = documentBuilder.parse(inStream);
				XPathFactory xPathFactory = XPathFactory.newInstance();
				XPath xPath = xPathFactory.newXPath();
				String versionString = xPath.evaluate("/process/@version", processXmlDocument);
				VersionNumber version = new VersionNumber(versionString);
				if (version.isAtMost(5, 2, 8)) {
					useFallback = true;
				}
			} catch (XPathExpressionException e) {
				useFallback = true;
			} catch (SAXException e) {
				useFallback = true;
			} catch (IOException e) {
				useFallback = true;
			} catch (NumberFormatException e) {
				useFallback = true;
			}
		}

		InputStreamReader reader = null;

		try (FileInputStream inStream = new FileInputStream(file)) {
			if (useFallback) {
				// default reader (as in old versions)
				reader = new InputStreamReader(inStream);
			} else {
				// utf8 reader
				reader = new InputStreamReader(inStream, XMLImporter.PROCESS_FILE_CHARSET);
			}

			return readTextFile(reader);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static String readTextFile(Reader r) throws IOException {
		StringBuilder contents = new StringBuilder();
		BufferedReader reader = new BufferedReader(r);
		String line = "";
		try {
			while ((line = reader.readLine()) != null) {
				contents.append(line + Tools.getLineSeparator());
			}
		} finally {
			reader.close();
		}
		return contents.toString();
	}

	/**
	 * Reads content from the provided input stream.
	 *
	 * @param stream
	 * 		the stream to read content from
	 * @return the content as String
	 * @throws IOException
	 * 		in case something goes wrong
	 */
	public static final String parseInputStreamToString(InputStream stream) throws IOException {
		return parseInputStreamToString(stream, false);
	}

	/**
	 * Reads content from the provided input stream.
	 *
	 * @param stream
	 * 		the stream to read content from
	 * @param html
	 * 		return the string as html with line breaks between the lines
	 * @return the content as String
	 * @throws IOException
	 * 		in case something goes wrong
	 */
	public static final String parseInputStreamToString(InputStream stream, boolean html) throws IOException {
		try (InputStreamReader inputStreamReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
			 BufferedReader reader = new BufferedReader(inputStreamReader);) {

			StringBuilder contentBuilder = new StringBuilder();
			if (html) {
				contentBuilder.append("<html>");
			}
			String line = reader.readLine();
			while (line != null) {
				contentBuilder.append(line);
				if (html) {
					contentBuilder.append("<br/>");
				}
				line = reader.readLine();
			}
			if (html) {
				contentBuilder.append("</html>");
			}
			return contentBuilder.toString();
		}
	}

	public static void writeTextFile(File file, String text) throws IOException {
		// ! THIS IS TO PREVENT A JAVA PROBLEM (BUG?) ON WINDOWS NTFS FILESYSTEM !
		// If the filename is something like C:\path\x:y.z, new FileOutputStream(file) will throw NO
		// error
		// but the result will be an EMPTY file C:\path\x and you never know it failed
		// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4645046
		if (file == null || !canFileBeStoredOnCurrentFilesystem(file.getName())) {
			throw new FileNotFoundException(I18N.getMessage(I18N.getErrorBundle(), "repository.illegal_entry_name",
					file == null ? "null" : file.getName()));
		}

		FileOutputStream outStream = new FileOutputStream(file);
		try {
			if (text != null) {
				outStream.write(text.getBytes(XMLImporter.PROCESS_FILE_CHARSET));
			}
		} finally {
			outStream.close();
		}
	}

	public static final String[] TRUE_STRINGS = {"true", "on", "yes", "y"};

	public static final String[] FALSE_STRINGS = {"false", "off", "no", "n"};

	public static boolean booleanValue(String string, boolean deflt) {
		if (string == null) {
			return deflt;
		}
		string = string.toLowerCase().trim();
		for (String element : TRUE_STRINGS) {
			if (element.equals(string)) {
				return true;
			}
		}
		for (String element : FALSE_STRINGS) {
			if (element.equals(string)) {
				return false;
			}
		}
		return deflt;
	}

	public static File findSourceFile(StackTraceElement e) {
		try {
			Class<?> clazz = Class.forName(e.getClassName());
			while (clazz.getDeclaringClass() != null) {
				clazz = clazz.getDeclaringClass();
			}
			String filename = clazz.getName().replace('.', File.separatorChar);
			return FileSystemService.getSourceFile(filename + ".java");
		} catch (Throwable t) {
		}
		String filename = e.getClassName().replace('.', File.separatorChar);
		return FileSystemService.getSourceFile(filename + ".java");
	}

	public static Process launchFileEditor(File file, int line) throws IOException {
		String editor = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_EDITOR);
		if (editor == null) {
			throw new IOException("Property 'rapidminer.tools.editor' undefined.");
		}
		editor = editor.replaceAll("%f", file.getAbsolutePath());
		editor = editor.replaceAll("%l", line + "");
		return Runtime.getRuntime().exec(editor);
	}

	/** Replaces angle brackets by html entities. */
	public static String escapeXML(String string) {
		if (string == null) {
			return "null";
		}
		return StringEscapeUtils.escapeXml(string);
	}

	/**
	 * This method will encode the given string by replacing all forbidden characters by the
	 * appropriate HTML entity.
	 */
	public static String escapeHTML(String string) {
		return StringEscapeUtils.escapeHtml(string);
	}

	public static void findImplementationsInJar(JarFile jar, Class<?> superClass, List<String> implementations) {
		findImplementationsInJar(Tools.class.getClassLoader(), jar, superClass, implementations);
	}

	public static void findImplementationsInJar(ClassLoader loader, JarFile jar, Class<?> superClass,
												List<String> implementations) {
		String classSuffix = ".class";
		int suffixLength = classSuffix.length();
		jar.stream().map(ZipEntry::getName).filter(n -> n.endsWith(classSuffix))
				.map(n -> n.substring(0, n.length() - suffixLength).replace('/', '.'))
				.map(suppress(loader::loadClass))
				.filter(c -> c != null && superClass.isAssignableFrom(c) && !java.lang.reflect.Modifier.isAbstract(c.getModifiers()))
				.map(Class::getName).forEach(implementations::add);
	}

	/** TODO: Looks like this can be replaced by {@link Plugin#getMajorClassLoader()} */
	public static Class<?> classForName(String className) throws ClassNotFoundException {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
		}
		try {
			return ClassLoader.getSystemClassLoader().loadClass(className);
		} catch (ClassNotFoundException e) {
		}
		Iterator<Plugin> i = Plugin.getAllPlugins().iterator();
		while (i.hasNext()) {
			Plugin p = i.next();
			try {
				return p.getClassLoader().loadClass(className);
			} catch (ClassNotFoundException e) {
				// this wasn't it, so continue
			}
		}
		throw new ClassNotFoundException(className);
	}

	/**
	 * Splits the given line according to the given separator pattern while only those separators
	 * will be regarded not lying inside of a quoting string. Please note that quoting characters
	 * will not be regarded if they are escaped by an escaping character. The usual double quote
	 * (&quot;) is used for quoting and can be escaped by a backslash, i.e. \&quot;.
	 */
	public static String[] quotedSplit(String line, Pattern separatorPattern) {
		return quotedSplit(line, separatorPattern, '"', '\\');
	}

	/**
	 * Splits the given line according to the given separator pattern while only those separators
	 * will be regarded not lying inside of a quoting string. Please note that quoting characters
	 * will not be regarded if they are escaped by an escaping character.
	 */
	public static String[] quotedSplit(String line, Pattern separatorPattern, char quotingChar, char escapeChar) {

		// determine split positions according to non-escaped quotes
		int[] quoteSplitIndices = new int[line.length()];
		char lastChar = '0';
		int lastSplitIndex = -1;
		for (int i = 0; i < line.length(); i++) {
			char currentChar = line.charAt(i);
			if (currentChar == quotingChar) {
				boolean escaped = false;
				if (i != 0 && lastChar == escapeChar) {
					escaped = true;
				}

				if (!escaped) {
					quoteSplitIndices[++lastSplitIndex] = i;
				}
			}
			lastChar = currentChar;
		}

		// add quote parts to a list and replace escape chars
		List<String> quotedSplits = new LinkedList<>();
		if (lastSplitIndex < 0) {
			line = line.replaceAll("\\\\\"", "\""); // remove escape characters
			quotedSplits.add(line);
		} else {
			int start = 0;
			for (int i = 0; i <= lastSplitIndex; i++) {
				int end = quoteSplitIndices[i];
				String part = "";
				if (end > start) {
					part = line.substring(start, end);
				}
				part = part.replaceAll("\\\\\"", "\""); // remove escape characters
				quotedSplits.add(part);
				start = end + 1;
			}
			if (start < line.length()) {
				String part = line.substring(start);
				part = part.replaceAll("\\\\\"", "\""); // remove escape characters
				quotedSplits.add(part);
			}
		}

		// now handle split and non split parts
		// ALGORITHM:
		// *** at Split-Parts: remove empty starts and endings, use empty parts in the middle (as
		// missing), use also
		// non-empty parts (as non missing)
		// - Exception: the first and the last split parts. Here also the start and the beginning
		// must be used even if
		// they are empty (they are missing then)
		// *** at Non-Split-Parts: simply use the whole value. It is missing if is empty.
		// IMPORTANT: a negative limit for the split method (here: -1) is very important in order to
		// get empty trailing
		// values
		List<String> result = new LinkedList<>();
		boolean isSplitPart = true;
		int index = 0;
		for (String part : quotedSplits) {
			if (index > 0 || part.trim().length() > 0) { // skip first split if part is empty
				// (coming from leading
				// quotes in the line)
				if (isSplitPart) {
					String[] separatedParts = separatorPattern.split(part, -1); // ATTENTION: a
					// negative Limit is
					// very
					// important to get trailing empty
					// strings
					for (int s = 0; s < separatedParts.length; s++) {
						String currentPart = separatedParts[s].trim();
						if (currentPart.length() == 0) { // part is empty -- missing if in the
							// middle or at line start
							// or end
							if (s == 0 && index == 0) {
								result.add(currentPart);
							} else if (s == separatedParts.length - 1 && index == quotedSplits.size() - 1) {
								result.add(currentPart);
							} else if (s > 0 && s < separatedParts.length - 1) {
								result.add(currentPart);
							}
						} else {
							result.add(currentPart);
						}
					}
				} else {
					result.add(part);
				}
			}
			isSplitPart = !isSplitPart;
			index++;
		}

		String[] resultArray = new String[result.size()];
		result.toArray(resultArray);
		return resultArray;
	}

	/**
	 * This method merges quoted splits, e.g. if a string line should be splitted by comma and
	 * commas inside of a quoted string should not be used as splitting point.
	 *
	 * @param line
	 * 		the original line
	 * @param splittedTokens
	 * 		the tokens as they were originally splitted
	 * @param quoteString
	 * 		the string which should be used as quote indicator, e.g. &quot; or '
	 * @return the array of strings where the given quoteString was regarded
	 * @throws IOException
	 * 		if an open quote was not ended
	 * @deprecated Please use {@link #quotedSplit(String, Pattern, char, char)} or
	 * {@link #quotedSplit(String, Pattern)} instead
	 */
	@Deprecated
	public static String[] mergeQuotedSplits(String line, String[] splittedTokens, String quoteString) throws IOException {
		int[] tokenStarts = new int[splittedTokens.length];
		int currentCounter = 0;
		int currentIndex = 0;
		for (String currentToken : splittedTokens) {
			tokenStarts[currentIndex] = line.indexOf(currentToken, currentCounter);
			currentCounter = tokenStarts[currentIndex] + currentToken.length() + 1;
			currentIndex++;
		}

		List<String> tokens = new LinkedList<>();
		int start = -1;
		int end = -1;
		for (int i = 0; i < splittedTokens.length; i++) {
			if (splittedTokens[i].trim().startsWith(quoteString)) {
				start = i;
			}
			if (start >= 0) {
				StringBuffer current = new StringBuffer();
				while (end < 0 && i < splittedTokens.length) {
					if (splittedTokens[i].endsWith(quoteString)) {
						end = i;
						break;
					}
					i++;
				}

				if (end < 0) {
					throw new IOException("Error during reading: open quote \" is not ended!");
				}

				String lastToken = null;
				for (int a = start; a <= end; a++) {
					String nextToken = splittedTokens[a];

					if (nextToken.length() == 0) {
						continue;
					}

					if (a == start) {
						nextToken = nextToken.substring(quoteString.length());
					}

					if (a == end) {
						nextToken = nextToken.substring(0, nextToken.length() - quoteString.length());
					}

					// add correct separator
					if (lastToken != null) {
						// int lastIndex = line.indexOf(lastToken, totalCounter -
						// lastToken.length()) +
						// lastToken.length();
						int lastIndex = tokenStarts[a - 1] + lastToken.length();
						int thisIndex = tokenStarts[a];
						if (lastIndex >= 0 && thisIndex >= lastIndex) {
							String separator = line.substring(lastIndex, thisIndex);
							current.append(separator);
						}
					}
					current.append(nextToken);
					lastToken = splittedTokens[a];
				}
				tokens.add(current.toString());
				start = -1;
				end = -1;
			} else {
				tokens.add(splittedTokens[i]);
			}
		}
		String[] quoted = new String[tokens.size()];
		tokens.toArray(quoted);
		return quoted;
	}

	/** Delivers the next token and skip empty lines. */
	public static void getFirstToken(StreamTokenizer tokenizer) throws IOException {
		// skip empty lines
		while (tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
		}
		;

		if (tokenizer.ttype == '\'' || tokenizer.ttype == '"') {
			tokenizer.ttype = StreamTokenizer.TT_WORD;
		} else if (tokenizer.ttype == StreamTokenizer.TT_WORD && tokenizer.sval.equals("?")) {
			tokenizer.ttype = '?';
		}
	}

	/** Delivers the next token and checks if its the end of line. */
	public static void getLastToken(StreamTokenizer tokenizer, boolean endOfFileOk) throws IOException {
		if (tokenizer.nextToken() != StreamTokenizer.TT_EOL && (tokenizer.ttype != StreamTokenizer.TT_EOF || !endOfFileOk)) {
			throw new IOException("expected the end of the line " + tokenizer.lineno());
		}
	}

	/** Delivers the next token and checks for an unexpected end of line or file. */
	public static void getNextToken(StreamTokenizer tokenizer) throws IOException {
		if (tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
			throw new IOException("unexpected end of line " + tokenizer.lineno());
		}

		if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
			throw new IOException("unexpected end of file in line " + tokenizer.lineno());
		} else if (tokenizer.ttype == '\'' || tokenizer.ttype == '"') {
			tokenizer.ttype = StreamTokenizer.TT_WORD;
		} else if (tokenizer.ttype == StreamTokenizer.TT_WORD && tokenizer.sval.equals("?")) {
			tokenizer.ttype = '?';
		}
	}

	/** Skips all tokens before next end of line (EOL). */
	public static void waitForEOL(StreamTokenizer tokenizer) throws IOException {
		// skip everything until EOL
		while (tokenizer.nextToken() != StreamTokenizer.TT_EOL) {
		}
		;
		tokenizer.pushBack();
	}

	/** Deletes the file. If it is a directory, deletes recursively. */
	public static boolean delete(File file) {
		if (file.isDirectory()) {
			boolean success = true;
			File[] files = file.listFiles();
			for (File child : files) {
				success &= delete(child);
				if (!success) {
					return false;
				}
			}
			boolean result = file.delete();
			if (!result) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.Tools.deleting_file_error", file);
				return false;
			}
			return success;
		} else {
			boolean result = file.delete();
			if (!result) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.Tools.deleting_file_error", file);
			}
			return result;
		}
	}

	public static void copy(File srcPath, File dstPath) throws IOException {
		if (srcPath.isDirectory()) {
			if (!dstPath.exists()) {
				boolean result = dstPath.mkdir();
				if (!result) {
					throw new IOException("Unable to create directoy: " + dstPath);
				}
			}

			String[] files = srcPath.list();
			for (String file : files) {
				copy(new File(srcPath, file), new File(dstPath, file));
			}
		} else {
			if (srcPath.exists()) {
				FileChannel in = null;
				FileChannel out = null;
				try (FileInputStream fis = new FileInputStream(srcPath);
					 FileOutputStream fos = new FileOutputStream(dstPath)) {
					in = fis.getChannel();
					out = fos.getChannel();
					long size = in.size();
					MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
					out.write(buf);
				} finally {
					if (in != null) {
						in.close();
					}
					if (out != null) {
						out.close();
					}
				}
			}
		}
	}

	/** Returns a whitespace with length indent. */
	public static String indent(int indent) {
		StringBuffer s = new StringBuffer();
		for (int i = 0; i < indent; i++) {
			s.append(" ");
		}
		return s.toString();
	}

	public static String formatBytes(long numberOfBytes) {
		if (numberOfBytes > 1024 * 1024) {
			long mBytes = numberOfBytes / (1024 * 1024);
			if (mBytes >= 100) {
				return mBytes + " MB";
			} else {
				long remainder = (numberOfBytes - mBytes * 1024 * 1024) / 1024;
				return mBytes + "." + Long.toString(remainder).charAt(0) + " MB";
			}
		} else if (numberOfBytes > 1024) {
			return numberOfBytes / 1024 + " kB";
		} else {
			return numberOfBytes + " bytes";
		}
	}

	/**
	 * Copies the contents read from the input stream to the output stream in the current thread.
	 * Both streams will be closed, even in case of a failure.
	 *
	 * @param closeOutputStream
	 */
	public static void copyStreamSynchronously(InputStream in, OutputStream out, boolean closeOutputStream)
			throws IOException {
		byte[] buffer = new byte[1024 * 20];
		try {
			int length;
			while ((length = in.read(buffer)) != -1) {
				out.write(buffer, 0, length);
			}
			out.flush();
		} finally {
			if (closeOutputStream && out != null) {
				try {
					out.close();
				} catch (IOException ex) {
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
				}
			}
		}

	}

	/** Esacapes quotes, newlines, and backslashes. */
	public static String escape(String unescaped) {
		StringBuilder result = new StringBuilder();
		for (char c : unescaped.toCharArray()) {
			switch (c) {
				case '"':
					result.append("\\\"");
					break;
				case '\\':
					result.append("\\\\");
					break;
				case '\n':
					result.append("\\n");
					break;
				default:
					result.append(c);
					break;
			}
		}
		return result.toString();
	}

	/**
	 * Returns the column name of the the n'th column like excel names it.
	 *
	 * @param index
	 * 		the index of the column
	 * @return
	 */
	public static String getExcelColumnName(int index) {
		if (index < 0) {
			return "error";
		}
		index++;
		StringBuilder builder = new StringBuilder();
		do {
			index--; // adjust column number to offset thinking
			builder.append((char) ('A' + (index % 26)));
			index /= 26;
		} while (index > 0);
		return builder.reverse().toString();
	}

	/**
	 * Replace quote chars in-quote characters by escapeChar+quotingChar
	 *
	 * Example: seperatorPatern = ',' , quotingChar = '"' , escapeCahr = '\\'
	 *
	 * line = '"Charles says: "Some people never go crazy, What truly horrible lives they must live"
	 * ", 1968, " US"' return = '"Charles says: \
	 * "Some people never go crazy, What truly horrible lives they must live\"", "1968", "US"'
	 */
	public static String escapeQuoteCharsInQuotes(String line, Pattern separatorPattern, char quotingChar, char escapeChar,
												  boolean showWarning) {
		// first remember quoteChar positions which should be escaped:
		char lastChar = '0';
		boolean openedQuote = false;

		List<Integer> rememberQuotePosition = new LinkedList<>();
		for (int i = 0; i < line.length(); i++) {
			if (lastChar == quotingChar) {
				if (openedQuote) {
					boolean matches = Pattern.matches(separatorPattern.pattern() + ".*", line.substring(i));
					if (matches) {
						openedQuote = false;
					} else {
						rememberQuotePosition.add(i - 1);
					}

				} else {
					openedQuote = true;
				}
			}
			lastChar = line.charAt(i);
		}
		if (openedQuote && lastChar == quotingChar) {
			openedQuote = false;
		}

		// print warning
		if (showWarning && !rememberQuotePosition.isEmpty()) {

			StringBuilder positions = new StringBuilder();
			int j = 1;
			for (int i = 0; i < rememberQuotePosition.size(); i++) {
				if (j % 10 == 0) {
					positions.append("\n");
				}
				positions.append(rememberQuotePosition.get(i));
				if (i + 1 < rememberQuotePosition.size()) {
					positions.append(", ");
				}
				j++;
			}

			String lineBeginning = line;
			if (line.length() > 20) {
				lineBeginning = line.substring(0, 20);
			}
			String warning = "While reading the line starting with \n\n\t" + lineBeginning + "   ...\n\n"
					+ ",an unescaped quote character was substituted by an escaped quote at the position(s) "
					+ positions.toString() + ". " + "In particular der character '" + Character.toString(lastChar)
					+ "' was replaced by '" + Character.toString(escapeChar) + Character.toString(lastChar) + ".";

			LogService.getRoot().log(Level.WARNING, warning);
		}

		// then build new line:
		if (!rememberQuotePosition.isEmpty()) {
			String newLine = "";
			int pos = rememberQuotePosition.remove(0);
			int i = 0;
			for (Character c : line.toCharArray()) {
				if (i == pos) {
					newLine += Character.toString(escapeChar) + c;
					if (!rememberQuotePosition.isEmpty()) {
						pos = rememberQuotePosition.remove(0);
					}
				} else {
					newLine += c;
				}
				i++;
			}
			line = newLine;
		}
		return line;
	}

	public static String unescape(String escaped) {
		StringBuilder result = new StringBuilder();
		for (int index = 0; index < escaped.length(); index++) {
			char c = escaped.charAt(index);
			switch (c) {
				case '\\':
					if (index < escaped.length() - 1) {
						index++;
						char next = escaped.charAt(index);
						switch (next) {
							case 'n':
								result.append('\n');
								break;
							case '\\':
								result.append('\\');
								break;
							case '"':
								result.append('"');
								break;
							// UGLY: Actually we should throw an exception when encountering
							// undefined escape character
							default:
								result.append('\\').append(next);
						}
					} else {
						result.append('\\');
					}
					break;
				default:
					result.append(c);
					break;
			}
		}
		return result.toString();
	}

	/** As {@link #toString(Collection, String)} with ", ". */
	public static String toString(Collection<?> collection) {
		return toString(collection, ", ");
	}

	/**
	 * Returns a string containing the toString()-representation of the elements of collection,
	 * separated by the given separator.
	 */
	public static String toString(Collection<?> collection, String separator) {
		boolean first = true;
		StringBuilder b = new StringBuilder();
		for (Object o : collection) {
			if (first) {
				first = false;
			} else {
				b.append(separator);
			}
			b.append(o);
		}
		return b.toString();
	}

	public static String toString(Object[] collection) {
		return toString(collection, ", ");
	}

	public static String toString(Object[] collection, String separator) {
		if (collection == null) {
			return null;
		} else {
			return toString(Arrays.asList(collection), separator);
		}
	}

	public static String formatSizeInBytes(long bytes) {
		long result = bytes;
		long rest = 0;
		int unit = 0;
		while (result > 1024) {
			rest = result % 1024;
			result /= 1024;
			unit++;
			if (unit >= Tools.MEMORY_UNITS.length - 1) {
				break;
			}
		}
		if (result < 10 && unit > 0) {
			return result + "." + 10 * rest / 1024 + " " + Tools.MEMORY_UNITS[unit];
		} else {
			return result + " " + Tools.MEMORY_UNITS[unit];
		}
	}

	/**
	 * This method will return a byte array containing the raw data from the given url. Please keep
	 * in mind that in order to load the data, the data will be stored in memory twice.
	 */
	public static byte[] readUrl(URL url) throws IOException {
		return readInputStream(new BufferedInputStream(WebServiceTools.openStreamFromURL(url)));
	}

	/**
	 * This method will return a byte array containing the raw data from the given input stream. The
	 * stream will be closed afterwards in any case.
	 */
	public static byte[] readInputStream(InputStream in) throws IOException {

		try {
			class Part {

				byte[] partData;
				int len;
			}

			LinkedList<Part> parts = new LinkedList<>();
			int len = 1;
			while (len > 0) {
				byte[] data = new byte[1024];
				len = in.read(data);
				if (len > 0) {
					Part part = new Part();
					part.partData = data;
					part.len = len;
					parts.add(part);
				}
			}

			int length = 0;
			for (Part part : parts) {
				length += part.len;
			}

			byte[] result = new byte[length];
			int pos = 0;
			for (Part part : parts) {
				System.arraycopy(part.partData, 0, result, pos, part.len);
				pos += part.len;
			}
			return result;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/** Prefixes every occurrence */
	public static String escape(String source, char escapeChar, char[] specialCharacters) {
		if (source == null) {
			return null;
		}
		StringBuilder b = new StringBuilder();
		for (char c : source.toCharArray()) {
			if (c == escapeChar) {
				b.append(escapeChar); // escape escape character
			} else {
				for (char s : specialCharacters) {
					if (c == s) {
						// escape escape specials
						b.append(escapeChar);
						break;
					}
				}
			}
			b.append(c);
		}
		return b.toString();
	}

	/** Splits the string at every split character unless escaped. */
	public static List<String> unescape(String source, char escapeChar, char[] specialCharacters, char splitCharacter) {
		return unescape(source, escapeChar, specialCharacters, splitCharacter, -1);
	}

	/**
	 * Splits the string at every split character unless escaped. If the split limit is not -1, at
	 * most so many tokens will be returned. No more escaping is performed in the last token!
	 */
	public static List<String> unescape(String source, char escapeChar, char[] specialCharacters, char splitCharacter,
										int splitLimit) {
		List<String> result = new LinkedList<>();
		StringBuilder b = new StringBuilder();
		// was the last character read an escape character?
		boolean readEscape = false;
		int indexCount = -1;
		for (char c : source.toCharArray()) {
			indexCount++;
			// in escape mode -> just write special character, throw exception if not special?
			if (readEscape) {
				boolean found = false;
				if (c == splitCharacter) {
					found = true;
					b.append(c);
				} else if (c == escapeChar) {
					found = true;
					b.append(c);
				} else {
					for (char s : specialCharacters) {
						if (s == c) {
							found = true;
							b.append(c);
							break;
						}
					}
				}
				if (!found) {
					throw new IllegalArgumentException(
							"String '" + source + "' contains illegal escaped character '" + c + "'.");
				}
				// reset to regular mode
				readEscape = false;
			} else if (c == escapeChar) {
				// not in escape mode and read escape character -> go to escape mode
				readEscape = true;
			} else if (c == splitCharacter) {
				// not in escape mode and read split character -> split
				readEscape = false;
				result.add(b.toString());
				if (splitLimit != -1) {
					if (result.size() == splitLimit - 1) {
						// Only one left? Add to result and terminate.
						result.add(source.substring(indexCount + 1));
						return result;
					}
				}
				b = new StringBuilder();
			} else {
				// not in escape mode and read other character -> just write it
				readEscape = false;
				b.append(c);
			}
		}
		result.add(b.toString());
		return result;
	}

	/** In contrast to o1.equals(o2), this method also works with p1==null. */
	public static boolean equals(Object o1, Object o2) {
		if (o1 != null) {
			return o1.equals(o2);
		} else {
			// o1 is null -> return true if o2 is also null
			return o2 == null;
		}

	}

	/**
	 * Iterates over a string an replaces all occurrences of charToMask by '%'. Furthermore all
	 * appearing '%' will be escaped by '\' and all '\' will also be escaped by '\'. To unmask the
	 * resulting string again use {@link #unmask(char, String)}.<br>
	 * Examples (charToMask= '|'):<br>
	 * hello|mandy => hello%mandy<br>
	 * hel\lo|mandy => hel\\lo%mandy<br>
	 * h%l\lo|mandy => h\%l\\lo%mandy<br>
	 *
	 * @param charToMask
	 * 		the character that should be masked. May not be '%' or '\\'
	 */
	public static String mask(char charToMask, String unmasked) {
		if (charToMask == '%' || charToMask == '\\') {
			throw new IllegalArgumentException("Parameter charToMask " + charToMask + " is not allowed!");
		}
		StringBuilder maskedStringBuilder = new StringBuilder();
		char maskChar = '%';
		char escapeChar = '\\'; // this means '\'
		for (char c : unmasked.toCharArray()) {
			if (c == charToMask) {
				maskedStringBuilder.append(maskChar);
			} else if (c == maskChar || c == escapeChar) {
				maskedStringBuilder.append(escapeChar);
				maskedStringBuilder.append(c);
			} else {
				maskedStringBuilder.append(c);
			}
		}

		return maskedStringBuilder.toString();
	}

	/**
	 * Unmaskes a masked string. Examples (charToUnmask= '|'):<br>
	 * hello%mandy => hello|mandy<br>
	 * hel\\lo%mandy => hel\lo|mandy<br>
	 * h\%l\\lo%mandy => h%l\lo|mandy<br>
	 *
	 * @param charToUnmask
	 * 		the char that should be unmasked
	 */
	public static String unmask(char charToUnmask, String masked) {
		if (charToUnmask == '%' || charToUnmask == '\\') {
			throw new IllegalArgumentException("Parameter charToMask " + charToUnmask + " is not allowed!");
		}
		StringBuilder unmaskedStringBuilder = new StringBuilder();
		char maskChar = '%';
		char escapeChar = '\\';
		boolean escapeCharFound = false;
		for (char c : masked.toCharArray()) {
			if (c == maskChar) {
				if (escapeCharFound) {
					unmaskedStringBuilder.append(maskChar);
					escapeCharFound = false;
				} else {
					unmaskedStringBuilder.append(charToUnmask);
				}
			} else if (c == escapeChar) {
				if (escapeCharFound) {
					unmaskedStringBuilder.append(escapeChar);
					escapeCharFound = false;
				} else {
					escapeCharFound = true;
				}
			} else {
				unmaskedStringBuilder.append(c);
			}
		}

		return unmaskedStringBuilder.toString();
	}

	/**
	 * This method tests if a file with the given file name could be stored on the current
	 * filesystem the program is working on. For example, if working on Windows the string
	 * <code>foo:bar</code> would return <code>false</code> (because <code>:</code> is forbidden).
	 * The string <code>foo_bar</code> would return <code>true</code>.
	 *
	 * @param fileName
	 * 		if <code>null</code>, returns <code>false</code>
	 * @return
	 */
	public static boolean canFileBeStoredOnCurrentFilesystem(String fileName) {
		if (fileName == null) {
			return false;
		}
		// check if file contains a ':', because then on windows machines this would lead to
		// C:\1:2.rmp becoming C:\1 without error - but writing operations will fail w/o error!
		// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4645046
		String osName = System.getProperty("os.name");
		boolean checkColon = osName == null ? true : osName.toLowerCase(Locale.ENGLISH).contains("windows") ? true : false;
		if (checkColon && fileName.contains(":")) {
			return false;
		}
		try {
			File file = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);

			if (!file.exists()) {
				file.createNewFile();
				if (file.exists()) {
					file.delete();
					return true;
				} else {
					return false;
				}
			}
		} catch (IOException e) {
			return false;
		} catch (SecurityException e) {
			return false;
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Failed to check filename for illegal characters.", e);
			return false;
		}

		return true;
	}

	/**
	 * Copies the given {@link String} to the system {@link Clipboard}.
	 *
	 * @param s
	 * 		the string to copy to the clipboard
	 */
	public static void copyStringToClipboard(String s) {

		StringSelection stringSelection = new StringSelection(s);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}

	/**
	 * This method checks for possible process circles via "Execute Process". Note that this is just
	 * a heuristic and does not take flow operators such as "Branch" into account. As soon as a
	 * possible circle has been detected, this method will always return {@code true}.
	 *
	 * @param process
	 * 		the root process to check for circles
	 * @return {@code true} if a possible circle has been found; {@code false} otherwise
	 * @since 6.5.0
	 */
	public static boolean doesProcessContainPossibleCircle(com.rapidminer.Process process) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}

		Set<String> dependencySet = new HashSet<>();
		return containsCircle(dependencySet, process);
	}

	private static boolean containsCircle(Set<String> dependencySet, com.rapidminer.Process process) {
		// store process location in set
		String processLoc = process.getProcessLocation().toString();
		dependencySet.add(processLoc);

		for (Operator op : process.getAllOperators()) {
			if (op instanceof ProcessEmbeddingOperator) {
				try {
					RepositoryLocation loc = op
							.getParameterAsRepositoryLocation(ProcessEmbeddingOperator.PARAMETER_PROCESS_FILE);
					com.rapidminer.Process embeddedProcess = loadEmbeddedProcess(loc);
					if (embeddedProcess != null) {
						String embeddedLoc = embeddedProcess.getProcessLocation().toString();
						// if we already have that, we can return now
						if (dependencySet.contains(embeddedLoc)) {
							return true;
						} else {
							dependencySet.add(embeddedLoc);
							return containsCircle(dependencySet, embeddedProcess);
						}
					}
				} catch (UserError e) {
					// ignore as it is not important here
				}
			}
		}

		// if we end up here, no circle has been found in the current process
		return false;
	}

	/**
	 * Tries to load the given process. Will simply return {@code null} if something goes wrong.
	 *
	 * @param location
	 * 		the location from which to load
	 * @return the process or {@code null}
	 */
	private static com.rapidminer.Process loadEmbeddedProcess(RepositoryLocation location) {
		try {
			Entry entry = location.locateEntry();
			if (entry == null) {
				return null;
			} else {
				return new RepositoryProcessLocation(location).load(null);
			}
		} catch (RepositoryException | IOException | XMLException e1) {
			return null;
		}
	}

	private static String getDecimalFormatPattern(int fractionDigits) {
		StringBuilder pattern = new StringBuilder();
		pattern.append('0');
		if (fractionDigits > 0) {
			pattern.append('.');
		}
		for (int i = 0; i < fractionDigits; i++) {
			pattern.append('0');
		}

		return pattern.toString();
	}

	/**
	 * Check if an Operator is contained in a circle via BFS.
	 *
	 * @param operator
	 * 		to be checked if there are connections to the inputports that are connected to operators outputports which are connected to the operators outputports.
	 * @param maxhops
	 * 		maximum amount of operators to look through when trying to check if it is a circle, will return false if maximum was reached. For 0 or less maxhops the default of 250 will be used.
	 * @return if there is a circle
	 * @since 9.2
	 */
	public static boolean isOperatorInCircle(Operator operator, int maxhops) {
		LinkedList<Operator> nextConnectedOperators = new LinkedList<>();
		Set<Operator> visitedOperators = new HashSet<>();
		nextConnectedOperators.add(operator);
		Operator nextOperator;
		int maxAmountVisitedOperators = maxhops > 0 ? maxhops : 250;
		while (!nextConnectedOperators.isEmpty()) {
			nextOperator = nextConnectedOperators.pop();
			for (OutputPort aPort : nextOperator.getOutputPorts().getAllPorts()) {
				if (aPort.isConnected()) {
					final Operator anotherOp = aPort.getDestination().getPorts().getOwner().getOperator();
					if (operator == anotherOp) {
						return true;
					}
					if (!visitedOperators.contains(anotherOp)) {
						nextConnectedOperators.add(anotherOp);
						visitedOperators.add(anotherOp);
					}
					if (--maxAmountVisitedOperators <= 0) {
						nextConnectedOperators.clear();
						visitedOperators.clear();
						break;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Checks if the caller has the {@link PluginSandboxPolicy#RAPIDMINER_INTERNAL_PERMISSION}
	 *
	 * @throws UnsupportedOperationException
	 * 		if the caller is not signed
	 * @since 9.3
	 */
	public static void requireInternalPermission() {
		try {
			if (System.getSecurityManager() != null) {
				AccessController.checkPermission(new RuntimePermission(PluginSandboxPolicy.RAPIDMINER_INTERNAL_PERMISSION));
			}
		} catch (AccessControlException e) {
			throw new UnsupportedOperationException(I18N.getErrorMessage("access_control.no_internal_permission"), e);
		}
	}
}
