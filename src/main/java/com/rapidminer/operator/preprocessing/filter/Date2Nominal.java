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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeParameterPrecondition;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.Tools;


/**
 * <p>
 * This operator transforms the specified date attribute and writes a new nominal attribute in a
 * user specified format. This might be useful for time base OLAP to change the granularity of the
 * time stamps from day to week or month.
 * </p>
 *
 * <p>
 * The date format can be specified by the date_format parameter like described in the following.
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
public class Date2Nominal extends AbstractDateDataProcessing {

	private static final String ATTRIBUTE_NAME_POSTFIX = "_nominal";

	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	/**
	 * @deprecated since 8.2; use {@link ParameterTypeDateFormat#PARAMETER_DATE_FORMAT} instead.
	 */
	@Deprecated
	public static final String PARAMETER_DATE_FORMAT = ParameterTypeDateFormat.PARAMETER_DATE_FORMAT;

	public static final String PARAMETER_TIME_ZONE = "time_zone";

	public static final String PARAMETER_LOCALE = "locale";

	public static final String PARAMETER_KEEP_OLD_ATTRIBUTE = "keep_old_attribute";

	/** The last version that removed the special role of the selected attribute */
	public static final OperatorVersion VERSION_DOES_NOT_KEEP_ROLE = new OperatorVersion(8, 1, 2);

	public Date2Nominal(OperatorDescription description) {
		super(description);
		getExampleSetInputPort().addPrecondition(
				new AttributeParameterPrecondition(getExampleSetInputPort(), this, PARAMETER_ATTRIBUTE_NAME,
						Ontology.DATE_TIME));
		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(
						this, PARAMETER_ATTRIBUTE_NAME)));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		AttributeMetaData amd = metaData.getAttributeByName(getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		if (amd != null) {
			AttributeMetaData newAttribute = new AttributeMetaData(amd.getName(), amd.getValueType(), amd.getRole());
			newAttribute.setType(Ontology.NOMINAL);
			newAttribute.getMean().setUnkown();
			HashSet<String> valueSet = new HashSet<>();
			if (amd.getValueRange() != null) {
				int localeIndex = getParameterAsInt(PARAMETER_LOCALE);
				Locale selectedLocale = Locale.US;
				if (localeIndex >= 0 && localeIndex < availableLocales.size()) {
					selectedLocale = availableLocales.get(getParameterAsInt(PARAMETER_LOCALE));
				}

				SimpleDateFormat parser = null;
				try {
					parser = ParameterTypeDateFormat.createCheckedDateFormat(this, selectedLocale, true);
				} catch (UserError userError) {
					// will not happen because of setup error
				}
				if (parser == null) {
					return metaData;
				}

				Date date = new Date((long) amd.getValueRange().getLower());
				String newDateStr = parser.format(date);
				valueSet.add(newDateStr);

				date = new Date((long) amd.getValueRange().getUpper());
				newDateStr = parser.format(date);
				valueSet.add(newDateStr);
			}

			newAttribute.setValueSet(valueSet, SetRelation.SUPERSET);
			if (!getParameterAsBoolean(PARAMETER_KEEP_OLD_ATTRIBUTE)) {
				metaData.removeAttribute(amd);
				if (getCompatibilityLevel().isAbove(VERSION_DOES_NOT_KEEP_ROLE)) {
					newAttribute.setRole(amd.getRole());
				}
			} else {
				newAttribute.setName(newAttribute.getName() + ATTRIBUTE_NAME_POSTFIX);
			}
			metaData.addAttribute(newAttribute);
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		String attributeName = getParameterAsString(PARAMETER_ATTRIBUTE_NAME);
		Attribute dateAttribute = exampleSet.getAttributes().get(attributeName);
		if (dateAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME, getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		}

		int valueType = dateAttribute.getValueType();
		if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE_TIME)) {
			throw new UserError(this, 218, dateAttribute.getName(), Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(valueType));
		}

		Attribute newAttribute = AttributeFactory.createAttribute(Ontology.NOMINAL);
		exampleSet.getExampleTable().addAttribute(newAttribute);
		exampleSet.getAttributes().addRegular(newAttribute);

		int localeIndex = getParameterAsInt(PARAMETER_LOCALE);
		Locale selectedLocale = Locale.US;
		if (localeIndex >= 0 && localeIndex < availableLocales.size()) {
			selectedLocale = availableLocales.get(getParameterAsInt(PARAMETER_LOCALE));
		}
		SimpleDateFormat parser = ParameterTypeDateFormat.createCheckedDateFormat(this, selectedLocale, false);
		parser.setTimeZone(Tools.getTimeZone(getParameterAsInt(PARAMETER_TIME_ZONE)));

		for (Example example : exampleSet) {
			if (Double.isNaN(example.getValue(dateAttribute))) {
				example.setValue(newAttribute, Double.NaN);
			} else {
				Date date = new Date((long) example.getValue(dateAttribute));
				String newDateStr = parser.format(date);
				example.setValue(newAttribute, newAttribute.getMapping().mapString(newDateStr));
			}
		}

		if (!getParameterAsBoolean(PARAMETER_KEEP_OLD_ATTRIBUTE)) {
			AttributeRole dateAttributeRole = exampleSet.getAttributes().getRole(dateAttribute);
			exampleSet.getAttributes().remove(dateAttribute);
			newAttribute.setName(attributeName);
			if (dateAttributeRole.isSpecial() && getCompatibilityLevel().isAbove(VERSION_DOES_NOT_KEEP_ROLE)) {
				exampleSet.getAttributes().getRole(newAttribute).setSpecial(dateAttributeRole.getSpecialName());
			}
		} else {
			newAttribute.setName(attributeName + ATTRIBUTE_NAME_POSTFIX);
		}
		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterTypeAttribute attributeParamType = new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME,
				"The attribute which should be parsed.", getExampleSetInputPort(), false, false, Ontology.DATE_TIME);
		types.add(attributeParamType);
		ParameterType type = new ParameterTypeDateFormat(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT,
				"The output format of the date values, for example \"yyyy/MM/dd\".", false);
		types.add(type);

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
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), Date2Nominal.class, null);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] changes = super.getIncompatibleVersionChanges();
		changes = Arrays.copyOf(changes, changes.length + 1);
		changes[changes.length - 1] = VERSION_DOES_NOT_KEEP_ROLE;
		return changes;
	}

}
