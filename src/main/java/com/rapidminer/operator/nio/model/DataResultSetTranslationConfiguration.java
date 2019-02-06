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

import static com.rapidminer.operator.nio.model.AbstractDataResultSetReader.ANNOTATION_NAME;
import static com.rapidminer.operator.nio.model.AbstractDataResultSetReader.PARAMETER_ANNOTATIONS;
import static com.rapidminer.operator.nio.model.AbstractDataResultSetReader.PARAMETER_FIRST_ROW_AS_NAMES;
import static com.rapidminer.operator.nio.model.AbstractDataResultSetReader.PARAMETER_LOCALE;
import static com.rapidminer.operator.nio.model.AbstractDataResultSetReader.PARAMETER_META_DATA;
import static com.rapidminer.operator.nio.model.AbstractDataResultSetReader.PARAMETER_READ_AS_POLYNOMINAL;
import static com.rapidminer.parameter.ParameterTypeDateFormat.PARAMETER_DATE_FORMAT;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.IntStream;

import com.rapidminer.example.Attributes;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorRuntimeException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.generator.ExampleSetGenerator;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.preprocessing.filter.AbstractDateDataProcessing;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;


/**
 * This class holds information how a DataResultSet is translated into an ExampleSet. Therefore it
 * holds information about the final name, the value type, role and if the column is selected at
 * all.
 *
 * @author Sebastian Land, Simon Fischer
 */
public class DataResultSetTranslationConfiguration {

	private ColumnMetaData[] columnMetaData;

	private Locale locale = Locale.getDefault();
	private String datePattern = "";

	private final SortedMap<Integer, String> annotationsMap = new TreeMap<>();
	private boolean faultTolerant = true;

	private ThreadLocal<DateFormat> dateFormat;

	private NumberFormat numberFormat;

	private int dataManagementType = DataRowFactory.TYPE_DOUBLE_ARRAY;

	private boolean trimAttributeNames = true;

	private boolean trimForGuessing = true;

	/**
	 * This constructor can be used to generate an empty configuration just depending on the given
	 * resultSet
	 *
	 * @param readerOperator
	 * @throws OperatorException
	 */
	public DataResultSetTranslationConfiguration(AbstractDataResultSetReader readerOperator) {
		this(readerOperator, null);
	}

	/**
	 * This constructor can be used to generate a configuration with the given annotations.
	 *
	 * @param dataResultSet
	 *            the result set to derive parameters from
	 * @param annotations
	 *            list of annotations for the rows
	 */
	public DataResultSetTranslationConfiguration(DataResultSet dataResultSet, List<String> annotations) {
		this(null, dataResultSet);
		int counter = 0;
		annotationsMap.clear();
		for (String annotation : annotations) {
			if (annotation != null) {
				annotationsMap.put(counter, annotation);
			}
			counter++;
		}
	}

	/**
	 * Creates the configuration based on the parameter values stored in the given reader. If these
	 * parameters aren't present they are derived from the data result set delivered and everything
	 * will just be passed. This means, names are identically as delivered from the underlying
	 * result set, value type will be the one fitting, everything is selected, no roles are defined.
	 *
	 * @throws OperatorException
	 */
	private DataResultSetTranslationConfiguration(AbstractDataResultSetReader readerOperator, DataResultSet dataResultSet) {
		reconfigure(dataResultSet);
		reconfigure(readerOperator);
	}

	public void reconfigure(AbstractDataResultSetReader readerOperator) {
		// reading parameter settings
		if (readerOperator != null) {
			try {
				dataManagementType = readerOperator.getParameterAsInt(ExampleSetGenerator.PARAMETER_DATAMANAGEMENT);
			} catch (UndefinedParameterError e1) {
				dataManagementType = DataRowFactory.TYPE_DOUBLE_ARRAY;
			}

			boolean firstRowAsNames = readerOperator.getParameterAsBoolean(PARAMETER_FIRST_ROW_AS_NAMES);
			if (firstRowAsNames) {
				annotationsMap.put(0, ANNOTATION_NAME);
			} else {
				List<String[]> annotations;
				try {
					annotations = readerOperator.getParameterList(PARAMETER_ANNOTATIONS);
				} catch (UndefinedParameterError e) {
					annotations = Collections.emptyList();
				}
				for (String[] annotation : annotations) {
					annotationsMap.put(Integer.parseInt(annotation[0]), annotation[1]);
				}
			}

			// reading date format settings
			try {
				setDatePattern(readerOperator.getParameterAsString(PARAMETER_DATE_FORMAT));
			} catch (UndefinedParameterError e) {
				setDatePattern("");
			}

			try {
				int localeIndex;
				localeIndex = readerOperator.getParameterAsInt(PARAMETER_LOCALE);
				if (localeIndex >= 0 && localeIndex < AbstractDateDataProcessing.availableLocales.size()) {
					locale = AbstractDateDataProcessing.availableLocales.get(localeIndex);
				}
			} catch (UndefinedParameterError e) {
				locale = Locale.getDefault();
			}

			columnMetaData = readColumnMetaData(readerOperator);
			setFaultTolerant(readerOperator.getParameterAsBoolean(AbstractDataResultSetReader.PARAMETER_ERROR_TOLERANT));
			trimAttributeNames = readerOperator.shouldTrimAttributeNames();
			trimForGuessing = readerOperator.trimForGuessing();
		} else {
			annotationsMap.put(0, ANNOTATION_NAME);
			trimAttributeNames = true;
			trimForGuessing = true;
		}
	}

