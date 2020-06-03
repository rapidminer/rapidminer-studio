/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.storage.hdf5;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SimpleExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.internal.ColumnarExampleTable;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.hdf5.BufferedInChannel;
import com.rapidminer.hdf5.file.ColumnInfo;
import com.rapidminer.hdf5.file.TableWriter;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MDNumber.Relation;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.storage.hdf5.HdfReaderException.Reason;
import com.rapidminer.tools.Ontology;

import io.jhdf.GlobalHeap;
import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;
import io.jhdf.api.Node;
import io.jhdf.api.dataset.ContiguousDataset;
import io.jhdf.dataset.DatasetReader;
import io.jhdf.dataset.VariableLengthDatasetReader;
import io.jhdf.exceptions.HdfException;
import io.jhdf.exceptions.HdfInvalidPathException;
import io.jhdf.object.datatype.DataType;
import io.jhdf.object.datatype.StringData;
import io.jhdf.object.datatype.VariableLength;


/**
 * A reader for hdf5 files containing a data table written in the format defined by the {@link TableWriter}. The file
 * must contain the attributes {@link TableWriter#ATTRIBUTE_COLUMNS} and {@link TableWriter#ATTRIBUTE_ROWS}. It must
 * have datasets {@code a0}, ..., {@code an} where n is specified by {@link TableWriter#ATTRIBUTE_COLUMNS}-1 and every
 * dataset must have the attribute {@link TableWriter#ATTRIBUTE_TYPE} and {@link TableWriter#ATTRIBUTE_NAME}.
 * <p>
 * Numeric data must be of type {@link com.rapidminer.hdf5.file.ColumnInfo.ColumnType#REAL} or
 * {@link com.rapidminer.hdf5.file.ColumnInfo.ColumnType#INTEGER} and can be {@code double}, {@code float}, {@code
 * int} or {@code long} values, where for long values, {@link Long#MAX_VALUE} is transformed to {@link Double#NaN} and
 * other values might loose precision.
 * <p>
 * String data must be of type {@link com.rapidminer.hdf5.file.ColumnInfo.ColumnType#NOMINAL}. If the string dataset
 * has the attribute {@link TableWriter#ATTRIBUTE_DICTIONARY}, the attribute must either contain a String array with
 * the dictionary or a reference to a String array dataset with the dictionary. The first dictionary entry stands for
 * the missing value. The data consists of a {@code byte}, {@code short} or {@link int} array containing the category
 * indices fitting to the dictionary. If the string dataset does not have the attribute
 * {@link TableWriter#ATTRIBUTE_DICTIONARY}, it must have the attribute {@link TableWriter#ATTRIBUTE_MISSING} which
 * specifies a String representing the missing value. The dataset is then an array of String values.
 * All String arrays can be either variable length or fixed length Strings.
 * <p>
 * Date-time data must be of type {@link com.rapidminer.hdf5.file.ColumnInfo.ColumnType#DATE_TIME} and the dataset
 * must be an array of {@code long} values specifying seconds since 1970. Additionally, the dataset can have the
 * attribute {@link TableWriter#ATTRIBUTE_ADDITIONAL} which contains a reference to a dataset containing additional
 * nanoseconds.
 * <p>
 * Statistics can be included for every column, see {@link StatisticsHandler}. The statistics is only read if the
 * attribute {@link ExampleSetHdf5Writer#ATTRIBUTE_HAS_STATISTICS} exists and is {@code 1}.
 *
 * @author Gisa Meier
 * @since 9.7.0
 */
public enum Hdf5ExampleSetReader {

	;//No-instance enum, only static methods

	private static final String DATASET_PATH_PREFIX = "/" + TableWriter.COLUMN_LINK_PREFIX;

