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

import static com.rapidminer.storage.hdf5.Hdf5DatasetReader.getDatasetByAddress;
import static com.rapidminer.storage.hdf5.Hdf5DatasetReader.toDataAddress;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.apache.commons.lang3.mutable.MutableInt;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.hdf5.BufferedInChannel;
import com.rapidminer.hdf5.file.TableWriter;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;

import io.jhdf.GlobalHeap;
import io.jhdf.HdfFile;
import io.jhdf.HdfFileChannel;
import io.jhdf.Utils;
import io.jhdf.api.Dataset;
import io.jhdf.api.dataset.ContiguousDataset;
import io.jhdf.object.datatype.StringData;
import io.jhdf.object.datatype.VariableLength;


/**
 * Reader for {@link NominalMapping}s written into hdf5 files.
 *
 * @author Gisa Meier
 * @see Hdf5ExampleSetReader
 * @since 9.7.0
 */
class Hdf5MappingReader {

	private static final Logger LOGGER = Logger.getLogger(Hdf5ExampleSetReader.class.getName());

	private final HdfFile hdfFile;
	private final BufferedInChannel inChannel;
	private final Map<Long, GlobalHeap> heaps;

	Hdf5MappingReader(HdfFile hdfFile, BufferedInChannel inChannel, Map<Long, GlobalHeap> heaps) {
		this.hdfFile = hdfFile;
		this.inChannel = inChannel;
		this.heaps = heaps;
	}

	/**
	 * Adds the mapping to the attribute if it is present for the set and the attribute is nominal.
	 *
	 * @param set
	 * 		the set to check if it contains an hdf5 attribute with information about the mapping
	 * @param attribute
	 * 		the attribute to add the mapping to
	 * @throws HdfReaderException
	 * 		if the data sets are not contiguous or attributes do not exists or are of wrong type
	 * @throws IOException
	 * 		if reading fails
	 */
	void addMapping(Dataset set, Attribute attribute) throws IOException {
		if (attribute.isNominal()) {
			io.jhdf.api.Attribute mapping = set.getAttribute(TableWriter.ATTRIBUTE_DICTIONARY);
			if (mapping != null) {
				if (mapping.getJavaType().equals(String.class)) {
					//only small mapping directly in attribute -> fine to read directly
					String[] mappingArray = (String[]) mapping.getData();
					readArrayIntoMapping(attribute, mappingArray);
				} else {
					NominalMapping attributeMapping = attribute.getMapping();
					readMappingFromDataset(mapping, set, value -> addNominalValue(attribute, attributeMapping, value),-1);
				}
			} else if (!set.getJavaType().equals(String.class)) {
				throw new HdfReaderException(HdfReaderException.Reason.INCONSISTENT_FILE, "dictionary attribute " +
						"missing for category indices dataset " + set.getPath());
			}
		}
	}

	/**
	 * Reads the first dictionary values or the first values of the column into the meta data. Tries to convert the
	 * modeIndex to a String value by finding the dictionary value with this index or the column value of this row.
	 *
	 * @param set
	 * 		the set with the dictionary information or String values
	 * @param attributeMetaData
	 * 		the meta data to store the dictionary and mode values in
	 * @param modeIndex
	 * 		the index of the mode value, either a category index if a dictionary is present or a row index
	 * @param limit
	 * 		the maximal number of dictionary values to read
	 * @throws HdfReaderException
	 * 		if the data sets are not contiguous or dictionary attributes are of wrong type
	 * @throws IOException
	 * 		if reading fails
	 */
	void addMapping(Dataset set, AttributeMetaData attributeMetaData, int modeIndex, int limit) throws IOException {
		if (attributeMetaData.isNominal()) {
			io.jhdf.api.Attribute mapping = set.getAttribute(TableWriter.ATTRIBUTE_DICTIONARY);
			if (mapping != null) {
				readFromSeparateDictionary(set, mapping, attributeMetaData, modeIndex, limit);
			} else if (set.getJavaType().equals(String.class)) {
				//No extra dictionary set, read first few and interpret mode as row index
				readFromStringData(set, attributeMetaData, modeIndex, limit);
			} else {
				throw new HdfReaderException(HdfReaderException.Reason.INCONSISTENT_FILE, "dictionary attribute " +
						"missing for category indices dataset " + set.getPath());
			}
		}
	}

