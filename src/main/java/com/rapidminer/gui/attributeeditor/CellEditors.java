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
package com.rapidminer.gui.attributeeditor;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.TableCellEditor;


/**
 * A generic collection class for cell editors. This class manages a vector containing the cell
 * editors for each row, i.e. a vector of a vector of cell editors.
 * 
 * @author Ingo Mierswa
 */
public class CellEditors {

	private List<List<TableCellEditor>> cellEditors;

	/** Creates a new cell editor collection. */
	public CellEditors(int size) {
		cellEditors = new ArrayList<List<TableCellEditor>>(size);
		for (int i = 0; i < size; i++) {
			cellEditors.add(new ArrayList<TableCellEditor>());
		}
	}

	/** Adds a new cell editor in the given row. */
	public void add(int row, TableCellEditor editor) {
		cellEditors.get(row).add(editor);
	}

	/** Returns the cell renderer in the given row and column. */
	public TableCellEditor get(int row, int column) {
		return cellEditors.get(row).get(column);
	}

	/** Returns the number of rows. */
	public int getSize() {
		return cellEditors.size();
	}

	/** Returns the size of the i-th row. */
	public int getSize(int i) {
		return cellEditors.get(i).size();
	}
}
