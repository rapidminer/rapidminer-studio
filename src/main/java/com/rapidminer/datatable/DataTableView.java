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
 * This class is a view on a DataTable which hides all examples not listed in an index list.
 * 
 * Set the list of selected examples via {@link #setSelectedIndices(Vector)}.
 * 
 * @author Marius Helf
 * 
 */
public class DataTableView extends AbstractDataTable implements DataTableListener {

	private final DataTable parentTable;
	private Vector<Integer> selectedIndices = null;
	private int numberOfSelectedRows;

	public DataTableView(DataTable parentDataTable) {
		super(parentDataTable.getName());

		this.parentTable = parentDataTable;

		// building initial selected indices: All
		numberOfSelectedRows = parentTable.getNumberOfRows();

		parentTable.addDataTableListener(this, true);

	}

	public DataTable getParentTable() {
		return parentTable;
	}

	public void setSelectedIndices(Vector<Integer> selectedIndices) {
		this.selectedIndices = selectedIndices;
		if (selectedIndices != null) {
			numberOfSelectedRows = selectedIndices.size();
		} else {
			numberOfSelectedRows = parentTable.getRowNumber();
		}
		fireEvent();
	}

	@Override
	public Iterator<DataTableRow> iterator() {
		return new Iterator<DataTableRow>() {

			int nextRow = 0;

			@Override
			public boolean hasNext() {
				return nextRow < numberOfSelectedRows;
			}

			@Override
			public DataTableRow next() {
				DataTableRow row = getRow(nextRow);
				nextRow++;
				return row;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("remove() not suppported by FilterDataTable");
			}
		};
	}

	@Override
	public int getNumberOfRows() {
		return numberOfSelectedRows;
	}

	@Override
	public DataTableRow getRow(int index) {
		if (index < numberOfSelectedRows) {
			if (selectedIndices == null) {
				return parentTable.getRow(index);
			} else {
				return parentTable.getRow(selectedIndices.get(index));
			}
		} else {
			throw new ArrayIndexOutOfBoundsException("Index exceeds filtered range: " + index);
		}
	}

	/*
	 * Delegating methods
	 */
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
	 * Creates a view onto this DataTableView (i.e. the this DataTableView won't get garbage
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

		return sampledDataTable;
	}

	/**
	 * Resets the selected indices and then contains all rows. Anything else is not possible since
	 * there does not exist any rule how to update the view.
	 * 
	 * Subclasses like {@link FilteredDataTable} should implement a smarter version of this
	 * function.
	 */
	@Override
	public void dataTableUpdated(DataTable source) {
		selectedIndices = null;
		numberOfSelectedRows = source.getRowNumber();
		fireEvent();
	}
}
