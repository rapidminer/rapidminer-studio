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
package com.rapidminer.gui.new_plotter.engine.jfreechart.dataset;

import com.rapidminer.gui.new_plotter.configuration.DefaultDimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.GroupCellKey;
import com.rapidminer.gui.new_plotter.configuration.ValueSource.SeriesUsageType;
import com.rapidminer.gui.new_plotter.data.DimensionConfigData;
import com.rapidminer.gui.new_plotter.data.GroupCellData;
import com.rapidminer.gui.new_plotter.data.GroupCellKeyAndData;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.data.ValueSourceData;
import com.rapidminer.gui.new_plotter.engine.jfreechart.ChartDatasetFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.jfree.data.Range;
import org.jfree.data.RangeInfo;
import org.jfree.data.general.AbstractDataset;
import org.jfree.data.statistics.MultiValueCategoryDataset;


/**
 * An adapter which wraps a value source inside a MultiValueCategoryDataset.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ValueSourceToMultiValueCategoryDatasetAdapter extends AbstractDataset implements MultiValueCategoryDataset,
		RangeInfo, Cloneable {

	private static final long serialVersionUID = 1L;

	private ValueSourceData valueSourceData;
	private PlotInstance plotInstance;

	private transient Vector<String> seriesNamesCache = null;
	private transient double minValueCache = Double.NaN;
	private transient double maxValueCache = Double.NaN;
	private transient Vector<Double> domainValuesCache = null;

	/**
	 * Maps {@link GroupCellKey}s of the value source to a map which maps domain values in that
	 * series to all y values which exist for that domain value in that series.
	 */
	private transient Map<GroupCellKey, Map<Integer, Vector<Double>>> groupCellKeyToDomainValueToValuesCache = null;
	private transient Map<GroupCellKey, Map<Integer, Vector<Integer>>> groupCellKeyToDomainValueToValueIdxCache = null;

	public ValueSourceToMultiValueCategoryDatasetAdapter(ValueSourceData valueSourceData, PlotInstance plotInstance) {
		if (valueSourceData == null || plotInstance == null) {
			throw new IllegalArgumentException("null not allowed");
		}

		this.valueSourceData = valueSourceData;
		this.plotInstance = plotInstance;
	}

	@Override
	public String getRowKey(int row) {
		if (seriesNamesCache == null) {
			updateSeriesNameCache();
		}
		return seriesNamesCache.get(row);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public int getRowIndex(Comparable key) {
		if (seriesNamesCache == null) {
			updateSeriesNameCache();
		}
		return seriesNamesCache.indexOf(key);
	}

	private void updateSeriesNameCache() {
		seriesNamesCache = new Vector<String>(valueSourceData.getSeriesCount());
		for (GroupCellKeyAndData groupCellKeyAndData : valueSourceData.getSeriesDataForAllGroupCells()) {
			GroupCellKey groupCellKey = groupCellKeyAndData.getKey();
			seriesNamesCache.add(ChartDatasetFactory.generateSeriesName(valueSourceData.getValueSource(), groupCellKey,
					plotInstance.getCurrentPlotConfigurationClone()));
		}
	}

	@Override
	public List<String> getRowKeys() {
		if (seriesNamesCache == null) {
			updateSeriesNameCache();
		}
		return seriesNamesCache;
	}

	@Override
	public String getColumnKey(int column) {
		if (domainValuesCache == null) {
			updateValuesCache();
		}
		Double columnValue = domainValuesCache.get(column);
		DefaultDimensionConfig domainConfig = valueSourceData.getValueSource().getDomainConfig();
		return plotInstance.getPlotData().getDimensionConfigData(domainConfig).getStringForValue(columnValue);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public int getColumnIndex(Comparable key) {
		if (domainValuesCache == null) {
			updateValuesCache();
		}
		return domainValuesCache.indexOf(key);
	}

	@Override
	public List<Double> getColumnKeys() {
		if (domainValuesCache == null) {
			updateValuesCache();
		}
		return domainValuesCache;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Number getValue(Comparable rowKey, Comparable columnKey) {
		return null;
	}

	@Override
	public int getRowCount() {
		return valueSourceData.getSeriesCount();
	}

	@Override
	public int getColumnCount() {
		DefaultDimensionConfig domainConfig = valueSourceData.getValueSource().getDomainConfig();
		return plotInstance.getPlotData().getDimensionConfigData(domainConfig).getDistinctValueCount();
	}

	@Override
	public Number getValue(int row, int column) {
		List<Double> values = getValues(row, column);
		if (values != null && values.size() > 0) {
			return values.get(0);
		}
		return null;
	}

	@Override
	public double getRangeLowerBound(boolean includeInterval) {
		if (Double.isNaN(minValueCache)) {
			updateValuesCache();
		}
		return minValueCache;
	}

	@Override
	public double getRangeUpperBound(boolean includeInterval) {
		if (Double.isNaN(minValueCache)) {
			updateValuesCache();
		}
		return maxValueCache;
	}

	@Override
	public Range getRangeBounds(boolean includeInterval) {
		if (Double.isNaN(minValueCache) || Double.isNaN(maxValueCache)) {
			updateValuesCache();
		}
		return new Range(minValueCache, maxValueCache);
	}

	@Override
	public List<Double> getValues(int row, int column) {
		if (groupCellKeyToDomainValueToValuesCache == null) {
			updateValuesCache();
		}
		GroupCellKey groupCellKey = valueSourceData.getSeriesDataForAllGroupCells().getGroupCellKeyAndData(row).getKey();
		Map<Integer, Vector<Double>> map = groupCellKeyToDomainValueToValuesCache.get(groupCellKey);
		if (map != null) {
			return map.get(column);
		} else {
			return new LinkedList<Double>();
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List<Double> getValues(Comparable rowKey, Comparable columnKey) {
		Number row = (Number) rowKey;
		Number column = (Number) columnKey;
		return getValues(row.intValue(), column.intValue());
	}

	private void updateValuesCache() {
		minValueCache = Double.POSITIVE_INFINITY;
		maxValueCache = Double.NEGATIVE_INFINITY;
		groupCellKeyToDomainValueToValuesCache = new HashMap<GroupCellKey, Map<Integer, Vector<Double>>>();
		groupCellKeyToDomainValueToValueIdxCache = new HashMap<GroupCellKey, Map<Integer, Vector<Integer>>>();
		seriesNamesCache = new Vector<String>(valueSourceData.getSeriesCount());
		DefaultDimensionConfig domainConfig = valueSourceData.getValueSource().getDomainConfig();
		DimensionConfigData domainConfigData = plotInstance.getPlotData().getDimensionConfigData(domainConfig);
		domainValuesCache = new Vector<Double>(domainConfigData.getDistinctValueCount());
		domainValuesCache.addAll(domainConfigData.getDistinctValues());
		Collections.sort(domainValuesCache);

		for (GroupCellKeyAndData groupCellKeyAndData : valueSourceData.getSeriesDataForAllGroupCells()) {
			GroupCellData groupCellData = groupCellKeyAndData.getData();
			GroupCellKey groupCellKey = groupCellKeyAndData.getKey();

			seriesNamesCache.add(ChartDatasetFactory.generateSeriesName(valueSourceData.getValueSource(), groupCellKey,
					plotInstance.getCurrentPlotConfigurationClone()));

			double[] yData = groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES).get(PlotDimension.VALUE);
			double[] domainData = groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES).get(PlotDimension.DOMAIN);
			Map<Integer, Vector<Double>> domainToValuesMap = new HashMap<Integer, Vector<Double>>();
			Map<Integer, Vector<Integer>> domainToValueIdxMap = new HashMap<Integer, Vector<Integer>>();
			for (int i = 0; i < yData.length; ++i) {
				double y = yData[i];
				double x = domainData[i];
				int xIdx = getColumnIndex(x);

				if (y > maxValueCache) {
					maxValueCache = y;
				}
				if (y < minValueCache) {
					minValueCache = y;
				}

				groupCellKeyToDomainValueToValuesCache.put(groupCellKey, domainToValuesMap);
				groupCellKeyToDomainValueToValueIdxCache.put(groupCellKey, domainToValueIdxMap);

				Vector<Double> valueListForX = domainToValuesMap.get(xIdx);
				Vector<Integer> valueIdxListForX = domainToValueIdxMap.get(xIdx);
				if (valueListForX == null) {
					valueListForX = new Vector<Double>();
					valueIdxListForX = new Vector<Integer>();
					valueListForX.add(y);
					valueIdxListForX.add(i);
					domainToValuesMap.put(xIdx, valueListForX);
					domainToValueIdxMap.put(xIdx, valueIdxListForX);
				} else {
					valueListForX.add(y);
					valueIdxListForX.add(i);
				}
			}
		}
	}

	/**
	 * This function maps from DataSet index to ValueSource index.
	 */
	public int getValueIndex(int row, int column, int valueIdx) {
		if (groupCellKeyToDomainValueToValueIdxCache == null) {
			updateValuesCache();
		}
		GroupCellKey groupCellKey = valueSourceData.getSeriesDataForAllGroupCells().getGroupCellKeyAndData(row).getKey();
		Map<Integer, Vector<Integer>> map = groupCellKeyToDomainValueToValueIdxCache.get(groupCellKey);
		Vector<Integer> vector = map.get(column);
		return vector.get(valueIdx);
	}
}
