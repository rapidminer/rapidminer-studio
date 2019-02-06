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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;


/**
 * <p>
 * This operator tries to parse numerical values and formats them in the specified number format. It
 * also supports different kinds of numbers, including integers (123), fixed-point numbers (123.4),
 * scientific notation (1.23E4), percentages (12%), and currency amounts ($123). The format type
 * parameter specifies the basic format, in all cases but for &quot;pattern&quot; the specified
 * locale will be used. In case of pattern the locale is ignored and the specified pattern is used
 * instead.
 * </p>
 * 
 * <p>
 * Please note that this operator only works on numerical attributes and the result will be in any
 * case a nominal attribute no matter if the resulting format would again be a parsable number.
 * </p>
 * 
 * <p>
 * In case of the pattern format type, a pattern parameter is used to define the format. If two
 * different formats for positive and negative numbers should be used, those formats can be defined
 * by a separating ';'. The pattern must have the following structure: <br />
 * <br />
 * 
 * pattern := subpattern{;subpattern} <br />
 * subpattern := {prefix}integer{.fraction}{suffix} <br />
 * prefix := any character combination including white space <br />
 * suffix := any character combination including white space <br />
 * integer := '#'* '0'* '0' <br />
 * fraction := '0'* '#'* <br />
 * </p>
 * 
 * <p>
 * The following placeholders can be used within the pattern parameter: <br />
 * <br />
 * 
 * 0 &nbsp;&nbsp;a digit <br />
 * # &nbsp;&nbsp;a digit, zero shows as absent <br />
 * . &nbsp;&nbsp;placeholder for decimal separator <br />
 * , &nbsp;&nbsp;placeholder for grouping separator. <br />
 * E &nbsp;&nbsp;separates mantissa and exponent for exponential formats. <br />
 * - &nbsp;&nbsp;default negative prefix. <br />
 * % &nbsp;&nbsp;multiply by 100 and show as percentage <br />
 * X &nbsp;&nbsp;any other characters can be used in the prefix or suffix <br />
 * ' &nbsp;&nbsp;used to quote special characters in a prefix or suffix. <br />
 * </p>
 * 
 * @author Mierswa
 */
public class NumericToFormattedNominal extends NumericToNominal {

	public static final String PARAMETER_FORMAT_TYPE = "format_type";

	public static final String PARAMETER_PATTERN = "pattern";

	public static final String PARAMETER_LOCALE = "locale";

	public static final String PARAMETER_USE_GROUPING = "use_grouping";

	public static final String[] FORMAT_TYPES = new String[] { "number", "integer", "currency", "percent", "pattern" };

	public static final int FORMAT_TYPE_NUMBER = 0;
	public static final int FORMAT_TYPE_INTEGER = 1;
	public static final int FORMAT_TYPE_CURRENCY = 2;
	public static final int FORMAT_TYPE_PERCENT = 3;
	public static final int FORMAT_TYPE_PATTERN = 4;

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

	private NumberFormat numberFormat;

	public NumericToFormattedNominal(OperatorDescription description) {
		super(description);
	}

	@Override
	public void init() throws OperatorException {
		int localeIndex = getParameterAsInt(PARAMETER_LOCALE);
		Locale selectedLocale = Locale.US;
		if ((localeIndex >= 0) && (localeIndex < availableLocales.size())) {
			selectedLocale = availableLocales.get(getParameterAsInt(PARAMETER_LOCALE));
		}

		int formatType = getParameterAsInt(PARAMETER_FORMAT_TYPE);
		switch (formatType) {
			case FORMAT_TYPE_NUMBER:
				this.numberFormat = NumberFormat.getNumberInstance(selectedLocale);
				break;
			case FORMAT_TYPE_INTEGER:
				this.numberFormat = NumberFormat.getIntegerInstance(selectedLocale);
				break;
			case FORMAT_TYPE_CURRENCY:
				this.numberFormat = NumberFormat.getCurrencyInstance(selectedLocale);
				break;
			case FORMAT_TYPE_PERCENT:
				this.numberFormat = NumberFormat.getPercentInstance(selectedLocale);
				break;
			case FORMAT_TYPE_PATTERN:
				String formatString = getParameterAsString(PARAMETER_PATTERN);
				// the following line only works for Java Versions >= 6
				// this.numberFormat = new DecimalFormat(formatString,
				// DecimalFormatSymbols.getInstance(selectedLocale));
				this.numberFormat = new DecimalFormat(formatString, new DecimalFormatSymbols(selectedLocale));
				break;
		}

		this.numberFormat.setGroupingUsed(getParameterAsBoolean(PARAMETER_USE_GROUPING));
	}

	@Override
	public void cleanUp() throws OperatorException {
		this.numberFormat = null;
	}

	@Override
	protected void setValue(Example example, Attribute newAttribute, double value) {
		if (Double.isNaN(value)) {
			example.setValue(newAttribute, Double.NaN);
		} else {
			String newValue = this.numberFormat.format(value);
			example.setValue(newAttribute, newAttribute.getMapping().mapString(newValue));
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeCategory(PARAMETER_FORMAT_TYPE,
				"Number formatting will be performed according to the selected type.", FORMAT_TYPES, FORMAT_TYPE_NUMBER);
		types.add(type);

		type = new ParameterTypeString(PARAMETER_PATTERN, "The format string, e.g. '0.###E0 m/s'.", true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_FORMAT_TYPE, FORMAT_TYPES, true,
				FORMAT_TYPE_PATTERN));
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_LOCALE,
				"The used locale for date texts, for example \"Wed\" (English) in contrast to \"Mi\" (German).",
				availableLocaleNames, defaultLocale);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(
				PARAMETER_USE_GROUPING,
				"Indicates if a grouping character should be used for larger numbers (e.g. ',' for the US or '.' for Germany).",
				false);
		type.setExpert(false);
		types.add(type);

		return types;
	}

	@Override
	protected int getGeneratedAttributevalueType() {
		return Ontology.NOMINAL;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				NumericToFormattedNominal.class, null);
	}
}
