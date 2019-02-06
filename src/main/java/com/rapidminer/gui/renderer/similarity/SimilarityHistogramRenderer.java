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
package com.rapidminer.gui.renderer.similarity;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.charts.HistogramChart;
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.similarity.SimilarityMeasureObject;
import com.rapidminer.report.Reportable;
import com.rapidminer.tools.math.similarity.DistanceMeasure;


/**
 * A renderer for the histogram view of a similarity measure.
 *
 * @author Ingo Mierswa
 */
public class SimilarityHistogramRenderer extends AbstractRenderer {

	@Override
	public String getName() {
		return "Histogram";
	}

	private Plotter createHistogramPlotter(SimilarityMeasureObject sim, ExampleSet exampleSet) {
		DistanceMeasure measure = sim.getDistanceMeasure();
		DataTable dataTable = new SimpleDataTable("Histogram", new String[] { "Histogram" });
		double sampleRatio = Math.min(1.0d, 500.0d / exampleSet.size());

		Random random = new Random();
		int i = 0;
		for (Example example : exampleSet) {
			int j = 0;
			for (Example comExample : exampleSet) {
				if (j != i && random.nextDouble() < sampleRatio) {
					double simValue;
					if (measure.isDistance()) {
						simValue = measure.calculateDistance(example, comExample);
					} else {
						simValue = measure.calculateSimilarity(example, comExample);
					}
					dataTable.add(new SimpleDataTableRow(new double[] { simValue }));
				}
				j++;
			}
			i++;
		}
		PlotterConfigurationModel settings = new PlotterConfigurationModel(PlotterConfigurationModel.HISTOGRAM_PLOT,
				dataTable);

		settings.enablePlotColumn(0);
		settings.setParameterAsInt(HistogramChart.PARAMETER_NUMBER_OF_BINS, 100);

		return settings.getPlotter();
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int width, int height) {

		SimilarityMeasureObject sim = (SimilarityMeasureObject) renderable;
		Plotter plotter = createHistogramPlotter(sim, sim.getExampleSet());
		plotter.getRenderComponent().setSize(width, height);
		return plotter;

	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {

		SimilarityMeasureObject sim = (SimilarityMeasureObject) renderable;
		Plotter plotter = createHistogramPlotter(sim, sim.getExampleSet());
		JPanel panel = new JPanel(new BorderLayout());
		JPanel innerPanel = new JPanel(new BorderLayout());
		innerPanel.add(plotter.getPlotter());
		innerPanel.setBorder(BorderFactory.createMatteBorder(10, 10, 5, 5, Colors.WHITE));
		panel.add(innerPanel, BorderLayout.CENTER);
		return panel;

	}
}
