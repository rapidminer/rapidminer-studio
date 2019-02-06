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

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;


/**
 * This is an component of the {@link EditableHeaderJTable}. It was retrieved from
 * http://www.java2s.com/Code/Java/Swing-Components/EditableHeaderTableExample2.htm
 * 
 * 
 * @author Sebastian Land
 */
public class EditableTableHeaderColumn extends TableColumn {

	private static final long serialVersionUID = 1L;

	protected TableCellEditor headerEditor;

	protected boolean isHeaderEditable;

	public EditableTableHeaderColumn(int column) {
		super(column);
		setHeaderEditor(createDefaultHeaderEditor());
		isHeaderEditable = true;
	}

	public void setHeaderEditor(TableCellEditor headerEditor) {
		this.headerEditor = headerEditor;
	}

	public TableCellEditor getHeaderEditor() {
		return headerEditor;
	}

	public void setHeaderEditable(boolean isEditable) {
		isHeaderEditable = isEditable;
	}

	public boolean isHeaderEditable() {
		return isHeaderEditable;
	}

	public void copyValues(TableColumn base) {
		modelIndex = base.getModelIndex();
		identifier = base.getIdentifier();
		width = base.getWidth();
		minWidth = base.getMinWidth();
		setPreferredWidth(base.getPreferredWidth());
		maxWidth = base.getMaxWidth();
		headerRenderer = base.getHeaderRenderer();
		headerValue = base.getHeaderValue();
		cellRenderer = base.getCellRenderer();
		cellEditor = base.getCellEditor();
		isResizable = base.getResizable();
	}

	protected TableCellEditor createDefaultHeaderEditor() {
		return new DefaultCellEditor(new JTextField());
	}

}
