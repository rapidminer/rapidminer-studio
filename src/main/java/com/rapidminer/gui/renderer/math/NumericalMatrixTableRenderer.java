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
package com.rapidminer.gui.renderer.math;

import java.awt.Component;
import java.util.HashSet;
import java.util.Set;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.renderer.AbstractDataTableTableRenderer;
import com.rapidminer.gui.tools.CellColorProvider;
import com.rapidminer.gui.tools.CellColorProviderScaled;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.visualization.dependencies.NumericalMatrix;


/**
 *
 * @author Sebastian Land
 */
public class NumericalMatrixTableRenderer extends AbstractDataTableTableRenderer {

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		return addWarningPanel(super.getVisualizationComponent(renderable, ioContainer), ((NumericalMatrix) renderable).isUseless(), "numerical_matrix.not_enough_attributes.label");
	}

	@Override
	public DataTable getDataTable(Object renderable, IOContainer ioContainer, boolean isRendering) {
		NumericalMatrix matrix = (NumericalMatrix) renderable;
		return matrix.createMatrixDataTable();
	}

	@Override
	protected CellColorProvider getCellColorProvider(ExtendedJTable table, Object renderable) {
		NumericalMatrix matrix = (NumericalMatrix) renderable;
		// matrix viewer
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (int x = 0; x < matrix.getNumberOfRows(); x++) {
			for (int y = 0; y < matrix.getNumberOfColumns(); y++) {
				double value = Math.abs(matrix.getValue(x, y));
				if (!Double.isNaN(value)) {
					min = Math.min(min, value);
					max = Math.max(max, value);
				}
			}
		}

		Set<Integer> notColorized = new HashSet<Integer>();
		notColorized.add(0);

		return new CellColorProviderScaled(table, true, min, max, notColorized);
	}

	@Override
	public boolean isAutoresize() {
		return true;
	}

}
