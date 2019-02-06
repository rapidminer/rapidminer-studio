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
package com.rapidminer.operator.nio;

import static com.rapidminer.parameter.ParameterTypeDateFormat.DATE_FORMAT_DD_DOT_MM_DOT_YYYY;
import static com.rapidminer.parameter.ParameterTypeDateFormat.DATE_FORMAT_MM_DD_YYYY;
import static com.rapidminer.parameter.ParameterTypeDateFormat.DATE_TIME_FORMAT_MM_DD_YYYY_H_MM_A;
import static com.rapidminer.parameter.ParameterTypeDateFormat.TIME_FORMAT_H_MM_A;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;


/**
 * Tests for the {@link DateFormatGuesser}.
 *
 * @author Jan Czogalla
 * @since 9.1
 */
public class DateFormatGuesserTest {

	private static final String[] DATE_FORMATS = {DATE_FORMAT_MM_DD_YYYY, DATE_FORMAT_DD_DOT_MM_DOT_YYYY, DATE_TIME_FORMAT_MM_DD_YYYY_H_MM_A, TIME_FORMAT_H_MM_A};
	// date format examples for MM/dd/yyyy, dd.MM.yyyy, MM/dd/yyyy hh:mm a, hh:mm a
	private static final String[] FORMATTED_DATES = {"08/20/1999", "20.08.1999", "08/20/1999 08:15 AM", "08:15 AM"};
	private static final int ROW_COUNT = 100;
	private static final double DATE_COLUMN_CONFIDENCE = 0.5;

	@Test
	public void testSingleAttributeWithOnlyUnmatched() {
		DateFormatGuesser guesser = new DateFormatGuesser(1, null, null);
		String[] unmatched = {"Test"};
		for (int i = 0; i < ROW_COUNT; i++) {
			guesser.count(unmatched);
		}
		Map<String, Double> results = guesser.getResults(DATE_COLUMN_CONFIDENCE);
		for (String dateFormat : DATE_FORMATS) {
			Assert.assertTrue(unmatched[0] + " did match " + dateFormat + ", but should not", Double.isNaN(results.get(dateFormat)));
		}
	}

	@Test
	public void testSingleAttributeWithOneDateType() {
		for (int i = 0; i < DATE_FORMATS.length; i++) {
			Map<String, Double> map = testSingleAttribute(new int[]{i}, ROW_COUNT);
			Assert.assertTrue("Formats did not match: " + DATE_FORMATS[i] + " " + FORMATTED_DATES[i],
					map.get(DATE_FORMATS[i]) > 0);
		}
	}

	@Test
	public void testSingleAttributeWithMultipleDateTypes() {
		for (int i = 1; i < DATE_FORMATS.length; i++) {
			for (int j = 0; j < i; j++) {
				Map<String, Double> map = testSingleAttribute(new int[]{j, i}, ROW_COUNT);
				Assert.assertTrue("Formats did not match: " + DATE_FORMATS[j] + " " + FORMATTED_DATES[j],
						map.get(DATE_FORMATS[j]) > 0);
				Assert.assertTrue("Formats did not match: " + DATE_FORMATS[i] + " " + FORMATTED_DATES[i],
						map.get(DATE_FORMATS[i]) > 0);
			}
		}
	}

	@Test
	public void testMultipleAttributesWithSeparateDateTypes() {
		DateFormatGuesser guesser = testMultipleAttributes(new int[][]{{0}, {1}, {2}, {3}}, ROW_COUNT, 4);
		for (int i = 0; i < DATE_FORMATS.length; i++) {
			Assert.assertTrue("Date format not present in this column: " + i,
					guesser.getDateAttributes(DATE_FORMATS[i], .9).contains(i));
		}
	}

	private Map<String, Double> testSingleAttribute(int[] indices, int rows) {
		return testMultipleAttributes(new int[][]{indices}, rows, 1).getResults(DATE_COLUMN_CONFIDENCE);
	}

	private DateFormatGuesser testMultipleAttributes(int[][] indices, int rows, int columns) {
		DateFormatGuesser guesser = new DateFormatGuesser(columns, null, null);
		for (int i = 0; i < rows; i++) {
			String[] rowValues = new String[columns];
			for (int j = 0; j < columns; j++) {
				int rowIndex = i % indices[j].length;
				rowValues[j] = FORMATTED_DATES[indices[j][rowIndex]];
			}
			guesser.count(rowValues);
		}
		return guesser;
	}
}