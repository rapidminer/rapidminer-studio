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

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.category.SlidingCategoryDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.plotter.LabelRotatingPlotterAdapter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.PlotterConfigurationModel.PlotterSettingsChangedListener;
import com.rapidminer.gui.plotter.settings.ListeningJCheckBox;
import com.rapidminer.gui.plotter.settings.ListeningJComboBox;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.function.aggregation.AbstractAggregationFunction;
import com.rapidminer.tools.math.function.aggregation.AggregationFunction;
import com.rapidminer.tools.math.function.aggregation.AverageFunction;


/**
 * Web plotter based on the Spiderweb-plotter of JFreechart.
 *
 * @author David Arnu
 * @deprecated since 9.2.0
 */
@Deprecated
public class WebPlotter extends LabelRotatingPlotterAdapter implements ChangeListener {

	public static final String PARAMETER_AGGREGATION = "aggregation";

	public static final String PARAMETER_USE_DISTINCT = "use_distinct";

	private static final long serialVersionUID = 1208210421840512091L;

	private static final int MAX_CATEGORY_VIEW_COUNT = 40;

	/** Indicates which columns will be plotted. */
	private boolean[] columns = new boolean[0];

	/** The maximal number of printable categories. */
	private static final int MAX_CATEGORIES = 200;

	/** The currently used data table object. */
	private DataTable dataTable;

	/** The web data set. */
	private DefaultCategoryDataset categoryDataSet = new DefaultCategoryDataset();

	private SlidingCategoryDataset slidingCategoryDataSet = null;

	/** The column which is used for the group by attribute. */
	private int groupByColumn = -1;

	/** Indicates if only distinct values should be used for aggregation functions. */
	private ListeningJCheckBox useDistinct;

	/** The used aggregation function. */
	private ListeningJComboBox<String> aggregationFunction = null;

	/** Indicates if absolute values should be used. */
	private boolean absolute = false;

	private String aggregationFunctionName;

	private boolean useDistinctFlag = false;

	private JPanel scrollablePlotterPanel = new JPanel(new BorderLayout());

	private ChartPanel panel = null;

	private JScrollBar viewScrollBar = new JScrollBar(JScrollBar.HORIZONTAL, 0, MAX_CATEGORY_VIEW_COUNT, 0, MAX_CATEGORIES);

	private boolean showScrollbar = true;

	public WebPlotter(final PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);

