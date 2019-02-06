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
package com.rapidminer.gui.plotter.charts;

import com.rapidminer.gui.plotter.PlotterConfigurationModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.PieDataset;


/**
 * A simple 3D pie chart plotter.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class PieChart3DPlotter extends AbstractPieChartPlotter {

	private static final long serialVersionUID = -2107283003284552898L;

	/**
	 * @param settings
	 */
	public PieChart3DPlotter(PlotterConfigurationModel settings) {
		super(settings);

	}

	@Override
	public JFreeChart createChart(PieDataset pieDataSet, boolean createLegend) {
		JFreeChart chart = ChartFactory.createPieChart3D(null, pieDataSet, createLegend, // legend
				true, false);

		PiePlot3D plot = (PiePlot3D) chart.getPlot();
		plot.setForegroundAlpha(0.5f);

		return chart;
	}

	@Override
	public boolean isSupportingExplosion() {
		return false;
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.PIE_CHART_3D;
	}
}
