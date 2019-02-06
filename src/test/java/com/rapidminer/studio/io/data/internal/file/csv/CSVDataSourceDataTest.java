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
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.core.io.data.ColumnMetaData;
import com.rapidminer.core.io.data.ColumnMetaData.ColumnType;
import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetRow;
import com.rapidminer.core.io.data.ParseException;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.studio.io.data.HeaderRowBehindStartRowException;
import com.rapidminer.studio.io.data.HeaderRowNotFoundException;
import com.rapidminer.studio.io.data.StartRowNotFoundException;
import com.rapidminer.studio.io.data.internal.ResultSetAdapter;
import com.rapidminer.studio.io.data.internal.file.FileDataSourceTestUtils;
import com.rapidminer.tools.Tools;


/**
 * Unit tests for the {@link CSVDataSource#getData()} method.
 *
 * @author Nils Woehler, Gisa Schaefer
 *
 */
public class CSVDataSourceDataTest {

	private static File simpleTestFile;
	private static File simpleTestFileCommentsQuotesAndEscape;
	private static File simpleTestFileSeparatorAndDecimalCharacter;
	private static File simpleTestFileSpaceAsSeparator;
	private static File missingInHeaderRow;
	private static File nominalDateTestFile;

	// remember system locale
	private static Locale systemLocale = Locale.getDefault();

	@BeforeClass
	public static void setup() throws URISyntaxException, IOException {
		simpleTestFile = new File(CSVDataSourceDataTest.class.getResource("iris1.csv").toURI());
		simpleTestFileSeparatorAndDecimalCharacter = new File(CSVDataSourceDataTest.class.getResource("iris2.csv").toURI());
		simpleTestFileCommentsQuotesAndEscape = new File(CSVDataSourceDataTest.class.getResource("iris3.csv").toURI());
		simpleTestFileSpaceAsSeparator = new File(CSVDataSourceDataTest.class.getResource("iris4.csv").toURI());
		missingInHeaderRow = new File(CSVDataSourceDataTest.class.getResource("missingInHeaderRow.csv").toURI());
		nominalDateTestFile = new File(CSVDataSourceDataTest.class.getResource("nominal_dates_1.csv").toURI());

		// we need to set the local as otherwise test results might differ depending on the system
		// local running the test
		Locale.setDefault(Locale.ENGLISH);
	}

	@AfterClass
	public static void tearDown() {
		// restore system locale
		Locale.setDefault(systemLocale);
	}

