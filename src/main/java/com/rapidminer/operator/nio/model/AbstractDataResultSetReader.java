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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.operator.nio.file.FileInputPortHandler;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.preprocessing.filter.AbstractDateDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.parameter.internal.DataManagementParameterHelper;


/**
 * This class uses DataResultSets to load data from file and then delivers the data as an example
 * set.
 *
 * @author Sebastian Land
 */
public abstract class AbstractDataResultSetReader extends AbstractExampleSource {

	/** Pseudo-annotation to be used for attribute names. */
	public static final String ANNOTATION_NAME = "Name";

	/**
	 * This parameter holds the hole information about the attribute columns. I.e. which attributes
	 * are defined, the names, what value type they have, whether the att. is selected,
	 */
	public static final String PARAMETER_META_DATA = "data_set_meta_data_information";

	/**
	 * Parameters being part of the list for PARAMETER_META_DATA
	 */
	public static final String PARAMETER_COLUMN_INDEX = "column_index";
	public static final String PARAMETER_COLUMN_META_DATA = "attribute_meta_data_information";
	public static final String PARAMETER_COLUMN_NAME = "attribute name";
	public static final String PARAMETER_COLUMN_SELECTED = "column_selected";
	public static final String PARAMETER_COLUMN_VALUE_TYPE = "attribute_value_type";
	public static final String PARAMETER_COLUMN_ROLE = "attribute_role";
	public static final String PARAMETER_READ_AS_POLYNOMINAL = "read_all_values_as_polynominal";

	/**
	 * @deprecated since 8.2; use {@link ParameterTypeDateFormat#PARAMETER_DATE_FORMAT} instead.
	 */
	@Deprecated
	public static final String PARAMETER_DATE_FORMAT = ParameterTypeDateFormat.PARAMETER_DATE_FORMAT;
	public static final String PARAMETER_TIME_ZONE = "time_zone";
	public static final String PARAMETER_LOCALE = "locale";

	/**
	 * The parameter name for &quot;Determines, how the data is represented internally.&quot;
	 */
	public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";
	public static final String PARAMETER_FIRST_ROW_AS_NAMES = "first_row_as_names";
	public static final String PARAMETER_ANNOTATIONS = "annotations";

	public static final String PARAMETER_ERROR_TOLERANT = "read_not_matching_values_as_missings";

	private InputPort fileInputPort = getInputPorts().createPort("file");
	private FileInputPortHandler filePortHandler = new FileInputPortHandler(this, fileInputPort, this.getFileParameterName());

	public AbstractDataResultSetReader(OperatorDescription description) {
		super(description);
		fileInputPort.addPrecondition(new SimplePrecondition(fileInputPort, new MetaData(FileObject.class)) {

			@Override
			protected boolean isMandatory() {
				return false;
			}
		});
	}

