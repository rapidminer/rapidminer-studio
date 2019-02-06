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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.StackingMode;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.listener.events.RangeAxisConfigChangeEvent;
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;
import com.rapidminer.gui.new_plotter.utility.NumericalValueRange;
import com.rapidminer.tools.container.Pair;


/**
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class RangeAxisData {

	private RangeAxisConfig rangeAxisConfig;
	private final PlotInstance plotInstance;
	private RangeAxisConfigChangeEvent lastProcessedEvent = null;

	private List<Pair<ValueSource, Double>> cachedDistinctSourcesAndValues = null;

	private double cachedMaxYValue = Double.NaN;
	private double cachedMinYValue = Double.NaN;

	private NumericalValueRange cachedAutoViewRange = null;

	public RangeAxisData(RangeAxisConfig rangeAxisConfig, PlotInstance plotInstance) {
		super();
		this.rangeAxisConfig = rangeAxisConfig;
		this.plotInstance = plotInstance;
	}

	private void updateValueCache() {

		cachedDistinctSourcesAndValues = new LinkedList<Pair<ValueSource, Double>>();

		Set<Double> distinctValues = new HashSet<Double>();

		cachedMinYValue = Double.POSITIVE_INFINITY;
		cachedMaxYValue = Double.NEGATIVE_INFINITY;

		List<ValueSource> valueSources = new LinkedList<ValueSource>();
		valueSources.addAll(rangeAxisConfig.getValueSources());
		for (ValueSource valueSource : valueSources) {
			ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
			Pair<Double, Double> minMax = valueSourceData.getMinAndMaxValue();
			double minValue = minMax.getFirst();
			double maxValue = minMax.getSecond();

			if (valueSource.getSeriesFormat().getSeriesType() == VisualizationType.AREA
					|| valueSource.getSeriesFormat().getSeriesType() == VisualizationType.BARS) {
				if (valueSource.getSeriesFormat().getStackingMode() == StackingMode.RELATIVE) {
					minValue = 0;
					maxValue = 1;
				}
			}

			if (minValue < cachedMinYValue) {
				cachedMinYValue = minValue;
			}
			if (maxValue > cachedMaxYValue) {
				cachedMaxYValue = maxValue;
			}

			for (Double value : valueSourceData.getDistinctValues()) {
				if (!distinctValues.contains(value)) {
					distinctValues.add(value);
					cachedDistinctSourcesAndValues.add(new Pair<ValueSource, Double>(valueSource, value));
				}
			}
		}

		cachedAutoViewRange = new NumericalValueRange(cachedMinYValue, cachedMaxYValue, -1);

	}

	/**
	 * Returns a list of all values on this RangeAxisConfig, together with the ValueSource which
	 * generates the respective value. If a value appears in more than one ValueSource, only the
	 * value from the first ValueSource is added to the list.
	 */
	public List<Pair<ValueSource, Double>> getDistinctValues() {
		if (cachedDistinctSourcesAndValues == null) {
			updateValueCache();
		}
		return cachedDistinctSourcesAndValues;
	}

	/**
	 * This function is used receiving the viewing range of this range axis only.
	 * 
	 * @return lower viewing bound
	 */
	public double getLowerViewBound() {

		if (rangeAxisConfig.isUsingUserDefinedLowerViewBound()) {
			return rangeAxisConfig.getUserDefinedRange().getLowerBound();
		} else {
			if (cachedAutoViewRange == null) {
				updateValueCache();
			}
			return cachedAutoViewRange.getLowerBound();
		}
	}

	/**
	 * This function is used receiving the viewing range of this range axis only.
	 * 
	 * @return upper viewing bound
	 */
	public double getUpperViewBound() {
		if (rangeAxisConfig.isUsingUserDefinedUpperViewBound()) {
			return rangeAxisConfig.getUserDefinedRange().getUpperBound();
		} else {
			if (cachedAutoViewRange == null) {
				updateValueCache();
			}
			return cachedAutoViewRange.getUpperBound();
		}
	}

	private void invalidateCache() {
		cachedDistinctSourcesAndValues = null;
		cachedMaxYValue = Double.NaN;
		cachedMinYValue = Double.NaN;
		invalidateViewRange();
	}

	private void invalidateViewRange() {
		cachedAutoViewRange = null;
	}

	public double getMaxYValue() {
		if (Double.isNaN(cachedMaxYValue)) {
			updateValueCache();
		}
		return cachedMaxYValue;
	}

	public double getMinYValue() {
		if (Double.isNaN(cachedMinYValue)) {
			updateValueCache();
		}
		return cachedMinYValue;
	}

	public void rangeAxisConfigChanged(RangeAxisConfigChangeEvent e) {
		if (e == null || e == lastProcessedEvent) {
			return;
		}
		lastProcessedEvent = e;

		// update rangeAxisConfig to the one of the current plot configuration clone with the
		// corresponding id
		PlotConfiguration currentPlotConfigurationClone = plotInstance.getCurrentPlotConfigurationClone();
		int id = e.getSource().getId();
		RangeAxisConfig rangeAxisConfigById = currentPlotConfigurationClone.getRangeAxisConfigById(id);
		if (rangeAxisConfigById == null) {
			return;  // do nothing if range axis is not present anymore
		}
		setRangeAxisConfig(rangeAxisConfigById);

		switch (e.getType()) {
			case CLEARED:
			case RANGE_CHANGED:
			case VALUE_SOURCE_ADDED:
			case VALUE_SOURCE_CHANGED:
			case VALUE_SOURCE_REMOVED:
			case VALUE_SOURCE_MOVED:
				invalidateCache();
				break;
			default:
		}
	}

	public RangeAxisConfig getRangeAxisConfig() {
		return rangeAxisConfig;
	}

	/**
	 * @param rangeAxisConfig
	 *            the rangeAxisConfig to set
	 */
	private void setRangeAxisConfig(RangeAxisConfig rangeAxisConfig) {
		if (rangeAxisConfig != null && rangeAxisConfig.getId() == this.rangeAxisConfig.getId()) {
			this.rangeAxisConfig = rangeAxisConfig;
		}
	}

	public List<PlotConfigurationError> getWarnings() {
		List<PlotConfigurationError> warnings = new LinkedList<PlotConfigurationError>();

		if (rangeAxisConfig.getValueType() == ValueType.UNKNOWN) {
			return warnings;
		}

		double lowerViewBound = getLowerViewBound();
		double upperViewBound = getUpperViewBound();
		boolean equalUpperAndLowerBound = DataStructureUtils.almostEqual(lowerViewBound, upperViewBound, 1E-6);
		if (DataStructureUtils.greaterOrAlmostEqual(lowerViewBound, upperViewBound, 1E-6) && !equalUpperAndLowerBound) {
			warnings.add(new PlotConfigurationError("user_range_includes_no_data", rangeAxisConfig.getLabel()));
		}
		// else if (Double.isInfinite(lowerViewBound) || Double.isInfinite(upperViewBound)) {
		// warnings.add(new PlotConfigurationError("user_range_includes_no_data",
		// rangeAxisConfig.getLabel()));
		// }
		else {
			double maxYValue = getMaxYValue();
			double minYValue = getMinYValue();
			boolean equalMinAndMaxValue = DataStructureUtils.almostEqual(minYValue, maxYValue, 1E-6);
			if ((DataStructureUtils.greaterOrAlmostEqual(lowerViewBound, maxYValue, 1E-6) || DataStructureUtils
					.greaterOrAlmostEqual(minYValue, upperViewBound, 1E-6)) && !equalMinAndMaxValue) {
				warnings.add(new PlotConfigurationError("user_range_includes_no_data", rangeAxisConfig.getLabel()));
			}
		}
		return warnings;
	}

	/**
	 * @return
	 */
	public List<PlotConfigurationError> getErrors() {
		List<PlotConfigurationError> errors = new LinkedList<PlotConfigurationError>();

		double lowerViewBound = getLowerViewBound();
		double upperViewBound = getUpperViewBound();

		if (rangeAxisConfig.getValueType() == ValueType.DATE_TIME
				&& (Double.isInfinite(lowerViewBound) || Double.isInfinite(upperViewBound))) {
			errors.add(new PlotConfigurationError("infite_range_for_date_axis", rangeAxisConfig.getLabel()));
			return errors;
		}

		if (rangeAxisConfig.isUsingUserDefinedUpperViewBound()
				&& DataStructureUtils.greaterOrAlmostEqual(lowerViewBound, upperViewBound, 1E-6)) {
			errors.add(new PlotConfigurationError("axis_upper_range_below_lower_auto_range", rangeAxisConfig.getLabel(),
					lowerViewBound + ""));
			return errors;
		}

		return errors;
	}
}
