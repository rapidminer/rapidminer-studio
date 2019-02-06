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

import com.rapidminer.ObjectVisualizer;
import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.plotter.LabelRotatingPlotterAdapter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.tools.ObjectVisualizerService;
import com.rapidminer.tools.Tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;


/**
 * This is a multiple scatter plotter.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class MultipleScatterPlotter extends LabelRotatingPlotterAdapter {

	private static final long serialVersionUID = 4568273282283350833L;

	public static final String PARAMETER_POINTS_AND_LINES = "points_and_lines";

	public static final String PARAMETER_LINES = "show_lines";

	public static final String PARAMETER_POINTS = "show_points";

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

	/** The currently used data table object. */
	private transient DataTable dataTable;

	private XYDataset dataSet = new DefaultXYDataset();

	/** The columns which are used for the axes. */
	private int xAxis = -1;

	/** The column which is used for the color. */
	private boolean[] plotColumns;

	private int jitterAmount = 0;

	private boolean xLogScale = false;

	private List<Integer> plotIndexToColumnIndexMap = new ArrayList<Integer>();

	private Map<SeriesAndItem, String> idMap = new HashMap<SeriesAndItem, String>();

	private ChartPanel panel = new ChartPanel(null);

	private JButton pointsAndLinesOptions = new JButton("Points and Lines...");

	private boolean[] showPoints;

	private boolean[] showLines;

	public MultipleScatterPlotter(final PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);

		pointsAndLinesOptions
				.setToolTipText("Opens a dialog where the draw type (points, lines, or points and lines) for each dimension can be specified.");
		pointsAndLinesOptions.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				showPointsAndLinesOptions();
			}
		});
	}

	public MultipleScatterPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	@Override
	public void setDataTable(DataTable dataTable) {
		super.setDataTable(dataTable);
		this.dataTable = dataTable;
		this.plotColumns = new boolean[dataTable.getNumberOfColumns()];
		this.showLines = new boolean[dataTable.getNumberOfColumns()];
		this.showPoints = new boolean[dataTable.getNumberOfColumns()];
		for (int i = 0; i < showLines.length; i++) {
			showLines[i] = false;
			showPoints[i] = true;
		}
		updatePlotter();
	}

	@Override
	public int getValuePlotSelectionType() {
		return MULTIPLE_SELECTION;
	}

	/** Returns a line icon depending on the index. */
	@Override
	public Icon getIcon(int index) {
		return null;
	}

	private void showPointsAndLinesOptions() {
		String[] names = new String[this.dataTable.getNumberOfColumns()];
		for (int i = 0; i < names.length; i++) {
			names[i] = this.dataTable.getColumnName(i);
		}
		PointsAndLinesDialog dialog = new PointsAndLinesDialog(names, this.showPoints, this.showLines);
		dialog.setVisible(true);
		if (dialog.isOk()) {
			List<String[]> selectedValues = new ArrayList<String[]>(names.length);
			for (int i = 0; i < names.length; i++) {
				String[] selected = new String[2];
				selected[0] = dialog.showPoints(i) + "";
				selected[1] = dialog.showLines(i) + "";
				selectedValues.add(selected);
			}
			// now set it to parameter
			settings.setParameterAsString(PARAMETER_POINTS_AND_LINES, ParameterTypeList.transformList2String(selectedValues));
		}
	}

	public void setShowLines(int index, boolean show) {
		this.showLines[index] = show;
	}

	public void setShowPoints(int index, boolean show) {
		this.showPoints[index] = show;
	}

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
		if (axis == 0) {
			return true;
		} else {
			return false;
		}
	}

	/** Sets if the given axis should be plotted with log scale. */
	@Override
	public void setLogScale(int axis, boolean logScale) {
		if (axis == 0) {
			xLogScale = logScale;
			updatePlotter();
		}
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		this.plotColumns[index] = plot;
		updatePlotter();
	}

	@Override
	public boolean getPlotColumn(int index) {
		return this.plotColumns[index];
	}

	@Override
	public String getPlotName() {
		return "y-Axis";
	}

	@Override
	public int getNumberOfAxes() {
		return 1;
	}

	@Override
	public void setAxis(int index, int dimension) {
		if (index == 0) {
			xAxis = dimension;
			updatePlotter();
		}
	}

	@Override
	public int getAxis(int index) {
		if (index == 0) {
			return xAxis;
		} else {
			return -1;
		}
	}

	@Override
	public String getAxisName(int index) {
		if (index == 0) {
			return "x-Axis";
		} else {
			return "None";
		}
	}

	private void prepareData() {
		idMap.clear();
		this.plotIndexToColumnIndexMap.clear();
		dataSet = new DefaultXYDataset();

		if (xAxis >= 0) {
			Map<String, List<double[]>> dataCollection = new LinkedHashMap<String, List<double[]>>();
			Map<String, List<String>> idCollection = new LinkedHashMap<String, List<String>>();

			synchronized (dataTable) {
				for (int column = 0; column < plotColumns.length; column++) {
					if (plotColumns[column]) {
						plotIndexToColumnIndexMap.add(column);
						String columnName = this.dataTable.getColumnName(column);
						for (DataTableRow row : dataTable) {
							double xValue = row.getValue(xAxis);
							double yValue = row.getValue(column);

							if (!Double.isNaN(xValue) && !Double.isNaN(yValue)) {
								addPoint(dataCollection, idCollection, row.getId(), xValue, yValue, columnName);
							}
						}
					}
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
					minX = Math.min(minX, current[0]);
					maxX = Math.max(maxX, current[0]);
					minY = Math.min(minY, current[1]);
					maxY = Math.max(maxY, current[1]);
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
					data[0][listCounter] = current[0];
					data[1][listCounter] = current[1];

					if (this.jitterAmount > 0) {
						double pertX = oldXRange * (jitterAmount / 200.0d) * jitterRandom.nextGaussian();
						double pertY = oldYRange * (jitterAmount / 200.0d) * jitterRandom.nextGaussian();
						data[0][listCounter] += pertX;
						data[1][listCounter] += pertY;
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
			double x, double y, String columnName) {
		List<double[]> dataList = null;
		List<String> idList = null;

		dataList = dataCollection.get(columnName);
		if (dataList == null) {
			dataList = new LinkedList<double[]>();
			dataCollection.put(columnName, dataList);
		}

		idList = idCollection.get(columnName);
		if (idList == null) {
			idList = new LinkedList<String>();
			idCollection.put(columnName, idList);
		}

		dataList.add(new double[] { x, y });
		idList.add(id);
	}

	@Override
	public JComponent getPlotter() {
		return panel;
	}

	@Override
	public void updatePlotter() {

		prepareData();

		JFreeChart chart = ChartFactory.createScatterPlot(null,                     // chart title
				null,                     // domain axis label
				null,                     // range axis label
				dataSet,                  // data
				PlotOrientation.VERTICAL, // orientation
				false,                    // include legend
				true,                     // tooltips
				false                     // URLs
				);

		if (xAxis >= 0) {
			int size = dataSet.getSeriesCount();
			chart = ChartFactory.createScatterPlot(null,                     // chart title
					null,                     // domain axis label
					null,                     // range axis label
					dataSet,                  // data
					PlotOrientation.VERTICAL, // orientation
					true,                     // include legend
					true,                     // tooltips
					false                     // URLs
					);

			// renderer settings
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
			renderer.setBaseOutlinePaint(Color.BLACK);
			renderer.setUseOutlinePaint(true);
			renderer.setDrawOutlines(true);

			for (int i = 0; i < size; i++) {
				renderer.setSeriesShapesVisible(i, this.showPoints[plotIndexToColumnIndexMap.get(i)]);
				renderer.setSeriesLinesVisible(i, this.showLines[plotIndexToColumnIndexMap.get(i)]);
			}

			renderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 7, 7));

			// legend settings
			LegendTitle legend = chart.getLegend();
			if (legend != null) {
				legend.setPosition(RectangleEdge.TOP);
				legend.setFrame(BlockBorder.NONE);
				legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
				legend.setItemFont(LABEL_FONT);
			}
		}

		// GENERAL CHART SETTINGS

		int size = dataSet.getSeriesCount();
		if (size <= 1) {
			chart.getXYPlot().getRenderer().setSeriesPaint(0, getColorProvider().getPointColor(1.0d));
		} else {
			for (int i = 0; i < dataSet.getSeriesCount(); i++) {
				chart.getXYPlot().getRenderer()
						.setSeriesStroke(i, new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				chart.getXYPlot().getRenderer()
						.setSeriesPaint(i, getColorProvider().getPointColor(i / (double) (dataSet.getSeriesCount() - 1)));
			}
		}

		// set the background colors for the chart...
		chart.setBackgroundPaint(Color.WHITE);
		chart.getPlot().setBackgroundPaint(Color.WHITE);
		chart.setAntiAlias(false);

		// general plot settings
		XYPlot plot = chart.getXYPlot();
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

		// domain axis
		if (xAxis >= 0) {
			if (dataTable.isNominal(xAxis)) {
				String[] values = new String[dataTable.getNumberOfValues(xAxis)];
				for (int i = 0; i < values.length; i++) {
					values[i] = dataTable.mapIndex(xAxis, i);
				}
				plot.setDomainAxis(new SymbolAxis(dataTable.getColumnName(xAxis), values));
			} else if ((dataTable.isDate(xAxis)) || (dataTable.isDateTime(xAxis))) {
				DateAxis domainAxis = new DateAxis(dataTable.getColumnName(xAxis));
				domainAxis.setTimeZone(Tools.getPreferredTimeZone());
				plot.setDomainAxis(domainAxis);
			} else {
				if (xLogScale) {
					LogAxis domainAxis = new LogAxis(dataTable.getColumnName(xAxis));
					domainAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits(Locale.US));
					plot.setDomainAxis(domainAxis);
				} else {
					NumberAxis domainAxis = new NumberAxis(dataTable.getColumnName(xAxis));
					domainAxis.setAutoRangeStickyZero(false);
					domainAxis.setAutoRangeIncludesZero(false);
					plot.setDomainAxis(domainAxis);
				}
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
		plot.getRangeAxis().setLabelFont(LABEL_FONT_BOLD);
		plot.getRangeAxis().setTickLabelFont(LABEL_FONT);

		// Chart Panel Settings
		if (panel instanceof AbstractChartPanel) {
			panel.setChart(chart);
		} else {
			panel = new AbstractChartPanel(chart, getWidth(), getHeight() - MARGIN);

			final ChartPanelShiftController controller = new ChartPanelShiftController(panel);
			panel.addMouseListener(controller);
			panel.addMouseMotionListener(controller);

			// react to mouse clicks
			// ATTENTION: ACTIVATING THIS WILL LEAD TO SEVERE MEMORY LEAKS!!! (see below)
			panel.addChartMouseListener(new ChartMouseListener() {

				@Override
				public void chartMouseClicked(ChartMouseEvent e) {
					if (e.getTrigger().getClickCount() > 1) {
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

				@Override
				public void chartMouseMoved(ChartMouseEvent e) {}
			});
		}

		// tooltips
		class CustomXYToolTipGenerator implements XYToolTipGenerator {

			public CustomXYToolTipGenerator() {}

			@Override
			public String generateToolTip(XYDataset dataset, int row, int column) {
				String id = idMap.get(new SeriesAndItem(row, column));
				if (id != null) {
					return "<html><b>Id: " + id + "</b> (" + dataset.getSeriesKey(row) + ", "
							+ Tools.formatIntegerIfPossible(dataset.getXValue(row, column)) + ", "
							+ Tools.formatIntegerIfPossible(dataset.getYValue(row, column)) + ")</html>";
				} else {
					return "<html>(" + dataset.getSeriesKey(row) + ", "
							+ Tools.formatIntegerIfPossible(dataset.getXValue(row, column)) + ", "
							+ Tools.formatIntegerIfPossible(dataset.getYValue(row, column)) + ")</html>";
				}
			}
		}

		for (int i = 0; i < dataSet.getSeriesCount(); i++) {
			plot.getRenderer().setSeriesToolTipGenerator(i, new CustomXYToolTipGenerator());
		}
	}

	@Override
	public List<ParameterType> getAdditionalParameterKeys(InputPort inputPort) {
		List<ParameterType> types = super.getAdditionalParameterKeys(inputPort);
		types.add(new ParameterTypeList(PARAMETER_POINTS_AND_LINES, "Describes if points and lines are shown.",
				new ParameterTypeBoolean(PARAMETER_POINTS, "If checked, points will be shown.", true),
				new ParameterTypeBoolean(PARAMETER_LINES, "If checked, lines will be shown.", false)));
		return types;
	}

	@Override
	public void setAdditionalParameter(String key, String value) {
		super.setAdditionalParameter(key, value);
		if (key.equals(PARAMETER_POINTS_AND_LINES)) {
			List<String[]> valuePairs = ParameterTypeList.transformString2List(value);
			Iterator<String[]> iterator = valuePairs.iterator();
			for (int i = 0; iterator.hasNext() && i < showPoints.length; i++) {
				String[] pair = iterator.next();
				showPoints[i] = "true".equals(pair[0]);
				showLines[i] = "true".equals(pair[1]);
			}
			updatePlotter();
		}
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		switch (index) {
			case 0:
				return getRotateLabelComponent();
			case 1:
				return pointsAndLinesOptions;
		}
		return null;
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.MULTIPLE_SELECTION_SCATTER_PLOT;
	}
}
