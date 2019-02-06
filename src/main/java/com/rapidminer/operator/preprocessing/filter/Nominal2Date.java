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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.container.Range;


/**
 * <p>
 * This operator parses given nominal attributes in order to create date and / or time attributes.
 * The date format can be specified by the date_format parameter. The old nominal attribute will be
 * removed and replaced by a new date attribute if the corresponding parameter is not set (default).
 * </p>
 *
 * <h4>Date and Time Patterns</h4>
 *
 * <p>
 * Date and time formats are specified by <em>date and time pattern</em> strings in the date_format
 * parameter. Within date and time pattern strings, unquoted letters from <code>'A'</code> to
 * <code>'Z'</code> and from <code>'a'</code> to <code>'z'</code> are interpreted as pattern letters
 * representing the components of a date or time string. Text can be quoted using single quotes (
 * <code>'</code>) to avoid interpretation. <code>"''"</code> represents a single quote. All other
 * characters are not interpreted; they're simply copied into the output string during formatting or
 * matched against the input string during parsing.
 * </p>
 *
 * <p>
 * The following pattern letters are defined (all other characters from <code>'A'</code> to
 * <code>'Z'</code> and from <code>'a'</code> to <code>'z'</code> are reserved):
 * </p>
 *
 * <ul>
 * <li><em>G</em>: era designator; Text; example: AD</li>
 * <li><em>y</em>: year; Year; example: 1996; 96</li>
 * <li><em>M</em>: month in year; Month; example: July; Jul; 07</li>
 * <li><em>w</em>: week in year; Number; example: 27</li>
 * <li><em>W</em>: week in month; Number; example: 2</li>
 * <li><em>D</em>: day in year; Number; example: 189</li>
 * <li><em>d</em>: day in month; Number; example: 10</li>
 * <li><em>F</em>: day of week in month; Number; example: 2</li>
 * <li><em>E</em>: day in week; Text; example: Tuesday; Tue</li>
 * <li><em>a</em>: am/pm marker; Text; example: PM</li>
 * <li><em>H</em>: hour in day (0-23); Number; example: 0</li>
 * <li><em>k</em>: hour in day (1-24); Number; example: 24</li>
 * <li><em>K</em>: hour in am / pm (0-11); Number; example: 0</li>
 * <li><em>h</em>: hour in am / pm (1-12); Number; example: 12</li>
 * <li><em>m</em>: minute in hour; Number; example: 30</li>
 * <li><em>s</em>: second in minute; Number; example: 55</li>
 * <li><em>S</em>: millisecond; Number; example: 978</li>
 * <li><em>z</em>: time zone; General Time Zone; example: Pacific Standard Time; PST; GMT-08:00</li>
 * <li><em>Z</em>: time zone; RFC 822 Time Zone; example: -0800</li>
 * </ul>
 *
 * <p>
 * Pattern letters are usually repeated, as their number determines the exact presentation:
 * </p>
 *
 * <ul>
 * <li><em>Text:</em> For formatting, if the number of pattern letters is 4 or more, the full form
 * is used; otherwise a short or abbreviated form is used if available. For parsing, both forms are
 * accepted, independent of the number of pattern letters.</li>
 * <li><em>Number:</em> For formatting, the number of pattern letters is the minimum number of
 * digits, and shorter numbers are zero-padded to this amount. For parsing, the number of pattern
 * letters is ignored unless it's needed to separate two adjacent fields.</li>
 * <li><em>Year:</em> If the underlying calendar is the Gregorian calendar, the following rules are
 * applied.
 *
 * <ul>
 * <li>For formatting, if the number of pattern letters is 2, the year is truncated to 2 digits;
 * otherwise it is interpreted as a <em>number</em>.</li>
 * <li>For parsing, if the number of pattern letters is more than 2, the year is interpreted
 * literally, regardless of the number of digits. So using the pattern "MM/dd/yyyy", "01/11/12"
 * parses to Jan 11, 12 A.D.</li>
 * <li>For parsing with the abbreviated year pattern ("y" or "yy"), this operator must interpret the
 * abbreviated year relative to some century. It does this by adjusting dates to be within 80 years
 * before and 20 years after the time the operator is created. For example, using a pattern of
 * "MM/dd/yy" and the operator created on Jan 1, 1997, the string &quot;01/11/12&quot; would be
 * interpreted as Jan 11, 2012 while the string &quot;05/04/64&quot; would be interpreted as May 4,
 * 1964. During parsing, only strings consisting of exactly two digits will be parsed into the
 * default century. Any other numeric string, such as a one digit string, a three or more digit
 * string, or a two digit string that isn't all digits (for example, &quot;-1&quot;), is interpreted
 * literally. So &quot;01/02/3&quot; or &quot;01/02/003&quot; are parsed, using the same pattern, as
 * Jan 2, 3 AD. Likewise, &quot;01/02/-3&quot; is parsed as Jan 2, 4 BC.</li>
 * </ul>
 *
 * Otherwise, calendar system specific forms are applied. If the number of pattern letters is 4 or
 * more, a calendar specific long form is used. Otherwise, a calendar short or abbreviated form is
 * used.</li>
 *
 * <li><em>Month:</em> If the number of pattern letters is 3 or more, the month is interpreted as
 * <em>text</em>; otherwise, it is interpreted as a <em>number</em>.</li>
 *
 * <li><em>General time zone:</em> Time zones are interpreted as <em>text</em> if they have names.
 * It is possible to define time zones by representing a GMT offset value. RFC 822 time zones are
 * also accepted.</li>
 *
 * <li><em>RFC 822 time zone:</em> For formatting, the RFC 822 4-digit time zone format is used.
 * General time zones are also accepted.</li>
 * </ul>
 *
 * <p>
 * This operator also supports <em>localized date and time
 * pattern</em> strings by defining the locale parameter. In these strings, the pattern letters
 * described above may be replaced with other, locale dependent, pattern letters.
 * </p>
 *
 * <h4>Examples</h4>
 *
 * <p>
 * The following examples show how date and time patterns are interpreted in the U.S. locale. The
 * given date and time are 2001-07-04 12:08:56 local time in the U.S. Pacific Time time zone.
 * </p>
 *
 * <ul>
 * <li><em>&quot;yyyy.MM.dd G 'at' HH:mm:ss z&quot;</em>: 2001.07.04 AD at 12:08:56 PDT</li>
 * <li><em>&quot;EEE, MMM d, ''yy&quot;</em>: Wed, Jul 4, '01</li>
 * <li><em>&quot;h:mm a&quot;</em>: 12:08 PM</li>
 * <li><em>&quot;hh 'o''clock' a, zzzz&quot;</em>: 12 o'clock PM, Pacific Daylight Time</li>
 * <li><em>&quot;K:mm a, z&quot;</em>: 0:08 PM, PDT</li>
 * <li><em>&quot;yyyy.MMMMM.dd GGG hh:mm aaa&quot;</em>: 02001.July.04 AD 12:08 PM</li>
 * <li><em>&quot;EEE, d MMM yyyy HH:mm:ss Z&quot;</em>: Wed, 4 Jul 2001 12:08:56 -0700</li>
 * <li><em>&quot;yyMMddHHmmssZ&quot;</em>: 010704120856-0700</li>
 * <li><em>&quot;yyyy-MM-dd'T'HH:mm:ss.SSSZ&quot;</em>: 2001-07-04T12:08:56.235-0700</li>
 * </ul>
 *
 * @author Ingo Mierswa
 */
