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
package com.rapidminer.gui.properties;

import com.rapidminer.parameter.ParameterType;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;


/**
 * Table model returning values from the list of string pairs. Column types are determined by
 * {@link ParameterType}s.
 * 
 * @author Simon Fischer
 */
public class ListTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	private List<String[]> parameterList;
	private ParameterType[] types;

	public ListTableModel(ParameterType[] types, List<String[]> parameterList) {
		super();
		this.types = types;
		this.parameterList = new ArrayList<String[]>(parameterList);
	}

	@Override
	public String getColumnName(int column) {
		return types[column].getKey().replace('_', ' ');
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}

	@Override
	public int getRowCount() {
		return parameterList.size();
	}

	@Override
	public int getColumnCount() {
		return types.length;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		ParameterType columnType = types[columnIndex];
		if (aValue == null) {
			parameterList.get(rowIndex)[columnIndex] = columnType.getDefaultValueAsString();
		} else {
			parameterList.get(rowIndex)[columnIndex] = aValue.toString();
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return parameterList.get(rowIndex)[columnIndex];
	}

	void addRow() {
		String[] initialValues = new String[types.length];
		for (int i = 0; i < initialValues.length; i++) {
			initialValues[i] = types[i].getDefaultValueAsString();
		}
		parameterList.add(initialValues);
		fireTableRowsInserted(parameterList.size() - 1, parameterList.size() - 1);
	}

	public void removeRow(int selectedRow) {
		parameterList.remove(selectedRow);
		// fireTableStructureChanged();
		fireTableRowsDeleted(selectedRow, selectedRow);
	}

	public List<String[]> getParameterList() {
		return parameterList;
	}

}
