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

import javax.swing.table.TableCellRenderer;


/**
 * A generic collection class for cell renderers. This class manages a vector containing the cell
 * renderers for each row, i.e. a vector of a vector of cell renderers.
 * 
 * @author Ingo Mierswa
 */
public class CellRenderers {

	private List<List<TableCellRenderer>> cellRenderers;

	/** Creates a new cell renderer collection. */
	public CellRenderers(int size) {
		cellRenderers = new ArrayList<List<TableCellRenderer>>(size);
		for (int i = 0; i < size; i++) {
			cellRenderers.add(new ArrayList<TableCellRenderer>());
		}
	}

	/** Adds a new cell renderer in the given row. */
	public void add(int row, TableCellRenderer renderer) {
		cellRenderers.get(row).add(renderer);
	}

	/** Returns the cell renderer in the given row and column. */
	public TableCellRenderer get(int row, int column) {
		return cellRenderers.get(row).get(column);
	}

	/** Returns the number of rows. */
	public int getSize() {
		return cellRenderers.size();
	}

	/** Returns the size of the i-th row. */
	public int getSize(int i) {
		return cellRenderers.get(i).size();
	}
}