public class Nominal2Date extends AbstractDateDataProcessing {

	private static final String ATTRIBUTE_NAME_POSTFIX = "_old";

	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	public static final String PARAMETER_DATE_TYPE = "date_type";

	/**
	 * @deprecated since 8.2; use {@link ParameterTypeDateFormat#PARAMETER_DATE_FORMAT} instead.
	 */
	@Deprecated
	public static final String PARAMETER_DATE_FORMAT = ParameterTypeDateFormat.PARAMETER_DATE_FORMAT;

	public static final String PARAMETER_TIME_ZONE = "time_zone";

	public static final String PARAMETER_LOCALE = "locale";

	public static final String PARAMETER_KEEP_OLD_ATTRIBUTE = "keep_old_attribute";

	public static final String[] VALUE_TYPES = { "date", "time", "date_time" };

	public static final int DATE = 0;
	public static final int TIME = 1;
	public static final int DATE_TIME = 2;

	public Nominal2Date(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(
						this, PARAMETER_ATTRIBUTE_NAME)));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		// testing the date format
		DateFormat format = null;
		int localeIndex = getParameterAsInt(PARAMETER_LOCALE);
		Locale selectedLocale = Locale.US;
		if (localeIndex >= 0 && localeIndex < availableLocales.size()) {
			selectedLocale = availableLocales.get(getParameterAsInt(PARAMETER_LOCALE));
		}
		try {
			format = ParameterTypeDateFormat.createCheckedDateFormat(this, selectedLocale, true);
		} catch (UserError userError) {
			// will not happen because of setup error
		}
		// attribute: change type
		AttributeMetaData amd = metaData.getAttributeByName(getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		if (amd != null) {
			AttributeMetaData newAttribute = amd.clone();
			int dateType = getParameterAsInt(PARAMETER_DATE_TYPE);
			int valueType = Ontology.DATE_TIME;
			if (dateType == TIME) {
				valueType = Ontology.TIME;
			} else if (dateType == DATE) {
				valueType = Ontology.DATE;
			}

			newAttribute.setType(valueType);
			newAttribute.getMean().setUnkown();
			if (format == null) {
				newAttribute.setValueSetRelation(SetRelation.UNKNOWN);
			} else {
				if (amd.getValueSet() != null) {
					try {
						long min = Long.MAX_VALUE;
						long max = Long.MIN_VALUE;
						for (String value : amd.getValueSet()) {
							Date date = format.parse(value);
							long millis = date.getTime();
							if (millis < min) {
								min = millis;
							}
							if (millis > max) {
								max = millis;
							}
						}
						newAttribute.setValueRange(new Range(min, max), amd.getValueSetRelation());
					} catch (ParseException e) {
						getExampleSetInputPort().addError(
								new SimpleMetaDataError(Severity.WARNING, getExampleSetInputPort(), "cannot_parse_date", e
										.getLocalizedMessage()));
						newAttribute.setValueSetRelation(SetRelation.UNKNOWN);
					}
				} else {
					newAttribute.setValueSetRelation(SetRelation.UNKNOWN);
				}
			}

			if (!getParameterAsBoolean(PARAMETER_KEEP_OLD_ATTRIBUTE)) {
				metaData.removeAttribute(amd);
			} else {
				amd.setName(amd.getName() + ATTRIBUTE_NAME_POSTFIX);
			}

			metaData.addAttribute(newAttribute);
		}

		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		String attributeName = getParameterAsString(PARAMETER_ATTRIBUTE_NAME);
		Attribute oldAttribute = exampleSet.getAttributes().get(attributeName);
		if (oldAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME, attributeName);
		}

		String dateFormat = getParameterAsString(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT);
		int dateType = getParameterAsInt(PARAMETER_DATE_TYPE);
		int localeIndex = getParameterAsInt(PARAMETER_LOCALE);
		Locale selectedLocale = Locale.US;
		if (localeIndex >= 0 && localeIndex < availableLocales.size()) {
			selectedLocale = availableLocales.get(getParameterAsInt(PARAMETER_LOCALE));
		}

		int valueType = Ontology.DATE_TIME;
		if (dateType == TIME) {
			valueType = Ontology.TIME;
		} else if (dateType == DATE) {
			valueType = Ontology.DATE;
		}

		// oldAttribute.setName(oldAttribute.getName()+"_old");
		Attribute newAttribute = AttributeFactory.createAttribute(valueType);
		exampleSet.getExampleTable().addAttribute(newAttribute);
		exampleSet.getAttributes().addRegular(newAttribute);

		// parser can not be null;  either the pattern specified is working or a UserError is thrown
		SimpleDateFormat parser = ParameterTypeDateFormat.createCheckedDateFormat(this, selectedLocale, false);
		parser.setTimeZone(Tools.getTimeZone(getParameterAsInt(PARAMETER_TIME_ZONE)));

		int row = 1;
		for (Example e : exampleSet) {
			if (Double.isNaN(e.getValue(oldAttribute))) {
				e.setValue(newAttribute, Double.NaN);
			} else {
				String oldValue = e.getValueAsString(oldAttribute);
				Date date = null;
				try {
					date = parser.parse(oldValue);
				} catch (ParseException e1) {
					throw new UserError(this, 931, dateFormat, oldAttribute.getName(), row, e1.getMessage().replaceAll("\"",
							"\'"));
				}
				if (dateType == TIME) {
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(date);
					calendar.set(1970, 1, 1);
					e.setValue(newAttribute, calendar.getTimeInMillis());
				} else {
					e.setValue(newAttribute, date.getTime());
				}
			}
			row++;
		}

		if (!getParameterAsBoolean(PARAMETER_KEEP_OLD_ATTRIBUTE)) {
			exampleSet.getAttributes().remove(oldAttribute);
		} else {
			oldAttribute.setName(attributeName + ATTRIBUTE_NAME_POSTFIX);
		}
		newAttribute.setName(attributeName);

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterTypeAttribute attributeParamType = new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME,
				"The attribute which should be parsed.", getExampleSetInputPort(), false, false, Ontology.NOMINAL);
		types.add(attributeParamType);

		ParameterType type = new ParameterTypeCategory(PARAMETER_DATE_TYPE,
				"The desired value type for the parsed attribute.", VALUE_TYPES, DATE);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeDateFormat(attributeParamType, getExampleSetInputPort()));

		type = new ParameterTypeCategory(PARAMETER_TIME_ZONE,
				"The time zone used for the date objects if not specified in the date string itself.",
				Tools.getAllTimeZones(), Tools.getPreferredTimeZoneIndex());
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_LOCALE,
				"The used locale for date texts, for example \"Wed\" (English) in contrast to \"Mi\" (German).",
				availableLocaleNames, defaultLocale);
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_OLD_ATTRIBUTE,
				"Indicates if the original date attribute should be kept.", false));

		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), Nominal2Date.class, null);
	}
}
