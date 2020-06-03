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

import static com.rapidminer.storage.hdf5.ExampleSetHdf5Writer.MILLISECONDS_PER_SECOND;
import static com.rapidminer.storage.hdf5.ExampleSetHdf5Writer.NANOS_PER_MILLISECOND;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.IntToDoubleFunction;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.table.internal.ColumnarExampleTable;
import com.rapidminer.hdf5.CustomDataInput;

import io.jhdf.GlobalHeap;
import io.jhdf.HdfFileChannel;
import io.jhdf.Utils;
import io.jhdf.object.datatype.StringData;


/**
 * Utility class that supplies different {@link IntToDoubleFunction}s to use them in the {@link Hdf5DatasetReader}.
 *
 * @author Gisa Meier
 * @since 9.7.0
 * @see Hdf5DatasetReader
 */
enum ColumnFillers {

	;//No-instance enum, only static methods

	/**
	 * Wrapper exception for {@link IOException}.
	 */
	static class IORuntimeException extends RuntimeException {

		IORuntimeException(IOException cause) {
			super(cause);
		}

	}

	/**
	 * Abstract super class for other column fillers.
	 */
	private abstract static class ColumnFiller implements IntToDoubleFunction {

		final CustomDataInput channel;
		ByteBuffer byteBuffer;

		private ColumnFiller(CustomDataInput channel, int width) {
			this.channel = channel;
			try {
				byteBuffer = channel.next(width);
			} catch (EOFException e) {
				throw new HdfReaderException(HdfReaderException.Reason.INCONSISTENT_FILE, "end of file reached while" +
						" reading the first row");
			} catch (IOException e) {
				throw new IORuntimeException(e);
			}
		}

		@Override
		public double applyAsDouble(int i) {
			try {
				ensureBuffer();
				return read();
			} catch (EOFException e) {
				throw new HdfReaderException(HdfReaderException.Reason.INCONSISTENT_FILE, "end of file reached while" +
						" reading row " + i);
			} catch (IOException e) {
				throw new IORuntimeException(e);
			}
		}

		protected abstract double read();

		protected abstract void ensureBuffer() throws IOException;
	}

	/**
	 * Column filler that reads int data from a channel and converts it to double values.
	 */
	private static class IntegerColumnFiller extends ColumnFiller {

		private IntBuffer buffer;

		private IntegerColumnFiller(CustomDataInput channel) {
			super(channel, 4);
			buffer = byteBuffer.asIntBuffer();
		}


		@Override
		protected double read() {
			return buffer.get();
		}

		@Override
		protected void ensureBuffer() throws IOException {
			if (buffer.remaining() < 1) {
				byteBuffer.position(byteBuffer.position() + buffer.position() * 4);
				byteBuffer = channel.next(4);
				buffer = byteBuffer.asIntBuffer();
			}
		}
	}

	/**
	 * Column filler that reads long data from a channel and converts it to double values, mapping {@link
	 * Long#MAX_VALUE} to {@link Double#NaN}.
	 */
	private static class LongColumnFiller extends ColumnFiller {

		private LongBuffer buffer;

		private LongColumnFiller(CustomDataInput channel) {
			super(channel, 8);
			buffer = byteBuffer.asLongBuffer();
		}

		@Override
		protected double read() {
			long value = buffer.get();
			return value == Long.MAX_VALUE ? Double.NaN : value;
		}

		@Override
		protected void ensureBuffer() throws IOException {
			if (buffer.remaining() < 1) {
				byteBuffer.position(byteBuffer.position() + buffer.position() * 8);
				byteBuffer = channel.next(8);
				buffer = byteBuffer.asLongBuffer();
			}
		}
	}

	/**
	 * Column filler that reads double data from a channel.
	 */
	private static class DoubleColumnFiller extends ColumnFiller {

		private DoubleBuffer buffer;

		private DoubleColumnFiller(CustomDataInput channel) {
			super(channel, 8);
			buffer = byteBuffer.asDoubleBuffer();
		}

		@Override
		protected double read() {
			return buffer.get();
		}

		@Override
		protected void ensureBuffer() throws IOException {
			if (buffer.remaining() < 1) {
				byteBuffer.position(byteBuffer.position() + buffer.position() * 8);
				byteBuffer = channel.next(8);
				buffer = byteBuffer.asDoubleBuffer();
			}
		}
	}

	/**
	 * Column filler that reads float data from a channel and converts it to double values.
	 */
	private static class FloatColumnFiller extends ColumnFiller {

		private FloatBuffer buffer;

		private FloatColumnFiller(CustomDataInput channel) {
			super(channel, 4);
			buffer = byteBuffer.asFloatBuffer();
		}

