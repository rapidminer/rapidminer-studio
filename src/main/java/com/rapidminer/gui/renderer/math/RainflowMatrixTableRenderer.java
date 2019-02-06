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

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.renderer.AbstractDataTableTableRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.visualization.dependencies.NumericalMatrix;
import com.rapidminer.operator.visualization.dependencies.RainflowMatrix;


/**
 *
 * @author Sebastian Land
 */
public class RainflowMatrixTableRenderer extends AbstractDataTableTableRenderer {

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		return addWarningPanel(super.getVisualizationComponent(renderable, ioContainer), ((NumericalMatrix) renderable).isUseless(), "numerical_matrix.not_enough_attributes.label");
	}

	@Override
	public DataTable getDataTable(Object renderable, IOContainer ioContainer, boolean isRendering) {
		RainflowMatrix matrix = (RainflowMatrix) renderable;
		return matrix.createResidualTable();
	}

	@Override
	public String getName() {
		return "Residual Table";
	}

	@Override
	public boolean isAutoresize() {
		return true;
	}
}
