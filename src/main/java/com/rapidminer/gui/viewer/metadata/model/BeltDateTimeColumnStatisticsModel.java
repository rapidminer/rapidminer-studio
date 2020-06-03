/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.viewer.metadata.model;

import java.awt.Color;
import java.text.DateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;

import com.rapidminer.belt.column.Statistics.Result;
import com.rapidminer.belt.column.Statistics.Statistic;
import com.rapidminer.belt.reader.ObjectReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.LegacyType;
import com.rapidminer.belt.table.Table;
import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.viewer.metadata.BeltColumnStatisticsPanel;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;


/**
 * Model for {@link BeltColumnStatisticsPanel}s which are backed by a date_time
 * {@link com.rapidminer.belt.column.Column}.
 *
 * @author Marco Boeck, Gisa Meier
 * @since 9.7.0
 */
public class BeltDateTimeColumnStatisticsModel extends AbstractBeltColumnStatisticsModel {

	private static final double SECONDS_TO_MILLIS = 1000.0;
	private static final double MILLIS_TO_NANOS = 1_000_000.0;

	/** the index for the histogram chart */
	private static final int INDEX_HISTOGRAM_CHART = 0;

	/** the max number of bins */
	private static final int MAX_BINS_HISTOGRAM = 10;

	/** used to color the chart background invisible */
	private static final Color COLOR_INVISIBLE = new Color(255, 255, 255, 0);

	/** @{value} */
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
	private final DateFormat formatDate = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());

	/** the formatter for date_time values */
	private final DateFormat formatTime = DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.getDefault());

	/** the formatter for date_time values */
	private final DateFormat formatDateTime = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
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
	 * Creates a new {@link BeltDateTimeColumnStatisticsModel}.
	 *
	 * @param table
	 * 		the belt table
	 * @param columnName
	 * 		the name of the column
	 */
	public BeltDateTimeColumnStatisticsModel(final Table table, final String columnName) {
		super(table, columnName);

		chartsArray = new JFreeChart[1];
	}

	@Override
	public void updateStatistics(Map<String, Map<Statistic, Result>> allStatistics) {
		Map<Statistic, Result> statistics = allStatistics.get(getColumnName());
		final String days = WHITESPACE
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.days.label");
		final String hours = WHITESPACE
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.hours.label");

		Instant minObject = statistics.get(Statistic.MIN).getObject(Instant.class);
		Instant maxObject = statistics.get(Statistic.MAX).getObject(Instant.class);
		long minMilliseconds;
		long maxMilliseconds;
		if(minObject !=null && maxObject !=null){
			minMilliseconds = minObject.toEpochMilli();
			maxMilliseconds = maxObject.toEpochMilli();
		}else{
			minMilliseconds = 0;
			maxMilliseconds = 0;
		}

		long difference = maxMilliseconds - minMilliseconds;

		String dura = "";
		LegacyType legacyType = getTableOrNull().getFirstMetaData(getColumnName(), LegacyType.class);
		if (legacyType == LegacyType.DATE) {
			// days
			dura += com.rapidminer.tools.Tools.formatIntegerIfPossible(
					Math.floor(difference / (H_IN_D * M_IN_H * S_IN_M * MS_IN_S)), 3)
					+ days;
		} else if (legacyType == LegacyType.TIME) {
			// hours
			dura += com.rapidminer.tools.Tools.formatIntegerIfPossible(Math.floor(difference / (M_IN_H * S_IN_M * MS_IN_S)),
					3) + hours;
		} else {
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
		if (legacyType == LegacyType.DATE) {
			minResult = formatDate.format(new Date(minMilliseconds));
			maxResult = formatDate.format(new Date(maxMilliseconds));
		} else if (legacyType == LegacyType.TIME) {
			minResult = formatTime.format(new Date(minMilliseconds));
			maxResult = formatTime.format(new Date(maxMilliseconds));
		} else {
			minResult = formatDateTime.format(new Date(minMilliseconds));
			maxResult = formatDateTime.format(new Date(maxMilliseconds));
		}
		missing = getTableOrNull().height() - statistics.get(Statistic.COUNT).getNumeric();
		from = minResult;
		until = maxResult;
		duration = dura;

		fireStatisticsChangedEvent();
	}

	@Override
	public Type getType() {
		return Type.DATETIME;
	}

	/**
	 * Gets the duration of the date_time values.
	 *
	 * @return the duration
	 */
	public String getDuration() {
		return duration;
	}

	/**
	 * Gets the from value (min) of the date_time values.
	 *
	 * @return the min value
	 */
	public String getFrom() {
		return from;
	}

	/**
	 * Gets the until value (max) of the date_time values.
	 *
	 * @return the max value
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
	 * Creates a {@link HistogramDataset} for this {@link com.rapidminer.belt.column.Column}.
	 */
	private HistogramDataset createHistogramDataset(final Table table) {
		HistogramDataset dataset = new HistogramDataset();

		double[] array = new double[table.height()];
		int count = 0;

		ObjectReader<Instant> reader = Readers.objectReader(table.column(getColumnName()),
				Instant.class);
		while(reader.hasRemaining()){
			Instant instant = reader.read();
			// don't use missing values because otherwise JFreeChart tries to plot them too which
			// can lead to false histograms
			if (instant !=null) {
				array[count++] = instant.getEpochSecond() * SECONDS_TO_MILLIS + instant.getNano() / MILLIS_TO_NANOS;
			}
		}

		// add points to data set (if any)
		if (count > 0) {
			// truncate array if necessary
			if (count < array.length) {
				array = Arrays.copyOf(array, count);
			}
			dataset.addSeries(getColumnName(), array, Math.min(array.length, MAX_BINS_HISTOGRAM));
		}

		return dataset;
	}

	/**
	 * Creates the histogram chart.
	 */
	private JFreeChart createHistogramChart(final Table table) {
		JFreeChart chart = ChartFactory.createHistogram(null, null, null, createHistogramDataset(table),
				PlotOrientation.VERTICAL, false, false, false);
		setDefaultChartFonts(chart);
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
		if (chartsArray[INDEX_HISTOGRAM_CHART] == null && getTableOrNull() != null) {
			chartsArray[INDEX_HISTOGRAM_CHART] = createHistogramChart(getTableOrNull());
		}
	}

}
