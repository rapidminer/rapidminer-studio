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

import java.util.Date;

import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetRow;
import com.rapidminer.core.io.data.ParseException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.studio.io.data.internal.ResultSetAdapter;


/**
 * An adapter that converts an Excel {@link DataResultSet} into a {@link DataSet}.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
class ExcelResultSetAdapter extends ResultSetAdapter {

	private final DataSetRow dataRow = new DataSetRow() {

		@Override
		public Date getDate(int columnIndex) throws ParseException {
			try {
				return getResultSet().getDate(columnIndex);
			} catch (com.rapidminer.operator.nio.model.ParseException e) {
				throw new ParseException(e.getMessage(), e, columnIndex);
			}
		}

		@Override
		public String getString(int columnIndex) throws ParseException {
			try {
				return getResultSet().getString(columnIndex);
			} catch (com.rapidminer.operator.nio.model.ParseException e) {
				throw new ParseException(e.getMessage(), e, columnIndex);
			}
		}

		@Override
		public double getDouble(int columnIndex) throws ParseException {
			try {
				Number number = getResultSet().getNumber(columnIndex);
				if (number == null) {
					return Double.NaN;
				}
				return number.doubleValue();
			} catch (com.rapidminer.operator.nio.model.ParseException e) {
				throw new ParseException(e.getMessage(), e, columnIndex);
			}
		}

		@Override
		public boolean isMissing(int columnIndex) {
			return getResultSet().isMissing(columnIndex);
		}

	};

	/**
	 * Creates a new {@link ExcelResultSetAdapter} instance.
	 *
	 * @param excelResultSet
	 *            the wrapped {@link DataResultSet}
	 * @param startRow
	 *            the actual data start row index (without the header row)
	 * @param maxEndRow
	 *            the end row or {@link ResultSetAdapter#NO_END_ROW} if the whole Excel file should
	 *            be read
	 * @throws DataSetException
	 *             in case the creation of the {@link ExcelResultSetAdapter} failed because of file
	 *             reading errors or alike
	 */
	public ExcelResultSetAdapter(DataResultSet excelResultSet, int startRow, int maxEndRow) throws DataSetException {
		super(excelResultSet, startRow, maxEndRow);
	}

	@Override
	protected DataSetRow getDataRow() {
		return dataRow;
	}

	@Override
	public DataSetRow nextRow() throws DataSetException {
		// check whether we need to skip to the data start row first
		boolean parseNewRow = true;
		if (getResultSet().getCurrentRow() < getDataStartRow() - 1) {
			try {
				skipToDataStartRow();
			} catch (OperatorException e) {
				throw new DataSetException(e.getMessage(), e.getCause());
			}
			// if the row before the data start row has been reached
			// we still need to parse the next row (true)
			// otherwise the pointer is pointing at the first row already and
			// we do not need to parse a new row (false)
			parseNewRow = getResultSet().getCurrentRow() == getDataStartRow() - 1;
		}
		if (parseNewRow) {
			return super.nextRow();
		} else {
			return getDataRow();
		}
	}

	@Override
	public void reset() throws DataSetException {
		try {
			// just reset result set, skip to start row will be done when calling the next row
			// method
			getResultSet().reset(null);
		} catch (OperatorException e) {
			throw new DataSetException(e.getMessage(), e.getCause());
		}
	}

}
