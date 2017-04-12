/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import com.rapidminer.example.utils.ExampleSetBuilder.DataManagement;


/**
 * {@link Column} that stores integer values in chunks. The chunks can either be sparse or dense and
 * switch automatically to the appropriate format for the given values. This column assumes that
 * {@link complete} is never called to indicate that the column is finished.
 *
 * @author Gisa Schaefer
 * @since 7.3.1
 */
final class IntegerIncompleteAutoColumn implements Column {

	private static final long serialVersionUID = 1L;

	/**
	 * Building block of a {@link IntegerIncompleteAutoColumn}.
	 *
	 * @author Gisa Schaefer
	 *
	 */
	static abstract class IntegerIncompleteChunk implements Serializable {

		private static final long serialVersionUID = 1L;

		/**
		 * the position of this chunk in {@link IntegerIncompleteAutoColumn#chunks}
		 */
		final int id;

		/**
		 * the chunk array {@link IntegerIncompleteAutoColumn#chunks}
		 */
		final IntegerIncompleteChunk[] chunks;

		/**
		 * decides about sparsity thresholds
		 */
		final DataManagement management;

		IntegerIncompleteChunk(int id, IntegerIncompleteChunk[] chunks, DataManagement management) {
			this.id = id;
			this.chunks = chunks;
			this.management = management;
		}

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

	}

	private IntegerIncompleteChunk[] chunks = new IntegerIncompleteChunk[Integer.MAX_VALUE / AutoColumnUtils.CHUNK_SIZE + 1];
	private int position = 0;
	private int chunkCount = 0;
	private int ensuredSize = 0;

	private final DataManagement management;

	/**
	 * Constructs a column with enough chunks to fit size values.
	 *
	 * @param size
	 *            the size of the column
	 */
	IntegerIncompleteAutoColumn(int size, DataManagement management) {
		this.management = management;
		ensure(size);
	}

	@Override
	public double get(int row) {
		return chunks[row >> AutoColumnUtils.CHUNK_SIZE_EXP].get(row & AutoColumnUtils.CHUNK_MODULO_MASK);
	}

	@Override
	public void set(int row, double value) {
		chunks[row >> AutoColumnUtils.CHUNK_SIZE_EXP].set(row & AutoColumnUtils.CHUNK_MODULO_MASK, value);
	}

	@Override
	public void append(double value) {
		set(position++, value);
	}

	@Override
	public void ensure(int size) {
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
					IntegerIncompleteSparseChunk sparse = new IntegerIncompleteSparseChunk(chunkCount, chunks, 0,
							management);
					sparse.hasGuessedDefault();
					sparse.ensure(chunkSize);
					chunks[chunkCount] = sparse;
				} else {
					chunks[chunkCount] = new IntegerIncompleteDenseChunk(chunkCount, chunks, chunkSize, management);
				}
				chunkCount++;
			}
			rowsLeft -= chunkSize;
		}

		ensuredSize = size;
	}

}
