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

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Map;

import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetMetaData;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.core.io.data.source.DataSourceConfiguration;
import com.rapidminer.core.io.data.source.FileDataSource;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.CSVResultSetConfiguration;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.studio.io.data.DefaultDataSetMetaData;
import com.rapidminer.studio.io.data.internal.ResultSetAdapter;
import com.rapidminer.studio.io.data.internal.ResultSetAdapterUtils;
import com.rapidminer.tools.StrictDecimalFormat;


/**
 * A {@link DataSource} implementation for CSV files.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
public class CSVDataSource extends FileDataSource {

	private DataSetMetaData metaData = new DefaultDataSetMetaData(Collections.emptyList(),
			Collections.emptyList());
	private CSVResultSetConfiguration configuration = new CSVResultSetConfiguration();
	private CSVResultSetAdapter dataSet;
	private DataSourceConfiguration dataSetConstructionConfiguration = null;

	@Override
	public void setLocation(Path newLocation) {
		super.setLocation(newLocation);
		configuration.setCsvFile(newLocation.toString());
	}

	/**
	 * @return the {@link CSVResultSetConfiguration} for this data source. Changes to the
	 *         configuration will affect the import process as it stores the internal import
	 *         configuration
	 */
	public CSVResultSetConfiguration getResultSetConfiguration() {
		return configuration;
	}

	@Override
	public CSVResultSetAdapter getData() throws DataSetException {
		CSVResultSetAdapter wholeData = getCachedDataSet();
		wholeData.setMaximumEndRow(ResultSetAdapter.NO_END_ROW);
		return wholeData;
	}

	/**
	 * Checks if the cached data set can be reused and stores a new one if not.
	 *
	 * @return the cached data set
	 * @throws DataSetException
	 */
	private CSVResultSetAdapter getCachedDataSet() throws DataSetException {
		boolean configurationChanged = dataSetConstructionConfiguration != null
				&& !dataSetConstructionConfiguration.getParameters().equals(getConfiguration().getParameters());

		if (dataSet == null || configurationChanged) {
			// close old data set in case a new one is created
			if (dataSet != null) {
				dataSet.close();
				dataSet = null;
			}

			try {
				dataSet = new CSVResultSetAdapter(this, getResultSetConfiguration().makeDataResultSet(null),
						getDataStartRow(), ResultSetAdapter.NO_END_ROW);
				dataSetConstructionConfiguration = getConfiguration();
			} catch (OperatorException e) {
				throw new DataSetException(e.getMessage(), e.getCause());
			}
		}
		return dataSet;
	}

	@Override
	public CSVResultSetAdapter getPreview(int maxPreviewRows) throws DataSetException {
		// choose endRow such that there are maxPreviewRow rows in total
		int endRow = getDataStartRow() + maxPreviewRows - 1;

		CSVResultSetAdapter previewData = getCachedDataSet();
		previewData.setMaximumEndRow(endRow);
		return previewData;
	}

	/**
	 * @return the row where the data starts, not counting the header row
	 */
	private int getDataStartRow() {
		int startRow = configuration.getStartingRow();
		if (configuration.hasHeaderRow() && configuration.getHeaderRow() == startRow) {
			startRow++;
		}
		return startRow;
	}

	@Override
	public DataSetMetaData getMetadata() {
		return metaData;
	}

	/**
	 * @return the number format associated to the configuration
	 */
	NumberFormat getNumberFormat() {
		return new StrictDecimalFormat(getResultSetConfiguration().getDecimalCharacter());
	}

	/**
	 * Creates a new {@link DataSetMetaData} instance with the results of the
	 * {@link CSVFormatSpecificationWizardStep} and assigns it to the {@link #metaData} field of
	 * this {@link CSVDataSource}.
	 * <p>
	 * The method also checks if the header row and the starting row exist and throws an exception
	 * otherwise.
	 *
	 * @throws DataSetException
	 *             in case the starting row or header row do not exist or the specified CSV file
	 *             could not be read because of IO issues
	 */
	public void createMetaData() throws DataSetException {

		// create a new CSV ResultSet configuration which reads the whole selected file
		// we cannot call getData() here as it might already skip the first lines
		try (CSVResultSetConfiguration configuration = new CSVResultSetConfiguration()) {

			configuration.setCsvFile(getLocation().toFile().toString());
			configuration.setSkipComments(getResultSetConfiguration().isSkipComments());
			configuration.setCommentCharacters(getResultSetConfiguration().getCommentCharacters());
			configuration.setDecimalCharacter(getResultSetConfiguration().getDecimalCharacter());
			configuration.setEncoding(getResultSetConfiguration().getEncoding());
			configuration.setEscapeCharacter(getResultSetConfiguration().getEscapeCharacter());
			configuration.setQuoteCharacter(getResultSetConfiguration().getQuoteCharacter());
			configuration.setUseQuotes(getResultSetConfiguration().isUseQuotes());
			configuration.setColumnSeparators(getResultSetConfiguration().getColumnSeparators());
			configuration.setHasHeaderRow(getResultSetConfiguration().hasHeaderRow());
			configuration.setHeaderRow(getResultSetConfiguration().getHeaderRow());
			configuration.setTrimValuesForParsing(getResultSetConfiguration().trimValuesForParsing());
			configuration.setStartingRow(getResultSetConfiguration().getStartingRow());
			configuration.setTrimLines(getResultSetConfiguration().isTrimLines());
			configuration.setSkipUTF8BOM(getResultSetConfiguration().isSkippingUTF8BOM());

			int headerRowIndex = configuration.hasHeaderRow() ? configuration.getHeaderRow()
					: ResultSetAdapter.NO_HEADER_ROW;
			try (DataResultSet dataSet = configuration.makeDataResultSet(null)) {
				this.metaData = ResultSetAdapterUtils.createMetaData(dataSet, getNumberFormat(), getDataStartRow(),
						headerRowIndex, false, configuration.trimValuesForParsing());
			} catch (OperatorException e) {
				throw new DataSetException(e.getMessage(), e);
			}
		}
	}

	@Override
	public DataSourceConfiguration getConfiguration() {
		final Map<String, String> storedConfiguration = configuration.getParameterMap();
		return new DataSourceConfiguration() {

			@Override
			public String getVersion() {
				return "0";
			}

			@Override
			public Map<String, String> getParameters() {
				return storedConfiguration;
			}
		};
	}

	@Override
	public void configure(DataSourceConfiguration configuration) throws DataSetException {}

	@Override
	public void close() throws DataSetException {
		configuration.close();
		if (dataSet != null) {
			dataSet.close();
			dataSet = null;
		}
	}
}
