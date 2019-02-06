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

import com.rapidminer.tools.Tools;

import javax.swing.table.AbstractTableModel;


/**
 * The model for the {@link com.rapidminer.gui.viewer.ConfusionMatrixViewerTable}.
 * 
 * @author Ingo Mierswa
 */
public class ConfusionMatrixViewerTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1206988933244249851L;

	private String[] classNames;
	private double[][] counter;
	private double[] rowSums;
	private double[] columnSums;

	public ConfusionMatrixViewerTableModel(String[] classNames, double[][] counter) {
		this.classNames = classNames;
		this.counter = counter;
		this.rowSums = new double[classNames.length];
		this.columnSums = new double[classNames.length];
		for (int i = 0; i < classNames.length; i++) {
			for (int j = 0; j < classNames.length; j++) {
				this.columnSums[i] += counter[i][j];
				this.rowSums[i] += counter[j][i];
			}
		}
	}

	@Override
	public int getRowCount() {
		return classNames.length + 2;
	}

	@Override
	public int getColumnCount() {
		return classNames.length + 2;
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (row == 0) {
			if (col == 0) {
				return "";
			} else if (col == getColumnCount() - 1) {
				return "class precision";
			} else {
				return "true " + classNames[col - 1];
			}
		} else if (row == getRowCount() - 1) {
			if (col == 0) {
				return "class recall";
			} else if (col == getColumnCount() - 1) {
				return "";
			} else {
				double recall = counter[col - 1][col - 1] / columnSums[col - 1];
				if (Double.isNaN(recall)) {
					return Tools.formatPercent(0);
				} else {
					return Tools.formatPercent(recall);
				}
			}
		} else {
			if (col == 0) {
				if (row - 1 >= 0) {
					return "pred. " + classNames[row - 1];
				} else {
					return "";
				}
			} else if (col == getColumnCount() - 1) {
				double precision = counter[row - 1][row - 1] / rowSums[row - 1];
				if (Double.isNaN(precision)) {
					return Tools.formatPercent(0);
				} else {
					return Tools.formatPercent(precision);
				}
			} else {
				if ((col - 1 >= 0) && (row - 1 >= 0)) {
					return Tools.formatIntegerIfPossible(counter[col - 1][row - 1]);
				} else {
					return "";
				}
			}
		}
	}
}
