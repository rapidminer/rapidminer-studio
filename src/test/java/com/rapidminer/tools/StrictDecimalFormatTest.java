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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.text.ParseException;

import org.junit.Test;


/**
 * Tests the parser method of {@link StrictDecimalFormat}.
 *
 * @author Peter Hellinger
 */
public class StrictDecimalFormatTest {

	StrictDecimalFormat strictDecimalFormat = new StrictDecimalFormat('.');

	@Test
	public void parseUpperCaseE() {
		try {
			assertEquals(12300000L, strictDecimalFormat.parse("1.23E7"));
			assertEquals(0.0123d, strictDecimalFormat.parse("1.23E-2"));
		} catch (ParseException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void parseLowerCaseE() {
		try {
			assertEquals(12330000L, strictDecimalFormat.parse("1.233e7"));
			assertEquals(0.0123d, strictDecimalFormat.parse("1.23e-2"));
		} catch (ParseException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void parseInvalidExponent() {
		try {
			strictDecimalFormat.parse("1.233eE7");
			fail();
		} catch (ParseException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void parsePositiveExponent() throws ParseException {
		assertEquals(12330000L, strictDecimalFormat.parse("1.233e+7"));
		assertEquals(59876000L, strictDecimalFormat.parse("5.9876E+7"));
	}

	@Test
	public void parseNegativeExponent() throws ParseException {
		assertEquals(0.0000001233, strictDecimalFormat.parse("1.233e-7"));
		assertEquals(0.00000041973, strictDecimalFormat.parse("4.1973E-7"));
	}

	@Test
	public void parseInvalidDecimalSeparator() {
		try {
			strictDecimalFormat.parse("1,2337");
			fail();
		} catch (ParseException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void parseEmptyExponent() {
		try {
			strictDecimalFormat.parse("1,2337E");
			fail();
		} catch (ParseException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void parseCommaDecimalSeparator() {
		StrictDecimalFormat strictDecimalFormatComma = new StrictDecimalFormat(',');
		try {
			assertEquals(1.2337d, strictDecimalFormatComma.parse("1,2337"));
		} catch (ParseException e) {
			fail(e.getMessage());
		}
		try {
			strictDecimalFormatComma.parse("1.2337");
			fail();
		} catch (ParseException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void parseEmptyStrings() {
		StrictDecimalFormat strictDecimalFormatComma = new StrictDecimalFormat();
		try {
			strictDecimalFormatComma.parse("");
			fail();
		} catch (ParseException e) {
			assertNotNull(e.getMessage());
		}
		try {
			strictDecimalFormatComma.parse(" ");
			fail();
		} catch (ParseException e) {
			assertNotNull(e.getMessage());
		}
		try {
			strictDecimalFormatComma.parse("\n");
			fail();
		} catch (ParseException e) {
			assertNotNull(e.getMessage());
		}
		try {
			strictDecimalFormatComma.parse("\t");
			fail();
		} catch (ParseException e) {
			assertNotNull(e.getMessage());
		}
	}
}
