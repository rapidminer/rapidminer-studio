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
package com.rapidminer.example.table;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import com.rapidminer.example.Attribute;
import com.rapidminer.tools.Ontology;


/**
 * Reads datarows from a data base.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class DatabaseDataRow extends DataRow {

	private static final long serialVersionUID = 4043965829002723585L;

	/** The result set which backs this data row. */
	private transient ResultSet resultSet;

	/** The current row of the result set. Only used for checks */
	private int row;

	/** The last attribute for which a query should be / was performed. */
	private Attribute lastAttribute = null;

	/**
	 * Creates a data row from the given result set. The current row of the result set if used as
	 * data source.
	 */
	public DatabaseDataRow(ResultSet resultSet) throws SQLException {
		this.resultSet = resultSet;
		this.row = resultSet.getRow();
	}

	/** Ensures that the current row is the current row of the result set. */
	private void ensureRowCorrect() throws SQLException {
		if (row != resultSet.getRow()) {
			throw new RuntimeException("DatabaseDataRow: ResultSet was modified since creation of row!");
		}
	}

	/** Returns the desired data for the given attribute. */
	@Override
	public double get(Attribute attribute) {
		try {
			ensureRowCorrect();
		} catch (SQLException e) {
			throw new RuntimeException("Cannot read data: " + e);
		}
		this.lastAttribute = attribute;
		double value = attribute.getValue(this);
		this.lastAttribute = null;
		return value;
	}

	/** Sets the given data for the given attribute. */
	@Override
	public void set(Attribute attribute, double value) {
		try {
			ensureRowCorrect();
		} catch (SQLException e) {
			throw new RuntimeException("Cannot update data: " + e, e);
		}
		this.lastAttribute = attribute;
		attribute.setValue(this, value);
		this.lastAttribute = null;
	}

	@Override
	protected double get(int index, double defaultValue) {
		if (lastAttribute == null) {
			throw new RuntimeException(
					"Cannot read data, please use get(Attribute) method instead of get(int, double) in DatabaseDataRow.");
		} else {
			try {
				return readColumn(this.resultSet, lastAttribute);
			} catch (SQLException e) {
				throw new RuntimeException("Cannot read data: " + e, e);
			}
		}
	}

	@Override
	protected synchronized void set(int index, double value, double defaultValue) {
		try {
			String name = this.lastAttribute.getName();
			if (Double.isNaN(value)) {
				resultSet.updateNull(name);
			} else {
				if (this.lastAttribute.isNominal()) {
					resultSet.updateString(name, this.lastAttribute.getMapping().mapIndex((int) value));
				} else {
					resultSet.updateDouble(name, value);
				}
			}
			resultSet.updateRow();
		} catch (SQLException e) {
			throw new RuntimeException("Cannot update data: " + e, e);
		}
	}

	/** Does nothing. */
	@Override
	protected void ensureNumberOfColumns(int numberOfColumns) {}

	@Override
	public String toString() {
		return "Database Data Row";
	}

	/** Reads the data for the given attribute from the result set. */
	public static double readColumn(ResultSet resultSet, Attribute attribute) throws SQLException {
		ResultSetMetaData metaData = resultSet.getMetaData();
		String name = attribute.getName();
		int valueType = attribute.getValueType();
		double value;
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE_TIME)) {
			try {
				Timestamp timestamp = resultSet.getTimestamp(name);
				if (resultSet.wasNull()) {
					value = Double.NaN;
				} else {
					value = timestamp.getTime();
				}
			} catch (ClassCastException e) {
				// DBase JDBC driver is a bit special and returns an SQL date here. So try that one as well
				Date date = resultSet.getDate(name);
				if (resultSet.wasNull()) {
					value = Double.NaN;
				} else {
					value = date.getTime();
				}
			}
		} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NUMERICAL)) {
			value = resultSet.getDouble(name);
			if (resultSet.wasNull()) {
				value = Double.NaN;
			}
		} else {
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NOMINAL)) {
				String valueString = null;
				int tableIndex = attribute.getTableIndex() + 1;
				if (metaData.getColumnType(tableIndex) == Types.CLOB) {
					Clob clob = resultSet.getClob(name);
					if (clob != null) {
						BufferedReader in = null;
						try {
							in = new BufferedReader(clob.getCharacterStream());
							String line = null;
							try {
								StringBuffer buffer = new StringBuffer();
								while ((line = in.readLine()) != null) {
									buffer.append(line + "\n");
								}
								valueString = buffer.toString();
							} catch (IOException e) {
								value = Double.NaN;
							}
						} finally {
							if (in != null) {
								try {
									in.close();
								} catch (IOException e) {
								}
							}
						}
					} else {
						valueString = null;
					}
				} else {
					valueString = resultSet.getString(name);
				}
				if (resultSet.wasNull() || valueString == null) {
					value = Double.NaN;
				} else {
					value = attribute.getMapping().mapString(valueString);
				}
			} else {
				value = Double.NaN;
			}
		}

		return value;
	}

	@Override
	public int getType() {
		return DataRowFactory.TYPE_SPECIAL;
	}
}
