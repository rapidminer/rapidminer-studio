/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
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
 * This is the main bar chart plotter. The plotter is also capable to produce average aggregations
 * based on an additional group-by attribute.
 *
 * @author Ingo Mierswa
 *
 */
public class BarChartPlotter extends LabelRotatingPlotterAdapter implements ChangeListener {

	public static final String PARAMETER_ORIENTATION = "orientation";

	public static final String PARAMETER_AGGREGATION = "aggregation";

	public static final String PARAMETER_USE_DISTINCT = "use_distinct";

	private static final long serialVersionUID = 1208210421840512091L;

	private static final int MAX_CATEGORY_VIEW_COUNT = 40;

	/** The maximal number of printable categories. */
	private static final int MAX_CATEGORIES = 200;

	private static final String[] ORIENTATION_TYPES = new String[] { "vertical", "horizontal" };

	public static final int ORIENTATION_TYPE_VERTICAL = 0;

	public static final int ORIENTATION_TYPE_HORIZONTAL = 1;

	/** The currently used data table object. */
	private DataTable dataTable;

	/** The pie data set. */
	private DefaultCategoryDataset categoryDataSet = new DefaultCategoryDataset();

	private SlidingCategoryDataset slidingCategoryDataSet = null;

	/** The column which is used for the group by attribute. */
	private int groupByColumn = -1;

	/** The column which is used for the legend. */
	private int legendByColumn = -1;

	/** The column which is used for the values. */
	private int valueColumn = -1;

	/** The orientation of the bars. */
	private ListeningJComboBox<String> orientationType;

	/** Indicates if only distinct values should be used for aggregation functions. */
	private ListeningJCheckBox useDistinct;

	/** The used aggregation function. */
	private ListeningJComboBox<String> aggregationFunction = null;

	/** Indicates if absolute values should be used. */
	private boolean absolute = false;

	private String aggregationFunctionName;

	private boolean useDistinctFlag = false;

	private int orientationIndex = 0;

	private JPanel scrollablePlotterPanel = new JPanel(new BorderLayout());

	private ChartPanel panel = null;

	private JScrollBar viewScrollBar = new JScrollBar(JScrollBar.HORIZONTAL, 0, MAX_CATEGORY_VIEW_COUNT, 0, MAX_CATEGORIES);

	private boolean showScrollbar = true;

	public BarChartPlotter(final PlotterConfigurationModel settings) {
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

		orientationType = new ListeningJComboBox<>(settings, "_" + PARAMETER_ORIENTATION, ORIENTATION_TYPES);
		orientationType.setPreferredSize(
				new Dimension(aggregationFunction.getPreferredSize().width, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		orientationType.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				settings.setParameterAsString(PARAMETER_ORIENTATION, orientationType.getSelectedItem().toString());
			}
		});

