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
package com.rapidminer.example.set;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.rapidminer.MacroHandler;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.nio.model.DataResultSet.ValueType;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * The condition is fulfilled if the individual filters are fulfilled. This filter can be
 * constructed from several conditions of the type {@link CustomFilters} which either must all be
 * fulfilled (AND) or only one must be fulfilled (OR).
 *
 * @author Marco Boeck
 */
public class CustomFilter implements Condition {

	/**
	 * Enum for custom filters.
	 */
	public static enum CustomFilters {

		EQUALS_NUMERICAL("gui.comparator.numerical.equals", "eq", Ontology.NUMERICAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				// special case to handle missing values
				if (Double.isNaN(filter)) {
					return Double.isNaN(input);
				}
				return input == filter;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return false;
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		NOT_EQUALS_NUMERICAL("gui.comparator.numerical.not_equals", "ne", Ontology.NUMERICAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				// special case to handle missing values
				if (Double.isNaN(input)) {
					return !Double.isNaN(filter);
				}
				return input != filter;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return false;
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		LESS("gui.comparator.numerical.less", "lt", Ontology.NUMERICAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return input < filter;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return false;
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		LESS_EQUALS("gui.comparator.numerical.less_equals", "le", Ontology.NUMERICAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return input <= filter;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return false;
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		GREATER_EQUALS("gui.comparator.numerical.greater_equals", "ge", Ontology.NUMERICAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return input >= filter;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return false;
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		GREATER("gui.comparator.numerical.greater", "gt", Ontology.NUMERICAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return input > filter;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return false;
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},

		EQUALS_NOMINAL("gui.comparator.nominal.equals", "equals", Ontology.NOMINAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return false;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return input.equals(filter);
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		NOT_EQUALS_NOMINAL("gui.comparator.nominal.not_equals", "does_not_equal", Ontology.NOMINAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return false;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return !input.equals(filter);
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		IS_IN_NOMINAL("gui.comparator.nominal.is_in", "is_in", Ontology.NOMINAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return false;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				List<String> filterList = Tools.unescape(filter, ESCAPE_CHAR, new char[] { SEPERATOR_CHAR }, SEPERATOR_CHAR);
				for (String filterString : filterList) {
					if (input.equals(filterString)) {
						return true;
					}
				}
				return false;
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		IS_NOT_IN_NOMINAL("gui.comparator.nominal.is_not_in", "is_not_in", Ontology.NOMINAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return false;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				List<String> filterList = Tools.unescape(filter, ESCAPE_CHAR, new char[] { SEPERATOR_CHAR }, SEPERATOR_CHAR);
				for (String filterString : filterList) {
					if (input.equals(filterString)) {
						return false;
					}
				}
				return true;
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		CONTAINS("gui.comparator.nominal.contains", "contains", Ontology.NOMINAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return false;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return input.contains(filter);
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		NOT_CONTAINS("gui.comparator.nominal.not_contains", "does_not_contain", Ontology.NOMINAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return false;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return !input.contains(filter);
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		STARTS_WITH("gui.comparator.nominal.starts_with", "starts_with", Ontology.NOMINAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return false;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return input.startsWith(filter);
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		ENDS_WITH("gui.comparator.nominal.ends_with", "ends_with", Ontology.NOMINAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return false;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return input.endsWith(filter);
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		REGEX("gui.comparator.nominal.regex", "matches", Ontology.NOMINAL) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return false;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return input.matches(filter);
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return false;
			}
		},
		MISSING("gui.comparator.special.is_missing", "is_missing", -1) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return false;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return false;
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return Double.isNaN(input);
			}
		},
		NOT_MISSING("gui.comparator.special.is_not_missing", "is_not_missing", -1) {

			@Override
			public boolean isNumericalConditionFulfilled(final double input, final double filter) {
				return false;
			}

			@Override
			public boolean isNominalConditionFulfilled(final String input, final String filter) {
				return false;
			}

			@Override
			public boolean isSpecialConditionFulfilled(final double input) {
				return !Double.isNaN(input);
			}
		};

		/** the symbol to seperate strings for IS_IN and IS_NOT_IN input */
		public static final char SEPERATOR_CHAR = ';';

		/** the symbol to escape seperator symbols in IS_IN and IS_NOT_IN input */
		public static final char ESCAPE_CHAR = '\\';

		/** the format string for date_time */
		public static final String DATE_TIME_FORMAT_STRING = "MM/dd/yyyy h:mm:ss a";

		/** the old (bugged) format string for date_time */
		public static final String DATE_TIME_FORMAT_STRING_OLD = "MM/dd/yy h:mm:ss a";

		/** the format string for date */
		public static final String DATE_FORMAT_STRING = ParameterTypeDateFormat.DATE_FORMAT_MM_DD_YYYY;

		/** the old (bugged) format string for date */
		public static final String DATE_FORMAT_STRING_OLD = "MM/dd/yy";

		/** the format string for time */
		public static final String TIME_FORMAT_STRING = "h:mm:ss a";

		// ThreadLocal because DateFormat is NOT threadsafe and creating a new DateFormat is
		// EXTREMELY expensive
		/** the format for date_time */
		private static final ThreadLocal<DateFormat> FORMAT_DATE_TIME = new ThreadLocal<DateFormat>() {

			@Override
			protected DateFormat initialValue() {
				return new SimpleDateFormat(DATE_TIME_FORMAT_STRING, Locale.ENGLISH);
			}
		};

		/** the old format for date_time */
		private static final ThreadLocal<DateFormat> FORMAT_DATE_TIME_OLD = new ThreadLocal<DateFormat>() {

			@Override
			protected DateFormat initialValue() {
				return new SimpleDateFormat(DATE_TIME_FORMAT_STRING_OLD, Locale.ENGLISH);
			}
		};

		// ThreadLocal because DateFormat is NOT threadsafe and creating a new DateFormat is
		// EXTREMELY expensive
		/** the format for date */
		private static final ThreadLocal<DateFormat> FORMAT_DATE = new ThreadLocal<DateFormat>() {

			@Override
			protected DateFormat initialValue() {
				return new SimpleDateFormat(DATE_FORMAT_STRING, Locale.ENGLISH);
			}
		};

		/** the old format for date */
		private static final ThreadLocal<DateFormat> FORMAT_DATE_OLD = new ThreadLocal<DateFormat>() {

			@Override
			protected DateFormat initialValue() {
				return new SimpleDateFormat(DATE_FORMAT_STRING_OLD, Locale.ENGLISH);
			}
		};

		// ThreadLocal because DateFormat is NOT threadsafe and creating a new DateFormat is
		// EXTREMELY expensive
		/** the format for time */
		private static final ThreadLocal<DateFormat> FORMAT_TIME = new ThreadLocal<DateFormat>() {

			@Override
			protected DateFormat initialValue() {
				return new SimpleDateFormat(TIME_FORMAT_STRING, Locale.ENGLISH);
			}
		};

		/** the label for this filter */
		private String label;

		/** the string representation for this filter */
		private String symbol;

		/** the help text for this filter */
		private String helptext;

		/** the valueType for this filter */
		private int valueType;

		/**
		 * Creates a new {@link CustomFilters} instance which is represented by the specified symbol
		 * and accepts the given {@link ValueType}.
		 *
		 * @param key
		 * @param symbol
		 * @param valueType
		 *            the applicable {@link Ontology#ATTRIBUTE_VALUE_TYPE}. If set to -1, denotes a
		 *            special filter which is not restricted to any value type
		 */
		private CustomFilters(final String key, final String symbol, final int valueType) {
			this.symbol = symbol;
			this.label = I18N.getMessage(I18N.getGUIBundle(), key + ".label");
			this.helptext = I18N.getMessage(I18N.getGUIBundle(), key + ".tip");
			this.valueType = valueType;
		}

		/**
		 * Returns the {@link String} label for this comparator.
		 *
		 * @return
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * Returns the {@link String} representation for this comparator.
		 *
		 * @return
		 */
		public String getSymbol() {
			return symbol;
		}

		/**
		 * Returns the helptext for this comparator.
		 *
		 * @return
		 */
		public String getHelptext() {
			return helptext;
		}

		/**
		 * Returns <code>true</code> if this filter is applicable for numerical values;
		 * <code>false</code> otherwise.
		 *
		 * @return
		 */
		public boolean isNumericalFilter() {
			if (isSpecialFilter()) {
				return false;
			}
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NUMERICAL)) {
				return true;
			}
			return false;
		}

		/**
		 * Returns <code>true</code> if this filter is applicable for nominal values;
		 * <code>false</code> otherwise.
		 *
		 * @return
		 */
		public boolean isNominalFilter() {
			if (isSpecialFilter()) {
				return false;
			}
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NOMINAL)) {
				return true;
			}
			return false;
		}

		/**
		 * Returns <code>true</code> if this filter is a special filter (e.g. missing value filter);
		 * <code>false</code> otherwise. <br/>
		 * These filters are applicable for all attribute types.
		 *
		 * @return
		 */
		public boolean isSpecialFilter() {
			if (valueType == -1) {
				return true;
			}
			return false;
		}

		/**
		 * Returns <code>true</code> if the numerical condition for this filter is fulfilled for the
		 * given value. Returns always <code>false</code> if the condition is for nominal values
		 * only.
		 *
		 * @param input
		 * @param filterValue
		 * @return
		 */
		public abstract boolean isNumericalConditionFulfilled(double input, double filterValue);

		/**
		 * Returns <code>true</code> if the nominal condition for this filter is fulfilled for the
		 * given value. Returns always <code>false</code> if the condition is for numerical values
		 * only.
		 *
		 * @param input
		 * @param filterValue
		 * @return
		 */
		public abstract boolean isNominalConditionFulfilled(String input, String filterValue);

		/**
		 * Returns <code>true</code> if the special condition for this filter is fulfilled for the
		 * given value.
		 *
		 * @param input
		 * @return
		 */
		public abstract boolean isSpecialConditionFulfilled(double input);

		/**
		 * Returns the {@link CustomFilters} matching the given label {@link String}. If none can be
		 * found, returns <code>null</code>.
		 *
		 * @param label
		 * @return
		 */
		public static CustomFilters getByLabel(final String label) {
			for (CustomFilters filter : values()) {
				if (filter.getLabel().equals(label)) {
					return filter;
				}
			}
			return null;
		}

		/**
		 * Returns the {@link CustomFilters} matching the given symbol {@link String}. If none can
		 * be found, returns <code>null</code>.
		 *
		 * @param symbol
		 * @return
		 */
		public static CustomFilters getBySymbol(final String symbol) {
			for (CustomFilters filter : values()) {
				if (filter.getSymbol().equals(symbol)) {
					return filter;
				}
			}
			return null;
		}

		/**
		 * Returns a list of {@link CustomFilters}s for the given {@link ValueType}. Returns an
		 * empty list if no filter was found.
		 *
		 * @param valueType
		 * @return
		 */
		public static List<CustomFilters> getFiltersForValueType(final int valueType) {
			List<CustomFilters> list = new LinkedList<>();
			for (CustomFilters filter : values()) {
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NOMINAL)) {
					// only nominal filters
					if (filter.isSpecialFilter() || filter.isNominalFilter()) {
						list.add(filter);
					}
				} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NUMERICAL)) {
					// only numerical filters
					if (filter.isSpecialFilter() || filter.isNumericalFilter()) {
						list.add(filter);
					}
				} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE_TIME)) {
					// only numerical filters
					if (filter.isSpecialFilter() || filter.isNumericalFilter()) {
						list.add(filter);
					}
				} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.ATTRIBUTE_VALUE)) {
					// unknown value type right now, allow all filters
					list.add(filter);
				} else {
					// filter only defined for numerical or nominal or unknown
				}
			}

			return list;
		}

		/**
		 * Returns a {@link Date} parsed via the date {@link String} or <code>null</code> if the
		 * given string could not be parsed. Uses {@link Locale#ENGLISH}.
		 * <p>
		 * Not static because {@link DateFormat} is NOT threadsafe.
		 * </p>
		 *
		 * @see #FORMAT_DATE_TIME
		 * @param dateTimeString
		 * @return
		 */
		public Date parseDateTime(final String dateTimeString) {
			try {
				return FORMAT_DATE_TIME.get().parse(dateTimeString);
			} catch (ParseException e) {
				return null;
			}
		}

		/**
		 * Old parser for date_time.
		 *
		 * @param dateTimeString
		 * @return
		 */
		private Date parseDateTimeOld(final String dateTimeString) {
			try {
				return FORMAT_DATE_TIME_OLD.get().parse(dateTimeString);
			} catch (ParseException e) {
				return null;
			}
		}

		/**
		 * Returns a {@link Date} parsed via the date {@link String} or <code>null</code> if the
		 * given string could not be parsed. Uses {@link Locale#ENGLISH}.
		 * <p>
		 * Not static because {@link DateFormat} is NOT threadsafe.
		 * </p>
		 *
		 * @see #FORMAT_DATE
		 * @param dateString
		 * @return
		 */
		public Date parseDate(final String dateString) {
			try {
				return FORMAT_DATE.get().parse(dateString);
			} catch (ParseException e) {
				return null;
			}
		}

		/**
		 * Old parser for date.
		 *
		 * @param dateString
		 * @return
		 */
		private Date parseDateOld(final String dateString) {
			try {
				return FORMAT_DATE_OLD.get().parse(dateString);
			} catch (ParseException e) {
				return null;
			}
		}

		/**
		 * Returns a {@link Date} parsed via the date {@link String} or <code>null</code> if the
		 * given string could not be parsed. Uses {@link Locale#ENGLISH}.
		 * <p>
		 * Not static because {@link DateFormat} is NOT threadsafe.
		 * </p>
		 *
		 * @see #FORMAT_TIME
		 * @param timeString
		 * @return
		 */
		public Date parseTime(final String timeString) {
			try {
				return FORMAT_TIME.get().parse(timeString);
			} catch (ParseException e) {
				return null;
			}
		}

		/**
		 * Returns a {@link String} formatted from the {@link Date}. Uses {@link Locale#ENGLISH}.
		 * <p>
		 * Not static because {@link DateFormat} is NOT threadsafe.
		 * </p>
		 *
		 * @see #FORMAT_TIME
		 * @param dateTime
		 * @return
		 */
		public String formatDateTime(final Date dateTime) {
			if (dateTime == null) {
				throw new IllegalArgumentException("dateTime must not be null!");
			}
			return FORMAT_DATE_TIME.get().format(dateTime);
		}

		/**
		 * Old format for date_time.
		 *
		 * @param dateTime
		 * @return
		 */
		public String formatDateTimeOld(final Date dateTime) {
			if (dateTime == null) {
				throw new IllegalArgumentException("dateTime must not be null!");
			}
			return FORMAT_DATE_TIME_OLD.get().format(dateTime);
		}

		/**
		 * Returns a {@link String} formatted from the {@link Date}. Uses {@link Locale#ENGLISH}.
		 * <p>
		 * Not static because {@link DateFormat} is NOT threadsafe.
		 * </p>
		 *
		 * @see #FORMAT_TIME
		 * @param date
		 * @return
		 */
		public String formatDate(final Date date) {
			if (date == null) {
				throw new IllegalArgumentException("date must not be null!");
			}
			return FORMAT_DATE.get().format(date);
		}

		/**
		 * Returns a {@link String} formatted from the {@link Date}. Uses {@link Locale#ENGLISH}.
		 * <p>
		 * Not static because {@link DateFormat} is NOT threadsafe.
		 * </p>
		 *
		 * @see #FORMAT_TIME
		 * @param time
		 * @return
		 */
		public String formatTime(final Date time) {
			if (time == null) {
				throw new IllegalArgumentException("time must not be null!");
			}
			return FORMAT_TIME.get().format(time);
		}
	}

	private static final long serialVersionUID = -1369785656210631292L;

	private static final String WHITESPACE = " ";
	private static final String BACKSLASH = "/";

	private static final int CONDITION_ARRAY_REQUIRED_SIZE = 2;
	private static final int CONDITION_ARRAY_CONDITION_INDEX = 1;

	private static final int CONDITION_TUPEL_REQUIRED_SIZE = 3;
	private static final int CONDITION_TUPEL_ATT_INDEX = 0;
	private static final int CONDITION_TUPEL_FILTER_INDEX = 1;
	private static final int CONDITION_TUPEL_VALUE_INDEX = 2;

	/** the list of all conditions */
	private List<String[]> conditions = new LinkedList<>();

	/**
	 * an array which indicates if for the ordered filter index the old (bugged) date parsing should
	 * be used
	 */
	private boolean[] conditionsOldDateFilter;

	/** the {@link MacroHandler}, will be used to resolve filter values, can be <code>null</code> */
	private MacroHandler macroHandler;

	private boolean fulfillAllConditions;

	/**
	 * Creates a new {@link CustomFilter} instance with the {@link CustomFilters} encoded in the
	 * {@link List} of {@link String} arrays. The {@link Boolean} parameter defines if either all
	 * conditions must be fulfilled or only one of them.
	 *
	 * @param exampleSet
	 * @param conditions
	 * @param fulfillAllConditions
	 * @param macroHandler
	 *            the macro handler which will be used to resolve macros for the filter value, can
	 *            be <code>null</code>
	 * @param version
	 *            can be used to force old behaviour. If not needed and current implementation is
	 *            desired, can be set to <code>null</code>
	 *
	 */
	public CustomFilter(final ExampleSet exampleSet, final List<String[]> conditions, final boolean fulfillAllConditions,
			final MacroHandler macroHandler) {
		if (conditions == null) {
			throw new IllegalArgumentException("typeList must not be null!");
		}

		conditionsOldDateFilter = new boolean[conditions.size()];
		// check if given conditions list is well formed and valid!
		int counter = 0;
		for (String[] conditionArray : conditions) {
			if (conditionArray.length != CONDITION_ARRAY_REQUIRED_SIZE) {
				throw new IllegalArgumentException("conditions must only consist of arrays of length 2!");
			}

			String condition = conditionArray[CONDITION_ARRAY_CONDITION_INDEX];
			String[] conditionTupel = ParameterTypeTupel.transformString2Tupel(condition);
			if (conditionTupel.length != CONDITION_TUPEL_REQUIRED_SIZE) {
				throw new IllegalArgumentException("Malformed condition tupels! Expected size 3 but was "
						+ conditionTupel.length);
			}

			String attName = conditionTupel[CONDITION_TUPEL_ATT_INDEX];
			Attribute att = exampleSet.getAttributes().get(attName);
			String filterSymbol = conditionTupel[CONDITION_TUPEL_FILTER_INDEX];
			CustomFilters filter = CustomFilters.getBySymbol(filterSymbol);
			String filterValue = conditionTupel[CONDITION_TUPEL_VALUE_INDEX];
			if (macroHandler != null) {
				this.macroHandler = macroHandler;
				filterValue = substituteMacros(filterValue, macroHandler);
			}
			if (filter == null) {
				throw new IllegalArgumentException(I18N.getMessageOrNull(I18N.getErrorBundle(),
						"custom_filters.filter_not_found", filterSymbol));
			}
			if (att == null) {
				throw new IllegalArgumentException(I18N.getMessageOrNull(I18N.getErrorBundle(),
						"custom_filters.attribute_not_found", attName));
			}

			// special checks for numerical filters
			if (filter.isNumericalFilter()) {
				// check if attribute is numerical
				if (att.isNominal()) {
					throw new AttributeTypeException(I18N.getMessageOrNull(I18N.getErrorBundle(),
							"custom_filters.numerical_comparator_type_invalid", filter.getLabel(), att.getName()));
				}
				if (att.isDateTime()) {
					// check if filter value works for date attribute
					if (filterValue == null || "".equals(filterValue) || !isStringValidDoubleValue(filter, filterValue, att)) {
						throw new IllegalArgumentException(I18N.getMessageOrNull(I18N.getErrorBundle(),
								"custom_filters.illegal_date_value", filterValue, att.getName()));
					}
				} else if (att.isNumerical()) {
					// check if filter value works for numerical attribute
					if (filterValue == null || "".equals(filterValue) || !isStringValidDoubleValue(filter, filterValue, att)) {
						throw new IllegalArgumentException(I18N.getMessageOrNull(I18N.getErrorBundle(),
								"custom_filters.illegal_numerical_value", filterValue, att.getName()));
					}
				}

				// keep compatibility with processes from versions prior to 6.0.004
				// only affects DATE and DATE_TIME filters
				int yearIndex = filterValue.lastIndexOf(BACKSLASH) + 1;
				int firstWhitespaceIndex = filterValue.indexOf(WHITESPACE);
				String yearString = null;
				if (yearIndex > 0 && firstWhitespaceIndex > 0 && yearIndex < firstWhitespaceIndex) {
					yearString = filterValue.substring(yearIndex, firstWhitespaceIndex);
				}

				// if true, the old (bugged) parsing will be used; otherwise the new yyyy parsing
				// will be used
				conditionsOldDateFilter[counter] = yearString != null && yearString.length() == 2;
			} else if (filter.isNominalFilter()) {
				if (!att.isNominal()) {
					throw new AttributeTypeException(I18N.getMessageOrNull(I18N.getErrorBundle(),
							"custom_filters.nominal_comparator_type_invalid", filter.getLabel(), att.getName()));
				}
			}

			counter++;
		}

		this.conditions = conditions;
		this.fulfillAllConditions = fulfillAllConditions;
	}

	/**
	 * The sole purpose of this constructor is to provide a constructor that matches the expected
	 * signature for the {@link ConditionedExampleSet} reflection invocation. However, this class
	 * cannot be instantiated by an ExampleSet and a String, so we <b>always</b> throw an
	 * {@link IllegalArgumentException} to signal this filter cannot be instantiated that way.
	 *
	 * @throws IllegalArgumentException
	 *             <b>ALWAYS THROWN!</b>
	 */
	@Deprecated
	public CustomFilter(final ExampleSet exampleSet, final String parameterString) throws IllegalArgumentException {
		throw new IllegalArgumentException("This condition cannot be instantiated this way!");
	}

	/**
	 * Since the condition cannot be altered after creation we can just return the condition object
	 * itself.
	 *
	 * @deprecated Conditions should not be able to be changed dynamically and hence there is no
	 *             need for a copy
	 */
	@Deprecated
	@Override
	public Condition duplicate() {
		return this;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (String[] array : conditions) {
			builder.append(Arrays.toString(array));
			builder.append(' ');
		}
		return builder.toString();
	}

	@Override
	public boolean conditionOk(final Example e) {
		boolean conditionsFulfilled = fulfillAllConditions ? true : false;
		int counter = 0;
		for (String[] conditionArray : conditions) {
			// we checked for malformed conditions in the constructor so no need to do it again
			String condition = conditionArray[CONDITION_ARRAY_CONDITION_INDEX];
			String[] conditionTupel = ParameterTypeTupel.transformString2Tupel(condition);
			String attName = conditionTupel[CONDITION_TUPEL_ATT_INDEX];
			Attribute att = e.getAttributes().get(attName);
			String filterSymbol = conditionTupel[CONDITION_TUPEL_FILTER_INDEX];
			CustomFilters filter = CustomFilters.getBySymbol(filterSymbol);
			String filterValue = conditionTupel[CONDITION_TUPEL_VALUE_INDEX];
			if (macroHandler != null) {
				filterValue = substituteMacros(filterValue, macroHandler);
			}

			// check if condition is fulfilled
			boolean fulfilled;
			if (filter.isSpecialFilter()) {
				fulfilled = filter.isSpecialConditionFulfilled(e.getValue(att));
			} else if (filter.isNominalFilter()) {
				fulfilled = filter.isNominalConditionFulfilled(e.getNominalValue(att), filterValue);
			} else {
				fulfilled = checkNumericalCondition(e, att, filter, filterSymbol, filterValue,
						conditionsOldDateFilter[counter]);
			}

			// store result
			if (fulfillAllConditions) {
				conditionsFulfilled &= fulfilled;
			} else {
				conditionsFulfilled |= fulfilled;
			}

			// shortcut for OR - one fulfilled condition is enough
			if (!fulfillAllConditions && conditionsFulfilled) {
				return true;
			}

			counter++;
		}

		return conditionsFulfilled;
	}

	/**
	 * Returns <code>true</code> if the given filter is fulfilled for the given value.
	 *
	 * @param e
	 * @param att
	 * @param filter
	 * @param filterSymbol
	 * @param filterValue
	 * @param oldBehavior
	 *            if <code>true</code>, old, bugged parsing with format dd/MM/yy will be used
	 * @return
	 */
	private boolean checkNumericalCondition(final Example e, final Attribute att, final CustomFilters filter,
			final String filterSymbol, final String filterValue, final boolean oldBehavior) {
		// special handling because we can have DATE_TIME, DATE and TIME strings in human readable
		// format here
		double doubleOriginalValue = e.getValue(att);

		double doubleFilterValue;
		try {
			doubleFilterValue = Double.parseDouble(filterValue);
		} catch (NumberFormatException e1) {
			// if we have a date we are losing precision - therefore we need to convert the original
			// value back and forth once so both lose the same amount of precision -
			// otherwise the filters will not work correctly
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(att.getValueType(), Ontology.DATE)) {
				String formattedOriginal = filter.formatDate(new Date((long) doubleOriginalValue));
				doubleOriginalValue = filter.parseDate(formattedOriginal).getTime();
				// keep compatibility with processes from versions prior to 6.0.004
				if (oldBehavior) {
					// if year consists of 2 chars, use old (bugged) version
					doubleFilterValue = filter.parseDateOld(filterValue).getTime();
				} else {
					// new behavior
					doubleFilterValue = filter.parseDate(filterValue).getTime();
				}
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(att.getValueType(), Ontology.TIME)) {
				String formattedOriginal = filter.formatTime(new Date((long) doubleOriginalValue));
				doubleOriginalValue = filter.parseTime(formattedOriginal).getTime();
				doubleFilterValue = filter.parseTime(filterValue).getTime();
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(att.getValueType(), Ontology.DATE_TIME)) {
				String formattedOriginal = filter.formatDateTime(new Date((long) doubleOriginalValue));
				doubleOriginalValue = filter.parseDateTime(formattedOriginal).getTime();
				// keep compatibility with processes from versions prior to 6.0.004
				if (oldBehavior) {
					// if year consists of 2 chars, use old (bugged) version
					doubleFilterValue = filter.parseDateTimeOld(filterValue).getTime();
				} else {
					// new behavior
					doubleFilterValue = filter.parseDateTime(filterValue).getTime();
				}
			} else {
				// because we have checked the filters in the constructor, this is the only option
				// left
				// special handling for ? as missing value
				doubleFilterValue = Double.NaN;
			}
		}

		return filter.isNumericalConditionFulfilled(doubleOriginalValue, doubleFilterValue);
	}

	/**
	 * Tries to parse the given {@link String} to a {@link Double} and returns <code>true</code> if
	 * successful; <code>false</code> otherwise.
	 *
	 * @param value
	 * @param att
	 * @return
	 */
	private boolean isStringValidDoubleValue(final CustomFilters filter, final String value, final Attribute att) {
		try {
			Double.parseDouble(value);
		} catch (NumberFormatException e1) {
			// if we have a date we are losing precision - therefore we need to convert the original
			// value back and forth once so both lose the same amount of precision -
			// otherwise the filters will not work correctly
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(att.getValueType(), Ontology.DATE)) {
				if (filter.parseDate(value) == null) {
					return false;
				}
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(att.getValueType(), Ontology.TIME)) {
				if (filter.parseTime(value) == null) {
					return false;
				}
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(att.getValueType(), Ontology.DATE_TIME)) {
				if (filter.parseDateTime(value) == null) {
					return false;
				}
			} else {
				if ("?".equals(value)) {
					// special handling for ? as missing value
					return true;
				} else {
					// all parsing tries failed
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Tries to substitute macros with their real value.
	 *
	 * @param value
	 * @param macroHandler
	 * @return
	 */
	private static String substituteMacros(String value, final MacroHandler macroHandler) {
		int startIndex = value.indexOf("%{");
		if (startIndex == -1) {
			return value;
		}
		try {
			StringBuffer result = new StringBuffer();
			while (startIndex >= 0) {
				result.append(value.substring(0, startIndex));
				int endIndex = value.indexOf("}", startIndex + 2);
				String macroString = value.substring(startIndex + 2, endIndex);
				String macroValue = macroHandler.getMacro(macroString);
				if (macroValue != null) {
					result.append(macroValue);
				} else {
					result.append("%{" + macroString + "}");
				}
				value = value.substring(endIndex + 1);
				startIndex = value.indexOf("%{");
			}
			result.append(value);
			return result.toString();
		} catch (Exception e) {
			return value;
		}
	}
}
