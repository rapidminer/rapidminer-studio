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
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

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
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;


/**
 * Model for {@link AttributeStatisticsPanel}s which are backed by a date_time {@link Attribute}.
 *
 * @author Marco Boeck
 *
 */
public class DateTimeAttributeStatisticsModel extends AbstractAttributeStatisticsModel {

	/** the index for the histogram chart */
	private static final int INDEX_HISTOGRAM_CHART = 0;

	/** the max number of bins */
	private static final int MAX_BINS_HISTOGRAM = 10;

	/** used to color the chart background invisible */
	private static final Color COLOR_INVISIBLE = new Color(255, 255, 255, 0);

	/** @{value */
	private static final String WHITESPACE = " ";

	/** short symbol for day: {@value} */
	private static final String SHORT_DAY = "d";

	/** short symbol for hour: {@value} */
	private static final String SHORT_HOUR = "h";

	/** short symbol for minute: {@value} */
	private static final String SHORT_MINUTE = "m";

	/** short symbol for second: {@value} */
	private static final String SHORT_SECOND = "s";

	/** {@value} */
	private static final double MS_IN_S = 1000.0d;

	/** {@value} */
	private static final double S_IN_M = 60.0d;

	/** {@value} */
	private static final double M_IN_H = 60.0d;

	/** {@value} */
	private static final double H_IN_D = 24.0d;

	/** the formatter for date_time values */
	private final DateFormat FORMAT_DATE = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());

	/** the formatter for date_time values */
	private final DateFormat FORMAT_TIME = DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.getDefault());

	/** the formatter for date_time values */
	private final DateFormat FORMAT_DATE_TIME = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
			Locale.getDefault());

	/** the duration of the date_time values */
	private String duration;

	/** the minimum of the date_time values */
	private String from;

	/** the maximum of the date_time values */
	private String until;

	/** array of charts for this model */
	private final JFreeChart[] chartsArray;

	/**
	 * Creates a new {@link DateTimeAttributeStatisticsModel}.
	 *
	 * @param exampleSet
	 * @param attribute
	 */
	public DateTimeAttributeStatisticsModel(final ExampleSet exampleSet, final Attribute attribute) {
		super(exampleSet, attribute);

		chartsArray = new JFreeChart[1];
	}

	@Override
	public void updateStatistics(final ExampleSet exampleSet) {
		final String days = WHITESPACE
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.days.label");
		final String hours = WHITESPACE
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.hours.label");

		long minMilliseconds = (long) exampleSet.getStatistics(getAttribute(), Statistics.MINIMUM);
		long maxMilliseconds = (long) exampleSet.getStatistics(getAttribute(), Statistics.MAXIMUM);
		long difference = maxMilliseconds - minMilliseconds;
		String dura = "";
		if (getAttribute().getValueType() == Ontology.DATE) {
			// days
			dura += com.rapidminer.tools.Tools.formatIntegerIfPossible(
					Math.floor(difference / (H_IN_D * M_IN_H * S_IN_M * MS_IN_S)), 3)
					+ days;
		} else if (getAttribute().getValueType() == Ontology.TIME) {
			// hours
			dura += com.rapidminer.tools.Tools.formatIntegerIfPossible(Math.floor(difference / (M_IN_H * S_IN_M * MS_IN_S)),
					3) + hours;
		} else if (getAttribute().getValueType() == Ontology.DATE_TIME) {
			// days + hours + minutes + seconds
			dura += com.rapidminer.tools.Tools.formatIntegerIfPossible(
					Math.floor(difference / (H_IN_D * M_IN_H * S_IN_M * MS_IN_S)), 3)
					+ SHORT_DAY;
			dura += WHITESPACE;
			double leftoverMilliSeconds = difference % (H_IN_D * M_IN_H * S_IN_M * MS_IN_S);
			dura += com.rapidminer.tools.Tools.formatIntegerIfPossible(
					Math.floor(leftoverMilliSeconds / (M_IN_H * S_IN_M * MS_IN_S)), 3)
					+ SHORT_HOUR;
			dura += WHITESPACE;
			leftoverMilliSeconds = leftoverMilliSeconds % (M_IN_H * S_IN_M * MS_IN_S);
			dura += com.rapidminer.tools.Tools.formatIntegerIfPossible(
					Math.floor(leftoverMilliSeconds / (S_IN_M * MS_IN_S)), 3) + SHORT_MINUTE;
			dura += WHITESPACE;
			leftoverMilliSeconds = leftoverMilliSeconds % (S_IN_M * MS_IN_S);
			dura += com.rapidminer.tools.Tools.formatIntegerIfPossible(Math.floor(leftoverMilliSeconds / MS_IN_S), 3)
					+ SHORT_SECOND;
		}
		String minResult = null;
		String maxResult = null;
		if (getAttribute().getValueType() == Ontology.DATE) {
			minResult = FORMAT_DATE.format(new Date(minMilliseconds));
			maxResult = FORMAT_DATE.format(new Date(maxMilliseconds));
		} else if (getAttribute().getValueType() == Ontology.TIME) {
			minResult = FORMAT_TIME.format(new Date(minMilliseconds));
			maxResult = FORMAT_TIME.format(new Date(maxMilliseconds));
		} else if (getAttribute().getValueType() == Ontology.DATE_TIME) {
			minResult = FORMAT_DATE_TIME.format(new Date(minMilliseconds));
			maxResult = FORMAT_DATE_TIME.format(new Date(maxMilliseconds));
		}
		missing = exampleSet.getStatistics(getAttribute(), Statistics.UNKNOWN);
		from = minResult;
		until = maxResult;
		duration = dura;

		fireStatisticsChangedEvent();
	}

	/**
	 * Gets the duration of the date_time values.
	 *
	 * @return
	 */
	public String getDuration() {
		return duration;
	}

	/**
	 * Gets the from value (min) of the date_time values.
	 *
	 * @return
	 */
	public String getFrom() {
		return from;
	}

	/**
	 * Gets the until value (max) of the date_time values.
	 *
	 * @return
	 */
	public String getUntil() {
		return until;
	}

	@Override
	public JFreeChart getChartOrNull(final int index) {
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
	private HistogramDataset createHistogramDataset(final ExampleSet exampleSet) {
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
	private JFreeChart createHistogramChart(final ExampleSet exampleSet) {
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
		plot.getDomainAxis().setTickLabelsVisible(false);
		plot.setBackgroundPaint(COLOR_INVISIBLE);
		plot.setBackgroundImageAlpha(0.0f);

		XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
		renderer.setSeriesPaint(0, AttributeGuiTools.getColorForValueType(Ontology.DATE_TIME));
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
