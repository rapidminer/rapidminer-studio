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

import org.apache.commons.math3.util.FastMath;
import org.junit.Test;

import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.internal.antlr.AntlrParser;


/**
 *
 * Tests the results of {@link AntlrParser#parse(String)} for trigonometric functions.
 *
 * @author Denis Schernov
 *
 */
public class AntlrParserTrigonometricTest extends AntlrParserTest {

	// sin()
	@Test
	public void sinInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("sin(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sin(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("sin(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sin(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("sin(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sin(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinInf() {
		try {
			Expression expression = getExpressionWithFunctionContext("sin(INFINITY)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinEmpty() {
		try {
			getExpressionWithFunctionContext("sin()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinString() {
		try {
			getExpressionWithFunctionContext("sin( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinNull() {
		try {
			getExpressionWithFunctionContext("sin(0)");
			Expression expression = getExpressionWithFunctionContext("sin(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sin(0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("sin(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sin(90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("sin(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sin(Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("sin(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sin(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// cos()
	@Test
	public void cosInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("cos(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cos(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("cos(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cos(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("cos(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cos(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosINFINITY() {
		try {
			Expression expression = getExpressionWithFunctionContext("cos(INFINITY)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosEmpty() {
		try {
			getExpressionWithFunctionContext("cos()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosString() {
		try {
			getExpressionWithFunctionContext("cos( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosNull() {
		try {
			getExpressionWithFunctionContext("cos(0)");
			Expression expression = getExpressionWithFunctionContext("cos(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cos(0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("cos(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cos(90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("cos(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cos(Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("cos(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cos(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// tan()
	@Test
	public void tanInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("tan(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tan(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("tan(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tan(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("tan(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tan(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanInfinity() {
		try {
			Expression expression = getExpressionWithFunctionContext("tan(INFINITY)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanEmpty() {
		try {
			getExpressionWithFunctionContext("tan()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanString() {
		try {
			getExpressionWithFunctionContext("tan( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanNull() {
		try {
			getExpressionWithFunctionContext("tan(0)");
			Expression expression = getExpressionWithFunctionContext("tan(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tan(0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("tan(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tan(90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("tan(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tan(Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("tan(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tan(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// cot()
	@Test
	public void cotInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("cot(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.0 / Math.tan(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cotDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("cot(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.0 / Math.tan(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cotNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("cot(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.0 / Math.tan(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cotInfinity() {
		try {
			Expression expression = getExpressionWithFunctionContext("cot(INFINITY)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cotEmpty() {
		try {
			getExpressionWithFunctionContext("cot()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cotString() {
		try {
			getExpressionWithFunctionContext("cot( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cotNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("cot(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cotPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("cot(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cotPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("cot(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(0, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// sec()
	@Test
	public void secInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("sec(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.0 / Math.cos(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void secDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("sec(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.0 / Math.cos(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void secNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("sec(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.0 / Math.cos(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void secInfinity() {
		try {
			Expression expression = getExpressionWithFunctionContext("sec(INFINITY)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void secEmpty() {
		try {
			getExpressionWithFunctionContext("sec()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void secString() {
		try {
			getExpressionWithFunctionContext("sec( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void secNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("sec(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.0 / Math.cos(0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void secPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("sec(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.0 / Math.cos(Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void secPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("sec(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// cosec()
	@Test
	public void cosecInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosec(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.0 / Math.sin(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosecDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosec(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.0 / Math.sin(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosecNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosec(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.0 / Math.sin(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosecInfinity() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosec(INFINITY)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosecEmpty() {
		try {
			getExpressionWithFunctionContext("cosec()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosecString() {
		try {
			getExpressionWithFunctionContext("cosec( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosecNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosec(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosecNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosec(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.0 / Math.sin(90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosecPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosec(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cosecPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosec(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.0 / Math.sin(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// asin()
	@Test
	public void asinInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("asin(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.asin(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("asin(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.asin(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("asin(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.asin(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinInfinity() {
		try {
			Expression expression = getExpressionWithFunctionContext("asin(INIFNITY)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinEmpty() {
		try {
			getExpressionWithFunctionContext("asin()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinString() {
		try {
			getExpressionWithFunctionContext("asin( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("asin(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.asin(0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("asin(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.asin(90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("asin(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.asin(Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("asin(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.asin(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// acos()
	@Test
	public void acosInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("acos(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.acos(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acosDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("acos(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.acos(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acosNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("acos(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.acos(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acosInfinity() {
		try {
			Expression expression = getExpressionWithFunctionContext("acos(INFINITY)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acosEmpty() {
		try {
			getExpressionWithFunctionContext("acos()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acosString() {
		try {
			getExpressionWithFunctionContext("acos( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acosNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("acos(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.acos(0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acosNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("acos(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.acos(90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acosPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("acos(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.acos(Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acosPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("acos(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.acos(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// atan()
	@Test
	public void atanInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanEmpty() {
		try {
			getExpressionWithFunctionContext("atan()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanString() {
		try {
			getExpressionWithFunctionContext("atan( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.asin(0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan(90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan(Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// atan2()
	@Test
	public void atan2IntInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(16,16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(16, 16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2DoubleInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(16.3,16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(16.3, 16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2IntDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(16,16.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(16, 16.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2DoubleDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(33.3,33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(33.3, 33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2NegativeNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(-10,-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(-10, -10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2NegativePositive() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(-10,10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(-10, 10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2PositiveNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(10,-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(10, -10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2NullNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(0,0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(0, 0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2NullNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(0,90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(0, 90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2NinetyNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(90,0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(90, 0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2NinetyNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(90,90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(90, 90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2NullPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(0,pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(0, Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2NinetyPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(90,pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(90, Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2PiNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(pi,0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(Math.PI, 0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2PiNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(pi,90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(Math.PI, 90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2NullPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(0,pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(0, Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2NinetyPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(90,pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(90, Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2PiHalfNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(pi/2,0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(Math.PI / 2, 0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2PiHalfNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(pi/2,90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(Math.PI / 2, 90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2PiPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(pi,pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(Math.PI, Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2PiPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(pi,pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(Math.PI, Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2PiHalfPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(pi/2,pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(Math.PI / 2, Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2PiHalfPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("atan2(pi/2,pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.atan2(Math.PI / 2, Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2Empty() {
		try {
			getExpressionWithFunctionContext("atan2()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2String() {
		try {
			getExpressionWithFunctionContext("atan2( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atan2StringInt() {
		try {
			getExpressionWithFunctionContext("atan2( \"blup\",1)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// sinh()
	@Test
	public void sinhInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("sinh(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sinh(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinhDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("sinh(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sinh(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinhNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("sinh(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sinh(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinhEmpty() {
		try {
			getExpressionWithFunctionContext("sinh()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinhString() {
		try {
			getExpressionWithFunctionContext("sinh( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinhNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("sinh(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sinh(0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinhNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("sinh(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sinh(90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinhPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("sinh(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sinh(Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void sinhPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("sinh(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.sinh(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// cosh()
	@Test
	public void coshInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosh(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cosh(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void coshDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosh(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cosh(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void coshNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosh(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cosh(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void coshEmpty() {
		try {
			getExpressionWithFunctionContext("cosh()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void coshString() {
		try {
			getExpressionWithFunctionContext("cosh( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void coshNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosh(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cosh(0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void coshNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosh(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cosh(90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void coshPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosh(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cosh(Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void coshPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("cosh(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.cosh(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// tanh()
	@Test
	public void tanhInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("tanh(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tanh(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanhDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("tanh(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tanh(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanhNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("tanh(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tanh(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanhEmpty() {
		try {
			getExpressionWithFunctionContext("tanh()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanhString() {
		try {
			getExpressionWithFunctionContext("tanh( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanhNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("tanh(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tanh(0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanhNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("tanh(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tanh(90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanhPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("tanh(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tanh(Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void tanhPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("tanh(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.tanh(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// asinh()
	@Test
	public void asinhInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("asinh(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.asinh(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinhDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("asinh(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.asinh(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinhNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("asinh(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.asinh(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinhEmpty() {
		try {
			getExpressionWithFunctionContext("asinh()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinhString() {
		try {
			getExpressionWithFunctionContext("asinh( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinhNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("asinh(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.asinh(0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinhNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("asinh(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.asinh(90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinhPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("asinh(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.asinh(Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void asinhPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("asinh(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.asinh(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// acosh()
	@Test
	public void acoshInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("acosh(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.acosh(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acoshDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("acosh(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.acosh(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acoshNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("acosh(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.acosh(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acoshEmpty() {
		try {
			getExpressionWithFunctionContext("acosh()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acoshString() {
		try {
			getExpressionWithFunctionContext("acosh( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acoshNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("acosh(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.acosh(0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acoshNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("acosh(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.acosh(90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acoshPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("acosh(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.acosh(Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void acoshPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("acosh(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.acosh(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// atanh()
	@Test
	public void atanhInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("atanh(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.atanh(16), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanhDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("atanh(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.atanh(33.3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanhNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("atanh(-10)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.atanh(-10), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanhEmpty() {
		try {
			getExpressionWithFunctionContext("atanh()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanhString() {
		try {
			getExpressionWithFunctionContext("atanh( \"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanhNull() {
		try {
			Expression expression = getExpressionWithFunctionContext("atanh(0)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.atanh(0), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanhNinety() {
		try {
			Expression expression = getExpressionWithFunctionContext("atanh(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.atanh(90), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanhPi() {
		try {
			Expression expression = getExpressionWithFunctionContext("atanh(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.atanh(Math.PI), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void atanhPiHalf() {
		try {
			Expression expression = getExpressionWithFunctionContext("atanh(pi/2)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.atanh(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}
}
