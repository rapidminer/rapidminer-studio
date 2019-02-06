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
package com.rapidminer.gui.viewer.metadata.model;

import java.awt.Color;
import java.util.Arrays;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.viewer.metadata.AttributeStatisticsPanel;
import com.rapidminer.tools.Ontology;


/**
 * Model for {@link AttributeStatisticsPanel}s which are backed by a numerical {@link Attribute}.
 *
 * @author Marco Boeck
 *
 */
public class NumericalAttributeStatisticsModel extends AbstractAttributeStatisticsModel {

	/** the index for the histogram chart */
	private static final int INDEX_HISTOGRAM_CHART = 0;

	/** the max number of bins */
	private static final int MAX_BINS_HISTOGRAM = 10;

	/** used to color the chart background invisible */
	private static final Color COLOR_INVISIBLE = new Color(255, 255, 255, 0);

	/** the average of the numerical values */
	private double average;

	/** the standard deviation of the numerical values */
	private double deviation;

	/** the minimum of the numerical values */
	private double minimum;

	/** the maximum of the numerical values */
	private double maximum;

	/** array of charts for this model */
	private JFreeChart[] chartsArray;

	/**
	 * Creates a new {@link NumericalAttributeStatisticsModel}.
	 *
	 * @param exampleSet
	 * @param attribute
	 */
	public NumericalAttributeStatisticsModel(ExampleSet exampleSet, Attribute attribute) {
		super(exampleSet, attribute);

		chartsArray = new JFreeChart[1];
	}

	@Override
	public void updateStatistics(ExampleSet exampleSet) {
		average = exampleSet.getStatistics(getAttribute(), Statistics.AVERAGE);
		deviation = Math.sqrt(exampleSet.getStatistics(getAttribute(), Statistics.VARIANCE));
		minimum = exampleSet.getStatistics(getAttribute(), Statistics.MINIMUM);
		maximum = exampleSet.getStatistics(getAttribute(), Statistics.MAXIMUM);
		missing = exampleSet.getStatistics(getAttribute(), Statistics.UNKNOWN);

		fireStatisticsChangedEvent();
	}

	/**
	 * Gets the average of the numerical values.
	 *
	 * @return
	 */
	public double getAverage() {
		return average;
	}

	/**
	 * Gets the standard deviation of the numerical values.
	 *
	 * @return
	 */
	public double getDeviation() {
		return deviation;
	}

	/**
	 * Gets the minimum of the numerical values.
	 *
	 * @return
	 */
	public double getMinimum() {
		return minimum;
	}

	/**
	 * Gets the maximum of the numerical values.
	 *
	 * @return
	 */
	public double getMaximum() {
		return maximum;
	}

	@Override
	public JFreeChart getChartOrNull(int index) {
		prepareCharts();
		if (index == INDEX_HISTOGRAM_CHART) {
			return chartsArray[index];
		}

		return null;
	}

	/**
	 * Creates a {@link HistogramDataset} for this {@link Attribute}.
	 *
	 * @param exampleSet
	 * @return
	 */
	private HistogramDataset createHistogramDataset(ExampleSet exampleSet) {
		HistogramDataset dataset = new HistogramDataset();

		double[] array = new double[exampleSet.size()];
		int count = 0;

		for (Example example : exampleSet) {
			double value = example.getDataRow().get(getAttribute());
			// don't use missing values because otherwise JFreeChart tries to plot them too which
			// can lead to false histograms
			if (!Double.isNaN(value)) {
				array[count++] = value;
			}
		}

		// add points to data set (if any)
		if (count > 0) {
			// truncate array if necessary
			if (count < array.length) {
				array = Arrays.copyOf(array, count);
			}
			dataset.addSeries(getAttribute().getName(), array, Math.min(array.length, MAX_BINS_HISTOGRAM));
		}

		return dataset;
	}

	/**
	 * Creates the histogram chart.
	 *
	 * @param exampleSet
	 * @return
	 */
	private JFreeChart createHistogramChart(ExampleSet exampleSet) {
		JFreeChart chart = ChartFactory.createHistogram(null, null, null, createHistogramDataset(exampleSet),
				PlotOrientation.VERTICAL, false, false, false);
		AbstractAttributeStatisticsModel.setDefaultChartFonts(chart);
		chart.setBackgroundPaint(null);
		chart.setBackgroundImageAlpha(0.0f);

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setRangeGridlinesVisible(false);
		plot.setDomainGridlinesVisible(false);
		plot.setOutlineVisible(false);
		plot.setRangeZeroBaselineVisible(false);
		plot.setDomainZeroBaselineVisible(false);
		plot.setBackgroundPaint(COLOR_INVISIBLE);
		plot.setBackgroundImageAlpha(0.0f);

		XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
		renderer.setSeriesPaint(0, AttributeGuiTools.getColorForValueType(Ontology.NUMERICAL));
		renderer.setBarPainter(new StandardXYBarPainter());
		renderer.setDrawBarOutline(true);
		renderer.setShadowVisible(false);

		return chart;
	}

	@Override
	public void prepareCharts() {
		if (chartsArray[INDEX_HISTOGRAM_CHART] == null && getExampleSetOrNull() != null) {
			chartsArray[INDEX_HISTOGRAM_CHART] = createHistogramChart(getExampleSetOrNull());
		}
	}

}
