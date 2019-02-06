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
import com.rapidminer.gui.plotter.LocalNormalizationPlotterAdapter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.MathFunctions;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.BlockResult;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;


/**
 * This is the new parallel plotter.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ParallelPlotter2 extends LocalNormalizationPlotterAdapter {

	private static final long serialVersionUID = -8763693366081949249L;

	/** The currently used data table object. */
	private transient DataTable dataTable;

	/** The data set used for the plotter. */
	private XYSeriesCollection dataset = null;

	/** The column which is used for the values. */
	private int colorColumn = -1;

	private String[] domainAxisMap = null;

	private ChartPanel panel = new ChartPanel(null);

	private double[] colorMap = null;

	public ParallelPlotter2(final PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
	}

	public ParallelPlotter2(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	@Override
	public JComponent getPlotter() {
		return panel;
	}

	private static JFreeChart createChart(XYDataset dataset) {

		// create the chart...
		JFreeChart chart = ChartFactory.createXYLineChart(null,                      // chart title
				null,                      // x axis label
				null,                      // y axis label
				dataset,                   // data
				PlotOrientation.VERTICAL, false,                     // include legend
				true,                      // tooltips
				false                      // urls
				);

		chart.setBackgroundPaint(Color.white);

		// get a reference to the plot for further customization...
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);

		ValueAxis valueAxis = plot.getRangeAxis();
		valueAxis.setLabelFont(LABEL_FONT_BOLD);
		valueAxis.setTickLabelFont(LABEL_FONT);

		return chart;
	}

	@Override
	public void setDataTable(DataTable dataTable) {
		super.setDataTable(dataTable);
		this.dataTable = dataTable;
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
		return 0;
	}

	private void prepareData() {
		this.dataset = new XYSeriesCollection();

		// calculate min and max
		int columns = this.dataTable.getNumberOfColumns();
		double[] min = new double[columns];
		double[] max = new double[columns];
		if (isLocalNormalized()) {
			for (int c = 0; c < columns; c++) {
				min[c] = Double.POSITIVE_INFINITY;
				max[c] = Double.NEGATIVE_INFINITY;
			}

			synchronized (dataTable) {
				Iterator<DataTableRow> i = dataTable.iterator();
				while (i.hasNext()) {
					DataTableRow row = i.next();
					for (int c = 0; c < dataTable.getNumberOfColumns(); c++) {
						double value = row.getValue(c);
						min[c] = MathFunctions.robustMin(min[c], value);
						max[c] = MathFunctions.robustMax(max[c], value);
					}
				}
			}
		}

		this.domainAxisMap = null;
		synchronized (dataTable) {
			this.colorMap = new double[dataTable.getNumberOfRows()];

			Iterator<DataTableRow> i = this.dataTable.iterator();
			int idCounter = 0;

			while (i.hasNext()) {
				DataTableRow row = i.next();
				String id = row.getId();
				if (id == null) {
					id = (idCounter + 1) + "";
				}

				XYSeries series = new XYSeries(id, false, false);
				int counter = 0;
				for (int column = 0; column < dataTable.getNumberOfColumns(); column++) {
					if ((!dataTable.isSpecial(column)) && (column != colorColumn)) {
						double value = row.getValue(column);
						if (isLocalNormalized()) {
							value = (value - min[column]) / (max[column] - min[column]);
						}
						series.add(counter, value);
						counter++;
					}
				}

				if (colorColumn >= 0) {
					this.colorMap[idCounter] = row.getValue(colorColumn);
				}

				this.dataset.addSeries(series);
				idCounter++;
			}
		}

		if (domainAxisMap == null) {
			List<String> domainValues = new LinkedList<>();
			for (int column = 0; column < dataTable.getNumberOfColumns(); column++) {
				if ((!dataTable.isSpecial(column)) && (column != colorColumn)) {
					domainValues.add(dataTable.getColumnName(column));
				}
			}
			this.domainAxisMap = new String[domainValues.size()];
			domainValues.toArray(this.domainAxisMap);
		}
	}

	@Override
	public void updatePlotter() {
		prepareData();

		JFreeChart chart = createChart(this.dataset);

		// set the background color for the chart...
		chart.setBackgroundPaint(Color.white);

		// general plot settings
		XYPlot plot = chart.getXYPlot();
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

		// domain axis
		SymbolAxis axis = null;
		if (this.dataTable.isSupportingColumnWeights()) {
			List<Double> weightList = new LinkedList<>();
			for (int column = 0; column < dataTable.getNumberOfColumns(); column++) {
				if ((!dataTable.isSpecial(column)) && (column != colorColumn)) {
					weightList.add(this.dataTable.getColumnWeight(column));
				}
			}
			double[] weights = new double[weightList.size()];
			int index = 0;
			for (Double d : weightList) {
				weights[index++] = d;
			}
			axis = new WeightBasedSymbolAxis(null, domainAxisMap, weights);
		} else {
			axis = new SymbolAxis(null, domainAxisMap);
		}
		axis.setTickLabelFont(LABEL_FONT);
		axis.setLabelFont(LABEL_FONT_BOLD);

		// rotate labels
		if (isLabelRotating()) {
			axis.setTickLabelsVisible(true);
			axis.setVerticalTickLabels(true);
		}

		chart.getXYPlot().setDomainAxis(axis);

		// renderer
		final ColorizedLineAndShapeRenderer renderer = new ColorizedLineAndShapeRenderer(this.colorMap);
		plot.setRenderer(renderer);

		// legend settings
		if ((colorColumn >= 0) && (this.dataTable.isNominal(colorColumn))) {
			final LegendItemCollection legendItemCollection = new LegendItemCollection();
			for (int i = 0; i < this.dataTable.getNumberOfValues(colorColumn); i++) {
				legendItemCollection.add(new LegendItem(this.dataTable.mapIndex(colorColumn, i), null, null, null,
						new Rectangle2D.Double(0, 0, 7, 7), getColorProvider().getPointColor(
								i / (double) (this.dataTable.getNumberOfValues(colorColumn) - 1)), new BasicStroke(0.75f),
						Color.GRAY));
			}
			chart.addLegend(new LegendTitle(new LegendItemSource() {

				@Override
				public LegendItemCollection getLegendItems() {
					return legendItemCollection;
				}
			}));

			LegendTitle legend = chart.getLegend();
			if (legend != null) {
				legend.setPosition(RectangleEdge.TOP);
				legend.setFrame(BlockBorder.NONE);
				legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
				legend.setItemFont(LABEL_FONT);
			}
		} else if (colorColumn >= 0) {
			chart.addLegend(new LegendTitle(new LegendItemSource() {

				@Override
				public LegendItemCollection getLegendItems() {
					LegendItemCollection itemCollection = new LegendItemCollection();
					itemCollection.add(new LegendItem("Dummy"));
					return itemCollection;
				}
			}) {

				private static final long serialVersionUID = 1288380309936848376L;

				@Override
				public Object draw(java.awt.Graphics2D g2, java.awt.geom.Rectangle2D area, java.lang.Object params) {
					if (dataTable.isDate(colorColumn) || dataTable.isTime(colorColumn) || dataTable.isDateTime(colorColumn)) {
						drawSimpleDateLegend(g2, (int) (area.getCenterX() - 170), (int) (area.getCenterY() + 7), dataTable,
								colorColumn, renderer.getMinColorValue(), renderer.getMaxColorValue());
						return new BlockResult();
					} else {
						final String minColorString = Tools.formatNumber(renderer.getMinColorValue());
						final String maxColorString = Tools.formatNumber(renderer.getMaxColorValue());
						drawSimpleNumericalLegend(g2, (int) (area.getCenterX() - 90), (int) (area.getCenterY() + 7),
								dataTable.getColumnName(colorColumn), minColorString, maxColorString);
						return new BlockResult();
					}
				}

				@Override
				public void draw(java.awt.Graphics2D g2, java.awt.geom.Rectangle2D area) {
					draw(g2, area, null);
				}

			});
		}

		// chart panel
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
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		if (index == 0) {
			return getLocalNormalizationComponent();
		} else if (index == 1) {
			return getRotateLabelComponent();
		} else {
			return null;
		}
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.PARALLEL_PLOT;
	}
}
