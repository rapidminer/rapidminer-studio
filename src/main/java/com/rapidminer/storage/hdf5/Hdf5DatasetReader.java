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
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.table.internal.ColumnarExampleTable;
import com.rapidminer.hdf5.BufferedInChannel;
import com.rapidminer.hdf5.file.TableWriter;

import io.jhdf.GlobalHeap;
import io.jhdf.HdfFile;
import io.jhdf.HdfFileChannel;
import io.jhdf.ObjectHeader;
import io.jhdf.api.Dataset;
import io.jhdf.api.dataset.ContiguousDataset;
import io.jhdf.dataset.DatasetLoader;
import io.jhdf.dataset.NoParent;
import io.jhdf.object.datatype.StringData;
import io.jhdf.object.datatype.VariableLength;
import io.jhdf.object.message.DataSpaceMessage;


/**
 * A Reader that reads values from {@link Dataset}s into a {@link ColumnarExampleTable}.
 *
 * @author Gisa Meier
 * @since 9.7.0
 * @see Hdf5ExampleSetReader
 */
class Hdf5DatasetReader {

	private final ColumnarExampleTable table;
	private final HdfFile hdfFile;
	private final BufferedInChannel inChannel;
	private final Map<Long, GlobalHeap> heaps;

	Hdf5DatasetReader(ColumnarExampleTable table, HdfFile hdfFile, BufferedInChannel inChannel, Map<Long, GlobalHeap> heaps) {
		this.table = table;
		this.hdfFile = hdfFile;
		this.inChannel = inChannel;
		this.heaps = heaps;
	}

	/**
	 * Fills the data from the datasets into the {@link #table} at the column specified by the attributes.
	 *
	 * @param datasets
	 * 		the list of contiguous dataset containing the data
	 * @param attributes
	 * 		the list of attribute for which the data is read
	 * @throws IOException
	 * 		if reading fails
	 * @throws HdfReaderException
	 * 		if the format is wrong, e.g. the dataset is not contiguous or information is missing or of a wrong type
	 */
	void fillTable(List<Attribute> attributes, List<Dataset> datasets) throws IOException {
		int index = 0;
		for (Dataset dataset : datasets) {
			Attribute attribute = attributes.get(index++);
			table.resetColumn(attribute);
			if (dataset instanceof ContiguousDataset) {
				ContiguousDataset contiguousDataset = (ContiguousDataset) dataset;
				long dataAddress = toDataAddress(hdfFile, contiguousDataset.getDataAddress());
				if (inChannel.position(dataAddress).position() != dataAddress) {
					throw new IOException("Cannot move to position " + dataAddress);
				}
				handleContiguous(attribute, contiguousDataset);

			} else {
				throw new HdfReaderException(HdfReaderException.Reason.NON_CONTIGUOUS,
						"non-contigous dataset " + dataset.getPath());
			}
		}
		table.complete();
	}

	/**
	 * Reads the data from the dataset into the table at the column for the attribute.
	 *
	 * @throws IOException
	 * 		if reading fails
	 */
	private void handleContiguous(Attribute attribute, ContiguousDataset dataset) throws IOException {
		try {
			if (attribute.isNumerical()) {
				fillNumeric(attribute, dataset);
			} else if (attribute.isDateTime()) {
				fillDateTime(attribute, dataset);
			} else {
				fillNominal(attribute, dataset);
			}
		} catch (ColumnFillers.IORuntimeException e) {
			throw (IOException) e.getCause();
		}
	}

