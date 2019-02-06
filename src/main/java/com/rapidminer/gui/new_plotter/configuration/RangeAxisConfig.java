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

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.gui.new_plotter.ChartConfigurationException;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.StackingMode;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;
import com.rapidminer.gui.new_plotter.configuration.event.AxisParallelLinesConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.RangeAxisConfigListener;
import com.rapidminer.gui.new_plotter.listener.ValueRangeListener;
import com.rapidminer.gui.new_plotter.listener.ValueSourceListener;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.RangeAxisConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.RangeAxisConfigChangeEvent.RangeAxisConfigChangeType;
import com.rapidminer.gui.new_plotter.listener.events.ValueRangeChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueRangeChangeEvent.ValueRangeChangeType;
import com.rapidminer.gui.new_plotter.listener.events.ValueSourceChangeEvent;
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;
import com.rapidminer.gui.new_plotter.utility.ListUtility;
import com.rapidminer.gui.new_plotter.utility.NumericalValueRange;
import com.rapidminer.tools.I18N;


/**
 * This class represents a range axis of a plot. It contains a list of {@link ValueSource}s that are
 * displayed on the same range axis.
 * 
 * @author Nils Woehler, Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class RangeAxisConfig implements ValueSourceListener, ValueRangeListener, Cloneable,
		AxisParallelLinesConfigurationListener {

	private List<ValueSource> valueSourceList = new LinkedList<ValueSource>();
	private String name = "";

	private boolean autoNaming = false;
	private boolean logarithmicAxis = false;

	private NumericalValueRange userDefinedRange;
	private boolean useUserDefinedLowerBound;
	private boolean useUserDefinedUpperBound;

	private AxisParallelLinesConfiguration crosshairLines = new AxisParallelLinesConfiguration();

	public static final double padFactor = 0.05;
	public static final double logPadFactor = 0.15;

	private transient List<WeakReference<RangeAxisConfigListener>> listeners = new LinkedList<WeakReference<RangeAxisConfigListener>>();
	private final int Id;

	/**
	 * Constructor that sets the name of the range axis. If name is <code>null</code> auto naming
	 * will be enabled. If {@link SeriesFormat} is <code>null</code> the value sources default
	 * series format will be used.
	 */
	public RangeAxisConfig(String name, PlotConfiguration plotConfig) {
		this(name, plotConfig.getNextId());
	}

	/**
	 * Private C'tor that is used for cloning.
	 */
	private RangeAxisConfig(String name, int Id) {
		this.Id = Id;
		if (name == null) {
			this.autoNaming = true;
			setAutoLabelIfEnabled();
		} else {
			this.name = name;
		}
		userDefinedRange = new NumericalValueRange(0, 1, -1);
		userDefinedRange.addValueRangeListener(this);

		crosshairLines.addAxisParallelLinesConfigurationListener(this);
	}

	/**
	 * Returns the name of the {@link RangeAxisConfig}. Maybe <code>null</code>.
	 */
	public String getLabel() {
		return name;
	}

	/**
	 * Sets the name of the plot range axis
	 */
	public void setLabel(String name) {
		if (name == null ? name != this.name : !name.equals(this.name)) {
			this.name = name;
			fireLabelChanged();
		}
	}

	/**
	 * Adds a {@link PlotValueConfig} to the list of {@link ValueSource}s on this axis.
	 * 
	 * @param seriesFormat
	 *            if this is <code>null</code> the value sources default series format is used.
	 *            Otherwise this series format will be set for the new value source.
	 * 
	 */
	public void addValueSource(int index, ValueSource valueSource, SeriesFormat seriesFormat) {

		if (seriesFormat != null) {
			valueSource.setSeriesFormat(seriesFormat);
		}
		valueSourceList.add(index, valueSource);
		valueSource.addValueSourceListener(this);
		setAutoLabelIfEnabled();
		fireAdded(index, valueSource);
		if (getValueType() == ValueType.DATE_TIME) {
			setUseUserDefinedLowerViewBound(false);
			setUseUserDefinedUpperViewBound(false);
		}
	}

	/**
	 * Adds a {@link PlotValueConfig} to the list of {@link ValueSource}s on this axis.
	 * 
	 * It is mandatory that all {@link ValueSource}s have the same {@link ValueType}. Because of
	 * this a {@link ChartConfigurationException} is thrown if the {@link ValueType} of the
	 * {@link ValueSource} differs from other existing {@link ValueSource}'s {@link ValueType}s.
	 * 
	 * To check if adding a value source is possible call isAddingValueTypePossible(ValueType type).
	 * 
	 * @param adaptSeriesFormat
	 *            if this is <code>true</code> a new automatic created series format will be set for
	 *            the new value source
	 */
	public void addValueSource(ValueSource valueSource, SeriesFormat seriesFormat) { // throws
																						// ChartConfigurationException
																						// {
		addValueSource(valueSourceList.size(), valueSource, seriesFormat);
	}

	/**
	 * Removes a {@link ValueSource} from the list of plot value configs on this axis.
	 * 
	 * @param valueSource
	 */
	public void removeValueSource(ValueSource valueSource) {
		int idx = valueSourceList.indexOf(valueSource);
		removeValueSource(idx);
	}

	public void removeValueSource(int index) {
		ValueSource cachedValueSource = valueSourceList.get(index);
		cachedValueSource.removeValueSourceListener(this);
		valueSourceList.remove(index);
		setAutoLabelIfEnabled();
		fireRemoved(index, cachedValueSource);
		if (getValueType() == ValueType.DATE_TIME) {
			setUseUserDefinedLowerViewBound(false);
			setUseUserDefinedUpperViewBound(false);
		}
	}

	public void changeIndex(int index, ValueSource source) {
		if (ListUtility.changeIndex(valueSourceList, source, index)) {
			setAutoLabelIfEnabled();
			fireMoved(index, source);
		}
	}

	/**
	 * Returns all plot value configs on this range axis.
	 */
	public List<ValueSource> getValueSources() {
		return valueSourceList;
	}

	public void addRangeAxisConfigListener(RangeAxisConfigListener l) {
		listeners.add(new WeakReference<RangeAxisConfigListener>(l));
	}

	public void removeRangeAxisConfigListener(RangeAxisConfigListener l) {
		Iterator<WeakReference<RangeAxisConfigListener>> it = listeners.iterator();
		while (it.hasNext()) {
			RangeAxisConfigListener listener = it.next().get();
			if (listener == null || listener == l) {
				it.remove();
			}
		}
	}

	/**
	 * Unsubscripes all listeners from value sources and clears value source list afterwards.
	 */
	public void clearValueSources() {
		for (ValueSource valueSource : valueSourceList) {
			valueSource.removeAllListeners();
		}
		valueSourceList.clear();
		setAutoLabelIfEnabled();
		fireCleared();
	}

	/**
	 * Returns the {@link ValueType} of this {@link RangeAxisConfig}.
	 * 
	 * All {@link ValueSource}s mandatory have to be of the same {@link ValueType}. If that is not
	 * true, INVALID is returned.
	 * 
	 * If there are no {@link ValueSource}s UNKNOWN is returned.
	 * 
	 */
	public ValueType getValueType() {
		ValueType valueType = ValueType.UNKNOWN;

		for (ValueSource valueSource : getValueSources()) {
			ValueType valueSourceValueType = valueSource.getValueType();
			if (valueSourceValueType != valueType) {
				if (valueType == ValueType.UNKNOWN) {
					valueType = valueSourceValueType;
				} else {
					return ValueType.INVALID;
				}
			}
		}
		return valueType;
	}

	private void setAutoLabelIfEnabled() {
		if (isAutoNaming()) {
			StringBuilder stringBuilder = new StringBuilder();
			for (ValueSource valueSource : valueSourceList) {
				stringBuilder.append(valueSource.toString());
				stringBuilder.append(", ");
			}

			if (stringBuilder.toString().isEmpty()) {
				setLabel(I18N.getMessageOrNull(I18N.getGUIBundle(),
						"gui.label.plotter.configuration_dialog.empty_dimension.label"));
			} else {
				setLabel(stringBuilder.toString().substring(0, stringBuilder.toString().length() - 2));
			}
		}
	}

	public boolean hasAbsolutStackedPlot() {
		for (ValueSource valueSource : valueSourceList) {
			VisualizationType seriesType = valueSource.getSeriesFormat().getSeriesType();
			if (seriesType == VisualizationType.BARS || seriesType == VisualizationType.AREA) {
				if (valueSource.getSeriesFormat().getStackingMode() == StackingMode.ABSOLUTE) {
					return true;
				}
			}
		}
		return false;
	}

	public ValueSource getValueSourceById(int id) {
		for (ValueSource valueSource : getValueSources()) {
			if (valueSource.getId() == id) {
				return valueSource;
			}
		}

		return null;
	}

	/**
	 * @return the autoName
	 */
	public boolean isAutoNaming() {
		return autoNaming;
	}

	/**
	 * @param autoName
	 *            the autoName to set
	 */
	public void setAutoNaming(boolean autoName) {
		if (autoName != this.autoNaming) {
			this.autoNaming = autoName;
			setAutoLabelIfEnabled();
			fireRangeAxisChanged(new RangeAxisConfigChangeEvent(this, RangeAxisConfigChangeType.AUTO_NAMING, autoName));
		}
	}

	public void setUseUserDefinedUpperViewBound(boolean useUpperBound) {
		if (this.useUserDefinedUpperBound != useUpperBound) {
			this.useUserDefinedUpperBound = useUpperBound;
			fireValueRangeChanged(new ValueRangeChangeEvent(userDefinedRange, ValueRangeChangeType.USE_UPPER_BOUND,
					useUpperBound));
		}
	}

	public boolean isUsingUserDefinedUpperViewBound() {
		return useUserDefinedUpperBound;
	}

	public void setUseUserDefinedLowerViewBound(boolean useLowerBound) {
		if (this.useUserDefinedLowerBound != useLowerBound) {
			this.useUserDefinedLowerBound = useLowerBound;
			fireValueRangeChanged(new ValueRangeChangeEvent(userDefinedRange, ValueRangeChangeType.USE_LOWER_BOUND,
					useLowerBound));
		}
	}

	public boolean isUsingUserDefinedLowerViewBound() {
		return useUserDefinedLowerBound;
	}

	public int getSize() {
		return valueSourceList.size();
	}

	public NumericalValueRange getUserDefinedRange() {
		return userDefinedRange;
	}

	private void fireCleared() {
		fireRangeAxisChanged(new RangeAxisConfigChangeEvent(this));
	}

	private void fireMoved(int index, ValueSource valueSource) {
		fireRangeAxisChanged(new RangeAxisConfigChangeEvent(this, RangeAxisConfigChangeType.VALUE_SOURCE_MOVED, valueSource,
				index));

	}

	private void fireAdded(int index, ValueSource valueSource) {
		fireRangeAxisChanged(new RangeAxisConfigChangeEvent(this, RangeAxisConfigChangeType.VALUE_SOURCE_ADDED, valueSource,
				index));
	}

	private void fireRemoved(int index, ValueSource valueSource) {
		fireRangeAxisChanged(new RangeAxisConfigChangeEvent(this, RangeAxisConfigChangeType.VALUE_SOURCE_REMOVED,
				valueSource, index));
	}

	private void fireLabelChanged() {
		fireRangeAxisChanged(new RangeAxisConfigChangeEvent(this, getLabel()));
	}

	private void fireAxisScalingChanged() {
		fireRangeAxisChanged(new RangeAxisConfigChangeEvent(this, RangeAxisConfigChangeType.SCALING, logarithmicAxis));
	}

	private void fireValueRangeChanged(ValueRangeChangeEvent e) {
		fireRangeAxisChanged(new RangeAxisConfigChangeEvent(this, e));
	}

	private void fireRangeAxisChanged(RangeAxisConfigChangeEvent e) {
		Iterator<WeakReference<RangeAxisConfigListener>> it = listeners.iterator();
		while (it.hasNext()) {
			RangeAxisConfigListener listener = it.next().get();
			if (listener == null) {
				it.remove();
			} else {
				listener.rangeAxisConfigChanged(e);
			}
		}
	}

	public boolean isLogarithmicAxis() {
		return logarithmicAxis;
	}

	public void setLogarithmicAxis(boolean logarithmicAxis) {
		if (logarithmicAxis != this.logarithmicAxis) {
			this.logarithmicAxis = logarithmicAxis;
			fireAxisScalingChanged();
		}
	}

	private void usageTypeToColumnMapChange() {
		setAutoLabelIfEnabled();
	}

	public void domainDimensionConfigChanged(DimensionConfigChangeEvent e) {
		setAutoLabelIfEnabled();
	}

	private void aggregationFunctionChanged() {
		setAutoLabelIfEnabled();
	}

	@Override
	public void valueSourceChanged(ValueSourceChangeEvent e) {
		switch (e.getType()) {
			case AGGREGATION_FUNCTION_MAP:
				aggregationFunctionChanged();
				break;
			case DATATABLE_COLUMN_MAP:
				usageTypeToColumnMapChange();
				break;
			case SERIES_FORMAT_CHANGED:
				// SeriesFormatChangeEvent change = e.getSeriesFormatChange();
				// SeriesFormatChangeType type = change.getType();
				// if (type == SeriesFormatChangeType.UTILITY_INDICATOR || type ==
				// SeriesFormatChangeType.STACKING_MODE) {
				// invalidateCache();
				// }
				break;
			case USES_GROUPING:
				aggregationFunctionChanged();
				break;
			case AGGREGATION_WINDOWING_CHANGED:
				aggregationFunctionChanged();
				break;
			case USE_RELATIVE_UTILITIES:
				// invalidateCache();
				break;
			case UPDATED:
				// invalidateCache();
				break;
			case LABEL:
				setAutoLabelIfEnabled();
				break;
			default:
		}
		if (getValueType() == ValueType.DATE_TIME) {
			setUseUserDefinedLowerViewBound(false);
			setUseUserDefinedUpperViewBound(false);
		}
		fireRangeAxisChanged(new RangeAxisConfigChangeEvent(this, e));

	}

	@Override
	public void valueRangeChanged(ValueRangeChangeEvent e) {
		fireValueRangeChanged(e);
	}

	public boolean mustHaveUpperBoundOne(double initialUpperBound) {
		int relativePlots = 0;
		for (ValueSource valueSource : valueSourceList) {

			VisualizationType seriesType = valueSource.getSeriesFormat().getSeriesType();
			if (seriesType == VisualizationType.BARS || seriesType == VisualizationType.AREA) {
				if (valueSource.getSeriesFormat().getStackingMode() == StackingMode.RELATIVE) {
					++relativePlots;
				}
			}
		}

		boolean onlyRelativePlots = (relativePlots == valueSourceList.size());
		boolean hasRelativePlotsButUpperBoundBelowOne = (relativePlots > 0) && (initialUpperBound < 1.0);
		return onlyRelativePlots || hasRelativePlotsButUpperBoundBelowOne;
	}

	@Override
	public RangeAxisConfig clone() {
		RangeAxisConfig clone = new RangeAxisConfig(name, Id);

		clone.autoNaming = autoNaming;
		clone.logarithmicAxis = logarithmicAxis;
		clone.crosshairLines = crosshairLines.clone();
		clone.userDefinedRange = userDefinedRange.clone();
		clone.userDefinedRange.addValueRangeListener(clone);
		clone.useUserDefinedLowerBound = useUserDefinedLowerBound;
		clone.useUserDefinedUpperBound = useUserDefinedUpperBound;

		// add value sources
		for (ValueSource valueSource : valueSourceList) {
			clone.addValueSource(valueSource.clone(), null);
		}
		return clone;
	}

	public void setLowerViewBound(double lowerBound) {
		userDefinedRange.setLowerBound(lowerBound);
	}

	public void setUpperViewBound(double upperBound) {
		userDefinedRange.setUpperBound(upperBound);
	}

	public List<PlotConfigurationError> getErrors() {
		List<PlotConfigurationError> errors = new LinkedList<PlotConfigurationError>();

		String label = getLabel();

		if (label == null) {
			label = I18N.getGUILabel("plotter.unnamed_value_label");
		}

		if (getValueType() == ValueType.INVALID) {
			errors.add(new PlotConfigurationError("mixed_value_types_on_axis", label));
		}

		// check nominal mappings for compatibility
		List<ValueSource> valueSources = getValueSources();
		if (getValueType() == ValueType.NOMINAL) {
			String columnName = null;
			for (ValueSource valueSource : valueSources) {
				String valueSourceColumnName = valueSource.getLabel();
				if (valueSourceColumnName == null) {
					valueSourceColumnName = I18N.getGUILabel("plotter.unnamed_value_label");
				}
				if (columnName == null) {
					columnName = valueSourceColumnName;
				}
				if (columnName != null && !columnName.equals(valueSourceColumnName)) {
					errors.add(new PlotConfigurationError("mixed_categories_on_axis", label));
					break;
				}
			}
		}

		// check if lower user bound is bigger than upper bound
		if (isUsingUserDefinedLowerViewBound() && isUsingUserDefinedUpperViewBound()) {
			if (DataStructureUtils.greaterOrAlmostEqual(getUserDefinedRange().getLowerBound(), getUserDefinedRange()
					.getUpperBound(), 1E-7)) {
				PlotConfigurationError error = new PlotConfigurationError("min_bound_greater_max_bound", getLabel());
				errors.add(error);
			}
		}

		// check for mixed domain config value types
		ValueType valueType = ValueType.UNKNOWN;
		for (ValueSource valueSource : valueSources) {
			ValueType valueSourceValueType = valueSource.getDomainConfigValueType();
			if (valueSourceValueType != valueType) {
				if (valueType == ValueType.UNKNOWN) {
					valueType = valueSourceValueType;
				} else {
					PlotConfigurationError error = new PlotConfigurationError("mixed_domain_dimension_value_types_on_axis",
							getLabel());
					errors.add(error);
					break;
				}
			}
		}

		// add all errors from value sources
		for (ValueSource valueSource : valueSourceList) {
			errors.addAll(valueSource.getErrors());
		}

		return errors;
	}

	public List<PlotConfigurationError> getWarnings() {
		List<PlotConfigurationError> warnings = new LinkedList<PlotConfigurationError>();

		for (ValueSource valueSource : valueSourceList) {
			warnings.addAll(valueSource.getWarnings());
		}

		return warnings;
	}

	@Override
	public void axisParallelLineConfigurationsChanged(AxisParallelLinesConfigurationChangeEvent e) {
		fireCrosshairLinesChanged(e);
	}

	private void fireCrosshairLinesChanged(AxisParallelLinesConfigurationChangeEvent e) {
		fireRangeAxisChanged(new RangeAxisConfigChangeEvent(this, e));
	}

	public AxisParallelLinesConfiguration getCrossHairLines() {
		return crosshairLines;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return Id;
	}

	@Override
	public String toString() {
		return String.valueOf(getLabel());
	}
}
