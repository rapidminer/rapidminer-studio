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
 * This class can be used to use all pairs (entries) of a numerical matrix as data table. The data
 * is directly read from the numerical matrix instead of building a copy. If the matrix is
 * symmetrical, only pairs of the lower left triangle are returned. Please note that the method for
 * adding new rows is not supported by this type of data tables.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public class DataTablePairwiseMatrixExtractionAdapter extends AbstractDataTable {

	private NumericalMatrix matrix;

	private String[] rowNames;

	private Map<String, Integer> rowName2IndexMap = new HashMap<String, Integer>();

	private String[] columnNames;

	private Map<String, Integer> columnName2IndexMap = new HashMap<String, Integer>();

	private String[] tableColumnNames;

	private boolean showSymetrical;

	public DataTablePairwiseMatrixExtractionAdapter(NumericalMatrix matrix, String[] rowNames, String[] columnNames,
			String[] tableColumnNames, boolean showSymetrical) {
		super("Pairwise Correlation Table");
		this.showSymetrical = showSymetrical;
		this.matrix = matrix;

		this.rowNames = rowNames;
		for (int i = 0; i < this.rowNames.length; i++) {
			this.rowName2IndexMap.put(this.rowNames[i], i);
		}

		this.columnNames = columnNames;
		for (int i = 0; i < this.columnNames.length; i++) {
			this.columnName2IndexMap.put(this.columnNames[i], i);
		}

		this.tableColumnNames = tableColumnNames;
		if ((this.tableColumnNames == null) || (this.tableColumnNames.length != 3)) {
			throw new RuntimeException(
					"Cannot create pairwise matrix extraction data table with other than 3 table column names.");
		}
	}

	/**
	 * Creates all pairs of one triangle of the given symetrical matrix.
	 */
	public DataTablePairwiseMatrixExtractionAdapter(NumericalMatrix matrix, String[] rowNames, String[] columnNames,
			String[] tableColumnNames) {
		this(matrix, rowNames, columnNames, tableColumnNames, true);
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
		return (index <= 1);
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
		return (index > 1);
	}

	@Override
	public String mapIndex(int column, int value) {
		if (column == 0) {
			return rowNames[value];
		} else if (column == 1) {
			return columnNames[value];
		} else {
			return "?";
		}
	}

	/**
	 * Please note that this method does not map new strings but is only able to deliver strings
	 * which where already known during construction.
	 */
	@Override
	public int mapString(int column, String value) {
		if (column == 0) {
			Integer result = this.rowName2IndexMap.get(value);
			if (result == null) {
				return -1;
			} else {
				return result;
			}
		} else if (column == 1) {
			Integer result = this.columnName2IndexMap.get(value);
			if (result == null) {
				return -1;
			} else {
				return result;
			}
		} else {
			return -1;
		}
	}

	@Override
	public int getNumberOfValues(int column) {
		if (column == 0) {
			return rowNames.length;
		} else if (column == 1) {
			return columnNames.length;
		} else {
			return -1;
		}
	}

	@Override
	public String getColumnName(int i) {
		return tableColumnNames[i];
	}

	@Override
	public int getColumnIndex(String name) {
		for (int i = 0; i < tableColumnNames.length; i++) {
			if (tableColumnNames[i].equals(name)) {
				return i;
			}
		}
		return -1;
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
		return tableColumnNames.length;
	}

	@Override
	public void add(DataTableRow row) {
		throw new RuntimeException("DataTablePairwiseCorrelationMatrixAdapter: adding new rows is not supported!");
	}

	@Override
	public DataTableRow getRow(int rowIndex) {
		if (matrix.isSymmetrical() && showSymetrical) {
			int firstAttribute = 0;
			int secondAttribute = 1;
			for (int i = 0; i < rowIndex; i++) {
				secondAttribute++;
				if (secondAttribute >= matrix.getNumberOfColumns()) {
					firstAttribute++;
					secondAttribute = firstAttribute + 1;
				}
			}
			return new PairwiseMatrix2DataTableRowWrapper(this.matrix, firstAttribute, secondAttribute);
		} else {
			return new PairwiseMatrix2DataTableRowWrapper(this.matrix, rowIndex / matrix.getNumberOfColumns(), rowIndex
					% matrix.getNumberOfColumns());
		}
	}

	@Override
	public Iterator<DataTableRow> iterator() {
		return new PairwiseMatrix2DataTableRowIterator(this.matrix, showSymetrical);
	}

	@Override
	public int getNumberOfRows() {
		if (matrix.isSymmetrical() && showSymetrical) {
			return ((rowNames.length * rowNames.length) - rowNames.length) / 2;
		} else {
			return (matrix.getNumberOfRows() * matrix.getNumberOfColumns());
		}
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
