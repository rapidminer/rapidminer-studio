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
import com.rapidminer.datatable.DataTableRow;

import java.util.Date;

import javax.swing.table.AbstractTableModel;


/**
 * The model for the {@link com.rapidminer.gui.viewer.DataTableViewerTable}.
 * 
 * @author Ingo Mierswa
 */
public class DataTableViewerTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 8116530590493627673L;

	private transient DataTable dataTable;

	public DataTableViewerTableModel(DataTable dataTable) {
		this.dataTable = dataTable;
	}

	@Override
	public Class<?> getColumnClass(int column) {
		Class<?> type = super.getColumnClass(column);
		if ((dataTable.isDate(column)) || (dataTable.isTime(column)) || (dataTable.isDateTime(column))) {
			type = Date.class;
		} else if (dataTable.isNumerical(column)) {
			type = Double.class;
		} else {
			type = String.class;
		}
		return type;
	}

	@Override
	public int getRowCount() {
		return dataTable.getNumberOfRows();
	}

	@Override
	public int getColumnCount() {
		return dataTable.getNumberOfColumns();
	}

	@Override
	public Object getValueAt(int row, int col) {
		DataTableRow tableRow = dataTable.getRow(row);
		if (dataTable.isDate(col) || dataTable.isTime(col) || dataTable.isDateTime(col)) {
			double value = tableRow.getValue(col);
			long milliseconds = (long) value;
			return new Date(milliseconds);
		} else if (dataTable.isNominal(col)) {
			return dataTable.getValueAsString(tableRow, col);
		} else {
			return tableRow.getValue(col);
		}
	}

	@Override
	public String getColumnName(int col) {
		return dataTable.getColumnName(col);
	}
}
