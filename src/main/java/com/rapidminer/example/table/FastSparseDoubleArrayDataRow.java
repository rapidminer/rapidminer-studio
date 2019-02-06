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
package com.rapidminer.example.table;

import com.rapidminer.example.Tools;

import java.util.Arrays;


/**
 * This implementation of a sparse DataRow makes use of the binary search index to maintain a sorted
 * list of indices to save the effort of re sorting on adding a new index.
 * 
 * @author Sebastian Land, Tobias Malbrecht
 */
public class FastSparseDoubleArrayDataRow extends DataRow {

	private static final long serialVersionUID = -4709836940912838649L;

	private double[] values;
	private int[] tableIndices;

	private int numberOfValues = 0;

	public FastSparseDoubleArrayDataRow(int initialSize) {
		this.values = new double[initialSize];
		this.tableIndices = new int[initialSize];
	}

	@Override
	protected double get(int index, double defaultValue) {
		int valueIndex = Arrays.binarySearch(tableIndices, 0, numberOfValues, index);
		if (valueIndex >= 0) {
			return values[valueIndex];
		}
		return defaultValue;
	}

	@Override
	protected synchronized void set(int index, double value, double defaultValue) {
		assert numberOfValues <= tableIndices.length;
		assert tableIndices.length == values.length;
		int valueIndex = Arrays.binarySearch(tableIndices, 0, numberOfValues, index);
		if (valueIndex > 0) {
			// if index already has a value: test if new values is equal to defaultValue
			if (Tools.isDefault(defaultValue, value)) {
				// new value is default
				if (valueIndex + 1 < values.length) {
					// if it is not last: copy all subsequent one in front
					System.arraycopy(values, valueIndex + 1, values, valueIndex, values.length - valueIndex - 1);
					System.arraycopy(tableIndices, valueIndex + 1, tableIndices, valueIndex, values.length - valueIndex - 1);
				}
				numberOfValues--;
			} else {
				// old value must simply be overridden
				values[valueIndex] = value;
			}
		} else if (!Tools.isDefault(defaultValue, value)) {
			// index is unknown and value different from defaultValue, we have to insert it
			int insertionIndex = -(valueIndex + 1);
			if (numberOfValues == tableIndices.length) {
				// no space left: enlarge array
				double[] newValues = new double[values.length + (values.length >> 1) + 1];
				int[] newTableIndices = new int[values.length + (values.length >> 1) + 1];
				// copy the two halves before and after the insertion point
				System.arraycopy(values, 0, newValues, 0, insertionIndex);
				System.arraycopy(values, insertionIndex, newValues, insertionIndex + 1, numberOfValues - insertionIndex);
				System.arraycopy(tableIndices, 0, newTableIndices, 0, insertionIndex);
				System.arraycopy(tableIndices, insertionIndex, newTableIndices, insertionIndex + 1, numberOfValues
						- insertionIndex);
				values = newValues;
				tableIndices = newTableIndices;
			} else {
				// enough space: shift subsequent arrays
				System.arraycopy(values, insertionIndex, values, insertionIndex + 1, numberOfValues - insertionIndex);
				System.arraycopy(tableIndices, insertionIndex, tableIndices, insertionIndex + 1, numberOfValues
						- insertionIndex);
			}
			// finally setting value
			tableIndices[insertionIndex] = index;
			values[insertionIndex] = value;

			// and increase counter
			numberOfValues++;
		}
	}

	@Override
	public synchronized void trim() {
		values = Arrays.copyOfRange(values, 0, numberOfValues);
		tableIndices = Arrays.copyOfRange(tableIndices, 0, numberOfValues);
	}

	@Override
	protected void ensureNumberOfColumns(int numberOfColumns) {
		// Do nothing since a runtime exception is never thrown: Default value is returned for
		// unknown indices anyway.
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < tableIndices.length; i++) {
			if (i != 0) {
				result.append(", ");
			}
			result.append(tableIndices[i] + ":" + values[i]);
		}
		result.append(", counter: " + numberOfValues);
		return result.toString();
	}

	@Override
	public int getType() {
		return DataRowFactory.TYPE_DOUBLE_SPARSE_ARRAY;
	}
}
