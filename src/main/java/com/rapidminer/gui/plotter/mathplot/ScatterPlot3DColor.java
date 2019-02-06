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
package com.rapidminer.gui.plotter.mathplot;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.tools.math.MathFunctions;

import java.awt.Color;
import java.util.Iterator;

import org.math.plot.Plot3DPanel;


/**
 * This plotter can be used to create 3D scatter plots where a 4th dimension can be shown by using a
 * color scale.
 * 
 * @author Sebastian Land, Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ScatterPlot3DColor extends JMathPlotter3D {

	private static final long serialVersionUID = 6967871061963724679L;

	public ScatterPlot3DColor(PlotterConfigurationModel settings) {
		super(settings);
	}

	public ScatterPlot3DColor(PlotterConfigurationModel settings, DataTable dataTable) {
		super(settings, dataTable);
	}

	@Override
	public void update() {
		int colorColumn = -1;
		if (getAxis(0) != -1 && getAxis(1) != -1 && getAxis(2) != -1) {
			getPlotPanel().removeAllPlots();
			for (int currentVariable = 0; currentVariable < countColumns(); currentVariable++) {
				if (getPlotColumn(currentVariable)) {
					double min = Double.POSITIVE_INFINITY;
					double max = Double.NEGATIVE_INFINITY;
					colorColumn = currentVariable;
					DataTable table = getDataTable();
					synchronized (table) {
						Iterator<DataTableRow> iterator = table.iterator();
						// search for bounds
						while (iterator.hasNext()) {
							DataTableRow row = iterator.next();
							double value = row.getValue(currentVariable);
							min = MathFunctions.robustMin(min, value);
							max = MathFunctions.robustMax(max, value);
						}
						iterator = getDataTable().iterator();
						while (iterator.hasNext()) {
							double[][] data = new double[1][3];
							DataTableRow row = iterator.next();
							data[0][0] = row.getValue(getAxis(0));
							data[0][1] = row.getValue(getAxis(1));
							data[0][2] = row.getValue(getAxis(2));
							double colorValue = getColorProvider().getPointColorValue(table, row, currentVariable, min, max);
							if (Double.isNaN(colorValue)) {
								colorValue = 0.0d;
							}
							Color color = getColorProvider().getPointColor(colorValue);
							if (!Double.isNaN(data[0][0]) && !Double.isNaN(data[0][1]) && !Double.isNaN(data[0][2])) {
								((Plot3DPanel) getPlotPanel()).addScatterPlot(getDataTable().getColumnName(currentVariable),
										color, data);
							}
						}
					}
				}
			}
		} else {
			getPlotPanel().removeAllPlots();
		}
		if (colorColumn != -1) {
			getLegendComponent().setLegendColumn(getDataTable(), colorColumn);
		}
	}

	@Override
	public boolean hasLegend() {
		return false;
	}

	@Override
	public boolean hasRapidMinerValueLegend() {
		return true;
	}

	@Override
	public int getNumberOfAxes() {
		return 3;
	}

	@Override
	public String getAxisName(int index) {
		switch (index) {
			case 0:
				return "x-Axis";
			case 1:
				return "y-Axis";
			case 2:
				return "z-Axis";
			default:
				return "empty";
		}
	}

	@Override
	public String getPlotName() {
		return "Color";
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.SCATTER_PLOT_3D_COLOR;
	}
}
