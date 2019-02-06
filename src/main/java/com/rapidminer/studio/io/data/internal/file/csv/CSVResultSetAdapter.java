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

import java.text.NumberFormat;
import java.util.Date;

import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetRow;
import com.rapidminer.core.io.data.ParseException;
import com.rapidminer.operator.nio.model.CSVResultSet;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.studio.io.data.internal.ResultSetAdapter;


/**
 * Class that transforms a {@link CSVResultSet} into a {@link DataSet}.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
class CSVResultSetAdapter extends ResultSetAdapter {

	private final DataSetRow dataRow = new DataSetRow() {

		@Override
		public Date getDate(int columnIndex) throws ParseException {
			String value = getString(columnIndex);
			String trimmedValue = value == null ? "" : value.trim();

			// check for missing value
			if (trimmedValue.isEmpty()) {
				return null;
			}

			if (dataSource.getResultSetConfiguration().trimValuesForParsing()) {
				value = trimmedValue;
			}

			// parse to Date
			try {
				return dataSource.getMetadata().getDateFormat().parse(value);
			} catch (java.text.ParseException e) {
				throw new ParseException(e.getMessage(), e, columnIndex);
			}
		}

		@Override
		public String getString(int columnIndex) throws ParseException {
			if (columnIndex >= getNumberOfColumns()) {
				throw new IndexOutOfBoundsException();
			}
			try {
				String value = getResultSet().getString(columnIndex);
				if (value == null || value.trim().isEmpty()) {
					return null;
				}
				return value;
			} catch (com.rapidminer.operator.nio.model.ParseException e) {
				throw new ParseException(e.getMessage(), e, columnIndex);
			}
		}

		@Override
		public double getDouble(int columnIndex) throws ParseException {
			String value = getString(columnIndex);
			String trimmedValue = value == null ? "" : value.trim();

			// check for missing value
			if (trimmedValue.isEmpty()) {
				return Double.NaN;
			}

			if (dataSource.getResultSetConfiguration().trimValuesForParsing()) {
				value = trimmedValue;
			}

			// parse to double
			NumberFormat numberFormat = dataSource.getNumberFormat();
			if (numberFormat != null) {
				try {
					Number parsedValue = numberFormat.parse(value);
					if (parsedValue == null) {
						return Double.NaN;
					} else {
						return parsedValue.doubleValue();
					}
				} catch (java.text.ParseException e) {
					throw new ParseException(e.getMessage(), e, columnIndex);
				}
			} else {
				try {
					return Double.parseDouble(value);
				} catch (NumberFormatException e) {
					throw new ParseException(e.getMessage(), e, columnIndex);
				}
			}
		}

		@Override
		public boolean isMissing(int columnIndex) {
			return getResultSet().isMissing(columnIndex);
		}

	};

	private final CSVDataSource dataSource;

	/**
	 * Constructs a {@link DataSet} from the given resultSet using the given data.
	 *
	 * @throws DataSetException
	 *             in case the creation of the {@link CSVResultSetAdapter} failed (e.g. because of
	 *             file reading errors)
	 */
	public CSVResultSetAdapter(CSVDataSource dataSource, DataResultSet resultSet, int startRow, int endRow)
			throws DataSetException {
		super(resultSet, startRow, endRow);
		this.dataSource = dataSource;
	}

	@Override
	protected DataSetRow getDataRow() {
		return dataRow;
	}

	/**
	 * Sets the maximal end row
	 *
	 * @param maxEndRow
	 *            the maximal end row
	 */
	void setMaximumEndRow(int maxEndRow) {
		super.setMaxEndRow(maxEndRow);
	}

}
