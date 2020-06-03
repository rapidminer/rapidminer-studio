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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Dictionary;
import com.rapidminer.belt.column.Statistics;
import com.rapidminer.belt.column.Statistics.Result;
import com.rapidminer.belt.column.Statistics.Statistic;
import com.rapidminer.belt.table.Table;
import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.viewer.metadata.BeltColumnStatisticsPanel;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.ValueAndCount;


/**
 * Model for {@link BeltColumnStatisticsPanel}s which are backed by a nominal {@link Column}.
 * 
 * @author Marco Boeck, Gisa Meier
 * @since 9.7.0
 */
public class BeltNominalColumnStatisticsModel extends AbstractBeltColumnStatisticsModel {

	/** the index for the barchart */
	private static final int INDEX_BAR_CHART = 0;

	/** the max number of bars in the barchart */
	private static final int MAX_BARS = 5;

	/** used to color the chart background invisible */
	private static final Color COLOR_INVISIBLE = new Color(255, 255, 255, 0);

	/** the default number of displayed nominal values when NOT enlarged */
	private static final int DEFAULT_MAX_DISPLAYED_VALUES = 2;

	/** the default number of displayed nominal values when enlarged */
	public static final int DEFAULT_MAX_DISPLAYED_VALUES_ENLARGED = 4;

	/** {@value} */
	private static final String LABEL_DOTS = "...";

	/** {@value} */
	private static final String PARENTHESIS_CLOSE = ")";

	/** {@value} */
	private static final String PARANTHESIS_OPEN = " (";

	/** {@value} */
	private static final String BRACKET_OPEN = " [";

	/** {@value} */
	private static final String BRACKET_CLOSE = "]";

	/** the list containing the nominal values and their count */
	private List<ValueAndCount> nominalValues;

	/** array of charts for this model */
	private JFreeChart[] chartsArray;

	/**
	 * Creates a new {@link BeltNominalColumnStatisticsModel}.
	 */
	public BeltNominalColumnStatisticsModel(Table table, String columnName) {
		super(table, columnName);

		nominalValues = new ArrayList<>();
		chartsArray = new JFreeChart[1];
	}

	@Override
	public void updateStatistics(Map<String, Map<Statistic, Result>> allStatistics) {
		Map<Statistic, Result> statistics = allStatistics.get(getColumnName());
		missing = getTableOrNull().height() - statistics.get(Statistic.COUNT).getNumeric();
		Statistics.CategoricalIndexCounts counts = statistics.get(Statistics.Statistic.INDEX_COUNTS)
				.getObject(Statistics.CategoricalIndexCounts.class);
		// count nominal values
		Dictionary dictionary = getTableOrNull().column(getColumnName()).getDictionary();
		int totalNumberOfValues = dictionary.size();
		if (totalNumberOfValues > 0) {
			// create a list of all nominal values and their corresponding count
			Iterator<Dictionary.Entry> i = dictionary.iterator();
			nominalValues.clear();
			while (i.hasNext()) {
				Dictionary.Entry value = i.next();
				if (value != null) {
					nominalValues.add(new ValueAndCount(value.getValue(), counts.countForIndex(value.getIndex())));
				}
			}
			Collections.sort(nominalValues);
		}

		fireStatisticsChangedEvent();
	}

	@Override
	public Type getType() {
		Column column = getTableOrNull().column(getColumnName());
		if(column.type().id() == Column.TypeId.NOMINAL && column.getDictionary().isBoolean()){
			return Type.BINOMINAL;
		}
		return Type.NOMINAL;
	}

	@Override
	public JFreeChart getChartOrNull(int index) {
		prepareCharts();
		if (index == INDEX_BAR_CHART) {
			return chartsArray[index];
		}

		return null;
	}

	@Override
	public void prepareCharts() {
		if (chartsArray[INDEX_BAR_CHART] == null) {
			chartsArray[INDEX_BAR_CHART] = createBarChart();
		}
	}

	/**
	 * Returns a {@link List} of nominal values and their count.
	 * 
	 * @return the nominal values and their count
	 */
	public List<ValueAndCount> getNominalValuesAndCount() {
		return new ArrayList<>(nominalValues);
	}

