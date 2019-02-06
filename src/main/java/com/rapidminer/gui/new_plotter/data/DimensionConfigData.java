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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.new_plotter.ConfigurationChangeResponse;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.PlotConfigurationQuickFix;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.DefaultDimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent.DimensionConfigChangeType;
import com.rapidminer.gui.new_plotter.utility.CategoricalColorProvider;
import com.rapidminer.gui.new_plotter.utility.CategoricalSizeProvider;
import com.rapidminer.gui.new_plotter.utility.ColorProvider;
import com.rapidminer.gui.new_plotter.utility.ContinuousColorProvider;
import com.rapidminer.gui.new_plotter.utility.ContinuousSizeProvider;
import com.rapidminer.gui.new_plotter.utility.NumericalValueRange;
import com.rapidminer.gui.new_plotter.utility.ShapeProvider;
import com.rapidminer.gui.new_plotter.utility.SizeProvider;
import com.rapidminer.gui.new_plotter.utility.ValueRange;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class DimensionConfigData {

	private PlotInstance plotInstance;

	private SizeProvider sizeProvider = null;
	private ColorProvider colorProvider = null;
	private ShapeProvider shapeProvider = null;

	private transient double cachedMinValue = Double.NaN;
	private transient double cachedMaxValue = Double.NaN;
	private transient double cachedMinGroupValue = Double.NaN;
	private transient double cachedMaxGroupValue = Double.NaN;
	private transient List<ValueRange> cachedValueGroups = null;
	private transient List<Double> cachedValues = null;
	private transient List<Double> cachedDistinctValues = null;
	private DataTableColumnIndex columnIdx;
	private DimensionConfigChangeEvent lastProcessedEvent = null;
	private int dimensionConfigId;

	public DimensionConfigData(PlotInstance plotInstance, DefaultDimensionConfig dimensionConfig) {
		this.dimensionConfigId = dimensionConfig.getId();
		this.plotInstance = plotInstance;
		DataTable dataTable = plotInstance.getPlotData().getSortedDataTableWithoutImplicitUpdate();
		this.columnIdx = new DataTableColumnIndex(getDimensionConfig().getDataTableColumn(), dataTable);
	}

	/**
	 * The returned values are sorted and filtered according to the selected sort order and value
	 * range.
	 */
	public List<Double> getValues() {
		if (cachedValues == null) {
			updateValueCache();
		}
		return cachedValues;
	}

	protected void updateShapeProvider() {
		if (getDimensionConfig().isNominal()) {
			List<Double> categoryList = new LinkedList<Double>();
			if (getDimensionConfig().isGrouping()) {
				for (ValueRange valueGroup : getGroupingModel()) {
					categoryList.add(valueGroup.getValue());
				}
			} else {
				categoryList = getDistinctValues();
			}
			setShapeProvider(new ShapeProvider(categoryList));
		} else {
			setShapeProvider(null);
		}
	}

	private void setShapeProvider(ShapeProvider shapeProvider) {
		if (this.shapeProvider != shapeProvider) {
			this.shapeProvider = shapeProvider;
		}
	}

	/**
	 * If isNominal() returns true, returns a string for the given value. If isGrouping() returns
	 * true, value must be an integer and this function returns the toString() method of the
	 * valueGroup with value==valueGroup.getValue(). If no grouping is found with
	 * value==valueGroup.getValue() return <code>null</code>. If isGrouping() returns false, calls
	 * the mapIndex() function of the underlying DataTable with value.
	 * 
	 * If isNominal() returns false, this functions returns null.
	 */
	public String getStringForValue(double value) {
		if (getDimensionConfig().isNominal()) {
			if (getDimensionConfig().isGrouping()) {
				List<ValueRange> groupingModel = getGroupingModel();
				for (ValueRange range : groupingModel) {
					// if range's value is equal to value or both are NaN (i.e. missing value):
					if (range.getValue() == value || (Double.isNaN(range.getValue()) && Double.isNaN(value))) {
						return range.toString();
					}
				}
				return null;
			} else {
				DataTable dataTable = plotInstance.getPlotData().getValueMappingDataTable();
				int columnIdx = DataTableColumn.getColumnIndex(dataTable, getDimensionConfig().getDataTableColumn());
				String valueString;
				if (Double.isNaN(value)) {
					valueString = I18N.getGUILabel("plotter.unknown_value_label");
				} else {
					valueString = dataTable.mapIndex(columnIdx, (int) value);
				}
				return valueString;
			}
		} else {
			return null;
		}
	}

	private void invalidateFormatProviders() {
		colorProvider = null;
		shapeProvider = null;
		sizeProvider = null;
	}

	private void updateGroupingModel() {
		DefaultDimensionConfig dimensionConfig = getDimensionConfig();
		double upperBound = Double.POSITIVE_INFINITY;
		double lowerBound = Double.NEGATIVE_INFINITY;
		if (dimensionConfig.isUsingUserDefinedUpperBound()) {
			upperBound = dimensionConfig.getUserDefinedUpperBound();
		}
		if (dimensionConfig.isUsingUserDefinedLowerBound()) {
			lowerBound = dimensionConfig.getUserDefinedLowerBound();
		}
		cachedValueGroups = dimensionConfig.getGrouping().getGroupingModel(plotInstance.getPlotData().getDataTable(),
				upperBound, lowerBound);

		int maxAllowedValueCount = PlotConfiguration.getMaxAllowedValueCount();
		if (cachedValueGroups.size() > maxAllowedValueCount) {
			ConfigurationChangeResponse response = new ConfigurationChangeResponse();
			response.addError(new PlotConfigurationError("too_many_values_in_plot", dimensionConfig.getDimension().getName()));
			plotInstance.getMasterOfDesaster().registerConfigurationChangeResponse(response);
			cachedValueGroups = new LinkedList<ValueRange>();
			cachedMinGroupValue = 0;
			cachedMaxGroupValue = 1;
			return;
		}

		cachedMinGroupValue = Double.POSITIVE_INFINITY;
		cachedMaxGroupValue = Double.NEGATIVE_INFINITY;

		for (ValueRange group : cachedValueGroups) {
			double value = group.getValue();
			if (value < cachedMinGroupValue) {
				cachedMinGroupValue = value;
			}
			if (value > cachedMaxGroupValue) {
				cachedMaxGroupValue = value;
			}
		}
	}

	/**
	 * May not contain null values. If there are no value groups (because this dimension is not
	 * grouping), this function returns null.
	 * 
	 * Classes implementing this method are strongly advised to cache to list of value ranges, since
	 * this method might be called quite often, and the calculation of the grouping might be quite
	 * expensive.
	 */
	public List<ValueRange> getGroupingModel() {
		if (getDimensionConfig().isGrouping()) {
			if (cachedValueGroups == null) {
				updateGroupingModel();
			}
			return cachedValueGroups;
		} else {
			return null;
		}
	}

	public ColorProvider getColorProvider() {
		if (colorProvider == null) {
			updateColorProvider();
		}
		return colorProvider;
	}

	public SizeProvider getSizeProvider() {
		if (sizeProvider == null) {
			updateSizeProvider();
		}
		return sizeProvider;
	}

	private void setColorProvider(ColorProvider colorProvider) {
		this.colorProvider = colorProvider;
	}

	/**
	 * @return a sorted list of all distinct values in this domain.
	 */
	public List<Double> getDistinctValues() {
		if (cachedDistinctValues == null) {
			cachedDistinctValues = new LinkedList<Double>();
			if (getDimensionConfig().isGrouping()) {
				List<ValueRange> valueRanges = getGroupingModel();
				for (ValueRange range : valueRanges) {
					if (range != null) {
						cachedDistinctValues.add(range.getValue());
					} else {
						cachedDistinctValues.add(Double.NaN);
					}
				}
			} else {
				Set<Double> distinctValuesSet = new HashSet<Double>();
				for (Double value : getValues()) {
					// Set.add() returns true if the element was NOT present before the function
					// call
					if (distinctValuesSet.add(value)) {
						cachedDistinctValues.add(value);
					}
				}
			}
			Collections.sort(cachedDistinctValues);
		}

		return cachedDistinctValues;

	}

	public ShapeProvider getShapeProvider() {
		if (shapeProvider == null) {
			updateShapeProvider();
		}
		return shapeProvider;
	}

	public void clearCache() {
		cachedValues = null;
		cachedValueGroups = null;
		cachedDistinctValues = null;
		cachedMinValue = Double.NaN;
		cachedMaxValue = Double.NaN;
		cachedMinGroupValue = Double.NaN;
		cachedMaxGroupValue = Double.NaN;
		columnIdx.invalidate();
		invalidateFormatProviders();
	}

	/**
	 * Updates the cache of ungrouped values.
	 */
	protected void updateValueCache() {
		DataTable dataTable = plotInstance.getPlotData().getDataTable();

		cachedValues = new LinkedList<Double>();

		// return if we don't have a valid column
		if (columnIdx.getIndex() == -1) {
			cachedMinValue = Double.NaN;
			cachedMaxValue = Double.NaN;
			return;
		}

		cachedMinValue = Double.POSITIVE_INFINITY;
		cachedMaxValue = Double.NEGATIVE_INFINITY;

		boolean useUserDefinedRange = getDimensionConfig().isUsingUserDefinedLowerBound()
				|| getDimensionConfig().isUsingUserDefinedUpperBound();

		// get user defined range (if necessary; only one bound may be required)
		ValueRange userDefinedRange = null;
		if (useUserDefinedRange) {
			userDefinedRange = getDimensionConfig().getUserDefinedRangeClone(dataTable);
			if (userDefinedRange instanceof NumericalValueRange) {
				// set unused bounds to INFINITY
				NumericalValueRange numericalUserDefinedRange = (NumericalValueRange) userDefinedRange;
				if (!getDimensionConfig().isUsingUserDefinedLowerBound()) {
					numericalUserDefinedRange.setLowerBound(Double.NEGATIVE_INFINITY);
				}
				if (!getDimensionConfig().isUsingUserDefinedUpperBound()) {
					numericalUserDefinedRange.setUpperBound(Double.POSITIVE_INFINITY);
				}
				userDefinedRange = numericalUserDefinedRange;
			}
		}

		for (DataTableRow row : dataTable) {
			if (!useUserDefinedRange || userDefinedRange.keepRow(row)) {
				Double value = row.getValue(columnIdx.getIndex());

				// add value to value cache
				cachedValues.add(value);

				// update min/max values
				if (!Double.isInfinite(value) && !Double.isNaN(value)) {
					if (cachedMinValue > value) {
						cachedMinValue = value;
					}
					if (cachedMaxValue < value) {
						cachedMaxValue = value;
					}
				}
			}
		}

		if (cachedMaxValue < cachedMinValue) {
			boolean maxInfinite = Double.isInfinite(cachedMaxValue);
			boolean minInfinite = Double.isInfinite(cachedMinValue);
			if (maxInfinite || minInfinite) {
				if (maxInfinite) {
					cachedMaxValue = Double.POSITIVE_INFINITY;
				}
				if (minInfinite) {
					cachedMinValue = Double.NEGATIVE_INFINITY;
				}
			} else {
				cachedMaxValue = cachedMinValue + 1;
			}
		}
	}

	public NumericalValueRange getEffectiveRange() {
		if (getDimensionConfig().isNominal()) {
			return null;
		}

		double effectiveMin;
		double effectiveMax;

		if (!getDimensionConfig().isUsingUserDefinedLowerBound() || !getDimensionConfig().isUsingUserDefinedUpperBound()) {

			if (Double.isNaN(cachedMaxValue)) {
				updateValueCache();
			}

			effectiveMin = cachedMinValue;
			effectiveMax = cachedMaxValue;

			if (!getDimensionConfig().isUsingUserDefinedLowerBound() && !getDimensionConfig().isUsingUserDefinedUpperBound()) {
				return new NumericalValueRange(effectiveMin, effectiveMax, columnIdx.getIndex(), true, true);
			} else if (getDimensionConfig().isUsingUserDefinedLowerBound()) {
				effectiveMin = getDimensionConfig().getUserDefinedLowerBound();
				if (effectiveMin > effectiveMax) {
					effectiveMax = effectiveMin + 1;
				}
				return new NumericalValueRange(effectiveMin, effectiveMax, columnIdx.getIndex(), true, true);
			} else {
				effectiveMax = getDimensionConfig().getUserDefinedUpperBound();
				if (effectiveMin > effectiveMax) {
					effectiveMin = effectiveMax - 1;
				}
				return new NumericalValueRange(effectiveMin, effectiveMax, columnIdx.getIndex(), true, true);
			}
		} else {
			return (NumericalValueRange) getDimensionConfig().getUserDefinedRangeClone(
					plotInstance.getPlotData().getOriginalDataTable());
		}
	}

	protected void updateColorProvider() {
		if (getDimensionConfig().isNominal()) {
			List<Double> categoryList;
			categoryList = getDistinctValues();
			setColorProvider(new CategoricalColorProvider(plotInstance, categoryList, 255));
		} else {
			setColorProvider(new ContinuousColorProvider(plotInstance, getMinValue(), getMaxValue(), 255,
					getDimensionConfig().isLogarithmic()));
		}
	}

	protected void updateSizeProvider() {
		PlotConfiguration plotConfiguration = plotInstance.getCurrentPlotConfigurationClone();
		if (getDimensionConfig().isNominal()) {
			List<Double> categoryList;
			categoryList = getDistinctValues();
			setSizeProvider(new CategoricalSizeProvider(categoryList, plotConfiguration.getMinShapeSize(),
					plotConfiguration.getMaxShapeSize()));
		} else {
			double minValue = getMinValue();
			double maxValue = getMaxValue();
			setSizeProvider(new ContinuousSizeProvider(minValue, maxValue, plotConfiguration.getMinShapeSize(),
					plotConfiguration.getMaxShapeSize(), getDimensionConfig().isLogarithmic()));
		}
	}

	private void setSizeProvider(SizeProvider sizeProvider) {
		this.sizeProvider = sizeProvider;
	}

	/**
	 * If getRange() is a NumericalValueRange, returns the same as getRange().getLowerBound(). Else
	 * returns the smallest value in getAllValues(), excluding NEG_INFINITY and NaNs.
	 */
	public double getMinValue() {
		if (getDimensionConfig().isGrouping()) {
			if (Double.isNaN(cachedMinGroupValue)) {
				updateGroupingModel();
			}
			return cachedMinGroupValue;
		} else {
			if (Double.isNaN(cachedMinValue)) {
				updateValueCache();
			}
			return getEffectiveRange().getLowerBound();
		}
	}

	/**
	 * If getRange() is not null, returns the same as getRange().getUpperBound(). Else returns the
	 * greatest value in getAllValues(), excluding POS_INFINITY and NaNs.
	 */
	public double getMaxValue() {
		if (getDimensionConfig().isGrouping()) {
			if (Double.isNaN(cachedMaxGroupValue)) {
				updateGroupingModel();
			}
			return cachedMaxGroupValue;
		} else {
			if (Double.isNaN(cachedMaxValue)) {
				updateValueCache();
			}
			return getEffectiveRange().getUpperBound();
		}
	}

	public int getValueCount() {
		if (getDimensionConfig().isGrouping()) {
			return getGroupingModel().size();
		} else {
			return getValues().size();
		}
	}

	public boolean hasDuplicateValues() {
		int distinctValueCount = getDistinctValues().size();
		return distinctValueCount < getValueCount();
	}

	public boolean isLogarithmicPossible() {
		if (getMinValue() < 0.0) {
			return false;
		}
		if (getDimensionConfig().isNominal()) {
			return false;
		}
		return true;
	}

	public int getDistinctValueCount() {
		return getDistinctValues().size();
	}

	public void dimensionConfigChanged(DimensionConfigChangeEvent e) {
		if (e == null || e == lastProcessedEvent) {
			return;
		}
		lastProcessedEvent = e;

		// update dimension config to the one of the current clone
		PlotDimension dimension = e.getSource().getDimension();
		DimensionConfig dimConf = plotInstance.getCurrentPlotConfigurationClone().getDimensionConfig(dimension);
		if (dimConf == null) {
			return;
		}

		switch (e.getType()) {
			case COLUMN:
				columnIdx.setDataTableColumn(dimConf.getDataTableColumn());
			case RESET:
			case GROUPING_CHANGED:
			case RANGE:
			case SORTING:
			case SCALING:
				clearCache();
				invalidateFormatProviders();
				break;
			case COLOR_SCHEME:
				invalidateFormatProviders();
				break;
			default:
		}
	}

	public List<PlotConfigurationError> getErrors() {
		List<PlotConfigurationError> errorList = new LinkedList<PlotConfigurationError>();

		if (cachedValueGroups != null) {
			plotInstance.getCurrentPlotConfigurationClone();
			if (cachedValueGroups.size() > PlotConfiguration.getMaxAllowedValueCount()) {
				errorList.add(new PlotConfigurationError("too_many_values_in_plot", getDimensionConfig().getDimension()
						.getName()));
			}
		}

		if (getDimensionConfig().getDimension() == PlotDimension.SHAPE) {
			ShapeProvider shapeProvider = getShapeProvider();
			if (shapeProvider != null && shapeProvider.maxCategoryCount() < getDistinctValues().size()) {
				PlotConfigurationError error = new PlotConfigurationError("too_many_values_in_dimension",
						PlotDimension.SHAPE.getName(), getDistinctValues().size(), shapeProvider.maxCategoryCount());
				errorList.add(error);
			}
		}
		if (!getDimensionConfig().isNominal() && getDimensionConfig().isLogarithmic() && getMinValue() <= 0.0) {
			PlotConfigurationError error = new PlotConfigurationError("log_axis_contains_zero", getDimensionConfig()
					.getDimension().getName());
			PlotConfigurationQuickFix quickFix = new PlotConfigurationQuickFix(new DimensionConfigChangeEvent(
					getDimensionConfig(), getDimensionConfig().getDimension(), false, DimensionConfigChangeType.SCALING));
			error.addQuickFix(quickFix);
			errorList.add(error);
		}

		return errorList;
	}

	public List<PlotConfigurationError> getWarnings() {
		List<PlotConfigurationError> warnings = new LinkedList<PlotConfigurationError>();

		// check if there are enough defined colors for all categories, or if we use darker.darker
		if (getDimensionConfig() != null) {
			if (getDimensionConfig().getDimension() == PlotDimension.COLOR) {
				if (getDimensionConfig().getValueType() == ValueType.NOMINAL) {
					ColorProvider colorProvider = getColorProvider();
					int categoryCount = getDistinctValueCount();
					int colorListSize = plotInstance.getCurrentPlotConfigurationClone().getActiveColorScheme().getColors()
							.size();
					if (colorProvider != null && colorListSize < categoryCount) {
						PlotConfigurationError warning = new PlotConfigurationError("darken_category_colors", categoryCount,
								colorListSize);
						warnings.add(warning);
					}

				}
			}

			PlotData plotData = plotInstance.getPlotData();
			if ((getDimensionConfig().isUsingUserDefinedLowerBound() || getDimensionConfig().isUsingUserDefinedUpperBound())
					&& plotData.isDataTableValid() && plotData.getDataTable().getRowNumber() == 0) {
				warnings.add(new PlotConfigurationError("user_range_includes_no_data", getDimensionConfig().getDimension()
						.getName()));
			}
		} else {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.new_plotter.data.DimensionConfigData.null_dimension_config");
		}

		return warnings;
	}

	public int getColumnIdx() {
		dimensionConfigChanged(lastProcessedEvent);
		return columnIdx.getIndex();
	}

	@Override
	public String toString() {
		return "DimensionConfigData for " + getDimensionConfig().toString();
	}

	public void setDataTable(DataTable dataTable) {
		columnIdx.setDataTable(dataTable);
	}

	/**
	 * @return The dimension config with the id of this {@link DimensionConfigData} of the
	 *         {@link PlotInstance#getCurrentPlotConfigurationClone()}.
	 */
	public DefaultDimensionConfig getDimensionConfig() {
		return plotInstance.getCurrentPlotConfigurationClone().getDefaultDimensionConfigById(dimensionConfigId);
	}

	/**
	 * @param getDimensionConfig
	 *            () the getDimensionConfig() to set
	 */
	// private void setDimensionConfig(DefaultDimensionConfig getDimensionConfig()) {
	// if(getDimensionConfig() != null && getDimensionConfig().getId() ==
	// this.getDimensionConfig().getId()) {
	// this.getDimensionConfig() = getDimensionConfig();
	// } else {
	// throw new
	// IllegalArgumentException("Trying to set getDimensionConfig() on dimensionConfigData with different id (or null) - this should not happen");
	// }
	// }
}
