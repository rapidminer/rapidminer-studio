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
package com.rapidminer.operator.visualization;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.report.Renderable;
import com.rapidminer.tools.OperatorService;

import java.awt.Graphics;
import java.io.ObjectStreamException;
import java.util.Collections;


/**
 * This object can usually not be passed to other operators but can simply be used for the inline
 * visualization of a Lift Pareto chart (without a dialog).
 * 
 * @author Ingo Mierswa
 */
public class LiftParetoChart extends ResultObjectAdapter implements Renderable {

	static {
		OperatorService.registerIOObjects(Collections.<Class<? extends IOObject>> singletonList(LiftParetoChart.class));
	}

	private static final long serialVersionUID = 7559555964863472326L;

	private transient Plotter plotter;

	private final SimpleDataTable liftChartData;

	private final String targetValue;

	private final boolean showBarLabels;

	private final boolean showCumulativeLabels;

	private final boolean rotateLabels;

	public LiftParetoChart(SimpleDataTable liftChartData, String targetValue, boolean showBarLabels,
			boolean showCumulativeLabels, boolean rotateLabels) {
		this.liftChartData = liftChartData;
		this.targetValue = targetValue;
		this.showBarLabels = showBarLabels;
		this.showCumulativeLabels = showCumulativeLabels;
		this.rotateLabels = rotateLabels;

		PlotterConfigurationModel settings = new PlotterConfigurationModel(PlotterConfigurationModel.PARETO_PLOT,
				liftChartData);

		this.plotter = settings.getPlotter();
	}

	public DataTable getLiftChartData() {
		return this.liftChartData;
	}

	public String getTargetValue() {
		return this.targetValue;
	}

	public boolean showBarLabels() {
		return showBarLabels;
	}

	public boolean showCumulativeLabels() {
		return showCumulativeLabels;
	}

	public boolean rotateLabels() {
		return rotateLabels;
	}

	@Override
	public String getName() {
		return "Lift Chart";
	}

	@Override
	public String toString() {
		return "A visualization of the discretized confidences together with the counts for " + targetValue + ".";
	}

	public String getExtension() {
		return "lpc";
	}

	public String getFileDescription() {
		return "Lift Pareto Chart files";
	}

	@Override
	public void prepareRendering() {
		plotter.prepareRendering();
	}

	@Override
	public void finishRendering() {
		plotter.finishRendering();
	}

	@Override
	public int getRenderHeight(int preferredHeight) {
		return plotter.getRenderHeight(preferredHeight);
	}

	@Override
	public int getRenderWidth(int preferredWidth) {
		return plotter.getRenderWidth(preferredWidth);
	}

	@Override
	public void render(Graphics graphics, int width, int height) {
		plotter.render(graphics, width, height);
	}

	private Object readResolve() throws ObjectStreamException {
		PlotterConfigurationModel settings = new PlotterConfigurationModel(PlotterConfigurationModel.PARETO_PLOT,
				liftChartData);
		this.plotter = settings.getPlotter();
		return this;
	}
}
