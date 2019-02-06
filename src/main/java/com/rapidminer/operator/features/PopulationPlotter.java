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
package com.rapidminer.operator.features;

import com.rapidminer.ObjectVisualizer;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.plotter.ScatterPlotter;
import com.rapidminer.gui.plotter.SimplePlotterDialog;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.viewer.DataTableViewer;
import com.rapidminer.operator.features.selection.NonDominatedSortingSelection;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.tools.Tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.WindowConstants;


/**
 * Plots all individuals in performance space, i.e. the dimensions of the plot (color for the third
 * dimension) corresponds to performance criteria.
 * 
 * @author Ingo Mierswa
 */
public class PopulationPlotter implements PopulationOperator, ObjectVisualizer {

	/** Indicates in which generations the plot should be updated. */
	private int plotGenerations = 1;

	/** The plotter. */
	private SimplePlotterDialog plotter = null;

	/** The data table containing the individuals criteria data. */
	private SimpleDataTable criteriaDataTable = null;

	/** Indicates if the draw range should be set. */
	private boolean setDrawRange = false;

	/** Indicates if dominated points should also be drawn. */
	private boolean drawDominated = true;

	/** Contains a copy of the individuals of the last generation. */
	private Map<String, double[]> lastPopulation = new HashMap<String, double[]>();

	private ExampleSet exampleSet;

	/** Creates plotter panel which is repainted every generation. */
	public PopulationPlotter(ExampleSet exampleSet) {
		this(exampleSet, 1, false, true);
	}

	/**
	 * Creates plotter panel which is repainted each plotGenerations generations.
	 */
	public PopulationPlotter(ExampleSet exampleSet, int plotGenerations, boolean setDrawRange, boolean drawDominated) {
		this.exampleSet = exampleSet;
		this.plotGenerations = plotGenerations;
		this.setDrawRange = setDrawRange;
		this.drawDominated = drawDominated;
	}

	/**
	 * Returns true if the current generation modulo the plotGenerations parameter is zero.
	 */
	@Override
	public boolean performOperation(int generation) {
		return ((generation % plotGenerations) == 0);
	}

	@Override
	public void operate(Population pop) {
		if (pop.getNumberOfIndividuals() == 0) {
			return;
		}
		if ((pop.getGeneration() % plotGenerations) != 0) {
			return;
		}

		// init data table
		if (criteriaDataTable == null) {
			this.criteriaDataTable = createDataTable(pop);
		}

		// fill table
		int numberOfCriteria = fillDataTable(this.criteriaDataTable, this.lastPopulation, pop, drawDominated);

		// create plotter
		if (plotter == null) {
			plotter = new SimplePlotterDialog(criteriaDataTable, false);
			if (numberOfCriteria == 1) {
				plotter.setXAxis(0);
				plotter.plotColumn(0, true);
			} else if (numberOfCriteria == 2) {
				plotter.setXAxis(0);
				plotter.plotColumn(1, true);
			} else if (numberOfCriteria > 2) {
				plotter.setXAxis(0);
				plotter.setYAxis(1);
				plotter.plotColumn(2, true);
			}
			plotter.setPointType(ScatterPlotter.POINTS);
			plotter.setVisible(true);
			plotter.addObjectVisualizer(this);
		}

		// change some plotter paras
		if (setDrawRange) {
			plotter.setDrawRange(0.0d, 1.0d, 0.0d, 1.0d);
		}
		plotter.setKey("Generation " + pop.getGeneration());
	}

	public static SimpleDataTable createDataTable(Population pop) {
		PerformanceVector prototype = pop.get(0).getPerformance();
		SimpleDataTable dataTable = new SimpleDataTable("Population", prototype.getCriteriaNames());
		return dataTable;
	}

	public static int fillDataTable(SimpleDataTable dataTable, Map<String, double[]> lastPopulation, Population pop,
			boolean drawDominated) {
		lastPopulation.clear();
		dataTable.clear();
		int numberOfCriteria = 0;
		for (int i = 0; i < pop.getNumberOfIndividuals(); i++) {
			boolean dominated = false;
			if (!drawDominated) {
				for (int j = 0; j < pop.getNumberOfIndividuals(); j++) {
					if (i == j) {
						continue;
					}
					if (NonDominatedSortingSelection.isDominated(pop.get(i), pop.get(j))) {
						dominated = true;
						break;
					}
				}
			}

			if (drawDominated || (!dominated)) {
				StringBuffer id = new StringBuffer(i + " (");
				PerformanceVector current = pop.get(i).getPerformance();
				numberOfCriteria = Math.max(numberOfCriteria, current.getSize());
				double[] data = new double[current.getSize()];
				for (int d = 0; d < data.length; d++) {
					data[d] = current.getCriterion(d).getFitness();
					if (d != 0) {
						id.append(", ");
					}
					id.append(Tools.formatNumber(data[d]));
				}
				id.append(")");
				dataTable.add(new SimpleDataTableRow(data, id.toString()));
				double[] weights = pop.get(i).getWeights();
				double[] clone = new double[weights.length];
				System.arraycopy(weights, 0, clone, 0, weights.length);
				lastPopulation.put(id.toString(), clone);
			}
		}
		return numberOfCriteria;
	}

	// ================================================================================

	@Override
	public boolean isCapableToVisualize(Object id) {
		return this.lastPopulation.get(id) != null;
	}

	@Override
	public String getTitle(Object id) {
		return id instanceof String ? (String) id : ((Double) id).toString();
	}

	@Override
	public String getDetailData(Object id, String fieldName) {
		return null;
	}

	@Override
	public String[] getFieldNames(Object id) {
		return new String[0];
	}

	@Override
	public void stopVisualization(Object id) {}

	@Override
	public void startVisualization(Object id) {
		double[] weights = lastPopulation.get(id);

		SimpleDataTable dataTable = new SimpleDataTable("Attribute Weights", new String[] { "Attribute", "Weight" });
		int a = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			dataTable
					.add(new SimpleDataTableRow(new double[] { dataTable.mapString(0, attribute.getName()), weights[a++] }));
		}

		Component visualizationComponent = new DataTableViewer(dataTable);
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());

		frame.getContentPane().add(new ExtendedJScrollPane(visualizationComponent), BorderLayout.CENTER);
		frame.setSize(600, 400);
		frame.setLocationRelativeTo(ApplicationFrame.getApplicationFrame());
		frame.setVisible(true);
	}
}
