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
import com.rapidminer.gui.new_plotter.ChartConfigurationException;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping.GroupingType;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping.ValueGroupingFactory;
import com.rapidminer.gui.new_plotter.configuration.event.AxisParallelLinesConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.DimensionConfigListener;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent.DimensionConfigChangeType;
import com.rapidminer.gui.new_plotter.utility.NumericalValueRange;
import com.rapidminer.gui.new_plotter.utility.ValueRange;

import java.text.DateFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;


/**
 * A class which manages the domain groupings of all {@link ValueSource}s in a
 * {@link PlotConfiguration}. This manager is necessary, because switching between grouping types is
 * not possible all the time because of some constraints. The DimensionConfigManager enforces these
 * constraints.
 *
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class DomainConfigManager extends AbstractDimensionConfig implements DimensionConfigListener, Cloneable,
		AxisParallelLinesConfigurationListener {

	public static enum Sorting {
		NONE, ASCENDING
	}

	public enum GroupingState {
		GROUPED, UNGROUPED, PARTIALLY_GROUPED
	}

	private static final Sorting DEFAULT_SORTING_MODE = Sorting.ASCENDING;

	private DefaultDimensionConfig ungroupedMasterDimensionConfig, groupedMasterDimensionConfig;
	private PlotConfiguration plotConfiguration;
	private Sorting sortingMode = DEFAULT_SORTING_MODE;
	private AxisParallelLinesConfiguration crosshairLines = new AxisParallelLinesConfiguration();
	private final int id;

	public DomainConfigManager(PlotConfiguration plotConfiguration, DataTableColumn domainColumn) {
		super(PlotDimension.DOMAIN);

		if (plotConfiguration == null) {
			throw new IllegalArgumentException("null plotConfiguration not allowed.");
		}

		this.id = plotConfiguration.getNextId();
		this.plotConfiguration = plotConfiguration;
		ungroupedMasterDimensionConfig = new DefaultDimensionConfig(plotConfiguration, domainColumn, getDimension());

		groupedMasterDimensionConfig = new DefaultDimensionConfig(plotConfiguration, domainColumn, getDimension());
		try {
			groupedMasterDimensionConfig.setGrouping(new EquidistantFixedBinCountBinning(5, Double.NaN, Double.NaN,
					domainColumn, domainColumn.isNominal(), getDateFormat()));
		} catch (ChartConfigurationException e) {
			groupedMasterDimensionConfig.setGrouping(new DistinctValueGrouping(domainColumn, domainColumn.isNominal(),
					getDateFormat()));
		}
		groupedMasterDimensionConfig.addDimensionConfigListener(this);
		crosshairLines.addAxisParallelLinesConfigurationListener(this);
	}

	/**
	 * Private ctor, used only by the clone method.
	 */
	private DomainConfigManager(int id) {
		super(PlotDimension.DOMAIN);
		ungroupedMasterDimensionConfig = null;
		groupedMasterDimensionConfig = null;
		plotConfiguration = null;
		crosshairLines.addAxisParallelLinesConfigurationListener(this);
		this.id = id;
	}

	public DefaultDimensionConfig getDomainConfig(boolean grouped) {
		if (grouped) {
			return groupedMasterDimensionConfig;
		} else {
			return ungroupedMasterDimensionConfig;
		}
	}

	public boolean isLogarithmicDomainAxis() {
		return groupedMasterDimensionConfig.isLogarithmic();
	}

	@Override
	public ValueRange getUserDefinedRangeClone(DataTable dataTable) {
		return groupedMasterDimensionConfig.getUserDefinedRangeClone(dataTable);
	}

	public DataTableColumn getDomainColumn() {
		return ungroupedMasterDimensionConfig.getDataTableColumn();
	}

	@Override
	public PlotDimension getDimension() {
		return PlotDimension.DOMAIN;
	}

	@Override
	public DataTableColumn getDataTableColumn() {
		return groupedMasterDimensionConfig.getDataTableColumn();
	}

	@Override
	public ValueGrouping getGrouping() {
		return groupedMasterDimensionConfig.getGrouping();
	}

	@Override
	public String getLabel() {
		return groupedMasterDimensionConfig.getLabel();
	}

	@Override
	public boolean isAutoRangeRequired() {
		return groupedMasterDimensionConfig.isAutoRangeRequired();
	}

	@Override
	public boolean isLogarithmic() {
		return groupedMasterDimensionConfig.isLogarithmic();
	}

	@Override
	public boolean isAutoNaming() {
		return groupedMasterDimensionConfig.isAutoNaming();
	}

	@Override
	public void setDataTableColumn(DataTableColumn column) {

		if (column == null) {
			throw new IllegalArgumentException("Null Domain columns are not allowed");
		}

		if (column.isNominal()
				&& groupedMasterDimensionConfig.getGrouping().getGroupingType() != GroupingType.DISTINCT_VALUES) {
			setFireEvents(false);
			try {
				setGrouping(ValueGroupingFactory.getValueGrouping(GroupingType.DISTINCT_VALUES, getDataTableColumn(), true,
						getDateFormat()));
			} catch (ChartConfigurationException e) {
				throw new RuntimeException("Could not create grouping. This should not happen");
			}
			setFireEvents(true);
		}

		ungroupedMasterDimensionConfig.setDataTableColumn(column);
		groupedMasterDimensionConfig.setDataTableColumn(column);
	}

	public Sorting getSortingMode() {
		return sortingMode;
	}

	public void setSortingMode(Sorting sortingMode) {
		if (sortingMode != this.sortingMode) {
			this.sortingMode = sortingMode;
			fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, PlotDimension.DOMAIN, sortingMode));
		}
	}

	@Override
	public void setUserDefinedRange(NumericalValueRange range) {
		ungroupedMasterDimensionConfig.setUserDefinedRange(range);
		groupedMasterDimensionConfig.setUserDefinedRange(range);
	}

	@Override
	public void setLogarithmic(boolean logarithmic) {
		ungroupedMasterDimensionConfig.setLogarithmic(logarithmic);
		groupedMasterDimensionConfig.setLogarithmic(logarithmic);
	}

	@Override
	public void setAutoNaming(boolean autoNaming) {
		groupedMasterDimensionConfig.setAutoNaming(autoNaming);
	}

	@Override
	public void setLabel(String label) {
		groupedMasterDimensionConfig.setLabel(label);
	}

	@Override
	public List<PlotConfigurationError> getErrors() {
		// TODO check all preconditions
		// TODO additional checks if valueType is NOMINAL (compatible categories etc.)
		LinkedList<PlotConfigurationError> errors = new LinkedList<PlotConfigurationError>();

		DataTableColumn dataTableColumn = getDataTableColumn();
		if (dataTableColumn.getName() == null || dataTableColumn.getName().isEmpty()) {
			PlotConfigurationError error = new PlotConfigurationError("undefined_dimension", PlotDimension.DOMAIN.getName());
			errors.add(error);
			return errors;
		}

		// This is now check within each RangeAxis separately
		// if (getValueType() == ValueType.INVALID) {
		// PlotConfigurationError error = new PlotConfigurationError("illegal_domain_type",
		// getValueType(), ValueType.INVALID);
		// errors.add(error);
		// return errors;
		// }

		errors.addAll(groupedMasterDimensionConfig.getErrors());
		if (errors.size() > 0) {
			return errors;
		}
		errors.addAll(ungroupedMasterDimensionConfig.getErrors());

		return errors;
	}

	@Override
	public List<PlotConfigurationError> getWarnings() {
		LinkedList<PlotConfigurationError> warnings = new LinkedList<PlotConfigurationError>();

		warnings.addAll(groupedMasterDimensionConfig.getWarnings());
		if (warnings.size() > 0) {
			return warnings;
		}
		warnings.addAll(ungroupedMasterDimensionConfig.getWarnings());

		return warnings;
	}

	@Override
	public boolean isValid() {
		return getErrors().isEmpty();
	}

	@Override
	public ValueType getValueType() {
		ValueType valueType = ValueType.INVALID;
		List<ValueSource> allValueSources = plotConfiguration.getAllValueSources();
		if (allValueSources.isEmpty()) {
			if (isGrouping() && getDataTableColumn().getName().length() == 0) {
				return getGrouping().getDomainType();
			} else {
				return ValueType.UNKNOWN;
			}
		}

		for (ValueSource valueSource : allValueSources) {
			ValueType currentValueType = valueSource.getDomainConfig().getValueType();
			if (currentValueType != valueType) {
				if (valueType == ValueType.INVALID) {
					valueType = currentValueType;
				} else {
					// This implies that some valueSources have a different type on
					// the domain axis than others.
					// That results in an unplottable domain axis.
					return ValueType.INVALID;
				}
			}
		}
		return valueType;
	}

	@Override
	public boolean isNominal() {
		return getValueType() == ValueType.NOMINAL;
	}

	@Override
	public boolean isNumerical() {
		return getValueType() == ValueType.NUMERICAL;
	}

	@Override
	public boolean isDate() {
		return getValueType() == ValueType.DATE_TIME;
	}

	@Override
	public void setGrouping(ValueGrouping grouping) {
		groupedMasterDimensionConfig.setGrouping(grouping);
	}

	@Override
	public void setUpperBound(Double upperBound) {
		ungroupedMasterDimensionConfig.setUpperBound(upperBound);
		groupedMasterDimensionConfig.setUpperBound(upperBound);
	}

	@Override
	public void setLowerBound(Double lowerBound) {
		ungroupedMasterDimensionConfig.setLowerBound(lowerBound);
		groupedMasterDimensionConfig.setLowerBound(lowerBound);
	}

	@Override
	public Double getUserDefinedUpperBound() {
		return groupedMasterDimensionConfig.getUserDefinedUpperBound();
	}

	@Override
	public Double getUserDefinedLowerBound() {
		return groupedMasterDimensionConfig.getUserDefinedLowerBound();
	}

	/**
	 * Returns true if at least one ValueSource in the PlotConfiguration uses the domain axis
	 * grouping provided by this DomainConfigManager.
	 * 
	 * @see com.rapidminer.gui.new_plotter.configuration.DimensionConfig#isGrouping()
	 */
	@Override
	public boolean isGrouping() {
		for (ValueSource valueSource : plotConfiguration.getAllValueSources()) {
			if (valueSource.isUsingDomainGrouping()) {
				return true;
			}
		}
		return false;
	}

	public GroupingState getGroupingState() {
		int groupingCounter = 0;
		for (ValueSource valueSource : plotConfiguration.getAllValueSources()) {
			if (valueSource.isUsingDomainGrouping()) {
				++groupingCounter;
			}
		}
		if (groupingCounter == plotConfiguration.getAllValueSources().size()) {
			return GroupingState.GROUPED;
		}
		if (groupingCounter > 0) {
			return GroupingState.PARTIALLY_GROUPED;
		}

		return GroupingState.UNGROUPED;
	}

	@Override
	public void dimensionConfigChanged(DimensionConfigChangeEvent change) {
		DimensionConfigChangeEvent domainManagerEvent;
		DimensionConfigChangeType type = change.getType();
		switch (type) {
			case ABOUT_TO_CHANGE_GROUPING:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(), type);
				break;
			case AUTO_NAMING:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(), change.getAutoNaming(),
						type);
				break;
			case COLOR_PROVIDER:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(), change.getColorProvider());
				break;
			case COLOR_SCHEME:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(), type);
				break;
			case COLUMN:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(), change.getDataTableColumn());
				break;
			case CROSSHAIR_LINES_CHANGED:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(),
						change.getCrosshairLinesChange());
				break;
			case DATE_FORMAT_CHANGED:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDateFormat());
				break;
			case GROUPING_CHANGED:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(),
						change.getGroupingChangeEvent());
				break;
			case LABEL:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(), change.getLabel());
				break;
			case RANGE:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(),
						change.getValueRangeChangedEvent());
				break;
			case RESET:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(), type);
				break;
			case SCALING:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(), change.getLogarithmic(),
						type);
				break;
			case SHAPE_PROVIDER:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(), change.getShapeProvider());
				break;
			case SIZE_PROVIDER:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(), change.getSizeProvider());
				break;
			case SORTING:
				domainManagerEvent = new DimensionConfigChangeEvent(this, change.getDimension(), change.getSortingMode());
				break;
			default:
				throw new IllegalArgumentException("Should not happen. Unknown event typ " + type);
		}

		fireDimensionConfigChanged(domainManagerEvent);
	}

	@Override
	public Vector<GroupingType> getValidGroupingTypes() {
		GroupingType[] values = ValueGrouping.GroupingType.values();
		Vector<GroupingType> valueVector = new Vector<GroupingType>();
		for (int i = 0; i < values.length; ++i) {
			GroupingType type = values[i];
			if (getDataTableColumn().isNominal()) {
				if (type == GroupingType.EQUAL_DATA_FRACTION) {
					continue;
				}
				if (type == GroupingType.EQUIDISTANT_FIXED_BIN_COUNT) {
					continue;
				}
			}
			if (type == GroupingType.NONE) {
				continue;
			}
			valueVector.add(type);
		}
		return valueVector;
	}

	@Override
	public Set<ValueType> getSupportedValueTypes() {
		Set<ValueType> supportedTypes = new HashSet<DataTableColumn.ValueType>();
		supportedTypes.add(ValueType.NOMINAL);
		supportedTypes.add(ValueType.NUMERICAL);
		supportedTypes.add(ValueType.DATE_TIME);
		return supportedTypes;
	}

	@Override
	public boolean isUsingUserDefinedLowerBound() {
		return groupedMasterDimensionConfig.isUsingUserDefinedLowerBound();
	}

	@Override
	public boolean isUsingUserDefinedUpperBound() {
		return groupedMasterDimensionConfig.isUsingUserDefinedUpperBound();
	}

	@Override
	public void setUseUserDefinedUpperBound(boolean useUpperBound) {
		ungroupedMasterDimensionConfig.setUseUserDefinedUpperBound(useUpperBound);
		groupedMasterDimensionConfig.setUseUserDefinedUpperBound(useUpperBound);
	}

	@Override
	public void setUseUserDefinedLowerBound(boolean useLowerBound) {
		ungroupedMasterDimensionConfig.setUseUserDefinedLowerBound(useLowerBound);
		groupedMasterDimensionConfig.setUseUserDefinedLowerBound(useLowerBound);
	}

	/*
	 * Does not clone plotConfiguration and dataTable, but copies references for these fields.
	 */
	@Override
	public DomainConfigManager clone() {
		DomainConfigManager clone = new DomainConfigManager(getId());
		clone.plotConfiguration = plotConfiguration;
		clone.groupedMasterDimensionConfig = groupedMasterDimensionConfig.clone();
		clone.groupedMasterDimensionConfig.addDimensionConfigListener(clone);
		clone.ungroupedMasterDimensionConfig = ungroupedMasterDimensionConfig.clone();
		clone.crosshairLines = crosshairLines.clone();
		clone.sortingMode = sortingMode;
		return clone;
	}

	@Override
	public void colorSchemeChanged() {
		ungroupedMasterDimensionConfig.colorSchemeChanged();
		groupedMasterDimensionConfig.colorSchemeChanged();
	}

	public void setPlotConfiguration(PlotConfiguration plotConfiguration) {
		this.plotConfiguration = plotConfiguration;
	}

	@Override
	public void axisParallelLineConfigurationsChanged(AxisParallelLinesConfigurationChangeEvent e) {
		fireDimensionConfigChanged(new DimensionConfigChangeEvent(this, getDimension(), e));
	}

	public AxisParallelLinesConfiguration getCrosshairLines() {
		return crosshairLines;
	}

	@Override
	public DateFormat getDateFormat() {
		return groupedMasterDimensionConfig.getDateFormat();
	}

	@Override
	public void setUserDefinedDateFormatString(String formatString) {
		ungroupedMasterDimensionConfig.setUserDefinedDateFormatString(formatString);
		groupedMasterDimensionConfig.setUserDefinedDateFormatString(formatString);
	}

	@Override
	public String getUserDefinedDateFormatString() {
		return groupedMasterDimensionConfig.getUserDefinedDateFormatString();
	}

	@Override
	public void setUseUserDefinedDateFormat(boolean yes) {
		ungroupedMasterDimensionConfig.setUseUserDefinedDateFormat(yes);
		groupedMasterDimensionConfig.setUseUserDefinedDateFormat(yes);
	}

	@Override
	public boolean isUsingUserDefinedDateFormat() {
		return groupedMasterDimensionConfig.isUsingUserDefinedDateFormat();
	}

	@Override
	public int getId() {
		return this.id;
	}

	public void resetToDefaults() {
		DataTableColumn domainColumn = new DataTableColumn(null, ValueType.INVALID);
		setDataTableColumn(domainColumn);

		setSortingMode(DEFAULT_SORTING_MODE);
		while (!crosshairLines.getLines().isEmpty()) {
			crosshairLines.removeLine(crosshairLines.getLines().get(0));
		}

		setAutoNaming(true);
		try {
			setGrouping(new EquidistantFixedBinCountBinning(5, Double.NaN, Double.NaN, domainColumn,
					domainColumn.isNominal(), getDateFormat()));
		} catch (ChartConfigurationException e) {
			setGrouping(new DistinctValueGrouping(domainColumn, domainColumn.isNominal(), getDateFormat()));
		}
		setUserDefinedDateFormatString(DEFAULT_DATE_FORMAT_STRING);
		setUseUserDefinedDateFormat(DEFAULT_USE_USER_DEFINED_DATE_FORMAT);
		setUseUserDefinedLowerBound(false);
		setUseUserDefinedUpperBound(false);
		setLowerBound(DEFAULT_USER_DEFINED_LOWER_BOUND);
		setUpperBound(DEFAULT_USER_DEFINED_UPPER_BOUND);
		setLogarithmic(false);
	}
}
