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
package com.rapidminer.tools.expression.internal.antlr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParser;
import com.rapidminer.tools.expression.ExpressionParserBuilder;
import com.rapidminer.tools.expression.ExpressionRegistry;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.UnknownValue;
import com.rapidminer.tools.expression.internal.ExpressionParserConstants;
import com.rapidminer.tools.expression.internal.StandardFunctionsWithConstants;
import com.rapidminer.tools.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for constants known by the
 * {@link BasicConstantsResolver} or the {@link StandardFunctionsWithConstants}.
 *
 * @author Gisa Schaefer
 *
 */
public class ParserConstantsTest {

	/**
	 * Parses string expressions into {@link Expression}s if those expressions use constants but
	 * don't use any functions, macros or attributes.
	 */
	private Expression getExpressionWithConstantContext(String expression) throws ExpressionException {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		ExpressionParser parser = builder.withModules(ExpressionRegistry.INSTANCE.getAll()).build();
		return parser.parse(expression);
	}

	@Test
	public void constantTrue() {
		try {
			Expression expression = getExpressionWithConstantContext("true");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantTRUE() {
		try {
			Expression expression = getExpressionWithConstantContext("TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantFalse() {
		try {
			Expression expression = getExpressionWithConstantContext("false");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantFALSE() {
		try {
			Expression expression = getExpressionWithConstantContext("FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantE() {
		try {
			Expression expression = getExpressionWithConstantContext("e");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.E, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantPi() {
		try {
			Expression expression = getExpressionWithConstantContext("pi");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.PI, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantInfinity() {
		try {
			Expression expression = getExpressionWithConstantContext("INFINITY");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantNaN() {
		try {
			Expression expression = getExpressionWithConstantContext("NaN");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantNAN() {
		try {
			Expression expression = getExpressionWithConstantContext("NAN");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantMissingNumeric() {
		try {
			Expression expression = getExpressionWithConstantContext("MISSING_NUMERIC");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantMissing() {
		try {
			Expression expression = getExpressionWithConstantContext("MISSING");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
			assertEquals(UnknownValue.UNKNOWN_NOMINAL, expression.evaluate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantMissingNominal() {
		try {
			Expression expression = getExpressionWithConstantContext("MISSING_NOMINAL");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
			assertEquals(UnknownValue.UNKNOWN_NOMINAL, expression.evaluate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantMissingDate() {
		try {
			Expression expression = getExpressionWithConstantContext("MISSING_DATE");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(null, expression.evaluateDate());
			assertEquals(UnknownValue.UNKNOWN_DATE, expression.evaluate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantDateShort() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_SHORT");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_FORMAT_SHORT, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantDateMedium() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_MEDIUM");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_FORMAT_MEDIUM, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantDateLong() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_LONG");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_FORMAT_LONG, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantDateFull() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_FULL");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_FORMAT_FULL, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantShowDateOnly() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_SHOW_DATE_ONLY");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_SHOW_DATE_ONLY, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantShowTimeOnly() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_SHOW_TIME_ONLY");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_SHOW_TIME_ONLY, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantShowDateAndTime() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_SHOW_DATE_AND_TIME");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_SHOW_DATE_AND_TIME, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantDateUnitYear() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_UNIT_YEAR");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_UNIT_YEAR, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantDateUnitMonth() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_UNIT_MONTH");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_UNIT_MONTH, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantDateUnitWeek() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_UNIT_WEEK");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_UNIT_WEEK, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantDateUnitDay() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_UNIT_DAY");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_UNIT_DAY, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantDateUnitHour() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_UNIT_HOUR");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_UNIT_HOUR, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantDateUnitMinute() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_UNIT_MINUTE");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_UNIT_MINUTE, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantDateUnitSecond() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_UNIT_SECOND");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_UNIT_SECOND, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void constantDateUnitMillisecond() {
		try {
			Expression expression = getExpressionWithConstantContext("DATE_UNIT_MILLISECOND");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(ExpressionParserConstants.DATE_UNIT_MILLISECOND, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

}
