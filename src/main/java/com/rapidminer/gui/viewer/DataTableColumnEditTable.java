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
import com.rapidminer.datatable.DataTableListener;
import com.rapidminer.gui.tools.CellColorProvider;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.container.Pair;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.jfree.ui.DateCellRenderer;


/**
 * Can be used to display (parts of) a DataTable by means of a JTable while presenting additional
 * columns to enter arbitrary values.
 * 
 * @author Sebastian Land
 */
public class DataTableColumnEditTable extends ExtendedJTable implements DataTableListener {

	private static final long serialVersionUID = 3206734427933036268L;

	private DataTableColumnEditTableModel model;

	private List<String> editableColumnNames;
	private List<Pair<TableCellRenderer, TableCellEditor>> cellComponents;

	public DataTableColumnEditTable(DataTable dataTable, List<String> editableColumnNames,
			List<Pair<TableCellRenderer, TableCellEditor>> cellComponents, boolean sortable, boolean columnMovable,
			boolean autoResize) {
		super(sortable, columnMovable, autoResize);
		this.editableColumnNames = editableColumnNames;
		this.cellComponents = cellComponents;

		if (model != null) {
			setDataTable(dataTable);
		}
	}

	public void setDataTable(DataTable dataTable) {
		// constructing model
		model = new DataTableColumnEditTableModel(dataTable, editableColumnNames);
		setModel(model);
		int i = 0;
		for (Pair<TableCellRenderer, TableCellEditor> cellComponent : cellComponents) {
			TableColumn column = getColumnModel().getColumn(i);
			column.setCellEditor(cellComponent.getSecond());
			column.setCellRenderer(cellComponent.getFirst());
			i++;
		}
		for (; i < model.getColumnCount(); i++) {
			TableColumn column = getColumnModel().getColumn(i);
			column.setCellEditor(new DefaultCellEditor(new JTextField()));
			column.setCellRenderer(new DateCellRenderer());
		}

		// gui related issues
		setCellColorProvider(new CellColorProvider() {

			@Override
			public Color getCellColor(int row, int col) {
				if (row % 2 == 0) {
					return Color.WHITE;
				} else {
					return SwingTools.LIGHTEST_BLUE;
				}
			}
		});
	}

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		return super.getCellEditor(row, column);
	}

	/**
	 * This method will return the values entered in the editable columns.
	 */
	public Object[] getEnteredValues(int column) {
		return model.getEnteredValues(column);
	}

	@Override
	public void dataTableUpdated(DataTable source) {
		if (this.model != null) {
			this.model.fireTableDataChanged();
		}
	}

	/** This method ensures that the correct tool tip for the current column is delivered. */
	@Override
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {

			private static final long serialVersionUID = 1L;

			@Override
			public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int index = columnModel.getColumnIndexAtX(p.x);
				int realColumnIndex = convertColumnIndexToModel(index);
				if ((realColumnIndex >= 0) && (realColumnIndex < getModel().getColumnCount())) {
					return "The column " + getModel().getColumnName(realColumnIndex);
				} else {
					return "";
				}
			}
		};
	}
}
