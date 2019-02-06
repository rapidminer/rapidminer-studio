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
package com.rapidminer.gui.new_plotter.configuration;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.new_plotter.ChartConfigurationException;
import com.rapidminer.gui.new_plotter.listener.events.ValueGroupingChangeEvent;
import com.rapidminer.gui.new_plotter.utility.NumericalValueRange;
import com.rapidminer.gui.new_plotter.utility.ValueRange;

import java.text.DateFormat;
import java.util.LinkedList;
import java.util.List;


/**
 * Defines a binning with a fixed number of equal width bins between a min and max value. Can be
 * either categorical or numerical. If the binning is categorical, each created ValueRange delivers
 * an integer idx as identifier, otherwise the mean value of upper and lower bound.
 * 
 * If this binning is categorical, overflow and underflow bins are created for values which are
 * greater/lesser than minValue/maxValue.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class EquidistantFixedBinCountBinning extends AbstractValueGrouping {

	/**
	 * Number of bins (excluding overflow bins)
	 */
	private int binCount;
	private double userDefinedMinValue;
	private double userDefinedMaxValue;
	private boolean autoRange = true;

	private final GroupingType type = GroupingType.EQUIDISTANT_FIXED_BIN_COUNT;

	/**
	 * @param binCount
	 * @param minValue
	 *            The left value of the left-most normal (non-overflow) bin. If NaN, the left-most
	 *            point in the data is chosen. Infinity is not allowed.
	 * @param maxValue
	 *            The right value of the right-most normal (non-overflow) bin. If NaN, the
	 *            right-most point in the data is chosen. Infinity is not allowed.
	 * @param dataTableColumn
	 * @param categorical
	 * @throws ChartConfigurationException
	 *             if dataTableColumn is nominal
	 */
	public EquidistantFixedBinCountBinning(int binCount, double minValue, double maxValue, DataTableColumn dataTableColumn,
			boolean categorical, DateFormat dateFormat) throws ChartConfigurationException {
		super(dataTableColumn, categorical, dateFormat);

		if (dataTableColumn.isNominal()) {
			throw new ChartConfigurationException("grouping.illegal_column_type", getGroupingType().getName(),
					dataTableColumn.getName(), dataTableColumn.getValueType(), "numerical or date.");
		}

		this.binCount = binCount;
		this.userDefinedMinValue = minValue;
		this.userDefinedMaxValue = maxValue;

	}

	/**
	 * Copy ctor.
	 */
	public EquidistantFixedBinCountBinning(EquidistantFixedBinCountBinning other) {
		super(other.getDataTableColumn(), other.isCategorical(), other.getDateFormat());
		this.binCount = other.binCount;
		this.userDefinedMinValue = other.userDefinedMinValue;
		this.userDefinedMaxValue = other.userDefinedMaxValue;
		this.autoRange = other.autoRange;
		this.forceDataTableColumn(other.getDataTableColumn());
	}

	/**
	 * @return the binCount
	 */
	public int getBinCount() {
		return binCount;
	}

	/**
	 * @param binCount
	 *            the binCount to set
	 */
	public void setBinCount(int binCount) {
		if (binCount != this.binCount) {
			this.binCount = binCount;
			// invalidateCache();
			fireGroupingChanged(new ValueGroupingChangeEvent(this, binCount));
		}
	}

	/**
	 * @return the minValue
	 */
	public double getMinValue() {
		return userDefinedMinValue;
	}

	/**
	 * @param minValue
	 *            the minValue to set
	 */
	public void setMinValue(double minValue) {
		if (minValue != this.userDefinedMinValue) {
			this.userDefinedMinValue = minValue;
			fireGroupingChanged(new ValueGroupingChangeEvent(this));
		}
	}

	/**
	 * @return the maxValue
	 */
	public double getMaxValue() {
		return userDefinedMaxValue;
	}

	/**
	 * @param maxValue
	 *            the maxValue to set
	 */
	public void setMaxValue(double maxValue) {
		if (maxValue != this.userDefinedMaxValue) {
			this.userDefinedMaxValue = maxValue;
			fireGroupingChanged(new ValueGroupingChangeEvent(this));
		}
	}

	@Override
	protected List<ValueRange> createGroupingModel(DataTable data, double userDefinedUpperDimensionBound,
			double userDefinedLowerDimensionBound) {
		if (getDataTableColumn() == null) {
			return null;
		}

		List<ValueRange> groupingModel = new LinkedList<ValueRange>();

		double currentMinValue = userDefinedMinValue;
		double currentMaxValue = userDefinedMaxValue;

		if (calculatesAutoRange()) {

			// determine min and max values from data
			double newMinValue = Double.POSITIVE_INFINITY;
			double newMaxValue = Double.NEGATIVE_INFINITY;
			int columnIdx = DataTableColumn.getColumnIndex(data, getDataTableColumn());
			for (DataTableRow row : data) {
				double value = row.getValue(columnIdx);
				if (value >= userDefinedLowerDimensionBound && value <= userDefinedUpperDimensionBound) {
					if (newMinValue > value) {
						newMinValue = value;
					}
					if (newMaxValue < value) {
						newMaxValue = value;
					}

				}
			}

			if (!Double.isInfinite(userDefinedLowerDimensionBound) && newMinValue > userDefinedLowerDimensionBound) {
				newMinValue = userDefinedLowerDimensionBound;
			}

			if (!Double.isInfinite(userDefinedUpperDimensionBound) && newMaxValue < userDefinedUpperDimensionBound) {
				newMaxValue = userDefinedUpperDimensionBound;
			}
			currentMinValue = newMinValue;
			currentMaxValue = newMaxValue;
		}

		int columnIdx = DataTableColumn.getColumnIndex(data, getDataTableColumn());
		boolean columnIsDate = getDataTableColumn().isDate();
		groupingModel = new LinkedList<ValueRange>();
		if (isCategorical() && (!isAutoRanging() || Double.isNaN(currentMinValue))) {
			// create underflow bin
			ValueRange group = new NumericalValueRange(Double.NEGATIVE_INFINITY, currentMinValue, columnIdx, null, true,
					false);
			groupingModel.add(group);
		}

		boolean includeLowerBound = true;
		boolean includeUpperBound = false;

		// create normal bins
		double stepWidth = (currentMaxValue - currentMinValue) / binCount;
		for (int i = 0; i < binCount; ++i) {
			userDefinedLowerDimensionBound = currentMinValue + (currentMaxValue - currentMinValue) / binCount * i;
			userDefinedUpperDimensionBound = userDefinedLowerDimensionBound + stepWidth;

			if (i == binCount - 1) {
				// last bin should include largest value
				includeUpperBound = true;
				groupingModel.add(new NumericalValueRange(userDefinedLowerDimensionBound, currentMaxValue, columnIdx, null,
						includeLowerBound, includeUpperBound));
				continue;
			}
			groupingModel.add(new NumericalValueRange(userDefinedLowerDimensionBound, userDefinedUpperDimensionBound,
					columnIdx, null, includeLowerBound, includeUpperBound));
		}

		if (isCategorical() && (!isAutoRanging() || Double.isNaN(currentMaxValue))) {
			// create overflow bin
			ValueRange group = new NumericalValueRange(currentMaxValue, Double.POSITIVE_INFINITY, columnIdx, null, false,
					true);
			groupingModel.add(group);
		}

		applyAdaptiveVisualRounding(groupingModel, columnIsDate);

		return groupingModel;
	}

	@Override
	public GroupingType getGroupingType() {
		return type;
	}

	@Override
	public ValueGrouping clone() {
		return new EquidistantFixedBinCountBinning(this);
	}

	// @Override
	// protected void invalidateCache() {
	// super.invalidateCache();
	// }

	private boolean calculatesAutoRange() {
		return Double.isNaN(userDefinedMinValue) || Double.isNaN(userDefinedMaxValue) || autoRange;
	}

	/**
	 * @return the autoRange
	 */
	public boolean isAutoRanging() {
		return autoRange;
	}

	/**
	 * @param autoRange
	 *            the autoRange to set
	 */
	public void setAutoRange(boolean autoRange) {
		if (autoRange != this.autoRange) {
			this.autoRange = autoRange;
			fireGroupingChanged(new ValueGroupingChangeEvent(this));
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof EquidistantFixedBinCountBinning)) {
			return false;
		}

		EquidistantFixedBinCountBinning other = (EquidistantFixedBinCountBinning) obj;

		if (other.isCategorical() != isCategorical()) {
			return false;
		}

		if (other.getBinCount() != getBinCount()) {
			return false;
		}

		if (other.isAutoRanging() != isAutoRanging()) {
			return false;
		}

		if (Double.isNaN(other.getMaxValue())) {
			if (!Double.isNaN(getMaxValue())) {
				return false;
			}
		} else if (other.getMaxValue() != getMaxValue()) {
			return false;
		}

		if (Double.isNaN(other.getMinValue())) {
			if (!Double.isNaN(getMinValue())) {
				return false;
			}
		} else if (other.getMinValue() != getMinValue()) {
			return false;
		}

		return true;
	}

	@Override
	public boolean definesUpperLowerBounds() {
		return true;
	}

}