	/**
	 * Reads an {@link ExampleSet} from the hdf5 file at the given path. See the class javadoc for the admissible
	 * formats.
	 *
	 * @param path
	 * 		the path to read from
	 * @return the example set read from the path
	 * @throws IOException
	 * 		if reading fails
	 * @throws HdfReaderException
	 * 		in case the content of the hdf5 file does not match the admissible format
	 */
	public static ExampleSet read(Path path) throws IOException {
		try (HdfFile hdfFile = new HdfFile(path)) {
			if (isMetadata(hdfFile)) {
				throw new HdfReaderException(Reason.IS_META_DATA, "File only contains meta data");
			}

			int numberOfRows = getNonnegativeIntAttribute(hdfFile, TableWriter.ATTRIBUTE_ROWS);
			int numberOfColumns = getNonnegativeIntAttribute(hdfFile, TableWriter.ATTRIBUTE_COLUMNS);

			List<Attribute> attributes = new ArrayList<>(numberOfColumns);
			Map<Attribute, String> roles = new LinkedHashMap<>();
			List<Dataset> sets = new ArrayList<>(numberOfColumns);
			try (BufferedInChannel inChannel =
						 new BufferedInChannel(hdfFile.getHdfChannel().getFileChannel(), 1 << 16)) {
				Map<Long, GlobalHeap> heaps = new HashMap<>();
				Hdf5MappingReader mappingReader = new Hdf5MappingReader(hdfFile, inChannel, heaps);
				for (int i = 0; i < numberOfColumns; i++) {
					Dataset set = getDatasetOrException(hdfFile, i);
					sets.add(set);
					Attribute attribute = createAttribute(set, AttributeFactory::createAttribute);
					mappingReader.addMapping(set, attribute);
					attributes.add(attribute);
					addRole(set, value -> roles.put(attribute, value));
				}

				ExampleSet set = createExampleSet(numberOfRows, attributes, roles);
				addAnnotations(hdfFile, set.getAnnotations());
				if (numberOfRows > 0) {
					ColumnarExampleTable table = (ColumnarExampleTable) set.getExampleTable();
					new Hdf5DatasetReader(table, hdfFile, inChannel, heaps).fillTable(attributes, sets);
				}
				return set;
			}
		} catch (HdfException e) {
			throw new HdfReaderException(HdfReaderException.Reason.INCONSISTENT_FILE, e.getMessage());
		}
	}

	/**
	 * Reads an {@link ExampleSetMetaData} from the hdf5 file at the given path. See the class javadoc for the
	 * admissible formats. Ignores missing hdf5 statistics attributes or their wrong formats but fails on violations
	 * of other parts of the admissible formats.
	 *
	 * @param path
	 * 		the path to read from
	 * @return the example set metadata read from the path, or {@code null} if the {@link
	 *            ExampleSetHdf5Writer#ATTRIBUTE_HAS_STATISTICS} attribute is not 1
	 * @throws IOException
	 * 		if reading fails
	 * @throws HdfReaderException
	 * 		in case the content of the hdf5 file does not match the admissible format, this means it will also not be
	 * 		possible to read the {@link ExampleSet} and create the meta data from it
	 */
	public static ExampleSetMetaData readMetaData(Path path) throws IOException {
		return readMetaData(path, true);
	}

	/**
	 * Reads an {@link ExampleSetMetaData} from the hdf5 file at the given path. See the class javadoc for the
	 * admissible formats. Ignores missing hdf5 statistics attributes or their wrong formats but fails on violations
	 * of other parts of the admissible formats.
	 *
	 * @param path
	 * 		the path to read from
	 * @param statsMandatory
	 * 		whether the statistics must be part of the file to be read
	 * @return the example set metadata read from the path, or {@code null} if the {@link
	 *            ExampleSetHdf5Writer#ATTRIBUTE_HAS_STATISTICS} attribute is not 1
	 * @throws IOException
	 * 		if reading fails
	 * @throws HdfReaderException
	 * 		in case the content of the hdf5 file does not match the admissible format, this means it will also not be
	 * 		possible to read the {@link ExampleSet} and create the meta data from it
	 */
	public static ExampleSetMetaData readMetaData(Path path, boolean statsMandatory) throws IOException {
		try (HdfFile hdfFile = new HdfFile(path)) {
			Number statsValue = getSingleAttributeValueOrNull(hdfFile, ExampleSetHdf5Writer.ATTRIBUTE_HAS_STATISTICS, Number.class);
			if (statsMandatory && (statsValue == null || statsValue.byteValue() != 1)) {
				return null;
			}
			boolean isMetaData = isMetadata(hdfFile);

			ExampleSetMetaData metaData = new ExampleSetMetaData();
			List<AttributeMetaData> nominalMetaData = new ArrayList<>();
			List<Dataset> nominalDatasets = new ArrayList<>();

			readNumberOfRows(hdfFile, metaData, isMetaData);
			readColumns(hdfFile, metaData, nominalMetaData, nominalDatasets, isMetaData);
			addAnnotations(hdfFile, metaData.getAnnotations());
			readMappings(hdfFile, nominalMetaData, nominalDatasets, isMetaData);

			return metaData;

		} catch (HdfException e) {
			throw new HdfReaderException(HdfReaderException.Reason.INCONSISTENT_FILE, e.getMessage());
		}
	}

