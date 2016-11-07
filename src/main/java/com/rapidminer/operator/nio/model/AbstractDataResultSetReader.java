/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.io.AbstractDataReader.AttributeColumn;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.operator.io.ExampleSource;
import com.rapidminer.operator.nio.file.FileInputPortHandler;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.Port;
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
import com.rapidminer.parameter.PortProvider;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


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

	public static final String PARAMETER_DATE_FORMAT = "date_format";
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

		if (configComplete) {
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
	 * these options is chosen is determined by the parameter {@link #PARAMETER_DESTINATION_TYPE}.
	 * */
	public File getSelectedFile() throws OperatorException {
		return filePortHandler.getSelectedFile();
	}

	/**
	 * Same as {@link #getSelectedFile()}, but opens the stream.
	 * */
	public InputStream openSelectedFile() throws OperatorException, IOException {
		return filePortHandler.openSelectedFile();
	}

	/**
	 * Same as {@link #getSelectedFile()}, but returns true if file is specified (in the respective
	 * way).
	 * */
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
				"Name of the file to read the data from.", new PortProvider() {

					@Override
					public Port getPort() {
						return fileInputPort;
					}
				}, true, getFileExtensions());
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();

		if (isSupportingFirstRowAsNames()) {
			types.add(new ParameterTypeBoolean(
					PARAMETER_FIRST_ROW_AS_NAMES,
					"Indicates if the first row should be used for the attribute names. If activated no annotations can be used.",
					true, false));
		}

		List<String> annotations = new LinkedList<String>();
		annotations.add(ANNOTATION_NAME);
		annotations.addAll(Arrays.asList(Annotations.ALL_KEYS_ATTRIBUTE));
		ParameterType type = new ParameterTypeList(PARAMETER_ANNOTATIONS, "Maps row numbers to annotation names.", //
				new ParameterTypeInt("row_number", "Row number which contains an annotation", 0, Integer.MAX_VALUE), //
				new ParameterTypeCategory("annotation", "Name of the annotation to assign this row.",
						annotations.toArray(new String[annotations.size()]), 0), true);
		if (isSupportingFirstRowAsNames()) {
			type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_FIRST_ROW_AS_NAMES, false, false));
		}
		types.add(type);

		type = new ParameterTypeDateFormat(PARAMETER_DATE_FORMAT,
				"The parse format of the date values, for example \"yyyy/MM/dd\".", false);
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

		type = new ParameterTypeList(PARAMETER_META_DATA, "The meta data information", //
				new ParameterTypeInt(PARAMETER_COLUMN_INDEX, "The column index", 0, Integer.MAX_VALUE), //
				new ParameterTypeTupel(PARAMETER_COLUMN_META_DATA, "The meta data definition of one column", //
						new ParameterTypeString(PARAMETER_COLUMN_NAME, "Describes the attributes name."), //
						new ParameterTypeBoolean(PARAMETER_COLUMN_SELECTED, "Indicates if a column is selected", true), //
						new ParameterTypeCategory(PARAMETER_COLUMN_VALUE_TYPE, "Indicates the value type of an attribute",
								Ontology.VALUE_TYPE_NAMES, Ontology.NOMINAL), //
						new ParameterTypeStringCategory(PARAMETER_COLUMN_ROLE, "Indicates the role of an attribute",
								Attributes.KNOWN_ATTRIBUTE_TYPES, AttributeColumn.REGULAR)), true);

		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_ERROR_TOLERANT,
				"Values which does not match to the specified value typed are considered as missings.", true, true));

		types.add(new ParameterTypeCategory(ExampleSource.PARAMETER_DATAMANAGEMENT,
				"Determines, how the data is represented internally.", DataRowFactory.TYPE_NAMES,
				DataRowFactory.TYPE_DOUBLE_ARRAY, true));

		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] changes = super.getIncompatibleVersionChanges();
		changes = Arrays.copyOf(changes, changes.length + 1);
		changes[changes.length - 1] = DataResultSetTranslator.VERSION_6_0_3;
		return changes;
	}

}
