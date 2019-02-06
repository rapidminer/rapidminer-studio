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

import java.util.Arrays;

import com.rapidminer.tools.Ontology;


/**
 * This implementation of {@link Column} uses an internal byte array to store up to four values in
 * one byte and is usually used for bi-nominal attributes (see {@link Ontology#BINOMINAL}). To allow
 * for missing values, one value takes up two bits instead of just one.
 *
 * @author Jan Czogalla
 * @see Column
 * @see ColumnarExampleTable
 *
 */
class ByteArrayColumn implements Column {

	private static final long serialVersionUID = 1L;

	/** mask for x % 4 (same as x & 3) */
	private static final int MOD_FOUR_MASK = 0b11;

	/** mask for return value, getting the last 2 bits */
	private static final int GETTER_MASK = 0b11;

	/** mask for keeping all unaffected values */
	private static final int VALUE_BASE_MASK = 0b11111100111111;

	/** shift offset for {@link #VALUE_BASE_MASK} */
	private static final int SHIFT_OFFSET = 6;

	/** bit pattern that represents a binary missing value */
	private static final int BYTE_NAN = 0b10;

	private byte[] data;

	/** Creates a new {@code ByteArrayColumn} with a capacity for {@code size} boolean values. */
	ByteArrayColumn(int size) {
		data = new byte[byteSize(size)];
	}

	@Override
	public double get(int row) {
		// get the value from the packed byte
		// shift the byte to the right corresponding to the row subindex,
		// then get the last two bits
		int value = data[row >> 2] >>> ((row & MOD_FOUR_MASK) << 1) & GETTER_MASK;
		return value == BYTE_NAN ? Double.NaN : value;
	}

	@Override
	public void set(int row, double value) {
		// get the byte the old value is packed into
		int oldvalue = data[row >> 2];
		// calculate shift by row subindex
		int shift = (row & MOD_FOUR_MASK) << 1;
		// get mask to keep old values
		// after shifting, all bits are "1" except for the two bits to be set
		int mask = VALUE_BASE_MASK >> SHIFT_OFFSET - shift;
		// shift new value into position
		int newValue = Double.isNaN(value) ? BYTE_NAN : (int) value;
		newValue <<= shift;
		// combine old surrounding values with new value
		data[row >> 2] = (byte) (oldvalue & mask | newValue);
	}

	@Override
	public void ensure(int size) {
		data = Arrays.copyOf(data, byteSize(size));
	}

	/**
	 * Returns the required size for the data array.<br/>
	 * Effectively calculates {@code ceiling(size/4)}.
	 *
	 * @return the size for the data array
	 */
	private int byteSize(int size) {
		return size > 0 ? (size >> 2) + ((size & MOD_FOUR_MASK) > 0 ? 1 : 0) : 0;
	}

	@Override
	public void setLast(int row, double value) {
		set(row, value);
	}

}
