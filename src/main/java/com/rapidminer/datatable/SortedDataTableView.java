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

import java.util.Iterator;
import java.util.Random;
import java.util.Vector;


/**
 * A view on a parent DataTable which maps the row indices such that the view is sorted. The sort
 * order is defined by a DataTableSortProvider.
 * 
 * @author Marius Helf, Nils Woehler
 */
public class SortedDataTableView extends AbstractDataTable implements DataTableListener {

	private DataTable parentTable;
	private DataTableSortProvider sortProvider;
	private int[] indexMappingCache = null;

	public SortedDataTableView(DataTable parentDataTable, DataTableSortProvider sortProvider) {
		super(parentDataTable.getName());
		this.parentTable = parentDataTable;
		this.sortProvider = sortProvider;
		parentTable.addDataTableListener(this);
	}

	@Override
	public Iterator<DataTableRow> iterator() {
		return new Iterator<DataTableRow>() {

			int nextRow = 0;

			@Override
			public boolean hasNext() {
				return nextRow < getNumberOfRows();
			}

			@Override
			public DataTableRow next() {
				DataTableRow row = getRow(nextRow);
				nextRow++;
				return row;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("remove() not suppported by SortedDataTableView");
			}
		};
	}

	@Override
	public DataTableRow getRow(int index) {
		if (indexMappingCache == null) {
			updateIndexMapping();
		}
		return parentTable.getRow(indexMappingCache[index]);
	}

	private void updateIndexMapping() {
		if (sortProvider != null) {
			indexMappingCache = sortProvider.getIndexMapping(parentTable);
		} else {
			int rowCount = getRowNumber();
			indexMappingCache = new int[rowCount];
			for (int i = 0; i < rowCount; ++i) {
				indexMappingCache[i] = i;
			}
		}
	}

	public DataTableSortProvider getSortProvider() {
		return sortProvider;
	}

	/**
	 * Sets the new sort provider, invalidates cache and informs all listeners.
	 */
	public void setSortProvider(DataTableSortProvider sortProvider) {
		if (this.sortProvider != sortProvider) {
			invalidateIndexMapping();
			this.sortProvider = sortProvider;
			fireEvent();
		}
	}

	private void invalidateIndexMapping() {
		indexMappingCache = null;
	}

	/*
	 * Delegating methods
	 */
	@Override
	public int getNumberOfRows() {
		return parentTable.getNumberOfRows();
	}

	@Override
	public void add(DataTableRow row) {
		parentTable.add(row);
	}

	@Override
	public int getColumnIndex(String name) {
		return parentTable.getColumnIndex(name);
	}

	@Override
	public String getColumnName(int i) {
		return parentTable.getColumnName(i);
	}

	@Override
	public double getColumnWeight(int i) {
		return parentTable.getColumnWeight(i);
	}

	@Override
	public int getNumberOfColumns() {
		return parentTable.getNumberOfColumns();
	}

	@Override
	public int getNumberOfSpecialColumns() {
		return parentTable.getNumberOfSpecialColumns();
	}

	@Override
	public int getNumberOfValues(int column) {
		return parentTable.getNumberOfValues(column);
	}

	@Override
	public boolean isDate(int index) {
		return parentTable.isDate(index);
	}

	@Override
	public boolean isDateTime(int index) {
		return parentTable.isDateTime(index);
	}

	@Override
	public boolean isNominal(int index) {
		return parentTable.isNominal(index);
	}

	@Override
	public boolean isNumerical(int index) {
		return parentTable.isNumerical(index);
	}

	@Override
	public boolean isSpecial(int column) {
		return parentTable.isSpecial(column);
	}

	@Override
	public boolean isSupportingColumnWeights() {
		return parentTable.isSupportingColumnWeights();
	}

	@Override
	public boolean isTime(int index) {
		return parentTable.isTime(index);
	}

	@Override
	public String mapIndex(int column, int index) {
		return parentTable.mapIndex(column, index);
	}

	@Override
	public int mapString(int column, String value) {
		return parentTable.mapString(column, value);
	}

	/**
	 * Performs a simple sampling without replacement. If newSize is greater than the size of this
	 * DataTableView, this DataTableView is returned.
	 * 
	 * Creates a view onto this SortedDataTableView (i.e. this SortedDataTableView won't get garbage
	 * collected as long as the sampled DataTableView is around.)
	 */
	@Override
	public DataTable sample(int newSize) {
		int rowCount = getRowNumber();
		if (rowCount <= newSize) {
			return this;
		}

		// initialize sampled indices
		int[] sampledSelectedIndices = new int[rowCount];
		for (int i = 0; i < rowCount; ++i) {
			sampledSelectedIndices[i] = i;
		}

		// shuffle sampled indices
		Random rng = new Random(0);
		int swapIdx;
		int tmpValue;
		for (int i = 0; i < rowCount; ++i) {
			swapIdx = rng.nextInt(rowCount);
			tmpValue = sampledSelectedIndices[swapIdx];
			sampledSelectedIndices[swapIdx] = sampledSelectedIndices[i];
			sampledSelectedIndices[i] = tmpValue;
		}

		// convert prefix of sampled indices to vector and set as selected indices for sampled data
		// table view
		DataTableView sampledDataTable = new DataTableView(this);
		Vector<Integer> sampledSelectedIndicesVector = new Vector<Integer>(newSize);
		for (int i = 0; i < newSize; ++i) {
			sampledSelectedIndicesVector.add(sampledSelectedIndices[i]);
		}
		sampledDataTable.setSelectedIndices(sampledSelectedIndicesVector);

		return new SortedDataTableView(sampledDataTable, sortProvider);
	}

	@Override
	public void dataTableUpdated(DataTable source) {
		invalidateIndexMapping();	// invalidate cache
		fireEvent();
	}
}
