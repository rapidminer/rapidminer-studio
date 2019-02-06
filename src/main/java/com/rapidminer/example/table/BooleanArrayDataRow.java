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
 * Implementation of DataRow that is backed up by a boolean array. Please note that for almost no
 * practical applications the usage of this data row type is recommended. It might however come
 * handy in case of association rule learning or after a discretization step.
 * 
 * @author Ingo Mierswa
 */
public class BooleanArrayDataRow extends DataRow {

	private static final long serialVersionUID = -432265332489304646L;

	/** Holds the data for all attributes. */
	private boolean[] data;

	/** Creates a new data row backed by an primitive array. */
	public BooleanArrayDataRow(boolean[] data) {
		this.data = data;
	}

	/**
	 * Returns the value for the desired index. This method should only be used by attributes.
	 */
	@Override
	protected double get(int index, double defaultValue) {
		if (data[index]) {
			return 1.0d;
		} else {
			return 0.0d;
		}
	}

	/**
	 * Sets the value for the given index. This method should only be used by attributes.
	 */
	@Override
	protected synchronized void set(int index, double value, double defaultValue) {
		data[index] = value == 0.0d ? false : true;
	}

	/**
	 * Creates a new array of the given size if necessary and copies the data into the new array.
	 */
	@Override
	protected synchronized void ensureNumberOfColumns(int numberOfColumns) {
		if (data.length >= numberOfColumns) {
			return;
		}
		boolean[] newData = new boolean[numberOfColumns];
		System.arraycopy(data, 0, newData, 0, data.length);
		data = newData;
	}

	/** Returns a string representation of the data row. */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			result.append((i == 0 ? "" : ",") + data[i]);
		}
		return result.toString();
	}

	@Override
	public int getType() {
		return DataRowFactory.TYPE_BOOLEAN_ARRAY;
	}
}
