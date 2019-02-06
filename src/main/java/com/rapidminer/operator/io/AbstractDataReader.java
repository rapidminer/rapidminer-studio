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
package com.rapidminer.operator.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.ExcelExampleSource;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.container.Range;


/**
 * Abstract super class of all example sources reading from files.
 *
 * @author Tobias Malbrecht
 * @author Sebastian Loh (29.04.2010)
 */
public abstract class AbstractDataReader extends AbstractExampleSource {

	public static final int PREVIEW_LINES = 300;

	/**
	 * DO NOT SET THIS PARAMETER DIRECTLY. USE THE
	 * {@link AbstractDataReader#setErrorTolerant(boolean)} in order to cache the value.
	 *
	 * Indicates whether the reader tolerates values, which do not match a attributes value type.
	 * <p>
	 * For example if the value type is NUMERICAL and the reader reads a string. If
	 * {@link AbstractDataReader#PARAMETER_ERROR_TOLERANT} is <code>true</code>, the reader writes a
	 * missing value, if it is <code>false</code> the reader throws an exception. The reader
	 * replaces also a binomial value type by nominal the attributes domain has more then two values
	 * and this parameter is checked.
	 */
	public static final String PARAMETER_ERROR_TOLERANT = "read_not_matching_values_as_missings";

	/**
	 * Hidden parameter in order to remember whether attributes names, which are defined by the user
	 * are used or not. This parameter is set <code>true</code> when the user uses the import
	 * wizards in order to configure the reader.
	 */
	private static final String PARAMETER_ATTRIBUTE_NAMES_DEFINED_BY_USER = "attribute_names_already_defined";

	/**
	 * This parameter holds the hole information about the attribute columns. I.e. which attributes
	 * are defined, the names, what value type they have, whether the att. is selected,
	 */
	private static final String PARAMETER_META_DATA = "data_set_meta_data_information";

	/**
	 * hidden parameter which is used to construct the
	 * {@link AbstractDataReader#PARAMETER_META_DATA}
	 */
	public static final String PARAMETER_COLUMN_INDEX = "column_index";

	/**
	 * hidden parameter which is used to construct the
	 * {@link AbstractDataReader#PARAMETER_META_DATA}
	 */
	public static final String PARAMETER_COLUMN_META_DATA = "attribute_meta_data_information";

	/**
	 * hidden parameter which is used to construct the
	 * {@link AbstractDataReader#PARAMETER_META_DATA}
	 */
	public static final String PARAMETER_COLUMN_NAME = "attribute name";

	/**
	 * hidden parameter which is used to construct the
	 * {@link AbstractDataReader#PARAMETER_META_DATA}
	 */
	public static final String PARAMETER_COLUMN_SELECTED = "column_selected";

	/**
	 * hidden parameter which is used to construct the
	 * {@link AbstractDataReader#PARAMETER_META_DATA}
	 */
	public static final String PARAMETER_COLUM_VALUE_TYPE = "attribute_value_type";

	/**
	 * hidden parameter which is used to construct the
	 * {@link AbstractDataReader#PARAMETER_META_DATA}
	 */
	public static final String PARAMETER_COLUM_ROLE = "attribute_role";

	public static final List<String> ROLE_NAMES = new ArrayList<>();

	{
		ROLE_NAMES.clear();
		for (int i = 0; i < Attributes.KNOWN_ATTRIBUTE_TYPES.length; i++) {
			if (Attributes.KNOWN_ATTRIBUTE_TYPES[i].equals("attribute")) {
				ROLE_NAMES.add(AttributeColumn.REGULAR);
			} else {
				ROLE_NAMES.add(Attributes.KNOWN_ATTRIBUTE_TYPES[i]);
			}
		}
	}

	/**
	 * a list of errors which might occurred during the importing prozess.
	 */
	private List<OperatorException> importErrors = new LinkedList<>();

	protected abstract DataSet getDataSet() throws OperatorException, IOException;

	/**
	 * the row count is the number of row/lines which are read during the guessing process. It is
	 * only used for the operator's MetaData prediction.
	 *
	 * @see AbstractDataReader#guessValueTypes()
	 */
	private int rowCountFromGuessing = 0;

	/**
	 * Indicated whether the operator MetaData is only guessed ( <code>metaDataFixed == false</code>
	 * ) or somebody called {@link AbstractDataReader#fixMetaDataDefinition()}.
	 *
	 */
	private boolean metaDataFixed = false;

	/**
	 * Indicates whether the ValueTypes were already guessed once.
	 */
	private boolean guessedValueTypes = false;

	private boolean detectErrorsInPreview = false;

	/**
	 * cached flag in order to avoid reading the parameter every single row
	 */
	boolean isErrorTollerantCache = true;

	/**
	 * Flag which interrupts the reading prcocess if it is set <code>true</code> . @see
	 * {@link AbstractDataReader#stopReading()}
	 */
	private boolean stopReading = false;

	/**
	 * Flag which interrupts the reading prcocess if it is set <code>true</code> . @see
	 * {@link AbstractDataReader#stopReading()}
	 */
	protected boolean skipGuessingValueTypes = false;

	/**
	 * Data structure to manage the background highlighting for cells, which can not be parsed as
	 * the specified value type.
	 *
	 * Maps the column to a set of row which in which the parsing failed.
	 *
	 * @see AbstractDataReader#hasParseError(int, int)
	 * @see AbstractDataReader#hasParseErrorInColumn(int)
	 */
	TreeMap<Integer, TreeSet<Integer>> errorCells = new TreeMap<>();

	/**
	 * The columns of the created {@link ExampleSet}.
	 *
	 * @see AbstractDataReader#createExampleSet()
	 * @see AbstractDataReader#guessValueTypes()
	 */
	private List<AttributeColumn> attributeColumns = new ArrayList<>();

	public void clearAllReaderSettings() {
		clearReaderSettings();
		deleteAttributeMetaDataParamters();
	}

	public void clearReaderSettings() {
		stopReading();
		attributeColumns.clear();
		importErrors.clear();
	}

	public void deleteAttributeMetaDataParamters() {
		setParameter(PARAMETER_META_DATA, null);
		setAttributeNamesDefinedByUser(false);
	}

	public AbstractDataReader(OperatorDescription description) {
		super(description);
	}

	public boolean attributeNamesDefinedByUser() {
		return getParameterAsBoolean(PARAMETER_ATTRIBUTE_NAMES_DEFINED_BY_USER);
	}

	public void setAttributeNamesDefinedByUser(boolean flag) {
		setParameter(PARAMETER_ATTRIBUTE_NAMES_DEFINED_BY_USER, Boolean.toString(flag));
	}

	/**
	 * Returns all <b>activated</b> attribute columns.
	 *
	 * @return
	 */
	public List<AttributeColumn> getActiveAttributeColumns() {
		List<AttributeColumn> list = new LinkedList<AttributeColumn>();
		for (AttributeColumn column : attributeColumns) {
			if (column.isActivated()) {
				list.add(column);
			}
		}
		return list;
	}

