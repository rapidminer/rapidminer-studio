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

import org.junit.Test;

import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for bitwise functions.
 *
 * @author David Arnu
 *
 */
public class AntlrParserBitwiseTest extends AntlrParserTest {

	@Test
	public void bitOrSimple() {
		try {
			Expression expression = getExpressionWithFunctionContext("bit_or(2,1)");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(3, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void bitOrNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("bit_or(-2,1)");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(-1, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void bitOrDoubleInteger() {
		try {
			getExpressionWithFunctionContext(" bit_or(2.5,1)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitOrIntegerDouble() {
		try {
			getExpressionWithFunctionContext(" bit_or(2,1.5)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitOrInfinity() {
		try {
			getExpressionWithFunctionContext(" bit_or(2,INFINITY)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitOrMissing() {
		try {
			getExpressionWithFunctionContext(" bit_or(2,MISSING_NUMERIC)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitOrWrongType() {
		try {
			getExpressionWithFunctionContext(" bit_or(\"aa\",1.5)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitEmpty() {
		try {
			getExpressionWithFunctionContext(" bit_or()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// bit XOR

	@Test
	public void bitXorSimple() {
		try {
			Expression expression = getExpressionWithFunctionContext("bit_xor(6,5)");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(3, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void bitXorNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("bit_xor(-2,1)");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(-1, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void bitXorDoubleInteger() {
		try {
			getExpressionWithFunctionContext(" bit_xor(2.5,1)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitXorIntegerDouble() {
		try {
			getExpressionWithFunctionContext(" bit_xor(2,1.5)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitXorInfinity() {
		try {
			getExpressionWithFunctionContext(" bit_xor(2,INFINITY)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitXorMissing() {
		try {
			getExpressionWithFunctionContext(" bit_xor(2,MISSING_NUMERIC)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitXorWrongType() {
		try {
			getExpressionWithFunctionContext(" bit_xor(\"aa\",1.5)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitXorEmpty() {
		try {
			getExpressionWithFunctionContext(" bit_xor()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// bit AND

	@Test
	public void bitAndSimple() {
		try {
			Expression expression = getExpressionWithFunctionContext("bit_and(6,5)");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(4, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void bitAndNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("bit_and(-2,5)");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(4, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void bitAndDoubleInteger() {
		try {
			getExpressionWithFunctionContext("bit_and(2.5,1)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitAndIntegerDouble() {
		try {
			getExpressionWithFunctionContext(" bit_and(2,1.5)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitAndInfinity() {
		try {
			getExpressionWithFunctionContext(" bit_and(2,INFINITY)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitAndMissing() {
		try {
			getExpressionWithFunctionContext(" bit_and(2,MISSING_NUMERIC)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitAndWrongType() {
		try {
			getExpressionWithFunctionContext(" bit_and(\"aa\",1.5)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitAndEmpty() {
		try {
			getExpressionWithFunctionContext(" bit_and()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// bit NOT

	@Test
	public void bitNotSimple() {
		try {
			Expression expression = getExpressionWithFunctionContext("bit_not(2)");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(-3, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void bitNotNegative() {
		try {
			Expression expression = getExpressionWithFunctionContext("bit_not(-2)");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(1, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void bitNotDouble() {
		try {
			getExpressionWithFunctionContext(" bit_not(2.5)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitNotInfinity() {
		try {
			getExpressionWithFunctionContext(" bit_not(INFINITY)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitNotMissing() {
		try {
			getExpressionWithFunctionContext(" bit_not(MISSING_NUMERIC)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitNotWrongType() {
		try {
			getExpressionWithFunctionContext(" bit_not(\"aa\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void bitNotEmpty() {
		try {
			getExpressionWithFunctionContext(" bit_not()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}
}
