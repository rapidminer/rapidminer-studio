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
import java.io.InputStream;
import java.util.List;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.generator.ExampleSetGenerator;
import com.rapidminer.operator.nio.file.FileInputPortHandler;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.parameter.internal.DataManagementParameterHelper;


/**
 * Superclass for file data source operators which read the file byte per byte into a byte array and
 * extract the actual data from that array. This class provides some methods to extract integer and
 * floating point values from such an array.
 *
 * @author Tobias Malbrecht
 */
public abstract class BytewiseExampleSource extends AbstractExampleSource {

	/** The parameter name for &quot;Name of the file to read the data from.&quot; */
	public static final String PARAMETER_FILENAME = "filename";

	/** The parameter name for &quot;Determines, how the data is represented internally.&quot; */
	public static final String PARAMETER_DATAMANAGEMENT = ExampleSetGenerator.PARAMETER_DATAMANAGEMENT;

	/** A generic wrong file format error message. */
	protected static final String GENERIC_ERROR_MESSAGE = "Wrong file format";

	/** A even more generic error message. */
	protected static final String UNSPECIFIED_ERROR_MESSAGE = "Unspecified error";

	/** The length of a byte measured in bytes. */
	protected static final int LENGTH_BYTE = 1;

	/** The length of an int measured in bytes. */
	protected static final int LENGTH_INT_32 = 4;

	/** The length of a double measured in bytes. */
	protected static final int LENGTH_DOUBLE = 8;

	protected InputPort fileInputPort = getInputPorts().createPort("file");
	protected FileInputPortHandler filePortHandler = new FileInputPortHandler(this, fileInputPort, PARAMETER_FILENAME);

