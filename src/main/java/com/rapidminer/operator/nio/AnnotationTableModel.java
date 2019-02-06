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

import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;


/**
 * Wraps around another table model and adds another column at position 0 for editing annotations.
 * The table should use an {@link AnnotationCellEditor} as an editor in column 0.
 * 
 * @author Simon Fischer
 * 
 */
public class AnnotationTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private TableModel wrappedModel;
	private Map<Integer, String> annotationsMap;

	public AnnotationTableModel(TableModel wrappedModel, Map<Integer, String> annotationsMap) {
		this.annotationsMap = annotationsMap;
		this.wrappedModel = wrappedModel;
		wrappedModel.addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {
				TableModelEvent translated;
				translated = new TableModelEvent(AnnotationTableModel.this, e.getFirstRow(), e.getLastRow(),
						e.getColumn() + 1, e.getType());
				fireTableChanged(translated);
			}
		});
	}

	@Override
	public String getColumnName(int column) {
		if (column == 0) {
			return "Annotation";
		} else {
			return wrappedModel.getColumnName(column - 1);
		}
	}

	@Override
	public int getRowCount() {
		return wrappedModel.getRowCount();
	}

	@Override
	public int getColumnCount() {
		return wrappedModel.getColumnCount() + 1;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
			if (AnnotationCellEditor.NONE.equals(aValue)) {
				annotationsMap.remove(rowIndex);
			} else {
				annotationsMap.put(rowIndex, (String) aValue);
			}
		} else {
			wrappedModel.setValueAt(aValue, rowIndex, columnIndex - 1);
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
			String annotation = annotationsMap.get(rowIndex);
			if (annotation == null) {
				annotation = AnnotationCellEditor.NONE;
			}
			return annotation;
		} else {
			return wrappedModel.getValueAt(rowIndex, columnIndex - 1);
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 0;
	}
}
