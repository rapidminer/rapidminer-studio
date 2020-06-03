/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
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
import com.rapidminer.belt.table.Table;
import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.viewer.metadata.BeltColumnStatisticsPanel;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * Model for {@link BeltColumnStatisticsPanel}s which are backed by a time {@link com.rapidminer.belt.column.Column}.
 *
 * @author Marco Boeck, Gisa Meier
 * @since 9.7.0
 */
public class BeltTimeColumnStatisticsModel extends AbstractBeltColumnStatisticsModel {

	private static final LocalDate EPOCH = LocalDate.ofEpochDay(0);
	private static final double SECONDS_TO_MILLIS = 1000.0;
	private static final double MILLIS_TO_NANOS = 1_000_000.0;

	/** the index for the histogram chart */
	private static final int INDEX_HISTOGRAM_CHART = 0;

	/** the max number of bins */
	private static final int MAX_BINS_HISTOGRAM = 10;

	/** used to color the chart background invisible */
	private static final Color COLOR_INVISIBLE = new Color(255, 255, 255, 0);

	/** the average of the numerical values */
	private LocalTime average;

	/** the standard deviation of the numerical values */
	private LocalTime deviation;

	/** the minimum of the numerical values */
	private LocalTime minimum;

	/** the maximum of the numerical values */
	private LocalTime maximum;

	/** array of charts for this model */
	private JFreeChart[] chartsArray;

	/**
	 * Creates a new {@link BeltTimeColumnStatisticsModel}.
	 */
	public BeltTimeColumnStatisticsModel(Table table, String attribute) {
		super(table, attribute);

		chartsArray = new JFreeChart[1];
	}

	@Override
	public void updateStatistics(Map<String, Map<Statistic, Result>> allStatistics) {
		Map<Statistic, Result> statistics = allStatistics.get(getColumnName());
		average = statistics.get(Statistic.MEAN).getObject(LocalTime.class);
		deviation = statistics.get(Statistic.SD).getObject(LocalTime.class);
		minimum = statistics.get(Statistic.MIN).getObject(LocalTime.class);
		maximum = statistics.get(Statistic.MAX).getObject(LocalTime.class);
		missing = getTableOrNull().height() - statistics.get(Statistic.COUNT).getNumeric();

		fireStatisticsChangedEvent();
	}

	@Override
	public Type getType() {
		return Type.TIME;
	}

	/**
	 * Gets the average of the numerical values.
	 *
	 * @return the average
	 */
	public LocalTime getAverage() {
		return average;
	}

	/**
	 * Gets the standard deviation of the numerical values.
	 *
	 * @return the standard deviation
	 */
	public LocalTime getDeviation() {
		return deviation;
	}

	/**
	 * Gets the minimum of the numerical values.
	 *
	 * @return the minimum of the numerical values
	 */
	public LocalTime getMinimum() {
		return minimum;
	}

	/**
	 * Gets the maximum of the numerical values.
	 *
	 * @return the maximum of the numerical values
	 */
	public LocalTime getMaximum() {
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
	 * Creates a {@link HistogramDataset} for this column.
	 */
	private HistogramDataset createHistogramDataset(final Table table) {
		HistogramDataset dataset = new HistogramDataset();

		double[] array = new double[table.height()];
		int count = 0;

		ObjectReader<LocalTime> reader = Readers.objectReader(table.column(getColumnName()),
				LocalTime.class);
		while (reader.hasRemaining()) {
			LocalTime localTime = reader.read();
			// don't use missing values because otherwise JFreeChart tries to plot them too which
			// can lead to false histograms
			if (localTime != null) {
 				//charts can plot only as milliseconds since epoch, so convert
				Instant instant = localTime.atDate(EPOCH).atZone(Tools.getPreferredTimeZone().toZoneId()).toInstant();
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
