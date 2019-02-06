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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping.GroupingType;
import com.rapidminer.gui.new_plotter.listener.ValueGroupingListener;
import com.rapidminer.gui.new_plotter.listener.ValueRangeListener;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent.DimensionConfigChangeType;
import com.rapidminer.gui.new_plotter.listener.events.ValueGroupingChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueRangeChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueRangeChangeEvent.ValueRangeChangeType;
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;
import com.rapidminer.gui.new_plotter.utility.NumericalValueRange;
import com.rapidminer.gui.new_plotter.utility.ValueRange;


/**
 * A source for numerical values.
 * 
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class DefaultDimensionConfig extends AbstractDimensionConfig implements ValueGroupingListener, ValueRangeListener {

	private DataTableColumn dataTableColumn;
	private ValueGrouping valueGrouping;
	private NumericalValueRange userDefinedRange = null;

	private String dimensionAxisLabel = DEFAULT_AXIS_LABEL;

	private boolean logarithmicDimensionAxis = false;
	private boolean autoNaming = true;
	private boolean useUserDefinedLowerBound;
	private boolean useUserDefinedUpperBound;
	private String userDefinedDateFormatString = DEFAULT_DATE_FORMAT_STRING;
	private boolean useUserDefinedDateFormatString = DEFAULT_USE_USER_DEFINED_DATE_FORMAT;

	private final int Id;

	public DefaultDimensionConfig(PlotConfiguration plotConfiguration, DataTableColumn dataTableColumn,
			PlotDimension dimension) {
		this(dataTableColumn, dimension, plotConfiguration.getNextId());
	}

	/**
	 * Private C'tor that is used for cloning.
	 */
	private DefaultDimensionConfig(DataTableColumn dataTableColumn, PlotDimension dimension, int id) {
		super(dimension);
		this.Id = id;
		this.dataTableColumn = dataTableColumn;

		setUserDefinedRange(new NumericalValueRange(0, 1, -1, true, true));

		setAutoLabelIfEnabled();
	}

	@Override
	public void setGrouping(ValueGrouping valueGrouping) {

		if (!isGrouping() || (valueGrouping != null && valueGrouping.getGroupingType() != getGrouping().getGroupingType())
				|| valueGrouping != this.valueGrouping) {

			fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, getDimension(),
					DimensionConfigChangeType.ABOUT_TO_CHANGE_GROUPING));

			// update member variable
			if (this.valueGrouping != null) {
				this.valueGrouping.removeListener(this);
			}
			this.valueGrouping = valueGrouping;
			if (valueGrouping != null) {
				if (isLogarithmic() && valueGrouping.isCategorical()) {
					setLogarithmic(false);
				}
				valueGrouping.addListener(this);
			}

			fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, getDimension(), new ValueGroupingChangeEvent(
					valueGrouping)));
		}
	}

	@Override
	public ValueGrouping getGrouping() {
		return this.valueGrouping;
	}

	@Override
	public ValueRange getUserDefinedRangeClone(DataTable dataTable) {
		int columnIdx = DataTableColumn.getColumnIndex(dataTable, dataTableColumn);
		NumericalValueRange clone = userDefinedRange.clone();
		clone.setColumnIdx(columnIdx);
		return clone;
	}

	@Override
	public boolean isAutoRangeRequired() { // TODO handle dates

		// if valueType is nominal auto range is required
		if (isNominal()) {

			// if auto range isn't active, activate auto range
			if (isUsingUserDefinedLowerBound()) {
				setUseUserDefinedLowerBound(false);
			}

			if (isUsingUserDefinedUpperBound()) {
				setUseUserDefinedUpperBound(false);
			}

			return true;
		}
		return false;
	}

	@Override
	public void setLowerBound(Double lowerBound) {
		this.userDefinedRange.setLowerBound(lowerBound);
	}

	@Override
	public void setUpperBound(Double upperBound) {
		this.userDefinedRange.setUpperBound(upperBound);
	}

	@Override
	public void setUserDefinedRange(NumericalValueRange range) {
		if (range == null) {
			throw new RuntimeException("NULL value range is not allowed");
		}
		if (this.userDefinedRange != range) {
			if (this.userDefinedRange != null) {
				this.userDefinedRange.removeValueRangeListener(this);
			}

			this.userDefinedRange = range;

			range.addValueRangeListener(this);

			fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, getDimension(), new ValueRangeChangeEvent(
					userDefinedRange, ValueRangeChangeType.RESET)));
		}
	}

	@Override
	public boolean isGrouping() {
		return valueGrouping != null;
	}

	@Override
	public DataTableColumn getDataTableColumn() {
		return dataTableColumn;
	}

	@Override
	public void setDataTableColumn(DataTableColumn column) {
		if (dataTableColumn != column || !dataTableColumn.equals(column)) {
			setFireEvents(false);
			if (getGrouping() instanceof AbstractValueGrouping) {
				AbstractValueGrouping grouping = (AbstractValueGrouping) getGrouping();
				grouping.setDataTableColumn(column);
			}
			this.dataTableColumn = column;
			setAutoLabelIfEnabled();
			if (column.getValueType() == ValueType.DATE_TIME) {
				setUseUserDefinedLowerBound(false);
				setUseUserDefinedUpperBound(false);
			}
			setFireEvents(true);
			fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, getDimension(), column));
		}
	}

	@Override
	public boolean isValid() {
		return getErrors().isEmpty();
	}

	@Override
	public List<PlotConfigurationError> getErrors() {
		List<PlotConfigurationError> errorList = new LinkedList<PlotConfigurationError>();

		if (getDataTableColumn().getName() == null || getDataTableColumn().getName().isEmpty()) {
			PlotConfigurationError error = new PlotConfigurationError("undefined_dimension", PlotDimension.DOMAIN.getName());
			errorList.add(error);
			return errorList;
		}

		ValueType valueType = getValueType();
		if (!getSupportedValueTypes().contains(valueType)) {
			PlotConfigurationError error = new PlotConfigurationError("illegal_dimension_type", getDimension().getName(),
					valueType);
			errorList.add(error);
			return errorList;
		}

		// check if lower user bound is bigger than upper bound
		if (isUsingUserDefinedLowerBound() && isUsingUserDefinedUpperBound()) {
			if (DataStructureUtils.greaterOrAlmostEqual(getUserDefinedLowerBound(), getUserDefinedUpperBound(), 1E-6)) {
				PlotConfigurationError error = new PlotConfigurationError("min_filter_greater_max_filter", getDimension()
						.getName());
				errorList.add(error);
				return errorList;
			}
		}

		return errorList;
	}

	@Override
	public List<PlotConfigurationError> getWarnings() {
		List<PlotConfigurationError> warnings = new LinkedList<PlotConfigurationError>();

		if (isLogarithmic() && !isNumerical()) {
			if (!isNominal()) {
				// ignore nominal axes, since there logarithmic cannot be set and is not visible in
				// the gui
				warnings.add(new PlotConfigurationError("logarithmic_ignored_when_not_numerical", getDimension().getName()));
			}
		}

		// check if userDefinedDateFormatString holds a valid pattern (if it is used)
		if (useUserDefinedDateFormatString) {
			try {
				new SimpleDateFormat(userDefinedDateFormatString);
			} catch (IllegalArgumentException e) {
				// thrown when userDefinedDateFormatString is not a valid pattern

				warnings.add(new PlotConfigurationError("illegal_date_format_string", getDimension().getName()));
			}
		}

		return warnings;
	}

	@Override
	public boolean isNominal() {
		if (isGrouping()) {
			return getGrouping().isCategorical();
		} else {
			return getDataTableColumn().isNominal();
		}
	}

	@Override
	public void setLogarithmic(boolean logarithmic) {
		if (logarithmic != this.logarithmicDimensionAxis) {
			this.logarithmicDimensionAxis = logarithmic;

			fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, getDimension(), logarithmic,
					DimensionConfigChangeType.SCALING));
		}
	}

	@Override
	public boolean isLogarithmic() {
		return logarithmicDimensionAxis;
	}

	private void setAutoLabelIfEnabled() {
		if (isAutoNaming()) {
			if (dataTableColumn != null) {
				setLabel(dataTableColumn.getName());
			} else {
				setLabel(null);
			}
		}
	}

	@Override
	public boolean isAutoNaming() {
		return autoNaming;
	}

	@Override
	public void setAutoNaming(boolean autoNaming) {
		if (autoNaming != this.autoNaming) {
			this.autoNaming = autoNaming;
			setAutoLabelIfEnabled();
			fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, getDimension(), autoNaming,
					DimensionConfigChangeType.AUTO_NAMING));
		}
	}

	@Override
	public String getLabel() {
		return dimensionAxisLabel;
	}

	@Override
	public void setLabel(String label) {
		if (label == null || !label.equals(dimensionAxisLabel)) {
			this.dimensionAxisLabel = label;
			fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, getDimension(), label));
		}
	}

	@Override
	public boolean isNumerical() {
		if (isGrouping()) {
			return valueGrouping.getDomainType() == ValueType.NUMERICAL;
		} else {
			return dataTableColumn.isNumerical();
		}
	}

	@Override
	public DefaultDimensionConfig clone() {
		DefaultDimensionConfig clone = new DefaultDimensionConfig(getDataTableColumn().clone(), getDimension(), Id);

		ValueGrouping grouping = getGrouping();
		if (grouping != null) {
			clone.setGrouping(grouping.clone());
		}

		clone.autoNaming = autoNaming;
		clone.dimensionAxisLabel = dimensionAxisLabel;

		clone.logarithmicDimensionAxis = logarithmicDimensionAxis;

		clone.useUserDefinedLowerBound = useUserDefinedLowerBound;
		clone.useUserDefinedUpperBound = useUserDefinedUpperBound;
		clone.useUserDefinedDateFormatString = useUserDefinedDateFormatString;
		clone.userDefinedDateFormatString = userDefinedDateFormatString;

		clone.setUserDefinedRange(userDefinedRange.clone());

		// SortProvider sorting = getSorting();
		// if (sorting != null) {
		// configCopy.setSortProvider(sorting.clone());
		// }

		return clone;
	}

	@Override
	public ValueType getValueType() {
		if (isGrouping()) {
			return getGrouping().getDomainType();
		} else {
			return getDataTableColumn().getValueType();
		}
	}

	@Override
	public void valueGroupingChanged(ValueGroupingChangeEvent change) {
		fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, getDimension(), change));
	}

	@Override
	public void valueRangeChanged(ValueRangeChangeEvent change) {
		fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, getDimension(), change));
	}

	@Override
	public boolean isDate() {
		if (isGrouping()) {
			return valueGrouping.getDomainType() == ValueType.DATE_TIME;
		} else {
			return dataTableColumn.isDate();
		}
	}

	@Override
	public Double getUserDefinedUpperBound() {
		return userDefinedRange.getUpperBound();
	}

	@Override
	public Double getUserDefinedLowerBound() {
		return userDefinedRange.getLowerBound();
	}

	@Override
	public Vector<GroupingType> getValidGroupingTypes() {
		GroupingType[] values = ValueGrouping.GroupingType.values();
		Vector<GroupingType> validGroupings = new Vector<GroupingType>();
		for (int i = 0; i < values.length; ++i) {
			GroupingType type = values[i];
			if (dataTableColumn.isNominal()) {
				switch (type) {
					case EQUAL_DATA_FRACTION:
					case EQUIDISTANT_FIXED_BIN_COUNT:
						break;
					default:
						validGroupings.add(type);
				}
			} else {
				validGroupings.add(type);
			}
		}
		return validGroupings;
	}

	@Override
	public Set<ValueType> getSupportedValueTypes() {
		PlotDimension dimension = getDimension();
		Set<ValueType> valueTypes = new HashSet<DataTableColumn.ValueType>();
		switch (dimension) {
			case SHAPE:
				valueTypes.add(ValueType.NOMINAL);
				break;
			case COLOR:
			case DOMAIN:
			case SIZE:
				valueTypes.add(ValueType.NOMINAL);
				valueTypes.add(ValueType.NUMERICAL);
				valueTypes.add(ValueType.DATE_TIME);
				break;
			default:
		}
		return valueTypes;
	}

	@Override
	public boolean isUsingUserDefinedLowerBound() {
		return useUserDefinedLowerBound;
	}

	@Override
	public boolean isUsingUserDefinedUpperBound() {
		return useUserDefinedUpperBound;
	}

	@Override
	public void setUseUserDefinedUpperBound(boolean useUpperBound) {
		useUserDefinedUpperBound = useUpperBound;
		valueRangeChanged(new ValueRangeChangeEvent(userDefinedRange, ValueRangeChangeType.USE_UPPER_BOUND, useUpperBound));
	}

	@Override
	public void setUseUserDefinedLowerBound(boolean useLowerBound) {
		useUserDefinedLowerBound = useLowerBound;
		valueRangeChanged(new ValueRangeChangeEvent(userDefinedRange, ValueRangeChangeType.USE_LOWER_BOUND, useLowerBound));
	}

	@Override
	public void colorSchemeChanged() {
		fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, getDimension(),
				DimensionConfigChangeType.COLOR_SCHEME));
	}

	@Override
	public String toString() {
		return "DefaultDimensionConfig for " + getDimension().getName() + " ("
				+ (isGrouping() ? "grouping" : "not grouping") + ")";
	}

	@Override
	public DateFormat getDateFormat() {
		DateFormat dateFormat;
		if (useUserDefinedDateFormatString) {
			try {
				dateFormat = new SimpleDateFormat(userDefinedDateFormatString);
				dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			} catch (IllegalArgumentException e) {
				// thrown when userDefinedDateFormatString is not a valid pattern

				dateFormat = (DateFormat) DateFormat.getDateTimeInstance().clone();
				dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			}
		} else {
			dateFormat = (DateFormat) DateFormat.getDateTimeInstance().clone();
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		}
		return dateFormat;
	}

	@Override
	public void setUserDefinedDateFormatString(String formatString) {
		if (formatString != null ? !formatString.equals(this.userDefinedDateFormatString)
				: userDefinedDateFormatString != null) {
			this.userDefinedDateFormatString = formatString;
			if (valueGrouping != null) {
				valueGrouping.setDateFormat(getDateFormat());
			}
			fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, getDateFormat()));
		}
	}

	@Override
	public String getUserDefinedDateFormatString() {
		return userDefinedDateFormatString;
	}

	@Override
	public boolean isUsingUserDefinedDateFormat() {
		return useUserDefinedDateFormatString;
	}

	@Override
	public void setUseUserDefinedDateFormat(boolean yes) {
		if (yes != useUserDefinedDateFormatString) {
			this.useUserDefinedDateFormatString = yes;
			if (valueGrouping != null) {
				valueGrouping.setDateFormat(getDateFormat());
			}
			fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, getDateFormat()));
		}
	}

	/**
	 * @return the id
	 */
	@Override
	public int getId() {
		return Id;
	}
}
