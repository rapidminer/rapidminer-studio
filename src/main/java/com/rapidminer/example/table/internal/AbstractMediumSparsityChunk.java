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
 * Super class for sparse chunks with medium sparsity. Stores only values different from the default
 * value. {@link #set(int, double)} returns {@code true} if the chunk is filled more than
 * {@link #getMaximalDensity(int)}. Should only be extended by two classes in order to be fast.
 *
 * @author Jan Czogalla
 * @since 7.3.1
 */
abstract class AbstractMediumSparsityChunk implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final int MIN_NON_EMPTY_SIZE = 8;

	/**
	 * the maximal row that can occur. This is a power of 2 minus 1, so instead of
	 * {@code % (MAX_SIZE+1)} we can do {@code & MAX_SIZE}.
	 */
	private static final int MAX_SIZE = AutoColumnUtils.CHUNK_SIZE - 1;

	private int[] indices = AutoColumnUtils.EMPTY_INTEGER_ARRAY;
	protected int valueCount;
	private double defaultValue;
	private int ensuredCount;
	private byte[] bloomFilter = AutoColumnUtils.EMPTY_BYTE_ARRAY;

	/**
	 * 2^(AbstractAutoColumn.CHUNK_SIZE_EXP - bloomShift) = number of bits in bloomFilter =
	 * bloomFilter.length * 2^3
	 *
	 * This constant is adjusted by {@link #growBloomFilter()} so that this stays true when the
	 * bloomFilter grows
	 */
	private int bloomShift = AutoColumnUtils.CHUNK_SIZE_EXP - 3 + 1;

	private int bloomMult = (int) Math.pow(2, bloomShift) + 1;

	AbstractMediumSparsityChunk(double defaultValue) {
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
		int index = getIndex(row, true);
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
		if (Tools.isDefault(defaultValue, value)) {
			int index = getIndex(row, true);
			// index not set, default value => do nothing
			if (index < 0) {
				return false;
			}
			// remove existing index
			removeIndex(index);
			return false;
		}
		boolean tooFull = false;
		int index = getIndex(row, false);
		if (index < 0) {
			// insert new index
			// see Arrays.binarySearch
			index = -index - 1;
			insertIndex(index);
			// check density
			if (valueCount / (double) ensuredCount > getMaximalDensity(row)) {
				tooFull = true;
			}
			// fill bloom filter
			int hash = hashForBloom(row);
			bloomFilter[hash >>> 3] |= 1 << (hash & 7);
		}
		// set index
		indices[index] = row;
		// set value in base column
		setValue(index, value);
		return tooFull;
	}

	/**
	 * Returns the maximal density of this chunk depending on whether it stores integer or double
	 * values.
	 *
	 * @return the maximal density for this chunk
	 */
	protected abstract double getMaximalDensity(int row);

	/**
	 * The index returned by binary search or -1 if lookup is {@code true} and mayContain returns
	 * {@link false}. Returns only a positive number if the row was found in {@link #indices}. If
	 * called with lookup {@code false} then a returned negative index encodes where to insert this
	 * new row (see {@link Arrays#binarySearch}).
	 *
	 * @param row
	 *            the row to search for
	 * @param lookup
	 *            whether to look in the bloom filter, use {@code true} if the exact negative index
	 *            is not necessary
	 * @return the index where row is found or a negative index
	 */
	private int getIndex(int row, boolean lookup) {
		// if no row inserted yet, return
		// bloom filter first, if only lookup
		if (valueCount == 0 || lookup && !mayContain(row)) {
			return -1;
		}
		// if new row is bigger than the biggest or no row inserted yet,
		// no binary search is necessary
		if (row > indices[valueCount - 1]) {
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
	 * Checks the bloom filter if the set of rows might contain this row.
	 *
	 * @param row
	 *            the row to look for
	 * @return {@code false} if this row was never added before, {@code true} if it is unknown
	 */
	private boolean mayContain(int row) {
		int hash = hashForBloom(row);
		return (bloomFilter[hash >>> 3] & 1 << (hash & 7)) != 0;
	}

	/**
	 * Calculates as hash value for row using the multiply-shift hash of Dietzfelbinger.
	 */
	private int hashForBloom(int row) {
		// h: [2^w] -> [2^l], h(x) = (a*x mod 2^w) / 2^(w-l) = (a*x & (2^w -1))>>>(w-l)
		return (bloomMult * row & MAX_SIZE) >>> bloomShift;
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
		growBloomFilter();
		int length = indices.length;
		if (valueCount < length) {
			return indices;
		}
		// grow
		int newLength = length == 0 ? MIN_NON_EMPTY_SIZE : length + (length >> 1);
		return new int[newLength];
	}

	/**
	 * Ensures that the {@link #bloomFilter} contains at least twice as many bits as the value
	 * count. If growing the bloom filter is necessary the values for the hash function are
	 * recalculated and the bloom filter is rehashed.
	 */
	private void growBloomFilter() {
		int length = bloomFilter.length;
		// maintain 1:2 relation for inserted indices vs bloom filter size
		if (valueCount >> 2 < length) {
			return;
		}
		length = length == 0 ? 1 : length << 1;
		bloomFilter = new byte[length];
		bloomShift--;
		bloomMult = (int) Math.pow(2, bloomShift) + 1;
		// rehash bloom filter
		for (int i = 0; i < valueCount; i++) {
			int hash = hashForBloom(indices[i]);
			bloomFilter[hash >>> 3] |= 1 << (hash & 7);
		}
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
