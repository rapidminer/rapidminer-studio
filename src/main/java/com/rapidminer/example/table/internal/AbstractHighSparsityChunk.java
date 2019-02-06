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

import com.rapidminer.example.Tools;


/**
 * Super class for sparse chunks with high sparsity (e.g., few non-default values). Stores only
 * values different from the default value. {@link #set(int, double)} returns {@code true} if the
 * chunk is filled more than {@link AutoColumnUtils#THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY}. Should only
 * be extended by two classes in order to be fast.
 *
 * @author Jan Czogalla
 * @since 7.3.1
 */
abstract class AbstractHighSparsityChunk implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final int MIN_NON_EMPTY_SIZE = 8;

	private int[] indices = AutoColumnUtils.EMPTY_INTEGER_ARRAY;
	protected int valueCount;
	private double defaultValue;
	private int ensuredCount;

	AbstractHighSparsityChunk(double defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Returns the value stored for this row.
	 *
	 * @param row
	 *            the row for which to obtain the stored value
	 * @return the value stored for row
	 */
	public final double get(int row) {
		int index = getIndex(row);
		return index < 0 ? defaultValue : getValue(index);
	}

	/**
	 * Sets the value for the given row. Returns {@code true} if after this set the sparse chunk is
	 * too full, i.e. its density is bigger than
	 * {@link AutoColumnUtils#THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY}. Note that the density check only
	 * works if the total size was {@link #ensure}d before.
	 *
	 * @param row
	 *            the row for which to set the value
	 * @param value
	 *            the value to store
	 * @return {@code true} if the maximal density is reached
	 */
	public final boolean set(int row, double value) {
		int index = getIndex(row);
		if (Tools.isDefault(defaultValue, value)) {
			// index not set, default value => do nothing
			if (index < 0) {
				return false;
			}
			// remove existing index
			removeIndex(index);
			return false;
		}
		boolean tooFull = false;
		if (index < 0) {
			// insert new index
			// see Arrays.binarySearch
			index = -index - 1;
			insertIndex(index);
			// check density
			if (valueCount / (double) ensuredCount > AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY) {
				tooFull = true;
			}
		}
		// set index
		indices[index] = row;
		// set value in base column
		setValue(index, value);
		return tooFull;
	}

	/**
	 * The index returned by binary search for the row in {@link #indices}. Returns only a positive
	 * number if the row was found. If the row was not found the returned negative index encodes
	 * where to insert this new row (see {@link Arrays#binarySearch}).
	 *
	 * @param row
	 *            the row to search for
	 * @return the index where row is found or a negative index
	 */
	private int getIndex(int row) {
		// if new row is bigger than the biggest or no row inserted yet,
		// no binary search is necessary
		if (valueCount == 0 || row > indices[valueCount - 1]) {
			return -valueCount - 1;
		}
		return Arrays.binarySearch(indices, 0, valueCount, row);
	}

	/**
	 * Sets the total size.
	 *
	 * @param size
	 *            the expected size
	 */
	public final void ensure(int size) {
		ensuredCount = size;
	}

	/**
	 * Grows and shifts the indices and value arrays so that there is a new place at index.
	 */
	private void insertIndex(int index) {
		int[] tmp = checkedGrow();
		AutoColumnUtils.copy(indices, tmp, index, index, index + 1, valueCount);
		indices = tmp;
		insertValueIndex(index, indices.length);
		valueCount++;
	}

	/**
	 * Removes the given index from the indices and value arrays.
	 */
	private void removeIndex(int index) {
		int[] tmp = checkedShrink();
		AutoColumnUtils.copy(indices, tmp, index, index + 1, index, tmp.length);
		indices = tmp;
		removeValueIndex(index, indices.length);
		// overwrite duplicate last row with MAX_VALUE
		indices[indices.length - 1] = Integer.MAX_VALUE;
		valueCount--;
	}

	/**
	 * Enlarges the {@link #indices} array if necessary.
	 */
	private int[] checkedGrow() {
		int length = indices.length;
		if (valueCount < length) {
			return indices;
		}
		// grow
		int newLength = length == 0 ? MIN_NON_EMPTY_SIZE : length + (length >> 1);
		return new int[newLength];
	}

	/**
	 * Checks if the {@link #indices} array is too empty and shrinks it if necessary.
	 */
	private int[] checkedShrink() {
		int length = indices.length;
		if (length >> 1 >= MIN_NON_EMPTY_SIZE && valueCount - 1 <= length >> 2) {
			// shrink
			return new int[length >> 1];
		}
		return indices;
	}

	/**
	 * Removes the given index from the values array and sets its length.
	 *
	 * @param index
	 *            the index to remove
	 * @param length
	 *            the desired array length
	 */
	abstract void removeValueIndex(int index, int length);

	/**
	 * Inserts a new place in the values array at the given index and ensures that the array has the
	 * given length.
	 *
	 * @param index
	 *            the index to insert
	 * @param length
	 *            the desired array length
	 */
	abstract void insertValueIndex(int index, int length);

	/**
	 * Returns the value stored at the given index.
	 *
	 * @param index
	 *            the index to look up
	 * @return the value for the index
	 */
	abstract double getValue(int index);

	/**
	 * Sets the value at position index of the values array.
	 *
	 * @param index
	 *            the index where to set the value
	 * @param value
	 *            the value to store
	 */
	abstract void setValue(int index, double value);

}
