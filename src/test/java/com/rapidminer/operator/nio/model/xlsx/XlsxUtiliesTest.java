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
package com.rapidminer.operator.nio.model.xlsx;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.rapidminer.operator.nio.model.xlsx.XlsxUtilities.XlsxCellCoordinates;


/**
 * Unit tests for methods from {@link XlsxUtilities} class.
 *
 * @author Nils Woehler
 *
 */
public class XlsxUtiliesTest {

	@Test
	public void convertToColumnIndexTest() {
		assertEquals(-1, XlsxUtilities.convertToColumnIndex(""));
		assertEquals(0, XlsxUtilities.convertToColumnIndex("A"));
		assertEquals(1, XlsxUtilities.convertToColumnIndex("B"));
		assertEquals(2, XlsxUtilities.convertToColumnIndex("C"));
		assertEquals(25, XlsxUtilities.convertToColumnIndex("Z"));
		assertEquals(26, XlsxUtilities.convertToColumnIndex("AA"));
		assertEquals(27, XlsxUtilities.convertToColumnIndex("AB"));
		assertEquals(28, XlsxUtilities.convertToColumnIndex("AC"));
		assertEquals(51, XlsxUtilities.convertToColumnIndex("AZ"));
		assertEquals(52, XlsxUtilities.convertToColumnIndex("BA"));
		assertEquals(16_383, XlsxUtilities.convertToColumnIndex("XFD"));
	}

	@Test
	public void convertToColumnNameTest() {
		assertEquals("A", XlsxUtilities.convertToColumnName(0));
		assertEquals("B", XlsxUtilities.convertToColumnName(1));
		assertEquals("C", XlsxUtilities.convertToColumnName(2));
		assertEquals("Z", XlsxUtilities.convertToColumnName(25));
		assertEquals("AA", XlsxUtilities.convertToColumnName(26));
		assertEquals("AB", XlsxUtilities.convertToColumnName(27));
		assertEquals("AC", XlsxUtilities.convertToColumnName(28));
		assertEquals("AZ", XlsxUtilities.convertToColumnName(51));
		assertEquals("BA", XlsxUtilities.convertToColumnName(52));
		assertEquals("XFD", XlsxUtilities.convertToColumnName(16_383));
	}

	@Test(expected = IllegalArgumentException.class)
	public void convertToColumnNameIllegalArgumentTest() {
		XlsxUtilities.convertToColumnName(-1);
	}

	@Test
	public void convertCellRefToCoordinatesTest() {
		assertEquals(new XlsxCellCoordinates(0, 0), XlsxUtilities.convertCellRefToCoordinates("A1"));
		assertEquals(new XlsxCellCoordinates(0, 1), XlsxUtilities.convertCellRefToCoordinates("A2"));
		assertEquals(new XlsxCellCoordinates(1, 0), XlsxUtilities.convertCellRefToCoordinates("B1"));
		assertEquals(new XlsxCellCoordinates(1, 1), XlsxUtilities.convertCellRefToCoordinates("B2"));
		assertEquals(new XlsxCellCoordinates(26, 0), XlsxUtilities.convertCellRefToCoordinates("AA1"));
		assertEquals(new XlsxCellCoordinates(52, 0), XlsxUtilities.convertCellRefToCoordinates("BA1"));
		assertEquals(new XlsxCellCoordinates(26, 1), XlsxUtilities.convertCellRefToCoordinates("AA2"));
		assertEquals(new XlsxCellCoordinates(52, 1), XlsxUtilities.convertCellRefToCoordinates("BA2"));
		assertEquals(new XlsxCellCoordinates(16_383, 1_048_575), XlsxUtilities.convertCellRefToCoordinates("XFD1048576"));
	}

	@Test
	public void convertToCellRefToCoordinatesIllegalArgumentNoDigitTest() {
		assertEquals(new XlsxCellCoordinates(26, XlsxCellCoordinates.NO_ROW_NUMBER),
				XlsxUtilities.convertCellRefToCoordinates("AA"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void convertToCellRefToCoordinatesIllegalArgumentNotLetterTest() {
		XlsxUtilities.convertCellRefToCoordinates("1");
	}
}
