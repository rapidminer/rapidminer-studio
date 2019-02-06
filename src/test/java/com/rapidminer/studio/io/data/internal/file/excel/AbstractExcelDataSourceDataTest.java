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
import java.text.SimpleDateFormat;

import org.junit.Test;

import com.rapidminer.core.io.data.ColumnMetaData;
import com.rapidminer.core.io.data.ColumnMetaData.ColumnType;
import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetRow;
import com.rapidminer.core.io.data.ParseException;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.studio.io.data.HeaderRowBehindStartRowException;
import com.rapidminer.studio.io.data.HeaderRowNotFoundException;
import com.rapidminer.studio.io.data.StartRowNotFoundException;
import com.rapidminer.studio.io.data.internal.ResultSetAdapter;
import com.rapidminer.studio.io.data.internal.file.FileDataSourceTestUtils;
import com.rapidminer.tools.Tools;


/**
 * Abstract super class for all Excel tests
 *
 * @author Nils Woehler
 *
 */
public abstract class AbstractExcelDataSourceDataTest {

	static File testFile;
	static File nominalDateTestFile;
	static File dateDateTestFile;

	@Test
	public void defaultMetaDataTest() throws DataSetException, ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			dataSource.setHeaderRowIndex(0);
			configureDataSource(dataSource);

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

	@Test(expected = HeaderRowBehindStartRowException.class)
	public void headerRowBehindStartRow() throws DataSetException, ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			configureDataSource(dataSource);

			// set header row behind the start row
			dataSource.getResultSetConfiguration().setSheet(0);
			dataSource.setHeaderRowIndex(1);

