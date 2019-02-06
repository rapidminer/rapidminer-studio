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
import java.awt.Font;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.swing.JComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BlockResult;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.block.LabelBlock;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;

import com.rapidminer.ObjectVisualizer;
import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.RangeablePlotterAdapter;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.ObjectVisualizerService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.MathFunctions;


/**
 * This is the abstract superclass for scatter plotter based on JFreeChart.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class Abstract2DChartPlotter extends RangeablePlotterAdapter {

	private static final long serialVersionUID = 4568273282283350833L;

	public static class SeriesAndItem {

		private int series;

		private int item;

		public SeriesAndItem(int series, int item) {
			this.series = series;
			this.item = item;
		}

		@Override
		public int hashCode() {
			return Integer.valueOf(series).hashCode() ^ Integer.valueOf(item).hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof SeriesAndItem)) {
				return false;
			}
			SeriesAndItem s = (SeriesAndItem) o;
			return this.series == s.series && this.item == s.item;
		}
	}

	/** The axis names. */
	private static final String[] axisNames = { "x-Axis", "y-Axis" };

	private static final int X_AXIS = 0;
	private static final int Y_AXIS = 1;
	private static final int COLOR_AXIS = 2;

	/** The currently used data table object. */
	protected transient DataTable dataTable;

	private XYDataset dataSet = new DefaultXYDataset();

	/** The columns which are used for the axes. */
	private int[] axis = new int[] { -1, -1 };

	/** The column which is used for the color. */
	private int colorColumn = -1;

	private int jitterAmount = 0;

	private boolean[] logScales = new boolean[] { false, false };

	private boolean plotColumnsLogScale = false;

	private double minColor;

	private double maxColor;

	private boolean nominal = true;

	private Map<SeriesAndItem, String> idMap = new HashMap<SeriesAndItem, String>();

	public Abstract2DChartPlotter(final PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);

	}

	public Abstract2DChartPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	/** Subclasses have to implement this method. */
	public abstract AbstractXYItemRenderer getItemRenderer(boolean nominal, int size, double minColor, double maxColor);

	/** Returns true. */
	@Override
	public boolean canHandleJitter() {
		return true;
	}

	/** Sets the level of jitter and initiates a repaint. */
	@Override
	public void setJitter(int jitter) {
		this.jitterAmount = jitter;
		updatePlotter();
	}

	/**
	 * Returns true if a log scale for this column is supported. Returns true for the x- and y-axis.
	 */
	@Override
	public boolean isSupportingLogScale(int axis) {
		if (axis == X_AXIS || axis == Y_AXIS) {
			return true;
		} else {
			return false;
		}
	}

	/** Returns true. */
	@Override
	public boolean isSupportingLogScaleForPlotColumns() {
		return true;
	}

	/** Sets if the given axis should be plotted with log scale. */
	@Override
	public void setLogScale(int axis, boolean logScale) {
		logScales[axis] = logScale;
		updatePlotter();
	}

	/** Sets if the given axis should be plotted with log scale. */
	@Override
	public void setLogScaleForPlotColumns(boolean logScale) {
		plotColumnsLogScale = logScale;
		updatePlotter();
	}

	@Override
	public void dataTableSet() {
		this.dataTable = getDataTable();
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
		return axisNames.length;
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

	private void prepareData() {
		idMap.clear();

		if (colorColumn < 0 || dataTable.isNominal(colorColumn)) {
			prepareNominalData();
		} else {
			prepareNumericalData();
		}
	}

	private void prepareNumericalData() {
		this.nominal = false;
		dataSet = new DefaultXYZDataset();

		if (axis[X_AXIS] >= 0 && axis[Y_AXIS] >= 0) {
			this.minColor = Double.POSITIVE_INFINITY;
			this.maxColor = Double.NEGATIVE_INFINITY;

			List<double[]> dataList = new LinkedList<double[]>();
			List<String> idList = new LinkedList<String>();
			synchronized (dataTable) {
				Iterator<DataTableRow> i = this.dataTable.iterator();
				while (i.hasNext()) {
					DataTableRow row = i.next();

					double xValue = Double.NaN;
					if (axis[X_AXIS] >= 0) {
						xValue = row.getValue(axis[X_AXIS]);
					}

					double yValue = Double.NaN;
					if (axis[Y_AXIS] >= 0) {
						yValue = row.getValue(axis[Y_AXIS]);
					}

					double colorValue = Double.NaN;
					if (colorColumn >= 0) {
						colorValue = row.getValue(colorColumn);
					}

					if (plotColumnsLogScale) {
						if (Tools.isLessEqual(colorValue, 0.0d)) {
							colorValue = 0;
						} else {
							colorValue = Math.log10(colorValue);
						}
					}

					// TM: removed check
					// if (!Double.isNaN(xValue) && !Double.isNaN(yValue)) {
					double[] data = new double[3];
					data[X_AXIS] = xValue;
					data[Y_AXIS] = yValue;
					data[COLOR_AXIS] = colorValue;

					if (!Double.isNaN(colorValue)) {
						this.minColor = Math.min(this.minColor, colorValue);
						this.maxColor = Math.max(this.maxColor, colorValue);
					}

					dataList.add(data);
					idList.add(row.getId());
					// }
				}
			}

			double[][] data = new double[3][dataList.size()];

			double minX = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY;
			double minY = Double.POSITIVE_INFINITY;
			double maxY = Double.NEGATIVE_INFINITY;

			int index = 0;
			for (double[] d : dataList) {
				data[X_AXIS][index] = d[X_AXIS];
				data[Y_AXIS][index] = d[Y_AXIS];
				data[COLOR_AXIS][index] = d[COLOR_AXIS];

				minX = MathFunctions.robustMin(minX, d[X_AXIS]);
				maxX = MathFunctions.robustMax(maxX, d[X_AXIS]);
				minY = MathFunctions.robustMin(minY, d[Y_AXIS]);
				maxY = MathFunctions.robustMax(maxY, d[Y_AXIS]);

				index++;
			}

			// jittering
			if (this.jitterAmount > 0) {
				Random jitterRandom = new Random(2001);
				double oldXRange = maxX - minX;
				double oldYRange = maxY - minY;
				for (int i = 0; i < dataList.size(); i++) {
					if (Double.isInfinite(oldXRange) || Double.isNaN(oldXRange)) {
						oldXRange = 0;
					}
					if (Double.isInfinite(oldYRange) || Double.isNaN(oldYRange)) {
						oldYRange = 0;
					}
					double pertX = oldXRange * (jitterAmount / 200.0d) * jitterRandom.nextGaussian();
					double pertY = oldYRange * (jitterAmount / 200.0d) * jitterRandom.nextGaussian();
					data[X_AXIS][i] += pertX;
					data[Y_AXIS][i] += pertY;
				}
			}

			// add data
			((DefaultXYZDataset) dataSet).addSeries("All", data);

			// id handling
			int idCounter = 0;
			for (String id : idList) {
				idMap.put(new SeriesAndItem(0, idCounter++), id);
			}
		}
	}

	protected String getId(int series, int index) {
		return idMap.get(new SeriesAndItem(series, index));
	}

	private void prepareNominalData() {
		this.nominal = true;
		dataSet = new DefaultXYDataset();

		if (axis[X_AXIS] >= 0 && axis[Y_AXIS] >= 0) {
			Map<String, List<double[]>> dataCollection = new LinkedHashMap<String, List<double[]>>();
			Map<String, List<String>> idCollection = new LinkedHashMap<String, List<String>>();

			synchronized (dataTable) {
				if (colorColumn >= 0) {
					for (int v = 0; v < dataTable.getNumberOfValues(colorColumn); v++) {
						String key = dataTable.mapIndex(colorColumn, v);
						if(key !=null) {
							dataCollection.put(key, new LinkedList<>());
							idCollection.put(key, new LinkedList<>());
						}
					}
				}

				for (DataTableRow row : dataTable) {
					double xValue = row.getValue(axis[X_AXIS]);
					double yValue = row.getValue(axis[Y_AXIS]);

					double colorValue = Double.NaN;
					if (colorColumn >= 0) {
						colorValue = row.getValue(colorColumn);
					}

					// TM: removed check
					// if (!Double.isNaN(xValue) && !Double.isNaN(yValue)) {
					addPoint(dataCollection, idCollection, row.getId(), xValue, yValue, colorValue);
					// }
				}
			}

			double minX = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY;
			double minY = Double.POSITIVE_INFINITY;
			double maxY = Double.NEGATIVE_INFINITY;

			Iterator<Map.Entry<String, List<double[]>>> i = dataCollection.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<String, List<double[]>> entry = i.next();
				List<double[]> dataList = entry.getValue();
				Iterator<double[]> j = dataList.iterator();
				while (j.hasNext()) {
					double[] current = j.next();
					minX = MathFunctions.robustMin(minX, current[X_AXIS]);
					maxX = MathFunctions.robustMax(maxX, current[X_AXIS]);
					minY = MathFunctions.robustMin(minY, current[Y_AXIS]);
					maxY = MathFunctions.robustMax(maxY, current[Y_AXIS]);
				}
			}

			Random jitterRandom = new Random(2001);
			double oldXRange = maxX - minX;
			double oldYRange = maxY - minY;

			if (Double.isInfinite(oldXRange) || Double.isNaN(oldXRange)) {
				oldXRange = 0;
			}
			if (Double.isInfinite(oldYRange) || Double.isNaN(oldYRange)) {
				oldYRange = 0;
			}

			i = dataCollection.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<String, List<double[]>> entry = i.next();
				String seriesName = entry.getKey();
				List<double[]> dataList = entry.getValue();

				double[][] data = new double[2][dataList.size()];
				int listCounter = 0;
				Iterator<double[]> j = dataList.iterator();
				while (j.hasNext()) {
					double[] current = j.next();
					data[X_AXIS][listCounter] = current[X_AXIS];
					data[Y_AXIS][listCounter] = current[Y_AXIS];

					if (this.jitterAmount > 0) {
						double pertX = oldXRange * (jitterAmount / 200.0d) * jitterRandom.nextGaussian();
						double pertY = oldYRange * (jitterAmount / 200.0d) * jitterRandom.nextGaussian();
						data[X_AXIS][listCounter] += pertX;
						data[Y_AXIS][listCounter] += pertY;
					}

					listCounter++;
				}

				((DefaultXYDataset) dataSet).addSeries(seriesName, data);
			}

			int seriesCounter = 0;
			Iterator<List<String>> v = idCollection.values().iterator();
			while (v.hasNext()) {
				List<String> idList = v.next();
				int itemCounter = 0;
				Iterator<String> j = idList.iterator();
				while (j.hasNext()) {
					idMap.put(new SeriesAndItem(seriesCounter, itemCounter++), j.next());
				}
				seriesCounter++;
			}
		}
	}

	private void addPoint(Map<String, List<double[]>> dataCollection, Map<String, List<String>> idCollection, String id,
			double x, double y, double color) {
		List<double[]> dataList = null;
		List<String> idList = null;

		if (Double.isNaN(color)) {
			dataList = dataCollection.get("Unknown");
			if (dataList == null) {
				dataList = new LinkedList<double[]>();
				dataCollection.put("Unknown", dataList);
			}

			idList = idCollection.get("Unknown");
			if (idList == null) {
				idList = new LinkedList<String>();
				idCollection.put("Unknown", idList);
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
				dataList = new LinkedList<double[]>();
				dataCollection.put(name, dataList);
			}
			idList = idCollection.get(name);
			if (idList == null) {
				idList = new LinkedList<String>();
				idCollection.put(name, idList);
			}
		}

		dataList.add(new double[] { x, y });
		idList.add(id);
	}

	@Override
	public void updatePlotter() {
		prepareData();
		JFreeChart chart;
		if (axis[X_AXIS] >= 0 && axis[Y_AXIS] >= 0) {
			if (nominal) {
				int size = dataSet.getSeriesCount();
				chart = ChartFactory.createScatterPlot(null,                     // chart title
						null,                     // domain axis label
						null,                     // range axis label
						dataSet,                  // data
						PlotOrientation.VERTICAL, // orientation
						colorColumn >= 0 && size < 100 ? true : false, // include legend
						true,                     // tooltips
						false                     // URLs
				);

				// renderer settings
				try {
					chart.getXYPlot().setRenderer(getItemRenderer(nominal, size, this.minColor, this.maxColor));
				} catch (Exception e) {
					// do nothing
				}

				// legend settings
				LegendTitle legend = chart.getLegend();
				if (legend != null) {
					legend.setPosition(RectangleEdge.TOP);
					legend.setFrame(BlockBorder.NONE);
					legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
					legend.setItemFont(LABEL_FONT);

					BlockContainer wrapper = new BlockContainer(new BorderArrangement());

					LabelBlock title = new LabelBlock(getDataTable().getColumnName(colorColumn),
							FontTools.getFont(Font.SANS_SERIF, Font.BOLD, 12));
					title.setPadding(0, 5, 5, 5);
					wrapper.add(title, RectangleEdge.LEFT);

					BlockContainer items = legend.getItemContainer();
					wrapper.add(items, RectangleEdge.RIGHT);

					legend.setWrapper(wrapper);
				}
			} else {

				chart = ChartFactory.createScatterPlot(null,                     // chart title
						null,                     // domain axis label
						null,                     // range axis label
						dataSet,                  // data
						PlotOrientation.VERTICAL, // orientation
						false,                    // include legend
						true,                     // tooltips
						false                     // URLs
				);

				// renderer settings
				try {
					chart.getXYPlot().setRenderer(getItemRenderer(nominal, -1, minColor, maxColor));
				} catch (Exception e) {
					// do nothing
				}

				LegendTitle legendTitle = new LegendTitle(chart.getXYPlot().getRenderer()) {

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
							drawSimpleNumericalLegend(g2, (int) (area.getCenterX() - 75), (int) (area.getCenterY() + 7),
									getDataTable().getColumnName(colorColumn), minColorString, maxColorString);
							return new BlockResult();
						}
					}

					@Override
					public void draw(java.awt.Graphics2D g2, java.awt.geom.Rectangle2D area) {
						draw(g2, area, null);
					}

				};

				BlockContainer wrapper = new BlockContainer(new BorderArrangement());

				LabelBlock title = new LabelBlock(getDataTable().getColumnName(colorColumn),
						FontTools.getFont(Font.SANS_SERIF, Font.BOLD, 12));
				title.setPadding(0, 5, 5, 5);
				wrapper.add(title, RectangleEdge.LEFT);

				BlockContainer items = legendTitle.getItemContainer();
				wrapper.add(items, RectangleEdge.RIGHT);

				legendTitle.setWrapper(wrapper);

				chart.addLegend(legendTitle);
			}
		} else {
			chart = ChartFactory.createScatterPlot(null,                     // chart title
					null,                     // domain axis label
					null,                     // range axis label
					dataSet,                  // data
					PlotOrientation.VERTICAL, // orientation
					false,                    // include legend
					true,                     // tooltips
					false                     // URLs
			);
		}

		// GENERAL CHART SETTINGS

		// set the background colors for the chart...
		chart.setBackgroundPaint(Color.WHITE);
		chart.getPlot().setBackgroundPaint(Color.WHITE);
		chart.setAntiAlias(false);

		// general plot settings
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
					NumberAxis domainAxis = new NumberAxis(dataTable.getColumnName(axis[X_AXIS]));
					domainAxis.setAutoRangeStickyZero(false);
					domainAxis.setAutoRangeIncludesZero(false);
					plot.setDomainAxis(domainAxis);
				}
			}
		}

		if (axis[X_AXIS] >= 0) {
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
				if (logScales[Y_AXIS]) {
					LogAxis rangeAxis = new LogAxis(dataTable.getColumnName(axis[Y_AXIS]));
					rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits(Locale.US));
					plot.setRangeAxis(rangeAxis);
				} else {
					NumberAxis rangeAxis = new NumberAxis(dataTable.getColumnName(axis[Y_AXIS]));
					rangeAxis.setAutoRangeStickyZero(false);
					rangeAxis.setAutoRangeIncludesZero(false);
					plot.setRangeAxis(rangeAxis);
				}
			}
		}
		plot.getRangeAxis().setLabelFont(LABEL_FONT_BOLD);
		plot.getRangeAxis().setTickLabelFont(LABEL_FONT);

		if (axis[Y_AXIS] >= 0) {
			Range range = getRangeForDimension(axis[Y_AXIS]);
			if (range != null) {
				plot.getRangeAxis().setRange(range, true, false);
			} else {
				plot.getRangeAxis().setAutoRange(true);
			}
		}

		// Chart Panel Settings
		AbstractChartPanel panel = getPlotterPanel();
		if (panel == null) {
			panel = createPanel(chart);

			// react to mouse clicks
			panel.addChartMouseListener(new ChartMouseListener() {

				@Override
				public void chartMouseClicked(ChartMouseEvent e) {
					if (e.getTrigger().getClickCount() > 1) {
						if (e.getEntity() instanceof XYItemEntity) {
							XYItemEntity entity = (XYItemEntity) e.getEntity();
							if (entity != null) {
								String id = idMap.get(new SeriesAndItem(entity.getSeriesIndex(), entity.getItem()));
								if (id != null) {
									ObjectVisualizer visualizer = ObjectVisualizerService.getVisualizerForObject(dataTable);
									visualizer.startVisualization(id);
								}
							}
						}
					}
				}

				@Override
				public void chartMouseMoved(ChartMouseEvent e) {}
			});

		} else {
			panel.setChart(chart);
		}

		// tooltips
		class CustomXYToolTipGenerator implements XYToolTipGenerator {

			public CustomXYToolTipGenerator() {}

			private String formatValue(int axis, double value) {
				if (dataTable.isNominal(axis)) {
					// TODO add mapping of value to nominal value
					return Tools.formatIntegerIfPossible(value);
				} else if (dataTable.isNumerical(axis)) {
					return Tools.formatIntegerIfPossible(value);
				} else if (dataTable.isDate(axis)) {
					return Tools.createDateAndFormat(value);
				} else if (dataTable.isTime(axis)) {
					return Tools.createTimeAndFormat(value);
				} else if (dataTable.isDateTime(axis)) {
					return Tools.createDateTimeAndFormat(value);
				}
				return "?";
			}

			@Override
			public String generateToolTip(XYDataset dataset, int row, int column) {
				String id = idMap.get(new SeriesAndItem(row, column));
				if (id != null) {
					return "<html><b>Id: " + id + "</b> (" + dataset.getSeriesKey(row) + ", "
							+ formatValue(axis[X_AXIS], dataset.getXValue(row, column)) + ", "
							+ formatValue(axis[Y_AXIS], dataset.getYValue(row, column)) + ")</html>";

				} else {
					return "<html>(" + dataset.getSeriesKey(row) + ", "
							+ formatValue(axis[X_AXIS], dataset.getXValue(row, column)) + ", "
							+ formatValue(axis[Y_AXIS], dataset.getYValue(row, column)) + ")</html>";
				}
			}
		}

		for (int i = 0; i < dataSet.getSeriesCount(); i++) {
			plot.getRenderer().setSeriesToolTipGenerator(i, new CustomXYToolTipGenerator());
		}
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
	public Collection<String> resolveXAxis(int axisIndex) {
		if (axis[X_AXIS] != -1) {
			return Collections.singletonList(dataTable.getColumnName(axis[X_AXIS]));
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public Collection<String> resolveYAxis(int axisIndex) {
		if (axis[Y_AXIS] != -1) {
			return Collections.singletonList(dataTable.getColumnName(axis[Y_AXIS]));
		} else {
			return Collections.emptyList();
		}
	}
}