	/**
	 * Reads the first limit dictionary values from the mapping attribute or the dictionary it points to. Tries to
	 * associate the mode category index with a String value.
	 *
	 * @throws IOException
	 * 		if reading fails
	 */
	private void readFromSeparateDictionary(Dataset set, io.jhdf.api.Attribute mapping,
											AttributeMetaData attributeMetaData,
											int mode, int limit) throws IOException {
		if (mapping.getJavaType().equals(String.class)) {
			//only small mapping directly in attribute -> fine to read directly
			String[] mappingArray = (String[]) mapping.getData();
			attributeMetaData.setValueSetRelation(SetRelation.EQUAL);
			Set<String> valueSet = attributeMetaData.getValueSet();
			int length = mappingArray.length;
			if (length > limit + 1) {
				length = limit + 1;
				attributeMetaData.setValueSetRelation(SetRelation.SUPERSET);
			}
			for (int i = 1; i < length; i++) {
				valueSet.add(mappingArray[i]);
			}
			if (mode < mappingArray.length && mode >= 0) {
				attributeMetaData.setMode(mappingArray[mode]);
			} else if (mode != -1) {
				LOGGER.warning(() -> "mode index " + mode + " outside of mapping (length " + mappingArray.length + ")");
			}
		} else {
			boolean limited = readMappingFromDataset(mapping, set, getValueSetFiller(attributeMetaData, mode),
					limit + 1);
			if (mode > limit) {
				//already know that the dataset is contiguous and the value a long
				String modeValue = getModeAfter((ContiguousDataset) getDatasetByAddress(hdfFile,
						(long) mapping.getData()), mode);
				attributeMetaData.setMode(modeValue);
			}
			attributeMetaData.setValueSetRelation(limited ? SetRelation.SUPERSET : SetRelation.EQUAL);
		}
	}

	/**
	 * Reads the first limit values from the set. Tries to associate the mode row index with a String value.
	 *
	 * @throws IOException
	 * 		if reading fails
	 */
	private void readFromStringData(Dataset set, AttributeMetaData attributeMetaData, int mode, int limit) throws IOException {
		if (set instanceof ContiguousDataset) {
			boolean limited = addContiguousMapping((ContiguousDataset) set, getValueSetFiller(attributeMetaData, mode),
					limit + 1, false, set.getPath());
			if (mode > limit) {
				//already know that the dataset is contiguous
				String modeValue = getModeAfter((ContiguousDataset) set, mode - 1);
				attributeMetaData.setMode(modeValue);
			}
			attributeMetaData.setValueSetRelation(limited ? SetRelation.SUPERSET : SetRelation.EQUAL);
		} else {
			throw new HdfReaderException(HdfReaderException.Reason.NON_CONTIGUOUS, "non-contigous " +
					"dataset " + set.getPath());
		}
	}


	/**
	 * Creates a consumer that adds to the value set of the {@link AttributeMetaData} and adds the mode value if it is
	 * encountered.
	 */
	private Consumer<String> getValueSetFiller(AttributeMetaData attributeMetaData, int mode) {
		Set<String> valueSet = attributeMetaData.getValueSet();
		MutableInt counter = new MutableInt();
		return value -> {
			if (counter.intValue() == mode - 1) {
				attributeMetaData.setMode(value);
			}
			counter.increment();
			valueSet.add(value);
		};
	}

