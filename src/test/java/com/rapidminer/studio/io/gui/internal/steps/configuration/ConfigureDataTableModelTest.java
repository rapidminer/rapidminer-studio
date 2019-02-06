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
package com.rapidminer.studio.io.gui.internal.steps.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.core.io.data.ColumnMetaData;
import com.rapidminer.core.io.data.ColumnMetaData.ColumnType;
import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetMetaData;
import com.rapidminer.core.io.data.DataSetRow;
import com.rapidminer.core.io.data.ParseException;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.core.io.data.source.DataSourceConfiguration;
import com.rapidminer.studio.io.data.DefaultColumnMetaData;
import com.rapidminer.studio.io.gui.internal.steps.configuration.ConfigureDataTableModel;
import com.rapidminer.studio.io.gui.internal.steps.configuration.ParsingError;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;


/**
 * Tests the {@link ConfigureDataTableModel}.
 *
 * @author Gisa Schaefer
 *
 */
public class ConfigureDataTableModelTest {

	private static int numberOfRows = 10;
	private static DataSetRow row;
	private static DataSet dataSet;
	private static Date testDate = new Date();
	private static DataSource dataSource;
	private static List<ColumnMetaData> columnMetaData;
	private static DataSetMetaData metaData;

	@BeforeClass
	public static void setUpForAll() {
		// row with number, String, date, missing
		row = new DataSetRow() {

			@Override
			public Date getDate(int columnIndex) throws ParseException {
				switch (columnIndex) {
					case 0:
					case 1:
						throw new ParseException("not a date");
					case 2:
						return testDate;
				}
				return null;
			}

			@Override
			public String getString(int columnIndex) throws ParseException {
				switch (columnIndex) {
					case 0:
						return "5.27";
					case 1:
						return "Xxxx";
					case 2:
						return "1.1.1900";

				}
				return null;
			}

			@Override
			public double getDouble(int columnIndex) throws ParseException {
				switch (columnIndex) {
					case 0:
						return 5.27;
					case 1:
					case 2:
						throw new ParseException("not a number");
				}
				return Double.NaN;
			}

			@Override
			public boolean isMissing(int columnIndex) {
				switch (columnIndex) {
					case 0:
					case 1:
					case 2:
						return false;
				}
				return true;
			}

		};

		dataSet = new DataSet() {

			int counter = -1;

			@Override
			public boolean hasNext() {
				return counter < numberOfRows - 1;
			}

			@Override
			public DataSetRow nextRow() throws DataSetException, NoSuchElementException {
				counter++;
				return row;
			}

			@Override
			public int getCurrentRowIndex() {
				return counter;
			}

			@Override
			public void reset() throws DataSetException {
				counter = -1;
			}

			@Override
			public int getNumberOfColumns() {
				return 4;
			}

			@Override
			public int getNumberOfRows() {
				return numberOfRows;
			}

			@Override
			public void close() throws DataSetException {}

		};

		columnMetaData = new ArrayList<ColumnMetaData>(
				Arrays.asList(new ColumnMetaData[] { new DefaultColumnMetaData("att1", ColumnType.REAL),
						new DefaultColumnMetaData("att2", ColumnType.CATEGORICAL),
						new DefaultColumnMetaData("att3", ColumnType.DATE),
						new DefaultColumnMetaData("att4", ColumnType.INTEGER) }));

		metaData = new DataSetMetaData() {

			@Override
			public void setFaultTolerant(boolean faultTolerant) {}

			@Override
			public boolean isFaultTolerant() {
				return false;
			}

			@Override
			public DateFormat getDateFormat() {
				return null;
			}

			@Override
			public ColumnMetaData getColumnMetaData(int columnIndex) {
				return columnMetaData.get(columnIndex);
			}

			@Override
			public List<ColumnMetaData> getColumnMetaData() {
				return columnMetaData;
			}

			@Override
			public DataSetMetaData copy() {
				return null;
			}

			@Override
			public void configure(DataSetMetaData other) {}

			@Override
			public void setDateFormat(DateFormat dateFormat) {}
		};

		dataSource = new DataSource() {

			@Override
			public DataSourceConfiguration getConfiguration() {
				return null;
			}

			@Override
			public void configure(DataSourceConfiguration configuration) throws DataSetException {}

			@Override
			public void close() throws DataSetException {}

			@Override
			public DataSet getPreview(int maxPreviewSize) throws DataSetException {
				return dataSet;
			}

			@Override
			public DataSetMetaData getMetadata() {
				return metaData;
			}

			@Override
			public DataSet getData() throws DataSetException {
				return dataSet;
			}
		};
	}

