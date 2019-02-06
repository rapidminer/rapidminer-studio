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
package com.rapidminer.operator.nio;

import javax.swing.table.AbstractTableModel;

import com.rapidminer.tools.Tools;

import jxl.Sheet;


/**
 * Returns values backed by an operned excel workbook.
 *
 * Note: This model used to be created in
 * {@link ExcelResultSetConfiguration#makePreviewTableModel(com.rapidminer.tools.ProgressListener),
 * but this lead to problems because effects like empty columns or rows (e.g.
 * {@link ExcelResultSet#emptyColumns) were not respected.}
 *
 * @author Simon Fischer
 *
 */
public class ExcelSheetTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private Sheet sheet;

	public ExcelSheetTableModel(Sheet sheet) {
		this.sheet = sheet;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return sheet.getCell(columnIndex, rowIndex).getContents();
	}

	@Override
	public int getRowCount() {
		try {
			// library contains a bug where it throws an NPE in
			// jxl.read.biff.Record.<init>(Record.java:79)
			// catch it and return 0 if that happenes, otherwise there is an NPE in the EDT
			// which is really bad
			return sheet.getRows();
		} catch (NullPointerException e) {
			return 0;
		}

	}

	@Override
	public int getColumnCount() {
		return sheet.getColumns();
	}

	@Override
	public String getColumnName(int columnIndex) {
		return Tools.getExcelColumnName(columnIndex);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
}
