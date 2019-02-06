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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.plotter.PlotterAdapter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.PlotterConfigurationModel.PlotterSettingsChangedListener;
import com.rapidminer.gui.plotter.PlotterPanel;
import com.rapidminer.gui.plotter.settings.ListeningJCheckBox;
import com.rapidminer.gui.plotter.settings.ListeningJComboBox;
import com.rapidminer.gui.plotter.settings.ListeningJSlider;
import com.rapidminer.gui.plotter.settings.ListeningListSelectionModel;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.ExtendedJList;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedListModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.function.aggregation.AbstractAggregationFunction;
import com.rapidminer.tools.math.function.aggregation.AggregationFunction;
import com.rapidminer.tools.math.function.aggregation.AverageFunction;


/**
 * This is the main pie chart plotter.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class AbstractPieChartPlotter extends PlotterAdapter {

	public static final String PARAMETERS_AGGREGATION = "aggregation";

	public static final String PARAMETERS_USE_DISTINCT = "use_distinct";

	public static final String PARAMETERS_EXPLOSION_GROUPS = "explosion_groups";

	public static final String PARAMETERS_EXPLOSION_AMOUNT = "explosion_amount";

	private static final long serialVersionUID = 8750708105082707503L;

	/** The maximal number of printable categories. */
	private static final int MAX_CATEGORIES = 50;

	/** The currently used data table object. */
	private DataTable dataTable;

	/** The pie data set. */
	private DefaultPieDataset pieDataSet = new DefaultPieDataset();

	/** The column which is used for the piece names (or group-by statements). */
	private int groupByColumn = -1;

	/** The column which is used for the legend. */
	private int legendByColumn = -1;

	/** The column which is used for the values. */
	private int valueColumn = -1;

	/** Indicates if only distinct values should be used for aggregation functions. */
	private ListeningJCheckBox useDistinct;

	/** The used aggregation function. */
	private ListeningJComboBox<String> aggregationFunction = null;

	/** Indicates if absolute values should be used. */
	private boolean absoluteFlag = false;

	/** The currently selected groups for explosion. */
	private String[] explodingGroups = new String[0];

	/**
	 * This list hold all groups of the selected grouping column which should be selected for
	 * explosion.
	 */
	private ExtendedJList<String> explodingGroupList;
	private ListeningListSelectionModel explodingGroupListSelectionModel;

	/** The slider for the amount of explosion. */
	private ListeningJSlider explodingSlider;

	/** The currently selected amount of explosion. */
	private double explodingAmount = 0.0d;

	private String selectedAggregationFunction;
	private boolean useDistinctFlag = false;

	private ChartPanel panel = new ChartPanel(null);

	public AbstractPieChartPlotter(final PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
		useDistinct = new ListeningJCheckBox("_" + PARAMETERS_USE_DISTINCT, "Use Only Distinct", false);
		useDistinct.setToolTipText("Indicates if only distinct values should be used for aggregation functions.");
		useDistinct.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				settings.setParameterAsBoolean(PARAMETERS_USE_DISTINCT, useDistinct.isSelected());
			}
		});

		String[] allFunctions = new String[AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES.length + 1];
		allFunctions[0] = "none";
		System.arraycopy(AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES, 0, allFunctions, 1,
				AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES.length);
		aggregationFunction = new ListeningJComboBox<>(settings, "_" + PARAMETERS_AGGREGATION, allFunctions);
		aggregationFunction.setPreferredSize(
				new Dimension(aggregationFunction.getPreferredSize().width, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		aggregationFunction
				.setToolTipText("Select the type of the aggregation function which should be used for grouped values.");
		aggregationFunction.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				settings.setParameterAsString(PARAMETERS_AGGREGATION, aggregationFunction.getSelectedItem().toString());
			}
		});

		for (int i = 0; i < allFunctions.length; i++) {
			if (allFunctions[i].equals("count")) {
				aggregationFunction.setSelectedIndex(i);
			}
		}

		explodingGroupList = new ExtendedJList<>(new ExtendedListModel<>(), 200);
		explodingGroupListSelectionModel = new ListeningListSelectionModel("_" + PARAMETERS_EXPLOSION_GROUPS,
				explodingGroupList);
		explodingGroupList.setSelectionModel(explodingGroupListSelectionModel);
		explodingGroupList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					List<String> list = explodingGroupList.getSelectedValuesList();
					String result = ParameterTypeEnumeration.transformEnumeration2String(list);
					settings.setParameterAsString(PARAMETERS_EXPLOSION_GROUPS, result);
				}
			}
		});
		explodingGroupList.setForeground(Color.BLACK);
		explodingGroupList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		explodingGroupList.setCellRenderer(new PlotterPanel.LineStyleCellRenderer<>(this));

		updateGroups();

		explodingSlider = new ListeningJSlider("_" + PARAMETERS_EXPLOSION_AMOUNT, 0, 100, 0);
		explodingSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				settings.setParameterAsInt(PARAMETERS_EXPLOSION_AMOUNT, explodingSlider.getValue());
			}
		});
	}

	public AbstractPieChartPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	public abstract JFreeChart createChart(PieDataset pieDataSet, boolean createLegend);

	public abstract boolean isSupportingExplosion();

	@Override
	public void setDataTable(DataTable dataTable) {
		super.setDataTable(dataTable);
		this.dataTable = dataTable;
		groupByColumn = -1;
		legendByColumn = -1;
		valueColumn = -1;
		absoluteFlag = false;
		explodingGroups = new String[0];
		explodingAmount = 0.0d;
		updatePlotter();
	}

	@Override
	public void setAbsolute(boolean absolute) {
		this.absoluteFlag = absolute;
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
	public Icon getIcon(int index) {
		return null;
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
			updateGroups();
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

	private void updateGroups() {
		SortedSet<String> groups = new TreeSet<String>();
		if (groupByColumn >= 0) {
			synchronized (dataTable) {
				if (dataTable.isDate(groupByColumn)) {
					for (int i = 0; i < dataTable.getNumberOfRows(); i++) {
						DataTableRow row = dataTable.getRow(i);
						groups.add(Tools.createDateAndFormat(row.getValue(groupByColumn)));
					}
				} else if (dataTable.isTime(groupByColumn)) {
					for (int i = 0; i < dataTable.getNumberOfRows(); i++) {
						DataTableRow row = dataTable.getRow(i);
						groups.add(Tools.createTimeAndFormat(row.getValue(groupByColumn)));
					}
				} else if (dataTable.isDateTime(groupByColumn)) {
					for (int i = 0; i < dataTable.getNumberOfRows(); i++) {
						DataTableRow row = dataTable.getRow(i);
						groups.add(Tools.createDateTimeAndFormat(row.getValue(groupByColumn)));
					}
				} else if (dataTable.isNominal(groupByColumn)) {
					for (int i = 0; i < dataTable.getNumberOfRows(); i++) {
						DataTableRow row = dataTable.getRow(i);
						groups.add(dataTable.mapIndex(groupByColumn, (int) row.getValue(groupByColumn)));
					}
				} else {
					for (int i = 0; i < dataTable.getNumberOfRows(); i++) {
						DataTableRow row = dataTable.getRow(i);
						groups.add(Tools.formatIntegerIfPossible(row.getValue(groupByColumn)));
					}
				}
			}
		}
		ExtendedListModel<String> model = new ExtendedListModel<>();
		if (groups.size() > 0) {
			for (String group : groups) {
				model.addElement(group, "Select group '" + group + "' for explosion.");
			}
		} else {
			model.addElement("Specify 'Group By' first...");
		}
		this.explodingGroupList.setModel(model);
	}

	private int prepareData() {
		synchronized (dataTable) {
			AggregationFunction aggregation = null;
			if (selectedAggregationFunction != null && !selectedAggregationFunction.equals("none")) {
				try {
					aggregation = AbstractAggregationFunction.createAggregationFunction(selectedAggregationFunction);
				} catch (Exception e) {
					// LogService.getGlobal().logWarning("Cannot instantiate aggregation function '"
					// + selectedAggregationFunction +
					// "', using 'average' as default.");
					LogService
							.getRoot()
							.log(Level.WARNING,
									"com.rapidminer.gui.plotter.charts.AbstractPieChartPlotter.instantiating_aggregation_function_error",
									selectedAggregationFunction);
					aggregation = new AverageFunction();
				}
			}
			Iterator<DataTableRow> i = this.dataTable.iterator();
			Map<String, Collection<Double>> categoryValues = new LinkedHashMap<String, Collection<Double>>();

			pieDataSet.clear();

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
					if (absoluteFlag) {
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
							values = new TreeSet<Double>();
						} else {
							values = new LinkedList<Double>();
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
						if (legendByColumn >= 0) {
							pieDataSet.setValue(name, value);
						} else {
							pieDataSet.setValue(name + " (" + Tools.formatIntegerIfPossible(value) + ")", value);
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
							if (legendByColumn >= 0) {
								pieDataSet.setValue(name, value);
							} else {
								pieDataSet.setValue(name + " (" + Tools.formatIntegerIfPossible(value) + ")", value);
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
		return panel;
	}

	public void updatePlotter() {
		int categoryCount = prepareData();
		String maxClassesProperty = ParameterService
				.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_CLASSLIMIT);
		int maxClasses = 20;
		try {
			if (maxClassesProperty != null) {
				maxClasses = Integer.parseInt(maxClassesProperty);
			}
		} catch (NumberFormatException e) {
			// LogService.getGlobal().log("Pie Chart plotter: cannot parse property 'rapidminer.gui.plotter.colors.classlimit', using maximal 20 different classes.",
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.plotter.charts.AbstractPieChartPlotter.pie_chart_plotter_parsing_error");
		}
		boolean createLegend = categoryCount > 0 && categoryCount < maxClasses;

		if (categoryCount <= MAX_CATEGORIES) {
			JFreeChart chart = createChart(pieDataSet, createLegend);

			// set the background color for the chart...
			chart.setBackgroundPaint(Color.white);

			PiePlot plot = (PiePlot) chart.getPlot();

			plot.setBackgroundPaint(Color.WHITE);
			plot.setSectionOutlinesVisible(true);
			plot.setShadowPaint(new Color(104, 104, 104, 100));

			int size = pieDataSet.getKeys().size();
			for (int i = 0; i < size; i++) {
				Comparable<?> key = pieDataSet.getKey(i);
				plot.setSectionPaint(key, getColorProvider(true).getPointColor(i / (double) (size - 1)));

				boolean explode = false;
				for (String explosionGroup : explodingGroups) {
					if (key.toString().startsWith(explosionGroup) || explosionGroup.startsWith(key.toString())) {
						explode = true;
						break;
					}
				}

				if (explode) {
					plot.setExplodePercent(key, this.explodingAmount);
				}
			}

			plot.setLabelFont(LABEL_FONT);
			plot.setNoDataMessage("No data available");
			plot.setCircular(true);
			plot.setLabelGap(0.02);
			plot.setOutlinePaint(Color.WHITE);

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
		} else {
			// LogService.getGlobal().logNote("Too many columns (" + categoryCount +
			// "), this chart is only able to plot up to " + MAX_CATEGORIES +
			// " different categories.");
			LogService.getRoot().log(Level.INFO,
					"com.rapidminer.gui.plotter.charts.AbstractPieChartPlotter.too_many_columns",
					new Object[] { categoryCount, MAX_CATEGORIES });
		}
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		switch (index) {
			case 0:
				JLabel label = new JLabel("Aggregation");
				label.setToolTipText("Select the type of the aggregation function which should be used for grouped values.");
				return label;
			case 1:
				return aggregationFunction;
			case 2:
				return useDistinct;
			case 3:
				if (isSupportingExplosion()) {
					label = new JLabel("Explosion Groups");
					label.setToolTipText("Select the groups which should explode, i.e. which should be located outside of the chart to the specified extent.");
					return label;
				} else {
					return null;
				}
			case 4:
				if (isSupportingExplosion()) {
					explodingGroupList
							.setToolTipText("Select the groups which should explode, i.e. which should be located outside of the chart to the specified extent.");
					JScrollPane pane = new ExtendedJScrollPane(explodingGroupList);
					pane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Colors.TEXTFIELD_BORDER));
					return pane;
				} else {
					return null;
				}
			case 5:
				if (isSupportingExplosion()) {
					label = new JLabel("Explosion Amount");
					label.setToolTipText("Select the amount of explosion for the selected groups.");
					return label;
				} else {
					return null;
				}
			case 6:
				if (isSupportingExplosion()) {
					explodingSlider.setToolTipText("Select the amount of explosion for the selected groups.");
					return explodingSlider;
				}
		}
		return null;
	}

	@Override
	public List<ParameterType> getAdditionalParameterKeys(InputPort inputPort) {
		List<ParameterType> types = super.getAdditionalParameterKeys(inputPort);
		String[] allFunctions = new String[AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES.length + 1];
		allFunctions[0] = "none";
		System.arraycopy(AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES, 0, allFunctions, 1,
				AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES.length);
		types.add(new ParameterTypeCategory(PARAMETERS_AGGREGATION,
				"The function used for aggregating the values grouped by the specified column.", allFunctions, 0));
		types.add(new ParameterTypeBoolean(PARAMETERS_USE_DISTINCT,
				"Indicates if only distinct values should be regarded for aggregation.", false));
		if (isSupportingExplosion()) {
			types.add(new ParameterTypeString(
					PARAMETERS_EXPLOSION_GROUPS,
					"A comma separated list of groups which should be exploded, i.e. moved out from the center of the plot.",
					true));
			types.add(new ParameterTypeInt(PARAMETERS_EXPLOSION_AMOUNT,
					"The percentage of explosion for the selected groups.", 0, 100, 0));
		}
		return types;
	}

	/** The default implementation does nothing. */
	@Override
	public void setAdditionalParameter(String key, String value) {
		super.setAdditionalParameter(key, value);
		if (key.equals(PARAMETERS_AGGREGATION)) {
			selectedAggregationFunction = value;
			updatePlotter();
		} else if (key.equals(PARAMETERS_USE_DISTINCT)) {
			useDistinctFlag = Boolean.parseBoolean(value);
			updatePlotter();
		} else if (key.equals(PARAMETERS_EXPLOSION_GROUPS)) {
			String[] newGroups = new String[0];
			if (value != null) {
				newGroups = value.split(",");
			}
			for (int i = 0; i < newGroups.length; i++) {
				newGroups[i] = newGroups[i].trim();
			}
			this.explodingGroups = newGroups;
			updatePlotter();
		} else if (key.equals(PARAMETERS_EXPLOSION_AMOUNT)) {
			explodingAmount = Double.parseDouble(value) / 50d;
			updatePlotter();
		} else if (key.equals(PARAMETERS_USE_DISTINCT)) {
			this.useDistinctFlag = Boolean.parseBoolean(value);
			updatePlotter();
		}
	}

	@Override
	public List<PlotterSettingsChangedListener> getListeningObjects() {
		List<PlotterSettingsChangedListener> listeningObjects = super.getListeningObjects();
		listeningObjects.add(explodingGroupListSelectionModel);
		listeningObjects.add(useDistinct);
		listeningObjects.add(explodingSlider);
		listeningObjects.add(aggregationFunction);
		return listeningObjects;
	}
}