	public BytewiseExampleSource(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		int datamanagement = getParameterAsInt(PARAMETER_DATAMANAGEMENT);
		if (!Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT))) {
			datamanagement = DataRowFactory.TYPE_DOUBLE_ARRAY;
		}
		DataRowFactory dataRowFactory = new DataRowFactory(datamanagement, '.');
		ExampleSet result = null;

		// read file and construct example set
		try {
			InputStream inputStream = filePortHandler.openSelectedFile();
			result = readStream(inputStream, dataRowFactory);
			inputStream.close();
		} catch (IOException e) {
			throw new UserError(this, e, 302, filePortHandler.getSelectedFileDescription(), e.getMessage());
		}

		// verify that the result is not null
		if (result == null) {
			throw new UserError(this, 302, filePortHandler.getSelectedFileDescription(), UNSPECIFIED_ERROR_MESSAGE);
		}

		// verify that the resulting example set is not empty
		Tools.isNonEmpty(result);
		return result;
	}

	/**
	 * Returns the suffix of the files which should be read by the input operator.
	 */
	protected abstract String getFileSuffix();

	/**
	 * Reads the given file and constructs an example set from the read data.
	 */
	protected abstract ExampleSet readStream(InputStream inputStream, DataRowFactory dataRowFactory) throws IOException,
			UndefinedParameterError;

	/**
	 * Reads a number (specified by length) of bytes from a given file reader into a byte array
	 * beginning at index 0.
	 */
	protected int read(InputStream inputStream, byte[] buffer, int length) throws IOException {
		final int offset = 0;
		return read(inputStream, buffer, offset, length);
	}

	/**
	 * Reads a number (specified by length) of bytes from a given file reader into a byte array
	 * beginning at the given offset.
	 */
	protected int read(InputStream inputStream, byte[] buffer, int offset, int length) throws IOException {
		int readLength = inputStream.read(buffer, offset, length);
		if (readLength != length) {
			throw new IOException("wrong byte length");
		}
		return readLength;
	}

	/**
	 * Reads a number (specified by length) of bytes from a given file reader into a byte array
	 * beginning at index 0. No read length verification is performed.
	 */
	protected int readWithoutLengthCheck(InputStream inputStream, byte[] buffer, int length) throws IOException {
		return inputStream.read(buffer, 0, length);
	}

	/**
	 * Reads bytes from a given file reader until either a certain character is read, the buffer is
	 * completely filled or the end of file is reached.
	 */
	protected int read(InputStream inputStream, byte[] buffer, char divider) throws IOException {
		int index = 0;
		do {
			byte readByte = (byte) (0x000000FF & inputStream.read());
			if (readByte == -1 || readByte == (byte) divider) {
				return index;
			}
			buffer[index] = readByte;
			index++;
		} while (index < buffer.length);
		return index;
	}

	/**
	 * Reads bytes from a given file reader until either a specified character sequence is read, the
	 * buffer is completely filled or the end of file is reached.
	 */
	protected int read(InputStream inputStream, byte[] buffer, char[] divider) throws IOException {
		int index = 0;
		int dividerIndex = 0;
		do {
			byte readByte = (byte) (0x000000FF & inputStream.read());
			if (readByte == -1) {
				return index;
			}
			if (readByte == divider[dividerIndex]) {
				dividerIndex++;
			}
			if (dividerIndex == divider.length) {
				index -= dividerIndex - 1;
				for (int i = index; i < index + dividerIndex; i++) {
					if (i >= buffer.length) {
						break;
					}
					buffer[i] = 0;
				}
				return index;
			}
			buffer[index] = readByte;
			index++;
		} while (index < buffer.length);
		return index;
	}

	/**
	 * Extracts a 2-byte (short) int from a byte array.
	 */
	protected int extract2ByteInt(byte[] buffer, int offset, boolean reverseEndian) {
		int r = 0;
		if (reverseEndian) {
			r = (buffer[offset + 1] << 8) + (0x000000FF & buffer[offset]);
		} else {
			r = (buffer[offset] << 8) + (0x000000FF & buffer[offset + 1]);
		}
		return r;
	}

	/**
	 * Extracts an int from a byte array.
	 */
	protected int extractInt(byte[] buffer, int offset, boolean reverseEndian) {
		int r = 0;
		if (reverseEndian) {
			for (int i = offset + 3; i >= offset; i--) {
				r = r << 8;
				r += 0x000000FF & buffer[i];
			}
		} else {
			for (int i = offset; i < offset + 4; i++) {
				r = r << 8;
				r += 0x000000FF & buffer[i];
			}
		}
		return r;
	}

	/**
	 * Extracts a float from a byte array.
	 */
	protected float extractFloat(byte[] value, int offset, boolean reverseEndian) {
		int bits = 0;
		if (reverseEndian) {
			for (int i = offset + 3; i >= offset; i--) {
				bits = bits << 8;
				bits += 0x000000FF & value[i];
			}
		} else {
			for (int i = offset; i < offset + 4; i++) {
				bits = bits << 8;
				bits += 0x000000FF & value[i];
			}
		}
		return java.lang.Float.intBitsToFloat(bits);
	}

	/**
	 * Extracts a double from a byte array.
	 */
	protected double extractDouble(byte[] value, int offset, boolean reverseEndian) {
		long bits = 0;
		if (reverseEndian) {
			for (int i = offset + 7; i >= offset; i--) {
				bits = bits << 8;
				bits += 0x000000FF & value[i];
			}
		} else {
			for (int i = offset; i < offset + 8; i++) {
				bits = bits << 8;
				bits += 0x000000FF & value[i];
			}
		}
		return java.lang.Double.longBitsToDouble(bits);
	}

	/**
	 * Extracts string from byte array.
	 */
	protected String extractString(byte[] value, int offset, int length) {
		/* TODO: Shevek suggests this use a Charset for safety. */
		return (new String(value, offset, length)).trim();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = FileInputPortHandler.makeFileParameterType(this, PARAMETER_FILENAME,
				"Name of the file to read the data from.", getFileSuffix(), () -> fileInputPort);
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);
		DataManagementParameterHelper.addParameterTypes(types, this);
		return types;
	}
}
