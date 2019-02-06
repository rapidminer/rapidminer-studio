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
package com.rapidminer.gui.renderer.visualization;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.PlotterPanel;
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.visualization.SOMModelPlotter;
import com.rapidminer.operator.visualization.SOMModelVisualization.SOMModelVisualizationResult;
import com.rapidminer.report.Reportable;

import java.awt.Component;


/**
 * 
 * @author Sebastian Land
 */
public class SOMModelVisualizationRenderer extends AbstractRenderer {

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		SOMModelVisualizationResult result = (SOMModelVisualizationResult) renderable;
		DataTable table = new DataTableExampleSetAdapter(result.getExampleSet(), null);
		PlotterConfigurationModel settings = new PlotterConfigurationModel(
				PlotterConfigurationModel.MODEL_PLOTTER_SELECTION, table);
		settings.setPlotter("SOM");
		SOMModelPlotter plotter = (SOMModelPlotter) settings.getPlotter();
		plotter.setExampleSet(result.getExampleSet());
		plotter.setModel(result.getModel());
		return plotter;
	}

	@Override
	public String getName() {
		return "SOM Model Visualization";
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		SOMModelVisualizationResult result = (SOMModelVisualizationResult) renderable;
		DataTable table = new DataTableExampleSetAdapter(result.getExampleSet(), null);
		PlotterConfigurationModel settings = new PlotterConfigurationModel(
				PlotterConfigurationModel.MODEL_PLOTTER_SELECTION, table);
		settings.setPlotter("SOM");
		PlotterPanel panel = new PlotterPanel(settings);
		Plotter plotter = settings.getPlotter();
		((SOMModelPlotter) plotter).setExampleSet(result.getExampleSet());
		((SOMModelPlotter) plotter).setModel(result.getModel());
		return panel;
	}
}
