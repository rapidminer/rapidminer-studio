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

/**
 * A small sparse chunk that stores integer values with high sparsity.
 *
 * @author Jan Czogalla
 * @since 7.3.1
 */
final class IntegerHighSparsityChunk extends AbstractHighSparsityChunk implements IntegerSparseChunk {

	private static final long serialVersionUID = 1L;

	private int[] data = AutoColumnUtils.EMPTY_INTEGER_ARRAY;

	IntegerHighSparsityChunk(double defaultValue) {
		super(defaultValue);
	}

	@Override
	void removeValueIndex(int index, int length) {
		int[] tmp = data;
		if (length != tmp.length) {
			tmp = new int[length];
		}
		AutoColumnUtils.copy(data, tmp, index, index + 1, index, valueCount);
		data = tmp;
	}

	@Override
	void insertValueIndex(int index, int length) {
		int[] tmp = data;
		if (length != tmp.length) {
			tmp = new int[length];
		}
		AutoColumnUtils.copy(data, tmp, index, index, index + 1, valueCount);
		data = tmp;
	}

	@Override
	double getValue(int index) {
		int value = data[index];
		return value == AutoColumnUtils.INTEGER_NAN ? Double.NaN : value;
	}

	@Override
	void setValue(int index, double value) {
		data[index] = Double.isNaN(value) ? AutoColumnUtils.INTEGER_NAN : (int) value;
	}

}
