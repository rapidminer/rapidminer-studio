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

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * Tests for methods of the {@link Tools} class.
 *
 * @author Marco Boeck
 *
 */
public class ToolsTest {

	@Test
	public void formatIntegerIfPossibleNoArgs() {
		// default fraction digits are 3

		String formatted = Tools.formatIntegerIfPossible(0.001);
		assertEquals("0.001", formatted);
		formatted = Tools.formatIntegerIfPossible(0.0001);
		assertEquals("0.000", formatted);
		formatted = Tools.formatIntegerIfPossible(0.000000000000001);
		assertEquals("0.000", formatted);
		formatted = Tools.formatIntegerIfPossible(0.0000000000000001);
		assertEquals("0", formatted);

		formatted = Tools.formatIntegerIfPossible(1.01);
		assertEquals("1.010", formatted);
		// the test below could be expected to work but will fail due to how IEEE 754 works
		// we calculate (Math.abs(1 - 1.001) < 0.001) which returns 0.0009999999999 instead of 0.001
		formatted = Tools.formatIntegerIfPossible(1.0001);
		assertEquals("1.000", formatted);

		formatted = Tools.formatIntegerIfPossible(4.3751);
		assertEquals("4.375", formatted);
	}

	@Test
	public void formatIntegerIfPossibleAllArgs() {
		int fractionDigits = 5;
		double valueToFormat = 0.001;
		String formatted = Tools.formatIntegerIfPossible(valueToFormat, fractionDigits, false);
		assertEquals("0.00100", formatted);

		formatted = Tools.formatIntegerIfPossible(valueToFormat, fractionDigits, false);
		assertEquals("0.00100", formatted);
		formatted = Tools.formatIntegerIfPossible(valueToFormat, fractionDigits - 2, false);
		assertEquals("0.001", formatted);
	}

	@Test
	public void testExcelColumnNames() {
		// test error case
		assertEquals("Negative indices should create \"error\" result.", "error", Tools.getExcelColumnName(-1));
		// test base case
		for (int i = 0; i < 26; i++) {
			assertEquals("Index " + i + " creates wrong output.", "" + (char) ('A' + i), Tools.getExcelColumnName(i));
		}
		// edge cases
		assertEquals("Index 26 creates wrong output", "AA", Tools.getExcelColumnName(26));
		assertEquals("Index 51 creates wrong output", "AZ", Tools.getExcelColumnName(51));

	}

}