		String[] allFunctions = new String[AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES.length + 1];
		allFunctions[0] = "none";
		System.arraycopy(AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES, 0, allFunctions, 1,
				AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES.length);
		aggregationFunction = new ListeningJComboBox<>(settings, PARAMETER_AGGREGATION, allFunctions);
		aggregationFunction.setPreferredSize(
				new Dimension(aggregationFunction.getPreferredSize().width, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		aggregationFunction
				.setToolTipText("Select the type of the aggregation function which should be used for grouped values.");
		aggregationFunction.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				settings.setParameterAsString(PARAMETER_AGGREGATION, aggregationFunction.getSelectedItem().toString());
			}
		});

		useDistinct = new ListeningJCheckBox(PARAMETER_USE_DISTINCT, "Use Only Distinct", false);
		useDistinct.setToolTipText("Indicates if only distinct values should be used for aggregation functions.");
		useDistinct.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				settings.setParameterAsBoolean(PARAMETER_USE_DISTINCT, useDistinct.isSelected());
			}
		});

		viewScrollBar.getModel().addChangeListener(this);

	}

	public WebPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (slidingCategoryDataSet != null) {
			slidingCategoryDataSet.setFirstCategoryIndex(viewScrollBar.getValue());
		}
	}

	@Override
	public void setDataTable(DataTable dataTable) {
		super.setDataTable(dataTable);
		this.dataTable = dataTable;
		this.columns = new boolean[this.dataTable.getNumberOfColumns()];
		updatePlotter();
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
	public int getNumberOfAxes() {
		return 1;
	}

	@Override
	public int getValuePlotSelectionType() {
		return MULTIPLE_SELECTION;
	}

	@Override
	public void setAxis(int index, int dimension) {
		if (index == 0) {
			groupByColumn = dimension;
		}
		updatePlotter();
	}

	@Override
	public int getAxis(int index) {
		if (index == 0) {
			return groupByColumn;
		} else {
			return -1;
		}
	}

	@Override
	public String getAxisName(int index) {
		if (index == 0) {
			return "Group-By Column";
		} else {
			return "Unknown";
		}
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

	private int prepareData() {

		if (getNumberOfPlots() == 0) {
			return -1;
		}
		int categoryValuesSize = -1;

		categoryDataSet.clear();
		synchronized (dataTable) {
			for (int col = 0; col < this.dataTable.getNumberOfColumns(); col++) {
				if (this.columns[col] && this.dataTable.isNumerical(col)) {
					AggregationFunction aggregation = null;
					if (aggregationFunctionName != null && !aggregationFunctionName.equals("none")) {
						try {
							aggregation = AbstractAggregationFunction.createAggregationFunction(aggregationFunctionName);
						} catch (Exception e) {
							LogService
									.getRoot()
									.log(Level.WARNING,
											"com.rapidminer.gui.plotter.charts.BarChartPlotter.instantiating_aggregation_function_error",
											aggregationFunctionName);
							aggregation = new AverageFunction();
						}
					}
					Iterator<DataTableRow> i = this.dataTable.iterator();
					Map<String, Collection<Double>> categoryValues = new LinkedHashMap<>();

					if (groupByColumn >= 0 && dataTable.isNumerical(groupByColumn)) {
						return 0;
					}

					while (i.hasNext()) {
						DataTableRow row = i.next();

						double value = row.getValue(col);

						if (!Double.isNaN(value)) {
							if (absolute) {
								value = Math.abs(value);
							}

							String groupByName = "";
							if (groupByColumn >= 0) {
								double nameValue = row.getValue(groupByColumn);
								if (dataTable.isDate(groupByColumn)) {
									groupByName = Tools.createDateAndFormat(nameValue);
								} else if (dataTable.isTime(groupByColumn)) {
									groupByName = Tools.createTimeAndFormat(nameValue);
								} else if (dataTable.isDateTime(groupByColumn)) {
									groupByName = Tools.createDateTimeAndFormat(nameValue);
								} else if (dataTable.isNominal(groupByColumn)) {
									groupByName = dataTable.mapIndex(groupByColumn, (int) nameValue);
								} else {
									groupByName = Tools.formatIntegerIfPossible(nameValue) + "";
								}
							}

							// increment values
							Collection<Double> values = categoryValues.get(groupByName);
							if (values == null) {
								if (useDistinctFlag) {
									values = new TreeSet<>();
								} else {
									values = new LinkedList<>();
								}
								categoryValues.put(groupByName, values);
							}
							values.add(value);
						}
					}

					// calculate aggregation and set values
					if (col >= 0) {
						if (aggregation != null) {
							Iterator<Map.Entry<String, Collection<Double>>> c = categoryValues.entrySet().iterator();
							while (c.hasNext()) {
								Map.Entry<String, Collection<Double>> entry = c.next();
								String name = entry.getKey();
								Collection<Double> values = entry.getValue();
								double[] valueArray = new double[values.size()];
								Iterator<Double> v = values.iterator();
								int valueIndex = 0;
								while (v.hasNext()) {
									valueArray[valueIndex++] = v.next();
								}
								double value = aggregation.calculate(valueArray);

								categoryDataSet.setValue(value, name, dataTable.getColumnName(col));
							}
						} else {
							Iterator<Map.Entry<String, Collection<Double>>> c = categoryValues.entrySet().iterator();
							while (c.hasNext()) {
								Map.Entry<String, Collection<Double>> entry = c.next();
								String name = entry.getKey();
								Collection<Double> values = entry.getValue();
								Iterator<Double> v = values.iterator();
								while (v.hasNext()) {
									double value = v.next();
									categoryDataSet.setValue(value, name, dataTable.getColumnName(col));
								}
							}
						}
					}
					categoryValuesSize = categoryValues.size();
				}
			}
		}
		return categoryValuesSize;
	}

	@Override
	public JComponent getPlotter() {
		return scrollablePlotterPanel;
	}

	@Override
	public JComponent getRenderComponent() {
		return panel;
	}

	@Override
	public void prepareRendering() {
		super.prepareRendering();
		this.showScrollbar = false;
		updatePlotter();
	}

	@Override
	public void finishRendering() {
		super.finishRendering();
		this.showScrollbar = true;
		updatePlotter();
	}

	@Override
	public void updatePlotter() {
		final int categoryCount = prepareData();

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				scrollablePlotterPanel.remove(viewScrollBar);
			}
		});

		if (categoryCount > MAX_CATEGORY_VIEW_COUNT && showScrollbar) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					viewScrollBar.setOrientation(Adjustable.HORIZONTAL);
					scrollablePlotterPanel.add(viewScrollBar, BorderLayout.SOUTH);
				}
			});

			this.slidingCategoryDataSet = new SlidingCategoryDataset(categoryDataSet, 0, MAX_CATEGORY_VIEW_COUNT);
			viewScrollBar.setMaximum(categoryCount);
			viewScrollBar.setValue(0);

		} else {
			this.slidingCategoryDataSet = null;
		}

		if (categoryCount <= MAX_CATEGORIES) {

			SpiderWebPlot plot = new SpiderWebPlot(categoryDataSet);

			plot.setAxisLinePaint(Color.LIGHT_GRAY);
			plot.setOutlinePaint(Color.WHITE);

			plot.setLabelGenerator(new StandardCategoryItemLabelGenerator());

			JFreeChart chart = new JFreeChart("", TextTitle.DEFAULT_FONT, plot, true);

			double[] colorValues = null;
			if (groupByColumn >= 0 && this.dataTable.isNominal(groupByColumn)) {
				colorValues = new double[this.dataTable.getNumberOfValues(groupByColumn)];
			} else {
				colorValues = new double[categoryDataSet.getColumnCount()];
			}
			for (int i = 0; i < colorValues.length; i++) {
				colorValues[i] = i;
			}

			if (panel != null) {
				panel.setChart(chart);
			} else {
				panel = new AbstractChartPanel(chart, getWidth(), getHeight() - MARGIN);
				scrollablePlotterPanel.add(panel, BorderLayout.CENTER);
				final ChartPanelShiftController controller = new ChartPanelShiftController(panel);
				panel.addMouseListener(controller);
				panel.addMouseMotionListener(controller);
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
			if (groupByColumn < 0) {
				// no legend is needed when there is no group-by selection
				chart.removeLegend();
			}
			// ATTENTION: WITHOUT THIS WE GET SEVERE MEMORY LEAKS!!!
			panel.getChartRenderingInfo().setEntityCollection(null);
		} else {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.plotter.charts.BarChartPlotter.too_many_columns",
					new Object[] { categoryCount, MAX_CATEGORIES });
		}
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		switch (index) {
			case 0:
				JLabel label = new JLabel("Aggregation:");
				label.setToolTipText("Select the type of the aggregation function which should be used for grouped values.");
				return label;
			case 1:
				return aggregationFunction;
			case 2:
				return useDistinct;
			default:
				return null;
		}
	}

	/** The default implementation delivers an empty set. */
	@Override
	public List<ParameterType> getAdditionalParameterKeys(InputPort inputPort) {
		List<ParameterType> types = super.getAdditionalParameterKeys(inputPort);

		String[] allFunctions = new String[AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES.length + 1];
		allFunctions[0] = "none";
		System.arraycopy(AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES, 0, allFunctions, 1,
				AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES.length);

		types.add(new ParameterTypeCategory(PARAMETER_AGGREGATION,
				"The function used for aggregating the values grouped by the specified column.", allFunctions, 0));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_DISTINCT,
				"Indicates if only distinct values should be regarded for aggregation.", false));
		return types;
	}

	@Override
	public List<PlotterSettingsChangedListener> getListeningObjects() {
		List<PlotterSettingsChangedListener> list = super.getListeningObjects();
		list.add(aggregationFunction);
		list.add(useDistinct);
		return list;
	}

	/** The default implementation does nothing. */
	@Override
	public void setAdditionalParameter(String key, String value) {
		super.setAdditionalParameter(key, value);
		if (PARAMETER_AGGREGATION.equals(key)) {
			try {
				int index = Integer.valueOf(value);
				if (index > 0) {
					aggregationFunctionName = AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES[index - 1];
				} else {
					aggregationFunctionName = "none";
				}
			} catch (NumberFormatException e) {
				aggregationFunctionName = value;
			}
			updatePlotter();
		} else if (PARAMETER_USE_DISTINCT.equals(key)) {
			useDistinctFlag = Boolean.parseBoolean(value);
			updatePlotter();
		}
	}

	@Override
	public Icon getIcon(int index) {
		return null;
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.WEB_PLOT;
	}

}
