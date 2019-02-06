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
package com.rapidminer.studio.io.data.internal;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rapidminer.core.io.data.ColumnMetaData.ColumnType;
import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetMetaData;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.ColumnMetaData;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.DataResultSetTranslationConfiguration;
import com.rapidminer.operator.nio.model.DataResultSetTranslator;
import com.rapidminer.operator.nio.model.ParseException;
import com.rapidminer.studio.io.data.DefaultDataSetMetaData;
import com.rapidminer.studio.io.data.HeaderRowBehindStartRowException;
import com.rapidminer.studio.io.data.HeaderRowNotFoundException;
import com.rapidminer.studio.io.data.StartRowNotFoundException;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ProgressListener;


/**
 * Utility class that contains helper methods for {@link DataSource}s that return a
 * {@link ResultSetAdapter} as {@link DataSet}.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
public final class ResultSetAdapterUtils {

	/**
	 * Utility class constructor.
	 */
	private ResultSetAdapterUtils() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Creates a new {@link DataSetMetaData} instance for the provided {@link DataResultSet} based
	 * on the provided data range and header row index (if any). This includes reading the column
	 * names and guessing the column types for the selected columns. For guessing the column types
	 * the logic from {@link DataResultSetTranslator} is used.
	 *
	 * @param resultSet
	 * 		the {@link DataResultSet} that should be used to extract the meta data
	 * @param numberFormat
	 * 		the number format that should be used during column type guessing
	 * @param startingRowIndex
	 * 		the 0-based index of the first data row (not including the header row)
	 * @param headerRowIndex
	 * 		the 0-based index for the header row (if any,
	 * 		{@link ResultSetAdapter#NO_HEADER_ROW} otherwise)
	 * @return the new {@link DataSetMetaData} instance which contains meta data retrieved from the
	 * column name extraction and column type guessing
	 * @throws HeaderRowNotFoundException
	 * 		if the header row was not found
	 * @throws StartRowNotFoundException
	 * 		if the data start row was not found
	 * @throws HeaderRowBehindStartRowException
	 * 		in case the headerRowIndex > startingRowIndex
	 * @throws DataSetException
	 * 		if the meta data fetching fails
	 */
	public static DataSetMetaData createMetaData(DataResultSet resultSet, NumberFormat numberFormat, int startingRowIndex, int headerRowIndex)
			throws HeaderRowNotFoundException, StartRowNotFoundException, HeaderRowBehindStartRowException, DataSetException {
		return createMetaData(resultSet, numberFormat, startingRowIndex, headerRowIndex, false);
	}

	/**
	 * Creates a new {@link DataSetMetaData} instance for the provided {@link DataResultSet} based
	 * on the provided data range and header row index (if any). This includes reading the column
	 * names and guessing the column types for the selected columns. For guessing the column types
	 * the logic from {@link DataResultSetTranslator} is used.
	 *
	 * @param resultSet
	 * 		the {@link DataResultSet} that should be used to extract the meta data
	 * @param numberFormat
	 * 		the number format that should be used during column type guessing
	 * @param startingRowIndex
	 * 		the 0-based index of the first data row (not including the header row)
	 * @param headerRowIndex
	 * 		the 0-based index for the header row (if any,
	 * 		{@link ResultSetAdapter#NO_HEADER_ROW} otherwise)
	 * @param trimAttributeNames
	 * 		whether to trim attribute names before creating meta data or not
	 * @return the new {@link DataSetMetaData} instance which contains meta data retrieved from the
	 *         column name extraction and column type guessing
	 * @throws HeaderRowNotFoundException
	 * 		if the header row was not found
	 * @throws StartRowNotFoundException
	 * 		if the data start row was not found
	 * @throws HeaderRowBehindStartRowException
	 * 		in case the headerRowIndex > startingRowIndex
	 * @throws DataSetException
	 * 		if the meta data fetching fails
	 * @since 8.1.1
	 */
	public static DataSetMetaData createMetaData(DataResultSet resultSet, NumberFormat numberFormat, int startingRowIndex, int headerRowIndex, boolean trimAttributeNames)
			throws HeaderRowNotFoundException, StartRowNotFoundException, HeaderRowBehindStartRowException, DataSetException {
		return createMetaData(resultSet, numberFormat, startingRowIndex, headerRowIndex, trimAttributeNames, false);
	}

	/**
	 * Creates a new {@link DataSetMetaData} instance for the provided {@link DataResultSet} based
	 * on the provided data range and header row index (if any). This includes reading the column
	 * names and guessing the column types for the selected columns. For guessing the column types
	 * the logic from {@link DataResultSetTranslator} is used.
	 *
	 * @param resultSet
	 * 		the {@link DataResultSet} that should be used to extract the meta data
	 * @param numberFormat
	 * 		the number format that should be used during column type guessing
	 * @param startingRowIndex
	 * 		the 0-based index of the first data row (not including the header row)
	 * @param headerRowIndex
	 * 		the 0-based index for the header row (if any,
	 * 		{@link ResultSetAdapter#NO_HEADER_ROW} otherwise)
	 * @param trimAttributeNames
	 * 		whether to trim attribute names before creating meta data or not
	 * @param trimForGuessing
	 *      whether values should be trimmed for type guessing
	 * @return the new {@link DataSetMetaData} instance which contains meta data retrieved from the
	 *         column name extraction and column type guessing
	 * @throws HeaderRowNotFoundException
	 * 		if the header row was not found
	 * @throws StartRowNotFoundException
	 * 		if the data start row was not found
	 * @throws HeaderRowBehindStartRowException
	 * 		in case the headerRowIndex > startingRowIndex
	 * @throws DataSetException
	 * 		if the meta data fetching fails
	 * @since 9.2.0
	 */
	public static DataSetMetaData createMetaData(DataResultSet resultSet, NumberFormat numberFormat, int startingRowIndex, int headerRowIndex, boolean trimAttributeNames, boolean trimForGuessing)
			throws HeaderRowNotFoundException, StartRowNotFoundException, HeaderRowBehindStartRowException, DataSetException {

		// check whether the header row index is lower or equal to the starting row
		if (headerRowIndex > startingRowIndex) {
			throw new HeaderRowBehindStartRowException();
		}

		try {
			int numberOfColumns = resultSet.getNumberOfColumns();

			String[] columnNames = getColumnNames(resultSet, headerRowIndex, startingRowIndex, numberOfColumns, trimAttributeNames);
			List<ColumnType> columnTypes = guessColumnTypes(resultSet, startingRowIndex, headerRowIndex, numberOfColumns,
					numberFormat, trimForGuessing);
			return new DefaultDataSetMetaData(Arrays.asList(columnNames), columnTypes);
		} catch (OperatorException e) {
			throw new DataSetException(e.getMessage(), e);
		}
	}

	/**
	 * Checks if the start row is contained in the resultSet.
	 *
	 * @param startingRowIndex
	 *            the 0-based starting row index
	 * @throws StartRowNotFoundException
	 *             in case the starting row was not found
	 * @throws OperatorException
	 *             in case the underlying file could not be read
	 */
	private static void checkStartRow(DataResultSet resultSet, int startingRowIndex)
			throws StartRowNotFoundException, OperatorException {
		while (resultSet.getCurrentRow() < startingRowIndex) {
			if (!resultSet.hasNext()) {
				throw new StartRowNotFoundException();
			}
			resultSet.next(null);
		}
	}

	/**
	 * Reads the column names from the resultSet given the configuration.
	 *
	 * @param resultSet
	 * 		the data set
	 * @param headerRowIndex
	 * 		the index of the row that should be used to extract column names from or
	 * 		{@link ResultSetAdapter#NO_HEADER_ROW} in case the default names should be used
	 * @param startingRowIndex
	 * 		the index of the actual data start row
	 * @param numberOfColumns
	 * 		the number of columns for the {@link DataSource}
	 * @param trimAttributeNames
	 * 		whether to trim attribute names before creating meta data or not
	 * @return the column names as a String array
	 * @throws HeaderRowNotFoundException
	 * 		if the header row was not found
	 * @throws StartRowNotFoundException
	 * 		if the data start row was not found
	 * @throws OperatorException
	 * 		if reading the resultSet failed
	 */
	private static String[] getColumnNames(DataResultSet resultSet, int headerRowIndex, int startingRowIndex,
		    int numberOfColumns, boolean trimAttributeNames) throws HeaderRowNotFoundException, OperatorException, StartRowNotFoundException {
		resultSet.reset(null);
		String[] defaultNames = resultSet.getColumnNames();

		// check which last column index to use and create array of names
		String[] columnNames = new String[numberOfColumns];

		// read column names from specified row
		while (resultSet.getCurrentRow() < headerRowIndex) {
			if (!resultSet.hasNext()) {
				throw new HeaderRowNotFoundException();
			}
			resultSet.next(null);
		}

		for (int i = 0; i < numberOfColumns; i++) {
			if (headerRowIndex > ResultSetAdapter.NO_HEADER_ROW) {
				if (resultSet.isMissing(i)) {
					columnNames[i] = defaultNames[i];
				} else {
					try {
						// retrieve data with native value type and convert to String
						switch (resultSet.getNativeValueType(i)) {
							case DATE:
								columnNames[i] = String.valueOf(resultSet.getDate(i));
								break;
							case EMPTY:
								columnNames[i] = defaultNames[i];
								break;
							case NUMBER:
								columnNames[i] = String.valueOf(resultSet.getNumber(i));
								break;
							case STRING:
							default:
								columnNames[i] = resultSet.getString(i);
								if (trimAttributeNames && columnNames[i] != null) {
									columnNames[i] = columnNames[i].trim();
								}
								break;

						}
					} catch (ParseException e) {
						columnNames[i] = defaultNames[i];
					}
				}
			} else {
				columnNames[i] = defaultNames[i];
			}
		}

		checkStartRow(resultSet, startingRowIndex);
		return columnNames;

	}

	/**
	 * Guesses column types by using the
	 * {@link DataResultSetTranslator#guessValueTypes(DataResultSetTranslationConfiguration, DataResultSet, ProgressListener)}
	 * logic and transforming the guessed value types into {@link ColumnType}s.
	 */
	private static List<ColumnType> guessColumnTypes(DataResultSet dataResultSet, int startingRow, int headerRow,
			int numberOfColumns, NumberFormat numberFormat, boolean trimForGuessing) throws DataSetException {

		try {
			int[] valueTypes = getValueTypes(dataResultSet, startingRow, headerRow, numberOfColumns, numberFormat, trimForGuessing);
			List<ColumnType> columnTypes = new ArrayList<>(valueTypes.length);
			for (int type : valueTypes) {
				columnTypes.add(transformValueType(type));
			}

			return columnTypes;
		} catch (OperatorException e) {
			throw new DataSetException(e.getMessage(), e);
		}
	}

	/**
	 * Transforms a {@link Ontology#ATTRIBUTE_VALUE_TYPE} into a {@link ColumnType}.
	 */
	public static ColumnType transformValueType(int valueType) {
		switch (valueType) {
			case Ontology.TIME:
				return ColumnType.TIME;
			case Ontology.DATE:
				return ColumnType.DATE;
			case Ontology.DATE_TIME:
				return ColumnType.DATETIME;
			case Ontology.NUMERICAL:
			case Ontology.REAL:
				return ColumnType.REAL;
			case Ontology.INTEGER:
				return ColumnType.INTEGER;
			case Ontology.BINOMINAL:
				return ColumnType.BINARY;
			default:
				return ColumnType.CATEGORICAL;
		}
	}

	/**
	 * Transforms a {@link ColumnType} into a {@link Ontology#ATTRIBUTE_VALUE_TYPE} .
	 */
	public static int transformColumnType(ColumnType columnType) {
		switch (columnType) {
			case DATETIME:
				return Ontology.DATE_TIME;
			case DATE:
				return Ontology.DATE;
			case TIME:
				return Ontology.TIME;
			case INTEGER:
				return Ontology.INTEGER;
			case REAL:
				return Ontology.REAL;
			case BINARY:
				return Ontology.BINOMINAL;
			default:
			case CATEGORICAL:
				return Ontology.POLYNOMINAL;

		}
	}

	/**
	 * Uses the {@link DataResultSetTranslator} to guess the valueTypes.
	 *
	 * @param dataResultSet
	 *            the data set
	 * @param startingRow
	 *            the starting row
	 * @param headerRow
	 *            the header row
	 * @param numberOfColumns
	 *            the number of columns the {@link DataSource} is going to have
	 * @param numberFormat
	 *            the number format used to guess the value types (or {@code null} in case of the
	 *            default format)
	 * @return the guessed value types
	 * @throws OperatorException
	 *             if the guessing failed because of an IOException
	 */
	private static int[] getValueTypes(DataResultSet dataResultSet, int startingRow, int headerRow, int numberOfColumns,
			NumberFormat numberFormat, boolean trimForGuessing) throws OperatorException {

		// generate a DataResultSetTranslationConfiguration
		DataResultSetTranslationConfiguration translationConfiguration = new DataResultSetTranslationConfiguration(
				dataResultSet, getAnnotations(startingRow, headerRow));
		translationConfiguration.setNumberFormat(numberFormat);
		translationConfiguration.setTrimForGuessing(trimForGuessing);

		// use a translator to guess value types given the configuration
		new DataResultSetTranslator(null).guessValueTypes(translationConfiguration, dataResultSet, null);
		ColumnMetaData[] metadata = translationConfiguration.getColumnMetaData();

		// check which index to use as last column and create valueType array
		int[] valueTypes = new int[numberOfColumns];

		for (int i = 0; i < numberOfColumns; i++) {
			valueTypes[i] = metadata[i].getAttributeValueType();
		}

		return valueTypes;
	}

	/**
	 * Creates a list of annotations used by the {@link DataResultSet} to define comment and name
	 * rows.
	 *
	 * @param startingRow
	 *            the starting row index
	 * @param headerRow
	 *            the header row index
	 * @return the annotations associated to the configuration
	 */
	private static List<String> getAnnotations(int startingRow, int headerRow) {
		int lastCommentRow = startingRow - 1;
		int max = Math.max(headerRow, lastCommentRow);
		List<String> annotations = new ArrayList<>(max + 1);
		for (int i = 0; i <= max; i++) {
			if (i == headerRow) {
				annotations.add(Annotations.ANNOTATION_NAME);
			} else if (i < startingRow) {
				annotations.add(Annotations.KEY_COMMENT);
			} else {
				annotations.add(null);
			}
		}
		return annotations;
	}

}
