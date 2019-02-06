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

import java.util.Arrays;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;


/**
 * Removes rows from a table model.
 * 
 * @author Simon Fischer
 * 
 */
public class RowFilteringTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private TableModel wrappedModel;
	private boolean enabled = false;

	private int[] rowMap;

	public RowFilteringTableModel(TableModel wrappedModel, int[] rowMap, boolean enabled) {
		this.rowMap = rowMap;
		this.wrappedModel = wrappedModel;
		this.enabled = enabled;
		wrappedModel.addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {
				TableModelEvent translated;
				translated = new TableModelEvent(RowFilteringTableModel.this, translateRow(e.getFirstRow()), translateRow(e
						.getLastRow()), e.getColumn(), e.getType());
				fireTableChanged(translated);
			}
		});
	}

	/** Translates the row index in this table to the row index in the wrapped model. */
	protected int translateRow(int row) {
		if (enabled) {
			return rowMap[row];
		} else {
			return row;
		}
	}

	/** Translates the row index in the wrapped model to the row index in this model. */
	protected int inverseTranslateRow(int row) {
		if (enabled) {
			return Arrays.binarySearch(rowMap, row);
		} else {
			return row;
		}
	}

	@Override
	public int getRowCount() {
		if (enabled) {
			return rowMap.length;
		} else {
			return wrappedModel.getRowCount();
		}
	}

	@Override
	public int getColumnCount() {
		return wrappedModel.getColumnCount();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return wrappedModel.getValueAt(translateRow(rowIndex), columnIndex);
	}

	@Override
	public String getColumnName(int column) {
		return wrappedModel.getColumnName(column);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return wrappedModel.getColumnClass(columnIndex);
	}

	public void setWrappedModel(TableModel wm, int[] rowMap) {
		int oldSize = getRowCount();
		this.wrappedModel = wm;
		this.rowMap = rowMap;
		fireChange(oldSize);
	}

	public void setFilterEnabled(boolean enabled) {
		int oldSize = getRowCount();
		this.enabled = enabled;
		fireChange(oldSize);
	}

	private void fireChange(int oldSize) {
		if (getRowCount() > oldSize) {
			fireTableRowsInserted(oldSize, getRowCount());
		} else {
			fireTableRowsDeleted(getRowCount(), oldSize);
		}
		fireTableDataChanged();
	}
}
