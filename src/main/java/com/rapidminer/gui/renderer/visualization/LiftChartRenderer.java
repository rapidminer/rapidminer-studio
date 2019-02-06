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

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterAdapter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.charts.ParetoChartPlotter;
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.visualization.LiftParetoChart;
import com.rapidminer.report.Reportable;


/**
 * A renderer for the Lift Pareto chart.
 *
 * @author Ingo Mierswa
 */
public class LiftChartRenderer extends AbstractRenderer {

	@Override
	public String getName() {
		return "Plot";
	}

	private Plotter createLiftChartPlotter(Object renderable) {
		LiftParetoChart paretoChartInformation = (LiftParetoChart) renderable;
		PlotterConfigurationModel settings = new PlotterConfigurationModel(PlotterConfigurationModel.PARETO_PLOT,
				paretoChartInformation.getLiftChartData());
		Plotter plotter = settings.getPlotter();
		settings.setParameterAsString(
				PlotterAdapter.PARAMETER_SUFFIX_AXIS + PlotterAdapter.transformParameterName(plotter.getAxisName(0)),
				settings.getDataTable().getColumnName(0));
		settings.setParameterAsString(PlotterAdapter.PARAMETER_PLOT_COLUMN, settings.getDataTable().getColumnName(1));
		settings.setParameterAsString(ParetoChartPlotter.PARAMETER_COUNT_VALUE, paretoChartInformation.getTargetValue());
		settings.setParameterAsInt(ParetoChartPlotter.PARAMETER_SORTING_DIRECTION, ParetoChartPlotter.KEYS_DESCENDING);
		settings.setParameterAsBoolean(ParetoChartPlotter.PARAMETER_SHOW_BAR_LABELS, paretoChartInformation.showBarLabels());
		settings.setParameterAsBoolean(ParetoChartPlotter.PARAMETER_SHOW_CUMULATIVE_LABELS,
				paretoChartInformation.showCumulativeLabels());
		settings.setParameterAsBoolean(ParetoChartPlotter.PARAMETER_ROTATE_LABELS, paretoChartInformation.rotateLabels());
		return plotter;
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int width, int height) {
		Plotter plotter = createLiftChartPlotter(renderable);
		plotter.getRenderComponent().setSize(width, height);
		return plotter;
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		JPanel panel = new JPanel(new BorderLayout());
		JPanel innerPanel = new JPanel(new BorderLayout());
		innerPanel.add(createLiftChartPlotter(renderable).getPlotter());
		innerPanel.setBorder(BorderFactory.createMatteBorder(10, 10, 5, 5, Colors.WHITE));
		panel.add(innerPanel, BorderLayout.CENTER);
		return panel;
	}
}
