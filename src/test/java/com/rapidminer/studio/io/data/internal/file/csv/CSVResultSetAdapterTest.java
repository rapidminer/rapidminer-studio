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
package com.rapidminer.studio.io.data.internal.file.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.NoSuchElementException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetRow;
import com.rapidminer.core.io.data.ParseException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.CSVResultSetConfiguration;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.studio.io.data.internal.ResultSetAdapter;
import com.rapidminer.studio.io.data.internal.file.FileDataSourceTestUtils;
import com.rapidminer.studio.io.data.internal.file.csv.CSVDataSource;
import com.rapidminer.studio.io.data.internal.file.csv.CSVResultSetAdapter;


/**
 * A test case for the {@link CSVResultSetAdapter}.
 *
 * @author Nils Woehler, Gisa Schaefer
 *
 */
public class CSVResultSetAdapterTest {

	private static File simpleTestFile;
	private static File missingsTestFile;

	@BeforeClass
	public static void setup() throws URISyntaxException {
		simpleTestFile = new File(CSVResultSetAdapterTest.class.getResource("resultSetTest.csv").toURI());
		missingsTestFile = new File(CSVResultSetAdapterTest.class.getResource("missingsResultSetTest.csv").toURI());
	}

	@Test
	public void testSimpleImport() throws DataSetException, OperatorException, URISyntaxException, ParseException {
		try (CSVResultSetConfiguration configuration = new CSVResultSetConfiguration()) {
			// configure data import
			configuration.setCsvFile(simpleTestFile.toString());
			configuration.setEncoding(StandardCharsets.UTF_8);

			try (CSVResultSetAdapter csvResultSet = makeResultSet(configuration, 0, ResultSetAdapter.NO_END_ROW)) {
				int index = -1;
				assertEquals(-1, csvResultSet.getNumberOfRows());
				assertEquals(6, csvResultSet.getNumberOfColumns());
				assertEquals(index, csvResultSet.getCurrentRowIndex());
				assertTrue(csvResultSet.hasNext());

				while (csvResultSet.hasNext()) {
					DataSetRow row = csvResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(csvResultSet.getCurrentRowIndex(), index);

					// check data content of 0th and 10th row
					if (index == 0) {
						assertFirstSheetFirstRowContent(row);
					} else if (index == 10) {
						assertEquals(4.9, row.getDouble(0), 1e-10);
						assertEquals(3.1, row.getDouble(1), 1e-10);
						assertEquals(1.5, row.getDouble(2), 1e-10);
						assertEquals(.1, row.getDouble(3), 1e-10);
						assertEquals("id_10", row.getString(4));
						assertEquals("Iris-setosa", row.getString(5));
					}

				}

				assertEquals(150, csvResultSet.getCurrentRowIndex());

				// check reset
				csvResultSet.reset();
				assertEquals(csvResultSet.getCurrentRowIndex(), -1);
				assertTrue(csvResultSet.hasNext());
				assertFirstSheetFirstRowContent(csvResultSet.nextRow());
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
	public void testDateImport()
			throws DataSetException, OperatorException, URISyntaxException, ParseException, java.text.ParseException {
		try (CSVResultSetConfiguration configuration = new CSVResultSetConfiguration()) {

			String datePattern = "dd.MM.yyyy";

			// configure data import
			configuration.setCsvFile(missingsTestFile.toString());
			CSVDataSource dataSource = new CSVDataSource();
			dataSource.getMetadata().setDateFormat(new SimpleDateFormat(datePattern));

			SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
			try (CSVResultSetAdapter csvResultSet = makeResultSet(configuration, 0, 10, dataSource)) {
				int index = -1;
				assertEquals(4, csvResultSet.getNumberOfColumns());
				assertEquals(index, csvResultSet.getCurrentRowIndex());
				assertTrue(csvResultSet.hasNext());

				while (csvResultSet.hasNext()) {
					DataSetRow row = csvResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(csvResultSet.getCurrentRowIndex(), index);

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
		try (CSVResultSetConfiguration configuration = new CSVResultSetConfiguration()) {

			// configure data import
			configuration.setCsvFile(simpleTestFile.toString());

			String datePattern = "dd.MM.yyyy";
			SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
			try (CSVResultSetAdapter csvResultSet = makeResultSet(configuration, 0, 10)) {
				int index = -1;
				assertEquals(6, csvResultSet.getNumberOfColumns());
				assertEquals(index, csvResultSet.getCurrentRowIndex());
				assertTrue(csvResultSet.hasNext());

				while (csvResultSet.hasNext()) {
					DataSetRow row = csvResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(csvResultSet.getCurrentRowIndex(), index);

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
		try (CSVResultSetConfiguration configuration = new CSVResultSetConfiguration()) {

			// configure data import
			configuration.setCsvFile(missingsTestFile.toString());

			try (CSVResultSetAdapter csvResultSet = makeResultSet(configuration, 0, 10)) {
				int index = -1;
				assertEquals(4, csvResultSet.getNumberOfColumns());
				assertEquals(index, csvResultSet.getCurrentRowIndex());
				assertTrue(csvResultSet.hasNext());

				while (csvResultSet.hasNext()) {
					DataSetRow row = csvResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(csvResultSet.getCurrentRowIndex(), index);

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
		try (CSVResultSetConfiguration configuration = new CSVResultSetConfiguration()) {

			// configure data import
			configuration.setCsvFile(missingsTestFile.toString());

			try (CSVResultSetAdapter csvResultSet = makeResultSet(configuration, 0, 10)) {
				int index = -1;
				assertEquals(4, csvResultSet.getNumberOfColumns());
				assertEquals(index, csvResultSet.getCurrentRowIndex());
				assertTrue(csvResultSet.hasNext());

				while (csvResultSet.hasNext()) {
					DataSetRow row = csvResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(csvResultSet.getCurrentRowIndex(), index);

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
		try (CSVResultSetConfiguration configuration = new CSVResultSetConfiguration()) {

			// configure data import
			configuration.setCsvFile(simpleTestFile.toString());

			try (CSVResultSetAdapter csvResultSet = makeResultSet(configuration, 4, 10)) {
				int index = -1;
				assertEquals(6, csvResultSet.getNumberOfColumns());
				assertEquals(index, csvResultSet.getCurrentRowIndex());
				assertTrue(csvResultSet.hasNext());

				while (csvResultSet.hasNext()) {
					DataSetRow row = csvResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(csvResultSet.getCurrentRowIndex(), index);

					if (index == 0) {

						assertFalse(row.isMissing(0));
						assertEquals(4.6, row.getDouble(0), 1e-10);

						assertFalse(row.isMissing(1));
						assertEquals("id_4", row.getString(4));
					} else if (index == 6) {
						assertFalse(row.isMissing(0));
						assertEquals(4.9, row.getDouble(0), 1e-10);

						assertFalse(row.isMissing(4));
						assertEquals("id_10", row.getString(4));
					}

				}
			}
		}
	}

	@Test
	public void testFithRowAsStartAndNinthRowAsEndRow()
			throws DataSetException, OperatorException, URISyntaxException, ParseException, java.text.ParseException {
		try (CSVResultSetConfiguration configuration = new CSVResultSetConfiguration()) {

			// configure data import
			configuration.setCsvFile(simpleTestFile.toString());

			try (CSVResultSetAdapter csvResultSet = makeResultSet(configuration, 4, 8)) {
				int index = -1;
				assertEquals(6, csvResultSet.getNumberOfColumns());
				assertEquals(index, csvResultSet.getCurrentRowIndex());
				assertTrue(csvResultSet.hasNext());

				while (csvResultSet.hasNext()) {
					DataSetRow row = csvResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(csvResultSet.getCurrentRowIndex(), index);

					if (index == 0) {

						assertFalse(row.isMissing(0));
						assertEquals(4.6, row.getDouble(0), 1e-10);

						assertFalse(row.isMissing(4));
						assertEquals("id_4", row.getString(4));
					} else if (index == 4) {
						assertFalse(row.isMissing(0));
						assertEquals(5.0, row.getDouble(0), 1e-10);

						assertFalse(row.isMissing(4));
						assertEquals("id_8", row.getString(4));
					}

				}
			}
		}
	}

	@Test
	public void testEndRowBehindActualData()
			throws DataSetException, OperatorException, URISyntaxException, ParseException, java.text.ParseException {
		try (CSVResultSetConfiguration configuration = new CSVResultSetConfiguration()) {

			// configure data import
			configuration.setCsvFile(missingsTestFile.toString());

			try (CSVResultSetAdapter csvResultSet = makeResultSet(configuration, 0, 20)) {
				int index = -1;
				assertEquals(4, csvResultSet.getNumberOfColumns());
				assertEquals(index, csvResultSet.getCurrentRowIndex());
				assertTrue(csvResultSet.hasNext());

				while (csvResultSet.hasNext()) {
					DataSetRow row = csvResultSet.nextRow();

					// check if index has changed
					++index;
					assertEquals(csvResultSet.getCurrentRowIndex(), index);

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
		try (CSVResultSetConfiguration configuration = new CSVResultSetConfiguration()) {
			// configure data import
			configuration.setCsvFile(simpleTestFile.toString());

			try (CSVResultSetAdapter csvResultSet = makeResultSet(configuration, 0, 100)) {
				assertTrue(csvResultSet.hasNext());

				while (csvResultSet.hasNext()) {
					csvResultSet.nextRow().getString(6);
				}
			}
		}
	}

	@Test(expected = NoSuchElementException.class)
	public void testAccessNextRowOutOfBoundsImport()
			throws DataSetException, OperatorException, URISyntaxException, ParseException {
		try (CSVResultSetConfiguration configuration = new CSVResultSetConfiguration()) {
			// configure data import
			configuration.setCsvFile(simpleTestFile.toString());

			try (CSVResultSetAdapter csvResultSet = makeResultSet(configuration, 0, 100)) {
				assertTrue(csvResultSet.hasNext());

				while (csvResultSet.hasNext()) {
					csvResultSet.nextRow();
				}

				csvResultSet.nextRow();
			}
		}
	}

	private CSVResultSetAdapter makeResultSet(CSVResultSetConfiguration configuration, int startRow, int endRow)
			throws DataSetException, OperatorException {
		return makeResultSet(configuration, startRow, endRow, new CSVDataSource());
	}

	private CSVResultSetAdapter makeResultSet(CSVResultSetConfiguration configuration, int startRow, int endRow,
			CSVDataSource dataSource) throws DataSetException, OperatorException {
		DataResultSet dataResultSet = configuration.makeDataResultSet(null);
		return new CSVResultSetAdapter(dataSource, dataResultSet, startRow, endRow);
	}

}
