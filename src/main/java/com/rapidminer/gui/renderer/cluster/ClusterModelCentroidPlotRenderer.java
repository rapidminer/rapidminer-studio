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
package com.rapidminer.gui.renderer.cluster;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.plotter.LabelRotatingPlotterAdapter;
import com.rapidminer.gui.plotter.LocalNormalizationPlotterAdapter;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterAdapter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.clustering.CentroidClusterModel;
import com.rapidminer.report.Reportable;


/**
 * A renderer for centroid based cluster models.
 *
 * @author Ingo Mierswa
 */
public class ClusterModelCentroidPlotRenderer extends AbstractRenderer {

	@Override
	public String getName() {
		return "Centroid Plot View";
	}

	private Plotter createCentroidPlotter(CentroidClusterModel cm, int width, int height) {
		String[] dimensionNames = cm.getAttributeNames();
		String[] columnNames = new String[dimensionNames.length + 1];
		System.arraycopy(dimensionNames, 0, columnNames, 0, dimensionNames.length);
		columnNames[columnNames.length - 1] = "Cluster";
		SimpleDataTable dataTable = new SimpleDataTable("Centroid Positions", columnNames);
		for (int i = 0; i < cm.getNumberOfClusters(); i++) {
			double[] centroidValues = cm.getCentroidCoordinates(i);
			String clusterName = cm.getCluster(i).getClusterId() + "";
			double[] values = new double[centroidValues.length + 1];
			System.arraycopy(centroidValues, 0, values, 0, centroidValues.length);
			values[values.length - 1] = dataTable.mapString(values.length - 1, clusterName);
			dataTable.add(new SimpleDataTableRow(values));
		}
		PlotterConfigurationModel settings = new PlotterConfigurationModel(PlotterConfigurationModel.PARALLEL_PLOT,
				dataTable);
		Plotter plotter = settings.getPlotter();
		settings.setParameterAsString(PlotterAdapter.PARAMETER_PLOT_COLUMN, columnNames[columnNames.length - 1]);
		settings.setParameterAsBoolean(LocalNormalizationPlotterAdapter.PARAMETER_LOCAL_NORMALIZATION, false);
		settings.setParameterAsBoolean(LabelRotatingPlotterAdapter.PARAMETER_ROTATE_LABELS, true);
		plotter.getPlotter().setSize(width, height);
		return plotter;
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		CentroidClusterModel cm = (CentroidClusterModel) renderable;
		JPanel panel = new JPanel(new BorderLayout());
		JPanel innerPanel = new JPanel(new BorderLayout());
		innerPanel.add(createCentroidPlotter(cm, 800, 600).getPlotter());
		innerPanel.setBorder(BorderFactory.createMatteBorder(10, 10, 5, 5, Colors.WHITE));
		panel.add(innerPanel, BorderLayout.CENTER);
		return panel;
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int width, int height) {
		CentroidClusterModel cm = (CentroidClusterModel) renderable;
		return createCentroidPlotter(cm, width, height);
	}
}
