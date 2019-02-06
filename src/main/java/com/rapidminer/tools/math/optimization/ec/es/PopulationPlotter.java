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
package com.rapidminer.tools.math.optimization.ec.es;

import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.gui.plotter.ScatterPlotter;
import com.rapidminer.gui.plotter.SimplePlotterDialog;
import com.rapidminer.tools.Tools;


/**
 * Plots the current generation's Pareto set.
 * 
 * @author Ingo Mierswa
 */
public class PopulationPlotter implements PopulationOperator {

	/** The plotter. */
	private SimplePlotterDialog plotter = null;

	/** The data table containing the individuals criteria data. */
	private SimpleDataTable criteriaDataTable = null;

	@Override
	public void operate(Population pop) {
		if (pop.getNumberOfIndividuals() == 0) {
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
			plotter = new SimplePlotterDialog(null, criteriaDataTable, -1, -1, true, false);
			plotter.setCreateOtherPlottersEnabled(false);
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
		}
	}

	public SimpleDataTable createDataTable(Population pop) {
		double[] prototype = pop.get(0).getFitnessValues();
		String[] names = new String[prototype.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = "criterion" + i;
		}
		SimpleDataTable dataTable = new SimpleDataTable("Population", names);
		return dataTable;
	}

	public int fillDataTable(SimpleDataTable dataTable, Population pop) {
		dataTable.clear();
		int numberOfCriteria = 0;
		for (int i = 0; i < pop.getNumberOfIndividuals(); i++) {
			StringBuffer id = new StringBuffer(i + " (");
			double[] currentFitness = pop.get(i).getFitnessValues();
			numberOfCriteria = Math.max(numberOfCriteria, currentFitness.length);
			double[] data = new double[currentFitness.length];
			for (int d = 0; d < data.length; d++) {
				data[d] = currentFitness[d];
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

	public void setCreateOtherPlottersEnabled(boolean enabled) {
		this.plotter.setCreateOtherPlottersEnabled(enabled);
	}
}
