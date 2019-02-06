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
package com.rapidminer.gui.plotter.charts;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.plotter.LocalNormalizationPlotterAdapter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.math.MathFunctions;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;


/**
 * This is the deviation chart plotter.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class DeviationChartPlotter extends LocalNormalizationPlotterAdapter {

	private static final long serialVersionUID = -8763693366081949249L;

	/** The currently used data table object. */
	private transient DataTable dataTable;

	/** The data set used for the plotter. */
	private YIntervalSeriesCollection dataset = null;

	/** The column which is used for the values. */
	private int colorColumn = -1;

	private String[] domainAxisMap = null;

	private ChartPanel panel = new ChartPanel(null);

	public DeviationChartPlotter(PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
	}

	public DeviationChartPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	@Override
	public JComponent getPlotter() {
		return panel;
	}

	private JFreeChart createChart(XYDataset dataset, boolean createLegend) {

		// create the chart...
		JFreeChart chart = ChartFactory.createXYLineChart(null,      // chart title
				null,                      // x axis label
				null,                      // y axis label
				dataset,                  // data
				PlotOrientation.VERTICAL, createLegend,                     // include legend
				true,                     // tooltips
				false                     // urls
				);

		chart.setBackgroundPaint(Color.white);

		// get a reference to the plot for further customization...
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

		DeviationRenderer renderer = new DeviationRenderer(true, false);
		Stroke stroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		if (dataset.getSeriesCount() == 1) {
			renderer.setSeriesStroke(0, stroke);
			renderer.setSeriesPaint(0, Color.RED);
			renderer.setSeriesFillPaint(0, Color.RED);
		} else {
			for (int i = 0; i < dataset.getSeriesCount(); i++) {
				renderer.setSeriesStroke(i, stroke);
				Color color = getColorProvider().getPointColor((double) i / (double) (dataset.getSeriesCount() - 1));
				renderer.setSeriesPaint(i, color);
				renderer.setSeriesFillPaint(i, color);
			}
		}
		renderer.setAlpha(0.12f);

		plot.setRenderer(renderer);

		ValueAxis valueAxis = plot.getRangeAxis();
		valueAxis.setLabelFont(LABEL_FONT_BOLD);
		valueAxis.setTickLabelFont(LABEL_FONT);

		return chart;
	}

	@Override
	public void setDataTable(DataTable dataTable) {
		super.setDataTable(dataTable);
		this.dataTable = dataTable;
		updatePlotter();
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		if (plot) {
			this.colorColumn = index;
		} else {
			this.colorColumn = -1;
		}
		updatePlotter();
	}

	@Override
	public boolean getPlotColumn(int index) {
		return colorColumn == index;
	}

	@Override
	public String getPlotName() {
		return "Color Column";
	}

	@Override
	public int getNumberOfAxes() {
		return 0;
	}

	private int prepareData() {
		// calculate min and max
		int columns = this.dataTable.getNumberOfColumns();
		double[] min = new double[columns];
		double[] max = new double[columns];
		for (int c = 0; c < columns; c++) {
			min[c] = Double.POSITIVE_INFINITY;
			max[c] = Double.NEGATIVE_INFINITY;
		}

		synchronized (dataTable) {
			Iterator<DataTableRow> i = dataTable.iterator();
			while (i.hasNext()) {
				DataTableRow row = i.next();
				for (int c = 0; c < dataTable.getNumberOfColumns(); c++) {
					double value = row.getValue(c);
					min[c] = MathFunctions.robustMin(min[c], value);
					max[c] = MathFunctions.robustMax(max[c], value);
				}
			}
		}

		synchronized (dataTable) {
			this.dataset = new YIntervalSeriesCollection();
			if (colorColumn >= 0 && dataTable.isNominal(colorColumn)) {
				for (int v = 0; v < dataTable.getNumberOfValues(colorColumn); v++) {
					String valueName = dataTable.mapIndex(colorColumn, v);
					YIntervalSeries series = new YIntervalSeries(valueName);
					boolean first = true;
					List<String> domainValues = new LinkedList<String>();
					for (int column = 0; column < dataTable.getNumberOfColumns(); column++) {
						if (!dataTable.isSpecial(column) && column != colorColumn) {
							Iterator<DataTableRow> i = this.dataTable.iterator();
							double sum = 0.0d;
							double squaredSum = 0.0d;
							int counter = 0;
							while (i.hasNext()) {
								DataTableRow row = i.next();
								if (row.getValue(colorColumn) != v) {
									continue;
								}
								double value = row.getValue(column);
								sum += value;
								squaredSum += value * value;
								counter++;
							}

							double mean = sum / counter;
							double deviation = Math.sqrt(squaredSum / counter - mean * mean);
							if (isLocalNormalized()) {
								mean = (mean - min[column]) / (max[column] - min[column]);
								deviation = (deviation - min[column]) / (max[column] - min[column]);
							}
							series.add(column, mean, mean - deviation, mean + deviation);
							domainValues.add(dataTable.getColumnName(column));
						}
					}
					if (first) {
						this.domainAxisMap = new String[domainValues.size()];
						domainValues.toArray(this.domainAxisMap);
					}
					first = false;
					dataset.addSeries(series);
				}
				return dataTable.getNumberOfValues(colorColumn);
			} else {
				YIntervalSeries series = new YIntervalSeries(dataTable.getName());
				List<String> domainValues = new LinkedList<String>();
				for (int column = 0; column < dataTable.getNumberOfColumns(); column++) {
					if (!dataTable.isSpecial(column) && column != colorColumn) {
						Iterator<DataTableRow> i = this.dataTable.iterator();
						double sum = 0.0d;
						double squaredSum = 0.0d;
						int counter = 0;
						while (i.hasNext()) {
							DataTableRow row = i.next();
							double value = row.getValue(column);
							sum += value;
							squaredSum += value * value;
							counter++;
						}

						double mean = sum / counter;
						double deviation = Math.sqrt(squaredSum / counter - mean * mean);
						if (isLocalNormalized()) {
							mean = (mean - min[column]) / (max[column] - min[column]);
							// deviation = (deviation - min[column]) / (max[column] - min[column]);
						}
						series.add(column, mean, mean - deviation, mean + deviation);
						domainValues.add(dataTable.getColumnName(column));
					}
				}
				dataset.addSeries(series);
				this.domainAxisMap = new String[domainValues.size()];
				domainValues.toArray(this.domainAxisMap);
				return 0;
			}
		}
	}

	@Override
	public void updatePlotter() {
		int categoryCount = prepareData();
		String maxClassesProperty = ParameterService
				.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_COLORS_CLASSLIMIT);
		int maxClasses = 20;
		try {
			if (maxClassesProperty != null) {
				maxClasses = Integer.parseInt(maxClassesProperty);
			}
		} catch (NumberFormatException e) {
			// LogService.getGlobal().log("Deviation plotter: cannot parse property 'rapidminer.gui.plotter.colors.classlimit', using maximal 20 different classes.",
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.plotter.charts.DeviationChartPlotter.parsing_property_error");
		}
		boolean createLegend = categoryCount > 0 && categoryCount < maxClasses;

		JFreeChart chart = createChart(this.dataset, createLegend);

		// set the background color for the chart...
		chart.setBackgroundPaint(Color.white);

		// domain axis
		SymbolAxis axis = null;
		if (this.dataTable.isSupportingColumnWeights()) {
			List<Double> weightList = new LinkedList<Double>();
			for (int column = 0; column < dataTable.getNumberOfColumns(); column++) {
				if (!dataTable.isSpecial(column) && column != colorColumn) {
					weightList.add(this.dataTable.getColumnWeight(column));
				}
			}
			double[] weights = new double[weightList.size()];
			int index = 0;
			for (Double d : weightList) {
				weights[index++] = d;
			}
			axis = new WeightBasedSymbolAxis(null, domainAxisMap, weights);
		} else {
			axis = new SymbolAxis(null, domainAxisMap);
		}
		axis.setTickLabelFont(LABEL_FONT);
		axis.setLabelFont(LABEL_FONT_BOLD);

		// rotate labels
		if (isLabelRotating()) {
			axis.setTickLabelsVisible(true);
			axis.setVerticalTickLabels(true);
		}

		chart.getXYPlot().setDomainAxis(axis);

		// legend settings
		LegendTitle legend = chart.getLegend();
		if (legend != null) {
			legend.setPosition(RectangleEdge.TOP);
			legend.setFrame(BlockBorder.NONE);
			legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
			legend.setItemFont(LABEL_FONT);
		}

		if (panel instanceof AbstractChartPanel) {
			panel.setChart(chart);
		} else {
			panel = new AbstractChartPanel(chart, getWidth(), getHeight() - MARGIN);
			final ChartPanelShiftController controller = new ChartPanelShiftController(panel);
			panel.addMouseListener(controller);
			panel.addMouseMotionListener(controller);
		}

		// ATTENTION: WITHOUT THIS WE GET SEVERE MEMORY LEAKS!!!
		panel.getChartRenderingInfo().setEntityCollection(null);
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		if (index == 0) {
			return getLocalNormalizationComponent();
		} else if (index == 1) {
			return getRotateLabelComponent();
		} else {
			return null;
		}
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.DEVIATION_PLOT;
	}
}