	public void setColumnMetaData(ColumnMetaData[] columnMetaData) {
		this.columnMetaData = columnMetaData;
	}

	public void reconfigure(DataResultSet dataResultSet) {
		if (dataResultSet != null) {
			int numberOfColumns = dataResultSet.getNumberOfColumns();
			columnMetaData = new ColumnMetaData[numberOfColumns];
			final String[] originalColumnNames = dataResultSet.getColumnNames();
			int[] attributeValueTypes = dataResultSet.getValueTypes();
			for (int i = 0; i < numberOfColumns; i++) {
				columnMetaData[i] = new ColumnMetaData(originalColumnNames[i], originalColumnNames[i],
						attributeValueTypes[i], Attributes.ATTRIBUTE_NAME, true, trimAttributeNames);
			}
		}
	}

	/** Sets the parameters in the given operator to describe this configuration. */
	public void setParameters(AbstractDataResultSetReader operator) {
		operator.getParameters().setParameter(PARAMETER_DATE_FORMAT, getDatePattern());
		// meta data
		List<String[]> metaDataList = new LinkedList<>();
		int index = 0;
		for (ColumnMetaData cmd : getColumnMetaData()) {
			String[] tupel = new String[4];
			tupel[0] = cmd.getUserDefinedAttributeName();
			tupel[1] = String.valueOf(cmd.isSelected());
			tupel[2] = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(cmd.getAttributeValueType());
			tupel[3] = cmd.getRole();
			String encodedTupel = ParameterTypeTupel.transformTupel2String(tupel);
			metaDataList.add(new String[] { String.valueOf(index), encodedTupel });
			index++;
		}
		operator.getParameters().setParameter(PARAMETER_META_DATA, ParameterTypeList.transformList2String(metaDataList));

		// annotations
		List<String[]> annotationList = new LinkedList<>();
		for (Entry<Integer, String> annotation : annotationsMap.entrySet()) {
			annotationList.add(new String[] { annotation.getKey().toString(), annotation.getValue() });
		}
		operator.setParameter(PARAMETER_ANNOTATIONS, ParameterTypeList.transformList2String(annotationList));

		operator.getParameters().setParameter(AbstractDataResultSetReader.PARAMETER_ERROR_TOLERANT,
				String.valueOf(isFaultTolerant()));
		operator.getParameters().setParameter(PARAMETER_FIRST_ROW_AS_NAMES, "false");
	}

	public ColumnMetaData getColumnMetaData(int col) {
		if (columnMetaData != null && col < columnMetaData.length) {
			return columnMetaData[col];
		} else {
			return null;
		}
	}

	/**
	 * This will return all indices of each selected column
	 */
	public int[] getSelectedIndices() {
		return IntStream.range(0, columnMetaData.length).
				filter(i -> columnMetaData[i].isSelected()).toArray();
	}

	/**
	 * This returns the annotation of a line or null if no present
	 */
	public String getAnnotation(int line) {
		return annotationsMap.get(line);
	}

	public SortedSet<Integer> getAnnotatedRowIndices() {
		return new TreeSet<>(annotationsMap.keySet());
	}

	public Map<Integer, String> getAnnotationsMap() {
		return annotationsMap;
	}

	/**
	 * Returns the row annotated to be used as the name of the attribute or -1 if no such row was
	 * selected.
	 */
	public int getNameRow() {
		for (Entry<Integer, String> entry : annotationsMap.entrySet()) {
			if (Annotations.ANNOTATION_NAME.equals(entry.getValue())) {
				return entry.getKey();
			}
		}
		return -1;
	}

	public int getNumerOfColumns() {
		return columnMetaData.length;
	}

	public ColumnMetaData[] getColumnMetaData() {
		return columnMetaData;
	}

	public void setFaultTolerant(boolean faultTolerant) {
		this.faultTolerant = faultTolerant;
	}

	public boolean isFaultTolerant() {
		return faultTolerant;
	}

	public int getLastAnnotatedRowIndex() {
		if (annotationsMap.isEmpty()) {
			return -1;
		}
		SortedSet<Integer> annotatedRows = getAnnotatedRowIndices();
		return annotatedRows.last();
	}

	public void resetValueTypes() {
		for (ColumnMetaData cmd : columnMetaData) {
			cmd.setAttributeValueType(Ontology.ATTRIBUTE_VALUE);
		}
	}

