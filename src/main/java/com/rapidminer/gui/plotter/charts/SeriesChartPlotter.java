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
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.RangeablePlotterAdapter;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.MathFunctions;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;


/**
 * This is the series chart plotter.
 * 
 * @author Ingo Mierswa, Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public class SeriesChartPlotter extends RangeablePlotterAdapter {

	private static final long serialVersionUID = -8763693366081949249L;

	private static final String PARAMETER_MARKER = "marker_at";

	private static final String VALUEAXIS_LABEL = "value";
	private static final String SERIESINDEX_LABEL = "index";

	private JTextField limitField = new JTextField();

	/** The currently used data table object. */
	private transient DataTable dataTable;

	/** The data set used for the plotter. */
	private YIntervalSeriesCollection dataset = null;

	/** The column which is used for the values. */
	private boolean[] columns;

	/** The axis values for the upper and lower bounds. */
	private int[] axis = new int[] { -1, -1, -1 };

	private boolean useLimit = false;
	private double limit = 0;

	private static final int MIN = 0;
	private static final int MAX = 1;
	private static final int INDEX = 2;

	/** Indicates if bounds are plotted. */
	private boolean plotBounds = false;
	private int boundsSeriesIndex = 1;

	private List<Integer> plotIndexToColumnIndexMap = new ArrayList<Integer>();

	public SeriesChartPlotter(final PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);

		limitField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					double value = Double.parseDouble(limitField.getText());
					settings.setParameterAsDouble(PARAMETER_MARKER, value);
				} catch (NumberFormatException ex) {
					useLimit = false;
				}
			}
		});
	}

	public SeriesChartPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	private JFreeChart createChart(XYDataset dataset, boolean createLegend) {

		// create the chart...
		JFreeChart chart = ChartFactory.createXYLineChart(null, // chart title
				null, // x axis label
				null, // y axis label
				dataset, // data
				PlotOrientation.VERTICAL, createLegend, // include legend
				true, // tooltips
				false // urls
				);

		chart.setBackgroundPaint(Color.white);

		// get a reference to the plot for further customization...
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
		DeviationRenderer renderer = new DeviationRenderer(true, false);

		// colors
		if (dataset.getSeriesCount() == 1) {
			renderer.setSeriesStroke(0, new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			renderer.setSeriesPaint(0, getColorProvider().getPointColor(1.0d));
		} else {  // special case needed for avoiding devision by zero
			for (int i = 0; i < dataset.getSeriesCount(); i++) {
				renderer.setSeriesStroke(i, new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				renderer.setSeriesPaint(i,
						getColorProvider().getPointColor(1.0d - i / (double) (dataset.getSeriesCount() - 1)));
			}
		}
		// background for bounds
		if (plotBounds) {
			float[] dashArray = new float[] { 7, 14 };
			renderer.setSeriesStroke(boundsSeriesIndex, new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
					1.0f, dashArray, 0));
			renderer.setSeriesPaint(boundsSeriesIndex, Color.GRAY.brighter());
			renderer.setSeriesFillPaint(boundsSeriesIndex, Color.GRAY);
		}
		// alpha
		renderer.setAlpha(0.25f);

		plot.setRenderer(renderer);

		NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
		if (axis[INDEX] < 0) {
			xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits(Locale.US));
			xAxis.setLabel(SERIESINDEX_LABEL);
			Range range = getRangeForName(SERIESINDEX_LABEL);
			if (range == null) {
				xAxis.setAutoRange(true);
				xAxis.setAutoRangeStickyZero(false);
				xAxis.setAutoRangeIncludesZero(false);
			} else {
				xAxis.setRange(range, true, false);
			}
		} else {
			xAxis.setLabel(dataTable.getColumnName(axis[INDEX]));
			Range range = getRangeForDimension(axis[INDEX]);
			if (range == null) {
				xAxis.setAutoRange(true);
				xAxis.setAutoRangeStickyZero(false);
				xAxis.setAutoRangeIncludesZero(false);
			} else {
				xAxis.setRange(range, true, false);
			}
		}

		xAxis.setLabelFont(LABEL_FONT_BOLD);
		xAxis.setTickLabelFont(LABEL_FONT);
		xAxis.setVerticalTickLabels(isLabelRotating());

		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setLabel(VALUEAXIS_LABEL);
		yAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits(Locale.US));
		setYAxisRange(yAxis);

		yAxis.setLabelFont(LABEL_FONT_BOLD);
		yAxis.setTickLabelFont(LABEL_FONT);

		return chart;
	}

	/** Returns a line icon depending on the index. */
	@Override
	public Icon getIcon(int index) {
		return null;
	}

	@Override
	public void dataTableSet() {
		this.dataTable = getDataTable();
		columns = new boolean[dataTable.getNumberOfColumns()];
		updatePlotter();
	}

	@Override
	public int getValuePlotSelectionType() {
		return MULTIPLE_SELECTION;
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		if (index >= 0 && index < columns.length) {
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
		return axis.length;
	}

	@Override
	public String getAxisName(int index) {
		switch (index) {
			case MIN:
				return "Lower Bound";
			case MAX:
				return "Upper Bound";
			case INDEX:
				return "Index Dimension";
			default:
				return "none";
		}
	}

	@Override
	public List<ParameterType> getAdditionalParameterKeys(InputPort inputPort) {
		List<ParameterType> types = super.getAdditionalParameterKeys(inputPort);
		types.add(new ParameterTypeDouble(PARAMETER_MARKER, "Defines a horizontal line as a reference to the plot.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true));
		return types;
	}

	@Override
	public void setAdditionalParameter(String key, String value) {
		super.setAdditionalParameter(key, value);
		if (PARAMETER_MARKER.equals(key)) {
			try {
				double newLimit = Double.parseDouble(value);
				useLimit = true;
				if (limit != newLimit) {
					limitField.setText(limit + "");
					limit = newLimit;
					updatePlotter();
				}
			} catch (NumberFormatException e) {
				if (useLimit) {
					useLimit = false;
					updatePlotter();
				}
			}
		}
	}

	@Override
	public int getAxis(int index) {
		return axis[index];
	}

	@Override
	public void setAxis(int index, int dimension) {
		if (axis[index] != dimension) {
			axis[index] = dimension;
			updatePlotter();
		}
	}

	private int prepareData() {
		synchronized (dataTable) {
			this.dataset = new YIntervalSeriesCollection();
			this.plotBounds = false;
			this.plotIndexToColumnIndexMap.clear();

			// series
			int columnCount = 0;
			for (int c = 0; c < dataTable.getNumberOfColumns(); c++) {
				if (getPlotColumn(c)) {
					if (dataTable.isNumerical(c)) {
						YIntervalSeries series = new YIntervalSeries(this.dataTable.getColumnName(c));
						Iterator<DataTableRow> i = dataTable.iterator();
						int index = 0;
						while (i.hasNext()) {
							DataTableRow row = i.next();
							double value = row.getValue(c);
							if (axis[INDEX] >= 0 && !dataTable.isNominal(axis[INDEX])) {
								double indexValue = row.getValue(axis[INDEX]);
								series.add(indexValue, value, value, value);
							} else {
								series.add(index++, value, value, value);
							}
						}
						dataset.addSeries(series);
						plotIndexToColumnIndexMap.add(c);
						columnCount++;
					}
				}
			}

			// Lower and upper bound
			if (getAxis(MIN) > -1 && getAxis(MAX) > -1) {
				if (dataTable.isNumerical(getAxis(MIN)) && dataTable.isNumerical(getAxis(MAX))) {
					YIntervalSeries series = new YIntervalSeries("Bounds");
					Iterator<DataTableRow> i = dataTable.iterator();
					int index = 0;
					while (i.hasNext()) {
						DataTableRow row = i.next();
						double lowerValue = row.getValue(getAxis(0));
						double upperValue = row.getValue(getAxis(1));
						if (lowerValue > upperValue) {
							double dummy = lowerValue;
							lowerValue = upperValue;
							upperValue = dummy;
						}
						double mean = (upperValue - lowerValue) / 2.0d + lowerValue;
						if (axis[INDEX] >= 0 && !dataTable.isNominal(axis[INDEX])) {
							double indexValue = row.getValue(axis[INDEX]);
							series.add(indexValue, mean, lowerValue, upperValue);
						} else {
							series.add(index++, mean, lowerValue, upperValue);
						}
					}
					dataset.addSeries(series);
					this.plotBounds = true;
					this.boundsSeriesIndex = dataset.getSeriesCount() - 1;
				}
			}

			// limit
			if (useLimit) {
				YIntervalSeries series = new YIntervalSeries("Limit");
				int index = 0;
				for (DataTableRow row : dataTable) {
					if (!dataTable.isNominal(axis[INDEX])) {
						double indexValue = row.getValue(axis[INDEX]);
						series.add(indexValue, limit, limit, limit);
					} else {
						series.add(index++, limit, limit, limit);
					}
				}
				dataset.addSeries(series);
			}

			return columnCount;
		}
	}

	private void setYAxisRange(NumberAxis axis) {
		Range range = getRangeForName(VALUEAXIS_LABEL);
		if (range == null) {
			for (int c = 0; c < this.dataTable.getNumberOfColumns(); c++) {
				if (this.columns[c] || c == getAxis(0) || c == getAxis(1)) {
					if (range == null) {
						range = getRangeForDimension(c);
					} else {
						Range newRange = getRangeForDimension(c);
						if (newRange != null) {
							range = new Range(MathFunctions.robustMin(range.getLowerBound(), newRange.getLowerBound()),
									MathFunctions.robustMax(range.getUpperBound(), newRange.getUpperBound()));
						}
					}
				}
			}
		}
		if (range != null) {
			axis.setRange(range);
		} else {
			axis.setAutoRange(true);
			axis.setAutoRangeStickyZero(false);
			axis.setAutoRangeIncludesZero(false);
		}
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		if (index == 0) {
			return limitField;
		} else if (index == 1) {
			return getRotateLabelComponent();
		}
		return null;
	}

	@Override
	protected void updatePlotter() {
		int categoryCount = prepareData();
		String maxClassesProperty = ParameterService
				.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_COLORS_CLASSLIMIT);
		int maxClasses = 20;
		try {
			if (maxClassesProperty != null) {
				maxClasses = Integer.parseInt(maxClassesProperty);
			}
		} catch (NumberFormatException e) {
			// LogService.getGlobal().log("Series plotter: cannot parse property 'rapidminer.gui.plotter.colors.classlimit', using maximal 20 different classes.",
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.plotter.charts.SeriesChartPlotter.parsing_property_error");
		}
		boolean createLegend = categoryCount > 0 && categoryCount < maxClasses;

		JFreeChart chart = createChart(this.dataset, createLegend);

		// set the background color for the chart...
		chart.setBackgroundPaint(Color.white);

		// domain axis
		if (axis[INDEX] >= 0) {
			if (!dataTable.isNominal(axis[INDEX])) {
				if (dataTable.isDate(axis[INDEX]) || dataTable.isDateTime(axis[INDEX])) {
					DateAxis domainAxis = new DateAxis(dataTable.getColumnName(axis[INDEX]));
					domainAxis.setTimeZone(Tools.getPreferredTimeZone());
					chart.getXYPlot().setDomainAxis(domainAxis);
					if (getRangeForDimension(axis[INDEX]) != null) {
						domainAxis.setRange(getRangeForDimension(axis[INDEX]));
					}
					domainAxis.setLabelFont(LABEL_FONT_BOLD);
					domainAxis.setTickLabelFont(LABEL_FONT);
					domainAxis.setVerticalTickLabels(isLabelRotating());
				}
			} else {
				LinkedHashSet<String> values = new LinkedHashSet<String>();
				for (DataTableRow row : dataTable) {
					String stringValue = dataTable.mapIndex(axis[INDEX], (int) row.getValue(axis[INDEX]));
					if (stringValue.length() > 40) {
						stringValue = stringValue.substring(0, 40);
					}
					values.add(stringValue);
				}
				ValueAxis categoryAxis = new SymbolAxis(dataTable.getColumnName(axis[INDEX]),
						values.toArray(new String[values.size()]));
				categoryAxis.setLabelFont(LABEL_FONT_BOLD);
				categoryAxis.setTickLabelFont(LABEL_FONT);
				categoryAxis.setVerticalTickLabels(isLabelRotating());
				chart.getXYPlot().setDomainAxis(categoryAxis);
			}
		}

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

		// ATTENTION: WITHOUT THIS WE GET SEVERE MEMORY LEAKS!!!
		panel.getChartRenderingInfo().setEntityCollection(null);
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.SERIES_PLOT;
	}

	@Override
	public Collection<String> resolveXAxis(int axisIndex) {
		if (axis[INDEX] != -1) {
			return Collections.singletonList(dataTable.getColumnName(axis[INDEX]));
		} else {
			return Collections.singletonList(SERIESINDEX_LABEL);
		}
	}

	@Override
	public Collection<String> resolveYAxis(int axisIndex) {
		Collection<String> names = new LinkedList<String>();
		for (int i = 0; i < columns.length; i++) {
			if (columns[i]) {
				names.add(dataTable.getColumnName(i));
			}
		}
		return names;
	}
}
