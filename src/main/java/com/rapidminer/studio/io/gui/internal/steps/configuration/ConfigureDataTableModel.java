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

import java.text.DateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.table.AbstractTableModel;

import com.rapidminer.core.io.data.ColumnMetaData;
import com.rapidminer.core.io.data.ColumnMetaData.ColumnType;
import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetMetaData;
import com.rapidminer.core.io.data.DataSetRow;
import com.rapidminer.core.io.data.ParseException;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.example.Attribute;
import com.rapidminer.operator.nio.ImportWizardUtils;
import com.rapidminer.operator.nio.model.DefaultPreview;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;


/**
 * A model for the column configuration data table. It loads the model data from preview
 * {@link DataSet} provided by {@link DataSource#getPreview}. It does not load more data than
 * defined by {@link ImportWizardUtils#getPreviewLength()}. Stores {@link ParsingError}s encountered
 * during loading and the erroneous cells.
 *
 * @author Nils Woehler, Gisa Schaefer
 * @since 7.0.0
 */
final class ConfigureDataTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	/** error message for more than two values in binary column */
	private static final String ALREADY_TWO_BINARY_VALUES = I18N
			.getGUILabel("io.dataimport.step.data_column_configuration.error_table.parsing_error.not_binary_column") + " ";

	private final DataSetMetaData metaData;

	private int previewSize = ImportWizardUtils.getPreviewLength();
	private String[][] data;
	private final List<ParsingError> parsingErrorList = new LinkedList<>();
	private Map<Integer, Set<Integer>> errorCells = new HashMap<>();
	private final Map<Integer, Set<String>> binaryMapping = new HashMap<>();
	private DataSet dataSet;
	/**
	 * original meta data of the data set. Is not automatically changed. All changes have to be
	 * undone.
	 */
	private final DataSetMetaData originalDataSourceMetaData;

	/**
	 * Creates a new model instance.
	 *
	 * @param dataSource
	 *            the data source used to query the preview data
	 * @param metaData
	 *            the metaData copy to use
	 * @param l
	 *            the progress listener to report progress to
	 * @throws DataSetException
	 *             in case reading the preview fails
	 */
	public ConfigureDataTableModel(DataSource dataSource, DataSetMetaData metaData, ProgressListener l)
			throws DataSetException {
		this.metaData = metaData;
		this.originalDataSourceMetaData = dataSource.getMetadata();
		this.dataSet = dataSource.getPreview(ImportWizardUtils.getPreviewLength());
		read(dataSet, l);
	}

	/**
	 * Rereads the data with the current date format.
	 *
	 * @param listener
	 * @throws DataSetException
	 */
	void reread(ProgressListener listener) throws DataSetException {
		DateFormat originalDateFormat = originalDataSourceMetaData.getDateFormat();
		originalDataSourceMetaData.setDateFormat(metaData.getDateFormat());

		try {
			read(dataSet, listener);
		} finally {
			originalDataSourceMetaData.setDateFormat(originalDateFormat);
		}
	}

	/**
	 * Get the preview data set, e.g. for date format guessing
	 *
	 * @since 9.1
	 */
	DataSet getDataSet() {
		return dataSet;
	}

	private synchronized void read(DataSet dataPreview, ProgressListener listener) throws DataSetException {
		if (listener != null) {
			listener.setTotal(previewSize);
		}
		List<String[]> dataList = new LinkedList<>();
		parsingErrorList.clear();
		errorCells.clear();

		binaryMapping.clear();
		int columnIndex = 0;
		for (ColumnMetaData column : metaData.getColumnMetaData()) {
			if (column.getType() == ColumnType.BINARY) {
				binaryMapping.put(columnIndex, new HashSet<>(2));
			}
			columnIndex++;
		}

		// start from the beginning
		dataPreview.reset();
		int numberOfColumns = dataPreview.getNumberOfColumns();

		// read in data until preview size is reached
		// somehow the preview gets a maxEndRow of -1 and then this can take loooong
		while (dataPreview.hasNext() && dataList.size() < ImportWizardUtils.getPreviewLength()) {
			DataSetRow dataRow = dataPreview.nextRow();
			String[] row = new String[numberOfColumns];
			for (int i = 0; i < row.length; i++) {
				if (dataRow.isMissing(i)) {
					row[i] = Attribute.MISSING_NOMINAL_VALUE;
				} else if (metaData.getColumnMetaData().size() > i) {
					final ColumnType columnType = metaData.getColumnMetaData(i).getType();
					readNotMissingEntry(dataRow, row, i, dataPreview.getCurrentRowIndex(), columnType, errorCells);
				}
			}
			dataList.add(row);
			if (listener != null) {
				listener.setCompleted(dataList.size());
			}
		}

		// copy to array since will be accessed by index
		this.data = dataList.toArray(new String[dataList.size()][]);
		if (listener != null) {
			listener.complete();
		}
	}

	/**
	 * Reads the entry specified by columnIndex from the dataRow and stores it in the row array.
	 *
	 * @param dataRow
	 *            the data row containing the data
	 * @param row
	 *            the row array where to store the data
	 * @param columnIndex
	 *            the column to consider
	 * @param rowIndex
	 *            the current row, used for errors
	 * @param columnType
	 *            the type of the column
	 * @param errorCells
	 *            the map where to store the errorCells
	 */
	private void readNotMissingEntry(DataSetRow dataRow, String[] row, int columnIndex, int rowIndex,
			final ColumnType columnType, Map<Integer, Set<Integer>> errorCells) {
		try {
			switch (columnType) {
				case DATE:
					row[columnIndex] = Tools.formatDate(dataRow.getDate(columnIndex));
					break;
				case DATETIME:
					row[columnIndex] = Tools.formatDateTime(dataRow.getDate(columnIndex));
					break;
				case TIME:
					row[columnIndex] = Tools.formatTime(dataRow.getDate(columnIndex));
					break;
				case REAL:
					row[columnIndex] = Tools.formatNumber(dataRow.getDouble(columnIndex));
					break;
				case INTEGER:
					// can do round here, since value is not NaN
					row[columnIndex] = Tools.formatIntegerIfPossible(Math.round(dataRow.getDouble(columnIndex)));
					break;
				case CATEGORICAL:
					row[columnIndex] = DefaultPreview.shortenDisplayValue(dataRow.getString(columnIndex));
					break;
				case BINARY:
					String value = dataRow.getString(columnIndex);
					final Set<String> binaryEntries = binaryMapping.get(columnIndex);
					if (binaryEntries.size() == 2 && !binaryEntries.contains(value)) {
						throw new ParseException(ALREADY_TWO_BINARY_VALUES + binaryEntries.toString());
					} else {
						binaryEntries.add(value);
						row[columnIndex] = DefaultPreview.shortenDisplayValue(value);
					}
					break;
				default:
					break;
			}
		} catch (ParseException e) {
			row[columnIndex] = null;
			// store error with original value if possible
			String originalValue = null;

			// if the type is categorical, getString was already called above, so there will be a
			// parse exception again
			if (columnType != ColumnType.CATEGORICAL) {
				try {
					originalValue = dataRow.getString(columnIndex);
				} catch (ParseException e1) {
					originalValue = null;
				}
			}
			parsingErrorList.add(new ParsingError(columnIndex, rowIndex, originalValue, e.getMessage()));
			Set<Integer> errors = errorCells.get(columnIndex);
			if (errors != null) {
				errors.add(rowIndex);
			} else {
				Set<Integer> errorRows = new HashSet<>();
				errorRows.add(rowIndex);
				errorCells.put(columnIndex, errorRows);
			}
		}
	}

	/**
	 * Reads the column with index columnIndex again and stores the results.
	 *
	 * @param columnIndex
	 *            the column to reread
	 * @throws DataSetException
	 */
	synchronized void rereadColumn(int columnIndex, ProgressListener listener) throws DataSetException {
		if (listener != null) {
			listener.setTotal(100);
		}
		DateFormat originalDateFormat = originalDataSourceMetaData.getDateFormat();
		originalDataSourceMetaData.setDateFormat(metaData.getDateFormat());
		try {
			dataSet.reset();

			final ColumnType columnType = metaData.getColumnMetaData(columnIndex).getType();
			if (columnType == ColumnType.BINARY && !binaryMapping.containsKey(columnIndex)) {
				binaryMapping.put(columnIndex, new HashSet<>(2));
			}

			// copy errors cells such that errorCells change all at once
			Map<Integer, Set<Integer>> errorCellsCopy = new HashMap<>(errorCells);
			errorCellsCopy.remove(columnIndex);

			removeFromErrors(columnIndex);

			int rowIndex = 0;
			while (dataSet.hasNext() && rowIndex < data.length) {
				DataSetRow dataRow = dataSet.nextRow();
				String[] containerRow = data[rowIndex];

				if (dataRow.isMissing(columnIndex)) {
					containerRow[columnIndex] = Attribute.MISSING_NOMINAL_VALUE;
				} else {
					readNotMissingEntry(dataRow, containerRow, columnIndex, rowIndex, columnType, errorCellsCopy);
				}
				if (listener != null) {
					listener.setCompleted(100 * rowIndex / data.length);
				}
				rowIndex++;
			}
			errorCells = errorCellsCopy;
		} finally {
			originalDataSourceMetaData.setDateFormat(originalDateFormat);
		}

	}

	/**
	 * Removes all entries associated to the column with columnIndex from {@link #parsingErrorList}.
	 *
	 * @param columnIndex
	 *            the index for which to delete the entries
	 */
	private void removeFromErrors(int columnIndex) {
		parsingErrorList.removeIf(parsingError -> parsingError.getColumn() == columnIndex);
	}

	/**
	 * @return the stored list of {@link ParsingError}s that occurred during construction of this
	 *         table model
	 */
	List<ParsingError> getParsingErrors() {
		return parsingErrorList;
	}

	@Override
	public int getRowCount() {
		return data.length;
	}

	@Override
	public String getColumnName(int column) {
		return metaData.getColumnMetaData(column).getName();
	}

	@Override
	public int getColumnCount() {
		if (data != null && data.length > 0) {
			return data[0].length;
		} else {
			return 0;
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		final String[] row = data[rowIndex];
		if (row == null) {
			return null;
		} else if (columnIndex >= row.length) {
			return null;
		} else {
			return row[columnIndex];
		}
	}

	/**
	 * Returns whether a parsing error happened for the cell specified by rowIndex and columnIndex.
	 *
	 * @param rowIndex
	 *            the row index
	 * @param columnIndex
	 *            the column index
	 * @return {@code true} if a parsing error happened for this cell
	 */
	boolean hasError(int rowIndex, int columnIndex) {
		Set<Integer> errorRows = errorCells.get(columnIndex);
		return errorRows != null && errorRows.contains(rowIndex) && !metaData.getColumnMetaData(columnIndex).isRemoved();
	}

}
