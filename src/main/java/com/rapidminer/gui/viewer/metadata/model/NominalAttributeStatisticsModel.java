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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.viewer.metadata.AttributeStatisticsPanel;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.ValueAndCount;

import java.awt.Color;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;


/**
 * Model for {@link AttributeStatisticsPanel}s which are backed by a nominal {@link Attribute}.
 * 
 * @author Marco Boeck
 * 
 */
public class NominalAttributeStatisticsModel extends AbstractAttributeStatisticsModel {

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
	 * Creates a new {@link NominalAttributeStatisticsModel}.
	 * 
	 * @param exampleSet
	 * @param attribute
	 */
	public NominalAttributeStatisticsModel(ExampleSet exampleSet, Attribute attribute) {
		super(exampleSet, attribute);

		nominalValues = new LinkedList<>();
		chartsArray = new JFreeChart[1];
	}

	@Override
	public void updateStatistics(ExampleSet exampleSet) {
		missing = exampleSet.getStatistics(getAttribute(), Statistics.UNKNOWN);

		// count nominal values
		int totalNumberOfValues = getAttribute().getMapping().size();
		if (totalNumberOfValues > 0) {
			// create a list of all nominal values and their corresponding count
			Iterator<String> i = getAttribute().getMapping().getValues().iterator();
			nominalValues.clear();
			while (i.hasNext()) {
				String value = i.next();
				if(value!=null) {
					nominalValues.add(new ValueAndCount(value,
							(int) exampleSet.getStatistics(getAttribute(), Statistics.COUNT,
									value)));
				}
			}
			Collections.sort(nominalValues);
		}

		fireStatisticsChangedEvent();
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
	 * @return
	 */
	public List<ValueAndCount> getNominalValuesAndCount() {
		return new LinkedList<>(nominalValues);
	}

	/**
	 * Returns a {@link List} of all value {@link String}s which should be shown.
	 * 
	 * @return
	 */
	public List<String> getValueStrings() {
		List<String> list = new LinkedList<>();

		int maxDisplayValues = !isEnlarged() ? DEFAULT_MAX_DISPLAYED_VALUES : DEFAULT_MAX_DISPLAYED_VALUES_ENLARGED;
		Iterator<ValueAndCount> it = new LinkedList<>(nominalValues).iterator();
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
	 * @return
	 */
	public String getLeast() {
		if (nominalValues.size() == 0) {
			return "";
		}
		ValueAndCount value = nominalValues.get(nominalValues.size() - 1);
		return value.getValue() + PARANTHESIS_OPEN + value.getCount() + PARENTHESIS_CLOSE;
	}

	/**
	 * Returns the {@link String} value which appears the most in the nominal mapping.
	 * 
	 * @return
	 */
	public String getMost() {
		if (nominalValues.size() == 0) {
			return "";
		}
		ValueAndCount value = nominalValues.get(0);
		return value.getValue() + PARANTHESIS_OPEN + value.getCount() + PARENTHESIS_CLOSE;
	}

	/**
	 * Creates the histogram chart.
	 * 
	 * @return
	 */
	private JFreeChart createBarChart() {
		JFreeChart chart = ChartFactory.createBarChart(null, null, null, createBarDataset(), PlotOrientation.VERTICAL,
				false, false, false);
		AbstractAttributeStatisticsModel.setDefaultChartFonts(chart);
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
	 * Creates a {@link CategoryDataset} for this {@link Attribute}.
	 * 
	 * @param exampleSet
	 * @return
	 */
	private CategoryDataset createBarDataset() {
		Iterator<ValueAndCount> it = new LinkedList<>(nominalValues).iterator();
		LinkedList<ValueAndCount> listOfBarValues = new LinkedList<>();
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
