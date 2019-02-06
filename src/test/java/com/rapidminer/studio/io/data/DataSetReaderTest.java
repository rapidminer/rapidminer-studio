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
package com.rapidminer.studio.io.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.core.io.data.ColumnMetaData;
import com.rapidminer.core.io.data.ColumnMetaData.ColumnType;
import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetRow;
import com.rapidminer.core.io.data.ParseException;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.studio.io.data.DataSetReader;
import com.rapidminer.studio.io.data.DefaultColumnMetaData;


/**
 * Tests the {@link DataSetReader}.
 *
 * @author Gisa Schaefer
 *
 */
public class DataSetReaderTest {

	private static int numberOfRows = 10;
	private static DataSetRow row;
	private static DataSet dataSet;
	private static Date testDate = new Date();

	private DataSetReader reader;
	private List<ColumnMetaData> columnMetaData;

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
	}

	@Before
	public void setUp() {
		columnMetaData = new ArrayList<ColumnMetaData>(
				Arrays.asList(new ColumnMetaData[] { new DefaultColumnMetaData("att1", ColumnType.REAL),
						new DefaultColumnMetaData("att2", ColumnType.CATEGORICAL),
						new DefaultColumnMetaData("att3", ColumnType.DATE),
						new DefaultColumnMetaData("att4", ColumnType.INTEGER) }));
		reader = new DataSetReader(null, columnMetaData, false);
	}

	@Test
	public void attributeCreation() throws UserError, ProcessStoppedException, DataSetException, ParseException {
		ExampleSet result = reader.read(dataSet, null);
		Attributes attributes = result.getAttributes();
		assertEquals(4, attributes.size());
		assertTrue(attributes.get("att1").isNumerical());
		assertTrue(attributes.get("att2").isNominal());
		assertTrue(attributes.get("att3").isDateTime());
		assertTrue(attributes.get("att4").isNumerical());
	}

	@Test
	public void numberOfRows() throws UserError, ProcessStoppedException, DataSetException, ParseException {
		ExampleSet result = reader.read(dataSet, null);
		assertEquals(numberOfRows, result.size());
	}

	@Test
	public void removingColumn() throws UserError, ProcessStoppedException, DataSetException, ParseException {
		columnMetaData.get(0).setRemoved(true);
		ExampleSet result = reader.read(dataSet, null);
		Attributes attributes = result.getAttributes();
		assertEquals(null, attributes.get("att1"));
		assertEquals(3, attributes.size());
	}

	@Test
	public void checkValue0() throws UserError, ProcessStoppedException, DataSetException, ParseException {
		columnMetaData.get(1).setRemoved(true);
		columnMetaData.get(2).setRemoved(true);
		columnMetaData.get(3).setRemoved(true);
		ExampleSet result = reader.read(dataSet, null);
		Attribute attribute = result.getAttributes().get("att1");
		for (int i = 0; i < numberOfRows; i++) {
			assertEquals(5.27, result.getExample(i).getValue(attribute), 1e-15);
		}
	}

	@Test
	public void checkValue1() throws UserError, ProcessStoppedException, DataSetException, ParseException {
		columnMetaData.get(0).setRemoved(true);
		columnMetaData.get(2).setRemoved(true);
		columnMetaData.get(3).setRemoved(true);
		ExampleSet result = reader.read(dataSet, null);
		Attribute attribute = result.getAttributes().get("att2");
		for (int i = 0; i < numberOfRows; i++) {
			assertEquals("Xxxx", result.getExample(0).getNominalValue(attribute));
		}

	}

	@Test
	public void checkValue2() throws UserError, ProcessStoppedException, DataSetException, ParseException {
		columnMetaData.get(0).setRemoved(true);
		columnMetaData.get(1).setRemoved(true);
		columnMetaData.get(3).setRemoved(true);
		ExampleSet result = reader.read(dataSet, null);
		Attribute attribute = result.getAttributes().get("att3");
		for (int i = 0; i < numberOfRows; i++) {
			assertEquals(testDate, result.getExample(i).getDateValue(attribute));
		}
	}

	@Test
	public void checkValue3() throws UserError, ProcessStoppedException, DataSetException, ParseException {
		columnMetaData.get(0).setRemoved(true);
		columnMetaData.get(1).setRemoved(true);
		columnMetaData.get(2).setRemoved(true);
		ExampleSet result = reader.read(dataSet, null);
		Attribute attribute = result.getAttributes().get("att4");
		for (int i = 0; i < numberOfRows; i++) {
			assertEquals(Double.NaN, result.getExample(i).getValue(attribute), 1e-15);
		}
	}

	@Test
	public void specialRole() throws UserError, ProcessStoppedException, DataSetException, ParseException {
		columnMetaData.get(0).setRole("label");
		ExampleSet result = reader.read(dataSet, null);
		assertEquals("label", result.getAttributes().getRole("att1").getSpecialName());
	}

	@Test(expected = ParseException.class)
	public void parseExceptionNominalInNumericColumn()
			throws UserError, ProcessStoppedException, DataSetException, ParseException {
		columnMetaData.get(1).setType(ColumnType.REAL);
		reader.read(dataSet, null);
	}

	@Test
	public void faultTolerantNominalInNumericColumn()
			throws UserError, ProcessStoppedException, DataSetException, ParseException {
		reader.setFaultTolerant(true);
		columnMetaData.get(1).setType(ColumnType.REAL);
		ExampleSet result = reader.read(dataSet, null);
		Attribute attribute = result.getAttributes().get("att2");
		result.getExample(0).get(attribute);
		assertTrue(attribute.isNumerical());
		assertEquals(Double.NaN, result.getExample(0).getNumericalValue(attribute), 1e-15);
	}

	@Test
	public void faultTolerantNominalInDateColumn()
			throws UserError, ProcessStoppedException, DataSetException, ParseException {
		reader.setFaultTolerant(true);
		columnMetaData.get(1).setType(ColumnType.DATE);
		ExampleSet result = reader.read(dataSet, null);
		Attribute attribute = result.getAttributes().get("att2");
		result.getExample(0).get(attribute);
		assertTrue(attribute.isDateTime());
		assertEquals(Double.NaN, result.getExample(0).getValue(attribute), 1e-15);
	}

	@Test(expected = UserError.class)
	public void moreColumnsInMetaDataThanInDataSet()
			throws UserError, ProcessStoppedException, DataSetException, ParseException {
		columnMetaData.add(new DefaultColumnMetaData("att5", ColumnType.BINARY));
		reader.read(dataSet, null);
	}

	@Test(expected = UserError.class)
	public void moreColumnsInMetaDataThanInDataSetRemoveBefore()
			throws UserError, ProcessStoppedException, DataSetException, ParseException {
		columnMetaData.add(new DefaultColumnMetaData("att5", ColumnType.BINARY));
		columnMetaData.get(3).setRemoved(true);
		reader.read(dataSet, null);
	}

	@Test
	public void moreColumnsInMetaDataThanInDataSetButRemoved()
			throws UserError, ProcessStoppedException, DataSetException, ParseException {
		columnMetaData.add(new DefaultColumnMetaData("att5", ColumnType.BINARY));
		columnMetaData.get(4).setRemoved(true);
		reader.read(dataSet, null);
	}

	@Test(expected = UserError.class)
	public void twoColumnsWithSameName() throws UserError, ProcessStoppedException, DataSetException, ParseException {
		columnMetaData.get(3).setName("att1");
		reader.read(dataSet, null);
	}

	@Test
	public void twoColumnsWithSameNameButRemoved()
			throws UserError, ProcessStoppedException, DataSetException, ParseException {
		columnMetaData.get(3).setName("att1");
		columnMetaData.get(3).setRemoved(true);
		reader.read(dataSet, null);
	}

}
