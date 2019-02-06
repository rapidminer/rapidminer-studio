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
package com.rapidminer.gui.new_plotter.integration;

import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.gui.AbstractConfigurationPanel.DatasetTransformationType;
import com.rapidminer.gui.new_plotter.gui.ChartConfigurationPanel;
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.gui.renderer.DefaultReadable;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.report.Reportable;
import com.rapidminer.tools.I18N;

import java.awt.Component;
import java.util.Map;


/**
 * This renderer creates a plot view of the series (for integration in RapidMiner).
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class ExpertDataTableRenderer extends AbstractRenderer {

	/*
	 * (non-Javadoc) TODO: Needs to return proper {@link Reportable}
	 */
	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		ExampleSet exampleSet = (ExampleSet) renderable;
		return new DefaultReadable(exampleSet.toString());
	}

	@Override
	public String getName() {
		return I18N.getGUILabel("plotter.renderer_name");
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		ExampleSet exampleSet = (ExampleSet) renderable;
		DataTableExampleSetAdapter dataTable = new DataTableExampleSetAdapter(exampleSet, null, false);
		Map<DatasetTransformationType, PlotConfiguration> plotConfigurationMap = PlotConfigurationHistory
				.getPlotConfigurationMap(exampleSet, dataTable);
		PlotInstance plotInstance = new PlotInstance(plotConfigurationMap.get(DatasetTransformationType.ORIGINAL), dataTable);
		return new ChartConfigurationPanel(true, plotInstance, dataTable,
				plotConfigurationMap.get(DatasetTransformationType.DE_PIVOTED), exampleSet.getSource());
	}
}