	/**
	 * Returns a {@link List} of all value {@link String}s which should be shown.
	 * 
	 * @return all values
	 */
	public List<String> getValueStrings() {
		List<String> list = new ArrayList<>();

		int maxDisplayValues = !isEnlarged() ? DEFAULT_MAX_DISPLAYED_VALUES : DEFAULT_MAX_DISPLAYED_VALUES_ENLARGED;
		Iterator<ValueAndCount> it = new ArrayList<>(nominalValues).iterator();
		int n = 0;
		while (it.hasNext() && n < maxDisplayValues) {
			ValueAndCount value = it.next();
			n++;
			list.add(value.getValue() + PARANTHESIS_OPEN + value.getCount() + PARENTHESIS_CLOSE);
		}
		// count how many we could not display
		int omittedCount = 0;
		while (it.hasNext()) {
			it.next();
			omittedCount++;
		}
		if (omittedCount > 0) {
			list.add(LABEL_DOTS + BRACKET_OPEN + omittedCount + " "
					+ I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.values_more")
					+ BRACKET_CLOSE);
		}

		return list;
	}

	/**
	 * Returns the {@link String} value which appears the least in the nominal mapping.
	 * 
	 * @return the least appearing value
	 */
	public String getLeast() {
		if (nominalValues.isEmpty()) {
			return "";
		}
		ValueAndCount value = nominalValues.get(nominalValues.size() - 1);
		return value.getValue() + PARANTHESIS_OPEN + value.getCount() + PARENTHESIS_CLOSE;
	}

	/**
	 * Returns the {@link String} value which appears the most in the nominal mapping.
	 * 
	 * @return the most appearing value
	 */
	public String getMost() {
		if (nominalValues.isEmpty()) {
			return "";
		}
		ValueAndCount value = nominalValues.get(0);
		return value.getValue() + PARANTHESIS_OPEN + value.getCount() + PARENTHESIS_CLOSE;
	}

	/**
	 * The positive value in case of a boolean dictionary.
	 *
	 * @return the positive value or empty String
	 */
	public String getPositive(){
		Dictionary dictionary = getTableOrNull().column(getColumnName()).getDictionary();
		String positiveString = dictionary.get(dictionary.getPositiveIndex());
		return positiveString == null ? "" : positiveString;
	}

	/**
	 * The negative value in case of a boolean dictionary.
	 *
	 * @return the negative value or empty String
	 */
	public String getNegative(){
		Dictionary dictionary = getTableOrNull().column(getColumnName()).getDictionary();
		String negativeString = dictionary.get(dictionary.getNegativeIndex());
		return negativeString == null ? "" : negativeString;
	}

	/**
	 * Creates the histogram chart.
	 */
	private JFreeChart createBarChart() {
		JFreeChart chart = ChartFactory.createBarChart(null, null, null, createBarDataset(), PlotOrientation.VERTICAL,
				false, false, false);
		setDefaultChartFonts(chart);
		chart.setBackgroundPaint(null);
		chart.setBackgroundImageAlpha(0.0f);

		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setRangeGridlinesVisible(false);
		plot.setDomainGridlinesVisible(false);
		plot.setOutlineVisible(false);
		plot.setRangeZeroBaselineVisible(false);
		plot.setDomainGridlinesVisible(false);
		plot.setBackgroundPaint(COLOR_INVISIBLE);
		plot.setBackgroundImageAlpha(0.0f);

		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setSeriesPaint(0, AttributeGuiTools.getColorForValueType(Ontology.NOMINAL));
		renderer.setBarPainter(new StandardBarPainter());
		renderer.setDrawBarOutline(true);
		renderer.setShadowVisible(false);

		return chart;
	}

	/**
	 * Creates a {@link CategoryDataset} for this column.
	 */
	private CategoryDataset createBarDataset() {
		Iterator<ValueAndCount> it = new ArrayList<>(nominalValues).iterator();
		List<ValueAndCount> listOfBarValues = new ArrayList<>();
		int n = 0;
		while (it.hasNext() && n < MAX_BARS) {
			ValueAndCount value = it.next();
			n++;
			listOfBarValues.add(value);
		}

		// fill dataset with top 5 string values (if possible)
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (ValueAndCount value : listOfBarValues) {
			dataset.addValue(value.getCount(), "", value.getValue());
		}

		return dataset;
	}

}
