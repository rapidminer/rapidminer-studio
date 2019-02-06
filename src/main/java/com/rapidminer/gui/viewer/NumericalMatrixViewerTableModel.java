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

import com.rapidminer.operator.visualization.dependencies.NumericalMatrix;

import javax.swing.table.AbstractTableModel;


/**
 * The model for a NumericalMatrix.
 * 
 * @author Ingo Mierswa
 */
public class NumericalMatrixViewerTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 8116530590493627673L;

	private transient NumericalMatrix matrix;

	public NumericalMatrixViewerTableModel(NumericalMatrix matrix) {
		this.matrix = matrix;
	}

	@Override
	public Class<?> getColumnClass(int column) {
		Class<?> type = super.getColumnClass(column);
		if (column == 0) {
			type = String.class;
		} else {
			type = Double.class;
		}
		return type;
	}

	@Override
	public int getRowCount() {
		return matrix.getNumberOfRows();
	}

	@Override
	public int getColumnCount() {
		return matrix.getNumberOfColumns() + 1;
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (col == 0) {
			return matrix.getRowName(row);
		} else {
			return matrix.getValue(row, col - 1);
		}
	}

	@Override
	public String getColumnName(int col) {
		if (col == 0) {
			return "Name";
		} else {
			return matrix.getColumnName(col - 1);
		}
	}
}
