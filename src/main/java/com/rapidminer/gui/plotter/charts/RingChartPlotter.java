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

import java.awt.Font;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.PieDataset;

import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.tools.FontTools;


/**
 * A simple 2D ring chart plotter.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class RingChartPlotter extends AbstractPieChartPlotter {

	private static final long serialVersionUID = 4950755498257276805L;

	/**
	 * @param settings
	 */
	public RingChartPlotter(PlotterConfigurationModel settings) {
		super(settings);

	}

	@Override
	public JFreeChart createChart(PieDataset pieDataSet, boolean createLegend) {
		JFreeChart chart = ChartFactory.createRingChart(null, pieDataSet, createLegend, // legend
				true, false);

		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setSectionOutlinesVisible(false);
		plot.setLabelFont(FontTools.getFont(Font.SANS_SERIF, Font.PLAIN, 11));
		plot.setNoDataMessage("No data available");
		plot.setCircular(true);
		plot.setLabelGap(0.02);

		return chart;
	}

	@Override
	public boolean isSupportingExplosion() {
		return true;
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.RING_CHART;
	}
}
