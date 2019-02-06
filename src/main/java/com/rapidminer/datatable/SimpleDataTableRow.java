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
package com.rapidminer.datatable;

import java.io.Serializable;


/**
 * A data list that contains Object arrays that record process results or other data. Each row can
 * consist of an id and an object array which represents the data.
 * 
 * @author Ingo Mierswa, Marius Helf
 */
public class SimpleDataTableRow implements DataTableRow, Serializable {

	private static final long serialVersionUID = 1L;

	private double[] row;

	private String id;

	/**
	 * Creates a SimpleDataTableRow with the same values as other.
	 */
	public SimpleDataTableRow(DataTableRow other) {
		copyValuesFromOtherRow(other);
	}

	public SimpleDataTableRow(double[] row) {
		this(row, null);
	}

	public SimpleDataTableRow(double[] row, String id) {
		this.row = row;
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public double getValue(int index) {
		return row[index];
	}

	@Override
	public int getNumberOfValues() {
		return row.length;
	}

	public void copyValuesFromOtherRow(DataTableRow other) {
		int numberOfValues = other.getNumberOfValues();
		row = new double[numberOfValues];
		for (int i = 0; i < numberOfValues; ++i) {
			row[i] = other.getValue(i);
		}
	}
}
