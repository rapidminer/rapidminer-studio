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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;

import javax.swing.JComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.RangeablePlotterAdapter;
import com.rapidminer.operator.MissingIOObjectException;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.bayes.DistributionModel;
import com.rapidminer.operator.learner.bayes.NaiveBayes;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.math.distribution.ContinuousDistribution;
import com.rapidminer.tools.math.distribution.DiscreteDistribution;


/**
 * This plotter can be used in order to plot a distribution model like the one which can be
 * delivered by NaiveBayes.
 *
 * @author Sebastian Land, Ingo Mierswa, Tobias Malbrecht
 * @deprecated since 9.2.0
 */
@Deprecated
public class DistributionPlotter extends RangeablePlotterAdapter {

	public static final String RANGE_AXIS_NAME = "Density";

	public static final String MODEL_DOMAIN_AXIS_NAME = "Value";

	private static final long serialVersionUID = 2923008541302883925L;

	private static final int NUMBER_OF_STEPS = 300;

	private static final int MAX_NUMBER_OF_DIFFERENT_NOMINAL_VALUES = 1000;

	private int plotColumn = -1;

	private int groupColumn = -1;

	private transient DistributionModel model;
	private transient HashMap<String, Integer> dataTableModelColumnMap = new HashMap<String, Integer>();

	private transient DataTable dataTable;

	private boolean createFromModel = false;

	public DistributionPlotter(PlotterConfigurationModel settings) {
		super(settings);

	}

	public DistributionPlotter(PlotterConfigurationModel settings, DistributionModel model) {
		this(settings);
		this.model = model;
		this.createFromModel = true;
		this.plotColumn = 0;

		updatePlotter();
	}

	public DistributionPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	@Override
	public void prepareRendering() {
		super.prepareRendering();
		updatePlotter();
	}

	@Override
	public void finishRendering() {
		super.finishRendering();
		updatePlotter();
	}

	public void preparePlots() {
		if (!createFromModel) {
			if (groupColumn >= 0 && plotColumn >= 0 && groupColumn != plotColumn) {
				ExampleSet wrappedExampleSet = DataTableExampleSetAdapter.createExampleSetFromDataTable(this.dataTable);
				Attribute[] attributes = Tools.createRegularAttributeArray(wrappedExampleSet);
				Attribute label = attributes[groupColumn];
				if (label.isNominal()) {
					wrappedExampleSet.getAttributes().setLabel(label);
					try {
						NaiveBayes modelLearner = OperatorService.createOperator(NaiveBayes.class);
						this.model = (DistributionModel) modelLearner.doWork(wrappedExampleSet);

						// updating column map
						dataTableModelColumnMap.clear();
						int modelColumn = 0;
						for (Attribute attribute : wrappedExampleSet.getAttributes()) {
							dataTableModelColumnMap.put(attribute.getName(), modelColumn);
							modelColumn++;
						}

					} catch (OperatorCreationException e) {
						// LogService.getGlobal().logWarning("Cannot create distribution model generator. Skip plot...");
						LogService
								.getRoot()
								.log(Level.WARNING,
										"com.rapidminer.gui.plotter.charts.DistributionPlotter.creating_distribution_model_generator_error");
					} catch (MissingIOObjectException e) {
						// LogService.getGlobal().logWarning("No distribution model was created from data. Skip plot...");
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.gui.plotter.charts.DistributionPlotter.no_distribution_model_was_created");
					} catch (OperatorException e) {
						// LogService.getGlobal().logWarning("Error during creation of distribution model. Skip plot...");
						LogService
								.getRoot()
								.log(Level.WARNING,
										"com.rapidminer.gui.plotter.charts.DistributionPlotter.error_during_creation_of_distribution_model");
					}
				}
			}
		}
	}

	/**
	 * This method translates the plotColumn selected from the original data table to an attribute
	 * index of the model. They might differ, because during model construction the label is shifted
	 * from it's original position to the end, causing a shift of all subsequent attributes to left.
	 */
	private int translateToModelColumn(int plotColumn) {
		if (!createFromModel) {
			return dataTableModelColumnMap.get(dataTable.getColumnName(plotColumn));
		}
		return plotColumn;
	}