		@Override
		protected double read() {
			return buffer.get();
		}

		@Override
		protected void ensureBuffer() throws IOException {
			if (buffer.remaining() < 1) {
				byteBuffer.position(byteBuffer.position() + buffer.position() * 4);
				byteBuffer = channel.next(4);
				buffer = byteBuffer.asFloatBuffer();
			}
		}
	}

	/**
	 * Column filler that reads int category indices data from a channel and converts it to double values by
	 * converting {@code 0} to {@link Double#NaN} and decreasing the other values by {@code 1}.
	 */
	private static class IntIndexColumnFiller extends ColumnFiller {

		private IntBuffer buffer;

		private IntIndexColumnFiller(CustomDataInput channel) {
			super(channel, 4);
			buffer = byteBuffer.asIntBuffer();
		}

		@Override
		protected double read() {
			int value = buffer.get();
			return value == 0 ? Double.NaN : value - 1;
		}

		@Override
		protected void ensureBuffer() throws IOException {
			if (buffer.remaining() < 1) {
				byteBuffer.position(byteBuffer.position() + buffer.position() * 4);
				byteBuffer = channel.next(4);
				buffer = byteBuffer.asIntBuffer();
			}
		}
	}

	/**
	 * Column filler that reads short category indices data from a channel and converts it to double values by
	 * converting {@code 0} to {@link Double#NaN} and decreasing the other values by {@code 1}.
	 */
	private static class ShortIndexColumnFiller extends ColumnFiller {

		private ShortBuffer buffer;

		private ShortIndexColumnFiller(CustomDataInput channel) {
			super(channel, 2);
			buffer = byteBuffer.asShortBuffer();
		}

		@Override
		protected double read() {
			short value = buffer.get();
			return value == 0 ? Double.NaN : value - 1;
		}

		@Override
		protected void ensureBuffer() throws IOException {
			if (buffer.remaining() < 1) {
				byteBuffer.position(byteBuffer.position() + buffer.position() * 2);
				byteBuffer = channel.next(2);
				buffer = byteBuffer.asShortBuffer();
			}
		}
	}

	/**
	 * Column filler that reads byte category indices data from a channel and converts it to double values by
	 * converting {@code 0} to {@link Double#NaN} and decreasing the other values by {@code 1}.
	 */
	private static class ByteIndexColumnFiller extends ColumnFiller {

		private ByteIndexColumnFiller(CustomDataInput channel) {
			super(channel, 1);
		}

		@Override
		protected double read() {
			byte byteValue = byteBuffer.get();
			return byteValue == 0 ? Double.NaN : byteValue - 1;
		}

		@Override
		protected void ensureBuffer() throws IOException {
			if (byteBuffer.remaining() < 1) {
				byteBuffer = channel.next(1);
			}
		}
	}

	/**
	 * Column filler that reads fixed length String data from the channel and converts it to double values using a
	 * {@link NominalMapping}. If a String value equals a defined missingValue, it is converted to {@link Double#NaN}.
	 */
	private static class StringColumnFiller extends ColumnFiller {

		private final NominalMapping mapping;
		private final String missingValue;
		private Charset charset;
		private final int width;
		private final StringData.StringPaddingHandler paddingHandler;

		private StringColumnFiller(NominalMapping mapping, int width, String missingValue, CustomDataInput channel,
								   StringData.StringPaddingHandler paddingHandler, Charset charset) {
			super(channel, width);
			this.mapping = mapping;
			this.missingValue = missingValue;
			this.width = width;
			this.paddingHandler = paddingHandler;
			this.charset = charset;
		}

		@Override
		protected double read() {
			ByteBuffer elementBuffer = Utils.createSubBuffer(byteBuffer, width);
			paddingHandler.setBufferLimit(elementBuffer);
			String value = charset.decode(elementBuffer).toString();
			if (value.equals(missingValue)) {
				return Double.NaN;
			}
			return mapping.mapString(value);
		}

		@Override
		protected void ensureBuffer() throws IOException {
			if (byteBuffer.remaining() < width) {
				byteBuffer = channel.next(width);
			}
		}
	}

	/**
	 * Column filler that reads variable length String data from the channel and converts it to double values using a
	 * {@link NominalMapping}. If a String value equals a defined missingValue, it is converted to {@link Double#NaN}.
	 */
	private static class VLenColumnFiller implements IntToDoubleFunction {
		private final Map<Long, GlobalHeap> heaps;
		private final Charset charset;
		private final NominalMapping mapping;
		private final String missingValue;
		private final CustomDataInput channel;
		private final HdfFileChannel hdfFc;

