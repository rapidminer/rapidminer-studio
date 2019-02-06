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
package com.rapidminer.tools.expression.internal.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;

import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for conversion functions.
 *
 * @author Marcel Seifert
 *
 */
public class AntlrParserConversionTest extends AntlrParserTest {

	/**
	 * String to Numerical tests
	 */
	@Test
	public void parse() {
		try {
			Locale locale = new Locale(String.valueOf("de"));
			Date date = new Date(4156111665112L);
			Calendar cal = Calendar.getInstance(locale);
			cal.setTime(date);

			Expression expression = getExpressionWithFunctionContext("parse(\"4711\")");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(4711, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void parseEmptyOrInvalid() {
		try {
			Expression expression = getExpressionWithFunctionContext("parse(\"\")");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void parseNoArg() {
		try {
			getExpressionWithFunctionContext("parse()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void parseWrongType() {
		try {
			getExpressionWithFunctionContext("parse(777)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void parseMoreArgs() {
		try {
			getExpressionWithFunctionContext("parse(\"4711\", \"333\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void parseMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("parse(MISSING_NOMINAL)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void parseMissingDate() {
		try {
			getExpressionWithFunctionContext("parse(MISSING_DATE)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	/**
	 * Numerical to String tests
	 */

	@Test
	public void strInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("str(4711)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("4711", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void strDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("str(4711.7)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("4711.700", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void strInfinity() {
		try {
			Expression expression = getExpressionWithFunctionContext("str(INFINITY)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("\u221E", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void strInfinityParse() {
		try {
			Expression expression = getExpressionWithFunctionContext("parse(str(INFINITY))");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void strMinusInfinity() {
		try {
			Expression expression = getExpressionWithFunctionContext("str(-INFINITY)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("-\u221E", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void strMinusInfinityParse() {
		try {
			Expression expression = getExpressionWithFunctionContext("parse(str(-INFINITY))");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NEGATIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void strWrongType() {
		try {
			getExpressionWithFunctionContext("str(\"abc\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void strNoArg() {
		try {
			getExpressionWithFunctionContext("str()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void strMoreArgs() {
		try {
			getExpressionWithFunctionContext("str(1, 2)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void strMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("str(MISSING_NUMERIC)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void strMissingDate() {
		try {
			getExpressionWithFunctionContext("str(MISSING_DATE)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	/**
	 * DateParse tests
	 */

	@Test
	public void dateParse() {
		try {
			Date date = new Date(4156111665112L);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);

			Expression expression = getExpressionWithFunctionContext("date_parse(4156111665112)");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(cal.getTime(), expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseNominal() {
		try {
			Date date = new Date(707781600000L);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);

			Expression expression = getExpressionWithFunctionContext("date_parse(\"6/6/92\")");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(cal.getTime(), expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseInvalid() {
		try {
			getExpressionWithFunctionContext("date_parse(\"4156111665112\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseNoArg() {
		try {
			getExpressionWithFunctionContext("date_parse()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseMoreArgs() {
		try {
			getExpressionWithFunctionContext("date_parse(4156111665112, 123456789123)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseMissingNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_parse(MISSING_NUMERIC)");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseMissingNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_parse(MISSING_NOMINAL)");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseMissingDate() {
		try {
			getExpressionWithFunctionContext("date_parse(MISSING_DATE)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	/**
	 * DateParseWithLocale tests
	 */
	@Test
	public void dateParseLoc() {
		try {
			Locale locale = new Locale("en");
			Date date = new Date(4156111665112L);
			Calendar cal = Calendar.getInstance(locale);
			cal.setTime(date);

			Expression expression = getExpressionWithFunctionContext("date_parse_loc(4156111665112, \"en\")");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(cal.getTime(), expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseLocNominal() {
		try {
			Locale locale = new Locale("en");
			Date date = new Date(707781600000L);
			Calendar cal = Calendar.getInstance(locale);
			cal.setTime(date);

			Expression expression = getExpressionWithFunctionContext("date_parse_loc(\"6/6/92\", \"en\")");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(cal.getTime(), expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseLocInvalid() {
		try {
			getExpressionWithFunctionContext("date_parse_loc(\"4156111665112\", \"en\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseLocWrongType() {
		try {
			getExpressionWithFunctionContext("date_parse_loc(\"4156111665112\", 777)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseLocNoArg() {
		try {
			getExpressionWithFunctionContext("date_parse_loc()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseLocMoreArgs() {
		try {
			getExpressionWithFunctionContext("date_parse_loc(4156111665112, \"en\", 777)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseLocLessArgs() {
		try {
			getExpressionWithFunctionContext("date_parse_loc(4156111665112)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseLocMissingNumericDate() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_parse_loc(MISSING_NUMERIC, \"en\")");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseLocMissingNominalDate() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_parse_loc(MISSING_NOMINAL, \"en\")");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseLocMissingDateDate() {
		try {
			getExpressionWithFunctionContext("date_parse_loc(MISSING_DATE, \"en\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseLocNumericDateMissingNominalLocale() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_parse_loc(4156111665112, MISSING_NOMINAL)");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			System.out.println(e.getMessage());
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseLocNominalDateMissingNominalLocale() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_parse_loc(\"6/6/92\", MISSING_NOMINAL)");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseLocMissingDateLocale() {
		try {
			getExpressionWithFunctionContext("date_parse_loc(4156111665112, MISSING_DATE)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	/**
	 * DateParseCustom tests
	 */

	@Test
	public void dateParseCustom() {
		try {
			Locale locale = new Locale(String.valueOf("en"));
			Calendar cal = Calendar.getInstance(locale);
			cal.setTimeInMillis(1378072800000L); // // September 2, 2013
			Expression expression = getExpressionWithFunctionContext("date_parse_custom(\"September 2, 2013\", \"MMMM d, yyyy\", \"en\")");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(cal.getTime().compareTo(expression.evaluateDate()), 0);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomTwoArgs() {
		try {
			Locale locale = Locale.getDefault();
			Calendar cal = Calendar.getInstance(locale);
			cal.setTimeInMillis(1378072800000L); // // September 2, 2013
			Expression expression = getExpressionWithFunctionContext("date_parse_custom(\"September 2, 2013\", \"MMMM d, yyyy\")");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(cal.getTime().compareTo(expression.evaluateDate()), 0);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomInvalid() {
		try {
			getExpressionWithFunctionContext("date_parse_custom(1378072800000, \"MMMM d, yyyy\", \"en\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomWrongType1() {
		try {
			getExpressionWithFunctionContext("date_parse_custom(123456789, \"MMMM d, yyyy\", \"en\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomWrongType2() {
		try {
			getExpressionWithFunctionContext("date_parse_custom(\"September 2, 2013\", 1256, \"en\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomWrongType3() {
		try {
			getExpressionWithFunctionContext("date_parse_custom(\"September 2, 2013\", \"MMMM d, yyyy\", 777)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomNoArg() {
		try {
			getExpressionWithFunctionContext("date_parse_custom()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomMoreArgs() {
		try {
			getExpressionWithFunctionContext("date_parse_custom(\"September 2, 2013\", \"MMMM d, yyyy\", \"en\", 123)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomLessArgs() {
		try {
			getExpressionWithFunctionContext("date_parse_custom(\"September 2, 2013\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomMissingNominalDate() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_parse_custom(MISSING_NOMINAL, \"MMMM d, yyyy\", \"en\")");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomMissingNumericDate() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_parse_custom(MISSING_NUMERIC, \"MMMM d, yyyy\", \"en\")");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomMissingDateDate() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_parse_custom(MISSING_DATE, \"MMMM d, yyyy\", \"en\")");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomMissingNominalFormat() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_parse_custom(\"September 2, 2013\", MISSING_NOMINAL, \"en\")");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomMissingNumericFormat() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_parse_custom(\"September 2, 2013\", MISSING_NUMERIC, \"en\")");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomMissingNominalLocale() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_parse_custom(\"September 2, 2013\", \"MMMM d, yyyy\", MISSING_NOMINAL)");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateParseCustomMissingNumericLocale() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_parse_custom(\"September 2, 2013\", \"MMMM d, yyyy\", MISSING_NUMERIC)");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	/**
	 * DateString tests
	 */

	@Test
	public void dateString() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str(date_parse(123456789), DATE_MEDIUM, DATE_SHOW_DATE_AND_TIME)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("Jan 2, 1970 11:17:36 AM", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringInvalid1() {
		try {
			Expression exp = getExpressionWithFunctionContext("date_str(date_parse(123456789), \"456\", DATE_SHOW_DATE_AND_TIME)");
			exp.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringInvalid2() {
		try {
			Expression exp = getExpressionWithFunctionContext("date_str(date_parse(123456789), DATE_MEDIUM, \"789\")");
			exp.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringWrongType1() {
		try {
			getExpressionWithFunctionContext("date_str(date_parse(123456789), 777, DATE_SHOW_DATE_AND_TIME)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringWrongType2() {
		try {
			getExpressionWithFunctionContext("date_str(date_parse(123456789), DATE_MEDIUM, 777)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringNoArg() {
		try {
			getExpressionWithFunctionContext("date_str()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringMoreArgs() {
		try {
			getExpressionWithFunctionContext("date_str(date_parse(123456789), DATE_MEDIUM, DATE_SHOW_DATE_AND_TIME, 777)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringLessArgs() {
		try {
			getExpressionWithFunctionContext("date_str(date_parse(123456789), DATE_MEDIUM)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringMissingDateDate() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str(MISSING_DATE, DATE_MEDIUM, DATE_SHOW_DATE_AND_TIME)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringMissingNominalDate() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str(MISSING_NOMINAL, DATE_MEDIUM, DATE_SHOW_DATE_AND_TIME)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringMissingNominalDateSize() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str(date_parse(123456789), MISSING_NOMINAL, DATE_SHOW_DATE_AND_TIME)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringMissingNumericDateSize() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str(date_parse(123456789), MISSING_NUMERIC, DATE_SHOW_DATE_AND_TIME)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringMissingNominalDateFormat() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str(date_parse(123456789), DATE_MEDIUM, MISSING_NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringMissingNumericDateFormat() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str(date_parse(123456789), DATE_MEDIUM, MISSING_NUMERIC)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	/**
	 * DateStringWithLocale tests
	 */

	@Test
	public void dateStringLocale() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), DATE_MEDIUM, DATE_SHOW_DATE_AND_TIME, \"en\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("Jan 2, 1970 11:17:36 AM", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleInvalid1() {
		try {
			Expression exp = getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), \"777\", DATE_SHOW_DATE_AND_TIME, \"en\")");
			exp.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleInvalid2() {
		try {
			Expression exp = getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), DATE_MEDIUM, \"777\", \"en\")");
			exp.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleInvalid3() {
		try {
			getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), DATE_MEDIUM, DATE_SHOW_DATE_AND_TIME, 777)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleWrongType1() {
		try {
			getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), 777, DATE_SHOW_DATE_AND_TIME, \"en\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleWrongType2() {
		try {
			getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), DATE_MEDIUM, 777, \"en\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleWrongType3() {
		try {
			getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), DATE_MEDIUM, DATE_SHOW_DATE_AND_TIME, 777)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleNoArg() {
		try {
			getExpressionWithFunctionContext("date_str_loc()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleMoreArgs() {
		try {
			getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), DATE_MEDIUM, DATE_SHOW_DATE_AND_TIME, \"en\", 777)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleLessArgs() {
		try {
			getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), DATE_MEDIUM, DATE_SHOW_DATE_AND_TIME)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleMissingDateDate() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_loc(MISSING_DATE, DATE_MEDIUM, DATE_SHOW_DATE_AND_TIME, \"en\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleMissingNominalDate() {
		try {
			getExpressionWithFunctionContext("date_str_loc(MISSING_NOMINAL, DATE_MEDIUM, DATE_SHOW_DATE_AND_TIME, \"en\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleMissingNominalDateSize() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), MISSING_NOMINAL, DATE_SHOW_DATE_AND_TIME, \"en\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleMissingNumericDateSize() {
		try {
			getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), MISSING_NUMERIC, DATE_SHOW_DATE_AND_TIME, \"en\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleMissingNominalDateFormat() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), DATE_MEDIUM, MISSING_NOMINAL, \"en\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleMissingNumericDateFormat() {
		try {
			getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), DATE_MEDIUM, MISSING_NUMERIC, \"en\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleMissingNominalLocale() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), DATE_MEDIUM, DATE_SHOW_DATE_AND_TIME, MISSING_NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringLocaleMissingNumericLocale() {
		try {
			getExpressionWithFunctionContext("date_str_loc(date_parse(123456789), DATE_MEDIUM, DATE_SHOW_DATE_AND_TIME, MISSING_NUMERIC)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	/** DateStringCustom tests */

	@Test
	public void dateStringCustom() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_custom(date_parse(123456789), \"dd.MM.yy\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("02.01.70", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWithLocale() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_custom(date_parse(123456789), \"dd.MM.yy\", \"de\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("02.01.70", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWrongType1() {
		try {
			getExpressionWithFunctionContext("date_str_custom(777, \"dd.MM.yy\", \"de\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWrongType2() {
		try {
			getExpressionWithFunctionContext("date_str_custom(date_parse(123456789), 777, \"de\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWrongType3() {
		try {
			getExpressionWithFunctionContext("date_str_custom(date_parse(123456789), \"dd.MM.yy\", 777)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomInvalid1() {
		try {
			getExpressionWithFunctionContext("date_str_custom(\"777\", \"dd.MM.yy\", \"de\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomInvalid2() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_custom(date_parse(123456789), \"777\", \"de\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("777", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomInvalid3() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_custom(date_parse(123456789), \"dd.MM.yy\", \"777\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("02.01.70", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomNoArg() {
		try {
			getExpressionWithFunctionContext("date_str_custom()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomMoreArgs() {
		try {
			getExpressionWithFunctionContext("date_str_custom(date_parse(123456789), \"dd.MM.yy\", \"de\", 777)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomLessArgs() {
		try {
			getExpressionWithFunctionContext("date_str_custom(date_parse(123456789)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWithoutLocaleMissingDateDate() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_custom(MISSING_DATE, \"dd.MM.yy\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWithoutLocaleMissingNominalDate() {
		try {
			getExpressionWithFunctionContext("date_str_custom(MISSING_NOMINAL, \"dd.MM.yy\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWithoutLocaleMissingNominalDateFormat() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_custom(date_parse(123456789), MISSING_NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWithoutLocaleMissingNumericDateFormat() {
		try {
			getExpressionWithFunctionContext("date_str_custom(date_parse(123456789), MISSING_NUMERIC)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWithLocaleMissingDateDate() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_custom(MISSING_DATE, \"dd.MM.yy\", \"de\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWithLocaleMissingNominalDate() {
		try {
			getExpressionWithFunctionContext("date_str_custom(MISSING_NOMINAL, \"dd.MM.yy\", \"de\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWithLocaleMissingNominalDateFormat() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_custom(date_parse(123456789), MISSING_NOMINAL, \"de\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWithLocaleMissingNumericDateFormat() {
		try {
			getExpressionWithFunctionContext("date_str_custom(date_parse(123456789), MISSING_NUMERIC, \"de\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWithLocaleMissingNominalLocale() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_str_custom(date_parse(123456789), \"dd.MM.yy\", MISSING_NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateStringCustomWithLocaleMissingNumericLocale() {
		try {
			getExpressionWithFunctionContext("date_str_custom(date_parse(123456789), \"dd.MM.yy\", MISSING_NUMERIC)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

}