	@Before
	public void setUp() {
		try {
			dataSet.reset();
		} catch (DataSetException e) {
			// cannot happen
		}
		columnMetaData = new ArrayList<ColumnMetaData>(
				Arrays.asList(new ColumnMetaData[] { new DefaultColumnMetaData("att1", ColumnType.REAL),
						new DefaultColumnMetaData("att2", ColumnType.CATEGORICAL),
						new DefaultColumnMetaData("att3", ColumnType.DATE),
						new DefaultColumnMetaData("att4", ColumnType.INTEGER) }));
	}

	@Test
	public void dataCreationColumnNames() throws DataSetException {
		ConfigureDataTableModel model = new ConfigureDataTableModel(dataSource, metaData, null);
		assertEquals(4, model.getColumnCount());
		assertEquals("att1", model.getColumnName(0));
		assertEquals("att2", model.getColumnName(1));
		assertEquals("att3", model.getColumnName(2));
		assertEquals("att4", model.getColumnName(3));
	}

	@Test
	public void dataCreationDataValues() throws DataSetException {
		ConfigureDataTableModel model = new ConfigureDataTableModel(dataSource, metaData, null);
		assertEquals(numberOfRows, model.getRowCount());
		for (int i = 0; i < numberOfRows; i++) {
			assertEquals("5.270", model.getValueAt(i, 0));
			assertEquals("Xxxx", model.getValueAt(i, 1));
			assertEquals(Tools.formatDate(testDate), model.getValueAt(i, 2));
			assertEquals("?", model.getValueAt(i, 3));
		}
	}

	@Test
	public void noErrorCells() throws DataSetException {
		ConfigureDataTableModel model = new ConfigureDataTableModel(dataSource, metaData, null);
		for (int rowIndex = 0; rowIndex < model.getRowCount(); rowIndex++) {
			for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++) {
				assertFalse(model.hasError(rowIndex, columnIndex));
			}
		}
	}

	@Test
	public void errorCells() throws DataSetException {
		columnMetaData.get(1).setType(ColumnType.REAL);
		ConfigureDataTableModel model = new ConfigureDataTableModel(dataSource, metaData, null);
		for (int rowIndex = 0; rowIndex < model.getRowCount(); rowIndex++) {
			for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++) {
				assertEquals(columnIndex == 1, model.hasError(rowIndex, columnIndex));
			}
		}
	}

	@Test
	public void errorCellsErrorColumnRemoved() throws DataSetException {
		columnMetaData.get(1).setType(ColumnType.REAL);
		ConfigureDataTableModel model = new ConfigureDataTableModel(dataSource, metaData, null);
		columnMetaData.get(1).setRemoved(true);
		for (int rowIndex = 0; rowIndex < model.getRowCount(); rowIndex++) {
			for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++) {
				assertFalse(model.hasError(rowIndex, columnIndex));
			}
		}
	}

	@Test
	public void noParsingAndColumnErrors() throws DataSetException {
		ConfigureDataTableModel model = new ConfigureDataTableModel(dataSource, metaData, null);
		assertTrue(model.getParsingErrors().isEmpty());
	}

	@Test
	public void parsingErrors() throws DataSetException {
		columnMetaData.get(1).setType(ColumnType.REAL);
		ConfigureDataTableModel model = new ConfigureDataTableModel(dataSource, metaData, null);
		List<ParsingError> errors = model.getParsingErrors();
		assertEquals(numberOfRows, errors.size());
		for (int i = 0; i < numberOfRows; i++) {
			assertEquals(null, model.getValueAt(i, 1));
			assertEquals(i, errors.get(i).getRow());
			assertEquals(1, errors.get(i).getColumn());
			assertEquals("Xxxx", errors.get(i).getOriginalValue());
		}
	}

	@Test
	public void rereadColumnDataValues() throws DataSetException {
		final AtomicBoolean setTotalWasCalled = new AtomicBoolean();
		final AtomicBoolean setCompletedWasCalled = new AtomicBoolean();
		ProgressListener listener = new ProgressListener() {

			@Override
			public void setTotal(int total) {
				setTotalWasCalled.set(true);
			}

			@Override
			public void setMessage(String message) {}

			@Override
			public void setCompleted(int completed) {
				setCompletedWasCalled.set(true);
			}

			@Override
			public void complete() {}
		};

		ConfigureDataTableModel model = new ConfigureDataTableModel(dataSource, metaData, null);
		columnMetaData.get(0).setType(ColumnType.DATE);
		model.rereadColumn(0, listener);
		for (int i = 0; i < numberOfRows; i++) {
			assertEquals(null, model.getValueAt(i, 0));
			assertEquals("Xxxx", model.getValueAt(i, 1));
			assertEquals(Tools.formatDate(testDate), model.getValueAt(i, 2));
			assertEquals("?", model.getValueAt(i, 3));
			assertTrue(model.hasError(i, 0));
		}

		assertTrue(setTotalWasCalled.get());
		assertTrue(setCompletedWasCalled.get());
	}

}
