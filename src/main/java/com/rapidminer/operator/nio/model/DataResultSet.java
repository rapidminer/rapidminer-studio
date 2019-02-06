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
package com.rapidminer.operator.nio.model;

import java.util.Date;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ProgressListener;


/**
 * This interface represents a ResultSet like view on a data source. It is just the interface for
 * iterating over the raw data. The
 *
 * @author Tobias Malbrecht, Sebastian Loh, Sebastian Land
 */
public interface DataResultSet extends AutoCloseable {

	public enum ValueType {
		STRING(Ontology.POLYNOMINAL), DATE(Ontology.DATE_TIME),
		// INTEGER(Ontology.INTEGER),
		NUMBER(Ontology.NUMERICAL), EMPTY(Ontology.ATTRIBUTE_VALUE);

		private final int rapidMinerAttributeType;

		private ValueType(int rapidMinerAttributeType) {
			this.rapidMinerAttributeType = rapidMinerAttributeType;
		}

		public int getRapidMinerAttributeType() {
			return rapidMinerAttributeType;
		}
	}

	/**
	 * This returns if another row exists.
	 */
	boolean hasNext();

	/**
	 * Proceed to the next row if existent. Will throw NoSuchElementException if no further row
	 * exists
	 *
	 * @param listener
	 *
	 * @return
	 */
	void next(ProgressListener listener) throws OperatorException;

	/**
	 * Returns the number of columns found so far. If there are any rows containing more columns
	 * that this value, an error has to be registered.
	 */
	int getNumberOfColumns();

	/**
	 * This returns the names of the columns according to the underlying technical system. For
	 * example the database column name might be returned. This method is only used, if there's no
	 * user specific setting in the {@link DataResultSetTranslationConfiguration} or annotations
	 * present in the {@link DataResultSetTranslationConfiguration}.
	 */
	String[] getColumnNames();

	/**
	 * Returns whether the value in the specified column in the current row is missing.
	 *
	 * @param columnIndex
	 *            index of the column
	 * @return
	 */
	boolean isMissing(int columnIndex);

	/**
	 * Returns a numerical value contained in the specified column in the current row. Should return
	 * null if the value is not a numerical or if the value is missing.
	 *
	 * @param columnIndex
	 * @return
	 * @throws ParseException
	 */
	Number getNumber(int columnIndex) throws ParseException;

	/**
	 * Returns a nominal value contained in the specified column in the current row. Should return
	 * null if the value is not a nominal or a kind of string type or if the value is missing.
	 *
	 * @param columnIndex
	 * @return
	 */
	String getString(int columnIndex) throws ParseException;

	/**
	 * Returns a date, time or date_time value contained in the specified column in the current row.
	 * Should return null if the value is not a date or time value or if the value is missing.
	 *
	 * @param columnIndex
	 * @return
	 */
	Date getDate(int columnIndex) throws ParseException;

	/**
	 *
	 * @return The type which most closely matches the value type of the underlying data source. The
	 *         corresponding getter method for this type must not throw an RuntimeException when
	 *         invoked for this column.
	 */
	ValueType getNativeValueType(int columnIndex) throws ParseException;

	/**
	 * Closes the data source. May tear down a database connection or close a file which is read
	 * from. The underlying {@link DataResultSetFactory} may choose to keep the connection open for
	 * performance reasons and must be closed independently.
	 *
	 * @throws OperatorException
	 */
	@Override
	void close() throws OperatorException;

	/**
	 * This will reset the current iteration and start at the first row
	 *
	 * @param listener
	 * @throws OperatorException
	 */
	void reset(ProgressListener listener) throws OperatorException;

	/**
	 * This will return an integer array containing the value types of all columns. This might be
	 * determined by the underlying technical system like data bases. If not, just an Array
	 * Containing 0 might be returned.
	 */
	int[] getValueTypes();

	/**
	 * @return the current {@code 0} based row index of the current parsed row. In case no row has
	 *         been parsed yet, {@code -1} will be returned.
	 */
	int getCurrentRow();

}
