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
package com.rapidminer.gui.new_plotter.engine.jfreechart;

import com.rapidminer.gui.new_plotter.ChartPlottimeException;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.DomainConfigManager;
import com.rapidminer.gui.new_plotter.configuration.LinkAndBrushMaster;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.StackingMode;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.configuration.ValueSource.SeriesUsageType;
import com.rapidminer.gui.new_plotter.data.DomainConfigManagerData;
import com.rapidminer.gui.new_plotter.data.GroupCellData;
import com.rapidminer.gui.new_plotter.data.GroupCellKeyAndData;
import com.rapidminer.gui.new_plotter.data.GroupCellSeriesData;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.data.RangeAxisData;
import com.rapidminer.gui.new_plotter.data.ValueSourceData;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.axis.CustomDateAxis;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.axis.CustomLogarithmicAxis;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.axis.CustomNumberAxis;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.axis.CustomSymbolAxis;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.axis.LinkAndBrushAxis;
import com.rapidminer.gui.new_plotter.utility.ValueRange;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.container.Pair;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.Range;


/**
 * A class which creates a JFreeChart range axis for a given {@link RangeAxisConfig} or a domain
 * axis for a {@link DimensionConfig}.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ChartAxisFactory {

	/**
	 * Private ctor - pure static class
	 */
	private ChartAxisFactory() {}

	public static ValueAxis createRangeAxis(RangeAxisConfig rangeAxisConfig, PlotInstance plotInstance)
			throws ChartPlottimeException {
		if (rangeAxisConfig.getValueType() == ValueType.UNKNOWN || rangeAxisConfig.getValueType() == ValueType.INVALID) {
			return null;
		} else {
			RangeAxisData rangeAxisData = plotInstance.getPlotData().getRangeAxisData(rangeAxisConfig);

			double initialUpperBound = rangeAxisData.getUpperViewBound();
			double initialLowerBound = rangeAxisData.getLowerViewBound();

			double upperBound = initialUpperBound;
			double lowerBound = initialLowerBound;

			// fetch old zooming
			LinkAndBrushMaster linkAndBrushMaster = plotInstance.getMasterPlotConfiguration().getLinkAndBrushMaster();
			Range axisZoom = linkAndBrushMaster.getRangeAxisZoom(rangeAxisConfig,
					plotInstance.getCurrentPlotConfigurationClone());

			List<ValueSource> valueSources = rangeAxisConfig.getValueSources();
			if (rangeAxisConfig.hasAbsolutStackedPlot()) {
				for (ValueSource valueSource : valueSources) {
					VisualizationType seriesType = valueSource.getSeriesFormat().getSeriesType();
					if (seriesType == VisualizationType.BARS || seriesType == VisualizationType.AREA) {
						if (valueSource.getSeriesFormat().getStackingMode() == StackingMode.ABSOLUTE) {
							Pair<Double, Double> minMax = calculateUpperAndLowerBounds(valueSource, plotInstance);
							if (upperBound < minMax.getSecond()) {
								upperBound = minMax.getSecond();
							}
							if (lowerBound > minMax.getFirst()) {
								lowerBound = minMax.getFirst();
							}
						}
					}
				}
			}

			double margin = upperBound - lowerBound;
			if (lowerBound == upperBound) {
				margin = lowerBound;
			}
			if (margin == 0) {
				margin = 0.1;
			}

			double normalPad = RangeAxisConfig.padFactor * margin;

			if (rangeAxisConfig.isLogarithmicAxis()) {
				if (!rangeAxisConfig.isUsingUserDefinedLowerViewBound()) {
					lowerBound -= RangeAxisConfig.logPadFactor * lowerBound;
				}
				if (!rangeAxisConfig.isUsingUserDefinedUpperViewBound()) {
					upperBound += RangeAxisConfig.logPadFactor * upperBound;
				}
			} else {
				// add margin
				if (!rangeAxisConfig.isUsingUserDefinedLowerViewBound()) {
					lowerBound -= normalPad;
				}
				if (!rangeAxisConfig.isUsingUserDefinedUpperViewBound()) {
					upperBound += normalPad;
				}
			}

			boolean includeZero = false;
			if (isIncludingZero(rangeAxisConfig, initialLowerBound) && !rangeAxisConfig.isUsingUserDefinedLowerViewBound()) {
				// if so set lower bound to zero
				lowerBound = 0.0;
				includeZero = true;
			}

			boolean upToOne = false;
			// if there are only relative plots set upper Bound to 1.0
			if (rangeAxisConfig.mustHaveUpperBoundOne(initialUpperBound)
					&& !rangeAxisConfig.isUsingUserDefinedUpperViewBound()) {
				upperBound = 1.0;
				upToOne = true;

			}

			if (includeZero && !upToOne) {
				upperBound *= 1.05;
			}

			String label = rangeAxisConfig.getLabel();
			if (label == null) {
				label = I18N.getGUILabel("plotter.unnamed_value_label");
			}

			ValueAxis rangeAxis;

			if (rangeAxisConfig.getValueType() == ValueType.NOMINAL && !valueSources.isEmpty()) {
				// get union of distinct values of all plotValueConfigs on range axis
				int maxValue = Integer.MIN_VALUE;
				for (ValueSource valueSource : rangeAxisData.getRangeAxisConfig().getValueSources()) {
					ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
					double maxValueInSource = valueSourceData.getMaxValue();
					if (maxValueInSource > maxValue) {
						maxValue = (int) maxValueInSource;
					}
				}
				Vector<String> yValueStrings = new Vector<String>(maxValue);
				yValueStrings.setSize(maxValue + 1);
				ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSources.get(0));

				for (int i = 0; i <= maxValue; ++i) {
					yValueStrings.set(i, valueSourceData.getStringForValue(SeriesUsageType.MAIN_SERIES, i));
				}
				String[] yValueStringArray = new String[yValueStrings.size()];
				int i = 0;
				for (String s : yValueStrings) {
					yValueStringArray[i] = s;
					++i;
				}
				CustomSymbolAxis symbolRangeAxis = new CustomSymbolAxis(null, yValueStringArray);
				symbolRangeAxis.setVisible(true);
				symbolRangeAxis.setAutoRangeIncludesZero(false);

				symbolRangeAxis.saveUpperBound(upperBound, initialUpperBound);
				symbolRangeAxis.saveLowerBound(lowerBound, initialLowerBound);

				symbolRangeAxis.setLabel(label);
				Font axesFont = plotInstance.getCurrentPlotConfigurationClone().getAxesFont();
				if (axesFont != null) {
					symbolRangeAxis.setLabelFont(axesFont);
					symbolRangeAxis.setTickLabelFont(axesFont);
				}

				// set range if axis has been zoomed before
				if (axisZoom != null) {
					symbolRangeAxis.setRange(axisZoom);
				}
				rangeAxis = symbolRangeAxis;
			} else if (rangeAxisConfig.getValueType() == ValueType.NUMERICAL) {
				NumberAxis numberRangeAxis;
				if (rangeAxisConfig.isLogarithmicAxis()) {
					if (rangeAxisData.getMinYValue() <= 0) {
						throw new ChartPlottimeException("log_axis_contains_zero", label);
					}
					numberRangeAxis = new CustomLogarithmicAxis(null);
					((CustomLogarithmicAxis) numberRangeAxis).saveUpperBound(upperBound, initialUpperBound);
					((CustomLogarithmicAxis) numberRangeAxis).saveLowerBound(lowerBound, initialLowerBound);
				} else {
					numberRangeAxis = new CustomNumberAxis();
					((CustomNumberAxis) numberRangeAxis).saveUpperBound(upperBound, initialUpperBound);
					((CustomNumberAxis) numberRangeAxis).saveLowerBound(lowerBound, initialLowerBound);
				}

				numberRangeAxis.setAutoRangeIncludesZero(false);

				numberRangeAxis.setVisible(true);
				numberRangeAxis.setLabel(label);
				Font axesFont = plotInstance.getCurrentPlotConfigurationClone().getAxesFont();
				if (axesFont != null) {
					numberRangeAxis.setLabelFont(axesFont);
					numberRangeAxis.setTickLabelFont(axesFont);
				}

				// set range if axis has been zoomed before
				if (axisZoom != null) {
					numberRangeAxis.setRange(axisZoom);
				}

				rangeAxis = numberRangeAxis;
			} else if (rangeAxisConfig.getValueType() == ValueType.DATE_TIME) {
				CustomDateAxis dateRangeAxis;
				if (rangeAxisConfig.isLogarithmicAxis()) {
					throw new ChartPlottimeException("logarithmic_not_supported_for_value_type", label, ValueType.DATE_TIME);
				} else {
					dateRangeAxis = new CustomDateAxis();
				}

				dateRangeAxis.saveUpperBound(upperBound, initialUpperBound);
				dateRangeAxis.saveLowerBound(lowerBound, initialLowerBound);

				dateRangeAxis.setVisible(true);
				dateRangeAxis.setLabel(label);
				Font axesFont = plotInstance.getCurrentPlotConfigurationClone().getAxesFont();
				if (axesFont != null) {
					dateRangeAxis.setLabelFont(axesFont);
				}

				// set range if axis has been zoomed before
				if (axisZoom != null) {
					dateRangeAxis.setRange(axisZoom);
				}

				rangeAxis = dateRangeAxis;
			} else {
				throw new RuntimeException("Unknown value type. This should not happen");
			}

			// configure format
			formatAxis(plotInstance.getCurrentPlotConfigurationClone(), rangeAxis);
			return rangeAxis;
		}
	}

	private static Pair<Double, Double> calculateUpperAndLowerBounds(ValueSource valueSource, PlotInstance plotInstance) {

		Pair<Double, Double> minMax = new Pair<Double, Double>(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);

		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		GroupCellSeriesData dataForAllGroupCells = valueSourceData.getSeriesDataForAllGroupCells();

		Map<Double, Double> stackedYValues = new HashMap<Double, Double>();

		// Loop all group cells and add data to dataset
		for (GroupCellKeyAndData groupCellKeyAndData : dataForAllGroupCells) {
			GroupCellData groupCellData = groupCellKeyAndData.getData();

			Map<PlotDimension, double[]> dataForUsageType = groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES);
			double[] xValues = dataForUsageType.get(PlotDimension.DOMAIN);
			double[] yValues = dataForUsageType.get(PlotDimension.VALUE);
			int rowCount = xValues.length;

			// Loop all rows and add data to series
			for (int row = 0; row < rowCount; ++row) {
				Double x = xValues[row];
				Double stackedYValue = stackedYValues.get(x);
				double d = yValues[row];
				if (!Double.isNaN(d)) {
					if (stackedYValue == null) {
						stackedYValues.put(x, d);
					} else {
						double value = stackedYValue + d;
						stackedYValues.put(x, value);
					}
				}
			}
		}

		for (Double xValue : stackedYValues.keySet()) {
			Double yValue = stackedYValues.get(xValue);
			if (yValue > minMax.getSecond()) {
				minMax.setSecond(yValue);
			}
			if (yValue < minMax.getFirst()) {
				minMax.setFirst(yValue);
			}
		}

		return minMax;
	}

	public static CategoryAxis createCategoryDomainAxis(PlotConfiguration plotConfiguration) {
		CategoryAxis domainAxis = new CategoryAxis(null);
		String label = plotConfiguration.getDomainConfigManager().getLabel();
		if (label == null) {
			label = I18N.getGUILabel("plotter.unnamed_value_label");
		}
		domainAxis.setLabel(label);

		Font axesFont = plotConfiguration.getAxesFont();
		if (axesFont != null) {
			domainAxis.setLabelFont(axesFont);
			domainAxis.setTickLabelFont(axesFont);
		}

		// rotate labels
		if (plotConfiguration.getOrientation() != PlotOrientation.HORIZONTAL) {
			domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 2.0d));
		}

		formatAxis(plotConfiguration, domainAxis);
		return domainAxis;
	}

	private static boolean isIncludingZero(RangeAxisConfig rangeAxisConfig, double initialLowerBound) {

		int includeZero = 0;
		boolean minValueBiggerOrEqualZero = (initialLowerBound >= 0);
		boolean hasRelatives = false;
		boolean hasAreaOrBars = false;

		// if the min value for y is above zero and by adding a pad the lower bound is below zero.
		List<ValueSource> valueSources = rangeAxisConfig.getValueSources();

		for (ValueSource valueSource : valueSources) {
			// check if there are value sources with bar or area series type only

			SeriesFormat seriesFormat = valueSource.getSeriesFormat();
			VisualizationType seriesType = seriesFormat.getSeriesType();
			if (seriesType == VisualizationType.BARS || seriesType == VisualizationType.AREA) {
				hasAreaOrBars = true;
			}

		}

		if (!hasAreaOrBars) {
			return false;
		}

		// iterate over all value sources
		for (ValueSource valueSource : valueSources) {
			SeriesFormat seriesFormat = valueSource.getSeriesFormat();
			VisualizationType seriesType = seriesFormat.getSeriesType();
			if (seriesType == VisualizationType.BARS || seriesType == VisualizationType.AREA) {

				if (!hasRelatives) {
					hasRelatives = (seriesFormat.getStackingMode() == StackingMode.RELATIVE);
				}
				if (minValueBiggerOrEqualZero) {
					++includeZero;
				}
			} else if (minValueBiggerOrEqualZero) {
				++includeZero;
			}
		}

		boolean allSourcesAreBarsOrArea = (includeZero == valueSources.size());
		boolean containsRelativeAndMinBiggerZero = hasRelatives && minValueBiggerOrEqualZero;

		return (allSourcesAreBarsOrArea || containsRelativeAndMinBiggerZero);
	}

	private static ValueAxis createNumberOrDateDomainAxis(PlotInstance plotInstance, boolean date)
			throws ChartPlottimeException {
		PlotConfiguration plotConfiguration = plotInstance.getCurrentPlotConfigurationClone();
		DomainConfigManager domainConfigManager = plotConfiguration.getDomainConfigManager();
		DomainConfigManagerData domainConfigManagerData = plotInstance.getPlotData().getDomainConfigManagerData();
		DimensionConfig domainConfig;
		domainConfig = domainConfigManager.getDomainConfig(false);

		if (domainConfig.isNominal()) {
			throw new IllegalArgumentException("Cannot create nominal domain axis from numerical domain config.");
		}

		ValueRange range = domainConfigManagerData.getEffectiveRange();

		double upperBound = range.getUpperBound();
		double lowerBound = range.getLowerBound();
		double lowerBoundWithMargin = lowerBound;
		double upperBoundWithMargin = upperBound;

		// fetch old zooming
		LinkAndBrushMaster linkAndBrushMaster = plotInstance.getMasterPlotConfiguration().getLinkAndBrushMaster();
		Range domainZoom = linkAndBrushMaster.getDomainZoom();

		ValueAxis domainAxis;
		boolean logarithmic = false;
		if (domainConfigManager.isLogarithmicDomainAxis()) {
			logarithmic = true;
			if (date) {
				// disable logarithmic scale for dates.
				// Yes, a warning should be created, see DefaultDimensionConfig.getWarnings() for
				// this.
				logarithmic = false;
			}
		}
		if (logarithmic) {
			if (range.getLowerBound() <= 0) {
				throw new ChartPlottimeException("axis_configuration_error", domainConfigManager.getDimension().getName());
			}
			CustomLogarithmicAxis customLogAxis = new CustomLogarithmicAxis(null);

			if (!domainConfigManager.isUsingUserDefinedLowerBound()) {
				lowerBoundWithMargin = lowerBound - (0.15 * lowerBound);
			}
			if (!domainConfigManager.isUsingUserDefinedUpperBound()) {
				upperBoundWithMargin = upperBound + (0.15 * upperBound);
			}

			// apply domain dimension range
			customLogAxis.saveUpperBound(upperBoundWithMargin, upperBound);
			customLogAxis.saveLowerBound(lowerBoundWithMargin, lowerBound);

			domainAxis = customLogAxis;
		} else {
			LinkAndBrushAxis linkAndBrushAxis;
			if (date) {
				CustomDateAxis dateAxis = new CustomDateAxis(null);
				if (domainConfig.isUsingUserDefinedDateFormat()) {
					dateAxis.setDateFormatOverride(domainConfig.getDateFormat());
					dateAxis.setTimeZone(TimeZone.getTimeZone("GMT"));
				}
				linkAndBrushAxis = dateAxis;

			} else {
				linkAndBrushAxis = new CustomNumberAxis(null);
				((NumberAxis) linkAndBrushAxis).setAutoRangeIncludesZero(false);
			}
			domainAxis = (ValueAxis) linkAndBrushAxis;

			// add margin
			double margin = upperBound - lowerBound;
			double pad = 0.04 * margin;
			if (!domainConfigManager.isUsingUserDefinedLowerBound()) {
				lowerBoundWithMargin = lowerBound - pad;
			}
			if (!domainConfigManager.isUsingUserDefinedUpperBound()) {
				upperBoundWithMargin = upperBound + pad;
			}

			// apply domain dimension range
			linkAndBrushAxis.saveUpperBound(upperBoundWithMargin, upperBound);
			linkAndBrushAxis.saveLowerBound(lowerBoundWithMargin, lowerBound);

		}

		// apply old zoom if axis has been zoomed before
		if (domainZoom != null) {
			domainAxis.setRange(domainZoom);
		}

		// domainAxis.setAutoRange(true);

		String label = domainConfigManager.getLabel();
		if (label == null) {
			label = I18N.getGUILabel("plotter.unnamed_value_label");
		}
		domainAxis.setLabel(label);
		Font axesFont = plotConfiguration.getAxesFont();
		if (axesFont != null) {
			domainAxis.setLabelFont(axesFont);
			domainAxis.setTickLabelFont(axesFont);
		}

		formatAxis(plotConfiguration, domainAxis);
		return domainAxis;
	}

	public static void formatAxis(PlotConfiguration plotConfiguration, Axis axis) {
		Color axisColor = plotConfiguration.getAxisLineColor();
		if (axis != null) {
			axis.setAxisLinePaint(axisColor);
			axis.setAxisLineStroke(new BasicStroke(plotConfiguration.getAxisLineWidth()));
			axis.setLabelPaint(axisColor);
			axis.setTickLabelPaint(axisColor);
		}
	}

	public static ValueAxis createNumericalDomainAxis(PlotInstance plotInstance) throws ChartPlottimeException {
		return createNumberOrDateDomainAxis(plotInstance, false);
	}

	public static ValueAxis createDateDomainAxis(PlotInstance plotInstance) throws ChartPlottimeException {
		return createNumberOrDateDomainAxis(plotInstance, true);
	}
}
