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

/**
 * Implementation of DataRow that is backed by primitive arrays. Should always be used if more than
 * 50% of the data is sparse. As fast (or even faster than map implementation) but needs
 * considerably less memory. This implementation uses boolean arrays instead of double arrays which
 * will reduce the used memory even more.
 * 
 * @author Ingo Mierswa
 */
public class BooleanSparseArrayDataRow extends AbstractSparseArrayDataRow {

	private static final long serialVersionUID = -4373978993763834478L;

	/** Stores the used attribute values. */
	private boolean[] values;

	/** Creates an empty sparse array data row with size 0. */
	public BooleanSparseArrayDataRow() {
		this(0);
	}

	/** Creates a sparse array data row of the given size. */
	public BooleanSparseArrayDataRow(int size) {
		super(size);
		values = new boolean[size];
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	@Override
	protected void swapValues(int a, int b) {
		boolean tt = values[a];
		values[a] = values[b];
		values[b] = tt;
	}

	@Override
	protected void resizeValues(int length) {
		boolean[] d = new boolean[length];
		System.arraycopy(values, 0, d, 0, Math.min(values.length, length));
		values = d;
	}

	@Override
	protected void removeValue(int index) {
		System.arraycopy(values, index + 1, values, index, values.length - (index + 1));
	}

	/** Returns the desired data for the given attribute. */
	@Override
	protected double getValue(int index) {
		return values[index] ? 1.0d : 0.0d;
	}

	/** Sets the given data for the given attribute. */
	@Override
	protected void setValue(int index, double v) {
		values[index] = v == 0.0d ? false : true;
	}

	@Override
	protected double[] getAllValues() {
		double[] result = new double[this.values.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = this.values[i] ? 1.0d : 0.0d;
		}
		return result;
	}

	@Override
	public int getType() {
		return DataRowFactory.TYPE_BOOLEAN_SPARSE_ARRAY;
	}
}