			// configure meta data
			dataSource.createMetaData();
		}
	}

	@Test(expected = HeaderRowBehindStartRowException.class)
	public void headerRowBehindStartRow2() throws DataSetException, ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			configureDataSource(dataSource);

			// set header row behind data start row
			dataSource.getResultSetConfiguration().setRowOffset(10);
			dataSource.setHeaderRowIndex(15);

			// configure the meta data
			dataSource.createMetaData();
		}
	}

	@Test(expected = StartRowNotFoundException.class)
	public void startRowNotAvailable() throws DataSetException, ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			configureDataSource(dataSource);

			// set start row behind actual data content
			dataSource.getResultSetConfiguration().setRowOffset(151);
			dataSource.setHeaderRowIndex(150);

			// configure the meta data
			dataSource.createMetaData();
		}
	}

	@Test(expected = HeaderRowNotFoundException.class)
	public void headerRowNotFound() throws DataSetException, ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			dataSource.getResultSetConfiguration().setRowOffset(155);
			dataSource.setHeaderRowIndex(155);
			dataSource.createMetaData();
		}
	}

	@Test
	public void dataContentStartsAtFithRow() throws DataSetException, ParseException {

		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			dataSource.getResultSetConfiguration().setRowOffset(4);
			dataSource.setHeaderRowIndex(0);
			configureDataSource(dataSource);

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

		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			dataSource.getResultSetConfiguration().setRowOffset(0);
			dataSource.setHeaderRowIndex(ResultSetAdapter.NO_HEADER_ROW);
			configureDataSource(dataSource);

			// use default guessed meta data
			dataSource.createMetaData();

			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(0), "A", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(1), "B", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(2), "C", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(3), "D", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(4), "E", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(5), "F", ColumnType.CATEGORICAL);

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
						assertEquals(getCategoricalBelowOne(), row.getString(3));
						assertEquals("id_9", row.getString(4));
						assertEquals("Iris-setosa", row.getString(5));
					} else if (index == 150) {
						assertEquals("5.9", row.getString(0));
						assertEquals(getCategoricalInteger(), row.getString(1));
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

	/**
	 * @return result for categorical integer number
	 */
	protected abstract String getCategoricalInteger();

	/**
	 * @return result for categorical number below 1.0
	 */
	protected abstract String getCategoricalBelowOne();

	@Test
	public void missingInHeaderRow() throws DataSetException, ParseException {

		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheetByName("Tabelle3");
			dataSource.getResultSetConfiguration().setSheetSelectionMode(ExcelResultSetConfiguration.SheetSelectionMode.BY_NAME);
			dataSource.getResultSetConfiguration().setRowOffset(0);
			dataSource.setHeaderRowIndex(0);
			configureDataSource(dataSource);

			// use default guessed meta data
			dataSource.createMetaData();

			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(0), "a1", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(1), "id", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(2), "label", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(3), "D", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(4), "a4", ColumnType.REAL);
		}
	}

	@Test
	public void dataContentStartsAtFithRowHeaderRowAsSecondRow() throws DataSetException, ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			configureDataSource(dataSource);

			// set content start row to 5th row and header row to second row
			dataSource.getResultSetConfiguration().setRowOffset(4);
			dataSource.setHeaderRowIndex(1);

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
	public void firstColumnSkipped() throws DataSetException, ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			dataSource.setHeaderRowIndex(0);
			configureDataSource(dataSource);

			// skip first column
			dataSource.getResultSetConfiguration().setColumnOffset(1);

			// use default guessed meta data
			dataSource.createMetaData();

			assertEquals(5, dataSource.getMetadata().getColumnMetaData().size());

			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(0), "a2", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(1), "a3133333333333333331311313",
					ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(2), "a4", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(3), "id", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(4), FileDataSourceTestUtils.getUtf8Label(),
					ColumnType.CATEGORICAL);

			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of first, 10th and last row
					if (index == 0) {
						assertFirstSheetRowContent(row, 1);
					} else if (index == 9) {
						assertEquals(3.1, row.getDouble(0), 1e-10);
						assertEquals(1.5, row.getDouble(1), 1e-10);
						assertEquals(.1, row.getDouble(2), 1e-10);
						assertEquals("id_10", row.getString(3));
						assertEquals("Iris-setosa", row.getString(4));
					} else if (index == 149) {
						assertEquals(3.0, row.getDouble(0), 1e-10);
						assertEquals(5.1, row.getDouble(1), 1e-10);
						assertEquals(1.8, row.getDouble(2), 1e-10);
						assertEquals("id_150", row.getString(3));
						assertEquals("Iris-virginica", row.getString(4));
					}
				}

				assertEquals(149, data.getCurrentRowIndex());

				// check reset
				data.reset();
				assertEquals(data.getCurrentRowIndex(), -1);
				assertTrue(data.hasNext());
				assertFirstSheetRowContent(data.nextRow(), 1);
			}
		}
	}

	@Test
	public void firstAndSecondColumnSkipped() throws DataSetException, ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			dataSource.setHeaderRowIndex(0);
			configureDataSource(dataSource);

			// skip first two columns
			dataSource.getResultSetConfiguration().setColumnOffset(2);

			// use default guessed meta data
			dataSource.createMetaData();

			assertEquals(4, dataSource.getMetadata().getColumnMetaData().size());

			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(0), "a3133333333333333331311313",
					ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(1), "a4", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(2), "id", ColumnType.CATEGORICAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(3), FileDataSourceTestUtils.getUtf8Label(),
					ColumnType.CATEGORICAL);

			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of first, 10th and last row
					if (index == 0) {
						assertFirstSheetRowContent(row, 2);
					} else if (index == 9) {
						assertEquals(1.5, row.getDouble(0), 1e-10);
						assertEquals(.1, row.getDouble(1), 1e-10);
						assertEquals("id_10", row.getString(2));
						assertEquals("Iris-setosa", row.getString(3));
					} else if (index == 149) {
						assertEquals(5.1, row.getDouble(0), 1e-10);
						assertEquals(1.8, row.getDouble(1), 1e-10);
						assertEquals("id_150", row.getString(2));
						assertEquals("Iris-virginica", row.getString(3));
					}
				}

				assertEquals(149, data.getCurrentRowIndex());

				// check reset
				data.reset();
				assertEquals(data.getCurrentRowIndex(), -1);
				assertTrue(data.hasNext());
				assertFirstSheetRowContent(data.nextRow(), 2);
			}
		}
	}

	@Test
	public void lastColumnSkipped() throws DataSetException, ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			dataSource.setHeaderRowIndex(0);
			configureDataSource(dataSource);

			// remove last column
			dataSource.getResultSetConfiguration().setColumnLast(4);

			// use default guessed meta data
			dataSource.createMetaData();

			assertEquals(5, dataSource.getMetadata().getColumnMetaData().size());

			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(0), "a1", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(1), "a2", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(2), "a3133333333333333331311313",
					ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(3), "a4", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(4), "id", ColumnType.CATEGORICAL);

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
						assertFirstSheetRowContent(row, 0, 4);
					} else if (index == 9) {
						assertEquals(4.9, row.getDouble(0), 1e-10);
						assertEquals(3.1, row.getDouble(1), 1e-10);
						assertEquals(1.5, row.getDouble(2), 1e-10);
						assertEquals(.1, row.getDouble(3), 1e-10);
						assertEquals("id_10", row.getString(4));
					} else if (index == 149) {
						assertEquals(5.9, row.getDouble(0), 1e-10);
						assertEquals(3.0, row.getDouble(1), 1e-10);
						assertEquals(5.1, row.getDouble(2), 1e-10);
						assertEquals(1.8, row.getDouble(3), 1e-10);
						assertEquals("id_150", row.getString(4));
					}
				}

				assertEquals(149, data.getCurrentRowIndex());

				// check reset
				data.reset();
				assertEquals(data.getCurrentRowIndex(), -1);
				assertTrue(data.hasNext());
				assertFirstSheetRowContent(data.nextRow(), 0, 4);
			}
		}
	}

	@Test
	public void firstAndLastColumnSkipped() throws DataSetException, ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			dataSource.setHeaderRowIndex(0);
			configureDataSource(dataSource);

			// remove first column
			dataSource.getResultSetConfiguration().setColumnOffset(1);
			// remove last column
			dataSource.getResultSetConfiguration().setColumnLast(4);

			// use default guessed meta data
			dataSource.createMetaData();

			assertEquals(4, dataSource.getMetadata().getColumnMetaData().size());

			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(0), "a2", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(1), "a3133333333333333331311313",
					ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(2), "a4", ColumnType.REAL);
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(3), "id", ColumnType.CATEGORICAL);

			try (DataSet data = dataSource.getData()) {
				assertTrue(data.hasNext());

				int index = -1;
				assertEquals(index, data.getCurrentRowIndex());

				while (data.hasNext()) {
					DataSetRow row = data.nextRow();

					++index;
					assertEquals(index, data.getCurrentRowIndex());

					// check data content of first, 10th and last row
					if (index == 0) {
						assertFirstSheetRowContent(row, 1, 4);
					} else if (index == 9) {
						assertEquals(3.1, row.getDouble(0), 1e-10);
						assertEquals(1.5, row.getDouble(1), 1e-10);
						assertEquals(.1, row.getDouble(2), 1e-10);
						assertEquals("id_10", row.getString(3));
					} else if (index == 149) {
						assertEquals(3.0, row.getDouble(0), 1e-10);
						assertEquals(5.1, row.getDouble(1), 1e-10);
						assertEquals(1.8, row.getDouble(2), 1e-10);
						assertEquals("id_150", row.getString(3));
					}
				}

				assertEquals(149, data.getCurrentRowIndex());

				// check reset
				data.reset();
				assertEquals(data.getCurrentRowIndex(), -1);
				assertTrue(data.hasNext());
				assertFirstSheetRowContent(data.nextRow(), 1, 4);
			}
		}
	}

	@Test
	public void lastRowDefined() throws DataSetException, ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			dataSource.setHeaderRowIndex(0);
			configureDataSource(dataSource);

			// set last row to 100th data row
			// (index = 100 because column names are retrieved from index 0)
			dataSource.getResultSetConfiguration().setRowLast(100);

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

					// check data content of first, 10th and last row
					if (index == 0) {
						assertFirstSheetRowContent(row);
					} else if (index == 9) {
						assertEquals(4.9, row.getDouble(0), 1e-10);
						assertEquals(3.1, row.getDouble(1), 1e-10);
						assertEquals(1.5, row.getDouble(2), 1e-10);
						assertEquals(.1, row.getDouble(3), 1e-10);
						assertEquals("id_10", row.getString(4));
						assertEquals("Iris-setosa", row.getString(5));
					} else if (index == 99) {
						assertEquals(5.7, row.getDouble(0), 1e-10);
						assertEquals(2.8, row.getDouble(1), 1e-10);
						assertEquals(4.1, row.getDouble(2), 1e-10);
						assertEquals(1.3, row.getDouble(3), 1e-10);
						assertEquals("id_100", row.getString(4));
						assertEquals("Iris-versicolor", row.getString(5));
					}
				}

				assertEquals(99, data.getCurrentRowIndex());

				// check reset
				data.reset();
				assertEquals(data.getCurrentRowIndex(), -1);
				assertTrue(data.hasNext());
				assertFirstSheetRowContent(data.nextRow());
			}
		}
	}

	@Test
	public void firstAndLastDataRowDefined() throws DataSetException, ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			dataSource.setHeaderRowIndex(0);
			configureDataSource(dataSource);

			// start with 50th data row
			dataSource.getResultSetConfiguration().setRowOffset(50);

			// set last row to 100th data row
			// (index = 100 because column names are retrieved from index 0)
			dataSource.getResultSetConfiguration().setRowLast(100);

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
					} else if (index == 50) {
						assertEquals(5.7, row.getDouble(0), 1e-10);
						assertEquals(2.8, row.getDouble(1), 1e-10);
						assertEquals(4.1, row.getDouble(2), 1e-10);
						assertEquals(1.3, row.getDouble(3), 1e-10);
						assertEquals("id_100", row.getString(4));
						assertEquals("Iris-versicolor", row.getString(5));
					}
				}

				assertEquals(50, data.getCurrentRowIndex());

				// check reset
				data.reset();
				assertEquals(data.getCurrentRowIndex(), -1);
				assertTrue(data.hasNext());

				// check first row one more time
				DataSetRow row = data.nextRow();
				assertEquals(5.0, row.getDouble(0), 1e-10);
				assertEquals(3.3, row.getDouble(1), 1e-10);
				assertEquals(1.4, row.getDouble(2), 1e-10);
				assertEquals(.2, row.getDouble(3), 1e-10);
				assertEquals("id_50", row.getString(4));
				assertEquals("Iris-setosa", row.getString(5));
			}
		}
	}

	@Test
	public void cachingTest() throws DataSetException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);

			DataSet firstPreviewSet = dataSource.getPreview(500);
			DataSet firstDataSet = dataSource.getData();
			DataSet secondPreviewSet = dataSource.getPreview(500);
			DataSet secondDataSet = dataSource.getData();
			assertTrue(firstPreviewSet == secondPreviewSet);
			assertTrue(firstDataSet == secondDataSet);

			dataSource.getResultSetConfiguration().setColumnOffset(3);
			DataSet thirdPreviewSet = dataSource.getPreview(500);
			DataSet thirdDataSet = dataSource.getData();
			assertFalse(thirdPreviewSet == firstPreviewSet);
			assertFalse(thirdDataSet == firstDataSet);
		}
	}

	@Test
	public void onlyHeaderRowSelected() throws DataSetException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			dataSource.setHeaderRowIndex(0);
			dataSource.getResultSetConfiguration().setRowLast(0);
			dataSource.getResultSetConfiguration().setRowOffset(0);

			DataSet firstDataSet = dataSource.getData();
			assertFalse(firstDataSet.hasNext());
		}
	}

	@Test
	public void lengthTest() throws DataSetException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(testFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			dataSource.getResultSetConfiguration().setRowOffset(3);
			configureDataSource(dataSource);

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
			assertEquals(147, set.getCurrentRowIndex());

		}
	}

	@Test
	public void simpleNominalToDateTest()
			throws DataSetException, ParseException, IndexOutOfBoundsException, java.text.ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(nominalDateTestFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			configureDataSource(dataSource);

			// use default guessed meta data
			dataSource.createMetaData();

			int dateColumnIndex = 6;
			SimpleDateFormat dateFormat = new SimpleDateFormat(ParameterTypeDateFormat.DATE_TIME_FORMAT_M_D_YY_H_MM_A);

			// check meta data and set to date
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(dateColumnIndex), "date", ColumnType.CATEGORICAL);
			dataSource.getMetadata().getColumnMetaData().get(dateColumnIndex).setType(ColumnType.DATETIME);

			// set correct date format
			dataSource.getMetadata().setDateFormat(dateFormat);

			DataSet ds = dataSource.getData();
			while (ds.hasNext()) {
				DataSetRow row = ds.nextRow();
				if (ds.getCurrentRowIndex() != 65) {
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
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(nominalDateTestFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			configureDataSource(dataSource);

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
	public void simpleDateToDateTest()
			throws DataSetException, ParseException, IndexOutOfBoundsException, java.text.ParseException {
		try (ExcelDataSource dataSource = new ExcelDataSource()) {
			dataSource.setLocation(dateDateTestFile.toPath());
			dataSource.getResultSetConfiguration().setSheet(0);
			configureDataSource(dataSource);

			// use default guessed meta data
			dataSource.createMetaData();

			int dateColumnIndex = 6;

			// check meta data and set to date
			checkColumnMetaData(dataSource.getMetadata().getColumnMetaData(dateColumnIndex), "date", ColumnType.DATETIME);

			DataSet ds = dataSource.getData();
			while (ds.hasNext()) {
				DataSetRow row = ds.nextRow();
				if (ds.getCurrentRowIndex() != 65) {
					assertFalse(row.isMissing(dateColumnIndex));
				} else {
					assertTrue(row.isMissing(dateColumnIndex));
				}

				if (ds.getCurrentRowIndex() == 20) {
					assertEquals(getDataToDateRow20(), row.getDate(dateColumnIndex).getTime());
				} else if (ds.getCurrentRowIndex() == 50) {
					assertEquals(getDateToDateRow50(), row.getDate(dateColumnIndex).getTime());
				}
			}

		}
	}

	/**
	 * @return the date value at row 20
	 */
	protected abstract long getDataToDateRow20();

	/**
	 * @return the date value at row 50
	 */
	protected abstract long getDateToDateRow50();

	/**
	 * Called by every test that uses a {@link ExcelDataSource} to adapt to XLS and XLSX special
	 * configurations
	 *
	 * @param dataSource
	 *            the data source to configure
	 */
	protected abstract void configureDataSource(ExcelDataSource dataSource);

	private void assertFirstSheetRowContent(DataSetRow row) throws ParseException {
		assertFirstSheetRowContent(row, 0, 5);
	}

	private void assertFirstSheetRowContent(DataSetRow row, int firstColumn) throws ParseException {
		assertFirstSheetRowContent(row, firstColumn, 5);
	}

	private void assertFirstSheetRowContent(DataSetRow row, int firstColumn, int lastColumn) throws ParseException {
		if (firstColumn < 1) {
			assertEquals(5.1, row.getDouble(0), 1e-10);
		}
		if (firstColumn < 2) {
			assertEquals(3.5, row.getDouble(1 - firstColumn), 1e-10);
		}
		if (firstColumn < 3 && lastColumn >= 2) {
			assertEquals(1.4, row.getDouble(2 - firstColumn), 1e-10);
		}
		if (firstColumn < 4 && lastColumn >= 3) {
			assertEquals(0.2, row.getDouble(3 - firstColumn), 1e-10);
		}
		if (firstColumn < 5 && lastColumn >= 4) {
			assertEquals("id_1", row.getString(4 - firstColumn));
		}
		if (firstColumn < 6 && lastColumn >= 5) {
			assertEquals("Iris-setosa", row.getString(5 - firstColumn));
		}
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