	@Test
	public void defaultMetaDataTest() throws DataSetException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());
			dataSource.getResultSetConfiguration().setHasHeaderRow(true);
			dataSource.getResultSetConfiguration().setHeaderRow(0);
			dataSource.getResultSetConfiguration().setEncoding(StandardCharsets.UTF_8);

			// use default guessed meta data
			dataSource.createMetaData();

			assertFalse(dataSource.getMetadata().isFaultTolerant());
			assertEquals(Tools.DATE_TIME_FORMAT.get(), dataSource.getMetadata().getDateFormat());
			assertEquals(6, dataSource.getMetadata().getColumnMetaData().size());

			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(0), "a1", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(1), "a2", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(2), "a3133333333333333331311313",
					ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(3), "a4", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(4), "id", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(5), FileDataSourceTestUtils.getUtf8Label(),
					ColumnType.CATEGORICAL);

			try (DataSet data = dataSource.getData()) {
				assertEquals(-1, data.getNumberOfRows());
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of 0th and 10th row
					if (index == 0) {
						assertFirstSheetRowContent(row);
					} else if (index == 9) {
						assertEquals(4.9, row.getDouble(0), 1e-10);
						assertEquals(3.1, row.getDouble(1), 1e-10);
						assertEquals(1.5, row.getDouble(2), 1e-10);
						assertEquals(.1, row.getDouble(3), 1e-10);
						assertEquals("id_10", row.getString(4));
						assertEquals("Iris-setosa", row.getString(5));
					} else if (index == 149) {
						assertEquals(5.9, row.getDouble(0), 1e-10);
						assertEquals(3.0, row.getDouble(1), 1e-10);
						assertEquals(5.1, row.getDouble(2), 1e-10);
						assertEquals(1.8, row.getDouble(3), 1e-10);
						assertEquals("id_150", row.getString(4));
						assertEquals("Iris-virginica", row.getString(5));
					}
				}

				assertEquals(149, data.getCurrentRowIndex());

				// check reset
				data.reset();
				assertEquals(data.getCurrentRowIndex(), -1);
				assertTrue(data.hasNext());
				assertFirstSheetRowContent(data.nextRow());
			}
		}
	}

	@Test
	public void defaultTestWithChangedSeparatorAndDecimalCharacter() throws DataSetException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFileSeparatorAndDecimalCharacter.toPath());
			dataSource.getResultSetConfiguration().setEncoding(StandardCharsets.UTF_8);
			dataSource.getResultSetConfiguration().setHasHeaderRow(true);
			dataSource.getResultSetConfiguration().setHeaderRow(0);
			dataSource.getResultSetConfiguration().setColumnSeparators("|");
			dataSource.getResultSetConfiguration().setDecimalCharacter(',');

			// use default guessed meta data
			dataSource.createMetaData();

			assertEquals(6, dataSource.getMetadata().getColumnMetaData().size());

			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(0), "a1", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(1), "a2", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(2), "a3133333333333333331311313",
					ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(3), "a4", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(4), "id", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(5), FileDataSourceTestUtils.getUtf8Label(),
					ColumnType.CATEGORICAL);

			try (DataSet data = dataSource.getData()) {
				assertEquals(-1, data.getNumberOfRows());
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of 0th and 10th row
					if (index == 0) {
						assertFirstSheetRowContent(row);
					} else if (index == 9) {
						assertEquals(4.9, row.getDouble(0), 1e-10);
						assertEquals(3.1, row.getDouble(1), 1e-10);
						assertEquals(1.5, row.getDouble(2), 1e-10);
						assertEquals(.1, row.getDouble(3), 1e-10);
						assertEquals("id_10", row.getString(4));
						assertEquals("Iris-setosa", row.getString(5));
					} else if (index == 149) {
						assertEquals(5.9, row.getDouble(0), 1e-10);
						assertEquals(3.0, row.getDouble(1), 1e-10);
						assertEquals(5.1, row.getDouble(2), 1e-10);
						assertEquals(1.8, row.getDouble(3), 1e-10);
						assertEquals("id_150", row.getString(4));
						assertEquals("Iris-virginica", row.getString(5));
					}
				}

				assertEquals(149, data.getCurrentRowIndex());

				// check reset
				data.reset();
				assertEquals(data.getCurrentRowIndex(), -1);
				assertTrue(data.hasNext());
				assertFirstSheetRowContent(data.nextRow());
			}
		}
	}

	@Test(expected = HeaderRowBehindStartRowException.class)
	public void headerRowBehindStartRow() throws DataSetException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());

			// set header row behind the start row
			dataSource.getResultSetConfiguration().setHasHeaderRow(true);
			dataSource.getResultSetConfiguration().setHeaderRow(1);

			// configure meta data
			dataSource.createMetaData();
		}
	}

	@Test(expected = HeaderRowBehindStartRowException.class)
	public void headerRowBehindStartRow2() throws DataSetException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());

			// set header row behind data start row
			dataSource.getResultSetConfiguration().setStartingRow(10);
			dataSource.getResultSetConfiguration().setHasHeaderRow(true);
			dataSource.getResultSetConfiguration().setHeaderRow(15);

			// configure the meta data
			dataSource.createMetaData();
		}
	}

	@Test(expected = StartRowNotFoundException.class)
	public void startRowNotAvailable() throws DataSetException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());

			// set start row behind actual data content
			dataSource.getResultSetConfiguration().setStartingRow(151);
			dataSource.getResultSetConfiguration().setHasHeaderRow(true);
			dataSource.getResultSetConfiguration().setHeaderRow(150);

			// configure the meta data
			dataSource.createMetaData();
		}
	}

	@Test(expected = HeaderRowNotFoundException.class)
	public void headerRowNotFound() throws DataSetException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());
			dataSource.getResultSetConfiguration().setStartingRow(155);
			dataSource.getResultSetConfiguration().setHasHeaderRow(true);
			dataSource.getResultSetConfiguration().setHeaderRow(155);
			dataSource.createMetaData();
		}
	}

	@Test
	public void dataContentStartsAtFithRow() throws DataSetException, ParseException {

		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());
			dataSource.getResultSetConfiguration().setStartingRow(4);
			dataSource.getResultSetConfiguration().setHasHeaderRow(true);
			dataSource.getResultSetConfiguration().setHeaderRow(0);
			dataSource.getResultSetConfiguration().setEncoding(StandardCharsets.UTF_8);

			// use default guessed meta data
			dataSource.createMetaData();

			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(0), "a1", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(1), "a2", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(2), "a3133333333333333331311313",
					ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(3), "a4", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(4), "id", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(5), FileDataSourceTestUtils.getUtf8Label(),
					ColumnType.CATEGORICAL);

			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of first, 10th, last row
					if (index == 0) {
						assertEquals(4.6, row.getDouble(0), 1e-10);
						assertEquals(3.1, row.getDouble(1), 1e-10);
						assertEquals(1.5, row.getDouble(2), 1e-10);
						assertEquals(.2, row.getDouble(3), 1e-10);
						assertEquals("id_4", row.getString(4));
						assertEquals("Iris-setosa", row.getString(5));
					} else if (index == 9) {
						assertEquals(4.8, row.getDouble(0), 1e-10);
						assertEquals(3.0, row.getDouble(1), 1e-10);
						assertEquals(1.4, row.getDouble(2), 1e-10);
						assertEquals(.1, row.getDouble(3), 1e-10);
						assertEquals("id_13", row.getString(4));
						assertEquals("Iris-setosa", row.getString(5));
					} else if (index == 146) {
						assertEquals(5.9, row.getDouble(0), 1e-10);
						assertEquals(3.0, row.getDouble(1), 1e-10);
						assertEquals(5.1, row.getDouble(2), 1e-10);
						assertEquals(1.8, row.getDouble(3), 1e-10);
						assertEquals("id_150", row.getString(4));
						assertEquals("Iris-virginica", row.getString(5));
					}
				}

				assertEquals(146, data.getCurrentRowIndex());

				// check reset
				data.reset();
				assertEquals(data.getCurrentRowIndex(), -1);
				assertTrue(data.hasNext());
			}
		}
	}

	@Test
	public void noHeaderRowDefined() throws DataSetException, ParseException {

		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());
			dataSource.getResultSetConfiguration().setHasHeaderRow(false);
			dataSource.getResultSetConfiguration().setHeaderRow(ResultSetAdapter.NO_HEADER_ROW);
			dataSource.getResultSetConfiguration().setEncoding(StandardCharsets.UTF_8);

			// use default guessed meta data
			dataSource.createMetaData();

			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(0), "att1", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(1), "att2", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(2), "att3", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(3), "att4", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(4), "att5", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(5), "att6", ColumnType.CATEGORICAL);

			try (DataSet data = dataSource.getData()) {
				assertEquals(-1, data.getNumberOfRows());
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of first, 10th, last row
					if (index == 0) {
						assertEquals("a1", row.getString(0));
						assertEquals("a2", row.getString(1));
						assertEquals("a3133333333333333331311313", row.getString(2));
						assertEquals("a4", row.getString(3));
						assertEquals("id", row.getString(4));
						assertEquals(FileDataSourceTestUtils.getUtf8Label(), row.getString(5));
					} else if (index == 9) {
						assertEquals("2.9", row.getString(1));
						assertEquals("1.4", row.getString(2));
						assertEquals("0.2", row.getString(3));
						assertEquals("id_9", row.getString(4));
						assertEquals("Iris-setosa", row.getString(5));
					} else if (index == 150) {
						assertEquals("5.9", row.getString(0));
						assertEquals("3.0", row.getString(1));
						assertEquals("1.8", row.getString(3));
						assertEquals("id_150", row.getString(4));
						assertEquals("Iris-virginica", row.getString(5));
					}
				}

				assertEquals(150, data.getCurrentRowIndex());

				// check reset
				data.reset();
				assertEquals(data.getCurrentRowIndex(), -1);
				assertTrue(data.hasNext());
			}
		}
	}

	@Test
	public void missingInHeaderRow() throws DataSetException {

		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(missingInHeaderRow.toPath());
			dataSource.getResultSetConfiguration().setHasHeaderRow(true);
			dataSource.getResultSetConfiguration().setHeaderRow(0);

			// use default guessed meta data
			dataSource.createMetaData();

			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(0), "a1", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(1), "id", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(2), "att3", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(3), "date", ColumnType.CATEGORICAL);
		}
	}

	@Test
	public void dataContentStartsAtFithRowHeaderRowAsSecondRow() throws DataSetException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());

			// set content start row to 5th row and header row to second row
			dataSource.getResultSetConfiguration().setStartingRow(4);
			dataSource.getResultSetConfiguration().setHasHeaderRow(true);
			dataSource.getResultSetConfiguration().setHeaderRow(1);

			// use default guessed meta data
			dataSource.createMetaData();

			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(0), "5.1", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(1), "3.5", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(2), "1.4", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(3), "0.2", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(4), "id_1", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(5), "Iris-setosa", ColumnType.CATEGORICAL);

			try (DataSet data = dataSource.getData()) {
				assertEquals(-1, data.getNumberOfRows());
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of first, 10th and last row
					if (index == 0) {
						assertEquals(4.6, row.getDouble(0), 1e-10);
						assertEquals(3.1, row.getDouble(1), 1e-10);
						assertEquals(1.5, row.getDouble(2), 1e-10);
						assertEquals(.2, row.getDouble(3), 1e-10);
						assertEquals("id_4", row.getString(4));
						assertEquals("Iris-setosa", row.getString(5));
					} else if (index == 9) {
						assertEquals(4.8, row.getDouble(0), 1e-10);
						assertEquals(3.0, row.getDouble(1), 1e-10);
						assertEquals(1.4, row.getDouble(2), 1e-10);
						assertEquals(.1, row.getDouble(3), 1e-10);
						assertEquals("id_13", row.getString(4));
						assertEquals("Iris-setosa", row.getString(5));
					} else if (index == 146) {
						assertEquals(5.9, row.getDouble(0), 1e-10);
						assertEquals(3.0, row.getDouble(1), 1e-10);
						assertEquals(5.1, row.getDouble(2), 1e-10);
						assertEquals(1.8, row.getDouble(3), 1e-10);
						assertEquals("id_150", row.getString(4));
						assertEquals("Iris-virginica", row.getString(5));
					}
				}

				assertEquals(146, data.getCurrentRowIndex());

				// check reset
				data.reset();
				assertEquals(data.getCurrentRowIndex(), -1);
				assertTrue(data.hasNext());
			}
		}
	}

	@Test
	public void firstDataRowDefined() throws DataSetException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());
			dataSource.getResultSetConfiguration().setHasHeaderRow(true);
			dataSource.getResultSetConfiguration().setHeaderRow(0);
			dataSource.getResultSetConfiguration().setEncoding(StandardCharsets.UTF_8);

			// start with 50th data row
			dataSource.getResultSetConfiguration().setStartingRow(50);

			// use default guessed meta data
			dataSource.createMetaData();

			assertFalse(dataSource.getMetadata().isFaultTolerant());
			assertEquals(Tools.DATE_TIME_FORMAT.get(), dataSource.getMetadata().getDateFormat());
			assertEquals(6, dataSource.getMetadata().getColumnMetaData().size());

			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(0), "a1", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(1), "a2", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(2), "a3133333333333333331311313",
					ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(3), "a4", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(4), "id", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(5), FileDataSourceTestUtils.getUtf8Label(),
					ColumnType.CATEGORICAL);

			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of first and last row
					if (index == 0) {
						assertEquals(5.0, row.getDouble(0), 1e-10);
						assertEquals(3.3, row.getDouble(1), 1e-10);
						assertEquals(1.4, row.getDouble(2), 1e-10);
						assertEquals(.2, row.getDouble(3), 1e-10);
						assertEquals("id_50", row.getString(4));
						assertEquals("Iris-setosa", row.getString(5));
					} else if (index == 100) {
						// check row 150 = 50 + 100, i.e. the 100th row 0-based
						assertEquals(5.9, row.getDouble(0), 1e-10);
						assertEquals(3.0, row.getDouble(1), 1e-10);
						assertEquals(5.1, row.getDouble(2), 1e-10);
						assertEquals(1.8, row.getDouble(3), 1e-10);
						assertEquals("id_150", row.getString(4));
						assertEquals("Iris-virginica", row.getString(5));
					}
				}

				assertEquals(100, data.getCurrentRowIndex());

				// check reset
				data.reset();
				assertEquals(data.getCurrentRowIndex(), -1);
				assertTrue(data.hasNext());
			}
		}
	}

	@Test(expected = StartRowNotFoundException.class)
	public void wrongColumnSeparator() throws DataSetException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());
			dataSource.getResultSetConfiguration().setColumnSeparators("\t");

			// try to guess meta data even though empty
			dataSource.createMetaData();
		}
	}

	@Test
	public void encodingTestUtf8() throws DataSetException, IndexOutOfBoundsException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());
			dataSource.getResultSetConfiguration().setEncoding(Charset.forName("UTF-8"));
			try (DataSet data = dataSource.getData()) {

				int index = -1;

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data 34th row
					if (index == 33) {
						assertEquals("id_34", row.getString(4));
						assertEquals(FileDataSourceTestUtils.getUtf8Entry(), row.getString(5));
						break;
					}
				}

			}
		}
	}

	@Test
	public void encodingTest() throws DataSetException, IndexOutOfBoundsException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());
			dataSource.getResultSetConfiguration().setEncoding(Charset.forName("windows-1250"));
			try (DataSet data = dataSource.getData()) {

				int index = -1;

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data 34th row
					if (index == 33) {
						assertEquals("id_34", row.getString(4));
						assertEquals(FileDataSourceTestUtils.getWindowsEntry(), row.getString(5));
						break;
					}
				}

			}
		}
	}

	@Test
	public void trimLinesEnabled() throws DataSetException, IndexOutOfBoundsException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFileSpaceAsSeparator.toPath());
			dataSource.getResultSetConfiguration().setTrimLines(true);
			dataSource.getResultSetConfiguration().setColumnSeparators(" ");
			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of second row
					if (index == 1) {
						assertEquals("id_2", row.getString(4));
						assertEquals(4.9, row.getDouble(0), 1e-10);
						assertEquals(3.0, row.getDouble(1), 1e-10);
						assertEquals(1.4, row.getDouble(2), 1e-10);
						assertEquals(.2, row.getDouble(3), 1e-10);
						assertEquals("Iris-setosa", row.getString(5));
					}
				}
			}
		}
	}

	@Test
	public void trimLinesDisabled() throws DataSetException, IndexOutOfBoundsException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFileSpaceAsSeparator.toPath());
			dataSource.getResultSetConfiguration().setColumnSeparators(" ");
			dataSource.getResultSetConfiguration().setTrimLines(false);
			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of second row
					if (index == 1) {
						assertEquals("id_2", row.getString(6));
						assertTrue(row.isMissing(0));
						assertTrue(row.isMissing(1));
						assertEquals(4.9, row.getDouble(2), 1e-10);
						assertEquals(3.0, row.getDouble(3), 1e-10);
						assertEquals(1.4, row.getDouble(4), 1e-10);
						assertEquals(.2, row.getDouble(5), 1e-10);
						assertEquals("Iris-setosa", row.getString(7));
					}
				}
			}
		}
	}

	@Test
	public void ignoreCommentsEnabled() throws DataSetException, IndexOutOfBoundsException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFileCommentsQuotesAndEscape.toPath());
			dataSource.getResultSetConfiguration().setSkipComments(true);
			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of 42nd and 43rd row
					if (index == 41) {
						assertEquals("id_42", row.getString(4));
					} else if (index == 42) {
						assertEquals("id_44", row.getString(4));
					}
				}
			}
		}
	}

	@Test
	public void ignoreCommentsDisabled() throws DataSetException, IndexOutOfBoundsException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFileCommentsQuotesAndEscape.toPath());
			dataSource.getResultSetConfiguration().setSkipComments(false);
			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of 42nd, 43rd and 44th row
					if (index == 41) {
						assertEquals("id_42", row.getString(4));
					} else if (index == 42) {
						assertEquals("id_43", row.getString(4));
						assertEquals("#4.4", row.getString(0));
					} else if (index == 43) {
						assertEquals("id_44", row.getString(4));
						assertEquals("%5.0", row.getString(0));
					}
				}
			}
		}
	}

	@Test
	public void ignoreCommentsEnabledOtherCharacter() throws DataSetException, IndexOutOfBoundsException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFileCommentsQuotesAndEscape.toPath());
			dataSource.getResultSetConfiguration().setSkipComments(true);
			dataSource.getResultSetConfiguration().setCommentCharacters("%");
			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of 42nd, 43rd and 44th row
					if (index == 41) {
						assertEquals("id_42", row.getString(4));
					} else if (index == 42) {
						assertEquals("id_43", row.getString(4));
						assertEquals("#4.4", row.getString(0));
					} else if (index == 43) {
						assertEquals("id_45", row.getString(4));
					}
				}
			}
		}
	}

	@Test
	public void usingQuotesAndStandardCharacters() throws DataSetException, IndexOutOfBoundsException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFileCommentsQuotesAndEscape.toPath());
			dataSource.getResultSetConfiguration().setUseQuotes(true);
			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					if (index == 0) {
						assertEquals("id_1", row.getString(4));
						assertEquals("5.1;\"3.5;1.4", row.getString(5));
					} else if (index == 1) {
						assertEquals("id_2", row.getString(4));
						assertEquals("Iris-setosa;", row.getString(5));
					} else if (index == 2) {
						assertEquals("id_3", row.getString(4));
						assertEquals("5.1;3.5;1.4", row.getString(5));
					}
				}
			}
		}
	}

	@Test
	public void notUsingQuotesAndStandardCharacters() throws DataSetException, IndexOutOfBoundsException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFileCommentsQuotesAndEscape.toPath());
			dataSource.getResultSetConfiguration().setUseQuotes(false);
			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					if (index == 0) {
						assertEquals("\"id_1\"", row.getString(4));
						assertEquals("\"5.1", row.getString(5));
						assertEquals("\"3.5", row.getString(6));
						assertEquals("1.4\"", row.getString(7));
					} else if (index == 1) {
						assertEquals("\"id_2\"", row.getString(4));
						assertEquals("\"Iris-setosa;\"", row.getString(5));
					} else if (index == 2) {
						assertEquals("\"id_3\"", row.getString(4));
						assertEquals("5.1;3.5;1.4", row.getString(5));
					}
				}
			}
		}
	}

	@Test
	public void notUsingQuotesAndOtherCharacters() throws DataSetException, IndexOutOfBoundsException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFileCommentsQuotesAndEscape.toPath());
			dataSource.getResultSetConfiguration().setUseQuotes(false);
			dataSource.getResultSetConfiguration().setQuoteCharacter('`');
			dataSource.getResultSetConfiguration().setEscapeCharacter('%');
			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					if (index == 0) {
						assertEquals("\"id_1\"", row.getString(4));
						assertEquals("\"5.1", row.getString(5));
						assertEquals("\\\"3.5", row.getString(6));
						assertEquals("1.4\"", row.getString(7));
					} else if (index == 1) {
						assertEquals("\"id_2\"", row.getString(4));
						assertEquals("\"Iris-setosa\\", row.getString(5));
						assertEquals("\"", row.getString(6));
					} else if (index == 2) {
						assertEquals("\"id_3\"", row.getString(4));
						assertEquals("5.1\\", row.getString(5));
						assertEquals("3.5\\", row.getString(6));
						assertEquals("1.4", row.getString(7));
					}
				}
			}
		}
	}

	@Test
	public void usingOtherQuotes() throws DataSetException, IndexOutOfBoundsException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFileCommentsQuotesAndEscape.toPath());
			dataSource.getResultSetConfiguration().setUseQuotes(true);
			dataSource.getResultSetConfiguration().setQuoteCharacter('`');
			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of 42nd and 43rd row
					if (index == 0) {
						assertEquals("\"id_1\"", row.getString(4));
						assertEquals("\"5.1", row.getString(5));
						assertEquals("\"3.5", row.getString(6));
						assertEquals("1.4\"", row.getString(7));
					} else if (index == 1) {
						assertEquals("\"id_2\"", row.getString(4));
						assertEquals("\"Iris-setosa;\"", row.getString(5));
					} else if (index == 2) {
						assertEquals("\"id_3\"", row.getString(4));
						assertEquals("5.1;3.5;1.4", row.getString(5));
					} else if (index == 3) {
						assertEquals("id_4", row.getString(4));
						assertEquals("5.1;`3.5;1.4;", row.getString(5));
					}
				}
			}
		}
	}

	@Test
	public void usingOtherEscapeCharacter() throws DataSetException, IndexOutOfBoundsException, ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFileCommentsQuotesAndEscape.toPath());
			dataSource.getResultSetConfiguration().setUseQuotes(true);
			dataSource.getResultSetConfiguration().setEscapeCharacter('%');
			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of 42nd and 43rd row
					if (index == 0) {
						assertEquals("id_2", row.getString(4));
						assertEquals("Iris-setosa\\;", row.getString(5));
					} else if (index == 1) {
						assertEquals("id_3", row.getString(4));
						assertEquals("5.1\\", row.getString(5));
						assertEquals("3.5\\", row.getString(6));
						assertEquals("1.4", row.getString(7));
					} else if (index == 2) {
						assertEquals("`id_4`", row.getString(4));
						assertEquals("`5.1", row.getString(5));
						assertEquals("\\`3.5", row.getString(6));
						assertEquals("1.4\\", row.getString(7));
						assertEquals("`", row.getString(8));
					} else if (index == 3) {
						assertEquals("id_5", row.getString(4));
						assertEquals("5.3;5.3", row.getString(5));
					}
				}
			}
		}
	}

	@Test
	public void cachingTest() throws DataSetException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());

			DataSet firstPreviewSet = dataSource.getPreview(10);
			DataSet firstDataSet = dataSource.getData();
			DataSet secondPreviewSet = dataSource.getPreview(10);
			DataSet secondDataSet = dataSource.getData();
			assertTrue(firstPreviewSet == secondPreviewSet);
			assertTrue(firstDataSet == secondDataSet);

			dataSource.getResultSetConfiguration().setColumnSeparators("\t");
			DataSet thirdPreviewSet = dataSource.getPreview(10);
			DataSet thirdDataSet = dataSource.getData();
			assertFalse(thirdPreviewSet == firstPreviewSet);
			assertFalse(thirdDataSet == firstDataSet);
		}
	}

	@Test
	public void lengthTest() throws DataSetException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());

			DataSet preview = dataSource.getPreview(10);
			preview.reset();
			assertEquals(-1, preview.getCurrentRowIndex());
			while (preview.hasNext()) {
				preview.nextRow();
			}
			assertEquals(9, preview.getCurrentRowIndex());

			DataSet set = dataSource.getData();
			set.reset();
			assertEquals(-1, set.getCurrentRowIndex());
			while (set.hasNext()) {
				set.nextRow();
			}
			assertEquals(149, set.getCurrentRowIndex());

			DataSet secondPreview = dataSource.getPreview(10);
			secondPreview.reset();
			assertEquals(-1, secondPreview.getCurrentRowIndex());
			while (secondPreview.hasNext()) {
				secondPreview.nextRow();
			}
			assertEquals(9, secondPreview.getCurrentRowIndex());
		}
	}

	@Test
	public void simpleNominalToDateTest()
			throws DataSetException, ParseException, IndexOutOfBoundsException, java.text.ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(nominalDateTestFile.toPath());
			dataSource.getResultSetConfiguration().setColumnSeparators(";");
			dataSource.getResultSetConfiguration().setHasHeaderRow(true);
			dataSource.getResultSetConfiguration().setHeaderRow(0);
			dataSource.getResultSetConfiguration().setEncoding(StandardCharsets.UTF_8);

			// use default guessed meta data
			dataSource.createMetaData();

			int dateColumnIndex = 6;
			SimpleDateFormat dateFormat = new SimpleDateFormat(ParameterTypeDateFormat.DATE_TIME_FORMAT_M_D_YY_H_MM_A);

			// check meta data and set to date
			assertEquals(7, dataSource.getMetadata().getColumnMetaData().size());
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(dateColumnIndex), "date", ColumnType.CATEGORICAL);
			dataSource.getMetadata().getColumnMetaData().get(dateColumnIndex).setType(ColumnType.DATETIME);

			// set correct date format
			dataSource.getMetadata().setDateFormat(dateFormat);

			DataSet ds = dataSource.getData();
			while (ds.hasNext()) {
				DataSetRow row = ds.nextRow();
				if (ds.getCurrentRowIndex() != 64) {
					assertFalse(row.isMissing(dateColumnIndex));
				} else {
					assertTrue(row.isMissing(dateColumnIndex));
				}

				if (ds.getCurrentRowIndex() == 20) {
					assertEquals(dateFormat.parse("2/2/17 8:24 AM"), row.getDate(dateColumnIndex));
				} else if (ds.getCurrentRowIndex() == 50) {
					assertEquals(dateFormat.parse("6/11/16 12:24 PM"), row.getDate(dateColumnIndex));
				}
			}

		}
	}

	@Test(expected = ParseException.class)
	public void wrongDateFormatTest()
			throws DataSetException, ParseException, IndexOutOfBoundsException, java.text.ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(nominalDateTestFile.toPath());
			dataSource.getResultSetConfiguration().setColumnSeparators(";");
			dataSource.getResultSetConfiguration().setHasHeaderRow(true);
			dataSource.getResultSetConfiguration().setHeaderRow(0);
			dataSource.getResultSetConfiguration().setEncoding(StandardCharsets.UTF_8);

			// use default guessed meta data
			dataSource.createMetaData();

			int dateColumnIndex = 6;

			// check meta data and set to date
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(dateColumnIndex), "date", ColumnType.CATEGORICAL);
			dataSource.getMetadata().getColumnMetaData().get(dateColumnIndex).setType(ColumnType.DATETIME);

			DataSet ds = dataSource.getData();
			while (ds.hasNext()) {
				DataSetRow row = ds.nextRow();

				// will throw a parse exception
				row.getDate(dateColumnIndex);
			}

		}
	}

	@Test
	public void testLastRowAsStartAndHeaderRow()
			throws DataSetException, ParseException, IndexOutOfBoundsException, java.text.ParseException {
		try (CSVDataSource dataSource = new CSVDataSource()) {
			dataSource.setLocation(simpleTestFile.toPath());
			dataSource.getResultSetConfiguration().setHasHeaderRow(true);
			dataSource.getResultSetConfiguration().setHeaderRow(150);
			dataSource.getResultSetConfiguration().setStartingRow(150);

			DataSet ds = dataSource.getData();
			assertFalse(ds.hasNext());
		}
	}

	private void assertFirstSheetRowContent(DataSetRow row) throws ParseException {
		assertFirstSheetRowContent(row, 0);
	}

	private void assertFirstSheetRowContent(DataSetRow row, int firstColumn) throws ParseException {
		if (firstColumn < 1) {
			assertEquals(5.1, row.getDouble(0), 1e-10);
		}
		if (firstColumn < 2) {
			assertEquals(3.5, row.getDouble(1 - firstColumn), 1e-10);
		}
		if (firstColumn < 3) {
			assertEquals(1.4, row.getDouble(2 - firstColumn), 1e-10);
		}
		assertEquals(0.2, row.getDouble(3 - firstColumn), 1e-10);
		assertEquals("id_1", row.getString(4 - firstColumn));
		assertEquals("Iris-setosa", row.getString(5 - firstColumn));
	}

	private void checkColumnMetaData(ColumnMetaData columnMetaData, String name, ColumnType type) {
		checkColumnMetaData(columnMetaData, name, null, type);
	}

	private void checkColumnMetaData(ColumnMetaData columnMetaData, String name, String role, ColumnType type) {
		assertEquals(name, columnMetaData.getName());
		assertEquals(role, columnMetaData.getRole());
		assertEquals(type, columnMetaData.getType());
	}

}
