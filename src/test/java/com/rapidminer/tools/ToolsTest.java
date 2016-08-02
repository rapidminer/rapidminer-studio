/**
 * Copyright (C) 2001-2016 RapidMiner GmbH
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

}
