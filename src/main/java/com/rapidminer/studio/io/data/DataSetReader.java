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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.rapidminer.Process;
import com.rapidminer.core.io.data.ColumnMetaData;
import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetRow;
import com.rapidminer.core.io.data.ParseException;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadStoppedException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.studio.io.data.internal.ResultSetAdapterUtils;
import com.rapidminer.studio.io.gui.internal.steps.StoreToRepositoryStep;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ProgressListener;


/**
 * Can read a {@link DataSet} into an {@link ExampleSet}. Uses a list of {@link ColumnMetaData} to
 * create {@link Attribute}s and fill the data for them by going through the rows of the
 * {@link DataSet}. Used by the {@link StoreToRepositoryStep} to store the data in the repository at
 * the end of the import wizard.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
public class DataSetReader {

	private int dataManagementType = DataRowFactory.TYPE_DOUBLE_ARRAY;

	private boolean shouldStop = false;
	private boolean isReading = false;
	private boolean isFaultTolerant = true;
	private final List<ColumnMetaData> metaData;
	private final Operator operator;

	/**
	 * Creates a new reader.
	 *
	 * @param operator
	 *            the operator that should be checked for stop, can be {@code null}
	 * @param metaData
	 *            the metaData to use for reading
	 * @param isFaultTolerant
	 *            {@code true} if the reader puts missing values on parsing errors, {@code false} if
	 *            the reader throws an exception on parsing errors
	 */
	public DataSetReader(Operator operator, List<ColumnMetaData> metaData, boolean isFaultTolerant) {
		this.operator = operator;
		this.isFaultTolerant = isFaultTolerant;
		this.metaData = metaData;
	}

	/**
	 * @return {@code true} if the reader puts missing values on parsing errors, {@code false} if
	 *         the reader throws an exception on parsing errors
	 */
	public boolean isFaultTolerant() {
		return isFaultTolerant;
	}

	/**
	 * Set if the reader should throw an exception or put a missing value on parsing errors
	 *
	 * @param isFaultTolerant
	 *            {@code true} if the reader puts missing values on parsing errors, {@code false} if
	 *            the reader throws an exception on parsing errors
	 */
	public void setFaultTolerant(boolean isFaultTolerant) {
		this.isFaultTolerant = isFaultTolerant;
	}

	/**
	 * Sets the data management type to use for the data rows in the example set created by
	 * {@link #read}. Default is {@link DataRowFactory.TYPE_DOUBLE_ARRAY}.
	 *
	 * @param dataManagmentType
	 *            the dataManagmentType to use for creating the example set, must be one of the
	 *            constants TYPE_DOUBLE_ARRAY, TYPE_FLOAT_ARRAY, TYPE_LONG_ARRAY, TYPE_INT_ARRAY,
	 *            TYPE_SHORT_ARRAY, TYPE_BYTE_ARRAY, TYPE_BOOLEAN_ARRAY, TYPE_DOUBLE_SPARSE_ARRAY,
	 *            TYPE_FLOAT_SPARSE_ARRAY, TYPE_LONG_SPARSE_ARRAY, TYPE_INT_SPARSE_ARRAY,
	 *            TYPE_SHORT_SPARSE_ARRAY, TYPE_BYTE_SPARSE_ARRAY, TYPE_BOOLEAN_SPARSE_ARRAY, or
	 *            TYPE_SPARSE_MAP of {@link DataRowFactory}.
	 */
	public void setDataManagmentType(int dataManagmentType) {
		this.dataManagementType = dataManagmentType;
	}

	/**
	 * Transforms the {@link DataSet} dataSet to an {@link ExampleSet} with respect to the metaData.
	 * Uses the metaData to create {@link Attribute}s and fill the data for them by going through
	 * the rows of the {@link DataSet}. The reading process can be stop at each new row by the
	 * operator this is associated to, by the listener, or by calling {@link #stop()}.
	 *
	 * @param dataSet
	 *            the data set to transform, will not be closed in this method
	 * @param listener
	 *            the progress listener, can be {@code null}
	 * @return the created example set
	 * @throws UserError
	 *             if a column specified in the metaData is not found or the column names are not
	 *             unique
	 * @throws DataSetException
	 *             when reading the data set fails
	 * @throws ProcessStoppedException
	 *             if the associated process is stopped or the {@link #stop()} method is called
	 * @throws ParseException
	 *             if parsing failed and the reading is not done fault tolerant
	 * @throws ProgressThreadStoppedException
	 *             if the {@link ProgressThread} associated to the listener was stopped
	 */
	public ExampleSet read(DataSet dataSet, ProgressListener listener)
			throws UserError, DataSetException, ProcessStoppedException, ParseException {
		isReading = true;

		if (listener != null) {
			listener.setTotal(120);
		}

		// check the meta data and create needed attributes
		int[] attributeColumns = parseMetaDataInformation();
		checkColumnsInDataSet(dataSet, attributeColumns);
		Attribute[] attributes = createAttributes(attributeColumns);

		if (listener != null) {
			listener.setCompleted(5);
		}

		// building example table
		ExampleSetBuilder builder = ExampleSets.from(attributes);
		fillExampleTable(dataSet, listener, attributeColumns, builder, attributes);

		// derive ExampleSet from exampleTable and assigning roles
		ExampleSet exampleSet = builder.build();
		assignRoles(attributeColumns, exampleSet);

		isReading = false;
		if (listener != null) {
			listener.setCompleted(110);
		}
		return exampleSet;
	}

	/**
	 * Stops the reading process. If the reading process is in progress, sets a flag that will stop
	 * the reading before the next row is read.
	 */
	public void stop() {
		if (isReading) {
			shouldStop = true;
		}
	}

	/**
	 * Assigns the roles stored in the metaData to the attributes of the exampleSet.
	 */
	private void assignRoles(int[] attributeColumns, ExampleSet exampleSet) {
		// Copy attribute list to avoid concurrent modification when setting to special
		List<Attribute> allAttributes = new LinkedList<>();
		for (Attribute att : exampleSet.getAttributes()) {
			allAttributes.add(att);
		}

		int attributeIndex = 0;
		for (Attribute attribute : allAttributes) {
			String roleId = metaData.get(attributeColumns[attributeIndex]).getRole();
			if (roleId != null && !Attributes.ATTRIBUTE_NAME.equals(roleId)) {
				exampleSet.getAttributes().setSpecialAttribute(attribute, roleId);
			}
			attributeIndex++;
		}
	}

	/**
	 * Fills the exampleTable with the data from the dataSet.
	 */
	private void fillExampleTable(DataSet dataSet, ProgressListener listener, int[] attributeColumns,
			ExampleSetBuilder builder, Attribute[] attributes)
			throws DataSetException, ProcessStoppedException, ParseException {

		dataSet.reset();
		int numberOfRows = dataSet.getNumberOfRows();
		DataRowFactory factory = new DataRowFactory(dataManagementType, DataRowFactory.POINT_AS_DECIMAL_CHARACTER);

		// detect if this is executed in a process
		boolean isRunningInProcess = isOperatorRunning();

		// now iterate over complete dataSet and copy data
		while (dataSet.hasNext()) {
			if (isRunningInProcess) {
				operator.checkForStop();
			}
			if (shouldStop) {
				throw new ProcessStoppedException();
			}

			DataSetRow currentRow = dataSet.nextRow();

			if (listener != null) {
				updateProcess(listener, dataSet.getCurrentRowIndex(), numberOfRows);
			}

			// creating data row
			DataRow row = factory.create(attributes.length);
			int attributeIndex = 0;
			for (Attribute attribute : attributes) {
				// check for missing
				if (currentRow.isMissing(attributeColumns[attributeIndex])) {
					row.set(attribute, Double.NaN);
				} else {
					switch (attribute.getValueType()) {
						case Ontology.INTEGER:
						case Ontology.NUMERICAL:
						case Ontology.REAL:
							row.set(attribute, getNumber(currentRow, attributeColumns[attributeIndex]));
							break;
						case Ontology.DATE_TIME:
						case Ontology.TIME:
						case Ontology.DATE:
							row.set(attribute, getDate(currentRow, attributeColumns[attributeIndex]));
							break;
						default:
							row.set(attribute, getStringIndex(attribute, currentRow, attributeColumns[attributeIndex]));
					}
				}
				attributeIndex++;
			}
			builder.addDataRow(row);
		}
	}

	/**
	 * @return if the operator is running in a process
	 */
	private boolean isOperatorRunning() {
		boolean isRunningInProcess = false;
		if (operator != null) {
			Process process = operator.getProcess();
			if (process != null && process.getProcessState() == Process.PROCESS_STATE_RUNNING) {
				isRunningInProcess = true;
			}
		}
		return isRunningInProcess;
	}

	/**
	 * Creates {@link Attribute}s from the metaData.
	 */
	private Attribute[] createAttributes(int[] attributeColumns) {
		int numberOfAttributes = attributeColumns.length;

		// create associated attributes
		Attribute[] attributes = new Attribute[numberOfAttributes];
		for (int i = 0; i < attributes.length; i++) {
			int attributeValueType = ResultSetAdapterUtils.transformColumnType(metaData.get(attributeColumns[i]).getType());
			attributes[i] = AttributeFactory.createAttribute(metaData.get(attributeColumns[i]).getName(),
					attributeValueType);
		}

		return attributes;
	}

	/**
	 * Checks if the unremoved columns described by the metaData are contained in the dataSet.
	 *
	 * @throws UserError
	 *             if a needed column is not found
	 */
	private void checkColumnsInDataSet(DataSet dataSet, int[] attributeColumns) throws UserError {
		// check whether all columns are accessible
		int numberOfAvailableColumns = dataSet.getNumberOfColumns();
		for (int attributeColumn : attributeColumns) {
			if (attributeColumn >= numberOfAvailableColumns) {
				throw new UserError(null, "data_import.specified_more_columns_than_exist",
						metaData.get(attributeColumn).getName(), attributeColumn);
			}
		}
	}

	/**
	 * Goes once through the metaData, checks which columns are removed and checks that the
	 * remaining columns have unique names.
	 *
	 * @return an array that contains at position i the original index of the column that will be
	 *         the i-th column in the example set
	 * @throws UserError
	 *             if the column names provided by the metaData are not unique
	 */
	private int[] parseMetaDataInformation() throws UserError {
		// create array that contains at position i the original index of the column that is now the
		// i-th column because of removals
		int[] selectedColumns = new int[metaData.size()];
		// create set of unique used column names
		Set<String> usedColumnNames = new HashSet<>();

		int columnIndex = 0;
		int usedColumnIndex = 0;
		for (ColumnMetaData column : metaData) {
			if (!column.isRemoved()) {

				selectedColumns[usedColumnIndex] = columnIndex;
				usedColumnIndex++;

				String columnName = column.getName();
				if (!usedColumnNames.contains(columnName)) {
					usedColumnNames.add(columnName);
				} else {
					throw new UserError(null, "data_import.non_unique_column_name", columnName);
				}
			}
			columnIndex++;
		}

		int[] attributeColumns = Arrays.copyOf(selectedColumns, usedColumnIndex);
		return attributeColumns;
	}

	/**
	 * Updates the process depending on whether the numberOfRows is known
	 */
	private void updateProcess(ProgressListener listener, int currentRow, int numberOfRows) {
		if (numberOfRows > 0) {
			listener.setCompleted(5 + 100 * currentRow / numberOfRows);
		} else {
			listener.setCompleted(50);
		}
	}

	/**
	 * Returns the nominal mapping index of the String entry at columnIndex of the row.
	 *
	 * @throws ParseException
	 *             if the parsing failed and we are not fault tolerant
	 */
	private double getStringIndex(Attribute attribute, DataSetRow row, int columnIndex) throws ParseException {
		try {
			String value = row.getString(columnIndex);
			return attribute.getMapping().mapString(value);
		} catch (ParseException e) {
			checkFaultTolerance(e);
			return Double.NaN;
		} catch (AttributeTypeException e) {
			// happens when binominal attribute with too many values
			checkFaultTolerance(new ParseException(e.getMessage(), e, columnIndex));
			return Double.NaN;
		}

	}

	/**
	 * Returns the date at index columnIndex of the row.
	 *
	 * @throws ParseException
	 *             if the parsing failed and we are not fault tolerant
	 */
	private double getDate(DataSetRow row, int columnIndex) throws ParseException {
		try {
			return row.getDate(columnIndex).getTime();
		} catch (ParseException e) {
			checkFaultTolerance(e);
			return Double.NaN;
		}
	}

	/**
	 * Returns the number at index columnIndex of the row.
	 *
	 * @throws ParseException
	 *             if the parsing failed and we are not fault tolerant
	 */
	private double getNumber(DataSetRow row, int columnIndex) throws ParseException {
		try {
			return row.getDouble(columnIndex);
		} catch (ParseException e) {
			checkFaultTolerance(e);
			return Double.NaN;
		}
	}

	/**
	 * Checks if an exception should be thrown.
	 */
	private void checkFaultTolerance(ParseException e) throws ParseException {
		if (!isFaultTolerant) {
			throw e;
		}
	}

}