	/**
	 * Returns all attribute columns, despite they are activated or not.
	 *
	 * @return
	 */
	public List<AttributeColumn> getAllAttributeColumns() {
		return Collections.unmodifiableList(attributeColumns);
	}

	/**
	 * Returns the attribute column with the given index if it exists (it does not matter if the
	 * column is activated or not). Else a {@link IllegalArgumentException} is thrown
	 *
	 * @param index
	 *            the index of the requested column.
	 * @return
	 */
	public AttributeColumn getAttributeColumn(int index) throws IllegalArgumentException {
		if (index < attributeColumns.size()) {
			return attributeColumns.get(index);
		}
		throw new IllegalArgumentException("The attribute column with index " + index + " does not exists.");
	}

	/**
	 * Returns the index of the given {@link AttributeColumn} (it does not matter if it is activated
	 * or not). If the attribute column does not exist, -1 is returned.
	 *
	 * @param column
	 * @return the index of the attribute column, -1 else.
	 */
	public int getIndexOfAttributeColumn(AttributeColumn column) {
		return attributeColumns.indexOf(column);
	}

	/**
	 * Returns the index of the given <b>activated</b> {@link AttributeColumn}. Returns -1 if the
	 * column is not activated or does not exist.
	 *
	 * @param column
	 * @return
	 */
	public int getIndexOfActiveAttributeColumn(AttributeColumn column) {
		return getActiveAttributeColumns().indexOf(column);
	}

	public void addAttributeColumn() {
		String name = getNewGenericColumnName(attributeColumns.size());
		attributeColumns.add(new AttributeColumn(name));
	}

	public void addAttributeColumn(String attributeName) {
		attributeColumns.add(new AttributeColumn(attributeName));
	}

	/**
	 * Returns <code>true</code> when somebody called
	 * {@link AbstractDataReader#fixMetaDataDefinition()}. Otherwise the operator MetaData is only
	 * guessed (<code>metaDataFixed == false</code>) or
	 *
	 * @return
	 */
	public boolean isMetaDatafixed() {
		return metaDataFixed;
	}

	/**
	 * Method to declare the operators MetaData as final.
	 */
	public void fixMetaDataDefinition() {
		metaDataFixed = true;
	}

	public void writeMetaDataInParameter() {
		deleteAttributeMetaDataParamters();
		setAttributeNamesDefinedByUser(true);
		for (AttributeColumn col : getAllAttributeColumns()) {
			col.setMetaParameter();
		}
	}

