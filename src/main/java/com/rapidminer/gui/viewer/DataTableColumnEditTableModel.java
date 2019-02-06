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
package com.rapidminer.gui.viewer;

import com.rapidminer.datatable.DataTable;

import java.util.List;


/**
 * The model for the {@link com.rapidminer.gui.viewer.DataTableViewerTable}.
 * 
 * @author Ingo Mierswa
 */
public class DataTableColumnEditTableModel extends DataTableViewerTableModel {

	private static final long serialVersionUID = 8116530590493627673L;

	private transient DataTable dataTable;
	private List<String> editableColumnNames;
	private Object[][] editableColumnValues;

	public DataTableColumnEditTableModel(DataTable dataTable, List<String> editableColumnNames) {
		super(dataTable);
		this.dataTable = dataTable;
		this.editableColumnNames = editableColumnNames;
		editableColumnValues = new Object[editableColumnNames.size()][dataTable.getNumberOfRows()];
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return (columnIndex < editableColumnNames.size());
	}

	@Override
	public Class<?> getColumnClass(int column) {
		if (column < editableColumnNames.size()) {
			return Object.class;
		}
		return super.getColumnClass(column - editableColumnNames.size());
	}

	@Override
	public int getRowCount() {
		return dataTable.getNumberOfRows();
	}

	@Override
	public int getColumnCount() {
		return dataTable.getNumberOfColumns() + editableColumnNames.size();
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex < editableColumnNames.size()) {
			editableColumnValues[columnIndex][rowIndex] = aValue;
		}
		super.setValueAt(aValue, rowIndex, columnIndex - editableColumnNames.size());
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (col < editableColumnNames.size()) {
			return editableColumnValues[col][row];
		}
		return super.getValueAt(row, col - editableColumnNames.size());
	}

	@Override
	public String getColumnName(int col) {
		if (col < editableColumnNames.size()) {
			return editableColumnNames.get(col);
		}
		return super.getColumnName(col - editableColumnNames.size());
	}

	public Object[] getEnteredValues(int column) {
		return editableColumnValues[column];
	}
}