	@Override
	public void updatePlotter() {

		JFreeChart chart = null;
		Attribute attr = null;
		if (plotColumn != -1 && createFromModel) {
			attr = model.getTrainingHeader().getAttributes().get(model.getAttributeNames()[plotColumn]);
		}
		if (attr != null && attr.isNominal()
				&& attr.getMapping().getValues().size() > MAX_NUMBER_OF_DIFFERENT_NOMINAL_VALUES) {
			// showing no chart because of too many different values
			chart = new JFreeChart(new Plot() {

				private static final long serialVersionUID = 1L;

				@Override
				public String getPlotType() {
					return "empty";
				}

				@Override
				public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, PlotState parentState,
						PlotRenderingInfo info) {
					String msg = I18N.getGUILabel("plotter_panel.too_many_nominals", "Distribution Plotter",
							DistributionPlotter.MAX_NUMBER_OF_DIFFERENT_NOMINAL_VALUES);
					g2.setColor(Color.BLACK);
					g2.setFont(g2.getFont().deriveFont(g2.getFont().getSize() * 1.35f));
					g2.drawChars(msg.toCharArray(), 0, msg.length(), 50, (int) (area.getHeight() / 2 + 0.5d));
				}
			});
			AbstractChartPanel panel = getPlotterPanel();
			// Chart Panel Settings
			if (panel == null) {
				panel = createPanel(chart);
			} else {
				panel.setChart(chart);
			}
			// ATTENTION: WITHOUT THIS WE GET SEVERE MEMORY LEAKS!!!
			panel.getChartRenderingInfo().setEntityCollection(null);
		} else {
			preparePlots();

			if (!createFromModel && (groupColumn < 0 || plotColumn < 0)) {
				CategoryDataset dataset = new DefaultCategoryDataset();
				chart = ChartFactory.createBarChart(null, // chart title
						"Not defined", // x axis label
						RANGE_AXIS_NAME, // y axis label
						dataset, // data
						PlotOrientation.VERTICAL, true, // include legend
						true, // tooltips
						false // urls
						);
			} else {
				try {
					if (model.isDiscrete(translateToModelColumn(plotColumn))) {
						chart = createNominalChart();
					} else {
						chart = createNumericalChart();
					}
				} catch (Exception e) {
					// do nothing - just do not draw the chart
				}
			}

			if (chart != null) {
				chart.setBackgroundPaint(Color.white);

				// get a reference to the plot for further customization...
				Plot commonPlot = chart.getPlot();
				commonPlot.setBackgroundPaint(Color.WHITE);
				if (commonPlot instanceof XYPlot) {
					XYPlot plot = (XYPlot) commonPlot;

					plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
					plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

					// domain axis
					if (dataTable != null) {
						if (dataTable.isDate(plotColumn) || dataTable.isDateTime(plotColumn)) {
							DateAxis domainAxis = new DateAxis(dataTable.getColumnName(plotColumn));
							domainAxis.setTimeZone(com.rapidminer.tools.Tools.getPreferredTimeZone());
							plot.setDomainAxis(domainAxis);
						} else {
							NumberAxis numberAxis = new NumberAxis(dataTable.getColumnName(plotColumn));
							plot.setDomainAxis(numberAxis);
						}
					}

					plot.getDomainAxis().setLabelFont(LABEL_FONT_BOLD);
					plot.getDomainAxis().setTickLabelFont(LABEL_FONT);

					plot.getRangeAxis().setLabelFont(LABEL_FONT_BOLD);
					plot.getRangeAxis().setTickLabelFont(LABEL_FONT);

					// ranging
					if (dataTable != null) {
						Range range = getRangeForDimension(plotColumn);
						if (range != null) {
							plot.getDomainAxis().setRange(range, true, false);
						}

						range = getRangeForName(RANGE_AXIS_NAME);
						if (range != null) {
							plot.getRangeAxis().setRange(range, true, false);
						}
					}

					// rotate labels
					if (isLabelRotating()) {
						plot.getDomainAxis().setTickLabelsVisible(true);
						plot.getDomainAxis().setVerticalTickLabels(true);
					}

				} else if (commonPlot instanceof CategoryPlot) {
					CategoryPlot plot = (CategoryPlot) commonPlot;

					plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
					plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

					plot.getRangeAxis().setLabelFont(LABEL_FONT_BOLD);
					plot.getRangeAxis().setTickLabelFont(LABEL_FONT);

					plot.getDomainAxis().setLabelFont(LABEL_FONT_BOLD);
					plot.getDomainAxis().setTickLabelFont(LABEL_FONT);
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
				// Chart Panel Settings
				if (panel == null) {
					panel = createPanel(chart);
				} else {
					panel.setChart(chart);
				}

				// ATTENTION: WITHOUT THIS WE GET SEVERE MEMORY LEAKS!!!
				panel.getChartRenderingInfo().setEntityCollection(null);
			}
		}
	}

	private XYDataset createNumericalDataSet() {
		XYSeriesCollection dataSet = new XYSeriesCollection();
		int translatedPlotColumn = translateToModelColumn(plotColumn);
		double start = model.getLowerBound(translatedPlotColumn);
		double end = model.getUpperBound(translatedPlotColumn);
		double stepSize = (end - start) / (NUMBER_OF_STEPS - 1);
		for (int classIndex : model.getClassIndices()) {
			XYSeries series = new XYSeries(model.getClassName(classIndex));
			ContinuousDistribution distribution = (ContinuousDistribution) model.getDistribution(classIndex,
					translatedPlotColumn);
			for (double currentValue = start; currentValue < end; currentValue += stepSize) {
				double probability = distribution.getProbability(currentValue);
				if (!Double.isNaN(probability)) {
					series.add(currentValue, distribution.getProbability(currentValue));
				}
			}
			dataSet.addSeries(series);
		}
		return dataSet;
	}