	public void loadMetaDataFromParameters() {
		List<AttributeColumn> oldColumns = attributeColumns;
		attributeColumns.clear();
		try {
			List<String[]> metaData = getParameterList(PARAMETER_META_DATA);
			// first create as many attribute columns as parameters in the list
			// exists
			for (int i = 0; i < metaData.size(); i++) {
				this.addAttributeColumn();
			}
			Iterator<AttributeColumn> it = oldColumns.iterator();
			// then let them load their properties from the meta data
			for (AttributeColumn column : getAllAttributeColumns()) {
				column.loadMetaParameter();
				// restore annotations
				Annotations ann = it.next().getAnnotations();
				for (String key : ann.getKeys()) {
					column.getAnnotations().setAnnotation(key, ann.get(key));
				}
			}
		} catch (UndefinedParameterError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Returns the number of all columns, regardless a column is activated or not.
	 *
	 * @return
	 */
	public int getColumnCount() {
		return attributeColumns.size();
	}

	/**
	 * Returns the number of row which are read during the value type guessing.
	 *
	 * @see AbstractDataReader#guessValueTypes()
	 *
	 * @return
	 */
	private int getRowCountFromGuessing() {
		return rowCountFromGuessing;
	}

	@Override
	public ExampleSetMetaData getGeneratedMetaData() {
		if (attributeNamesDefinedByUser()) {
			loadMetaDataFromParameters();
			guessedValueTypes = true;
		}

		if (!guessedValueTypes) {
			return getDefaultMetaData();
		}

		ExampleSetMetaData metaData = new ExampleSetMetaData();

		for (AttributeColumn column : getActiveAttributeColumns()) {

			AttributeMetaData amd = new AttributeMetaData(column.getName(), column.getValueType());
			amd.setAnnotations(column.getAnnotations());

			String role = column.getRole();
			if (role.equals(AttributeColumn.REGULAR)) {
				role = null;
			}
			amd.setRole(role);

			MDInteger missings = new MDInteger(column.numberOfMissings);
			SetRelation relation = SetRelation.EQUAL;

			if (!isMetaDatafixed()) {
				relation = SetRelation.SUPERSET;
				missings.increaseByUnknownAmount();
			}
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(column.getValueType(), Ontology.NUMERICAL)
					|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(column.getValueType(), Ontology.DATE_TIME)) {
				amd.setValueRange(new Range(column.maxValue, column.maxValue), relation);
			} else {
				amd.setValueSet(column.valueSet, relation);
			}
			amd.setNumberOfMissingValues(missings);
			metaData.addAttribute(amd);
		}
		metaData.setNumberOfExamples(new MDInteger(getRowCountFromGuessing()));
		if (!isMetaDatafixed()) {
			metaData.getNumberOfExamples().increaseByUnknownAmount();
			metaData.attributesAreSuperset();
		}
		return metaData;
	}

	@Override
	protected boolean isMetaDataCacheable() {
		return true;
	}

	private double[] generateRow(DataSet set, List<Attribute> activeAttributes, int rowNumber) throws OperatorException {

		List<AttributeColumn> allAttributeColumns = getAllAttributeColumns();
		if (allAttributeColumns.size() > set.getNumberOfColumnsInCurrentRow()) {
			UnexpectedRowLenghtException e = new TooShortRowLengthException(rowNumber, set.getNumberOfColumnsInCurrentRow(),
					allAttributeColumns.size());
			if (isErrorTolerant()) {
				this.logReadingError(e);
			} else {
				throw e;
			}
		}

		if (allAttributeColumns.size() < set.getNumberOfColumnsInCurrentRow()) {
			UnexpectedRowLenghtException e = new TooLongRowLengthException(rowNumber, set.getNumberOfColumnsInCurrentRow(),
					allAttributeColumns.size());
			if (isErrorTolerant()) {
				this.logReadingError(e);
			} else {
				throw e;
			}
		}
		double[] values = new double[activeAttributes.size()];

		for (int i = 0; i < values.length; i++) {
			values[i] = Double.NaN;
		}

		int activeAttributeIndex = 0;
		for (int columnIndex = 0; columnIndex < set.getNumberOfColumnsInCurrentRow(); columnIndex++) {
			AttributeColumn column = allAttributeColumns.get(columnIndex);
			// skip deactivated columns
			if (!column.isActivated()) {
				continue;
			}

			assert columnIndex != -1;

			try {
				// do Ontology.ATTRIBUTE_VALUE_TYPE.isA(..) comparisons after
				// the
				// explicit check on the value type due to performance reasons.
				if (set.isMissing(columnIndex)) {
					values[activeAttributeIndex] = Double.NaN;
				} else if (column.getValueType() == Ontology.DATE || column.getValueType() == Ontology.TIME
						|| column.getValueType() == Ontology.DATE_TIME
						|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(column.getValueType(), Ontology.DATE_TIME)) {
					Date dateValue = set.getDate(columnIndex);
					if (dateValue == null) {
						throw new UnexpectedValueTypeException(Ontology.DATE_TIME, rowNumber, columnIndex,
								set.getString(columnIndex));
					}
					values[activeAttributeIndex] = dateValue.getTime();
				} else if (column.getValueType() == Ontology.INTEGER || column.getValueType() == Ontology.REAL
						|| column.getValueType() == Ontology.NUMERICAL
						|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(column.getValueType(), Ontology.NUMERICAL)) {
					Number numberValue = set.getNumber(columnIndex);
					if (numberValue == null) {
						throw new UnexpectedValueTypeException(Ontology.NUMERICAL, rowNumber, columnIndex,
								set.getString(columnIndex));
					}
					values[activeAttributeIndex] = numberValue.doubleValue();
				} else if (column.getValueType() == Ontology.BINOMINAL || column.getValueType() == Ontology.NOMINAL
						|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(column.getValueType(), Ontology.NOMINAL)) {
					try {
						values[activeAttributeIndex] = activeAttributes.get(activeAttributeIndex).getMapping()
								.mapString(set.getString(columnIndex));
					} catch (AttributeTypeException e) {
						if (isErrorTolerant()) {
							column.setValueType(Ontology.NOMINAL);
							Attribute att = column.createAttribute();
							for (String value : activeAttributes.get(columnIndex).getMapping().getValues()) {
								att.getMapping().mapString(value);
							}
							values[activeAttributeIndex] = att.getMapping().mapString(set.getString(columnIndex));
							activeAttributes.set(columnIndex, att);
						} else {
							throw new AttributeTypeException("Attribute: " + column.getName() + ", Index: " + columnIndex
									+ ", Row: " + rowNumber + "\n\n" + e.getMessage());
						}
					}
				} else {
					throw new OperatorException("The value type of the attribute " + column.getName() + " is unknown.");
				}

			} catch (UnexpectedValueTypeException e) {
				if (isErrorTolerant()) {
					values[activeAttributeIndex] = Double.NaN;
					this.logReadingError(e);
				} else {
					throw e;
				}
			}
			activeAttributeIndex++;
		}
		return values;
	}

	/**
	 * This method adjusts the number of columns to the given number.
	 */
	private void adjustAttributeColumnsNumbers(int newNumberOfColumns) {
		// too short
		if (getAllAttributeColumns().size() < newNumberOfColumns) {
			int actualNumberOfAttributes = getAllAttributeColumns().size();
			int numberOfNewColumns = newNumberOfColumns - actualNumberOfAttributes;
			String[] genericNames = new String[numberOfNewColumns];
			for (int i = 0; i < numberOfNewColumns; i++) {
				genericNames[i] = getNewGenericColumnName(actualNumberOfAttributes + i);
			}
			for (String name : genericNames) {
				attributeColumns.add(new AttributeColumn(name));
			}

		}
		// too long
		if (getAllAttributeColumns().size() > newNumberOfColumns) {
			List<AttributeColumn> list = new ArrayList<>();
			for (int i = 0; i < newNumberOfColumns; i++) {
				list.add(getAttributeColumn(i));
			}
			attributeColumns = list;
		}
	}

	/**
	 * Sets the name of each attribute to the given name.
	 *
	 * @param newColumnNames
	 */
	protected void setAttributeNames(String[] newColumnNames) {

		adjustAttributeColumnsNumbers(newColumnNames.length);
		assert attributeColumns.size() == newColumnNames.length;

		if (attributeNamesDefinedByUser()) {
			// assume attributes names were set already by the user
			return;
		}
		List<AttributeColumn> allAttributeColumns = getAllAttributeColumns();
		String[] oldColumnNames = new String[allAttributeColumns.size()];
		int i = 0;
		for (AttributeColumn column : allAttributeColumns) {
			oldColumnNames[i] = column.getName();
			i++;
		}

		newColumnNames = getGenericColumnNames(newColumnNames, oldColumnNames);
		i = 0;
		for (AttributeColumn column : allAttributeColumns) {
			column.setName(newColumnNames[i]);
			i++;
		}
	}

	/**
	 * Resets the column names to a generic column name given by the method
	 * {@link AbstractDataReader#getNewGenericColumnName(int)}.
	 */
	protected void resetColumnNames() {
		int i = 0;
		for (AttributeColumn column : getAllAttributeColumns()) {
			column.setName(getNewGenericColumnName(i));
			i++;
		}
	}

	/**
	 *
	 */
	public void stopReading() {
		stopReading = true;

	}

	protected void setAnnotations(Annotations[] annotations) {
		assert getAllAttributeColumns().size() == annotations.length;
		int i = 0;
		for (AttributeColumn column : getAllAttributeColumns()) {
			column.getAnnotations().clear();
			column.getAnnotations().putAll(annotations[i]);
			i++;
		}
	}

	protected void setValueTypes(List<Integer> valueTypesList) throws OperatorException {
		if (getAllAttributeColumns().size() != valueTypesList.size()) {
			throw new OperatorException(
					"Internal error: The number of valueTypes does not match with the number of attributes.");
		} else {
			Iterator<Integer> it = valueTypesList.iterator();
			for (AttributeColumn column : getAllAttributeColumns()) {
				column.setValueType(it.next());
			}
		}
	}

	/**
	 * @param e
	 */
	private void logReadingError(OperatorException e) {
		importErrors.add(e);
	}

	public List<OperatorException> getImportErrors() {
		return importErrors;
	}

	public List<Object[]> getShortPreviewAsList(ProgressListener progress, boolean trimAttributeColumns)
			throws OperatorException {
		return getPreviewAsList(progress, false, trimAttributeColumns, PREVIEW_LINES);
	}

	public List<Object[]> getPreviewAsList(ProgressListener progress, boolean trimAttributeColumns)
			throws OperatorException {
		if (detectErrorsInPreview) {
			return getPreviewAsList(progress, true, trimAttributeColumns, -1);
		} else {
			return getShortPreviewAsList(progress, trimAttributeColumns);
		}
	}

	public List<Object[]> getErrorPreviewAsList(ProgressListener progress) throws OperatorException {
		List<Object[]> preview = getPreviewAsList(progress, true, false, -1);
		List<Object[]> errorPreview = new LinkedList<>();

		Iterator<Object[]> it = preview.iterator();
		int rowNum = 0;
		while (it.hasNext()) {
			Object[] row = it.next();
			if (hasParseErrorInRow(rowNum)) {
				errorPreview.add(row);
			}
			rowNum++;
		}
		return errorPreview;
	}

	/**
	 *
	 * @see AbstractDataReader#PREVIEW_LINES
	 *
	 * @return
	 * @throws OperatorException
	 */
	public List<Object[]> getPreviewAsList(ProgressListener progress, boolean enableErrorDetection,
			boolean trimAttributeColumns, int numberOfLinesRead) throws OperatorException {
		stopReading = false;
		int limit = numberOfLinesRead;
		if (progress != null) {
			progress.setTotal(rowCountFromGuessing);
		}
		int hundredPercent = rowCountFromGuessing / 100;
		hundredPercent = hundredPercent == 0 ? 10 : hundredPercent;
		rowCountFromGuessing = 0;

		if (numberOfLinesRead < 0) {
			limit = rowCountFromGuessing + 1;
		}

		errorCells.clear();
		// clear value sets
		for (AttributeColumn column : getAllAttributeColumns()) {
			column.valueSet.clear();
		}

		DataSet set = null;
		try {
			set = getDataSet();
		} catch (IOException e) {
			throw new UserError(this, e, 403, e.getMessage());
		}

		List<Object[]> preview = new LinkedList<>();
		// counting starts at one because the user sees it.
		int currentRow = 1;

		UnexpectedRowLenghtException rowLenghtWarning = null;
		while (!stopReading && set.next() && rowCountFromGuessing < limit) {

			rowLenghtWarning = null;

			if (progress != null && currentRow % hundredPercent == 0) {
				progress.setCompleted(currentRow);
			}
			int actualColumnCount = set.getNumberOfColumnsInCurrentRow();

			if (getAllAttributeColumns().size() < actualColumnCount || getAllAttributeColumns().size() > actualColumnCount) {
				// report too short/long line errors only after the first line
				if (rowCountFromGuessing > 0) {
					foundParseError(-1, rowCountFromGuessing);
					if (getAllAttributeColumns().size() > actualColumnCount) {
						rowLenghtWarning = new TooShortRowLengthException(currentRow, actualColumnCount,
								getAllAttributeColumns().size());
					}
					if (getAllAttributeColumns().size() < actualColumnCount) {
						rowLenghtWarning = new TooLongRowLengthException(currentRow, actualColumnCount,
								getAllAttributeColumns().size());
						// only extend number of attribute column, do not shrink
						// them.
						adjustAttributeColumnsNumbers(actualColumnCount);
					}
				} else {
					if (trimAttributeColumns) {
						adjustAttributeColumnsNumbers(actualColumnCount);
					}
				}

			}

			Object[] values = new Object[actualColumnCount + 1];

			// first column of the preview contains the line/row number
			values[0] = currentRow;
			currentRow++;

			// walk through the columns
			for (int i = 0; i < actualColumnCount; i++) {
				AttributeColumn column = getAttributeColumn(i);

				if (set.isMissing(i)) {
					values[i + 1] = ""; //
					continue;
				} else {
					if (enableErrorDetection) {
						if (column.getValueType() == Ontology.DATE || column.getValueType() == Ontology.TIME
								|| column.getValueType() == Ontology.DATE_TIME) {
							values[i + 1] = set.getDate(i);
						} else if (column.getValueType() == Ontology.INTEGER || column.getValueType() == Ontology.REAL
								|| column.getValueType() == Ontology.NUMERICAL) {
							values[i + 1] = set.getNumber(i);
						} else if (column.getValueType() == Ontology.BINOMINAL) {
							values[i + 1] = set.getString(i);
							// look for value type error and update meta data
							// information
							if (values[i + 1] != null) {
								if (column.valueSet.size() >= 2) {
									if (!column.valueSet.contains(values[i + 1]) && column.isActivated) {
										foundParseError(i, rowCountFromGuessing);
									}
								} else {
									column.valueSet.add((String) values[i + 1]);
								}
							}
						} else if (column.getValueType() == Ontology.NOMINAL) {
							values[i + 1] = set.getString(i);
							// update meta data information
							column.valueSet.add((String) values[i + 1]);
						}

						if (values[i + 1] == null) {
							values[i + 1] = set.getString(i);
							if (column.isActivated) {
								foundParseError(i, rowCountFromGuessing);
							}
						}
					} else {
						// read everything as a string
						values[i + 1] = set.getString(i);
					}

				}
			}
			preview.add(values);
			rowCountFromGuessing++;
			// numberOfLinesRead == -1 means read till eof.:
			if (numberOfLinesRead < 0) {
				limit = rowCountFromGuessing + 1;
			}
			// TODO integrate warning in the GUI
			if (rowLenghtWarning != null) {
				// getLogger().warning(rowLenghtWarning.getMessage());
			}
		}
		set.close();
		guessedValueTypes = true;
		return preview;
	}

	private void foundParseError(int column, int row) {
		TreeSet<Integer> treeSet = errorCells.get(column);
		if (treeSet == null) {
			treeSet = new TreeSet<>();
			errorCells.put(column, new TreeSet<>());
		}
		treeSet.add(row);
	}

	public boolean hasParseErrorInColumn(int column) {
		TreeSet<Integer> treeSet = errorCells.get(column);
		return treeSet != null && !treeSet.isEmpty();
	}

	public boolean hasParseErrorInRow(int row) {
		for (int column : errorCells.keySet()) {
			if (hasParseError(column, row)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasParseError(int column, int row) {
		TreeSet<Integer> treeSet = errorCells.get(column);
		if (treeSet != null) {
			return treeSet.contains(row);
		}
		return false;
	}

	public boolean isDetectErrorsInPreview() {
		return detectErrorsInPreview;
	}

	public void setDetectErrorsInPreview(boolean detectErrorsInPreview) {
		this.detectErrorsInPreview = detectErrorsInPreview;
	}

	/**
	 * Returns a new column name for new column to build. Probably something like "attribute_1".
	 *
	 * @param column
	 * @return a unique column name
	 */
	protected String getNewGenericColumnName(int column) {
		HashSet<String> usedNames = new HashSet<>();
		for (AttributeColumn col : getAllAttributeColumns()) {
			usedNames.add(col.getName());
		}

		while (usedNames.contains("attribute_" + column)) {
			column++;
		}
		return "attribute_" + column;
	}

	/**
	 * Returns a generic column name, probably something like proposedName+"_"+columnIndex.
	 *
	 * @param oldColumnNames
	 *
	 * @param proposedName
	 *            can be null, then "attribute" the proposed name is "attribute"
	 * @param columnIndex
	 *            the index of the column of this attribute.
	 * @return
	 */
	private String[] getGenericColumnNames(String[] proposedNames, String[] oldColumnNames) {
		HashSet<String> usedNames = new HashSet<>();
		for (AttributeColumn col : getAllAttributeColumns()) {
			usedNames.add(col.getName());
		}

		int offset = usedNames.size();
		String[] genericNames = new String[proposedNames.length];
		for (int i = 0; i < proposedNames.length; i++) {
			String proposedName = proposedNames[i];
			if (proposedName == null) {
				proposedName = "attribute_" + (offset + i + 1);
			}
			if (!proposedName.equals(oldColumnNames[i])) {
				if (usedNames.contains(proposedName)) {
					proposedName = proposedName + "_" + (offset + i + 1);
				}
				usedNames.add(proposedName);
			}
			genericNames[i] = proposedName;
		}
		return genericNames;
	}

	/**
	 * Guesses the attribute value types based on the values in the first
	 * {@link AbstractDataReader#PREVIEW_LINES} rows.
	 *
	 * @see {@link AbstractDataReader#PREVIEW_LINES}
	 *
	 * @throws OperatorException
	 */
	public void guessValueTypes(ProgressListener progress) throws OperatorException {
		stopReading = false;
		// this.clearReaderSettings();

		DataSet set = null;
		try {
			set = getDataSet();
		} catch (IOException e) {
			throw new UserError(this, e, 403, e.getMessage());
		}
		if (progress != null) {
			progress.setTotal(PREVIEW_LINES);
		}
		int tenPercent = PREVIEW_LINES / 10;
		tenPercent = tenPercent == 0 ? 10 : tenPercent;
		rowCountFromGuessing = 0;
		int linesTried = 0;

		// TODO introduce timeout instead PREVIEW_LINES
		while (!stopReading && set.next() && linesTried <= PREVIEW_LINES) {
			rowCountFromGuessing++;
			// only read every 10'th line to see more diverse cells
			if (rowCountFromGuessing > 20 && rowCountFromGuessing % 10 != 0) {
				continue;
			}
			if (progress != null && linesTried % tenPercent == 0) {
				progress.setCompleted(linesTried);
			}
			linesTried++;

			int actualColumnCount = set.getNumberOfColumnsInCurrentRow();

			// add column(s) if the read row has more columns then previously
			// seen.
			if (getAllAttributeColumns().size() < actualColumnCount) {
				adjustAttributeColumnsNumbers(actualColumnCount);
			}

			Object[] values = new Object[actualColumnCount];

			// TODO remove this from the loop
			Calendar lastDateCalendar = Calendar.getInstance();
			Calendar currDateCalendar = Calendar.getInstance();

			// go through the actual row
			for (int i = 0; i < actualColumnCount; i++) {
				AttributeColumn column = getAttributeColumn(i);
				if (set.isMissing(i)) {
					column.incNummerOfMissing();
					values[i] = null;
					continue;
				}
				// try numerical value type
				if (column.canParseDouble) {
					Number number = set.getNumber(i);
					if (number != null) {
						if (Double.isNaN(number.doubleValue())) {
							column.incNummerOfMissing();
							continue;
						}
						// actualize min/max values
						if (column.minValue > number.doubleValue()) {
							column.minValue = number.doubleValue();
						}
						if (column.maxValue < number.doubleValue()) {
							column.maxValue = number.doubleValue();
						}
						// try integer
						if (column.canParseInteger && !Tools.isEqual(Math.round(number.doubleValue()), number.doubleValue())) {
							column.canParseInteger = false;
						}
						// set the value
						values[i] = number;

						// for guessing binomial.
						if (column.valueSet.size() <= 2) {
							column.valueSet.add(number.toString());
						}
						continue;
					} else {
						// numerical failed
						column.canParseDouble = false;
						column.canParseInteger = false;
					}
				}
				// try Date mhh? what's going on here?
				if (column.canParseDate) {
					Date date = set.getDate(i);
					if (date != null) {
						// set the value
						values[i] = date;
						// determine whether it is date or time
						if (column.lastDate != null) {
							lastDateCalendar.setTime(column.lastDate);
							currDateCalendar.setTime(date);
							if (!column.shouldBeDate && (lastDateCalendar.get(Calendar.DAY_OF_MONTH) != currDateCalendar.get(Calendar.DAY_OF_MONTH)
									|| lastDateCalendar.get(Calendar.MONTH) != currDateCalendar.get(Calendar.MONTH)
									|| lastDateCalendar.get(Calendar.YEAR) != currDateCalendar.get(Calendar.YEAR))) {
								column.shouldBeDate = true;
							}
							if (!column.shouldBeTime && (lastDateCalendar.get(Calendar.HOUR_OF_DAY) != currDateCalendar.get(Calendar.HOUR_OF_DAY)
									|| lastDateCalendar.get(Calendar.MINUTE) != currDateCalendar.get(Calendar.MINUTE)
									|| lastDateCalendar.get(Calendar.SECOND) != currDateCalendar.get(Calendar.SECOND)
									|| lastDateCalendar.get(Calendar.MILLISECOND) != currDateCalendar.get(Calendar.MILLISECOND))) {
								column.shouldBeTime = true;
							}
						}
						column.lastDate = date;

						if (column.minValue > date.getTime()) {
							column.minValue = date.getTime();
						}
						if (column.maxValue < date.getTime()) {
							column.maxValue = date.getTime();
						}

						// for guessing binomial.
						if (column.valueSet.size() <= 2) {
							column.valueSet.add(date.toString());
						}
						continue;
					} else {
						column.canParseDate = false;
					}
				}
				// nothing worked, choose nominal
				String string = set.getString(i);
				if (string != null && !string.isEmpty()) {
					values[i] = string;
					// for guessing binomial.
					if (column.valueSet.size() <= 2) {
						column.valueSet.add(string);
					}
					continue;
				} else {
					column.incNummerOfMissing();
					values[i] = null;
					continue;
				}
			}
		}

		set.close();

		// set up the guessed value types
		for (AttributeColumn column : getAllAttributeColumns()) {
			if (column.numberOfMissings == rowCountFromGuessing) {
				column.setValueType(Ontology.NOMINAL);
				// column.activateColumn(false);
				continue;
			}
			if (column.canParseInteger) {
				column.setValueType(Ontology.INTEGER);
				continue;
			}
			if (column.canParseDouble) {
				column.setValueType(Ontology.REAL);
				continue;
			}
			if (column.canParseDate) {
				if (column.shouldBeDate && column.shouldBeTime) {
					column.setValueType(Ontology.DATE_TIME);
					continue;
				}
				if (column.shouldBeDate) {
					column.setValueType(Ontology.DATE);
					continue;
				}
				if (column.shouldBeTime) {
					column.setValueType(Ontology.TIME);
					continue;
				}
				throw new OperatorException("Could not determine the value type of the attribute " + column.getName()
						+ " in column number " + getIndexOfAttributeColumn(column) + ".");
			}
			// maybe binomial?
			if (column.valueSet.size() <= 2) {
				column.setValueType(Ontology.BINOMINAL);
				continue;
			}
			// last option nominal
			column.setValueType(Ontology.NOMINAL);
		}
		guessedValueTypes = true;
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		return createExampleSet(-1);
	}

	/**
	 * Creates an {@link ExampleSet} with the given {@link ExampleSetMetaData} and reads at the most
	 * <code>limitOfReadLines</code> lines.
	 *
	 *
	 * @param metaData
	 * @param limitOfReadLines
	 *            max number of read lines.
	 * @return
	 * @throws OperatorException
	 */
	private ExampleSet createExampleSet(int limitOfReadLines) throws OperatorException {
		List<Attribute> activeAttributes = new ArrayList<>();

		// load the attribute names/value types/ roles/... which are defined by
		// the user
		if (attributeNamesDefinedByUser()) {
			loadMetaDataFromParameters();
		} else {
			clearAllReaderSettings();
			if (!skipGuessingValueTypes) {
				guessValueTypes(null);
			}
		}

		// get the data set
		DataSet set = null;
		try {
			set = getDataSet();
		} catch (IOException e) {
			throw new UserError(this, e, 403, e.getMessage());
		}

		// finally create the actual attributes here
		for (AttributeColumn column : getActiveAttributeColumns()) {
			activeAttributes.add(column.getAttribute());
		}

		// list of double arrays which holds the read values fpr each line
		List<double[]> dataRows = new ArrayList<>();

		int lineCount = 0; // debugging purpose
		while (set.next() && (limitOfReadLines == -1 || limitOfReadLines > lineCount)) {
			double[] row = generateRow(set, activeAttributes, lineCount);

			// collect the read line
			dataRows.add(row);
			lineCount++;
			try {
				// check for abort of the process
				checkForStop();
			} catch (ProcessStoppedException e) {
				dataRows = null;
				// table = null;
				set.close();
				set = null;
				throw e;
			}
		}
		// build the example table with the active attributes.
		// The attributes might have changed during the loop above
		// (Only if this is instance of CSVDataReader, see generateRow() ).
		// This happens if a line is read that has more columns then expected.
		ExampleSetBuilder builder = ExampleSets.from(activeAttributes).withExpectedSize(dataRows.size());

		Iterator<double[]> rowIt = dataRows.iterator();
		double[] row = null;
		try {
			while (rowIt.hasNext()) {
				row = rowIt.next();
				// adopt size of the row in case some attributes were added during
				// the reading. Should happen only this is a CSVDataReader.
				if (row.length < activeAttributes.size()) {
					double[] values = new double[activeAttributes.size()];
					System.arraycopy(row, 0, values, 0, row.length);
					for (int i = row.length; i < values.length; i++) {
						values[i] = Double.NaN;
					}
					row = values;
				}
				builder.addRow(row);

				// check for abort of the process
				checkForStop();
			}

			ExampleSet exampleSet = builder.build();

			// set special attributes
			for (AttributeColumn column : getActiveAttributeColumns()) {
				if (!column.getRole().equals(AttributeColumn.REGULAR)) {
					exampleSet.getAttributes().setSpecialAttribute(exampleSet.getAttributes().get(column.getName()),
							column.getRole());
				}
			}
			// add annotations
			addAnnotations(exampleSet);
			return exampleSet;

		} finally {
			set.close();
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.rapidminer.operator.io.AbstractReader#getParameterTypes()
	 */
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<>();
		types.addAll(super.getParameterTypes());

		types.add(new ParameterTypeBoolean(PARAMETER_ERROR_TOLERANT,
				"Values which does not match to the specified value typed are considered as missings.", true, true));

		// The meta data parameters which holds the information about the
		// attribute/column properties, eg. name, role, value type ...
		String[] roles = new String[ROLE_NAMES.size()];
		for (int i = 0; i < roles.length; i++) {
			roles[i] = ROLE_NAMES.get(i);
		}
		// hidden param
		ParameterTypeList typeList = new ParameterTypeList(PARAMETER_META_DATA, "The meta data information",
				new ParameterTypeInt(PARAMETER_COLUMN_INDEX, "The column index", 0, 9999), //
				new ParameterTypeTupel(PARAMETER_COLUMN_META_DATA, "the meta data information of one column", //
						new ParameterTypeString(PARAMETER_COLUMN_NAME, "Describes the attributes name."), //
						new ParameterTypeBoolean(PARAMETER_COLUMN_SELECTED, "Indicates if a column is selected", true), //
						new ParameterTypeCategory(PARAMETER_COLUM_VALUE_TYPE, "Indicates the value type of an attribute",
								Ontology.VALUE_TYPE_NAMES, Ontology.NOMINAL), //
						new ParameterTypeStringCategory(PARAMETER_COLUM_ROLE, "Indicates the role of an attribute", roles,
								AttributeColumn.REGULAR)));
		typeList.setHidden(true);
		types.add(typeList);

		// hidden param
		ParameterTypeBoolean typeBool = new ParameterTypeBoolean(PARAMETER_ATTRIBUTE_NAMES_DEFINED_BY_USER,
				"the parameter describes whether the attribute names were set by the user manually or were generated by the the reader (generic names or first row of the file)",
				false);
		typeBool.setHidden(true);
		types.add(typeBool);
		return types;
	}

	/**
	 * Use this method to set the parameter {@link AbstractDataReader#PARAMETER_ERROR_TOLERANT}. Do
	 * not set the parameter directly because its value need to be cached.
	 *
	 * @param flag
	 */
	public void setErrorTolerant(boolean flag) {
		isErrorTollerantCache = flag;
		setParameter(PARAMETER_ERROR_TOLERANT, Boolean.toString(flag));
	}

	/**
	 * @return the cached value of the parameter {@link AbstractDataReader#PARAMETER_ERROR_TOLERANT}
	 *         . The parameter needs to be cached since it cost to much time to read the parameter
	 *         every line.
	 */
	public boolean isErrorTolerant() {
		return isErrorTollerantCache;
	}

	/**
	 * Observer that clears the reader settings if the source file is changed. Only relevant for
	 * {@link CSVDataReader} and {@link ExcelExampleSource}
	 *
	 * @author Sebastian Loh (14.07.2010)
	 *
	 */
	protected class CacheResetParameterObserver implements Observer<String> {

		private String parameterKey;
		private String oldFilename;

		protected CacheResetParameterObserver(String parameterKey) {
			this.parameterKey = parameterKey;
		}

		@Override
		public void update(Observable<String> observable, String arg) {
			String newFilename = getParameters().getParameterOrNull(parameterKey);
			if (oldFilename == newFilename) {
				return;
			}
			if (oldFilename == null || newFilename == null || !newFilename.equals(oldFilename)) {
				clearAllReaderSettings();
				this.oldFilename = newFilename;
			}
		}
	}

	private abstract class UnexpectedRowLenghtException extends OperatorException {

		private static final long serialVersionUID = 1L;

		private int rowNumber = -1;
		private int rowLenght = -1;
		int expectedRowLenght = -1;

		/**
		 *
		 */
		public UnexpectedRowLenghtException(String message, int rowNumber, int rowLenght, int expectedRowLenght) {
			super(message);
			this.rowNumber = rowNumber;
			this.rowLenght = rowLenght;
			this.expectedRowLenght = expectedRowLenght;
		}

		/**
		 *
		 */
		public UnexpectedRowLenghtException(int rowNumber, int rowLenght, int expectedRowLenght) {
			super("NO MESSAGE");
			this.rowNumber = rowNumber;

		}

		/**
		 * Returns the row where the error occurred. <b>Warning:</b> you might want to add +1 if you
		 * intend to present this number to the user.
		 *
		 *
		 * @return
		 */
		public int getRow() {
			return rowNumber;
		}

		/**
		 * Returns the length of the the row {@link UnexpectedRowLenghtException#rowNumber}
		 *
		 * @return
		 */
		public int getRowLenght() {
			return rowLenght;
		}

		public int getExpectedRowLenght() {
			return expectedRowLenght;
		}
	}

	public class TooShortRowLengthException extends UnexpectedRowLenghtException {

		private static final long serialVersionUID = -9183147637149034838L;

		/**
		 * @param rowNumber
		 * @param rowLenght
		 * @param expectedRowLenght
		 */
		public TooShortRowLengthException(int rowNumber, int rowLenght, int expectedRowLenght) {
			super("Row number <b>" + rowNumber + "<//b> is too <b>short<//b>. The row has <b>" + rowLenght
					+ "<//b> columns but it is expected to have <b>" + expectedRowLenght + "<//b> columns.", rowNumber,
					rowLenght, expectedRowLenght);
		}
	}

	public class TooLongRowLengthException extends UnexpectedRowLenghtException {

		private static final long serialVersionUID = -9079042758212112074L;

		/**
		 * @param rowNumber
		 * @param rowLenght
		 * @param expectedRowLenght
		 */
		public TooLongRowLengthException(int rowNumber, int rowLenght, int expectedRowLenght) {
			super("Row number <b>" + rowNumber + "</b> is too <b>long</b>. It has <b>" + rowLenght
					+ "</b> columns but it is expected to have <b>" + expectedRowLenght + "</b> columns.", rowNumber,
					rowLenght, expectedRowLenght);
		}
	}

	public static class UnexpectedValueTypeException extends OperatorException {

		private static final long serialVersionUID = 1L;
		private int expectedValueType = -1;
		private int row = -1;
		private int column = -1;
		private Object value = null;

		public UnexpectedValueTypeException(String message, int expectedValueType, int column, int row, Object value) {
			super(message);
			this.expectedValueType = expectedValueType;
			this.row = row;
			this.column = column;
			this.value = value;
		}

		/**
		 * Creates a proper error message;
		 *
		 * @param expectedValueType
		 * @param column
		 * @param row
		 * @param value
		 */
		public UnexpectedValueTypeException(int expectedValueType, int column, int row, Object value) {
			this("Could not interpreted the value <b>" + value + "<//b> in row <b>" + row + "<//b> and column <b>" + column
					+ "<//b> as a <b>" + Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(expectedValueType)
					+ "<//b>. Plaese adjust to a proper value type or enable the operator's error tolerance.",
					expectedValueType, column, row, value);
		}

		/**
		 * Returns the row where the error occurred. <b>Warning:</b> you might want to add +1 if you
		 * intend to present this number to the user.
		 *
		 * @return
		 */
		public int getRow() {
			return row;
		}

		/**
		 * Returns the column where the error occurred. <b>Warning:</b> you might want to add +1 if
		 * you intend to present this number to the user.
		 *
		 * @return
		 */
		public int getColumn() {
			return column;
		}

		/**
		 * Returns the value which caused the error
		 *
		 * @return
		 */
		public Object getValue() {
			return value;
		}

		/**
		 * @return the expectedValueType
		 */
		public int getExpectedValueType() {
			return expectedValueType;
		}
	}

	/**
	 * @author Sebastian Loh (28.04.2010)
	 *
	 *         <p>
	 *         Private class describing a column of the created ExampleSet. Holds all information
	 *         (name, value type, annotations) in order to create the actual attribute for this
	 *         column. Despite that the class manages different properties - eg. the activation
	 *         status (is the column actual selected to be read?) and missing values - in order to
	 *         build proper meta data description.
	 *         </p>
	 *
	 * @see AbstractDataReader#getGeneratedMetaData()
	 * @see AbstractDataReader#guessValueTypes()
	 * @see AbstractDataReader#createExampleSet(ExampleSetMetaData, int)
	 *
	 */
	public class AttributeColumn {

		public static final int NAME_PARAMETER = 0;

		public static final int IS_ACTIVATED_PARAMETER = 1;

		public static final int VALUE_TYPE_PARAMETER = 2;

		public static final int ROLE_PARAMETER = 3;

		@Override
		public String toString() {
			return name + "," + isActivated + "," + valueType + "," + role + "," + annotations;
		}

		/**
		 * ugly workaround to define regular role instead of role = null;
		 */
		public static final String REGULAR = "regular";

		private String name;

		private String role = REGULAR;

		private boolean isActivated = true;

		private int valueType = Ontology.NOMINAL;

		private Attribute attribute = null;

		/**
		 * the column's annotations that are also the attribute's annotations, which is created from
		 * this column.
		 */
		private Annotations annotations = new Annotations();

		/**
		 * The minValue of this attribute. Only for the operator MetaData purposes.
		 */
		protected double minValue = Double.NEGATIVE_INFINITY;

		/**
		 * The maxValue of this attribute. Only for the operator MetaData purposes.
		 */
		protected double maxValue = Double.POSITIVE_INFINITY;

		/**
		 * The valueSet of this attribute, in case it is (bi)nominal. Only for the operator MetaData
		 * purposes.
		 */
		protected Set<String> valueSet = new LinkedHashSet<>();

		/**
		 * The number of missing values which were read during the guessing. Only for the operator
		 * MetaData purposes.
		 */
		protected int numberOfMissings = 0;

		/**
		 * indicate whether this attribute is a candidate for value type real
		 */
		private boolean canParseDouble = true;

		/**
		 * indicate whether this attribute is a candidate for value type integer
		 */
		private boolean canParseInteger = true;

		/**
		 * indicate whether this attribute is a candidate for value type date
		 */
		private boolean canParseDate = true;

		/**
		 * indicate whether this attribute is a candidate for value type date without time
		 */
		private boolean shouldBeDate = false;

		/**
		 * indicate whether this attribute is a candidate for value type only time
		 */
		private boolean shouldBeTime = false;

		/**
		 * the last date which was read, to guess if it is date or date/time or both
		 */
		private Date lastDate = null;

		/**
		 * increase the number of read missing value by one.
		 *
		 * @return the number after the increasement.
		 */
		public int incNummerOfMissing() {
			return numberOfMissings++;
		}

		/**
		 * @return
		 */
		public Annotations getAnnotations() {
			return annotations;
		}

		/**
		 * Creates the actual attribute object that is described by this column's properties (name,
		 * value type, annotations).
		 *
		 * @return the created attribute.
		 */
		private Attribute createAttribute() {
			Attribute att = AttributeFactory.createAttribute(getName(), getValueType());
			att.getAnnotations().clear();
			att.getAnnotations().putAll(getAnnotations());
			attribute = att;
			return att;
		}

		/**
		 * @return the columns {@link Attribute}. If a attribute was not created before, a new
		 *         Attribute is created. Otherwise a new Attribute is created if the already
		 *         existing Attribute does not match to the current AttributeColumn settings.
		 */
		public Attribute getAttribute() {
			if (attribute == null) {
				return createAttribute();
			}
			if (!attribute.getName().equals(getName())) {
				return createAttribute();
			}
			if (attribute.getValueType() != this.getValueType()) {
				return createAttribute();
			}
			// check same annotations
			for (String key : this.getAnnotations().getKeys()) {
				if (!attribute.getAnnotations().get(key).equals(this.getAnnotations().get(key))) {
					return createAttribute();
				}
			}
			// else the attribute information are equal:
			return attribute;
		}

		/**
		 * Indicated whether this column is actual read/imported or if it is ignored. In other word,
		 * returns <code>true</code> if the column is active
		 */
		public boolean isActivated() {
			return isActivated;
		}

		/**
		 * Activates or deactivates this column.
		 *
		 * @param flag
		 */
		public void activateColumn(boolean flag) {
			isActivated = flag;
		}

		/**
		 * Returns the value type of the columns attribute.
		 *
		 * @see Ontology
		 *
		 * @return
		 */
		public int getValueType() {
			return valueType;
		}

		/**
		 * Sets the value type of the columns attribute by actually replacing the existing attribute
		 * with a new generated attribute with same name and the new type.
		 *
		 * @param newValueType
		 */
		public void setValueType(int newValueType) {
			valueType = newValueType;
		}

		/**
		 * Returns the name of this column, which is also the name of the attribute that is created
		 * from this column's properties.
		 *
		 * @return the name
		 */
		public String getName() {
			return name;
			//
		}

		/**
		 * @param name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Returns the attribute's role as a String.
		 *
		 * @return
		 */
		public String getRole() {
			return role;
		}

		/**
		 * Set the role of the attribute column
		 *
		 * @param role
		 */
		public void setRole(String role) {
			this.role = role;
		}

		private String getMetaParameter(int parameterIndex) {
			int index = getIndexOfAttributeColumn(this);

			try {
				// get former parameters
				List<String[]> list = getParameterList(PARAMETER_META_DATA);

				String[] metadata;
				String tuple;
				String[] map = null;
				// get the metadata of this attribute if exists
				for (String[] m : list) {
					if (Integer.parseInt(m[0]) == index) {
						map = m;
						break;
					}
				}
				if (map == null) {
					return null;
				}

				tuple = map[1];
				metadata = ParameterTypeTupel.transformString2Tupel(tuple);

				// return the parameter
				return metadata[parameterIndex];
			} catch (UndefinedParameterError e) {
				e.printStackTrace();
				return null;
			}
		}

		private void loadMetaParameter() {
			if (getMetaParameter(NAME_PARAMETER) != null) {
				setName(getMetaParameter(NAME_PARAMETER));
			}
			String s = getMetaParameter(VALUE_TYPE_PARAMETER);
			if (s != null) {
				setValueType(Integer.parseInt(s));
			}
			if (getMetaParameter(ROLE_PARAMETER) != null) {
				setRole(getMetaParameter(ROLE_PARAMETER));
			}
			if (getMetaParameter(IS_ACTIVATED_PARAMETER) != null) {
				activateColumn(Boolean.parseBoolean(getMetaParameter(IS_ACTIVATED_PARAMETER)));
			}

		}

		/**
		 *
		 *
		 * Sets the meta data value with entry <code>parameterIndex</code> of this
		 * {@link AttributeColumn}.
		 * <p>
		 * If this method is called the first time for this attributeColumn, default parameters are
		 * set. (the first time means that this attribute column does not have an index, ie.
		 * <code>getIndexOfAttributeColumn(this) == -1</code>.
		 * </p>
		 *
		 * @param parameterIndex
		 *            legal parameters are: {@link AttributeColumn#NAME_PARAMETER},
		 *            {@link AttributeColumn#IS_ACTIVATED_PARAMETER},
		 *            {@link AttributeColumn#VALUE_TYPE_PARAMETER},
		 *            {@link AttributeColumn#ROLE_PARAMETER}.
		 *
		 * @param value
		 *            the new value
		 */
		private void setMetaParameter() {
			// get index of this column
			int myIndex = getIndexOfAttributeColumn(this);

			try {
				List<String[]> list = getParameterList(PARAMETER_META_DATA);
				String[] map = null;
				for (String[] mapIndexToValues : list) {
					if (Integer.parseInt(mapIndexToValues[0]) == myIndex) {
						map = mapIndexToValues;
						break;
					}
				}
				String[] metadata;
				String tuple;
				// if an entry for this attribute column did not exist, create a
				// new one:
				if (map == null) {
					map = new String[2];
					map[0] = Integer.toString(myIndex);
					list.add(map);
					metadata = new String[4];
				} else {
					tuple = map[1];
					metadata = ParameterTypeTupel.transformString2Tupel(tuple);
				}
				// create new entries with default values
				metadata[NAME_PARAMETER] = name;
				// selection is true
				metadata[IS_ACTIVATED_PARAMETER] = Boolean.toString(isActivated);
				// value type is nominal
				metadata[VALUE_TYPE_PARAMETER] = Integer.toString(valueType);
				// role is regular
				metadata[ROLE_PARAMETER] = role;

				// write everything back
				tuple = ParameterTypeTupel.transformTupel2String(metadata);
				map[1] = tuple;

				// list.set(index, map);

				// store modified metadata in the parameter;
				String entry = ParameterTypeList.transformList2String(list);
				setParameter(PARAMETER_META_DATA, entry);
			} catch (UndefinedParameterError e) {
				e.printStackTrace();
			}
		}

		/**
		 * creates a new column and generated a attribute with the given name and nominal value type
		 *
		 * @param attributeName
		 */
		public AttributeColumn(String attributeName) {
			// default parameters for value type, ... are implicit created
			this.setName(attributeName);
			// this.setValueType(Ontology.NOMINAL);
		}
	}

	protected abstract class DataSet {

		/**
		 * Proceed to the next row if existent. Should return true if such a row exists or false, if
		 * no such next row exists.
		 *
		 * @return
		 */
		public abstract boolean next();

		/**
		 * Returns the number of columns in the current row, i.e. the length of the row.
		 *
		 * @return
		 */
		public abstract int getNumberOfColumnsInCurrentRow();

		/**
		 * Returns whether the value in the specified column in the current row is missing.
		 *
		 * @param columnIndex
		 *            index of the column
		 * @return
		 */
		public abstract boolean isMissing(int columnIndex);

		/**
		 * Returns a numerical value contained in the specified column in the current row. Should
		 * return null if the value is not a numerical or if the value is missing.
		 *
		 * @param columnIndex
		 * @return
		 */
		public abstract Number getNumber(int columnIndex);

		/**
		 * Returns a nominal value contained in the specified column in the current row. Should
		 * return null if the value is not a nominal or a kind of string type or if the value is
		 * missing.
		 *
		 * @param columnIndex
		 * @return
		 */
		public abstract String getString(int columnIndex);

		/**
		 * Returns a date, time or date_time value contained in the specified column in the current
		 * row. Should return null if the value is not a date or time value or if the value is
		 * missing.
		 *
		 * @param columnIndex
		 * @return
		 */
		public abstract Date getDate(int columnIndex) throws OperatorException;

		/**
		 * Closes the data source. May tear down a database connection or close a file which is re`
		 * from.
		 *
		 * @throws OperatorException
		 */
		public abstract void close() throws OperatorException;
	}

}
