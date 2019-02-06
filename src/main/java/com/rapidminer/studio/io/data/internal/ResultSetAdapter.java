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

import java.util.NoSuchElementException;

import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetRow;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.studio.io.data.StartRowNotFoundException;


/**
 * Abstract class to transform a {@link DataResultSet} into a {@link DataSet}. Subclasses must
 * implement {@link #getDataRow()}.
 *
 * @author Nils Woehler, Gisa Schaefer
 * @since 7.0.0
 */
public abstract class ResultSetAdapter implements DataSet {

	/**
	 * A flag that indicates that the whole {@link DataResultSet} should be read.
	 */
	public static final int NO_END_ROW = -1;

	/**
	 * A flag that indicates that no header row is defined.
	 */
	public static final int NO_HEADER_ROW = -1;

	/**
	 * A flag that indicates that no end column is defined.
	 */
	public static final int NO_COLUMN_END_INDEX = -1;

	private final DataResultSet resultSet;
	private final int startRow;
	private int maxEndRow;

	/**
	 * Creates a new {@link ResultSetAdapter} which wraps a {@link DataResultSet} into a
	 * {@link DataSet}.
	 *
	 * @param resultSet
	 *            the {@link DataResultSet} which should be wrapped into a {@link DataSet}
	 * @param startRow
	 *            the row index of the actual data content (it must <strong>not</strong> include the
	 *            header row index)
	 * @param maxEndRow
	 *            the maximum row index to read or {@link #NO_END_ROW} in case the whole
	 *            {@link DataResultSet} should be read
	 * @throws DataSetException
	 *             in case the underlying {@link DataResultSet} could not be reset (e.g. because of
	 *             file reading issues)
	 */
	public ResultSetAdapter(DataResultSet resultSet, int startRow, int maxEndRow) throws DataSetException {
		this.resultSet = resultSet;
		this.startRow = startRow;
		this.maxEndRow = maxEndRow;
		reset();
	}

	/**
	 * @return the {@link DataResultSet} which is wrapped by this class
	 */
	public DataResultSet getResultSet() {
		return resultSet;
	}

	@Override
	public DataSetRow nextRow() throws DataSetException {
		if (!hasNext()) {
			throw new NoSuchElementException("No more data available");
		}
		try {
			resultSet.next(null);
			return getDataRow();
		} catch (OperatorException e) {
			throw new DataSetException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public boolean hasNext() {
		if (maxEndRow > NO_END_ROW && (resultSet.getCurrentRow() >= maxEndRow || startRow > maxEndRow)) {
			return false;
		}
		return resultSet.hasNext();
	}

	@Override
	public int getCurrentRowIndex() {
		if (resultSet.getCurrentRow() < startRow) {
			return -1;
		}
		return resultSet.getCurrentRow() - startRow;
	}

	@Override
	public void reset() throws DataSetException {
		try {
			resultSet.reset(null);
			skipToDataStartRow();
		} catch (OperatorException e) {
			throw new DataSetException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public int getNumberOfColumns() {
		return resultSet.getNumberOfColumns();
	}

	@Override
	public int getNumberOfRows() {
		// even if the max end row is defined we cannot calculate the number of rows here as the
		// DataResultSet might have less rows than maxEndRow available
		return NO_END_ROW;
	}

	@Override
	public void close() throws DataSetException {
		try {
			resultSet.close();
		} catch (OperatorException e) {
			throw new DataSetException(e.getMessage(), e.getCause());
		}
	}

	/**
	 * @return the index of the data start row
	 */
	protected final int getDataStartRow() {
		return startRow;
	}

	/**
	 * Skips the first non-data rows until the index returned by
	 * {@link #resultSet#getCurrentRowIndex()} is equal to the index right before the
	 * {@link #startRow}.
	 *
	 * @throws StartRowNotFoundException
	 *             in case the start row index is behind the actual data content size
	 * @throws OperatorException
	 *             in case of other {@link DataResultSet} errors (e.g. file reading errors)
	 */
	protected final void skipToDataStartRow() throws StartRowNotFoundException, OperatorException {
		while (resultSet.getCurrentRow() < startRow - 1) {
			if (!hasNext()) {
				throw new StartRowNotFoundException();
			}
			resultSet.next(null);
		}
	}

	/**
	 * @return the current row of the {@link DataResultSet} wrapped in a {@link DataSetRow}
	 */
	protected abstract DataSetRow getDataRow();

	/**
	 * Sets the maximum end row
	 *
	 * @param maxEndRow
	 */
	protected void setMaxEndRow(int maxEndRow) {
		this.maxEndRow = maxEndRow;
	}

}
