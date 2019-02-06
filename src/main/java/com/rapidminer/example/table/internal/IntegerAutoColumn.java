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
package com.rapidminer.example.table.internal;

import java.io.Serializable;
import java.util.Arrays;

import com.rapidminer.example.utils.ExampleSetBuilder.DataManagement;


/**
 * {@link Column} that stores integer values in chunks. The chunks can either be sparse or dense and
 * switch automatically to the appropriate format for the given values. For this column,
 * {@link #complete()} must be called when the size is final before {@link #get(int)} or
 * {@link #set(int, double)} is called.
 *
 * @author Gisa Schaefer
 * @since 7.3.1
 */
final class IntegerAutoColumn implements Column {

	private static final long serialVersionUID = 1L;

	/**
	 * Building block of a {@link IntegerAutoColumn}.
	 *
	 * @author Gisa Schaefer
	 *
	 */
	static abstract class IntegerAutoChunk implements Serializable {

		private static final long serialVersionUID = 1L;

		/**
		 * the position of this chunk in {@link IntegerAutoColumn#chunks}
		 */
		final int id;

		/**
		 * the chunk array {@link IntegerAutoColumn#chunks}
		 */
		final IntegerAutoChunk[] chunks;

		/**
		 * decides about sparsity thresholds
		 */
		final DataManagement management;

		IntegerAutoChunk(int id, IntegerAutoChunk[] chunks, DataManagement management) {
			this.id = id;
			this.chunks = chunks;
			this.management = management;
		}

		/**
		 * Sets the value at the specified row to the given value. This assumes that row is the
		 * largest index set so far and no value for this row was set before. The necessary space
		 * must be allocated by {@link #ensure(int)} beforehand.
		 *
		 * @param row
		 *            the row that should be set
		 * @param value
		 *            the value that should be set at the row
		 */
		abstract void setLast(int row, double value);

		/**
		 * Ensures that the internal data structure can hold up to {@code size} values.
		 *
		 * @param size
		 *            the size that should be ensured
		 */
		abstract void ensure(int size);

		/**
		 * Gets the value at the specified row.
		 *
		 * @param row
		 *            the row that should be looked up
		 * @return the value at the specified row
		 */
		abstract double get(int row);

		/**
		 * Sets the value at the specified row to the given value.
		 *
		 * @param row
		 *            the row that should be set
		 * @param value
		 *            the value that should be set at the row
		 */
		abstract void set(int row, double value);

		/**
		 * Signals that no further calls to {@link #ensure(int)} and {@link #setLast(int,double)}
		 * will be made.
		 */
		void complete() {}
	}

	private IntegerAutoChunk[] chunks = new IntegerAutoChunk[1];
	private int chunkCount = 0;
	private int ensuredSize = 0;
	private final DataManagement management;

	/**
	 * Constructs a column with enough chunks to fit size values.
	 *
	 * @param size
	 *            the size of the column
	 */
	IntegerAutoColumn(int size, DataManagement management) {
		this.management = management;
		ensure(size);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Must not be called before {@link #complete()} was called.
	 */
	@Override
	public double get(int row) {
		return chunks[row >> AutoColumnUtils.CHUNK_SIZE_EXP].get(row & AutoColumnUtils.CHUNK_MODULO_MASK);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Must not be called before {@link #complete()} was called.
	 */
	@Override
	public void set(int row, double value) {
		chunks[row >> AutoColumnUtils.CHUNK_SIZE_EXP].set(row & AutoColumnUtils.CHUNK_MODULO_MASK, value);
	}

	@Override
	public void setLast(int row, double value) {
		chunks[row >> AutoColumnUtils.CHUNK_SIZE_EXP].setLast(row & AutoColumnUtils.CHUNK_MODULO_MASK, value);
	}

	@Override
	public void ensure(int size) {
		ensureChunks(size);
		int completeChunks = 0;
		boolean enlargeLastChunk = false;
		if (chunkCount > 0) {
			if (ensuredSize % AutoColumnUtils.CHUNK_SIZE > 0) {
				completeChunks = chunkCount - 1;
				enlargeLastChunk = true;
			} else {
				completeChunks = chunkCount;
			}
		}

		int rowsLeft = size - completeChunks * AutoColumnUtils.CHUNK_SIZE;

		while (rowsLeft > 0) {
			int chunkSize = Math.min(rowsLeft, AutoColumnUtils.CHUNK_SIZE);
			if (enlargeLastChunk) {
				chunks[chunkCount - 1].ensure(chunkSize);
				enlargeLastChunk = false;
			} else {
				if (management == DataManagement.MEMORY_OPTIMIZED) {
					// create sparse chunk with guessed default value 0
					IntegerAutoSparseChunk sparse = new IntegerAutoSparseChunk(chunkCount, chunks, 0, management);
					sparse.hasGuessedDefault();
					sparse.ensure(chunkSize);
					chunks[chunkCount] = sparse;
				} else {
					chunks[chunkCount] = new IntegerAutoDenseChunk(chunkCount, chunks, chunkSize, management);
				}
				chunkCount++;
			}
			rowsLeft -= chunkSize;
		}

		ensuredSize = size;
	}

	/**
	 * Ensures that the chunks array is big enough to hold all chunks.
	 *
	 * @param numberOfRows
	 *            the number of rows that should be stored in the chunks
	 */
	private void ensureChunks(int numberOfRows) {
		int chunksNeeded = numberOfRows / AutoColumnUtils.CHUNK_SIZE + 1;
		if (chunksNeeded <= chunks.length) {
			return;
		}
		int chunksMinGrowth = chunks.length == 1 ? 2 : chunks.length + (chunks.length >> 1);
		int newLength = Math.min(AutoColumnUtils.MAXIMAL_CHUNKS, Math.max(chunksNeeded, chunksMinGrowth));
		chunks = Arrays.copyOf(chunks, newLength);
	}

	@Override
	public void complete() {
		for (int i = 0; i < chunkCount; i++) {
			chunks[i].complete();
		}
	}
}