		viewScrollBar.getModel().addChangeListener(this);
	}

	public BarChartPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
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
		if (plot) {
			this.valueColumn = index;
		} else {
			this.valueColumn = -1;
		}
		updatePlotter();
	}

	@Override
	public boolean getPlotColumn(int index) {
		return valueColumn == index;
	}

	@Override
	public String getPlotName() {
		return "Value Column";
	}

	@Override
	public int getNumberOfAxes() {
		return 2;
	}

	@Override
	public void setAxis(int index, int dimension) {
		if (index == 0) {
			groupByColumn = dimension;
		} else if (index == 1) {
			legendByColumn = dimension;
		}
		updatePlotter();
	}

	@Override
	public int getAxis(int index) {
		if (index == 0) {
			return groupByColumn;
		} else if (index == 1) {
			return legendByColumn;
		} else {
			return -1;
		}
	}

	@Override
	public String getAxisName(int index) {
		if (index == 0) {
			return "Group-By Column";
		} else if (index == 1) {
			return "Legend Column";
		} else {
			return "Unknown";
		}
	}

	private int prepareData() {
		synchronized (dataTable) {
			AggregationFunction aggregation = null;
			if (aggregationFunctionName != null && !aggregationFunctionName.equals("none")) {
				try {
					aggregation = AbstractAggregationFunction.createAggregationFunction(aggregationFunctionName);
				} catch (Exception e) {
					// LogService.getGlobal().logWarning("Cannot instantiate aggregation function '"
					// + aggregationFunctionName +
					// "', using 'average' as default.");
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.gui.plotter.charts.BarChartPlotter.instantiating_aggregation_function_error",
							aggregationFunctionName);
					aggregation = new AverageFunction();
				}
			}
			Iterator<DataTableRow> i = this.dataTable.iterator();
			Map<String, Collection<Double>> categoryValues = new LinkedHashMap<String, Collection<Double>>();

			categoryDataSet.clear();

			if (groupByColumn >= 0 && dataTable.isNumerical(groupByColumn)) {
				return 0;
			}

			while (i.hasNext()) {
				DataTableRow row = i.next();

				double value = Double.NaN;
				if (valueColumn >= 0) {
					value = row.getValue(valueColumn);
				}

				if (!Double.isNaN(value)) {
					if (absolute) {
						value = Math.abs(value);
					}

					// name
					String valueString = null;
					if (dataTable.isDate(valueColumn)) {
						valueString = Tools.createDateAndFormat(value);
					} else if (dataTable.isTime(valueColumn)) {
						valueString = Tools.createTimeAndFormat(value);
					} else if (dataTable.isDateTime(valueColumn)) {
						valueString = Tools.createDateTimeAndFormat(value);
					} else if (dataTable.isNominal(valueColumn)) {
						valueString = dataTable.mapIndex(valueColumn, (int) value);
					} else {
						valueString = Tools.formatIntegerIfPossible(value);
					}

					String legendName = valueString + "";
					if (legendByColumn >= 0) {
						double nameValue = row.getValue(legendByColumn);
						if (dataTable.isDate(legendByColumn)) {
							legendName = Tools.createDateAndFormat(nameValue);
						} else if (dataTable.isTime(legendByColumn)) {
							legendName = Tools.createTimeAndFormat(nameValue);
						} else if (dataTable.isDateTime(legendByColumn)) {
							legendName = Tools.createDateTimeAndFormat(nameValue);
						} else if (dataTable.isNominal(legendByColumn)) {
							legendName = dataTable.mapIndex(legendByColumn, (int) nameValue) + " (" + valueString + ")";
						} else {
							legendName = Tools.formatIntegerIfPossible(nameValue) + " (" + valueString + ")";
						}
					}

					String groupByName = legendName;
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
			if (valueColumn >= 0) {
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

						if (groupByColumn < 0) {
							categoryDataSet.setValue(value, dataTable.getColumnName(valueColumn), name);
						} else {
							// categoryDataSet.setValue(value, name,
							// dataTable.getColumnName(valueColumn));
							categoryDataSet.setValue(value, dataTable.getColumnName(groupByColumn), name);
						}
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
							if (groupByColumn < 0) {
								categoryDataSet.setValue(value, dataTable.getColumnName(valueColumn), name);
							} else {
								// categoryDataSet.setValue(value, name,
								// dataTable.getColumnName(groupByColumn));
								categoryDataSet.setValue(value, dataTable.getColumnName(groupByColumn), name);
							}
						}
					}
				}
			}

			return categoryValues.size();
		}
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

		CategoryDataset usedCategoryDataSet = categoryDataSet;
		if (categoryCount > MAX_CATEGORY_VIEW_COUNT && showScrollbar) {
			if (orientationIndex == ORIENTATION_TYPE_VERTICAL) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						viewScrollBar.setOrientation(Adjustable.HORIZONTAL);
						scrollablePlotterPanel.add(viewScrollBar, BorderLayout.SOUTH);
					}
				});
			} else {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						viewScrollBar.setOrientation(Adjustable.VERTICAL);
						scrollablePlotterPanel.add(viewScrollBar, BorderLayout.EAST);
					}
				});
			}

			this.slidingCategoryDataSet = new SlidingCategoryDataset(categoryDataSet, 0, MAX_CATEGORY_VIEW_COUNT);
			viewScrollBar.setMaximum(categoryCount);
			viewScrollBar.setValue(0);

			usedCategoryDataSet = this.slidingCategoryDataSet;
		} else {
			this.slidingCategoryDataSet = null;
		}

		if (categoryCount <= MAX_CATEGORIES) {
			JFreeChart chart = ChartFactory.createBarChart(null,                     // chart title
					null,                     // domain axis label
					null,                     // range axis label
					usedCategoryDataSet,      // data
					orientationIndex == ORIENTATION_TYPE_VERTICAL ? PlotOrientation.VERTICAL : PlotOrientation.HORIZONTAL, // orientation
					false,      			  // include legend if group by column is set
					true,                     // tooltips
					false                     // URLs
					);

			// set the background color for the chart...
			chart.setBackgroundPaint(Color.WHITE);
			chart.getPlot().setBackgroundPaint(Color.WHITE);

			CategoryPlot plot = chart.getCategoryPlot();
			plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
			plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

			CategoryAxis domainAxis = plot.getDomainAxis();
			domainAxis.setLabelFont(LABEL_FONT_BOLD);
			domainAxis.setTickLabelFont(LABEL_FONT);
			String domainName = groupByColumn >= 0 ? dataTable.getColumnName(groupByColumn) : null;
			domainAxis.setLabel(domainName);

			// rotate labels
			if (isLabelRotating()) {
				plot.getDomainAxis().setTickLabelsVisible(true);
				plot.getDomainAxis().setCategoryLabelPositions(
						CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 2.0d));
			}

			// set the range axis to display integers only...
			NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
			rangeAxis.setLabelFont(LABEL_FONT_BOLD);
			rangeAxis.setTickLabelFont(LABEL_FONT);
			String rangeName = valueColumn >= 0 ? dataTable.getColumnName(valueColumn) : null;
			rangeAxis.setLabel(rangeName);

			// bar renderer
			double[] colorValues = null;
			if (groupByColumn >= 0 && this.dataTable.isNominal(groupByColumn)) {
				colorValues = new double[this.dataTable.getNumberOfValues(groupByColumn)];
			} else {
				colorValues = new double[categoryDataSet.getColumnCount()];
			}
			for (int i = 0; i < colorValues.length; i++) {
				colorValues[i] = i;
			}

			BarRenderer renderer = new ColorizedBarRenderer(colorValues);
			renderer.setBarPainter(new RapidBarPainter());
			renderer.setDrawBarOutline(true);
			int size = categoryDataSet.getRowCount();
			if (size > 1) {
				for (int i = 0; i < size; i++) {
					renderer.setSeriesPaint(i, getColorProvider(true).getPointColor(i / (double) (size - 1)));
				}
			}
			plot.setRenderer(renderer);

			// legend settings
			LegendTitle legend = chart.getLegend();
			if (legend != null) {
				legend.setPosition(RectangleEdge.TOP);
				legend.setFrame(BlockBorder.NONE);
				legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
				legend.setItemFont(LABEL_FONT);
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

			// ATTENTION: WITHOUT THIS WE GET SEVERE MEMORY LEAKS!!!
			panel.getChartRenderingInfo().setEntityCollection(null);
		} else {
			// LogService.getGlobal().logNote("Too many columns (" + categoryCount +
			// "), this chart is only able to plot up to " + MAX_CATEGORIES +
			// " different categories.");
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
			case 3:
				return getRotateLabelComponent();
			case 4:
				return orientationType;
		}
		return null;
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
		types.add(new ParameterTypeCategory(PARAMETER_ORIENTATION, "The orientation of the bars.", ORIENTATION_TYPES,
				ORIENTATION_TYPE_VERTICAL));
		return types;
	}

	@Override
	public List<PlotterSettingsChangedListener> getListeningObjects() {
		List<PlotterSettingsChangedListener> list = super.getListeningObjects();
		list.add(aggregationFunction);
		list.add(useDistinct);
		list.add(orientationType);
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
		} else if (PARAMETER_ORIENTATION.equals(key)) {
			orientationIndex = value.equals(ORIENTATION_TYPES[0]) ? 0 : 1;
			updatePlotter();
		}
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.BAR_CHART;
	}
}
