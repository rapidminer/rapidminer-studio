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
package com.rapidminer.gui.renderer.models;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.features.transformation.SVDModel;
import com.rapidminer.report.Reportable;


/**
 * This is a plot renderer that will show the cumulative amount of eigenvalues in the given model.
 *
 * @author Sebastian Land
 */
public class SVDModelCumulativePlotRenderer extends AbstractRenderer {

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		return getPlotter(renderable);
	}

	@Override
	public String getName() {
		return "Cumulative Variance Plot";
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		JPanel panel = new JPanel(new BorderLayout());
		JPanel innerPanel = new JPanel(new BorderLayout());
		innerPanel.add(getPlotter(renderable).getPlotter());
		innerPanel.setBorder(BorderFactory.createMatteBorder(10, 10, 5, 5, Colors.WHITE));
		panel.add(innerPanel, BorderLayout.CENTER);

		return panel;
	}

	private Plotter getPlotter(Object renderable) {
		SVDModel model = (SVDModel) renderable;

		double[] cumulativeVariance = new double[model.getNumberOfComponents()];
		for (int i = 0; i < cumulativeVariance.length; i++) {
			cumulativeVariance[i] = model.getCumulativeSingularValue(i);
		}
		DataTable dataTable = new SimpleDataTable("Cumulative Proportion of Singular Values", new String[] {
				"Singular Value Vectors", "Cumulative Proportion of Singular Values" });
		dataTable.add(new SimpleDataTableRow(new double[] { 0.0d, 0.0d }));
		for (int i = 0; i < cumulativeVariance.length; i++) {
			dataTable.add(new SimpleDataTableRow(new double[] { i + 1, cumulativeVariance[i] }));
		}

		PlotterConfigurationModel settings = new PlotterConfigurationModel(
				PlotterConfigurationModel.WEIGHT_PLOTTER_SELECTION, dataTable);
		settings.setPlotter(PlotterConfigurationModel.LINES_PLOT);
		Plotter plotter = settings.getPlotter();
		settings.setAxis(0, 0);
		settings.enablePlotColumn(1);

		return plotter;
	}
}
