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
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.RangeablePlotterAdapter;
import com.rapidminer.tools.Tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

import javax.swing.Icon;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;


/**
 * This is the multiple series chart plotter.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class MultipleSeriesChartPlotter extends RangeablePlotterAdapter {

	private static final long serialVersionUID = -8763693366081949249L;

	private static final String SERIESINDEX_LABEL = "index";

	/** The currently used data table object. */
	private transient DataTable dataTable;

	private int indexAxis = -1;

	/** The column which is used for the values. */
	private boolean[] columns;

	public MultipleSeriesChartPlotter(PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
	}

	public MultipleSeriesChartPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	private JFreeChart createChart() {

		// create the chart...
		JFreeChart chart = ChartFactory.createXYLineChart(null,                      // chart title
				null,                      // x axis label
				null,                      // y axis label
				null,                      // data
				PlotOrientation.VERTICAL, false,                     // include legend
				true,                      // tooltips
				false                      // urls
				);

		chart.setBackgroundPaint(Color.white);

		// get a reference to the plot for further customization...
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

		// domain axis

		if ((indexAxis >= 0) && (!dataTable.isNominal(indexAxis))) {
			if ((dataTable.isDate(indexAxis)) || (dataTable.isDateTime(indexAxis))) {
				DateAxis domainAxis = new DateAxis(dataTable.getColumnName(indexAxis));
				domainAxis.setTimeZone(Tools.getPreferredTimeZone());
				chart.getXYPlot().setDomainAxis(domainAxis);
			}
		} else {
			plot.getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits(Locale.US));
			((NumberAxis) plot.getDomainAxis()).setAutoRangeStickyZero(false);
			((NumberAxis) plot.getDomainAxis()).setAutoRangeIncludesZero(false);
		}
		ValueAxis xAxis = plot.getDomainAxis();
		if (indexAxis > -1) {
			xAxis.setLabel(getDataTable().getColumnName(indexAxis));
		} else {
			xAxis.setLabel(SERIESINDEX_LABEL);
		}
		xAxis.setAutoRange(true);
		xAxis.setLabelFont(LABEL_FONT_BOLD);
		xAxis.setTickLabelFont(LABEL_FONT);
		xAxis.setVerticalTickLabels(isLabelRotating());
		if (indexAxis > 0) {
			if (getRangeForDimension(indexAxis) != null) {
				xAxis.setRange(getRangeForDimension(indexAxis));
			}
		} else {
			if (getRangeForName(SERIESINDEX_LABEL) != null) {
				xAxis.setRange(getRangeForName(SERIESINDEX_LABEL));
			}
		}

		// renderer and range axis
		synchronized (dataTable) {
			int numberOfSelectedColumns = 0;
			for (int c = 0; c < dataTable.getNumberOfColumns(); c++) {
				if (getPlotColumn(c)) {
					if (dataTable.isNumerical(c)) {
						numberOfSelectedColumns++;
					}
				}
			}

			int columnCount = 0;
			for (int c = 0; c < dataTable.getNumberOfColumns(); c++) {
				if (getPlotColumn(c)) {
					if (dataTable.isNumerical(c)) {
						// YIntervalSeries series = new
						// YIntervalSeries(this.dataTable.getColumnName(c));
						XYSeriesCollection dataset = new XYSeriesCollection();
						XYSeries series = new XYSeries(dataTable.getColumnName(c));
						Iterator<DataTableRow> i = dataTable.iterator();
						int index = 1;
						while (i.hasNext()) {
							DataTableRow row = i.next();
							double value = row.getValue(c);

							if ((indexAxis >= 0) && (!dataTable.isNominal(indexAxis))) {
								double indexValue = row.getValue(indexAxis);
								series.add(indexValue, value);
							} else {
								series.add(index++, value);
							}
						}
						dataset.addSeries(series);

						XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

						Color color = getColorProvider().getPointColor(1.0d);
						if (numberOfSelectedColumns > 1) {
							color = getColorProvider().getPointColor(columnCount / (double) (numberOfSelectedColumns - 1));
						}
						renderer.setSeriesPaint(0, color);
						renderer.setSeriesStroke(0, new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
						renderer.setSeriesShapesVisible(0, false);

						NumberAxis yAxis = new NumberAxis(dataTable.getColumnName(c));
						if (getRangeForDimension(c) != null) {
							yAxis.setRange(getRangeForDimension(c));
						} else {
							yAxis.setAutoRange(true);
							yAxis.setAutoRangeStickyZero(false);
							yAxis.setAutoRangeIncludesZero(false);
						}
						yAxis.setLabelFont(LABEL_FONT_BOLD);
						yAxis.setTickLabelFont(LABEL_FONT);
						if (numberOfSelectedColumns > 1) {
							yAxis.setAxisLinePaint(color);
							yAxis.setTickMarkPaint(color);
							yAxis.setLabelPaint(color);
							yAxis.setTickLabelPaint(color);
						}

						plot.setRangeAxis(columnCount, yAxis);
						plot.setRangeAxisLocation(columnCount, AxisLocation.TOP_OR_LEFT);

						plot.setDataset(columnCount, dataset);
						plot.setRenderer(columnCount, renderer);
						plot.mapDatasetToRangeAxis(columnCount, columnCount);

						columnCount++;
					}
				}
			}
		}

		chart.setBackgroundPaint(Color.white);

		return chart;
	}

	/** Returns a line icon depending on the index. */
	@Override
	public Icon getIcon(int index) {
		return null;
	}

	@Override
	public int getValuePlotSelectionType() {
		return MULTIPLE_SELECTION;
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		if ((index >= 0) && (index < columns.length)) {
			this.columns[index] = plot;
		}
		updatePlotter();
	}

	@Override
	public boolean getPlotColumn(int index) {
		return this.columns[index];
	}

	@Override
	public String getPlotName() {
		return "Plot Series";
	}

	@Override
	public int getNumberOfAxes() {
		return 1;
	}

	@Override
	public String getAxisName(int index) {
		if (index == 0) {
			return "Index Dimension";
		} else {
			return "none";
		}
	}

	@Override
	public int getAxis(int index) {
		if (index == 0) {
			return indexAxis;
		} else {
			return -1;
		}
	}

	@Override
	public void setAxis(int index, int dimension) {
		if (index == 0) {
			indexAxis = dimension;
			updatePlotter();
		}
	}

	@Override
	protected void updatePlotter() {
		JFreeChart chart = createChart();

		AbstractChartPanel panel = getPlotterPanel();
		if (panel == null) {
			panel = createPanel(chart);
		}

		panel.setChart(chart);
		// ATTENTION: WITHOUT THIS WE GET SEVERE MEMORY LEAKS!!!
		panel.getChartRenderingInfo().setEntityCollection(null);
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.MULTIPLE_SERIES_PLOT;
	}

	@Override
	public void dataTableSet() {
		this.dataTable = getDataTable();
		columns = new boolean[dataTable.getNumberOfColumns()];

		updatePlotter();
	}

	@Override
	public Collection<String> resolveXAxis(int axisIndex) {
		if (indexAxis != -1) {
			return Collections.singletonList(dataTable.getColumnName(indexAxis));
		} else {
			return Collections.singletonList(SERIESINDEX_LABEL);
		}
	}

	@Override
	public Collection<String> resolveYAxis(int axisIndex) {
		Collection<String> names = new LinkedList<String>();
		int foundAxis = 0;
		for (int i = 0; i < columns.length; i++) {
			if (columns[i]) {
				if (axisIndex == foundAxis) {
					names.add(dataTable.getColumnName(i));
					break;
				}
				foundAxis++;
			}
		}
		return names;
	}
}
