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

import com.rapidminer.operator.visualization.dependencies.NumericalMatrix;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * This class can be used to use a symmetrical matrix as data table. The data is directly read from
 * the symmetrical matrix (e.g. a correlation matrix) instead of building a copy. Please note that
 * the method for adding new rows is not supported by this type of data tables.
 * 
 * @author Ingo Mierswa
 */
public class DataTableSymmetricalMatrixAdapter extends AbstractDataTable {

	private NumericalMatrix matrix;

	private String[] index2NameMap;

	private Map<String, Integer> name2IndexMap = new HashMap<String, Integer>();

	public DataTableSymmetricalMatrixAdapter(NumericalMatrix matrix, String name, String[] columnNames) {
		super(name);
		this.matrix = matrix;
		this.index2NameMap = columnNames;
		for (int i = 0; i < this.index2NameMap.length; i++) {
			this.name2IndexMap.put(this.index2NameMap[i], i);
		}
	}

	@Override
	public int getNumberOfSpecialColumns() {
		return 0;
	}

	@Override
	public boolean isSpecial(int index) {
		return false;
	}

	@Override
	public boolean isNominal(int index) {
		return index == 0;
	}

	@Override
	public boolean isDate(int index) {
		return false;
	}

	@Override
	public boolean isTime(int index) {
		return false;
	}

	@Override
	public boolean isDateTime(int index) {
		return false;
	}

	@Override
	public boolean isNumerical(int index) {
		return index != 0;
	}

	@Override
	public String mapIndex(int column, int value) {
		return this.index2NameMap[value];
	}

	/**
	 * Please note that this method does not map new strings but is only able to deliver strings
	 * which where already known during construction.
	 */
	@Override
	public int mapString(int column, String value) {
		Integer result = this.name2IndexMap.get(value);
		if (result == null) {
			return -1;
		} else {
			return result;
		}
	}

	@Override
	public int getNumberOfValues(int column) {
		if (column == 0) {
			return this.index2NameMap.length;
		} else {
			return -1;
		}
	}

	@Override
	public String getColumnName(int i) {
		if (i == 0) {
			return "Attributes";
		} else {
			return this.index2NameMap[i - 1];
		}
	}

	@Override
	public int getColumnIndex(String name) {
		if (name.equals("Attributes")) {
			return 0;
		} else {
			return mapString(0, name) + 1;
		}
	}

	@Override
	public boolean isSupportingColumnWeights() {
		return false;
	}

	@Override
	public double getColumnWeight(int column) {
		return Double.NaN;
	}

	@Override
	public int getNumberOfColumns() {
		return this.index2NameMap.length + 1;
	}

	@Override
	public void add(DataTableRow row) {
		throw new RuntimeException("DataTableCorrelationMatrixAdapter: adding new rows is not supported!");
	}

	@Override
	public DataTableRow getRow(int index) {
		return new CorrelationMatrixRow2DataTableRowWrapper(this.matrix, index);
	}

	@Override
	public Iterator<DataTableRow> iterator() {
		return new CorrelationMatrixRow2DataTableRowIterator(this.matrix);
	}

	@Override
	public int getNumberOfRows() {
		return this.index2NameMap.length;
	}

	/**
	 * Not implemented!!! Please use this class only for plotting purposes if you can ensure that
	 * the number of columns / rows is small.
	 */
	@Override
	public DataTable sample(int newSize) {
		return this;
	}
}
