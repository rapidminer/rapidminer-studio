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
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;


/**
 * A view on a parent DataTable which maps values from the parent table to other values. Can e.g. be
 * used to change the mapping of nominal values.
 * 
 * @author Marius Helf
 */
public class ValueMappingDataTableView extends AbstractDataTable implements DataTableListener {

	public static interface MappedDataTableListener {

		/**
		 * This method is called by a datatable, if its content is changed.
		 */
		public void informDataTableChange(DataTable dataTable);
	}

	private List<MappedDataTableListener> listeners = new LinkedList<MappedDataTableListener>();

	private DataTable parentTable;
	private Vector<DataTableMappingProvider> mappings;

	public ValueMappingDataTableView(DataTable parentDataTable) {
		super(parentDataTable.getName());
		this.parentTable = parentDataTable;

		// init mappings with nulls
		int columnCount = parentTable.getColumnNumber();
		mappings = new Vector<DataTableMappingProvider>(columnCount);
		for (int i = 0; i < columnCount; ++i) {
			mappings.add(null);
		}

		parentTable.addDataTableListener(this);
	}

	double mapValue(double originalValue, int columnIdx) {
		double mappedValue = originalValue;

		if (mappings.elementAt(columnIdx) != null) {
			mappedValue = mappings.elementAt(columnIdx).mapFromParentValue(originalValue);
		}

		return mappedValue;
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
				throw new UnsupportedOperationException("remove() not suppported by FilterDataTable");
			}
		};
	}

	@Override
	public DataTableRow getRow(int index) {
		return new ValueMappingDataTableRow(parentTable.getRow(index), this);
	}

	@Override
	public int getNumberOfRows() {
		return parentTable.getNumberOfRows();
	}

	public void setMappingProvider(int columnIdx, DataTableMappingProvider mapping) {
		if (mapping != mappings.elementAt(columnIdx)) {
			mappings.set(columnIdx, mapping);
			informMappedDataTableListeners();
			fireEvent();
		}
	}

	/*
	 * Listener Methods
	 */
	public void addMappedDataTableListener(MappedDataTableListener listener) {
		this.listeners.add(listener);
	}

	public void removeMappedDataTableListewner(MappedDataTableListener listener) {
		this.listeners.remove(listener);
	}

	private void informMappedDataTableListeners() {
		for (MappedDataTableListener listener : listeners) {
			listener.informDataTableChange(this);
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
		int parentIdx = index;
		DataTableMappingProvider mapping = mappings.elementAt(column);
		if (mapping != null) {
			if (mapping instanceof BidirectionalMappingProvider) {
				BidirectionalMappingProvider bidiMapping = (BidirectionalMappingProvider) mapping;
				parentIdx = (int) bidiMapping.mapToParentValue(index);
			} else {
				return null;
			}
		}
		return parentTable.mapIndex(column, parentIdx);
	}

	@Override
	public int mapString(int column, String value) {
		int index = parentTable.mapString(column, value);

		return (int) mapValue(index, column);
	}

	@Override
	public DataTable sample(int newSize) {
		// return parentTable.sample(newSize);
		return this;
	}

	@Override
	public void dataTableUpdated(DataTable source) {
		fireEvent();
	}

	public DataTable getParent() {
		return parentTable;
	}
}
