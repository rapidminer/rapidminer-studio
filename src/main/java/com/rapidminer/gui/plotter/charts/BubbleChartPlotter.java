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

import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.BlockResult;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBubbleRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.RangeablePlotterAdapter;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.MathFunctions;


/**
 * This is the bubble chart plotter.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class BubbleChartPlotter extends RangeablePlotterAdapter {

	private static final long serialVersionUID = 4568273282283350833L;

	/** The axis names. */
	private static final String[] axisNames = { "x-Axis", "y-Axis", "Bubble Size" };

	private static final int X_AXIS = 0;
	private static final int Y_AXIS = 1;
	private static final int BUBBLE_SIZE_AXIS = 2;

	/** The pie data set. */
	private DefaultXYZDataset xyzDataSet = new DefaultXYZDataset();

	/** The columns which are used for the axes. */
	private int[] axis = new int[] { -1, -1, -1 };

	/** The column which is used for the color. */
	private int colorColumn = -1;

	private double bubbleSizeMin = 0;

	private double bubbleSizeMax = 1;

	private double xAxisMin = 0;

	private double xAxisMax = 1;

	private double yAxisMin = 0;

	private double yAxisMax = 1;

	private double minColor;

	private double maxColor;

	private double[] colors;

	private boolean[] logScales = new boolean[] { false, false };

	private boolean nominal = true;

	public BubbleChartPlotter(PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
	}

	public BubbleChartPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
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
		return 3;
	}

	@Override
	public void setAxis(int index, int dimension) {
		axis[index] = dimension;
		updatePlotter();
	}

	@Override
	public int getAxis(int index) {
		return axis[index];
	}

	@Override
	public String getAxisName(int index) {
		return axisNames[index];
	}

	/** Returns true if a log scale for this column is supported. Returns true for the x-axis. */
	@Override
	public boolean isSupportingLogScale(int axis) {
		if (axis == X_AXIS) {
			return true;
		} else {
			return false;
		}
	}

	/** Sets if the given axis should be plotted with log scale. */
	@Override
	public void setLogScale(int axis, boolean logScale) {
		logScales[axis] = logScale;
		updatePlotter();
	}

	private void prepareData() {
		if (colorColumn < 0 || getDataTable().isNominal(colorColumn)) {
			prepareNominalData();
		} else {
			prepareNumericalData();
		}
	}

	private void prepareNumericalData() {
		DataTable dataTable = getDataTable();
		this.nominal = false;
		xyzDataSet = new DefaultXYZDataset();

		if (axis[X_AXIS] >= 0 && axis[Y_AXIS] >= 0 && axis[BUBBLE_SIZE_AXIS] >= 0) {

			this.bubbleSizeMin = Double.POSITIVE_INFINITY;
			this.bubbleSizeMax = Double.NEGATIVE_INFINITY;
			this.xAxisMin = Double.POSITIVE_INFINITY;
			this.xAxisMax = Double.NEGATIVE_INFINITY;
			this.yAxisMin = Double.POSITIVE_INFINITY;
			this.yAxisMax = Double.NEGATIVE_INFINITY;
			this.minColor = Double.POSITIVE_INFINITY;
			this.maxColor = Double.NEGATIVE_INFINITY;

			List<double[]> dataList = new LinkedList<>();
			synchronized (dataTable) {
				Iterator<DataTableRow> i = dataTable.iterator();
				while (i.hasNext()) {
					DataTableRow row = i.next();

					double xValue = row.getValue(axis[X_AXIS]);
					double yValue = row.getValue(axis[Y_AXIS]);
					double bubbleSizeValue = row.getValue(axis[BUBBLE_SIZE_AXIS]);

					double colorValue = Double.NaN;
					if (colorColumn >= 0) {
						colorValue = row.getValue(colorColumn);
					}

					if (!Double.isNaN(xValue) && !Double.isNaN(yValue) && !Double.isNaN(bubbleSizeValue)) {
						double[] data = new double[4];
						data[X_AXIS] = xValue;
						data[Y_AXIS] = yValue;
						data[BUBBLE_SIZE_AXIS] = bubbleSizeValue;
						data[3] = colorValue;

						this.bubbleSizeMin = MathFunctions.robustMin(this.bubbleSizeMin, bubbleSizeValue);
						this.bubbleSizeMax = MathFunctions.robustMax(this.bubbleSizeMax, bubbleSizeValue);
						this.xAxisMin = MathFunctions.robustMin(this.xAxisMin, xValue);
						this.yAxisMin = MathFunctions.robustMin(this.yAxisMin, yValue);
						this.xAxisMax = MathFunctions.robustMax(this.xAxisMax, xValue);
						this.yAxisMax = MathFunctions.robustMax(this.yAxisMax, yValue);
						this.minColor = MathFunctions.robustMin(this.minColor, colorValue);
						this.maxColor = MathFunctions.robustMax(this.maxColor, colorValue);

						dataList.add(data);
					}
				}
			}

			double[][] data = new double[3][dataList.size()];
			this.colors = new double[dataList.size()];

			int index = 0;
			double scaleFactor = Math.min(this.xAxisMax - this.xAxisMin, this.yAxisMax - this.yAxisMin) / 4.0d;
			for (double[] d : dataList) {
				data[X_AXIS][index] = d[X_AXIS];
				data[Y_AXIS][index] = d[Y_AXIS];
				data[BUBBLE_SIZE_AXIS][index] = ((d[BUBBLE_SIZE_AXIS] - bubbleSizeMin) / (bubbleSizeMax - bubbleSizeMin) + 0.1)
						* scaleFactor;
				this.colors[index] = d[3];
				index++;
			}

			xyzDataSet.addSeries("All", data);
		}
	}

	private void prepareNominalData() {
		DataTable dataTable = getDataTable();
		this.nominal = true;
		xyzDataSet = new DefaultXYZDataset();

		if (axis[X_AXIS] >= 0 && axis[Y_AXIS] >= 0 && axis[BUBBLE_SIZE_AXIS] >= 0) {

			this.bubbleSizeMin = Double.POSITIVE_INFINITY;
			this.bubbleSizeMax = Double.NEGATIVE_INFINITY;
			this.xAxisMin = Double.POSITIVE_INFINITY;
			this.xAxisMax = Double.NEGATIVE_INFINITY;
			this.yAxisMin = Double.POSITIVE_INFINITY;
			this.yAxisMax = Double.NEGATIVE_INFINITY;
			this.minColor = Double.POSITIVE_INFINITY;
			this.maxColor = Double.NEGATIVE_INFINITY;

			Map<String, List<double[]>> dataCollection = new LinkedHashMap<>();

			synchronized (dataTable) {
				Iterator<DataTableRow> i = dataTable.iterator();
				while (i.hasNext()) {
					DataTableRow row = i.next();

					double xValue = row.getValue(axis[X_AXIS]);
					double yValue = row.getValue(axis[Y_AXIS]);
					double bubbleSizeValue = row.getValue(axis[BUBBLE_SIZE_AXIS]);

					double colorValue = Double.NaN;
					if (colorColumn >= 0) {
						colorValue = row.getValue(colorColumn);
					}

					if (!Double.isNaN(xValue) && !Double.isNaN(yValue) && !Double.isNaN(bubbleSizeValue)) {
						addPoint(dataTable, dataCollection, xValue, yValue, bubbleSizeValue, colorValue);
					}
				}
			}

			Iterator<Map.Entry<String, List<double[]>>> i = dataCollection.entrySet().iterator();
			double scaleFactor = Math.min(this.xAxisMax - this.xAxisMin, this.yAxisMax - this.yAxisMin) / 4.0d;
			while (i.hasNext()) {
				Map.Entry<String, List<double[]>> entry = i.next();
				String seriesName = entry.getKey();
				List<double[]> dataList = entry.getValue();
				double[][] data = new double[3][dataList.size()];
				int listCounter = 0;
				Iterator<double[]> j = dataList.iterator();
				while (j.hasNext()) {
					double[] current = j.next();
					data[X_AXIS][listCounter] = current[X_AXIS];
					data[Y_AXIS][listCounter] = current[Y_AXIS];
					data[BUBBLE_SIZE_AXIS][listCounter] = ((current[BUBBLE_SIZE_AXIS] - bubbleSizeMin)
							/ (bubbleSizeMax - bubbleSizeMin) + 0.1)
							* scaleFactor;
					listCounter++;
				}
				xyzDataSet.addSeries(seriesName, data);
			}
		}
	}

	private void addPoint(DataTable dataTable, Map<String, List<double[]>> dataCollection, double x, double y, double z,
			double color) {
		List<double[]> dataList = null;
		if (Double.isNaN(color)) {
			dataList = dataCollection.get("All");
			if (dataList == null) {
				dataList = new LinkedList<>();
				dataCollection.put("All", dataList);
			}
		} else {
			String name = color + "";
			if (dataTable.isNominal(colorColumn)) {
				name = dataTable.mapIndex(colorColumn, (int) color);
			} else if (dataTable.isDate(colorColumn)) {
				name = Tools.createDateAndFormat(color);
			} else if (dataTable.isTime(colorColumn)) {
				name = Tools.createTimeAndFormat(color);
			} else if (dataTable.isDateTime(colorColumn)) {
				name = Tools.createDateTimeAndFormat(color);
			}
			dataList = dataCollection.get(name);
			if (dataList == null) {
				dataList = new LinkedList<>();
				dataCollection.put(name, dataList);
			}
		}

		this.bubbleSizeMin = MathFunctions.robustMin(this.bubbleSizeMin, z);
		this.bubbleSizeMax = MathFunctions.robustMax(this.bubbleSizeMax, z);
		this.xAxisMin = MathFunctions.robustMin(this.xAxisMin, x);
		this.yAxisMin = MathFunctions.robustMin(this.yAxisMin, y);
		this.xAxisMax = MathFunctions.robustMax(this.xAxisMax, x);
		this.yAxisMax = MathFunctions.robustMax(this.yAxisMax, y);
		this.minColor = MathFunctions.robustMin(this.minColor, color);
		this.maxColor = MathFunctions.robustMax(this.maxColor, color);

		dataList.add(new double[] { x, y, z });
	}

	@Override
	public void updatePlotter() {
		final DataTable dataTable = getDataTable();

		prepareData();

		JFreeChart chart = ChartFactory.createBubbleChart(null, // chart title
				null, // domain axis label
				null, // range axis label
				xyzDataSet, // data
				PlotOrientation.VERTICAL, // orientation
				false, // include legend
				true, // tooltips
				false // URLs
				);

		if (axis[X_AXIS] >= 0 && axis[Y_AXIS] >= 0 && axis[BUBBLE_SIZE_AXIS] >= 0) {
			if (nominal) {
				int size = xyzDataSet.getSeriesCount();
				chart = ChartFactory.createBubbleChart(null, // chart title
						null, // domain axis label
						null, // range axis label
						xyzDataSet, // data
						PlotOrientation.VERTICAL, // orientation
						colorColumn >= 0 && size < 100 ? true : false, // include legend
								true, // tooltips
								false // URLs
						);

				// renderer settings
				XYBubbleRenderer renderer = (XYBubbleRenderer) chart.getXYPlot().getRenderer();
				renderer.setBaseOutlinePaint(Color.BLACK);

				if (size > 1) {
					for (int i = 0; i < size; i++) {
						renderer.setSeriesPaint(i, getColorProvider(true).getPointColor(i / (double) (size - 1)));
						renderer.setSeriesShape(i, new Ellipse2D.Double(-3, -3, 7, 7));
					}
				} else {
					renderer.setSeriesPaint(0, getColorProvider().getPointColor(1.0d));
					renderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 7, 7));
				}

				// legend settings
				LegendTitle legend = chart.getLegend();
				if (legend != null) {
					legend.setPosition(RectangleEdge.TOP);
					legend.setFrame(BlockBorder.NONE);
					legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
					legend.setItemFont(LABEL_FONT);
				}
			} else {
				chart = ChartFactory.createScatterPlot(null, // chart title
						null, // domain axis label
						null, // range axis label
						xyzDataSet, // data
						PlotOrientation.VERTICAL, // orientation
						false, // include legend
						true, // tooltips
						false // URLs
						);

				// renderer settings
				ColorizedBubbleRenderer renderer = new ColorizedBubbleRenderer(this.colors);
				renderer.setBaseOutlinePaint(Color.BLACK);
				chart.getXYPlot().setRenderer(renderer);

				// legend settings
				chart.addLegend(new LegendTitle(renderer) {

					private static final long serialVersionUID = 1288380309936848376L;

					@Override
					public Object draw(java.awt.Graphics2D g2, java.awt.geom.Rectangle2D area, java.lang.Object params) {
						if (dataTable.isDate(colorColumn) || dataTable.isTime(colorColumn)
								|| dataTable.isDateTime(colorColumn)) {
							drawSimpleDateLegend(g2, (int) (area.getCenterX() - 170), (int) (area.getCenterY() + 7),
									dataTable, colorColumn, minColor, maxColor);
							return new BlockResult();
						} else {
							final String minColorString = Tools.formatNumber(minColor);
							final String maxColorString = Tools.formatNumber(maxColor);
							drawSimpleNumericalLegend(g2, (int) (area.getCenterX() - 90), (int) (area.getCenterY() + 7),
									dataTable.getColumnName(colorColumn), minColorString, maxColorString);
							return new BlockResult();
						}
					}

					@Override
					public void draw(java.awt.Graphics2D g2, java.awt.geom.Rectangle2D area) {
						draw(g2, area, null);
					}

				});
			}
		}

		// GENERAL CHART SETTINGS

		// set the background colors for the chart...
		chart.setBackgroundPaint(Color.WHITE);
		chart.getPlot().setBackgroundPaint(Color.WHITE);
		chart.getPlot().setForegroundAlpha(0.7f);

		XYPlot plot = chart.getXYPlot();
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

		// domain axis
		if (axis[X_AXIS] >= 0) {
			if (dataTable.isNominal(axis[X_AXIS])) {
				String[] values = new String[dataTable.getNumberOfValues(axis[X_AXIS])];
				for (int i = 0; i < values.length; i++) {
					values[i] = dataTable.mapIndex(axis[X_AXIS], i);
				}
				plot.setDomainAxis(new SymbolAxis(dataTable.getColumnName(axis[X_AXIS]), values));
			} else if (dataTable.isDate(axis[X_AXIS]) || dataTable.isDateTime(axis[X_AXIS])) {
				DateAxis domainAxis = new DateAxis(dataTable.getColumnName(axis[X_AXIS]));
				domainAxis.setTimeZone(Tools.getPreferredTimeZone());
				plot.setDomainAxis(domainAxis);
			} else {
				if (logScales[X_AXIS]) {
					LogAxis domainAxis = new LogAxis(dataTable.getColumnName(axis[X_AXIS]));
					domainAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits(Locale.US));
					plot.setDomainAxis(domainAxis);
				} else {
					plot.setDomainAxis(new NumberAxis(dataTable.getColumnName(axis[X_AXIS])));
				}
			}
			Range range = getRangeForDimension(axis[X_AXIS]);
			if (range != null) {
				plot.getDomainAxis().setRange(range, true, false);
			} else {
				plot.getDomainAxis().setAutoRange(true);
			}
		}
		plot.getDomainAxis().setLabelFont(LABEL_FONT_BOLD);
		plot.getDomainAxis().setTickLabelFont(LABEL_FONT);

		// rotate labels
		if (isLabelRotating()) {
			plot.getDomainAxis().setTickLabelsVisible(true);
			plot.getDomainAxis().setVerticalTickLabels(true);
		}

		// range axis
		if (axis[Y_AXIS] >= 0) {
			if (dataTable.isNominal(axis[Y_AXIS])) {
				String[] values = new String[dataTable.getNumberOfValues(axis[Y_AXIS])];
				for (int i = 0; i < values.length; i++) {
					values[i] = dataTable.mapIndex(axis[Y_AXIS], i);
				}
				plot.setRangeAxis(new SymbolAxis(dataTable.getColumnName(axis[Y_AXIS]), values));
			} else if (dataTable.isDate(axis[Y_AXIS]) || dataTable.isDateTime(axis[Y_AXIS])) {
				DateAxis rangeAxis = new DateAxis(dataTable.getColumnName(axis[Y_AXIS]));
				rangeAxis.setTimeZone(Tools.getPreferredTimeZone());
				plot.setRangeAxis(rangeAxis);
			} else {
				plot.setRangeAxis(new NumberAxis(dataTable.getColumnName(axis[Y_AXIS])));
			}
			Range range = getRangeForDimension(axis[Y_AXIS]);
			if (range != null) {
				plot.getRangeAxis().setRange(range, true, false);
			} else {
				plot.getRangeAxis().setAutoRange(true);
			}
		}
		plot.getRangeAxis().setLabelFont(LABEL_FONT_BOLD);
		plot.getRangeAxis().setTickLabelFont(LABEL_FONT);

		AbstractChartPanel panel = getPlotterPanel();
		// Chart Panel Settings
		if (panel == null) {
			panel = createPanel(chart);
		} else {
			panel.setChart(chart);
		}

		// ATTENTION: WITHOUT THIS WE GET SEVERE MEMORY LEAKS!!!
		panel.getChartRenderingInfo().setEntityCollection(null);
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		if (index == 0) {
			return getRotateLabelComponent();
		} else {
			return null;
		}
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.BUBBLE_PLOT;
	}

	@Override
	public void dataTableSet() {
		updatePlotter();
	}

	@Override
	public Collection<String> resolveXAxis(int axisIndex) {
		if (axis[X_AXIS] != -1) {
			return Collections.singletonList(getDataTable().getColumnName(axis[X_AXIS]));
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public Collection<String> resolveYAxis(int axisIndex) {
		if (axis[Y_AXIS] != -1) {
			return Collections.singletonList(getDataTable().getColumnName(axis[Y_AXIS]));
		} else {
			return Collections.emptyList();
		}
	}
}
