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
package com.rapidminer.studio.io.data.internal.file.excel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.NoSuchElementException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetRow;
import com.rapidminer.core.io.data.ParseException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.studio.io.data.internal.ResultSetAdapter;
import com.rapidminer.studio.io.data.internal.file.FileDataSourceTestUtils;


/**
 * A test case for the {@link ExcelResultSetAdapter}.
 *
 * @author Nils Woehler
 *
 */
public class ExcelResultSetAdapterTest {

	private static File testFile;

	@BeforeClass
	public static void setup() throws URISyntaxException {
		testFile = new File(ExcelResultSetAdapterTest.class.getResource("resultSetTest.xlsx").toURI());
	}

	@Test
	public void testSimpleImport() throws DataSetException, OperatorException, URISyntaxException, ParseException {
		try (ExcelResultSetConfiguration configuration = new ExcelResultSetConfiguration()) {
			// configure data import
			configuration.setWorkbookFile(testFile);
			configuration.setSheet(0);

			try (ExcelResultSetAdapter excelResultSet = makeResultSet(configuration, 0, ResultSetAdapter.NO_END_ROW)) {
				int index = -1;
				assertEquals(6, excelResultSet.getNumberOfColumns());
				assertEquals(index, excelResultSet.getCurrentRowIndex());
				assertTrue(excelResultSet.hasNext());

				while (excelResultSet.hasNext()) {
					DataSetRow row = excelResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(excelResultSet.getCurrentRowIndex(), index);

					// check data content of first, 10th, and last row
					if (index == 0) {
						assertFirstSheetFirstRowContent(row);
					} else if (index == 10) {
						assertEquals(4.9, row.getDouble(0), 1e-10);
						assertEquals(3.1, row.getDouble(1), 1e-10);
						assertEquals(1.5, row.getDouble(2), 1e-10);
						assertEquals(.1, row.getDouble(3), 1e-10);
						assertEquals("id_10", row.getString(4));
						assertEquals("Iris-setosa", row.getString(5));
					} else if (index == 150) {
						assertEquals(5.9, row.getDouble(0), 1e-10);
						assertEquals(3.0, row.getDouble(1), 1e-10);
						assertEquals(5.1, row.getDouble(2), 1e-10);
						assertEquals(1.8, row.getDouble(3), 1e-10);
						assertEquals("id_150", row.getString(4));
						assertEquals("Iris-virginica", row.getString(5));
					}

				}

				assertEquals(150, excelResultSet.getCurrentRowIndex());

				// check reset
				excelResultSet.reset();
				assertEquals(excelResultSet.getCurrentRowIndex(), -1);
				assertTrue(excelResultSet.hasNext());
				assertFirstSheetFirstRowContent(excelResultSet.nextRow());
			}
		}
	}

	private void assertFirstSheetFirstRowContent(DataSetRow row) throws ParseException {
		assertEquals("a1", row.getString(0));
		assertEquals("a2", row.getString(1));
		assertEquals("a3133333333333333331311313", row.getString(2));
		assertEquals("a4", row.getString(3));
		assertEquals("id", row.getString(4));
		assertEquals(FileDataSourceTestUtils.getUtf8Label(), row.getString(5));
	}

	@Test
	public void testSecondSheetImport() throws DataSetException, OperatorException, URISyntaxException, ParseException {
		try (ExcelResultSetConfiguration configuration = new ExcelResultSetConfiguration()) {

			// configure data import
			configuration.setWorkbookFile(testFile);
			configuration.setSheet(1);

			try (ExcelResultSetAdapter excelResultSet = makeResultSet(configuration, 0, 100)) {
				int index = -1;
				assertEquals(3, excelResultSet.getNumberOfColumns());
				assertEquals(index, excelResultSet.getCurrentRowIndex());
				assertTrue(excelResultSet.hasNext());

				while (excelResultSet.hasNext()) {
					DataSetRow row = excelResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(excelResultSet.getCurrentRowIndex(), index);

					if (index == 0) {
						assertFalse(row.isMissing(0));
						assertEquals("a1", row.getString(0));

						assertFalse(row.isMissing(1));
						assertEquals("id", row.getString(1));

						assertFalse(row.isMissing(2));
						assertEquals("label", row.getString(2));
					} else if (index == 10) {
						assertFalse(row.isMissing(0));
						assertEquals(4.9, row.getDouble(0), 1e-10);

						assertFalse(row.isMissing(1));
						assertEquals("id_10", row.getString(1));

						assertFalse(row.isMissing(2));
						assertEquals("Iris-setosa", row.getString(2));
					}

				}
			}
		}
	}