	/** Reads the number of rows from the {@link HdfFile}. If {@code isMetaData} is {@code true}, also reads the {@link Relation} */
	private static void readNumberOfRows(HdfFile hdfFile, ExampleSetMetaData metaData, boolean isMetaData) {
		int numberOfRows = getNonnegativeIntAttribute(hdfFile, TableWriter.ATTRIBUTE_ROWS);
		metaData.setNumberOfExamples(numberOfRows);
		if (isMetaData) {
			MDInteger nEx = metaData.getNumberOfExamples();
			switch (Relation.fromDescription(getSingleAttributeValueOrNull(hdfFile,
					ExampleSetHdf5Writer.ATTRIBUTE_ROW_RELATION, String.class))) {
				case UNKNOWN:
					nEx.setUnkown();
					break;
				case AT_MOST:
					nEx.reduceByUnknownAmount();
					break;
				case AT_LEAST:
					nEx.increaseByUnknownAmount();
					break;
				case EQUAL:
				default:
					// noop
			}
			metaData.mergeSetRelation(getSetRelation(hdfFile));
		}
	}

	/**
	 * Reads the columns from the {@link HdfFile} and adds the information as an {@link AttributeMetaData} to the
	 * {@code metaData}. Also collects information about additional nominal information in {@code nominalMetaData} and
	 * {@code nominalDatasets}. If {@code isMetaData} is {@code true}, also reads the {@link SetRelation} for each column.
	 */
	private static void readColumns(HdfFile hdfFile, ExampleSetMetaData metaData,
									List<AttributeMetaData> nominalMetaData, List<Dataset> nominalDatasets,
									boolean isMetaData) {
		int numberOfColumns = getNonnegativeIntAttribute(hdfFile, TableWriter.ATTRIBUTE_COLUMNS);
		for (int i = 0; i < numberOfColumns; i++) {
			Dataset set = getDatasetOrException(hdfFile, i);

			AttributeMetaData attribute = createAttribute(set, AttributeMetaData::new);
			if (attribute.isNominal()) {
				nominalMetaData.add(attribute);
				nominalDatasets.add(set);
			}
			if (isMetaData) {
				attribute.setValueSetRelation(getSetRelation(set));
				Number wasShrunk = getSingleAttributeValueOrNull(set, ExampleSetHdf5Writer.ATTRIBUTE_NOMINAL_SHRUNK, Number.class);
				if (wasShrunk != null && wasShrunk.byteValue() == 1) {
					attribute.valueSetIsShrunk(true);
				}
			}
			addRole(set, attribute::setRole);
			StatisticsHandler.readStatistics(set, attribute);
			metaData.addAttribute(attribute);
		}
	}

	/** Reads the nominal mappings of the given nominal columns and adds the mapping to the corresponding {@link AttributeMetaData} */
	private static void readMappings(HdfFile hdfFile, List<AttributeMetaData> nominalMetaData, List<Dataset> nominalDatasets, boolean isMetaData) throws IOException {
		if (!nominalMetaData.isEmpty()) {
			int limit = AttributeMetaData.getMaximumNumberOfNominalValues();
			try (BufferedInChannel inChannel
						 = new BufferedInChannel(hdfFile.getHdfChannel().getFileChannel(), 1 << 16)) {
				Hdf5MappingReader mappingReader = new Hdf5MappingReader(hdfFile, inChannel, new HashMap<>());
				int index = 0;
				for (AttributeMetaData attribute : nominalMetaData) {
					Dataset set = nominalDatasets.get(index++);
					int mode = StatisticsHandler.readModeIndex(set);
					SetRelation oldRelation = isMetaData ? attribute.getValueSetRelation() : SetRelation.EQUAL;
					mappingReader.addMapping(set, attribute, mode, limit);
					attribute.setValueSetRelation(oldRelation.merge(attribute.getValueSetRelation()));
				}
			}
		}
	}

