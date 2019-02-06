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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;


/**
 * This is the histogram color plotter based on JFreeCharts.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class HistogramColorChart extends HistogramChart {

	private static final long serialVersionUID = 9140046811324105445L;

	public static final int MIN_BIN_NUMBER = 2;

	public static final int MAX_BIN_NUMBER = 100;

	public static final int DEFAULT_BIN_NUMBER = 40;

	private HistogramDataset histogramDataset;

	private DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();

	private boolean nominal = false;

	private boolean datetime = false;

	/** Indicates which column will be plotted. */
	private int valueColumn = -1;

	/** Indicates which column should be used for the color definition. */
	private int colorColumn = -1;

	private boolean absolute = false;

	public HistogramColorChart(PlotterConfigurationModel settings) {
		super(settings);

	}

	public HistogramColorChart(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	@Override
	public int getNumberOfAxes() {
		return 1;
	}

	@Override
	public int getAxis(int axis) {
		return valueColumn;
	}

	@Override
	public String getAxisName(int index) {
		if (index == 0) {
			return "Histogram";
		} else {
			return "Unknown";
		}
	}

	@Override
	public boolean isLogScale() {
		return this.logScale;
	}

	@Override
	public void setAbsolute(boolean absolute) {
		this.absolute = absolute;
		updatePlotter();
	}

	@Override
	public boolean isSupportingAbsoluteValues() {
		return true;
	}

	@Override
	public void setAxis(int index, int dimension) {
		if (this.valueColumn != dimension) {
			this.valueColumn = dimension;
			updatePlotter();
		}
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		colorColumn = index;
		updatePlotter();
	}

	@Override
	public String getPlotName() {
		return "Color";
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.HISTOGRAM_PLOT_COLOR;
	}

	@Override
	public boolean getPlotColumn(int index) {
		return index == colorColumn;
	}

	@Override
	public int getValuePlotSelectionType() {
		return SINGLE_SELECTION;
	}

	@Override
	public void prepareData() {
		histogramDataset = new RapidHistogramDataset(isLogScale());
		categoryDataset.clear();

		if (colorColumn < 0 || valueColumn < 0) {
			return;
		}

		if (dataTable.isNominal(colorColumn)) {
			if (dataTable.isNominal(valueColumn)) {
				this.nominal = true;
				synchronized (dataTable) {
					Map<String, Map<String, AtomicInteger>> categoryValues = new LinkedHashMap<>();
					for (int i = 0; i < this.dataTable.getNumberOfValues(colorColumn); i++) {
						String key = this.dataTable.mapIndex(colorColumn, i);
						Map<String, AtomicInteger> innerMap = new LinkedHashMap<>();
						for (int j = 0; j < this.dataTable.getNumberOfValues(valueColumn); j++) {
							innerMap.put(this.dataTable.mapIndex(valueColumn, j), new AtomicInteger(0));
						}
						categoryValues.put(key, innerMap);
					}

					Iterator<DataTableRow> j = dataTable.iterator();
					while (j.hasNext()) {
						DataTableRow row = j.next();
						String colorString = this.dataTable.getValueAsString(row, colorColumn);
						String valueString = this.dataTable.getValueAsString(row, valueColumn);
						categoryValues.get(colorString).get(valueString).incrementAndGet();
					}

					for (String key : categoryValues.keySet()) {
						for (String value : categoryValues.get(key).keySet()) {
							int count = categoryValues.get(key).get(value).get();
							categoryDataset.addValue(count, key, value);
						}
					}
				}
			} else {
				this.nominal = false;
				synchronized (dataTable) {
					Map<String, List<Double>> classMap = new LinkedHashMap<>();
					for (int i = 0; i < this.dataTable.getNumberOfValues(colorColumn); i++) {
						classMap.put(this.dataTable.mapIndex(colorColumn, i), new LinkedList<Double>());
					}
					Iterator<DataTableRow> i = dataTable.iterator();
					while (i.hasNext()) {
						DataTableRow row = i.next();
						double value = row.getValue(valueColumn);
						if (this.absolute) {
							value = Math.abs(value);
						}
						String colorValue = this.dataTable.getValueAsString(row, colorColumn);
						List<Double> colorList = classMap.get(colorValue);
						if (colorList != null) {
							colorList.add(value);
						}
					}

					for (Entry<String, List<Double>> entry : classMap.entrySet()) {
						List<Double> valueList = entry.getValue();
						double[] values = new double[valueList.size()];
						int index = 0;
						for (double v : valueList) {
							values[index++] = v;
						}
						histogramDataset.addSeries(entry.getKey(), values, this.binNumber);
					}
				}

				if (dataTable.isDateTime(valueColumn)) {
					this.datetime = true;
				}
			}
		}
	}

	@Override
	protected void updatePlotter() {
		prepareData();

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
					"com.rapidminer.gui.plotter.charts.HistogramColorChart.parsing_property_error");
		}

		JFreeChart chart = null;
		if (nominal) {
			// ** nominal **
			int categoryCount = this.categoryDataset.getRowCount();
			boolean createLegend = categoryCount > 0 && categoryCount < maxClasses && this.drawLegend;

			String domainName = valueColumn >= 0 ? this.dataTable.getColumnName(valueColumn) : "Value";

			chart = ChartFactory.createBarChart(null,                      // title
					domainName, "Frequency", categoryDataset, PlotOrientation.VERTICAL, createLegend, true,                     // tooltips
					false);                   // urls

			CategoryPlot plot = chart.getCategoryPlot();
			plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
			plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
			plot.setBackgroundPaint(Color.WHITE);
			plot.setForegroundAlpha(this.opaqueness);

			BarRenderer renderer = new BarRenderer();
			if (categoryDataset.getRowCount() == 1) {
				renderer.setSeriesPaint(0, Color.RED);
				renderer.setSeriesFillPaint(0, Color.RED);
			} else {
				for (int i = 0; i < categoryDataset.getRowCount(); i++) {
					Color color = getColorProvider(true).getPointColor(
							(double) i / (double) (categoryDataset.getRowCount() - 1));
					renderer.setSeriesPaint(i, color);
					renderer.setSeriesFillPaint(i, color);
				}
			}
			renderer.setBarPainter(new RapidBarPainter());
			renderer.setDrawBarOutline(true);
			plot.setRenderer(renderer);

			plot.getRangeAxis().setLabelFont(LABEL_FONT_BOLD);
			plot.getRangeAxis().setTickLabelFont(LABEL_FONT);

			plot.getDomainAxis().setLabelFont(LABEL_FONT_BOLD);
			plot.getDomainAxis().setTickLabelFont(LABEL_FONT);

			// rotate labels
			if (isLabelRotating()) {
				plot.getDomainAxis().setTickLabelsVisible(true);
				plot.getDomainAxis().setCategoryLabelPositions(
						CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 2.0d));
			}
		} else {
			// ** numerical **
			int categoryCount = this.histogramDataset.getSeriesCount();
			boolean createLegend = categoryCount > 0 && categoryCount < maxClasses && this.drawLegend;

			String domainName = valueColumn >= 0 ? this.dataTable.getColumnName(valueColumn) : "Value";
			chart = ChartFactory.createHistogram(null,                      // title
					domainName, "Frequency", histogramDataset, PlotOrientation.VERTICAL, createLegend, true,                     // tooltips
					false);                   // urls

			XYPlot plot = chart.getXYPlot();
			plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
			plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
			plot.setBackgroundPaint(Color.WHITE);
			plot.setForegroundAlpha(this.opaqueness);

			XYBarRenderer renderer = new XYBarRenderer();
			if (histogramDataset.getSeriesCount() == 1) {
				renderer.setSeriesPaint(0, Color.RED);
				renderer.setSeriesFillPaint(0, Color.RED);
			} else {
				for (int i = 0; i < histogramDataset.getSeriesCount(); i++) {
					Color color = getColorProvider(true).getPointColor(
							(double) i / (double) (histogramDataset.getSeriesCount() - 1));
					renderer.setSeriesPaint(i, color);
					renderer.setSeriesFillPaint(i, color);
				}
			}
			renderer.setBarPainter(new RapidXYBarPainter());
			renderer.setDrawBarOutline(true);
			plot.setRenderer(renderer);

			plot.getRangeAxis().setLabelFont(LABEL_FONT_BOLD);
			plot.getRangeAxis().setTickLabelFont(LABEL_FONT);

			plot.getDomainAxis().setLabelFont(LABEL_FONT_BOLD);
			plot.getDomainAxis().setTickLabelFont(LABEL_FONT);

			// Correctly displays dates on x-axis
			if (datetime) {
				DateAxis dateAxis = new DateAxis();
				dateAxis.setDateFormatOverride(Tools.DATE_TIME_FORMAT.get());
				plot.setDomainAxis(dateAxis);
			}

			// range axis
			Range range = getRangeForDimension(valueColumn);
			if (range != null) {
				plot.getDomainAxis().setRange(range);
			}

			// rotate labels
			if (isLabelRotating()) {
				plot.getDomainAxis().setTickLabelsVisible(true);
				plot.getDomainAxis().setVerticalTickLabels(true);
			}

			if (histogramDataset.getSeriesCount() == 1) {
				String key = histogramDataset.getSeriesKey(0).toString();
				int index = this.dataTable.getColumnIndex(key);
				if (index >= 0) {
					if (this.dataTable.isNominal(index)) {
						String[] values = new String[dataTable.getNumberOfValues(index)];
						for (int i = 0; i < values.length; i++) {
							values[i] = dataTable.mapIndex(index, i);
						}
						plot.setDomainAxis(new SymbolAxis(key, values));

						// rotate labels
						if (isLabelRotating()) {
							plot.getDomainAxis().setTickLabelsVisible(true);
							plot.getDomainAxis().setVerticalTickLabels(true);
						}
					}
				}
			}
		}

		// set the background color for the chart...
		chart.setBackgroundPaint(Color.white);

		// legend settings
		LegendTitle legend = chart.getLegend();
		if (legend != null) {
			legend.setPosition(RectangleEdge.TOP);
			legend.setFrame(BlockBorder.NONE);
			legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
			legend.setItemFont(LABEL_FONT);
		}

		AbstractChartPanel panel = getPlotterPanel();
		if (panel == null) {
			panel = createPanel(chart);
		} else {
			panel.setChart(chart);
		}

		// Disable zooming for Histogram-Charts
		panel.setRangeZoomable(false);
		panel.setDomainZoomable(false);

		// ATTENTION: WITHOUT THIS WE GET SEVERE MEMORY LEAKS!!!
		panel.getChartRenderingInfo().setEntityCollection(null);
	}
}
