/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel;

import com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel.ExcelWorkbookPane.ExcelWorkbookSelection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import jxl.Sheet;


/**
 * 
 * @author Tobias Malbrecht
 */
public class ExcelTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -2650777734917514059L;

	private Sheet excelSheet;

	private ExcelWorkbookSelection reductionSelection;

	private List<String> columnNames;

	private final Map<Integer, String> annotationTypes = new HashMap<Integer, String>();

	public ExcelTableModel(Sheet sheet) {
		excelSheet = sheet;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 0;
	}

	@Override
	public int getColumnCount() {
		if (reductionSelection == null) {
			return excelSheet.getColumns() + 1;
		} else {
			return reductionSelection.getColumnIndexEnd() - reductionSelection.getColumnIndexStart() + 1 + 1;
		}
	}

	@Override
	public int getRowCount() {
		if (reductionSelection == null) {
			return excelSheet.getRows();
		} else {
			return reductionSelection.getRowIndexEnd() - reductionSelection.getRowIndexStart() + 1;
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
			String value = getAnnotationMap().get(rowIndex);
			if (value == null) {
				return AnnotationCellEditor.NONE;
			} else {
				return value;
			}
		}
		columnIndex--;

		if (reductionSelection == null) {
			return excelSheet.getCell(columnIndex, rowIndex).getContents();
		} else {
			return excelSheet.getCell(columnIndex + reductionSelection.getColumnIndexStart(),
					rowIndex + reductionSelection.getRowIndexStart()).getContents();
		}
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
			if (AnnotationCellEditor.NONE.equals(aValue)) {
				getAnnotationMap().remove(rowIndex);
			} else {
				getAnnotationMap().put(rowIndex, (String) aValue);
			}
			fireTableCellUpdated(rowIndex, columnIndex);
		}
	}

	@Override
	public String getColumnName(int column) {
		if (column == 0) {
			return "Use as";
		}
		column--;
		if (columnNames == null) {
			if (reductionSelection != null) {
				column += reductionSelection.getColumnIndexStart();
			}
			StringBuffer buffer = new StringBuffer();
			int currentNumber = column % 26;
			buffer.append(((char) (currentNumber + 65)));
			column -= currentNumber;
			while (column > 0) {
				column /= 26;
				currentNumber = column % 26;
				buffer.append(((char) (currentNumber + 65)));
				column -= currentNumber;
			}
			return buffer.toString();
		} else {
			return columnNames.get(column);
		}
	}

	public void createView(ExcelWorkbookSelection selection) {
		reductionSelection = selection;
	}

	public void resetReduction() {
		reductionSelection = null;
	}

	public void setNames(List<String> names) {
		columnNames = names;
	}

	public void resetNames() {
		columnNames = null;
	}

	public Map<Integer, String> getAnnotationMap() {
		return annotationTypes;
	}

}
