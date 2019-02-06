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

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.math.plot.Plot3DPanel;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.conditions.PlotterCondition;
import com.rapidminer.gui.plotter.conditions.RowsPlotterCondition;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * This plotter can be used to create 3D surface plots of equidistant data.
 *
 * @author Sebastian Land, Ingo Mierswa, Thilo Kamradt
 * @deprecated since 9.2.0
 */
@Deprecated
public class SurfacePlot3D extends JMathPlotter3D {

	private static final long serialVersionUID = -8086776011628491876L;

	private static final int MAX_NUMBER_OF_ROWS = 100;

	private int xAxis = -1;
	private int yAxis = -1;
	private boolean[] zAxis = null;
	private boolean runningCalculation = false;

	public SurfacePlot3D(PlotterConfigurationModel settings) {
		super(settings);
	}

	public SurfacePlot3D(PlotterConfigurationModel settings, DataTable dataTable) {
		super(settings, dataTable);
	}

	// private final

	private void addGridPlot(String columnName, Color color, double[] yArray, double[] xArray, double[][] zArray) {
		((Plot3DPanel) getPlotPanel()).addGridPlot(columnName, color, yArray, xArray, zArray);
	}

	@Override
	public void update() {
		if (getAxis(0) != -1 && getAxis(1) != -1) {
			// check for changes at z Axis
			boolean zChanged = false;
			if (zAxis == null || zAxis.length != countColumns()) {
				zChanged = true;
				zAxis = new boolean[countColumns()];
				for (int counter = 0; counter < zAxis.length; counter++) {
					zAxis[counter] = getPlotColumn(counter);
				}
			} else {
				for (int counter = 0; counter < zAxis.length; counter++) {
					if (zAxis[counter] != getPlotColumn(counter)) {
						zChanged = true;
						zAxis[counter] = getPlotColumn(counter);
					}
				}
			}
			// only recalculate the plot if anything changed
			if ((xAxis != getAxis(1) || yAxis != getAxis(0) || zChanged) && !runningCalculation) {
				runningCalculation = true;
				xAxis = getAxis(1);
				yAxis = getAxis(0);
				// recalculate the plot
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						getPlotPanel().removeAllPlots();
					}
				});
				ProgressThread plotCalculationThread = new ProgressThread("surface3DPlot") {

					@Override
					public void run() {
						this.getProgressListener().setTotal(zAxis.length);
						for (int currentVariable = 0; currentVariable < zAxis.length; currentVariable++) {
							if (zAxis[currentVariable]) {
								List<Double> xSet = new LinkedList<>();
								List<Double> ySet = new LinkedList<>();
								Map<String, Double> zMap = new HashMap<>();
								DataTable table = getDataTable();
								synchronized (table) {
									Iterator<DataTableRow> iterator = table.iterator();
									while (iterator.hasNext()) {
										DataTableRow row = iterator.next();
										double x = row.getValue(xAxis);
										double y = row.getValue(yAxis);
										double z = row.getValue(currentVariable);
										if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(z)) {
											xSet.add(x);
											ySet.add(y);
											zMap.put(x + "+" + y, z);
										}
									}

									// because all sets have the same size
									int size = xSet.size();
									final double[] xArray = new double[size];
									final double[] yArray = new double[size];
									final double[][] zArray = new double[size][size];
									int xCounter = 0;
									Iterator<Double> x = xSet.iterator();
									double last = 0.0d;
									while (x.hasNext()) {// n^2
										xArray[xCounter] = x.next();
										Iterator<Double> y = ySet.iterator();
										int yCounter = 0;
										while (y.hasNext()) {
											yArray[yCounter] = y.next();
											Double value = zMap.get(xArray[xCounter] + "+" + yArray[yCounter]);
											if (value != null) {
												zArray[xCounter][yCounter] = value;
												last = value;
											} else {
												zArray[xCounter][yCounter] = last;
											}
											yCounter++;
										}
										xCounter++;
									}

									// PlotPanel construction
									if (xArray.length > 0 && yArray.length > 0 && zArray.length > 0) {
										final Color color = getColorProvider().getPointColor(
												(currentVariable + 1) / (double) zAxis.length);
										final String columnName = getDataTable().getColumnName(currentVariable);
										// add the GridPlot inside the GUI-Thread
										SwingUtilities.invokeLater(new Runnable() {

											@Override
											public void run() {

												addGridPlot(columnName, color, yArray, xArray, zArray);
											}
										});
									}
								}// end Synchronize
								this.getProgressListener().setCompleted(currentVariable + 1);
							}
						}
						runningCalculation = false;
					}
				};
				plotCalculationThread.start();
			}
		} else if (!runningCalculation) {
			// no valid axis are selected
			getPlotPanel().removeAllPlots();
		}
	}

	@Override
	public PlotterCondition getPlotterCondition() {
		return new RowsPlotterCondition(MAX_NUMBER_OF_ROWS);
	}

	@Override
	public int getValuePlotSelectionType() {
		return MULTIPLE_SELECTION;
	}

	@Override
	public String getPlotName() {
		return "z-Axis";
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.SURFACE_PLOT_3D;
	}

	@Override
	public JComponent getPlotter() {
		DataTable table = getDataTable();
		if (table != null && table.getNumberOfRows() > MAX_NUMBER_OF_ROWS) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.plotter.mathplot.SurfacePlot2D.too_many_examples",
					new Object[] { table.getNumberOfRows(), MAX_NUMBER_OF_ROWS });
			// Display Label with error message because Plot3DPanel can not handle such a lot of
			// data points
			JLabel label = new JLabel(I18N.getGUILabel("surface3DPlot.too_many_examples", MAX_NUMBER_OF_ROWS));
			label.setHorizontalAlignment(SwingConstants.CENTER);
			Font originalFont = label.getFont();
			label.setFont(originalFont.deriveFont((float) (originalFont.getSize() * 1.25)));
			originalFont.deriveFont(new AffineTransform());

			return label;
		} else {
			return super.getPlotter();
		}
	}
}
