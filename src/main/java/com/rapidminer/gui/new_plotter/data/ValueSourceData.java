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
package com.rapidminer.gui.new_plotter.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.datatable.FilteredDataTable;
import com.rapidminer.gui.new_plotter.ConfigurationChangeResponse;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.DefaultDimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.GroupCellKey;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.IndicatorType;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.configuration.ValueSource.SeriesUsageType;
import com.rapidminer.gui.new_plotter.listener.events.ValueSourceChangeEvent;
import com.rapidminer.gui.new_plotter.utility.ValueRange;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.math.function.aggregation.AggregationFunction;


/**
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ValueSourceData {

	private ValueSource valueSource;

	/**
	 * This list contains data for each group cell. The data in this list represents data series
	 * ready to be plotted, i.e. aggregated etc. It does not contain data tables, but double[]
	 * arrays for each SeriesUsageType and each Dimension.
	 */
	private GroupCellSeriesData cachedSeriesDataForAllGroupCells = null;

	/**
	 * Maps groupCells to data. The data in this list is a part of the original data in dataTable
	 * and not yet aggregated.
	 */
	private Map<GroupCellKey, DataTable> cachedGroupCellToDataTableMap = null;

	private double cachedMinValue = Double.NaN;
	private double cachedMaxValue = Double.NaN;

	private PlotInstance plotInstance;

	private Map<SeriesUsageType, Integer> dataTableColumnIdxMap = new HashMap<SeriesUsageType, Integer>();

	private ValueSourceChangeEvent lastProcessedEvent = null;

	private Set<Double> cachedDistinctValues = null;

	public ValueSourceData(ValueSource valueSource, PlotInstance plotInstance) {
		this.valueSource = valueSource;
		this.plotInstance = plotInstance;
		updateDataTableColumns();
	}

	public void clearCache() {
		invalidateGroupingCache();
	}

	public double getAggregatedValueForGroupCell(SeriesUsageType seriesUsage, GroupCellKey groups) {
		DataTable data = getDataTableForGroupCell(groups);
		if (data == null) {
			return Double.NaN;
		}
		double[] doubleArray = new double[data.getRowNumber()];
		int i = 0;
		int columnIdx = getDataTableColumnIdx(seriesUsage);
		for (DataTableRow d : data) {
			doubleArray[i] = d.getValue(columnIdx);
			++i;
		}

		AggregationFunction aggregationFunction = valueSource.getAggregationFunction(seriesUsage);
		double calculate = aggregationFunction.calculate(doubleArray);
		if (Double.isInfinite(calculate)) {
			calculate = Double.NaN;
		}
		return calculate;
	}

	/**
	 * Returns the index of the column set for seriesUsageType, or null if no column for that
	 * seriesUsageType is defined.
	 */
	private int getDataTableColumnIdx(SeriesUsageType seriesUsageType) {
		Integer idx = dataTableColumnIdxMap.get(seriesUsageType);
		if (idx != null) {
			return idx;
		} else {
			return -1;
		}
	}

	/**
	 * Returns the filtered data for a single group cell.
	 */
	public DataTable getDataTableForGroupCell(GroupCellKey groupCellKey) {
		if (cachedGroupCellToDataTableMap == null) {
			applyGrouping();
		}
		return cachedGroupCellToDataTableMap.get(groupCellKey);
	}

	public Set<GroupCellKey> getNonEmptyGroupCells() {
		if (cachedGroupCellToDataTableMap == null) {
			applyGrouping();
		}

		return cachedGroupCellToDataTableMap.keySet();
	}

	/**
	 * Recursive function which fills the groupCellToDataTableRowMap member variable.
	 *
	 * @param dimensionList
	 *            The list of all dimensions by which a group cell is identified.
	 * @param dimensionIdx
	 *            The index of the current dimension. Is increased in each recursion level.
	 * @param data
	 *            Each recursion step applies a filter for each group in the current dimension and
	 *            then calls the next recursion step with the filtered data. The data parameter is
	 *            this filtered data.
	 * @param groupCellKey
	 *            Contains the (partial) key for the current group cell. In each recursion level the
	 *            group for the current dimension is added to the key. Thus in last recursion level
	 *            the key is complete and is used to store the filtered data in
	 *            groupCellToDataTableRowMap.
	 */
	private void createGroupCellData(Vector<PlotDimension> dimensionList, int dimensionIdx, DataTable data,
			GroupCellKey groupCellKey) {
		if (data == null) {
			return;
		}

		PlotDimension dimension = null;
		if (dimensionIdx < dimensionList.size()) {
			dimension = dimensionList.get(dimensionIdx);
		}

		DefaultDimensionConfig dimensionConfig;
		if (dimension == PlotDimension.DOMAIN) {
			dimensionConfig = valueSource.getDomainConfig();
		} else {
			dimensionConfig = (DefaultDimensionConfig) plotInstance.getCurrentPlotConfigurationClone().getDimensionConfig(
					dimension);
		}

		DimensionConfigData dimensionConfigData = plotInstance.getPlotData().getDimensionConfigData(dimensionConfig);
		if (!dimensionConfig.isValid() || dimensionConfigData.getColumnIdx() < 0) {
			ConfigurationChangeResponse response = new ConfigurationChangeResponse();
			response.addError(new PlotConfigurationError("undefined_dimension", dimensionConfig.getDimension().getName()));
			plotInstance.getMasterOfDesaster().registerConfigurationChangeResponse(response);
			return;
		}

		if (groupCellKey == null) {
			groupCellKey = new GroupCellKey();
		}

		int nextIdx = dimensionIdx + 1;
		List<ValueRange> allValueGroups = null;
		allValueGroups = dimensionConfigData.getGroupingModel();

		if (allValueGroups == null) {
			// no grouping in current dimension -> continue without filtering
			if (dimensionIdx < dimensionList.size() - 1) {
				// recurse
				createGroupCellData(dimensionList, nextIdx, data, groupCellKey);
			} else if (data != null) {
				// store result
				cachedGroupCellToDataTableMap.put((GroupCellKey) groupCellKey.clone(), data);
			}
		} else {
			// apply AggregationWindowing on DOMAIN dimension and store it in a vector for fast
			// access by index
			Vector<ValueRange> valueGroupsForFiltering;
			if (dimension == PlotDimension.DOMAIN) {
				valueGroupsForFiltering = new Vector<ValueRange>(valueSource.getAggregationWindowing().applyOnGrouping(
						allValueGroups));
			} else {
				valueGroupsForFiltering = new Vector<ValueRange>(allValueGroups);
			}

			// recurse, or store data for group if we are in the last dimension of the dimension
			// list
			int idx = 0;
			for (ValueRange group : allValueGroups) {
				groupCellKey.setRangeForDimension(dimension, group);
				FilteredDataTable dataFilteredToValueGroupRange = new FilteredDataTable(data);

				ValueRange condition = valueGroupsForFiltering.get(idx);
				if (condition != null) {
					dataFilteredToValueGroupRange.addCondition(condition);
				} else {
					dataFilteredToValueGroupRange = null;
				}

				if (dimensionIdx < dimensionList.size() - 1) {
					// recurse
					createGroupCellData(dimensionList, nextIdx, dataFilteredToValueGroupRange, groupCellKey);
				} else {
					// store result
					cachedGroupCellToDataTableMap.put((GroupCellKey) groupCellKey.clone(), dataFilteredToValueGroupRange);
				}

				++idx;
			}
		}
	}

	/**
	 * Fills cachedGroupCellToDataTableMap, i.e. a data structure which contains a (view on) a
	 * DataTable for each group cell.
	 *
	 * This function is basically just a wrapper for the first call of the recursive
	 * createGroupCellData() function.
	 */
	private void applyGrouping() {
		cachedGroupCellToDataTableMap = new HashMap<GroupCellKey, DataTable>();

		// get vector of all dimensions
		Vector<PlotDimension> dimensionList = new Vector<PlotDimension>();
		dimensionList.addAll(plotInstance.getCurrentPlotConfigurationClone().getDefaultDimensionConfigs().keySet());
		dimensionList.add(PlotDimension.DOMAIN);

		createGroupCellData(dimensionList, 0, plotInstance.getPlotData().getDataTable(valueSource.isSamplingSuggested()),
				null);
	}

	/**
	 * Returns a list of GroupCellKeyAndData which contains contains data for each group cell. The
	 * data in this list represents data series ready to be plotted, i.e. aggregated etc. It does
	 * not contain data tables, but double[] arrays for each SeriesUsageType and each Dimension.
	 *
	 * Since the generation of this list is usually quite expensive, implementations are strongly
	 * encouraged to provide a caching mechanism.
	 */
	public GroupCellSeriesData getSeriesDataForAllGroupCells() {
		synchronized (this) {
			if (cachedSeriesDataForAllGroupCells == null) {
				Vector<PlotDimension> dimensions = new Vector<PlotDimension>();
				for (Entry<PlotDimension, DefaultDimensionConfig> dimensionEntry : plotInstance
						.getCurrentPlotConfigurationClone().getDefaultDimensionConfigs().entrySet()) {
					PlotDimension d = dimensionEntry.getKey();
					if (d == PlotDimension.DOMAIN || d == PlotDimension.VALUE) {
						throw new RuntimeException("This should not happen!");
					}
					dimensions.add(d);
				}
				cachedSeriesDataForAllGroupCells = recursivelyGetSeriesDataForAllGroupCells(dimensions, 0, null, null);
			}
		}

		return cachedSeriesDataForAllGroupCells;
	}

	/**
	 * Fills a data structure containing data for all group cells, indexed by group cell keys.
	 *
	 * @param dimensions
	 *            all dimensions to be used, except X and Y dimension.
	 * @param dimensionIdx
	 *            Internal recursion parameter. Must be 0 on first call.
	 * @param groupCellKey
	 *            Internal recursion parameter. Must be null on first call.
	 * @param valueConfig
	 *            The ValueConfiguration for which the data is generated.
	 * @param dataForAllGroupCells
	 *            An input/output attribute for INTERNAL use only Should be null when called
	 *            manually It is a list containing tupels of groupCells and double[][] arrays which
	 *            represent the data for the group cell. The first index in the array represents the
	 *            row, the second index the dimension. The ordering of the dimensions is taken from
	 *            the dimensions parameter. The x dimension is always added as the last element.
	 * @return Returns dataForAllGroupCells.
	 */
	private GroupCellSeriesData recursivelyGetSeriesDataForAllGroupCells(Vector<PlotDimension> dimensions, int dimensionIdx,
			GroupCellKey groupCellKey, GroupCellSeriesData dataForAllGroupCells) {
		if (groupCellKey == null) {
			groupCellKey = new GroupCellKey();
		}

		if (dataForAllGroupCells == null) {
			dataForAllGroupCells = new GroupCellSeriesData();
		}

		if (dimensionIdx < dimensions.size()) {
			// recurse

			PlotDimension currentDimension = dimensions.get(dimensionIdx);
			DefaultDimensionConfig currentDimensionConfig = (DefaultDimensionConfig) plotInstance
					.getCurrentPlotConfigurationClone().getDimensionConfig(currentDimension);
			DimensionConfigData currentDimensionConfigData = plotInstance.getPlotData().getDimensionConfigData(
					currentDimensionConfig);

			int nextDimensionIdx = dimensionIdx + 1;

			List<ValueRange> allValueGroups = currentDimensionConfigData.getGroupingModel();
			if (allValueGroups == null) {
				// current dimension is not grouping --> recurse
				recursivelyGetSeriesDataForAllGroupCells(dimensions, nextDimensionIdx, groupCellKey, dataForAllGroupCells);
			} else {
				// current dimension is grouping --> iterate over all groups and recurse
				for (ValueRange group : allValueGroups) {
					// recurse
					groupCellKey.setRangeForDimension(currentDimension, group);
					recursivelyGetSeriesDataForAllGroupCells(dimensions, nextDimensionIdx, groupCellKey,
							dataForAllGroupCells);
				}
			}
		} else {
			// last recursion level reached

			// iterate over x-axis
			DefaultDimensionConfig xDimensionConfig = valueSource.getDomainConfig();
			DimensionConfigData xDimensionConfigData = plotInstance.getPlotData().getDimensionConfigData(xDimensionConfig);
			List<ValueRange> xValueGroups = xDimensionConfigData.getGroupingModel();

			if (xValueGroups == null) {
				// x-axis is not grouping --> generate one series from all values in this cell
				groupCellKey.removeRangeForDimension(PlotDimension.DOMAIN);

				DataTable dataForCurrentCell = getDataTableForGroupCell(groupCellKey);
				if (dataForCurrentCell != null) {
					int valueCountInCell = dataForCurrentCell.getRowNumber();

					// initialize data structure
					GroupCellData groupCellData = new GroupCellData();
					Vector<PlotDimension> allDimensions = new Vector<DimensionConfig.PlotDimension>(dimensions.size() + 2);
					allDimensions.addAll(dimensions);
					allDimensions.add(PlotDimension.SELECTED);
					allDimensions.add(PlotDimension.DOMAIN);
					allDimensions.add(PlotDimension.VALUE);
					for (SeriesUsageType usageType : valueSource.getDefinedUsageTypes()) {
						groupCellData.initDataForUsageType(usageType, allDimensions, valueCountInCell);
					}

					DataTable dataTable = plotInstance.getPlotData().getDataTable(valueSource.isSamplingSuggested());

					// iterate over all rows in cell
					int currentRowIdx = 0;
					for (DataTableRow row : dataForCurrentCell) {
						for (SeriesUsageType usageType : valueSource.getDefinedUsageTypes()) {
							// iterate over dimension and put value into output data

							Map<PlotDimension, double[]> dataForUsageType = groupCellData.getDataForUsageType(usageType);

							for (PlotDimension dimension : dimensions) {
								DimensionConfig dimensionConfig = plotInstance.getCurrentPlotConfigurationClone()
										.getDimensionConfig(dimension);
								int columnIdx = DataTableColumn.getColumnIndex(dataTable,
										dimensionConfig.getDataTableColumn());
								dataForUsageType.get(dimension)[currentRowIdx] = row.getValue(columnIdx);
							}

							double yValue = row.getValue(dataTableColumnIdxMap.get(usageType));
							double xValue = row.getValue(DataTableColumn.getColumnIndex(dataTable, valueSource
									.getDomainConfig().getDataTableColumn()));

							// just initialize selection with 1 (selected) as default
							dataForUsageType.get(PlotDimension.SELECTED)[currentRowIdx] = 1;
							dataForUsageType.get(PlotDimension.VALUE)[currentRowIdx] = yValue;
							dataForUsageType.get(PlotDimension.DOMAIN)[currentRowIdx] = xValue;
						}
						++currentRowIdx;
					}

					dataForAllGroupCells.addGroupCell(new GroupCellKeyAndData((GroupCellKey) groupCellKey.clone(),
							groupCellData));
				}
			} else {
				// x-axis is grouping --> iterate over all groups on x-axis and get aggregated
				// values to generate series
				int valueCountInCell = xValueGroups.size();

				// initialize data structure
				GroupCellData groupCellData = new GroupCellData();
				Vector<PlotDimension> allDimensions = new Vector<DimensionConfig.PlotDimension>(dimensions.size() + 2);
				allDimensions.addAll(dimensions);
				allDimensions.add(PlotDimension.SELECTED);
				allDimensions.add(PlotDimension.DOMAIN);
				allDimensions.add(PlotDimension.VALUE);
				for (SeriesUsageType usageType : valueSource.getDefinedUsageTypes()) {
					groupCellData.initDataForUsageType(usageType, allDimensions, valueCountInCell);
				}

				// iterate over all groups on x axis
				for (SeriesUsageType usageType : valueSource.getDefinedUsageTypes()) {
					int xGroupIdx = 0;
					Map<PlotDimension, double[]> dataForUsageType = groupCellData.getDataForUsageType(usageType);
					for (ValueRange group : xValueGroups) {
						groupCellKey.setRangeForDimension(PlotDimension.DOMAIN, group);

						// iterate all dimensions and store the values of the ValueRanges in
						// groupCellDataArrays
						for (PlotDimension dimension : dimensions) {
							ValueRange valueRange = groupCellKey.getRangeForDimension(dimension);
							double value = Double.NaN;
							if (valueRange != null) {
								value = valueRange.getValue();
							}
							dataForUsageType.get(dimension)[xGroupIdx] = value;
						}

						// just initialize selection with 1 (selected) as default
						dataForUsageType.get(PlotDimension.SELECTED)[xGroupIdx] = 1;

						// set X and Y dimension
						double y = getAggregatedValueForGroupCell(usageType, groupCellKey);
						if (xDimensionConfig.getGrouping().isCategorical()) {
							dataForUsageType.get(PlotDimension.DOMAIN)[xGroupIdx] = group.getValue();
						} else {
							dataForUsageType.get(PlotDimension.DOMAIN)[xGroupIdx] = group.getValue();
						}
						dataForUsageType.get(PlotDimension.VALUE)[xGroupIdx] = y;

						++xGroupIdx;
					}
				}

				// copy data from groupCellDataArrays into groupCellDataMap
				groupCellKey.removeRangeForDimension(PlotDimension.DOMAIN);
				dataForAllGroupCells
						.addGroupCell(new GroupCellKeyAndData((GroupCellKey) groupCellKey.clone(), groupCellData));
			}
		}
		return dataForAllGroupCells;
	}

	public int getSeriesCount() {
		if (getSeriesDataForAllGroupCells() != null) {
			return getSeriesDataForAllGroupCells().groupCellCount();
		} else {
			return 0;
		}
	}

	/**
	 * Returns values of utility series for upper or lower case.
	 *
	 * Returns null if {@link SeriesUsageType} UTILITY1 is not set or {@link IndicatorType} is NONE.
	 * If {@link IndicatorType} is DIFFERENCE and secondary is <code>false</code> a List with NaNs
	 * is returned.
	 */
	public double[] getAbsoluteUtilityValues(GroupCellKeyAndData groupCellKeyAndData, boolean secondary) {
		IndicatorType errorIndicator = valueSource.getSeriesFormat().getUtilityUsage();

		// only the UTILITY_1 is used for the DIFFERENCE plot
		if (errorIndicator == IndicatorType.DIFFERENCE && !secondary) {
			return null;
		}

		if (dataTableColumnIdxMap.get(SeriesUsageType.INDICATOR_1) == null || errorIndicator == IndicatorType.NONE) {
			return null;
		}

		SeriesUsageType seriesUsage = SeriesUsageType.INDICATOR_1;

		// check if second utility can should be used
		if (!secondary && dataTableColumnIdxMap.get(SeriesUsageType.INDICATOR_2) != null) {
			seriesUsage = SeriesUsageType.INDICATOR_2;
		}

		GroupCellData groupCellData = groupCellKeyAndData.getData();

		Map<PlotDimension, double[]> absoluteUtilityData = groupCellData.getDataForUsageType(seriesUsage);

		if (absoluteUtilityData == null) {
			return null;
		}

		boolean relative = valueSource.isUsingRelativeIndicator();

		// in the utility arrays always absolute values are stored - so nothing to do
		// if we want to return the absolute values.
		if (!relative) {
			return absoluteUtilityData.get(PlotDimension.VALUE);
		}

		// calculate relative values
		double[] relativeValues = new double[groupCellKeyAndData.getData().getSize()];
		double[] mainSeries = groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES).get(PlotDimension.VALUE);
		double[] utilitySeries = absoluteUtilityData.get(PlotDimension.VALUE);
		for (int valueIdx = 0; valueIdx < mainSeries.length; ++valueIdx) {
			double value;

			// if (relative) {
			if (secondary) {
				value = mainSeries[valueIdx] + utilitySeries[valueIdx];
			} else {
				value = mainSeries[valueIdx] - utilitySeries[valueIdx];
			}
			// } else {
			// value = utilitySeries[valueIdx];
			// }

			relativeValues[valueIdx] = value;
		}

		return relativeValues;
	}

	/**
	 * Returns the min and max value of this value source. If applicable, includes error bars/bands
	 * etc.
	 */
	public Pair<Double, Double> getMinAndMaxValue() {
		if (Double.isNaN(cachedMinValue) || Double.isNaN(cachedMaxValue)) {
			IndicatorType errorIndicator = valueSource.getSeriesFormat().getUtilityUsage();
			if (errorIndicator == IndicatorType.NONE) {
				Pair<Double, Double> minMaxPair = calculateMinMaxFast();
				cachedMinValue = minMaxPair.getFirst();
				cachedMaxValue = minMaxPair.getSecond();
				return minMaxPair;
			} else {
				boolean utility1Set = false;

				if (dataTableColumnIdxMap.get(SeriesUsageType.INDICATOR_1) != null) {
					utility1Set = true;
				}

				if (utility1Set) {
					Pair<Double, Double> minMaxPair = calculateMinMaxWithUtilities();
					cachedMinValue = minMaxPair.getFirst();
					cachedMaxValue = minMaxPair.getSecond();
					return minMaxPair;
				} else {
					Pair<Double, Double> minMaxPair = calculateMinMaxFast();
					cachedMinValue = minMaxPair.getFirst();
					cachedMaxValue = minMaxPair.getSecond();
					return minMaxPair;
				}

			}
		} else {
			return new Pair<Double, Double>(cachedMinValue, cachedMaxValue);
		}

	}

	private Pair<Double, Double> calculateMinMaxWithUtilities() {
		Pair<Double, Double> yMinMax = calculateMinMaxFast();
		double minValue = yMinMax.getFirst();
		double maxValue = yMinMax.getSecond();

		for (GroupCellKeyAndData groupCellKeyAndData : getSeriesDataForAllGroupCells()) {
			double[] upperValues = getAbsoluteUtilityValues(groupCellKeyAndData, true);
			if (upperValues != null) {
				for (Double value : upperValues) {
					if (value < minValue) {
						minValue = value;
					}
					if (value > maxValue) {
						maxValue = value;
					}
				}
			}

			// using DIFFERENCE plot there does not exist something like lower error
			if (valueSource.getSeriesFormat().getUtilityUsage() != IndicatorType.DIFFERENCE) {
				double[] lowerValues = getAbsoluteUtilityValues(groupCellKeyAndData, false);
				for (Double value : lowerValues) {
					if (value < minValue) {
						minValue = value;
					}
					if (value > maxValue) {
						maxValue = value;
					}
				}
			}
		}

		Pair<Double, Double> minMaxValues = new Pair<Double, Double>(minValue, maxValue);

		return minMaxValues;
	}

	/**
	 * fast calculation of min/max if no utility is set. The first value of the pair will hold the
	 * min value, the second value will hold the max value.
	 */
	private Pair<Double, Double> calculateMinMaxFast() {
		Pair<Double, Double> minMaxValues = new Pair<Double, Double>(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
		// no utility is used..

		GroupCellSeriesData dataForAllGroupCells = getSeriesDataForAllGroupCells();
		Set<Double> yValues = dataForAllGroupCells.getDistinctValues(SeriesUsageType.MAIN_SERIES, PlotDimension.VALUE);
		for (Double value : yValues) {
			if (value < minMaxValues.getFirst()) {
				minMaxValues.setFirst(value);
			}
			if (value > minMaxValues.getSecond()) {
				minMaxValues.setSecond(value);
			}
		}
		if (yValues.isEmpty()) {
			minMaxValues.setFirst(Double.NEGATIVE_INFINITY);
			minMaxValues.setSecond(Double.POSITIVE_INFINITY);
		}

		return minMaxValues;
	}

	public double getMinValue() {
		if (Double.isNaN(cachedMinValue)) {
			getMinAndMaxValue();
		}
		return cachedMinValue;
	}

	public double getMaxValue() {
		if (Double.isNaN(cachedMaxValue)) {
			getMinAndMaxValue();
		}
		return cachedMaxValue;
	}

	private void invalidateMinMaxCache() {
		cachedMinValue = Double.NaN;
		cachedMaxValue = Double.NaN;
	}

	private void invalidateValueCache() {
		cachedSeriesDataForAllGroupCells = null;
		cachedDistinctValues = null;
		invalidateMinMaxCache();
	}

	private void invalidateGroupingCache() {
		cachedGroupCellToDataTableMap = null;
		invalidateValueCache();
	}

	/**
	 * Calls updateDataTableColumn() on each dataTableColumn used in this ValueSource.
	 */
	private void updateDataTableColumns() {
		dataTableColumnIdxMap.clear();
		for (SeriesUsageType seriesUsageType : valueSource.getDefinedUsageTypes()) {
			DataTableColumn dataTableColumn = valueSource.getDataTableColumn(seriesUsageType);
			int columnIdx = DataTableColumn.getColumnIndex(plotInstance.getPlotData().getOriginalDataTable(),
					dataTableColumn);
			dataTableColumnIdxMap.put(seriesUsageType, columnIdx);
		}
	}

	public String getStringForValue(SeriesUsageType seriesUsage, double y) {
		if (valueSource.isNominal()) {
			return plotInstance.getPlotData().getValueMappingDataTable()
					.mapIndex(getDataTableColumnIdx(seriesUsage), (int) y);
		} else {
			return null;
		}
	}

	public void valueSourceChanged(ValueSourceChangeEvent e, ValueSource clonedValueSource) {
		if (e == null || e == lastProcessedEvent) {
			return;
		}
		lastProcessedEvent = e;

		if (clonedValueSource == null) {
			return;
		}
		setValueSource(clonedValueSource);

		switch (e.getType()) {
			case AGGREGATION_FUNCTION_MAP:
			case DATATABLE_COLUMN_MAP:
				updateDataTableColumns();
				invalidateValueCache();
				break;
			case AGGREGATION_WINDOWING_CHANGED:
			case USES_GROUPING:
			case UPDATED:
				invalidateGroupingCache();
				break;
			case USE_RELATIVE_UTILITIES:
			case SERIES_FORMAT_CHANGED:
				invalidateMinMaxCache();
				break;
			default:
		}

	}

	public ValueSource getValueSource() {
		return valueSource;
	}

	public List<PlotConfigurationError> getErrors() {
		// TODO implement me
		List<PlotConfigurationError> errorList = new LinkedList<PlotConfigurationError>();
		return errorList;
	}

	public List<PlotConfigurationError> getWarnings() {
		// TODO implement me
		List<PlotConfigurationError> warnings = new LinkedList<PlotConfigurationError>();
		return warnings;
	}

	public Set<Double> getDistinctValues() {
		if (cachedDistinctValues == null) {
			updateDistinctValues();
		}
		return cachedDistinctValues;
	}

	private void updateDistinctValues() {
		cachedDistinctValues = new HashSet<Double>();
		for (GroupCellKeyAndData groupCellKeyAndData : getSeriesDataForAllGroupCells()) {
			GroupCellData groupCellData = groupCellKeyAndData.getData();
			Map<PlotDimension, double[]> dataForUsageType = groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES);
			for (double value : dataForUsageType.get(PlotDimension.VALUE)) {
				cachedDistinctValues.add(value);
			}
		}
	}

	/**
	 * @param valueSource
	 *            the valueSource to set
	 */
	private void setValueSource(ValueSource valueSource) {
		if (valueSource != null && valueSource.getId() == this.valueSource.getId()) {
			this.valueSource = valueSource;
		}
	}
}