	/**
	 * Reads a root hdf5 attribute with attributeName from the hdfFile. Ensures that the value is a non-negative
	 * number.
	 */
	private static int getNonnegativeIntAttribute(HdfFile hdfFile, String attributeName) {
		io.jhdf.api.Attribute attribute = hdfFile.getAttribute(attributeName);
		if (attribute == null) {
			throw new HdfReaderException(HdfReaderException.Reason.MISSING_ATTRIBUTE, attributeName + " attribute " +
					"missing");
		}
		if (!Number.class.isAssignableFrom(attribute.getJavaType()) || attribute.getDimensions().length > 0) {
			throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE, attributeName +
					" attribute must have number value");
		}
		int number = ((Number) attribute.getData()).intValue();
		if (number < 0) {
			throw new HdfReaderException(HdfReaderException.Reason.INCONSISTENT_FILE, "negative " + attributeName);
		}
		return number;
	}

	/**
	 * Reads the {@link SetRelation} from the given node (either {@link HdfFile} or
	 * {@link io.jhdf.api.Attribute HDF Attribute}). If the attribute cannot be found or is an invalid value, returns
	 * {@link SetRelation#UNKNOWN}.
	 *
	 * @see SetRelation#fromDescription(String)
	 */
	private static SetRelation getSetRelation(Node node) {
		return SetRelation.fromDescription(getSingleAttributeValueOrNull(node,
				ExampleSetHdf5Writer.ATTRIBUTE_SET_RELATION, String.class));
	}

	/**
	 * Reads a single attribute value from the given node (either {@link HdfFile} or
	 * {@link io.jhdf.api.Attribute HDF Attribute}) and the specified attribute (by name). Checks if the value is
	 * actually compatible with the expected {@link Class}. If at any point there is a problem,
	 * this will return {@code null}.
	 *
	 * @param node
	 * 		the node to read the attribtue from
	 * @param attributeName
	 * 		the attribute name to find
	 * @param clazz
	 * 		the expected java type of the attribute
	 * @param <T>
	 * 		the expected java type of the attribute
	 * @return the attribute's value or {@code null} if any problem occurs
	 */
	private static<T> T getSingleAttributeValueOrNull(Node node, String attributeName, Class<T> clazz) {
		io.jhdf.api.Attribute attribute = node.getAttribute(attributeName);
		if (attribute == null) {
			return null;
		}
		if (!clazz.isAssignableFrom(attribute.getJavaType()) || attribute.getDimensions().length > 0) {
			return null;
		}
		return clazz.cast(attribute.getData());
	}

	/**
	 * Reads the dataset for the given datasetIndex from the hdfFile or handles the occurring exception.
	 */
	private static Dataset getDatasetOrException(HdfFile hdfFile, int datasetIndex) {
		try {
			return hdfFile.getDatasetByPath(DATASET_PATH_PREFIX + datasetIndex);
		} catch (HdfInvalidPathException e) {
			throw new HdfReaderException(HdfReaderException.Reason.INCONSISTENT_FILE,
					"dataset number " + datasetIndex + " not found");
		}
	}

	/**
	 * Adds the role derived from the set via the valueHandler.
	 *
	 * @param set
	 * 		the dataset with the required attributes
	 * @param valueHandler
	 * 		the value handler, only two valueHandlers are possible
	 */
	private static void addRole(Dataset set, Consumer<String> valueHandler) {
		io.jhdf.api.Attribute role = set.getAttribute(TableWriter.ATTRIBUTE_ROLE);
		if (role != null) {
			io.jhdf.api.Attribute legacyRole =
					set.getAttribute(ExampleSetHdf5Writer.ATTRIBUTE_LEGACY_ROLE);
			if (legacyRole != null) {
				if (!String.class.equals(legacyRole.getJavaType()) || legacyRole.getDimensions().length > 0) {
					throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE, "attribute legacy role must" +
							" have a String value for " + set.getPath());
				}
				String value = (String) legacyRole.getData();
				valueHandler.accept(value);
			} else {
				if (!String.class.equals(role.getJavaType()) || role.getDimensions().length > 0) {
					throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE, "attribute role must have a" +
							" String value for " + set.getPath());
				}
				String value = ((String) role.getData()).toLowerCase();
				valueHandler.accept(value);
			}
		}
	}


	/**
	 * Creates an empty example set with the given properties.
	 */
	private static ExampleSet createExampleSet(int numberOfRows, List<Attribute> attributes,
											   Map<Attribute, String> roles) {
		//cannot use ExampleSets.from here because that does not guarantee a ColumnarExampleTable
		ColumnarExampleTable table = new ColumnarExampleTable(attributes, ExampleSetBuilder.DataManagement.AUTO, true);
		table.setExpectedSize(numberOfRows);
		if (numberOfRows > 0) {
			table.addBlankRows(numberOfRows);
		}
		ExampleSet set = new SimpleExampleSet(table, attributes, null);
		Attributes simpleAttributes = set.getAttributes();
		roles.forEach(simpleAttributes::setSpecialAttribute);
		return set;
	}

	/**
	 * Creates an {@link Attribute} or {@link AttributeMetaData} from ontology and name stored in the set.
	 *
	 * @param set
	 * 		the set with type and name attributes
	 * @param creator
	 * 		a creator for attribute or attribute meta data from ontology and name
	 * @param <T>
	 * 		the type of the created object
	 * @return the attribute or attribute meta data
	 */
	private static <T> T createAttribute(Dataset set, BiFunction<String, Integer, T> creator) {
		io.jhdf.api.Attribute type = set.getAttribute(TableWriter.ATTRIBUTE_TYPE);
		io.jhdf.api.Attribute name = set.getAttribute(TableWriter.ATTRIBUTE_NAME);
		if (type == null) {
			throw new HdfReaderException(HdfReaderException.Reason.MISSING_ATTRIBUTE,
					TableWriter.ATTRIBUTE_TYPE + " attribute missing for " + set.getPath());
		}
		if (name == null) {
			throw new HdfReaderException(HdfReaderException.Reason.MISSING_ATTRIBUTE,
					TableWriter.ATTRIBUTE_NAME + " attribute missing for " + set.getPath());
		}
		Object nameData = name.getData();
		if (!(nameData instanceof String)) {
			throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE, TableWriter.ATTRIBUTE_NAME +
					" attribute must have String value");
		}

		io.jhdf.api.Attribute ontology = set.getAttribute(ExampleSetHdf5Writer.ATTRIBUTE_LEGACY_TYPE);
		if (ontology != null) {
			Object ontologyData = ontology.getData();
			if (!(ontologyData instanceof Number)) {
				throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE,
						ExampleSetHdf5Writer.ATTRIBUTE_LEGACY_TYPE + " attribute must have a number value");
			}
			return creator.apply((String) nameData, ((Number) ontologyData).intValue());
		} else {
			Object typeData = type.getData();
			if (!(typeData instanceof String)) {
				throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE, TableWriter.ATTRIBUTE_TYPE +
						" attribute must have String value");
			}
			int ontologyType = makeOntologyForType((String) nameData, (String) typeData);
			return creator.apply((String) nameData, ontologyType);
		}
	}

	/**
	 * Adds annotations if they are present.
	 */
	private static void addAnnotations(HdfFile hdfFile, Annotations annotations) {
		try {
			Dataset annotationsSet = hdfFile.getDatasetByPath("/" + TableWriter.ANNOTATIONS);
			if (annotationsSet == null) {
				// ignore invalid annotations
				return;
			}
			if (!(annotationsSet instanceof ContiguousDataset)) {
				// ignore invalid annotations
				return;
			}

			//cannot use annotationsSet.getData() since this uses FileChannel#map which locks the file indefinitely
			Object rawAnnotations = null;
			DataType type = annotationsSet.getDataType();
			long annotationSize = annotationsSet.getSize();
			int[] annotationDim = annotationsSet.getDimensions();
			ByteBuffer bb =
					hdfFile.getHdfChannel().readBufferFromAddress(((ContiguousDataset) annotationsSet).getDataAddress() + hdfFile.getUserBlockSize(),
					(int) annotationSize * type.getSize());
			if (type instanceof VariableLength) {
				rawAnnotations = VariableLengthDatasetReader.readDataset((VariableLength) type,
						bb, annotationDim, hdfFile.getHdfChannel());
			} else if (type instanceof StringData) {
				rawAnnotations = DatasetReader.readDataset(type, bb, annotationDim, hdfFile.getHdfChannel());
			}

			// ignore invalid annotations
			if (rawAnnotations instanceof String[][]) {
				String[][] data = (String[][]) rawAnnotations;
				for (String[] datum : data) {
					if (datum != null && datum.length > 1) {
						annotations.put(datum[0], datum[1]);
					}
				}
			}


		} catch (HdfInvalidPathException e) {
			//annotations set does not exist, ignore
		}
	}

	/**
	 * Converts the type to {@link Ontology} and creates an {@link Attribute} with the given name.
	 */
	private static int makeOntologyForType(String name, String type) {
		int ontology;
		ColumnInfo.ColumnType cType = ColumnInfo.ColumnType.fromString(type);
		if (cType == null) {
			throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE, type + " not allowed as type for " +
					"attribute " + name);
		}
		switch (cType) {
			case REAL:
				ontology = Ontology.REAL;
				break;
			case INTEGER:
				ontology = Ontology.INTEGER;
				break;
			case DATE_TIME:
				ontology = Ontology.DATE_TIME;
				break;
			case TIME:
				ontology = Ontology.INTEGER;
				break;
			case NOMINAL:
			default:
				ontology = Ontology.NOMINAL;
				break;
		}
		return ontology;
	}

	/**
	 * Checks whether {@link ExampleSetHdf5Writer#ATTRIBUTE_IS_METADATA} attribute is present and set to 1.
	 */
	private static boolean isMetadata(HdfFile hdfFile) {
		Number mdValue = getSingleAttributeValueOrNull(hdfFile, ExampleSetHdf5Writer.ATTRIBUTE_IS_METADATA, Number.class);
		return mdValue != null && mdValue.byteValue() == 1;
	}

}