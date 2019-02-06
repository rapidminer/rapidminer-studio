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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.DataUtilities;
import org.jfree.data.DefaultKeyedValues;
import org.jfree.data.KeyedValues;
import org.jfree.data.UnknownKeyException;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;
import org.jfree.util.SortOrder;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.plotter.LabelRotatingPlotterAdapter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.PlotterConfigurationModel.PlotterSettingsChangedListener;
import com.rapidminer.gui.plotter.settings.ListeningJCheckBox;
import com.rapidminer.gui.plotter.settings.ListeningJComboBox;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.Tools;


/**
 * This is the Pareto chart plotter.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ParetoChartPlotter extends LabelRotatingPlotterAdapter {

	public static final String PARAMETER_ROTATE_LABELS = "rotate_labels";

	public static final String PARAMETER_SHOW_CUMULATIVE_LABELS = "show_cumulative_labels";

	public static final String PARAMETER_SHOW_BAR_LABELS = "show_bar_labels";

	public static final String PARAMETER_SORTING_DIRECTION = "sorting_direction";

	public static final String PARAMETER_COUNT_VALUE = "count_value";

	private static final long serialVersionUID = -8763693366081949249L;

	/** The currently used data table object. */
	private transient DataTable dataTable;

	private DefaultKeyedValues data = new DefaultKeyedValues();

	private DefaultKeyedValues totalData = new DefaultKeyedValues();

	/** The column which is used for the piece names (or group-by statements). */
	private int groupByColumn = -1;

	/** The column which is used for the legend. */
	private int countColumn = -1;

	private String countValue;
	private int countValueIndex = -1;
	private boolean showBarLabelsFlag = false;
	private boolean showCumulativeLabelsFlag = false;
	private int sortingDirectionIndex;

	private static final String[] SORTING_DIRECTIONS = new String[] { "Descending Keys", "Ascending Keys",
			"Descending Values", "Ascending Values" };

	public static final int KEYS_DESCENDING = 0;
	public static final int KEYS_ASCENDING = 1;
	public static final int VALUES_DESCENDING = 2;
	public static final int VALUES_ASCENDING = 3;

	private ListeningJComboBox<String> countValues;

	private ListeningJComboBox<String> sortingDirection;

	private ListeningJCheckBox showCumulativeLabels;

	private ListeningJCheckBox showBarLabels;

	public ParetoChartPlotter(final PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);

		countValues = new ListeningJComboBox<>(PARAMETER_COUNT_VALUE, 200);
		countValues.setPreferredSize(
				new Dimension(countValues.getPreferredSize().width, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		countValues.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (countValues.getSelectedItem() != null) {
					settings.setParameterAsString(PARAMETER_COUNT_VALUE, countValues.getSelectedItem().toString());
				}
			}
		});

		sortingDirection = new ListeningJComboBox<>(settings, PARAMETER_SORTING_DIRECTION, SORTING_DIRECTIONS);
		sortingDirection.setPreferredSize(
				new Dimension(sortingDirection.getPreferredSize().width, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		sortingDirection.setSelectedIndex(0);
		sortingDirection.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				settings.setParameterAsInt(PARAMETER_SORTING_DIRECTION, sortingDirection.getSelectedIndex());
			}
		});

		showBarLabels = new ListeningJCheckBox(PARAMETER_SHOW_BAR_LABELS, "Show Bar Labels", true);
		showBarLabels.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				settings.setParameterAsBoolean(PARAMETER_SHOW_BAR_LABELS, showBarLabels.isSelected());
			}
		});

		showCumulativeLabels = new ListeningJCheckBox(PARAMETER_SHOW_CUMULATIVE_LABELS, "Show Cumulative Labels", false);
		showCumulativeLabels.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				settings.setParameterAsBoolean(PARAMETER_SHOW_CUMULATIVE_LABELS, showCumulativeLabels.isSelected());
			}
		});
	}

	public ParetoChartPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	private JFreeChart createChart() {
		if (data.getItemCount() > 0) {
			// get cumulative percentages
			KeyedValues cumulative = DataUtilities.getCumulativePercentages(data);

			CategoryDataset categoryDataset = DatasetUtilities.createCategoryDataset(
					"Count for " + this.dataTable.getColumnName(this.countColumn) + " = " + countValue, data);

			// create the chart...
			final JFreeChart chart = ChartFactory.createBarChart(null, // chart title
					this.dataTable.getColumnName(this.groupByColumn), // domain axis label
					"Count", // range axis label
					categoryDataset, // data
					PlotOrientation.VERTICAL, true, // include legend
					true, false);

			// set the background color for the chart...
			chart.setBackgroundPaint(Color.WHITE);

			// get a reference to the plot for further customization...
			CategoryPlot plot = chart.getCategoryPlot();

			CategoryAxis domainAxis = plot.getDomainAxis();
			domainAxis.setLowerMargin(0.02);
			domainAxis.setUpperMargin(0.02);
			domainAxis.setLabelFont(LABEL_FONT_BOLD);
			domainAxis.setTickLabelFont(LABEL_FONT);

			// set the range axis to display integers only...
			NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
			rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits(Locale.US));
			rangeAxis.setLabelFont(LABEL_FONT_BOLD);
			rangeAxis.setTickLabelFont(LABEL_FONT);

			// second data set (cumulative percentages)
			CategoryDataset dataset2 = DatasetUtilities.createCategoryDataset("Cumulative (Percent)", cumulative);

			LineAndShapeRenderer renderer2 = new LineAndShapeRenderer();
			renderer2.setSeriesPaint(0, SwingTools.VERY_DARK_BLUE.darker());

			NumberAxis axis2 = new NumberAxis("Percent of " + countValue);
			axis2.setNumberFormatOverride(NumberFormat.getPercentInstance());
			axis2.setLabelFont(LABEL_FONT_BOLD);
			axis2.setTickLabelFont(LABEL_FONT);

			plot.setRangeAxis(1, axis2);
			plot.setDataset(1, dataset2);
			plot.setRenderer(1, renderer2);
			plot.mapDatasetToRangeAxis(1, 1);

			axis2.setTickUnit(new NumberTickUnit(0.1));

			// show grid lines
			plot.setRangeGridlinesVisible(true);

			// bring cumulative line to front
			plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

			if (isLabelRotating()) {
				domainAxis.setTickLabelsVisible(true);
				domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 2.0d));
			}

			return chart;
		} else {
			return null;
		}
	}

	private void updateValueBox() {
		this.countValues.removeAllItems();
		if (this.countColumn == -1) {
			return;
		}
		if (this.dataTable.isNominal(this.countColumn)) {
			int numberOfValues = this.dataTable.getNumberOfValues(this.countColumn);
			for (int i = 0; i < numberOfValues; i++) {
				this.countValues.addItem(this.dataTable.mapIndex(this.countColumn, i));
			}
			if (countValue != null) {
				countValues.setSelectedItem(countValue);
			}
		}
	}

	@Override
	public void setDataTable(DataTable dataTable) {
		super.setDataTable(dataTable);
		this.dataTable = dataTable;
		repaint();
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		if (plot) {
			this.countColumn = index;
		} else {
			this.countColumn = -1;
		}
		updateValueBox();
		repaint();
	}

	@Override
	public boolean getPlotColumn(int index) {
		return countColumn == index;
	}

	@Override
	public String getPlotName() {
		return "Count Column";
	}

	@Override
	public int getNumberOfAxes() {
		return 1;
	}

	@Override
	public void setAxis(int index, int dimension) {
		if (index == 0) {
			groupByColumn = dimension;
		}
		repaint();
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

	private void prepareData() {
		synchronized (dataTable) {
			data.clear();

			if (this.groupByColumn < 0 || this.countColumn < 0 || countValueIndex < 0) {
				return;
			}

			if (!this.dataTable.isNominal(this.groupByColumn)) {
				return;
			}

			if (!this.dataTable.isNominal(this.countColumn)) {
				return;
			}

			Map<String, AtomicInteger> counters = new HashMap<>();
			Map<String, AtomicInteger> totalCounters = new HashMap<>();
			for (int v = 0; v < this.dataTable.getNumberOfValues(this.groupByColumn); v++) {
				String groupByValue = this.dataTable.mapIndex(this.groupByColumn, v);
				counters.put(groupByValue, new AtomicInteger(0));
				totalCounters.put(groupByValue, new AtomicInteger(0));
			}

			Iterator<DataTableRow> i = this.dataTable.iterator();
			while (i.hasNext()) {
				DataTableRow row = i.next();

				String groupByValue = this.dataTable.getValueAsString(row, this.groupByColumn);
				String countValue = this.dataTable.getValueAsString(row, this.countColumn);

				if (countValue != null && groupByValue != null) {
					if (countValue.equals(this.countValue)) {
						counters.get(groupByValue).getAndIncrement();
					}
					totalCounters.get(groupByValue).getAndIncrement();
				}
			}

			for (Map.Entry<String, AtomicInteger> entry : counters.entrySet()) {
				String category = entry.getKey();
				int categoryCount = entry.getValue().intValue();
				int totalCount = totalCounters.get(category).intValue();
				data.addValue(category, categoryCount);
				totalData.addValue(category, totalCount);
			}

			// sort data
			switch (sortingDirectionIndex) {
				case KEYS_DESCENDING:
					data.sortByKeys(SortOrder.DESCENDING);
					totalData.sortByKeys(SortOrder.DESCENDING);
					break;
				case KEYS_ASCENDING:
					data.sortByKeys(SortOrder.ASCENDING);
					totalData.sortByKeys(SortOrder.ASCENDING);
					break;
				case VALUES_DESCENDING:
					data.sortByValues(SortOrder.DESCENDING);
					totalData.sortByValues(SortOrder.DESCENDING);
					break;
				case VALUES_ASCENDING:
					data.sortByValues(SortOrder.ASCENDING);
					totalData.sortByValues(SortOrder.ASCENDING);
					break;
			}
		}
	}

	@Override
	protected void updatePlotter() {
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		paintParetoChart(g);
	}

	public void paintParetoChart(Graphics graphics) {
		prepareData();

		JFreeChart chart = createChart();

		if (chart != null) {
			// set the background color for the chart...
			chart.setBackgroundPaint(Color.white);
			chart.getPlot().setBackgroundPaint(Color.WHITE);

			// bar renderer --> own 3D effect
			CategoryPlot plot = chart.getCategoryPlot();
			BarRenderer renderer = (BarRenderer) plot.getRenderer();
			// renderer.setBarPainter(new StandardBarPainter());
			renderer.setBarPainter(new RapidBarPainter());

			renderer.setSeriesPaint(0, getColorProvider(true).getPointColor(1));

			// labels on top of bars
			Map<String, String> barItemLabels = new HashMap<>();
			Map<String, String> cumulativeItemLabels = new HashMap<>();
			int groupSum = 0;
			int totalSum = 0;
			for (Object key : totalData.getKeys()) {
				String k = (String) key;
				try {
					Number groupValue = data.getValue(k);
					Number totalValue = totalData.getValue(k);
					groupSum += groupValue.intValue();
					totalSum += totalValue.intValue();
					barItemLabels.put(
							k,
							Tools.formatIntegerIfPossible(groupValue.doubleValue()) + " / "
									+ Tools.formatIntegerIfPossible(totalValue.doubleValue()));
					cumulativeItemLabels.put(k, groupSum + " / " + totalSum);
				} catch (UnknownKeyException e) {
					// do nothing
				}
			}
			renderer.setSeriesItemLabelFont(0, LABEL_FONT);

			if (showBarLabelsFlag) {
				renderer.setSeriesItemLabelsVisible(0, true);
				renderer.setSeriesItemLabelGenerator(0, new ParetoChartItemLabelGenerator(barItemLabels));

				if (isLabelRotating()) {
					renderer.setSeriesPositiveItemLabelPosition(0, new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12,
							TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, -Math.PI / 2.0d));
					renderer.setSeriesNegativeItemLabelPosition(0, new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12,
							TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, -Math.PI / 2.0d));
				}
			}

			LineAndShapeRenderer renderer2 = (LineAndShapeRenderer) chart.getCategoryPlot().getRenderer(1);
			renderer2.setSeriesPaint(0, Color.GRAY.darker().darker());
			renderer2.setSeriesItemLabelFont(0, LABEL_FONT);
			renderer2.setSeriesItemLabelPaint(0, Color.BLACK);
			if (isLabelRotating()) {
				renderer2.setSeriesPositiveItemLabelPosition(0, new ItemLabelPosition(ItemLabelAnchor.OUTSIDE6,
						TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, -Math.PI / 2.0d));
				renderer2.setSeriesNegativeItemLabelPosition(0, new ItemLabelPosition(ItemLabelAnchor.OUTSIDE6,
						TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, -Math.PI / 2.0d));
			} else {
				renderer2.setSeriesPositiveItemLabelPosition(0, new ItemLabelPosition(ItemLabelAnchor.OUTSIDE10,
						TextAnchor.BOTTOM_RIGHT));
				renderer2.setSeriesNegativeItemLabelPosition(0, new ItemLabelPosition(ItemLabelAnchor.OUTSIDE10,
						TextAnchor.BOTTOM_RIGHT));
			}

			if (showCumulativeLabelsFlag) {
				renderer2.setSeriesItemLabelsVisible(0, true);
				renderer2.setSeriesItemLabelGenerator(0, new ParetoChartItemLabelGenerator(cumulativeItemLabels));
			}

			// draw outlines
			renderer.setDrawBarOutline(true);

			// gridline colors
			plot.setRangeGridlinePaint(Color.BLACK);

			// legend settings
			LegendTitle legend = chart.getLegend();
			if (legend != null) {
				legend.setPosition(RectangleEdge.TOP);
				legend.setFrame(BlockBorder.NONE);
				legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
				legend.setItemFont(LABEL_FONT);
			}

			Rectangle2D drawRect = new Rectangle2D.Double(0, 0, getWidth(), getHeight());
			chart.draw((Graphics2D) graphics, drawRect);
		}
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		switch (index) {
			case 0:
				JLabel label = new JLabel("Count Value:");
				label.setToolTipText("Select the value which should be counted in the count dimension.");
				return label;
			case 1:
				return countValues;
			case 2:
				label = new JLabel("Sorting Direction:");
				label.setToolTipText("Select the sorting type and direction.");
				return label;
			case 3:
				return sortingDirection;
			case 4:
				return showBarLabels;
			case 5:
				return showCumulativeLabels;
			case 6:
				return getRotateLabelComponent();
		}
		return null;
	}

	@Override
	public List<ParameterType> getAdditionalParameterKeys(InputPort inputPort) {
		List<ParameterType> types = super.getAdditionalParameterKeys(inputPort);
		boolean inputDeliversAttributes = false;
		if (inputPort != null) {
			MetaData metaData = inputPort.getMetaData();
			if (metaData != null && (metaData instanceof ExampleSetMetaData || metaData instanceof ModelMetaData)) {
				inputDeliversAttributes = true;
			}
		}

		if (inputDeliversAttributes) {
			types.add(new ParameterTypeAttribute(PARAMETER_COUNT_VALUE, "The value which should be counted.", inputPort,
					false));
		} else {
			types.add(new ParameterTypeString(PARAMETER_COUNT_VALUE, "The value which should be counted.", false));
		}
		types.add(new ParameterTypeCategory(PARAMETER_SORTING_DIRECTION, "The direction of sorting.", SORTING_DIRECTIONS,
				KEYS_DESCENDING));
		types.add(new ParameterTypeBoolean(PARAMETER_SHOW_BAR_LABELS, "Indicates if the bar labels should be displayed.",
				true));
		types.add(new ParameterTypeBoolean(PARAMETER_SHOW_CUMULATIVE_LABELS,
				"Indicates if the cumulative labels should be displayed.", false));
		return types;
	}

	@Override
	public List<PlotterSettingsChangedListener> getListeningObjects() {
		List<PlotterSettingsChangedListener> list = super.getListeningObjects();
		list.add(sortingDirection);
		list.add(showBarLabels);
		list.add(showCumulativeLabels);
		return list;
	}

	@Override
	public void setAdditionalParameter(String key, String value) {
		super.setAdditionalParameter(key, value);
		if (PARAMETER_COUNT_VALUE.equals(key)) {
			if (countColumn > -1) {
				countValue = value.toString();
				if (this.dataTable.isNominal(this.countColumn)) {
					int numberOfValues = this.dataTable.getNumberOfValues(this.countColumn);
					for (int i = 0; i < numberOfValues; i++) {
						if (countValue.equals(this.dataTable.mapIndex(this.countColumn, i))) {
							countValueIndex = i;
							break;
						}
					}
					repaint();
				}
			}
		} else if (PARAMETER_SORTING_DIRECTION.equals(key)) {
			for (int i = 0; i < SORTING_DIRECTIONS.length; i++) {
				sortingDirectionIndex = (int) Double.parseDouble(value);
			}
			repaint();
		} else if (PARAMETER_SHOW_BAR_LABELS.equals(key)) {
			showBarLabelsFlag = Boolean.parseBoolean(value);
			repaint();
		} else if (PARAMETER_SHOW_CUMULATIVE_LABELS.equals(key)) {
			showCumulativeLabelsFlag = Boolean.parseBoolean(value);
			repaint();
		}
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.PARETO_PLOT;
	}
}
