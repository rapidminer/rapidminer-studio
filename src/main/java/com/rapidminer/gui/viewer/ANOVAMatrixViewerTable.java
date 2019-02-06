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

import java.awt.Color;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.CellColorProvider;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.visualization.dependencies.ANOVAMatrix;


/**
 * Can be used to display (parts of) an ANOVA matrix by means of a JTable.
 *
 * @author Ingo Mierswa
 */
public class ANOVAMatrixViewerTable extends ExtendedJTable {

	private static final long serialVersionUID = 1L;

	private ANOVAMatrix matrix;

	public ANOVAMatrixViewerTable(ANOVAMatrix _matrix) {
		super(new ANOVAMatrixViewerTableModel(_matrix), true);
		this.matrix = _matrix;

		setCellColorProvider(new CellColorProvider() {

			@Override
			public Color getCellColor(int row, int col) {
				int actualCol = convertColumnIndexToModel(col);
				if (actualCol == 0) {
					return Colors.WHITE;
				} else {
					int actualRow = row;
					if (getTableSorter() != null) {
						if (getTableSorter().isSorting()) {
							actualRow = getTableSorter().modelIndex(row);
						}
					}
					double value = matrix.getProbabilities()[actualRow][actualCol - 1];
					if (value > matrix.getSignificanceLevel()) {
						return SwingTools.LIGHTEST_YELLOW;
					} else {
						return Colors.WHITE;
					}
				}
			}
		});
	}
}