		private VLenColumnFiller(Charset charset, NominalMapping mapping, String missingValue, CustomDataInput channel,
								 HdfFileChannel hdfFc, Map<Long, GlobalHeap> heaps) {
			this.charset = charset;
			this.mapping = mapping;
			this.missingValue = missingValue;
			this.channel = channel;
			this.hdfFc = hdfFc;
			this.heaps = heaps;
		}


		@Override
		public double applyAsDouble(int i) {
			String value;
			try {
				channel.readInt();//unused size
				long address = channel.readLong(hdfFc.getSizeOfOffsets());
				int index = channel.readInt();
				GlobalHeap heap = heaps.computeIfAbsent(address, add -> new GlobalHeap(hdfFc, add));
				ByteBuffer bb = heap.getObjectData(index);
				value = charset.decode(bb).toString();
				if (!value.equals(missingValue)) {
					return mapping.mapString(value);
				}
				return Double.NaN;
			} catch (EOFException e) {
				throw new HdfReaderException(HdfReaderException.Reason.INCONSISTENT_FILE, "end of file reached while" +
						" reading row " + i);
			} catch (IOException e) {
				throw new IORuntimeException(e);
			}
		}
	}


	/**
	 * Column filler that reads long data from the channel representing seconds and converts it to double values
	 * representing milli-seconds. {@link Long#MAX_VALUE} will be converted to {@link Double#NaN}.
	 */
	private static class LongDateColumnFiller extends ColumnFiller {

		private LongBuffer buffer;

		private LongDateColumnFiller(CustomDataInput channel) {
			super(channel, 8);
			buffer = byteBuffer.asLongBuffer();
		}

		@Override
		protected double read() {
			long value = buffer.get();
			return value == Long.MAX_VALUE ? Double.NaN : value * MILLISECONDS_PER_SECOND;
		}

		@Override
		protected void ensureBuffer() throws IOException {
			if (buffer.remaining() < 1) {
				byteBuffer.position(byteBuffer.position() + buffer.position() * 8);
				byteBuffer = channel.next(8);
				buffer = byteBuffer.asLongBuffer();
			}
		}
	}

	/**
	 * Column filler that reads long int from the channel representing nanoseconds and converts it to double values
	 * representing milli-seconds. It then adds the new milli-seconds to the existing milliseconds for the attribute in
	 * the table.
	 */
	private static class NanoDateColumnFiller extends ColumnFiller {
		private final ColumnarExampleTable table;
		private Attribute attribute;
		private IntBuffer buffer;
		private int index = 0;

		private NanoDateColumnFiller(CustomDataInput channel, ColumnarExampleTable table, Attribute attribute) {
			super(channel, 4);
			this.table = table;
			this.attribute = attribute;
			buffer = byteBuffer.asIntBuffer();
		}

		@Override
		protected double read() {
			int value = buffer.get();
			return table.getDataRow(index++).get(attribute) + value / NANOS_PER_MILLISECOND;
		}

		@Override
		protected void ensureBuffer() throws IOException {
			if (buffer.remaining() < 1) {
				byteBuffer.position(byteBuffer.position() + buffer.position() * 4);
				byteBuffer = channel.next(4);
				buffer = byteBuffer.asIntBuffer();
			}
		}
	}

	/**
	 * Gets a column filler that reads int category indices data from the channel and converts it to double values by
	 * converting {@code 0} to {@link Double#NaN} and decreasing the other values by {@code 1}.
	 *
	 * @param channel
	 * 		the channel to read from
	 * @return a {@link IntToDoubleFunction}
	 */
	static IntIndexColumnFiller getIntIndexColumnFiller(CustomDataInput channel) {
		return new IntIndexColumnFiller(channel);
	}

	/**
	 * Gets a column filler that reads short category indices data from the channel and converts it to double values by
	 * converting {@code 0} to {@link Double#NaN} and decreasing the other values by {@code 1}.
	 *
	 * @param channel
	 * 		the channel to read from
	 * @return a {@link IntToDoubleFunction}
	 */
	static ShortIndexColumnFiller getShortIndexColumnFiller(CustomDataInput channel) {
		return new ShortIndexColumnFiller(channel);
	}

	/**
	 * Gets a column filler that reads byte category indices data from the channel and converts it to double values by
	 * converting {@code 0} to {@link Double#NaN} and decreasing the other values by {@code 1}.
	 *
	 * @param channel
	 * 		the channel to read from
	 * @return a {@link IntToDoubleFunction}
	 */
	static ByteIndexColumnFiller getByteIndexColumnFiller(CustomDataInput channel) {
		return new ByteIndexColumnFiller(channel);
	}