	@Test
	public void testDateImport()
			throws DataSetException, OperatorException, URISyntaxException, ParseException, java.text.ParseException {
		try (ExcelResultSetConfiguration configuration = new ExcelResultSetConfiguration()) {

			String datePattern = "dd.MM.yyyy";

			// configure data import
			configuration.setWorkbookFile(testFile);
			configuration.setSheetByName("Tabelle2");
			configuration.setSheetSelectionMode(ExcelResultSetConfiguration.SheetSelectionMode.BY_NAME);
			configuration.setDatePattern(datePattern);

			SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
			try (ExcelResultSetAdapter excelResultSet = makeResultSet(configuration, 0, 10)) {
				int index = -1;
				assertEquals(4, excelResultSet.getNumberOfColumns());
				assertEquals(index, excelResultSet.getCurrentRowIndex());
				assertTrue(excelResultSet.hasNext());

				while (excelResultSet.hasNext()) {
					DataSetRow row = excelResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(excelResultSet.getCurrentRowIndex(), index);

					if (index == 1) {
						assertFalse(row.isMissing(3));
						assertEquals(dateFormat.parse("01.01.1876"), row.getDate(3));
					} else if (index == 3) {
						assertTrue(row.isMissing(3));
						assertEquals(null, row.getDate(3));
					} else if (index == 4) {
						assertFalse(row.isMissing(3));
						assertEquals(dateFormat.parse("02.01.1923"), row.getDate(3));
						return;
					}

				}
			}
		}
	}

	@Test(expected = ParseException.class)
	public void testDateImportWithoutDatePattern()
			throws DataSetException, OperatorException, URISyntaxException, ParseException, java.text.ParseException {
		try (ExcelResultSetConfiguration configuration = new ExcelResultSetConfiguration()) {

			// configure data import
			configuration.setWorkbookFile(testFile);
			configuration.setSheet(2);

			String datePattern = "dd.MM.YYYY";
			SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
			try (ExcelResultSetAdapter excelResultSet = makeResultSet(configuration, 0, 10)) {
				int index = -1;
				assertEquals(4, excelResultSet.getNumberOfColumns());
				assertEquals(index, excelResultSet.getCurrentRowIndex());
				assertTrue(excelResultSet.hasNext());

				while (excelResultSet.hasNext()) {
					DataSetRow row = excelResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(excelResultSet.getCurrentRowIndex(), index);

					if (index == 1) {
						assertFalse(row.isMissing(3));
						assertEquals(dateFormat.parse("01.01.1876"), row.getDate(3));
						return;
					}

				}
			}
		}
	}

	@Test
	public void testDateImportAsString()
			throws DataSetException, OperatorException, URISyntaxException, ParseException, java.text.ParseException {
		try (ExcelResultSetConfiguration configuration = new ExcelResultSetConfiguration()) {

			// configure data import
			configuration.setWorkbookFile(testFile);
			configuration.setSheet(2);

			try (ExcelResultSetAdapter excelResultSet = makeResultSet(configuration, 0, 10)) {
				int index = -1;
				assertEquals(4, excelResultSet.getNumberOfColumns());
				assertEquals(index, excelResultSet.getCurrentRowIndex());
				assertTrue(excelResultSet.hasNext());

				while (excelResultSet.hasNext()) {
					DataSetRow row = excelResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(excelResultSet.getCurrentRowIndex(), index);

					if (index == 1) {
						assertFalse(row.isMissing(3));
						assertEquals("01.01.1876", row.getString(3));
						return;
					}

				}
			}
		}
	}

	@Test
	public void testImportMissings()
			throws DataSetException, OperatorException, URISyntaxException, ParseException, java.text.ParseException {
		try (ExcelResultSetConfiguration configuration = new ExcelResultSetConfiguration()) {

			// configure data import
			configuration.setWorkbookFile(testFile);
			configuration.setSheet(2);

			try (ExcelResultSetAdapter excelResultSet = makeResultSet(configuration, 0, 10)) {
				int index = -1;
				assertEquals(4, excelResultSet.getNumberOfColumns());
				assertEquals(index, excelResultSet.getCurrentRowIndex());
				assertTrue(excelResultSet.hasNext());

				while (excelResultSet.hasNext()) {
					DataSetRow row = excelResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(excelResultSet.getCurrentRowIndex(), index);

					if (index == 3) {
						// missing numerical
						assertTrue(row.isMissing(0));
						assertEquals(Double.NaN, row.getDouble(0), 1e-10);

						// missing date
						assertTrue(row.isMissing(3));
						assertEquals(null, row.getDate(3));
					} else if (index == 6) {

						// missing string
						assertTrue(row.isMissing(2));
						assertEquals(null, row.getString(2));
					}

				}
			}
		}
	}

