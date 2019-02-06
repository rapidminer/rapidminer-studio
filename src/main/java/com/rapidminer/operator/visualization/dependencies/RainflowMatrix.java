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
package com.rapidminer.operator.visualization.dependencies;

import Jama.Matrix;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;


/**
 * The Rainflow Matrix adds another data table view for the residuals of the Rainflow Matrix
 * calculation as well as a new plot tab for the residuals.
 * 
 * @author Ingo Mierswa
 */
public class RainflowMatrix extends NumericalMatrix {

	private static final long serialVersionUID = -2316260417823285606L;

	private String[] residuals;

	public RainflowMatrix(String name, String[] columnNames, Matrix matrix, boolean symmetrical, String[] residuals) {
		super(name, columnNames, matrix, symmetrical);
		this.residuals = residuals;
	}

	public SimpleDataTable createResidualTable() {
		SimpleDataTable residualTable = new SimpleDataTable("Rainflow Matrix Residuals", new String[] { "Residual Index",
				"Residual Class" });
		for (int i = 0; i < getNumberOfColumns(); i++) {
			residualTable.mapString(1, getColumnName(i));
		}
		int index = 1;
		for (String residual : residuals) {
			residualTable.add(new SimpleDataTableRow(new double[] { index++, residualTable.mapString(1, residual) }));
		}
		return residualTable;
	}

}
