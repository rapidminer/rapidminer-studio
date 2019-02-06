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

import java.nio.file.Path;

import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetMetaData;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.core.io.data.source.DataSourceConfiguration;
import com.rapidminer.core.io.data.source.DataSourceFeature;
import com.rapidminer.core.io.data.source.FileDataSource;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.ExcelDateTimeTypeGuesser;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.DateFormatProvider;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.operator.nio.model.xlsx.XlsxResultSet.XlsxReadMode;
import com.rapidminer.operator.nio.model.xlsx.XlsxSheetMetaDataParser;
import com.rapidminer.operator.nio.model.xlsx.XlsxUtilities.XlsxCellCoordinates;
import com.rapidminer.studio.io.data.internal.ResultSetAdapter;
import com.rapidminer.studio.io.data.internal.ResultSetAdapterUtils;


/**
 * A {@link DataSource} implementation for Excel files.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
public final class ExcelDataSource extends FileDataSource {

	private DataSetMetaData metaData = null;

	private ExcelResultSetConfiguration configuration = new ExcelResultSetConfiguration();
	private int headerRowIndex = 0;

	private transient DataSet previewDataSet = null;
	private transient DataSourceConfiguration previewConfiguration = null;

	private transient DataSet dataSet = null;
	private transient DataSourceConfiguration dataSetConfiguration = null;

	@Override
	public void setLocation(Path newLocation) {
		super.setLocation(newLocation);
		if (newLocation != null) {
			configuration.setWorkbookFile(newLocation.toFile());

			/*
			 * Setting the workbook file will reset the row and column offset to 0. We want the row
			 * offset to -1 by default as this means we want to import the whole column from the
			 * beginning.
			 */
			getResultSetConfiguration().setRowOffset(XlsxCellCoordinates.NO_ROW_NUMBER);
			getResultSetConfiguration().setRowLast(XlsxSheetMetaDataParser.MAXIMUM_XLSX_ROW_INDEX);
		}

	}

	@Override
	public DataSet getData() throws DataSetException {
		/*
		 * Create a new data set instance in case no data set is available yet or configuration has
		 * changed
		 */
		boolean configurationChanged = dataSetConfiguration != null
				&& !dataSetConfiguration.getParameters().equals(getConfiguration().getParameters());
		if (dataSet == null || configurationChanged) {

			// close old preview set in case a new one is created
			if (dataSet != null) {
				dataSet.close();
				dataSet = null;
			}

			try {
				this.dataSet = createDataSet(XlsxReadMode.OPERATOR);
				this.dataSetConfiguration = getConfiguration();
			} catch (OperatorException e) {
				throw new DataSetException(e.getMessage(), e.getCause());
			}
		}
		return dataSet;
	}

	@Override
	public DataSet getPreview(int maxPreviewSize) throws DataSetException {
		/*
		 * Create a new preview data set instance in case no preview is available yet or
		 * configuration has changed
		 */
		boolean configChange = previewConfiguration != null
				&& !previewConfiguration.getParameters().equals(getConfiguration().getParameters());
		if (previewDataSet == null || configChange) {

			// close old preview set in case a new one is created
			if (previewDataSet != null) {
				previewDataSet.close();
				previewDataSet = null;
			}

			try {
				this.previewDataSet = createDataSet(XlsxReadMode.WIZARD_PREVIEW, maxPreviewSize);
				this.previewConfiguration = getConfiguration();
			} catch (OperatorException e) {
				throw new DataSetException(e.getMessage(), e.getCause());
			}
		}
		return previewDataSet;
	}

	private ExcelResultSetAdapter createDataSet(XlsxReadMode readMode) throws OperatorException, DataSetException {
		return createDataSet(readMode, -1);
	}

	private ExcelResultSetAdapter createDataSet(XlsxReadMode readMode, int maxPreviewSize)
			throws OperatorException, DataSetException {
		int startRow = getStartRowIndex();
		int endRow = getEndRowIndex();
		if (readMode == XlsxReadMode.WIZARD_PREVIEW) {
			// set end row such that length is maximal preview length
			final int endRowByLength = startRow + maxPreviewSize - 1;
			if (endRow > ResultSetAdapter.NO_END_ROW) {
				endRow = Math.min(endRow, endRowByLength);
			} else {
				endRow = endRowByLength;
			}
		}
		DateFormatProvider provider = () -> getMetadata().getDateFormat();
		return new ExcelResultSetAdapter(getResultSetConfiguration().makeDataResultSet(null, readMode, provider), startRow,
				endRow);
	}

	/**
	 * @return the {@link ExcelResultSetConfiguration} for this {@link ExcelDataSource}. It is
	 *         holding information about the Excel file path and the configured sheet and cell range
	 *         to be imported.
	 */
	ExcelResultSetConfiguration getResultSetConfiguration() {
		return configuration;
	}

	/**
	 * @return the actual data content start index (without the header row if defined)
	 */
	private int getStartRowIndex() {
		int rowOffset = getResultSetConfiguration().getRowOffset();

		// adjust start row to first row if rowOffset is set to NO_ROW_NUMBER
		int startRow = rowOffset == XlsxCellCoordinates.NO_ROW_NUMBER ? 0 : rowOffset;

		if (getHeaderRowIndex() > ResultSetAdapter.NO_HEADER_ROW && getHeaderRowIndex() == startRow) {
			startRow++;
		}
		return startRow;
	}

	/**
	 * @return the index of the last row to be imported or {@link ResultSetAdapter#NO_END_ROW} in
	 *         case the whole Excel file should be imported
	 */
	private int getEndRowIndex() {
		if (getResultSetConfiguration().getRowLast() == Integer.MAX_VALUE) {
			return ResultSetAdapter.NO_END_ROW;
		} else {
			return getResultSetConfiguration().getRowLast();
		}
	}

	/**
	 * Returns the index of the header row.
	 *
	 * @return the index of the header row or {@link ResultSetAdapter#NO_HEADER_ROW} if no header
	 *         row is specified.
	 */
	public int getHeaderRowIndex() {
		return headerRowIndex;
	}

	/**
	 * Updates the header row index.
	 *
	 * @param headerRowIndex
	 *            the new header row index
	 */
	public void setHeaderRowIndex(int headerRowIndex) {
		this.headerRowIndex = headerRowIndex;
	}

	@Override
	public DataSetMetaData getMetadata() {
		return metaData;
	}

	/**
	 * Creates a new meta data instance with the results of the
	 * {@link ExcelSheetSelectionWizardStep} and assigns it to the {@link #metaData} field of the
	 * {@link ExcelDataSource}.
	 * <p>
	 * The method checks if the header row and the starting row exist and throws an exception
	 * otherwise.
	 *
	 * @throws DataSetException
	 *             in case the guessing failed (e.g. because of file reading errors, wrong file
	 *             path, etc.)
	 */
	public void createMetaData() throws DataSetException {
		// create a new Excel ResultSet configuration which reads the whole selected sheet
		// we cannot call getData() here as it might already skip the first lines
		try (ExcelResultSetConfiguration configuration = new ExcelResultSetConfiguration()) {

			configuration.setWorkbookFile(getLocation().toFile());
			configuration.setSheet(getResultSetConfiguration().getSheet());
			configuration.setSheetByName(getResultSetConfiguration().getSheetByName());
			configuration.setSheetSelectionMode(getResultSetConfiguration().getSheetSelectionMode());
			configuration.setColumnOffset(getResultSetConfiguration().getColumnOffset());
			configuration.setColumnLast(getResultSetConfiguration().getColumnLast());
			configuration.setEncoding(getResultSetConfiguration().getEncoding());

			try (DataResultSet resultSet = configuration.makeDataResultSet(null)) {
				this.metaData = ResultSetAdapterUtils.createMetaData(resultSet, null, getStartRowIndex(),
						getHeaderRowIndex());
				this.metaData.configure(ExcelDateTimeTypeGuesser.guessDateTimeColumnType(getData(), metaData));
			} catch (OperatorException e) {
				throw new DataSetException(e.getMessage(), e);
			}
		}
	}

	@Override
	public DataSourceConfiguration getConfiguration() {
		return new ExcelDataSourceConfiguration(this);
	}

	@Override
	public void configure(DataSourceConfiguration configuration) throws DataSetException {}

	@Override
	public void close() throws DataSetException {
		configuration.close();
		if (previewDataSet != null) {
			previewDataSet.close();
			previewDataSet = null;
		}
		if (dataSet != null) {
			dataSet.close();
			dataSet = null;
		}
	}

	@Override
	public boolean supportsFeature(DataSourceFeature feature){
		return feature == DataSourceFeature.DATETIME_METADATA;
	}

}
