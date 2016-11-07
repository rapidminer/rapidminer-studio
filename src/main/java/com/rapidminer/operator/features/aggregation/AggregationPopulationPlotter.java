/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.operator.features.aggregation;

import com.rapidminer.ObjectVisualizer;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.generator.FeatureGenerator;
import com.rapidminer.generator.GenerationException;
import com.rapidminer.gui.plotter.ScatterPlotter;
import com.rapidminer.gui.plotter.SimplePlotterDialog;
import com.rapidminer.gui.processeditor.results.ResultDisplayTools;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.tools.Tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.WindowConstants;


/**
 * Plots the current generation's Pareto set.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public class AggregationPopulationPlotter implements ObjectVisualizer {

	/** The original example set. */
	private ExampleSet originalExampleSet;

	/** All original attributes. */
	private Attribute[] allAttributes;

	/** The feature generator which should be used to create the individuals. */
	private FeatureGenerator generator;

	/** The plotter. */
	private SimplePlotterDialog plotter = null;

	/** The data table containing the individuals criteria data. */
	private SimpleDataTable criteriaDataTable = null;

	/** The last population. */
	private List<AggregationIndividual> lastPopulation;

	/** Creates plotter panel which is repainted every generation. */
	public AggregationPopulationPlotter(ExampleSet originalExampleSet, Attribute[] allAttributes, FeatureGenerator generator) {
		this.originalExampleSet = originalExampleSet;
		this.allAttributes = allAttributes;
		this.generator = generator;
	}

	public void operate(List<AggregationIndividual> pop) {
		if (pop.size() == 0) {
			return;
		}

		// init data table
		if (criteriaDataTable == null) {
			this.criteriaDataTable = createDataTable(pop);
		}

		// fill data table
		int numberOfCriteria = fillDataTable(this.criteriaDataTable, pop);

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

		this.lastPopulation = pop;
	}

	public SimpleDataTable createDataTable(List<AggregationIndividual> pop) {
		PerformanceVector prototype = pop.get(0).getPerformance();
		SimpleDataTable dataTable = new SimpleDataTable("Population", prototype.getCriteriaNames());
		return dataTable;
	}

	public int fillDataTable(SimpleDataTable dataTable, List<AggregationIndividual> pop) {
		dataTable.clear();
		int numberOfCriteria = 0;
		for (int i = 0; i < pop.size(); i++) {
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
		}
		return numberOfCriteria;
	}

	// ================================================================================

	@Override
	public boolean isCapableToVisualize(Object id) {
		int index = 0;
		if (id instanceof String) {
			String idString = (String) id;
			index = Integer.parseInt(idString.substring(0, idString.indexOf("(")).trim());
		} else {
			index = ((Double) id).intValue();
		}
		return ((index >= 0) && (index < lastPopulation.size()));
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
		int index = 0;
		if (id instanceof String) {
			String idString = (String) id;
			index = Integer.parseInt(idString.substring(0, idString.indexOf("(")).trim());
		} else {
			index = ((Double) id).intValue();
		}
		AggregationIndividual individual = lastPopulation.get(index);
		ExampleSet es = null;
		try {
			es = individual.createExampleSet(originalExampleSet, allAttributes, generator);
		} catch (GenerationException e) {
			throw new RuntimeException("Cannot visualize individual '" + index + "': " + e.getMessage());
		}
		Component visualizationComponent = ResultDisplayTools.createVisualizationComponent(es, null, es.getName());
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(new ExtendedJScrollPane(visualizationComponent), BorderLayout.CENTER);
		frame.setSize(600, 400);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