	private JFreeChart createNumericalChart() {
		JFreeChart chart;
		XYDataset dataset = createNumericalDataSet();
		// create the chart...
		String domainName = dataTable == null ? MODEL_DOMAIN_AXIS_NAME : dataTable.getColumnName(plotColumn);
		chart = ChartFactory.createXYLineChart(null, // chart title
				domainName, // x axis label
				RANGE_AXIS_NAME, // y axis label
				dataset, // data
				PlotOrientation.VERTICAL, true, // include legend
				true, // tooltips
				false // urls
				);

		DeviationRenderer renderer = new DeviationRenderer(true, false);
		Stroke stroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		if (dataset.getSeriesCount() == 1) {
			renderer.setSeriesStroke(0, stroke);
			renderer.setSeriesPaint(0, Color.RED);
			renderer.setSeriesFillPaint(0, Color.RED);
		} else {
			for (int i = 0; i < dataset.getSeriesCount(); i++) {
				renderer.setSeriesStroke(i, stroke);
				Color color = getColorProvider().getPointColor((double) i / (double) (dataset.getSeriesCount() - 1));
				renderer.setSeriesPaint(i, color);
				renderer.setSeriesFillPaint(i, color);
			}
		}
		renderer.setAlpha(0.12f);

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setRenderer(renderer);

		return chart;
	}

	private JFreeChart createNominalChart() {
		JFreeChart chart;
		CategoryDataset dataset = createNominalDataSet();

		// create the chart...
		String domainName = dataTable == null ? MODEL_DOMAIN_AXIS_NAME : dataTable.getColumnName(plotColumn);
		chart = ChartFactory.createBarChart(null, // chart title
				domainName, // x axis label
				RANGE_AXIS_NAME, // y axis label
				dataset, // data
				PlotOrientation.VERTICAL, true, // include legend
				true, // tooltips
				false // urls
				);

		CategoryPlot plot = chart.getCategoryPlot();

		BarRenderer renderer = new BarRenderer();
		if (dataset.getRowCount() == 1) {
			renderer.setSeriesPaint(0, Color.RED);
			renderer.setSeriesFillPaint(0, Color.RED);
		} else {
			for (int i = 0; i < dataset.getRowCount(); i++) {
				Color color = getColorProvider(true).getPointColor((double) i / (double) (dataset.getRowCount() - 1));
				renderer.setSeriesPaint(i, color);
				renderer.setSeriesFillPaint(i, color);
			}
		}
		renderer.setBarPainter(new RapidBarPainter());
		renderer.setDrawBarOutline(true);
		plot.setRenderer(renderer);

		// rotate labels
		if (isLabelRotating()) {
			plot.getDomainAxis().setTickLabelsVisible(true);
			plot.getDomainAxis().setCategoryLabelPositions(
					CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 2.0d));
		}

		return chart;
	}

	private CategoryDataset createNominalDataSet() {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (Integer classIndex : model.getClassIndices()) {
			DiscreteDistribution distribution = (DiscreteDistribution) model.getDistribution(classIndex,
					translateToModelColumn(plotColumn));
			String labelName = model.getClassName(classIndex);

			// sort values by name
			TreeMap<String, Double> valueMap = new TreeMap<String, Double>();
			for (Double value : distribution.getValues()) {
				String valueName;
				if (Double.isNaN(value)) {
					valueName = "Unknown";
				} else {
					valueName = distribution.mapValue(value);
				}
				valueMap.put(valueName, value);
			}
			for (Entry<String, Double> entry : valueMap.entrySet()) {
				dataset.addValue(distribution.getProbability(entry.getValue()), labelName, entry.getKey());
			}
		}
		return dataset;
	}

	@Override
	public void setPlotColumn(int column, boolean plot) {
		this.plotColumn = column;
		updatePlotter();
	}

	@Override
	public boolean getPlotColumn(int column) {
		return column == this.plotColumn;
	}

	@Override
	public String getPlotName() {
		return "Plot Column";
	}

	@Override
	public int getNumberOfAxes() {
		return 1;
	}

	@Override
	public void setAxis(int index, int dimension) {
		if (groupColumn != dimension) {
			groupColumn = dimension;
			updatePlotter();
		}
	}

	@Override
	public int getAxis(int index) {
		return groupColumn;
	}

	@Override
	public String getAxisName(int axis) {
		return "Class Column";
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		if (index == 0) {
			return getRotateLabelComponent();
		} else {
			return null;
		}
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.DISTRIBUTION_PLOT;
	}

	@Override
	public void dataTableSet() {
		this.dataTable = getDataTable();
		if (!createFromModel) {
			updatePlotter();
		}
	}

	@Override
	public Collection<String> resolveXAxis(int axisIndex) {
		if (dataTable != null && plotColumn != -1) {
			return Collections.singletonList(dataTable.getColumnName(plotColumn));
		} else if (createFromModel) {
			return Collections.singletonList(MODEL_DOMAIN_AXIS_NAME);
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public Collection<String> resolveYAxis(int axisIndex) {
		return Collections.singletonList(RANGE_AXIS_NAME);
	}
}
