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
package com.rapidminer.gui.properties;

import com.rapidminer.gui.tools.CellColorProvider;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;

import java.awt.Color;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.table.TableColumn;


/**
 * For {@link com.rapidminer.parameter.ParameterTypeMatrix}
 * 
 * @author Helge Homburg
 */
public class MatrixPropertyTable extends ExtendedJTable {

	private static final long serialVersionUID = 2348648114479673318L;

	private transient Operator operator;

	private MatrixPropertyTableModel model;

	private String rowBaseName;

	public MatrixPropertyTable(String baseName, String rowBaseName, String columnBaseName, double[][] parameterMatrix,
			Operator operator) {
		super(null, false, false);
		this.rowBaseName = rowBaseName;
		this.operator = operator;

		setCellColorProvider(new CellColorProvider() {

			@Override
			public Color getCellColor(int row, int column) {
				if (column == 0) {
					return SwingTools.LIGHTEST_BLUE;
				} else {
					return Color.WHITE;
				}
			}
		});
		// build the table model
		if (parameterMatrix != null) {
			int numberOfRows = parameterMatrix[0].length;
			int numberOfColumns = parameterMatrix.length;

			model = new MatrixPropertyTableModel(baseName, columnBaseName, numberOfRows, numberOfColumns + 1);
			setModel(model);
			for (int i = 0; i < numberOfColumns; i++) {
				getModel().setValueAt(rowBaseName + " " + (i + 1), i, 0);
				for (int j = 0; j < numberOfRows; j++) {
					getModel().setValueAt(Double.toString(parameterMatrix[i][j]), i, j + 1);
				}
			}
		} else {
			model = new MatrixPropertyTableModel(baseName, columnBaseName, 0, 0);
			setModel(model);
		}
	}

	public void addRow() {
		if (model.getColumnCount() == 0) {
			addColumn();
		}
		model.addRow(new Object[model.getColumnCount()]);
		int currentRow = model.getRowCount() - 1;
		model.setValueAt(rowBaseName + " " + (currentRow + 1), currentRow, 0);
	}

	public void addColumn() {
		model.addColumn(model.getColumnCount(), new Object[model.getRowCount()]);
	}

	public void removeSelectedRow() {
		int[] selectedRows = getSelectedRows();
		if (selectedRows.length < model.getRowCount()) {
			for (int i = selectedRows.length - 1; i >= 0; i--) {
				model.removeRow(selectedRows[i]);
			}
			model.fireTableStructureChanged();
		}
	}

	public void removeSelectedColumn() {
		int[] selectedColumns = getSelectedColumns();
		if (selectedColumns.length < model.getColumnCount() - 1) {
			for (int i = selectedColumns.length - 1; i >= 0; i--) {
				removeColumn(selectedColumns[i]);
			}
			model.fireTableStructureChanged();
		}
	}

	public void removeSelectedRowAndColumn() {
		if (model.getRowCount() > 1) {
			model.removeRow(model.getRowCount() - 1);
			removeColumn(model.getColumnCount() - 1);
			model.fireTableStructureChanged();
		}
	}

	public void fillNewRowAndColumn() {
		int currentRow = model.getRowCount() - 1;
		int currentColumn = model.getColumnCount() - 1;
		model.setValueAt("1.0", 0, currentColumn);
		for (int i = 1; i < currentColumn; i++) {
			model.setValueAt("1.0", currentRow, i);
			model.setValueAt("1.0", i, currentColumn);
		}
		model.setValueAt("0.0", currentRow, currentColumn);
	}

	public double[][] getParameterMatrix() {
		double[][] matrix = new double[getModel().getRowCount()][getModel().getColumnCount() - 1];
		for (int i = 0; i < getModel().getRowCount(); i++) {
			for (int j = 0; j < getModel().getColumnCount() - 1; j++) {
				matrix[i][j] = Double.parseDouble((String) getModel().getValueAt(i, j + 1));
			}
		}
		return matrix;
	}

	public Operator getOperator(int row) {
		return operator;
	}

	public void removeColumn(int index) {
		TableColumn column = getColumnModel().getColumn(index);
		int modelIndex = column.getModelIndex();
		Vector<?> modelData = model.getDataVector();
		Vector<?> columnIdentifiers = model.getColumnIdentifiers();

		// remove the column from the table
		removeColumn(column);

		// remove the column header from the table model
		columnIdentifiers.removeElementAt(modelIndex);

		// remove the column data
		for (Object row : modelData) {
			((Vector<?>) row).removeElementAt(modelIndex);
		}
		model.setDataVector(modelData, columnIdentifiers);

		// correct the model indices in the TableColumn objects
		Enumeration<TableColumn> columns = getColumnModel().getColumns();
		while (columns.hasMoreElements()) {
			TableColumn currentColumn = columns.nextElement();
			if (currentColumn.getModelIndex() >= modelIndex) {
				currentColumn.setModelIndex(currentColumn.getModelIndex() - 1);
			}
		}
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		if (col == 0) {
			return false;
		} else {
			return true;
		}
	}
}
