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
package com.rapidminer.operator.annotation.test;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import com.rapidminer.operator.annotation.PolynomialFunction;

/**
 * @author Simon Fischer
 *
 */
public class PolynomialFunctionTest {

	@Test
	public void testLinearFunction() {
		PolynomialFunction f = PolynomialFunction.makeLinearFunction(2);
		assertEquals(70, f.evaluate(5, 7));
	}
	
	@Test
	public void testPolynomialFunction() {
		PolynomialFunction f = new PolynomialFunction(10, 2, 3);
		assertEquals(85750, f.evaluate(5, 7));
	}

	@Test
	public void testPolyPlusLogFunction() {
		PolynomialFunction f = new PolynomialFunction(10, 2, 1, 3, 1);
		// 10 * 5^2*ln(5) * 7^3*ln(7) * 10
		assertEquals((long)268553.69946285250055529643, f.evaluate(5, 7));
	}

	@Test
	public void testToStringForInvalidValue() {
		PolynomialFunction f = PolynomialFunction.makeLinearFunction(-0.0d);
		assertEquals("n/a", f.toString());
		f = PolynomialFunction.makeLinearFunction(0.0d);
		assertEquals("n/a", f.toString());
	}

}