	public DateFormat getDateFormat() throws OperatorException {
		if (dateFormat == null) {
			this.dateFormat = ThreadLocal.withInitial(() -> {
				if (datePattern == null || datePattern.trim().isEmpty()) {
					// clone because getDateInstance uses an internal pool which can return the
					// same instance for multiple threads
					return (DateFormat) DateFormat.getDateTimeInstance().clone();
				} else {
					try {
						return ParameterTypeDateFormat.createCheckedDateFormat(getDatePattern(), locale);
					} catch (UserError userError) {
						throw new DateFormatRuntimeException(userError);
					}
				}
			});
		}
		try {
			return this.dateFormat.get();
		} catch (DateFormatRuntimeException dfre) {
			throw dfre.toOperatorException();
		}
	}

	public String getDatePattern() {
		return datePattern;
	}

	public void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
		dateFormat = null;
	}

	@Override
	public String toString() {
		return "Annotations: " + annotationsMap + "; columns: " + Arrays.toString(columnMetaData);
	}

	public void addColumnMetaData(ExampleSetMetaData emd) {
		MDInteger numberOfExamples = emd.getNumberOfExamples();
		numberOfExamples.subtract(annotationsMap.size());
		for (ColumnMetaData cmd : columnMetaData) {
			if (cmd.isSelected()) {
				emd.addAttribute(cmd.getAttributeMetaData());
			}
		}
	}

	/** Returns true if meta data is manually set. */
	public boolean isComplete() {
		return columnMetaData != null && columnMetaData.length > 0;
	}

	public void setNumberFormat(NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
	}

	public NumberFormat getNumberFormat() {
		return numberFormat;
	}

	public int getDataManagementType() {
		return dataManagementType;
	}

	/**
	 * @param readerOperator
	 *            the operator to retrieve the parameter from
	 */
	public static ColumnMetaData[] readColumnMetaData(AbstractDataResultSetReader readerOperator) {
		// initializing data structures
		List<String[]> metaDataSettings;
		if (readerOperator.isParameterSet(PARAMETER_META_DATA) && !readerOperator.getParameterAsBoolean(PARAMETER_READ_AS_POLYNOMINAL)) {
			try {
				metaDataSettings = readerOperator.getParameterList(PARAMETER_META_DATA);
			} catch (UndefinedParameterError e) {
				metaDataSettings = Collections.emptyList();
			}
		} else {
			metaDataSettings = Collections.emptyList();
		}

		// find largest used column index
		int maxUsedColumnIndex = -1;
		for (String[] metaDataDefinition : metaDataSettings) {
			int columnIndex = Integer.parseInt(metaDataDefinition[0]);
			maxUsedColumnIndex = Math.max(maxUsedColumnIndex, columnIndex);
		}
		// initialize with values from settings
		ColumnMetaData[] columnMetaData = new ColumnMetaData[maxUsedColumnIndex + 1];
		boolean trimAttributeNames = readerOperator.shouldTrimAttributeNames();
		for (String[] metaDataDefinition : metaDataSettings) {
			int currentColumn = Integer.parseInt(metaDataDefinition[0]);
			String[] metaDataDefintionValues = ParameterTypeTupel.transformString2Tupel(metaDataDefinition[1]);
			columnMetaData[currentColumn] = new ColumnMetaData(trimAttributeNames);
			final ColumnMetaData cmd = columnMetaData[currentColumn];
			cmd.setSelected(Boolean.parseBoolean(metaDataDefintionValues[1]));
			if (cmd.isSelected()) { // otherwise details don't matter
				cmd.setRole(metaDataDefintionValues[3].trim());
				cmd.setUserDefinedAttributeName(metaDataDefintionValues[0].trim());

				int valueType = Ontology.ATTRIBUTE_VALUE_TYPE.mapName(metaDataDefintionValues[2]);
				// fallback for old processes where attribute value type was saved as index
				// rather than as string
				if (valueType == -1) {
					cmd.setAttributeValueType(Integer.parseInt(metaDataDefintionValues[2]));
				} else {
					cmd.setAttributeValueType(valueType);
				}
			}
		}
		// replace those which were not specified in the list by an empty ColumnMetaData (so it
		// is at least not null)
		for (int i = 0; i < columnMetaData.length; i++) {
			if (columnMetaData[i] == null) {
				columnMetaData[i] = new ColumnMetaData(trimAttributeNames);
			}
		}
		return columnMetaData;
	}

	/**
	 * Indicates if values should be trimmed for type guessing
	 *
	 * @return {@code true} if values should be trimmed for type guessing
	 * @since 9.2.0
	 */
	public boolean trimForGuessing() {
		return trimForGuessing;
	}

	/**
	 * Indicates if values should be trimmed for type guessing
	 *
	 * @param trimForGuessing
	 * 		if parameters should be trimmed for guessing
	 * @since 9.2.0
	 */
	public void setTrimForGuessing(boolean trimForGuessing) {
		this.trimForGuessing = trimForGuessing;
	}

	/**
	 * Simple wrapper class for a {@link UserError}.
	 *
	 * @author Jan Czogalla
	 * @since 8.2
	 */
	private static class DateFormatRuntimeException extends IllegalArgumentException implements OperatorRuntimeException {

		private final UserError userError;

		private DateFormatRuntimeException(UserError userError) {
			this.userError = userError;
		}

		@Override
		public OperatorException toOperatorException() {
			return userError;
		}
	}
}
