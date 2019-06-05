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
package com.rapidminer.gui.renderer.weights;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.renderer.AbstractDataTablePlotterRenderer;
import com.rapidminer.operator.IOContainer;

import java.util.LinkedHashMap;


/**
 * A renderer for the plot view of attribute weights.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.1
 */
@Deprecated
public class AttributeWeightsPlotRenderer extends AbstractDataTablePlotterRenderer {

	@Override
	public DataTable getDataTable(Object renderable, IOContainer ioContainer) {
		AttributeWeights weights = (AttributeWeights) renderable;
		return weights.createDataTable();
	}

	@Override
	public LinkedHashMap<String, Class<? extends Plotter>> getPlotterSelection() {
		return PlotterConfigurationModel.WEIGHT_PLOTTER_SELECTION;
	}
}
