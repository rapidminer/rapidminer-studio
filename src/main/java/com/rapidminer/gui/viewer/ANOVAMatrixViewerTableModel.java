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

import com.rapidminer.operator.visualization.dependencies.ANOVAMatrix;
import com.rapidminer.tools.Tools;

import javax.swing.table.AbstractTableModel;


/**
 * The model for the {@link com.rapidminer.gui.viewer.ANOVAMatrixViewerTable}.
 * 
 * @author Ingo Mierswa
 */
public class ANOVAMatrixViewerTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -5732155307505605893L;

	private ANOVAMatrix matrix;

	public ANOVAMatrixViewerTableModel(ANOVAMatrix matrix) {
		this.matrix = matrix;
	}

	@Override
	public int getRowCount() {
		return matrix.getAnovaAttributeNames().size();
	}

	@Override
	public int getColumnCount() {
		return matrix.getGroupingAttributeNames().size() + 1;
	}

	@Override
	public String getColumnName(int col) {
		if (col == 0) {
			return "ANOVA Attribute";
		} else {
			return "group " + matrix.getGroupingAttributeNames().get(col - 1);
		}
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (col == 0) {
			return matrix.getAnovaAttributeNames().get(row);
		} else {
			return Tools.formatNumber(matrix.getProbabilities()[row][col - 1]);
		}
	}
}
