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
package com.rapidminer.gui.tools.table;

import com.rapidminer.gui.tools.ExtendedJTable;

import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;


/**
 * This table allows to specify a component to perform editing in the table header.
 * 
 * Additionally this class offers static methods for attaching editable headers to any previously
 * created JTable.
 * 
 * @author Sebastian Land
 * @deprecated This class was never used anywhere
 */
@Deprecated
public class EditableHeaderJTable extends ExtendedJTable {

	private static final long serialVersionUID = 5755728775020479775L;

	//
	// /**
	// * This constructor will build a standard {@link ExtendedJTable} with the specified renderer
	// and editors for the
	// * header of all columns.
	// */
	// public EditableHeaderJTable(TableModel model, TableCellRenderer headerRenderer,
	// TableCellEditor headerEditor, boolean sortable, boolean
	// moveable, boolean autoresizing) {
	// super(model, sortable, moveable, autoresizing);
	//
	// TableColumnModel columnModel = getColumnModel();
	// setTableHeader(new EditableTableHeader(columnModel));
	//
	// for (int i = 0; i < getColumnCount(); i++) {
	// EditableTableHeaderColumn col = (EditableTableHeaderColumn)
	// this.getColumnModel().getColumn(i);
	// col.setHeaderRenderer(headerRenderer);
	// col.setHeaderEditor(headerEditor);
	// }
	// }

	// public Object getHeaderValue(int column) {
	// return this.getColumnModel().getColumn(column).getHeaderValue();
	// }

	/**
	 * This method installs the given renderer and editors to the header of the given table. The
	 * previous header names will be discarded and replaced by the objects given as initivalValues.
	 * These might be changed by the editor components. After this, they might be returned by
	 * getHeaderValue.
	 */
	public static void installEditableHeader(JTable table, TableCellRenderer headerRenderer, TableCellEditor headerEditor,
			Object[] initialValues) {
		TableColumnModel columnModel = table.getColumnModel();
		table.setTableHeader(new EditableTableHeader(columnModel));

		for (int i = 0; i < table.getColumnCount(); i++) {
			EditableTableHeaderColumn col = (EditableTableHeaderColumn) table.getColumnModel().getColumn(i);
			col.setHeaderValue(initialValues[i]);
			col.setHeaderRenderer(headerRenderer);
			col.setHeaderEditor(headerEditor);
		}
	}

	// public static Object getHeaderValue(JTable table, int columnIndex) {
	// return table.getColumnModel().getColumn(columnIndex).getHeaderValue();
	// }
}
