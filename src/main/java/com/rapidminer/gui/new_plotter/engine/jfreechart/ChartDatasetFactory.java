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
import com.rapidminer.gui.new_plotter.configuration.DefaultDimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.GroupCellKey;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.IndicatorType;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.configuration.ValueSource.SeriesUsageType;
import com.rapidminer.gui.new_plotter.data.DimensionConfigData;
import com.rapidminer.gui.new_plotter.data.GroupCellData;
import com.rapidminer.gui.new_plotter.data.GroupCellKeyAndData;
import com.rapidminer.gui.new_plotter.data.GroupCellSeriesData;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.data.ValueSourceData;
import com.rapidminer.gui.new_plotter.utility.ValueRange;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.container.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.SeriesException;
import org.jfree.data.statistics.DefaultMultiValueCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.data.xy.DefaultIntervalXYDataset;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


/**
 * Helper class for converting data delivered by {@link ValueSource}s into datasets usable by
 * JFreeChart.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class ChartDatasetFactory {

	/**
	 * private ctor, cause this is a static-only class
	 */
	private ChartDatasetFactory() {}

	/**
	 * @param valueSource
	 * @param plotInstance
	 * @param autoWidthFraction
	 *            If this value is greater than 0, an auto width for the intervals is calculated
	 *            such that the intervals nearest to each other touch. This value is then multiplied
	 *            with the value of autoWidthFtraction. If unset, the intervals have width 0.
	 * @param allowDuplicates
	 * @param sortByDomain
	 *            if true, the data is sorted by domain values (useful for bar and area charts)
	 * @return
	 * @throws ChartPlottimeException
	 */
	public static XYSeriesCollection createXYSeriesCollection(ValueSource valueSource, PlotInstance plotInstance,
			double autoWidthFraction, boolean allowDuplicates, boolean sortByDomain) throws ChartPlottimeException {
		XYSeriesCollection xyDataset = new XYSeriesCollection();
		if (autoWidthFraction > 0) {
			xyDataset.setAutoWidth(true);
		} else {
			xyDataset.setAutoWidth(false);
			xyDataset.setIntervalWidth(0);
		}

		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		assertMaxValueCountNotExceededOrThrowException(valueSourceData);
		GroupCellSeriesData dataForAllGroupCells = valueSourceData.getSeriesDataForAllGroupCells();

		// Loop over group cells and add data to dataset
		for (GroupCellKeyAndData groupCellKeyAndData : dataForAllGroupCells) {
			GroupCellKey groupCellKey = groupCellKeyAndData.getKey();
			GroupCellData groupCellData = groupCellKeyAndData.getData();

			String seriesName = generateSeriesName(valueSource, groupCellKey,
					plotInstance.getCurrentPlotConfigurationClone());

			XYSeries series = new XYSeries(seriesName, sortByDomain, allowDuplicates);
			Map<PlotDimension, double[]> dataForUsageType = groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES);
			int rowCount = dataForUsageType.get(PlotDimension.DOMAIN).length;
			double[] xValues = dataForUsageType.get(PlotDimension.DOMAIN);
			double[] yValues = dataForUsageType.get(PlotDimension.VALUE);

			try {
				// Loop over rows and add data to series
				for (int row = 0; row < rowCount; ++row) {
					double x = xValues[row];
					double y = yValues[row];
					if (!Double.isNaN(x)) {
						series.add(x, y);
					}

				}
			} catch (SeriesException e) {
				throw new ChartPlottimeException("duplicate_value", valueSource.toString(), PlotDimension.DOMAIN.getName());
			}

			xyDataset.addSeries(series);
		}
		// intervals should not touch each other, so decrease auto width.
		if (xyDataset.getIntervalWidth() > 0) {
			xyDataset.setIntervalWidth(xyDataset.getIntervalWidth() * autoWidthFraction);
		}
		return xyDataset;
	}

	/**
	 * Creates a dataset which supports custom intervals on both axes.
	 * 
	 * Expects a grouping on the domain axis.
	 * 
	 * @throws ChartPlottimeException
	 */
	public static DefaultIntervalXYDataset createDefaultIntervalXYDataset(ValueSource valueSource,
			PlotInstance plotInstance, boolean createRangeIntervals) throws ChartPlottimeException {
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		assertMaxValueCountNotExceededOrThrowException(valueSourceData);

		GroupCellSeriesData dataForAllGroupCells = valueSourceData.getSeriesDataForAllGroupCells();

		DefaultIntervalXYDataset intervalDataset = new DefaultIntervalXYDataset();

		DefaultDimensionConfig domainConfig = valueSource.getDomainConfig();
		DimensionConfigData domainConfigData = plotInstance.getPlotData().getDimensionConfigData(domainConfig);

		// Loop all group cells and add data to dataset
		for (GroupCellKeyAndData groupCellKeyAndData : dataForAllGroupCells) {

			GroupCellKey groupCellKey = groupCellKeyAndData.getKey();
			GroupCellData groupCellData = groupCellKeyAndData.getData();

			// create series name
			GroupCellKey groupCellKeyClone = (GroupCellKey) groupCellKey.clone();
			groupCellKeyClone.removeRangeForDimension(PlotDimension.DOMAIN);	// legend does not need
																				// X-group
			String seriesName = generateSeriesName(valueSource, groupCellKeyClone,
					plotInstance.getCurrentPlotConfigurationClone());

			List<ValueRange> domainValueGroups = domainConfigData.getGroupingModel();

			// Loop all rows and add data to series.
			// Remember that by definition one row in the groupCellData corresponds
			// to one group in xValueGroups (if the x-axis is grouped, which should
			// always be the case in this function).
			final int domainValueIdx = 0;
			final int domainLowerIdx = 1;
			final int domainUpperIdx = 2;
			final int rangeValueIdx = 3;
			final int rangeLowerIdx = 4;
			final int rangeUpperIdx = 5;
			Map<PlotDimension, double[]> dataForMainSeries = groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES);

			int rowCount = dataForMainSeries.get(PlotDimension.DOMAIN).length;
			double[] domainValues = dataForMainSeries.get(PlotDimension.DOMAIN);
			double[] rangeValues = dataForMainSeries.get(PlotDimension.VALUE);
			double[] upperErrorValues = null;
			double[] lowerErrorValues = null;

			upperErrorValues = valueSourceData.getAbsoluteUtilityValues(groupCellKeyAndData, true);
			lowerErrorValues = valueSourceData.getAbsoluteUtilityValues(groupCellKeyAndData, false);
			if (createRangeIntervals && upperErrorValues == null) {
				throw new ChartPlottimeException("undefined_series", valueSource.toString(), SeriesUsageType.INDICATOR_1);
			}

			double[][] series = new double[6][rowCount];
			Iterator<ValueRange> domainGroupIterator = null;
			if (domainValueGroups != null) {
				domainGroupIterator = domainValueGroups.iterator();
			}

			double domainLower;
			double domainUpper;
			double domainValue;
			double rangeValue;
			double rangeUpper;
			double rangeLower;
			for (int row = 0; row < rowCount; ++row) {
				domainValue = domainValues[row];
				domainLower = domainValue;
				domainUpper = domainValue;
				if (domainGroupIterator != null) {
					ValueRange currentDomainGroup = domainGroupIterator.next();
					if (currentDomainGroup.definesUpperLowerBound()) {
						domainLower = currentDomainGroup.getLowerBound();
						domainUpper = currentDomainGroup.getUpperBound();
					}
				}
				rangeValue = rangeValues[row];
				rangeUpper = upperErrorValues != null ? upperErrorValues[row] : Double.NaN;
				rangeLower = lowerErrorValues != null ? lowerErrorValues[row] : Double.NaN;

				series[domainValueIdx][row] = domainValue;
				series[domainLowerIdx][row] = domainLower;
				series[domainUpperIdx][row] = domainUpper;
				series[rangeValueIdx][row] = rangeValue;
				series[rangeLowerIdx][row] = rangeLower;
				series[rangeUpperIdx][row] = rangeUpper;
			}

			intervalDataset.addSeries(seriesName, series);
		}
		return intervalDataset;
	}

	public static CategoryDataset createDefaultCategoryDataset(ValueSource valueSource, PlotInstance plotInstance,
			boolean fillWithZero, boolean allowValuesLessThanZero) throws ChartPlottimeException {
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		assertMaxValueCountNotExceededOrThrowException(valueSourceData);
		GroupCellSeriesData dataForAllGroupCells = valueSourceData.getSeriesDataForAllGroupCells();
		DefaultDimensionConfig domainConfig = valueSource.getDomainConfig();
		DimensionConfigData domainConfigData = plotInstance.getPlotData().getDimensionConfigData(domainConfig);
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (GroupCellKeyAndData groupCellKeyAndData : dataForAllGroupCells) {

			GroupCellKey groupCellKey = groupCellKeyAndData.getKey();
			GroupCellData groupCellData = groupCellKeyAndData.getData();

			// create series name
			GroupCellKey groupCellKeyClone = (GroupCellKey) groupCellKey.clone();
			String seriesName = generateSeriesName(valueSource, groupCellKeyClone,
					plotInstance.getCurrentPlotConfigurationClone());

			Map<PlotDimension, double[]> dataForUsageType = groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES);
			double[] xValues = dataForUsageType.get(PlotDimension.DOMAIN);
			double[] yValues = dataForUsageType.get(PlotDimension.VALUE);

			int rowCount = xValues.length;
			for (int row = 0; row < rowCount; ++row) {
				double xValue = xValues[row];

				String xString = null;
				xString = domainConfigData.getStringForValue(xValue);
				Double y = yValues[row];
				if (!allowValuesLessThanZero && y < 0) {
					throw new ChartPlottimeException("illegal_zero_value", valueSource.toString());
				}

				if (xString != null) {
					dataset.addValue(y, seriesName, xString);
				}

			}
			if (fillWithZero) {
				for (int row = 0; row < rowCount; ++row) {
					double xValue = xValues[row];

					String xString = null;
					xString = domainConfigData.getStringForValue(xValue);
					Number value = dataset.getValue(seriesName, xString);
					if (value == null || Double.isNaN(value.doubleValue())) {
						dataset.addValue(0, seriesName, xString);
					}
				}
			}
		}

		return dataset;
	}

	public static String generateSeriesName(ValueSource valueSource, GroupCellKey groupCellKey,
			PlotConfiguration plotConfiguration) {
		// groupCellKey.removeRangeForDimension(Dimension.X); // legend does not need X-group
		String filterName = groupCellKey.getNiceString(plotConfiguration);
		String seriesName = valueSource.getLabel();
		StringBuilder builder = new StringBuilder();
		if (seriesName == null) {
			seriesName = I18N.getGUILabel("plotter.unnamed_value_label");
		}
		builder.append(seriesName);
		if (filterName.length() != 0) {
			builder.append(" [");
			builder.append(filterName);
			builder.append("]");
		}
		return builder.toString();
	}

	public static XYDataset createDefaultXYDataset(ValueSource valueSource, PlotInstance plotInstace)
			throws ChartPlottimeException {
		DefaultXYDataset dataset = new DefaultXYDataset();
		ValueSourceData valueSourceData = plotInstace.getPlotData().getValueSourceData(valueSource);
		// assertMaxValueCountNotExceededOrThrowException(valueSourceData);

		for (int seriesIdx = 0; seriesIdx < valueSourceData.getSeriesCount(); ++seriesIdx) {
			addSeriesToDefaultXYDataset(valueSource, seriesIdx, plotInstace, dataset);
		}
		return dataset;
	}

	public static XYDataset createDefaultXYDataset(ValueSource valueSource, int seriesIdx, PlotInstance plotInstance)
			throws ChartPlottimeException {
		DefaultXYDataset dataset = new DefaultXYDataset();
		addSeriesToDefaultXYDataset(valueSource, seriesIdx, plotInstance, dataset);
		return dataset;
	}

	private static void addSeriesToDefaultXYDataset(ValueSource valueSource, int seriesIdx, PlotInstance plotInstance,
			DefaultXYDataset dataset) throws ChartPlottimeException {
		final int xIdx = 0;
		final int yIdx = 1;

		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		assertMaxValueCountNotExceededOrThrowException(valueSourceData);

		GroupCellSeriesData dataForAllGroupCells = valueSourceData.getSeriesDataForAllGroupCells();
		GroupCellKeyAndData groupCellKeyAndData = dataForAllGroupCells.getGroupCellKeyAndData(seriesIdx);

		GroupCellKey groupCellKey = groupCellKeyAndData.getKey();
		GroupCellData groupCellData = groupCellKeyAndData.getData();

		// create series name
		GroupCellKey groupCellKeyClone = (GroupCellKey) groupCellKey.clone();
		String seriesName = generateSeriesName(valueSource, groupCellKeyClone,
				plotInstance.getCurrentPlotConfigurationClone());
		String differenceName = "__&%" + seriesName + "%&__";

		Map<PlotDimension, double[]> mainData = groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES);
		double[] xValues = mainData.get(PlotDimension.DOMAIN);
		double[] yValues = mainData.get(PlotDimension.VALUE);

		double[][] mainSeries = new double[2][xValues.length];
		mainSeries[xIdx] = xValues;
		mainSeries[yIdx] = yValues;
		dataset.addSeries(seriesName, mainSeries);

		if (valueSource.getSeriesFormat().getUtilityUsage() == IndicatorType.DIFFERENCE) {
			double[] differenceValues = valueSourceData.getAbsoluteUtilityValues(groupCellKeyAndData, true);
			if (differenceValues == null) {
				throw new ChartPlottimeException("undefined_series", valueSource.toString(), SeriesUsageType.INDICATOR_1);
			}
			double[][] differenceSeries = new double[2][xValues.length];
			differenceSeries[xIdx] = xValues;
			differenceSeries[yIdx] = differenceValues;
			dataset.addSeries(differenceName, differenceSeries);
		}
	}

	public static DefaultTableXYDataset createDefaultTableXYDataset(ValueSource valueSource, PlotInstance plotInstance)
			throws ChartPlottimeException {
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		assertMaxValueCountNotExceededOrThrowException(valueSourceData);
		GroupCellSeriesData dataForAllGroupCells = valueSourceData.getSeriesDataForAllGroupCells();
		DefaultTableXYDataset dataset = new DefaultTableXYDataset();

		// Loop all group cells and add data to dataset
		for (GroupCellKeyAndData groupCellKeyAndData : dataForAllGroupCells) {
			GroupCellKey groupCellKey = groupCellKeyAndData.getKey();
			GroupCellData groupCellData = groupCellKeyAndData.getData();

			Map<PlotDimension, double[]> dataForUsageType = groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES);
			double[] xValues = dataForUsageType.get(PlotDimension.DOMAIN);
			double[] yValues = dataForUsageType.get(PlotDimension.VALUE);
			int rowCount = xValues.length;

			String seriesName = generateSeriesName(valueSource, groupCellKey,
					plotInstance.getCurrentPlotConfigurationClone());

			XYSeries series = new XYSeries(seriesName, false, false);

			// Loop all rows and add data to series
			double x;
			double y;
			for (int row = 0; row < rowCount; ++row) {
				x = xValues[row];
				y = yValues[row];
				try {
					if (!Double.isNaN(x)) {
						series.add(x, y, false); // false means: do not notify. Since we are
													// currently initializing, this is not necessary
													// and saves
													// us some fractions of a second
					}
				} catch (SeriesException e) {
					throw new ChartPlottimeException("duplicate_value", valueSource.toString(),
							PlotDimension.DOMAIN.getName());
				}
			}
			dataset.addSeries(series);
		}
		dataset.setAutoWidth(true);
		return dataset;
	}

	public static DefaultStatisticalCategoryDataset createDefaultStatisticalCategoryDataset(ValueSource valueSource,
			PlotInstance plotInstance) throws ChartPlottimeException {
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		assertMaxValueCountNotExceededOrThrowException(valueSourceData);
		GroupCellSeriesData dataForAllGroupCells = valueSourceData.getSeriesDataForAllGroupCells();
		DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
		DefaultDimensionConfig domainConfig = valueSource.getDomainConfig();
		DimensionConfigData domainConfigData = plotInstance.getPlotData().getDimensionConfigData(domainConfig);

		// Loop all group cells and add data to dataset
		for (GroupCellKeyAndData groupCellKeyAndData : dataForAllGroupCells) {
			GroupCellKey groupCellKey = groupCellKeyAndData.getKey();
			GroupCellData groupCellData = groupCellKeyAndData.getData();
			String seriesName = generateSeriesName(valueSource, groupCellKey,
					plotInstance.getCurrentPlotConfigurationClone());

			Map<PlotDimension, double[]> mainSeriesData = groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES);
			double[] xValues = mainSeriesData.get(PlotDimension.DOMAIN);
			double[] yValues = mainSeriesData.get(PlotDimension.VALUE);
			double[] yErrorValues = valueSourceData.getAbsoluteUtilityValues(groupCellKeyAndData, true);
			if (yErrorValues == null) {
				throw new ChartPlottimeException("undefined_series", valueSource.toString(), SeriesUsageType.INDICATOR_1);
			}

			// this dataset does not support unsymmetric errors
			if (groupCellData.getDataForUsageType(SeriesUsageType.INDICATOR_2) != null) {
				throw new ChartPlottimeException("unsymmetric_utility_not_supported", valueSource.toString());
			}

			int rowCount = xValues.length;

			// Loop all rows and add data to series
			double xValue;
			double yValue;
			double yErrorValue;
			String xString;
			for (int row = 0; row < rowCount; ++row) {
				xValue = xValues[row];
				xString = domainConfigData.getStringForValue(xValue);
				yValue = yValues[row];
				yErrorValue = yErrorValues[row] - yValue;

				dataset.add(yValue, yErrorValue, seriesName, xString);
			}
		}
		return dataset;
	}

	/**
	 * Creates a new {@link DefaultMultiValueCategoryDataset}. Such a dataset contains a list of
	 * values for each datapoint of a series. For each series in valueSource a series in the dataset
	 * created. A datapoint in the dataset refers to one x value. In the valueSource there might
	 * exist several datapoints for each x value. These are collected in the list for the
	 * appropriate datapoint in the dataset.
	 * 
	 * @throws ChartPlottimeException
	 */
	public static DefaultMultiValueCategoryDataset createDefaultMultiValueCategoryDataset(ValueSource valueSource,
			PlotInstance plotInstance) throws ChartPlottimeException {
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		assertMaxValueCountNotExceededOrThrowException(valueSourceData);
		GroupCellSeriesData dataForAllGroupCells = valueSourceData.getSeriesDataForAllGroupCells();
		DefaultMultiValueCategoryDataset dataset = new DefaultMultiValueCategoryDataset();
		DefaultDimensionConfig domainConfig = valueSource.getDomainConfig();
		DimensionConfigData domainConfigData = plotInstance.getPlotData().getDimensionConfigData(domainConfig);

		// Loop all group cells and add data to dataset
		for (GroupCellKeyAndData groupCellKeyAndData : dataForAllGroupCells) {
			// the DefaultMultiValueCategoryDataset expects a List of values for each domain value.
			// This map
			// maps domain values to lists and is filled while we iterate through all data value
			// below.
			Map<Double, List<Double>> valueListsForDomainValues = new HashMap<Double, List<Double>>();
			for (Double value : domainConfigData.getDistinctValues()) {
				valueListsForDomainValues.put(value, new LinkedList<Double>());
			}

			GroupCellKey groupCellKey = groupCellKeyAndData.getKey();
			GroupCellData groupCellData = groupCellKeyAndData.getData();
			String seriesName = generateSeriesName(valueSource, groupCellKey,
					plotInstance.getCurrentPlotConfigurationClone());

			Map<PlotDimension, double[]> mainSeriesData = groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES);
			double[] xValues = mainSeriesData.get(PlotDimension.DOMAIN);
			double[] yValues = mainSeriesData.get(PlotDimension.VALUE);
			int rowCount = xValues.length;

			// Loop all rows and add data into map
			double xValue;
			double yValue;
			String xString;
			for (int row = 0; row < rowCount; ++row) {
				xValue = xValues[row];
				yValue = yValues[row];
				valueListsForDomainValues.get(xValue).add(yValue);
			}

			for (Double value : domainConfigData.getDistinctValues()) {
				xString = domainConfigData.getStringForValue(value);
				dataset.add(valueListsForDomainValues.get(value), seriesName, xString);
			}
		}
		return dataset;
	}

	/**
	 * Same as {@link #createDefaultMultiValueCategoryDataset(ValueSource, PlotConfiguration)}, but
	 * instead of storing only the values it stores pairs in the lists, where the first value is the
	 * data value, and the second value holds the index of the value in the series in valueSource.
	 * 
	 * Must be used for the FormattedScatterRenderer.
	 * 
	 * @throws ChartPlottimeException
	 */
	public static DefaultMultiValueCategoryDataset createAnnotatedDefaultMultiValueCategoryDataset(ValueSource valueSource,
			PlotInstance plotInstance) throws ChartPlottimeException {
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		assertMaxValueCountNotExceededOrThrowException(valueSourceData);
		GroupCellSeriesData dataForAllGroupCells = valueSourceData.getSeriesDataForAllGroupCells();
		DefaultMultiValueCategoryDataset dataset = new DefaultMultiValueCategoryDataset();
		DefaultDimensionConfig domainConfig = valueSource.getDomainConfig();
		DimensionConfigData domainConfigData = plotInstance.getPlotData().getDimensionConfigData(domainConfig);

		// Loop all group cells and add data to dataset
		for (GroupCellKeyAndData groupCellKeyAndData : dataForAllGroupCells) {
			// the DefaultMultiValueCategoryDataset expects a List of values for each domain value.
			// This map
			// maps domain values to lists and is filled while we iterate through all data value
			// below.
			Map<Double, List<Pair<Double, Integer>>> valueListsForDomainValues = new HashMap<Double, List<Pair<Double, Integer>>>();
			for (Double value : domainConfigData.getDistinctValues()) {
				valueListsForDomainValues.put(value, new LinkedList<Pair<Double, Integer>>());
			}

			GroupCellKey groupCellKey = groupCellKeyAndData.getKey();
			GroupCellData groupCellData = groupCellKeyAndData.getData();
			String seriesName = generateSeriesName(valueSource, groupCellKey,
					plotInstance.getCurrentPlotConfigurationClone());

			Map<PlotDimension, double[]> mainSeriesData = groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES);
			double[] xValues = mainSeriesData.get(PlotDimension.DOMAIN);
			double[] yValues = mainSeriesData.get(PlotDimension.VALUE);
			int rowCount = xValues.length;

			// Loop all rows and add data into map
			double xValue;
			double yValue;
			String xString;
			for (int row = 0; row < rowCount; ++row) {
				xValue = xValues[row];
				yValue = yValues[row];
				Pair<Double, Integer> valueRowNumberPair = new Pair<Double, Integer>(yValue, row);
				valueListsForDomainValues.get(xValue).add(valueRowNumberPair);
			}

			for (Double value : domainConfigData.getDistinctValues()) {
				xString = domainConfigData.getStringForValue(value);
				dataset.add(valueListsForDomainValues.get(value), seriesName, xString);
			}
		}
		return dataset;
	}

	private static void assertMaxValueCountNotExceededOrThrowException(ValueSourceData valueSourceData)
			throws ChartPlottimeException {
		if (valueSourceData == null) {
			return;
		}
		int maxAllowedValueCount = PlotConfiguration.getMaxAllowedValueCount();

		for (GroupCellKeyAndData groupCellKeyAndData : valueSourceData.getSeriesDataForAllGroupCells()) {
			int size = groupCellKeyAndData.getData().getSize();
			if (size > maxAllowedValueCount) {
				throw new ChartPlottimeException("too_many_values_in_plot", valueSourceData.getValueSource().toString());
			}
		}
	}

}