	@Test
	public void testFithRowAsStartRow()
			throws DataSetException, OperatorException, URISyntaxException, ParseException, java.text.ParseException {
		try (ExcelResultSetConfiguration configuration = new ExcelResultSetConfiguration()) {

			// configure data import
			configuration.setWorkbookFile(testFile);
			configuration.setSheet(2);

			try (ExcelResultSetAdapter excelResultSet = makeResultSet(configuration, 4, 10)) {
				int index = -1;
				assertEquals(4, excelResultSet.getNumberOfColumns());
				assertEquals(index, excelResultSet.getCurrentRowIndex());
				assertTrue(excelResultSet.hasNext());

				while (excelResultSet.hasNext()) {
					DataSetRow row = excelResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(excelResultSet.getCurrentRowIndex(), index);

					if (index == 0) {

						assertFalse(row.isMissing(0));
						assertEquals(4.6, row.getDouble(0), 1e-10);

						assertFalse(row.isMissing(1));
						assertEquals("id_4", row.getString(1));
					} else if (index == 5) {
						assertTrue(row.isMissing(0));

						assertFalse(row.isMissing(1));
						assertEquals("id_4", row.getString(1));
					}

				}
			}
		}
	}

	@Test
	public void testFithRowAsStartAndNinthRowAsEndRow()
			throws DataSetException, OperatorException, URISyntaxException, ParseException, java.text.ParseException {
		try (ExcelResultSetConfiguration configuration = new ExcelResultSetConfiguration()) {

			// configure data import
			configuration.setWorkbookFile(testFile);
			configuration.setSheet(2);

			try (ExcelResultSetAdapter excelResultSet = makeResultSet(configuration, 4, 8)) {
				int index = -1;
				assertEquals(4, excelResultSet.getNumberOfColumns());
				assertEquals(index, excelResultSet.getCurrentRowIndex());
				assertTrue(excelResultSet.hasNext());

				while (excelResultSet.hasNext()) {
					DataSetRow row = excelResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(excelResultSet.getCurrentRowIndex(), index);

					if (index == 0) {

						assertFalse(row.isMissing(0));
						assertEquals(4.6, row.getDouble(0), 1e-10);

						assertFalse(row.isMissing(1));
						assertEquals("id_4", row.getString(1));
					} else if (index == 4) {
						assertFalse(row.isMissing(0));
						assertEquals(4.7, row.getDouble(0), 1e-10);

						assertFalse(row.isMissing(1));
						assertEquals("id_3", row.getString(1));
					}

				}
			}
		}
	}

	@Test
	public void testEndRowBehindActualData()
			throws DataSetException, OperatorException, URISyntaxException, ParseException, java.text.ParseException {
		try (ExcelResultSetConfiguration configuration = new ExcelResultSetConfiguration()) {

			// configure data import
			configuration.setWorkbookFile(testFile);
			configuration.setSheet(2);

			try (ExcelResultSetAdapter excelResultSet = makeResultSet(configuration, 0, 20)) {
				int index = -1;
				assertEquals(4, excelResultSet.getNumberOfColumns());
				assertEquals(index, excelResultSet.getCurrentRowIndex());
				assertTrue(excelResultSet.hasNext());

				while (excelResultSet.hasNext()) {
					DataSetRow row = excelResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(excelResultSet.getCurrentRowIndex(), index);

					if (index == 1) {
						assertFalse(row.isMissing(0));
						assertEquals(5.1, row.getDouble(0), 1e-10);

						assertFalse(row.isMissing(1));
						assertEquals("id_1", row.getString(1));
					} else if (index == 10) {
						assertFalse(row.isMissing(0));
						assertEquals(4.9, row.getDouble(0), 1e-10);

						assertFalse(row.isMissing(1));
						assertEquals("id_2", row.getString(1));
					}
				}

				// cannot read more data than available
				assertEquals(10, index);
			}
		}
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testAccessOutOfColumnBoundsImport()
			throws DataSetException, OperatorException, URISyntaxException, ParseException {
		try (ExcelResultSetConfiguration configuration = new ExcelResultSetConfiguration()) {
			// configure data import
			configuration.setWorkbookFile(testFile);
			configuration.setSheet(1);

			try (ExcelResultSetAdapter excelResultSet = makeResultSet(configuration, 0, 100)) {
				assertTrue(excelResultSet.hasNext());

				while (excelResultSet.hasNext()) {
					excelResultSet.nextRow().getString(5);
				}
			}
		}
	}

	@Test(expected = NoSuchElementException.class)
	public void testAccessNextRowOutOfBoundsImport()
			throws DataSetException, OperatorException, URISyntaxException, ParseException {
		try (ExcelResultSetConfiguration configuration = new ExcelResultSetConfiguration()) {
			// configure data import
			configuration.setWorkbookFile(testFile);
			configuration.setSheet(1);

			try (ExcelResultSetAdapter excelResultSet = makeResultSet(configuration, 0, 100)) {
				assertTrue(excelResultSet.hasNext());

				while (excelResultSet.hasNext()) {
					excelResultSet.nextRow();
				}

				excelResultSet.nextRow();
			}
		}
	}

	private ExcelResultSetAdapter makeResultSet(ExcelResultSetConfiguration configuration, int startRow, int endRow)
			throws DataSetException, OperatorException {
		DataResultSet dataResultSet = configuration.makeDataResultSet(null);
		return new ExcelResultSetAdapter(dataResultSet, startRow, endRow);
	}

}