	/**
	 * Fills nominal values from the dataset into the table for the given attribute.
	 * Supported types are mapping indices as {@code byte}, {@code short} or {@code int} or fixed or variable-length
	 * {@code String}s.
	 */
	private void fillNominal(Attribute attribute, ContiguousDataset dataset) {
		if (dataset.getJavaType().equals(byte.class)) {
			table.fillColumn(attribute, ColumnFillers.getByteIndexColumnFiller(inChannel));
		} else if (dataset.getJavaType().equals(short.class)) {
			table.fillColumn(attribute, ColumnFillers.getShortIndexColumnFiller(inChannel));
		} else if (dataset.getJavaType().equals(int.class)) {
			table.fillColumn(attribute, ColumnFillers.getIntIndexColumnFiller(inChannel));
		} else if (dataset.getJavaType().equals(String.class)) {
			String missingReplace = getMissingValue(dataset);
			if (dataset.getDataType() instanceof StringData) {
				int width = dataset.getDataType().getSize();
				table.fillColumn(attribute, ColumnFillers.getStringColumnFiller(attribute.getMapping(), width,
						missingReplace, inChannel, ((StringData) dataset.getDataType()).getStringPaddingHandler(),
						((StringData) dataset.getDataType()).getCharset()));
			} else if (dataset.getDataType() instanceof VariableLength) {
				table.fillColumn(attribute,
						ColumnFillers.getVLenColumnFiller(((VariableLength) dataset.getDataType()).getEncoding(),
								attribute.getMapping(), missingReplace, inChannel, hdfFile.getHdfChannel(), heaps));
			} else {
				throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE,
						"unsupported String datatype " + dataset.getDataType() + " for dataset " + dataset.getPath());
			}
		} else {
			throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE, dataset.getJavaType() + " not " +
					"supported for nominal column, dataset: " + dataset.getPath());
		}
	}

	/**
	 * Fills date-time data from the dataset into the date-time attribute. If the dataset has the additional hdf5
	 * attribute for nanoseconds, the nanosecond data is added after reading the seconds.
	 *
	 * @throws IOException
	 * 		if reading fails
	 */
	private void fillDateTime(Attribute attribute, ContiguousDataset dataset) throws IOException {
		if (!dataset.getJavaType().equals(long.class)) {
			throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE, "date-time seconds must be " +
					"stored as long values for " + attribute.getName());
		}
		table.fillColumn(attribute, ColumnFillers.getLongDateColumnFiller(inChannel));
		io.jhdf.api.Attribute nanos = dataset.getAttribute(TableWriter.ATTRIBUTE_ADDITIONAL);
		if (nanos != null) {
			if (!Long.class.isAssignableFrom(nanos.getJavaType())) {
				throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE,
						TableWriter.ATTRIBUTE_ADDITIONAL + " attribute must contain a reference to another dataset " +
								"for dataset " + dataset.getPath());
			}
			long address = (long) nanos.getData();
			Dataset nanoData = getDatasetByAddress(hdfFile, address);
			if (nanoData instanceof ContiguousDataset) {
				ContiguousDataset contiguousDataset = (ContiguousDataset) nanoData;
				long dataAddress = toDataAddress(hdfFile, contiguousDataset.getDataAddress());
				if (inChannel.position(dataAddress).position() == dataAddress) {
					table.fillColumn(attribute, ColumnFillers.getNanoDateFiller(inChannel, table, attribute));
				} else {
					throw new IOException("Cannot move to position " + dataAddress);
				}
			} else {
				throw new HdfReaderException(HdfReaderException.Reason.NON_CONTIGUOUS,
						"non-contigous dataset " + dataset.getPath());
			}
		}
	}

	/**
	 * Fills numeric values from the dataset into the table for the given attribute.
	 * Supported types are {@code double}, {@code float}, {@code int}, {@code long}.
	 */
	private void fillNumeric(Attribute attribute, ContiguousDataset dataset) {
		if (dataset.getJavaType().equals(double.class)) {
			table.fillColumn(attribute, ColumnFillers.getDoubleColumnFiller(inChannel));
		} else if (dataset.getJavaType().equals(float.class)) {
			table.fillColumn(attribute, ColumnFillers.getFloatColumnFiller(inChannel));
		} else if (dataset.getJavaType().equals(int.class)) {
			table.fillColumn(attribute, ColumnFillers.getIntegerColumnFiller(inChannel));
		} else if (dataset.getJavaType().equals(long.class)) {
			table.fillColumn(attribute, ColumnFillers.getLongColumnFiller(inChannel));
		} else {
			throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE, dataset.getJavaType() + " not " +
					"supported for numeric column, dataset: " + dataset.getPath());
		}
	}

	/**
	 * Retrieves the missing value from the hdf5 attribute of the dataset.
	 */
	private static String getMissingValue(Dataset dataset) {
		String missing = "";
		io.jhdf.api.Attribute missingAttribute = dataset.getAttribute(TableWriter.ATTRIBUTE_MISSING);
		if (missingAttribute != null) {
			if (!missingAttribute.getJavaType().equals(String.class)) {
				throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE, "missing value must be a String" +
						" for dataset " + dataset.getPath());
			}
			missing = (String) missingAttribute.getData();
		}
		return missing;
	}


	/**
	 * Converts a hdf relative address to the absolute position in the file.
	 *
	 * @param file
	 * 		the hdf file
	 * @param hdfAddress
	 * 		the relative hdf5 address
	 * @return the address with respect to the beginning of the file
	 */
	static long toDataAddress(HdfFile file, long hdfAddress) {
		return file.getUserBlockSize() + hdfAddress;
	}

	/**
	 * Retrieves a {@link Dataset} from the hdfFile given the relative address.
	 *
	 * @param hdfFile
	 * 		the hdf5 file
	 * @param address
	 * 		the address of the dataset
	 * @return the dataset at the given address
	 * @throws HdfReaderException
	 * 		if there is no dataset at the given address
	 */
	static Dataset getDatasetByAddress(HdfFile hdfFile, long address) {
		HdfFileChannel hdfFc = hdfFile.getHdfChannel();
		ObjectHeader linkHeader = ObjectHeader.readObjectHeader(hdfFc, address);
		if (linkHeader.hasMessageOfType(DataSpaceMessage.class)) {
			// Its a a Dataset
			return DatasetLoader.createDataset(hdfFc, linkHeader, null, NoParent.INSTANCE);
		} else {
			throw new HdfReaderException(HdfReaderException.Reason.INCONSISTENT_FILE, "referenced address is not a " +
					"dataset address: " + address);
		}
	}

}