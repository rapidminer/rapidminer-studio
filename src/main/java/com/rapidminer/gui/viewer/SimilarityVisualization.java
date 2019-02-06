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
package com.rapidminer.gui.viewer;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.graphs.GraphViewer;
import com.rapidminer.gui.graphs.SimilarityGraphCreator;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.charts.HistogramChart;
import com.rapidminer.operator.similarity.SimilarityMeasureObject;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


/**
 * Visualizes a similarity measure interactively.
 * 
 * @author Ingo Mierswa
 */
public class SimilarityVisualization extends JPanel {

	private static final long serialVersionUID = 1976956148942768107L;

	public SimilarityVisualization(SimilarityMeasureObject sim, ExampleSet exampleSet) {
		super();
		setLayout(new BorderLayout());

		DistanceMeasure measure = sim.getDistanceMeasure();
		ButtonGroup group = new ButtonGroup();
		JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		// similarity table
		final JComponent tableView = new SimilarityTable(measure, exampleSet);
		final JRadioButton tableButton = new JRadioButton("Table View", true);
		tableButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (tableButton.isSelected()) {
					remove(1);
					add(tableView, BorderLayout.CENTER);
					repaint();
				}
			}
		});
		group.add(tableButton);
		togglePanel.add(tableButton);

		// graph view
		final JComponent graphView = new GraphViewer<String, String>(new SimilarityGraphCreator(measure, exampleSet));
		final JRadioButton graphButton = new JRadioButton("Graph View", false);
		graphButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (graphButton.isSelected()) {
					remove(1);
					add(graphView, BorderLayout.CENTER);
					repaint();
				}
			}
		});
		group.add(graphButton);
		togglePanel.add(graphButton);

		// histogram view
		DataTable dataTable = new SimpleDataTable("Histogram", new String[] { "Histogram" });
		double sampleRatio = Math.min(1.0d, 500.0d / exampleSet.size());

		Random random = new Random();
		int i = 0;
		for (Example example : exampleSet) {
			int j = 0;
			for (Example compExample : exampleSet) {
				if (i != j && random.nextDouble() < sampleRatio) {
					double simValue = measure.calculateSimilarity(example, compExample);
					dataTable.add(new SimpleDataTableRow(new double[] { simValue }));
				}
				j++;
			}
			i++;
		}

		final PlotterConfigurationModel settings = new PlotterConfigurationModel(PlotterConfigurationModel.HISTOGRAM_PLOT,
				dataTable);
		settings.enablePlotColumn(0);
		settings.setParameterAsInt(HistogramChart.PARAMETER_NUMBER_OF_BINS, 100);

		final JRadioButton histogramButton = new JRadioButton("Histogram View", false);
		histogramButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (histogramButton.isSelected()) {
					remove(1);
					add(settings.getPlotter().getPlotter(), BorderLayout.CENTER);
					repaint();
				}
			}
		});
		group.add(histogramButton);
		togglePanel.add(histogramButton);

		// K distance view
		final SimilarityKDistanceVisualization kDistancePlotter = new SimilarityKDistanceVisualization(measure, exampleSet);
		final JRadioButton kdistanceButton = new JRadioButton("k-Distance View", false);
		kdistanceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (kdistanceButton.isSelected()) {
					remove(1);
					add(kDistancePlotter, BorderLayout.CENTER);
					repaint();
				}
			}
		});
		group.add(kdistanceButton);
		togglePanel.add(kdistanceButton);

		add(togglePanel, BorderLayout.NORTH);
		add(tableView, BorderLayout.CENTER);
	}
}