	public InputPort getFileInputPort() {
		return fileInputPort;
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		// check if date format is valid
		int localeIndex = getParameterAsInt(PARAMETER_LOCALE);
		Locale selectedLocale = Locale.US;
		if (localeIndex >= 0 && localeIndex < AbstractDateDataProcessing.availableLocales.size()) {
			selectedLocale = AbstractDateDataProcessing.availableLocales.get(localeIndex);
		}
		ParameterTypeDateFormat.createCheckedDateFormat(this, selectedLocale, false);

		// loading data result set
		final ExampleSet exampleSet;
		try (DataResultSetFactory dataResultSetFactory = getDataResultSetFactory();
				DataResultSet dataResultSet = dataResultSetFactory.makeDataResultSet(this)) {
			exampleSet = transformDataResultSet(dataResultSet);
		}
		if (fileInputPort.isConnected()) {
			IOObject fileObject = fileInputPort.getDataOrNull(IOObject.class);
			if (fileObject != null) {
				String sourceAnnotation = fileObject.getAnnotations().getAnnotation(Annotations.KEY_SOURCE);
				if (sourceAnnotation != null) {
					exampleSet.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, sourceAnnotation);
				}
			}
		}
		return exampleSet;
	}

	/**
	 *
	 * Transforms the provided {@link DataResultSet} into an example set.
	 *
	 * @param dataResultSet
	 *            the data result set to transform into an example set
	 * @return the generated example set
	 * @throws OperatorException
	 *             in case something goes wrong
	 */
	protected ExampleSet transformDataResultSet(DataResultSet dataResultSet) throws OperatorException {

		// loading configuration
		DataResultSetTranslationConfiguration configuration = new DataResultSetTranslationConfiguration(this);
		final boolean configComplete = !configuration.isComplete();
		if (configComplete) {
			configuration.reconfigure(dataResultSet);
		}

		// now use translator to read, translate and return example set
		DataResultSetTranslator translator = new DataResultSetTranslator(this);
		NumberFormat numberFormat = getNumberFormat();
		if (numberFormat != null) {
			configuration.setNumberFormat(numberFormat);
		}

		if (configComplete && getParameterAsBoolean(PARAMETER_READ_AS_POLYNOMINAL)) {
			for (ColumnMetaData metaData : configuration.getColumnMetaData()) {
				metaData.setAttributeValueType(Ontology.POLYNOMINAL);
			}
		} else if (configComplete) {
			translator.guessValueTypes(configuration, dataResultSet, null);
		}

		return translator.read(dataResultSet, configuration, false, null);
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		try (DataResultSetFactory dataResultSetFactory = getDataResultSetFactory()) {
			ExampleSetMetaData result = dataResultSetFactory.makeMetaData();
			DataResultSetTranslationConfiguration configuration = new DataResultSetTranslationConfiguration(this);
			configuration.addColumnMetaData(result);
			return result;
		}
	}

	/** @return {@code true} */
	@Override
	protected boolean isMetaDataCacheable() {
		return true;
	}

	/**
	 * Must be implemented by subclasses to return the DataResultSet.
	 */
	protected abstract DataResultSetFactory getDataResultSetFactory() throws OperatorException;

	/**
	 * Returns the configured number format or null if a default number format should be used.
	 */
	protected abstract NumberFormat getNumberFormat() throws OperatorException;

	/**
	 * This method might be overwritten by subclasses to avoid that the first row might be
	 * misinterpreted as attribute names.
	 */
	protected boolean isSupportingFirstRowAsNames() {
		return true;
	}

	/**
	 * Returns either the selected file referenced by the value of the parameter with the name
	 * {@link #getFileParameterName()} or the file delivered at {@link #fileInputPort}. Which of
	 * these options is chosen is determined by the parameter {@link com.rapidminer.operator.nio.file.WriteFileOperator#PARAMETER_DESTINATION_TYPE}.
	 */
	public File getSelectedFile() throws OperatorException {
		return filePortHandler.getSelectedFile();
	}

	/**
	 * Same as {@link #getSelectedFile()}, but opens the stream.
	 */
	public InputStream openSelectedFile() throws OperatorException, IOException {
		return filePortHandler.openSelectedFile();
	}

	/**
	 * Same as {@link #getSelectedFile()}, but returns true if file is specified (in the respective
	 * way).
	 */
	public boolean isFileSpecified() {
		return filePortHandler.isFileSpecified();
	}

	/**
	 * Returns the name of the {@link ParameterTypeFile} to be added through which the user can
	 * specify the file name.
	 */
	protected abstract String getFileParameterName();

	/** Returns the allowed file extension. */
	protected abstract String getFileExtension();

	/** Returns the allowed file extensions. */
	protected String[] getFileExtensions() {
		return new String[] { getFileExtension() };
	}

	/**
	 * Creates (but does not add) the file parameter named by {@link #getFileParameterName()} that
	 * depends on whether or not {@link #fileInputPort} is connected.
	 */
	protected ParameterType makeFileParameterType() {
		return FileInputPortHandler.makeFileParameterType(this, getFileParameterName(),
				"Name of the file to read the data from.", () -> fileInputPort, true, getFileExtensions());
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<>();

		if (isSupportingFirstRowAsNames()) {
			types.add(new ParameterTypeBoolean(PARAMETER_FIRST_ROW_AS_NAMES,
					"Indicates if the first row should be used for the attribute names. If activated no annotations can be used.",
					true, false));
		}

		List<String> annotations = new LinkedList<>();
		annotations.add(ANNOTATION_NAME);
		annotations.addAll(Arrays.asList(Annotations.ALL_KEYS_ATTRIBUTE));
		ParameterType type = new ParameterTypeList(PARAMETER_ANNOTATIONS, "Maps row numbers to annotation names.", //
				new ParameterTypeInt("row_number", "Row number which contains an annotation", 0, Integer.MAX_VALUE), //
				new ParameterTypeCategory("annotation", "Name of the annotation to assign this row.",
						annotations.toArray(new String[0]), 0), true);
		if (isSupportingFirstRowAsNames()) {
			type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_FIRST_ROW_AS_NAMES, false, false));
		}
		types.add(type);

		type = new ParameterTypeDateFormat();
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_TIME_ZONE,
				"The time zone used for the date objects if not specified in the date string itself.",
				Tools.getAllTimeZones(), Tools.getPreferredTimeZoneIndex());
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_LOCALE,
				"The used locale for date texts, for example \"Wed\" (English) in contrast to \"Mi\" (German).",
				AbstractDateDataProcessing.availableLocaleNames, AbstractDateDataProcessing.defaultLocale);
		types.add(type);

		types.addAll(super.getParameterTypes());

		types.add(new ParameterTypeBoolean(PARAMETER_READ_AS_POLYNOMINAL,
				"Type guessing and manual meta data handling is disabled and everything is read as polynominal.", false, true));
		ParameterCondition dependsOnGuessValue = new BooleanParameterCondition(this, PARAMETER_READ_AS_POLYNOMINAL, false, false);

		type = new ParameterTypeList(PARAMETER_META_DATA, "The meta data information", //
				new ParameterTypeInt(PARAMETER_COLUMN_INDEX, "The column index", 0, Integer.MAX_VALUE), //
				new ParameterTypeTupel(PARAMETER_COLUMN_META_DATA, "The meta data definition of one column", //
						new ParameterTypeString(PARAMETER_COLUMN_NAME, "Describes the attributes name."), //
						new ParameterTypeBoolean(PARAMETER_COLUMN_SELECTED, "Indicates if a column is selected", true), //
						new ParameterTypeCategory(PARAMETER_COLUMN_VALUE_TYPE, "Indicates the value type of an attribute",
								Ontology.VALUE_TYPE_NAMES, Ontology.NOMINAL), //
						new ParameterTypeStringCategory(PARAMETER_COLUMN_ROLE, "Indicates the role of an attribute",
								Attributes.KNOWN_ATTRIBUTE_TYPES, Attributes.KNOWN_ATTRIBUTE_TYPES[0])),
				true);

		type.registerDependencyCondition(dependsOnGuessValue);

		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_ERROR_TOLERANT,
				"Values which does not match to the specified value typed are considered as missings.", true, true);
		type.registerDependencyCondition(dependsOnGuessValue);
		types.add(type);

		DataManagementParameterHelper.addParameterTypes(types, this);
		return types;
	}

	/**
	 * @return whether attribute names should be trimmed when parsing or not.
	 * @since 8.1.1
	 */
	public boolean shouldTrimAttributeNames() {
		return getCompatibilityLevel().isAbove(DataResultSetTranslator.BEFORE_ATTRIBUTE_TRIMMING);
	}

	/**
	 * Whether values should be trimmed for guessing
	 *
	 * @return if this operator requires trimming for guessing
	 * @since 9.2.0
	 */
	public boolean trimForGuessing() {
		return false;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] changes = super.getIncompatibleVersionChanges();
		changes = Arrays.copyOf(changes, changes.length + 2);
		changes[changes.length - 2] = DataResultSetTranslator.VERSION_6_0_3;
		changes[changes.length - 1] = DataResultSetTranslator.BEFORE_ATTRIBUTE_TRIMMING;
		return changes;
	}

	/**
	 * Configures the operator with the specified {@link DataSource}. Will throw {@link UnsupportedOperationException}
	 * unless overwritten by subclasses.
	 *
	 * @param dataSource
	 * 		the datasource
	 * @throws DataSetException
	 * 		if something goes wrong during configuration
	 * @since 9.0.0
	 */
	public void configure(DataSource dataSource) throws DataSetException {
		// reader not supported if this method is not overridden in subclass
		throw new UnsupportedOperationException("This reader is not (yet) supported by the data import wizards.");
	}

}