	/**
	 * Reads the mapping specified by the mapping hdf5 attribute into the nominal mapping of the attribute.
	 *
	 * @throws IOException
	 * 		if reading fails
	 */
	private boolean readMappingFromDataset(io.jhdf.api.Attribute mapping, Dataset set,
										   Consumer<String> valueCollector, int limit) throws IOException {
		if (!Long.class.isAssignableFrom(mapping.getJavaType())) {
			throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE,
					TableWriter.ATTRIBUTE_DICTIONARY + " attribute must contain a reference to another dataset " +
							"for dataset " + set.getPath());
		}
		Dataset mappingSet = getDatasetByAddress(hdfFile, (long) mapping.getData());
		if (mappingSet.getJavaType().equals(String.class)) {
			if (mappingSet instanceof ContiguousDataset) {
				return addContiguousMapping((ContiguousDataset) mappingSet, valueCollector, limit, true, set.getPath());
			} else {
				throw new HdfReaderException(HdfReaderException.Reason.NON_CONTIGUOUS, "non-contigous " +
						"dataset for dictionary of " + set.getPath());
			}
		} else {
			throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE,
					mappingSet.getJavaType() +
							" type not supported for dictionary of dataset: " + set.getPath());
		}
	}

	/**
	 * Reads the String values of the mappingSet into the nominal mapping of the attribute.
	 *
	 * @throws IOException
	 * 		if reading fails
	 */
	private boolean addContiguousMapping(ContiguousDataset mappingSet, Consumer<String> valueCollector, int limit,
										 boolean skipFirst, String name) throws IOException {
		long dataAddress = toDataAddress(hdfFile, mappingSet.getDataAddress());
		if (inChannel.position(dataAddress).position() == dataAddress) {
			if (mappingSet.getDataType() instanceof StringData) {
				return fillFixedLengthMapping(mappingSet, valueCollector, limit, skipFirst);
			} else if (mappingSet.getDataType() instanceof VariableLength) {
				return fillVariableLengthStringMapping(mappingSet, valueCollector, limit, skipFirst);
			} else {
				throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE,
						"unsupported String datatype " + mappingSet.getDataType() + " for dictionary of " + name);
			}
		} else {
			throw new IOException("Cannot move to position " + dataAddress);
		}
	}


	/**
	 * Reads the variable length Strings of the contiguousDataset into the value handler.
	 *
	 * @throws IOException
	 * 		if reading fails
	 */
	private boolean fillVariableLengthStringMapping(ContiguousDataset contiguousDataset, Consumer<String> valueHandler,
													int limit, boolean skipFirst) throws IOException {
		Charset charset = ((VariableLength) contiguousDataset.getDataType()).getEncoding();
		HdfFileChannel hdfFc = hdfFile.getHdfChannel();
		int dimension = contiguousDataset.getDimensions()[0];
		boolean limited = false;
		if (limit > 0 && dimension > limit) {
			dimension = limit;
			limited = true;
		}
		if (skipFirst) {
			//skip first (missing) value
			inChannel.skipBytes(4 + hdfFc.getSizeOfOffsets() + 4);//ignore missing value
		}
		for (int i = 1; i < dimension; i++) {
			String value = getVariableLengthValue(charset, hdfFc);
			valueHandler.accept(value);
		}
		return limited;
	}

	/**
	 * Reads the next variable length value from the channel and decodes the associated global heap value using the
	 * charset.
	 *
	 * @throws IOException
	 * 		if reading fails
	 */
	private String getVariableLengthValue(Charset charset, HdfFileChannel hdfFc) throws IOException {
		inChannel.readInt();//unused size
		long address = inChannel.readLong(hdfFc.getSizeOfOffsets());
		int index = inChannel.readInt();
		GlobalHeap heap = heaps.computeIfAbsent(address,
				add -> new GlobalHeap(hdfFc, add));
		ByteBuffer elementBuffer = heap.getObjectData(index);
		return charset.decode(elementBuffer).toString();
	}

	/**
	 * Reads the fixed length Strings of the contiguousDataset into the value handler.
	 *
	 * @throws IOException
	 * 		if reading fails
	 */
	private boolean fillFixedLengthMapping(ContiguousDataset contiguousDataset, Consumer<String> valueHandler,
										   int limit, boolean skipFirst) throws IOException {
		StringData stringData = (StringData) contiguousDataset.getDataType();
		int width = stringData.getSize();
		StringData.StringPaddingHandler paddingHandler = stringData.getStringPaddingHandler();
		Charset charset = stringData.getCharset();
		if (skipFirst) {
			inChannel.skipBytes(width);//ignore missing value
		}
		int dimension = contiguousDataset.getDimensions()[0];
		boolean limited = false;
		if (limit > 0 && dimension > limit) {
			dimension = limit;
			limited = true;
		}
		for (int i = 1; i < dimension; i++) {
			String value = getFixedLengthValue(width, paddingHandler, charset);
			valueHandler.accept(value);
		}
		return limited;
	}

	/**
	 * Reads the next fixed length String value of the given width from the channel decoding it using the padding
	 * handler and charset.
	 *
	 * @throws IOException
	 * 		if reading fails
	 */
	private String getFixedLengthValue(int width, StringData.StringPaddingHandler paddingHandler, Charset charset) throws IOException {
		ByteBuffer byteBuffer = inChannel.next(width);
		ByteBuffer elementBuffer = Utils.createSubBuffer(byteBuffer, width);
		paddingHandler.setBufferLimit(elementBuffer);
		return charset.decode(elementBuffer).toString();
	}

	/**
	 * Reads the value after skipping skip values from the mapping set. Assumes that parts of the set were read before.
	 *
	 * @return the value defined by skipping positions or {@code null} if skipping would lead to a value outside the
	 * set.
	 * @throws IOException
	 * 		if reading fails
	 */
	private String getModeAfter(ContiguousDataset mappingSet, int skip) throws IOException {
		if (skip >= mappingSet.getDimensions()[0]) {
			LOGGER.warning(() -> "mode index " + skip + " outside of mapping (length " + mappingSet.getDimensions()[0] + ")");
			return null;
		}
		long dataAddress = toDataAddress(hdfFile, mappingSet.getDataAddress());
		int width = mappingSet.getDataType().getSize();
		dataAddress += skip * width;
		if (inChannel.position(dataAddress).position() == dataAddress) {
			if (mappingSet.getDataType() instanceof StringData) {
				StringData dataType = (StringData) mappingSet.getDataType();
				return getFixedLengthValue(width, dataType.getStringPaddingHandler(), dataType.getCharset());
			} else if (mappingSet.getDataType() instanceof VariableLength) {
				return getVariableLengthValue(((VariableLength) mappingSet.getDataType()).getEncoding(),
						hdfFile.getHdfChannel());
			} else {
				//cannot happen since already tested in addContiguousMapping
				throw new AssertionError();
			}
		} else {
			throw new IOException("Cannot move to position " + dataAddress);
		}
	}

	/**
	 * Reads the mapping array into the nominal mapping of the attribute, ignoring the first value representing the
	 * missing value.
	 */
	private static void readArrayIntoMapping(Attribute attribute, String[] mappingArray) {
		NominalMapping nominalMapping = attribute.getMapping();
		for (int i = 1; i < mappingArray.length; i++) {
			String value = mappingArray[i];
			addNominalValue(attribute, nominalMapping, value);
		}
	}

	/**
	 * Adds the value to the nominalMapping of the attribute or throws an error if the value is already present.
	 */
	private static void addNominalValue(Attribute attribute, NominalMapping nominalMapping, String value) {
		if (nominalMapping.getIndex(value) >= 0) {
			throw new HdfReaderException(HdfReaderException.Reason.DICTIONARY_NOT_UNIQUE, value + " appears twice in" +
					" dictionary for column " + attribute.getName());
		}
		nominalMapping.mapString(value);
	}

}