	/**
	 * Gets a column filler that reads fixed length String data from the channel and converts it to double values using
	 * the mapping. If a String value equals the missingValue, it is converted to {@link Double#NaN}.
	 *
	 * @param mapping
	 * 		the nominal mapping to use to convert Strings to double values
	 * @param width
	 * 		the fixed length width of the String data
	 * @param missingValue
	 * 		the String representation for the missing value
	 * @param channel
	 * 		the channel to read from
	 * @param paddingHandler
	 * 		decides how many bytes to read
	 * @param charset
	 * 		the charset to use when converting bytes to String
	 * @return a {@link IntToDoubleFunction}
	 */
	static StringColumnFiller getStringColumnFiller(NominalMapping mapping, int width, String missingValue,
													CustomDataInput channel,
													StringData.StringPaddingHandler paddingHandler, Charset charset) {
		return new StringColumnFiller(mapping, width, missingValue, channel, paddingHandler, charset);
	}

	/**
	 * Gets a column filler that reads variable length String data from the channel and converts it to double values
	 * using the mapping. If a String value equals the missingValue, it is converted to {@link Double#NaN}.
	 *
	 * @param charset
	 * 		the charset to use when reading from the channel
	 * @param mapping
	 * 		the nominal mapping to use to convert Strings to double values
	 * @param missingValue
	 * 		the String representation for the missing value
	 * @param channel
	 * 		the channel to read from
	 * @param hdfFc
	 * 		the information holder for sizes
	 * @param heaps
	 * 		the map caching all already read global heaps
	 * @return a {@link IntToDoubleFunction}
	 */
	static VLenColumnFiller getVLenColumnFiller(Charset charset, NominalMapping mapping, String missingValue,
												CustomDataInput channel, HdfFileChannel hdfFc,
												Map<Long, GlobalHeap> heaps) {
		return new VLenColumnFiller(charset, mapping, missingValue, channel, hdfFc, heaps);
	}

	/**
	 * Gets a column filler that reads long int from the channel representing nanoseconds and converts it to double
	 * values representing milli-seconds. It then adds the new milli-seconds to the existing milliseconds for the
	 * attribute in the table.
	 *
	 * @param channel
	 * 		the channel to read from
	 * @param table
	 * 		the table to read the old value from
	 * @param attribute
	 * 		the attribute for which to read the old value from the table
	 * @return a {@link IntToDoubleFunction}
	 */
	static NanoDateColumnFiller getNanoDateFiller(CustomDataInput channel, ColumnarExampleTable table,
												  Attribute attribute) {
		return new NanoDateColumnFiller(channel, table, attribute);
	}

	/**
	 * Gets a column filler that reads long data from the channel representing seconds and converts it to double values
	 * representing milli-seconds. {@link Long#MAX_VALUE} will be converted to {@link Double#NaN}.
	 *
	 * @param channel
	 * 		the channel to read from
	 * @return a {@link IntToDoubleFunction}
	 */
	static LongDateColumnFiller getLongDateColumnFiller(CustomDataInput channel) {
		return new LongDateColumnFiller(channel);
	}

	/**
	 * Gets a column filler that reads int data from the channel and converts it to double values.
	 *
	 * @param channel
	 * 		the channel to read from
	 * @return a {@link IntToDoubleFunction}
	 */
	static IntegerColumnFiller getIntegerColumnFiller(CustomDataInput channel) {
		return new IntegerColumnFiller(channel);
	}

	/**
	 * Gets a column filler that reads long data from the channel and converts it to double values, mapping {@link
	 * Long#MAX_VALUE} to {@link Double#NaN}.
	 *
	 * @param channel
	 * 		the channel to read from
	 * @return a {@link IntToDoubleFunction}
	 */
	static LongColumnFiller getLongColumnFiller(CustomDataInput channel) {
		return new LongColumnFiller(channel);
	}

	/**
	 * Gets a column filler that reads double data from the channel.
	 *
	 * @param channel
	 * 		the channel to read from
	 * @return a {@link IntToDoubleFunction}
	 */
	static DoubleColumnFiller getDoubleColumnFiller(CustomDataInput channel) {
		return new DoubleColumnFiller(channel);
	}

	/**
	 * Gets a column filler that reads float data from the channel and converts it to double values.
	 *
	 * @param channel
	 * 		the channel to read from
	 * @return a {@link IntToDoubleFunction}
	 */
	static FloatColumnFiller getFloatColumnFiller(CustomDataInput channel) {
		return new FloatColumnFiller(channel);
	}

}