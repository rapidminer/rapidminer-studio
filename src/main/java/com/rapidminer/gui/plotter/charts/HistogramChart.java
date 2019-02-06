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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.plotter.ColorProvider;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.PlotterConfigurationModel.PlotterSettingsChangedListener;
import com.rapidminer.gui.plotter.RangeablePlotterAdapter;
import com.rapidminer.gui.plotter.settings.ListeningJCheckBox;
import com.rapidminer.gui.plotter.settings.ListeningJSlider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.MathFunctions;


/**
 * This is the histogram plotter based on JFreeCharts.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class HistogramChart extends RangeablePlotterAdapter {

	public static final String PARAMETER_OPAQUENESS = "opaqueness";

	public static final String PARAMETER_NUMBER_OF_BINS = "number_of_bins";

	public static final String PARAMETER_LOG_SCALE = "log_scale";

	private static final long serialVersionUID = 9140046811324105445L;

	public static final int MIN_BIN_NUMBER = 1;

	public static final int MAX_BIN_NUMBER = 100;

	public static final int DEFAULT_BIN_NUMBER = 40;

	protected transient DataTable dataTable;

	private HistogramDataset histogramDataset;

	/** Indicates which columns will be plotted. */
	private boolean[] columns = new boolean[0];

	protected int binNumber = DEFAULT_BIN_NUMBER;

	protected boolean logScale = false;

	private boolean absolute = false;

	protected boolean drawLegend = true;

	protected float opaqueness = 1.0f;

	private ListeningJCheckBox logScaleBox;
	private ListeningJSlider binNumberSlider;
	private ListeningJSlider opaquenessSlider;

	public HistogramChart(final PlotterConfigurationModel settings) {
		super(settings);

		logScaleBox = new ListeningJCheckBox(PARAMETER_LOG_SCALE, "Log Scale", false);
		logScaleBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				settings.setParameterAsBoolean(PARAMETER_LOG_SCALE, logScaleBox.isSelected());
			}
		});

		binNumberSlider = new ListeningJSlider(PARAMETER_NUMBER_OF_BINS, MIN_BIN_NUMBER, MAX_BIN_NUMBER, DEFAULT_BIN_NUMBER);
		binNumberSlider.setMajorTickSpacing(MAX_BIN_NUMBER - MIN_BIN_NUMBER);
		binNumberSlider.setMinorTickSpacing((MAX_BIN_NUMBER - MIN_BIN_NUMBER) / 10);
		binNumberSlider.setPaintTicks(true);
		binNumberSlider.setPaintLabels(true);
		binNumberSlider.setToolTipText("Set the number of bins which should be displayed.");
		binNumberSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (!binNumberSlider.getValueIsAdjusting()) {
					settings.setParameterAsInt(PARAMETER_NUMBER_OF_BINS, binNumberSlider.getValue());
				}
			}
		});

		opaquenessSlider = new ListeningJSlider(PARAMETER_OPAQUENESS, 0, 100, (int) (this.opaqueness * 100));
		opaquenessSlider.setPaintTicks(true);
		opaquenessSlider.setPaintLabels(true);
		opaquenessSlider.setToolTipText("Set the amount of opaqueness / transparency.");
		opaquenessSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (!opaquenessSlider.getValueIsAdjusting()) {
					settings.setParameterAsInt(PARAMETER_OPAQUENESS, opaquenessSlider.getValue());
				}
			}
		});

	}

	public HistogramChart(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	@Override
	public void dataTableSet() {
		this.dataTable = getDataTable();
		this.columns = new boolean[this.dataTable.getNumberOfColumns()];

		updatePlotter();
	}

	@Override
	public Icon getIcon(int index) {
		return null;
	}

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

	public void setDrawLegend(boolean drawLegend) {
		this.drawLegend = drawLegend;
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		columns[index] = plot;
		updatePlotter();
		super.repaint();
		revalidate();
	}

	@Override
	public boolean getPlotColumn(int index) {
		return columns[index];
	}

	@Override
	public int getValuePlotSelectionType() {
		return MULTIPLE_SELECTION;
	}

	protected int getNumberOfPlots() {
		int counter = 0;
		for (boolean column : columns) {
			if (column) {
				counter++;
			}
		}
		return counter;
	}

	public void prepareData() {
		histogramDataset = new RapidHistogramDataset(isLogScale());

		if (getNumberOfPlots() == 0) {
			return;
		}

		synchronized (dataTable) {
			for (int c = 0; c < this.dataTable.getNumberOfColumns(); c++) {
				if (this.columns[c]) {
					// double[] values = new double[this.dataTable.getNumberOfRows()];
					// if (this.dataTable.getSelectionCount() > 0) {
					double[] values = new double[this.dataTable.getSelectionCount() > 0 ? this.dataTable.getSelectionCount()
							: this.dataTable.getNumberOfRows()];
					int valueIndex = 0;
					Iterator<DataTableRow> i = dataTable.iterator();
					while (i.hasNext()) {
						DataTableRow row = i.next();
						if (this.dataTable.getSelectionCount() == 0 || !dataTable.isDeselected(row.getId())) {
							// if (this.dataTable.getSelectionCount() == 0 ||
							// !dataTable.isDeselected(index)) {
							double value = row.getValue(c);
							if (this.absolute) {
								value = Math.abs(value);
							}
							if (!Double.isNaN(value)) {
								values[valueIndex++] = value;
							}
						}
					}
					if (valueIndex != values.length) {
						double[] newValues = new double[valueIndex];
						for (int j = 0; j < valueIndex; j++) {
							newValues[j] = values[j];
						}
						values = newValues;
					}
					if (values.length > 0) {
						histogramDataset.addSeries(this.dataTable.getColumnName(c), values, this.binNumber);
						// }
					}
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
					"com.rapidminer.gui.plotter.charts.HistogramChart.parsing_property_error");
		}
		int categoryCount = this.histogramDataset.getSeriesCount();
		boolean createLegend = categoryCount > 0 && categoryCount < maxClasses && this.drawLegend;

		JFreeChart chart = ChartFactory.createHistogram(null, // title
				"Value", "Frequency", histogramDataset, PlotOrientation.VERTICAL, createLegend, true, // tooltips
				false); // urls

		XYPlot plot = chart.getXYPlot();
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
		plot.setBackgroundPaint(Color.WHITE);
		plot.setForegroundAlpha(this.opaqueness);

		XYBarRenderer renderer = new XYBarRenderer();
		if (histogramDataset.getSeriesCount() == 1) {
			renderer.setSeriesPaint(0, ColorProvider.reduceColorBrightness(Color.RED));
			renderer.setSeriesFillPaint(0, ColorProvider.reduceColorBrightness(Color.RED));
		} else {
			for (int i = 0; i < histogramDataset.getSeriesCount(); i++) {
				Color color = getColorProvider(true).getPointColor(
						(double) i / (double) (histogramDataset.getSeriesCount() - 1));
				renderer.setSeriesPaint(i, color);
				renderer.setSeriesFillPaint(i, color);
			}
		}
		renderer.setBarPainter(new RapidXYBarPainter());
		// renderer.setBarPainter(new StandardXYBarPainter());
		renderer.setDrawBarOutline(true);
		renderer.setShadowVisible(false);
		plot.setRenderer(renderer);

		plot.getRangeAxis().setLabelFont(LABEL_FONT_BOLD);
		plot.getRangeAxis().setTickLabelFont(LABEL_FONT);

		plot.getDomainAxis().setLabelFont(LABEL_FONT_BOLD);
		plot.getDomainAxis().setTickLabelFont(LABEL_FONT);
		setRange(plot.getDomainAxis());

		// display correct x-Axis labels
		int count = histogramDataset.getSeriesCount();
		if (count > 0) {
			String key = histogramDataset.getSeriesKey(0).toString();
			int index = this.dataTable.getColumnIndex(key);
			if (index >= 0) {
				// Correctly displays nominal values on x-axis
				if (count == 1 && this.dataTable.isNominal(index)) {
					String[] values = new String[dataTable.getNumberOfValues(index)];
					for (int i = 0; i < values.length; i++) {
						values[i] = dataTable.mapIndex(index, i);
					}
					plot.setDomainAxis(new SymbolAxis(key, values));
				}
				// Correctly displays dates on x-axis
				if (this.dataTable.isDateTime(index)) {
					boolean applyDateAxis = true;
					if (count > 1) {
						for (int i = 1; i < count; i++) {
							index = this.dataTable.getColumnIndex(histogramDataset.getSeriesKey(i).toString());
							if (index < 0 || !this.dataTable.isDateTime(index)) {
								applyDateAxis = false;
								break;
							}
						}
					}
					if (applyDateAxis) {
						DateAxis dateAxis = new DateAxis();
						dateAxis.setDateFormatOverride(Tools.DATE_TIME_FORMAT.get());
						plot.setDomainAxis(dateAxis);
					}
				}
			}

			// rotate labels
			if (isLabelRotating()) {
				plot.getDomainAxis().setTickLabelsVisible(true);
				plot.getDomainAxis().setVerticalTickLabels(true);
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

	private void setRange(ValueAxis axis) {
		Range range = null;
		for (int c = 0; c < this.dataTable.getNumberOfColumns(); c++) {
			if (this.columns[c]) {
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
		if (range != null) {
			axis.setRange(range);
		} else {
			axis.setAutoRange(true);
		}
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		if (index == 0) {
			return logScaleBox;
		} else if (index == 1) {
			return getRotateLabelComponent();
		} else if (index == 2) {
			JLabel label = new JLabel("Number of Bins");
			label.setToolTipText("Set the number of bins which should be displayed.");
			return label;
		} else if (index == 3) {
			return binNumberSlider;
		} else if (index == 4) {
			JLabel label = new JLabel("Opaqueness");
			label.setToolTipText("Sets the amount of opaqueness / transparency.");
			return label;
		} else if (index == 5) {
			return opaquenessSlider;
		} else {
			return null;
		}
	}

	@Override
	public List<ParameterType> getAdditionalParameterKeys(InputPort inputPort) {
		List<ParameterType> types = super.getAdditionalParameterKeys(inputPort);
		types.add(new ParameterTypeBoolean(PARAMETER_LOG_SCALE,
				"Indicates if the bin heights should be transformed with logarithm base 10.", false));
		types.add(new ParameterTypeInt(PARAMETER_NUMBER_OF_BINS, "The number of bins for each histogram.", MIN_BIN_NUMBER,
				MAX_BIN_NUMBER, DEFAULT_BIN_NUMBER));
		types.add(new ParameterTypeInt(PARAMETER_OPAQUENESS,
				"Indicates the opaqueness / transparency of the bins in percent.", 0, 100, 100));
		return types;
	}

	@Override
	public void setAdditionalParameter(String key, String value) {
		super.setAdditionalParameter(key, value);
		if (PARAMETER_LOG_SCALE.equals(key)) {
			this.logScale = Boolean.parseBoolean(value);
			updatePlotter();
		} else if (PARAMETER_NUMBER_OF_BINS.equals(key)) {
			this.binNumber = (int) Double.parseDouble(value);
			updatePlotter();
		} else if (PARAMETER_OPAQUENESS.equals(key)) {
			this.opaqueness = (int) Double.parseDouble(value) / 100f;
			updatePlotter();
		}
	}

	@Override
	public List<PlotterSettingsChangedListener> getListeningObjects() {
		List<PlotterSettingsChangedListener> list = super.getListeningObjects();
		list.add(opaquenessSlider);
		list.add(binNumberSlider);
		list.add(logScaleBox);
		return list;
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.HISTOGRAM_PLOT;
	}

	@Override
	public Collection<String> resolveXAxis(int axisIndex) {
		Collection<String> names = new LinkedList<>();
		for (int i = 0; i < columns.length; i++) {
			if (columns[i]) {
				names.add(dataTable.getColumnName(i));
			}
		}
		return names;
	}

	@Override
	public Collection<String> resolveYAxis(int axisIndex) {
		return Collections.emptyList();
	}
}
