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
package com.rapidminer.gui.new_plotter.listener.events;

import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.configuration.event.AxisParallelLinesConfigurationChangeEvent;

import java.awt.Color;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class RangeAxisConfigChangeEvent implements ConfigurationChangeEvent {

	public enum RangeAxisConfigChangeType {
		VALUE_SOURCE_ADDED, // a value source was added
		VALUE_SOURCE_REMOVED, // a value source was removed
		VALUE_SOURCE_MOVED, // a value sources index was changed
		VALUE_SOURCE_CHANGED, CLEARED, // all value sources were removed
		LABEL, // range axis label has changed
		SCALING, // range axis scaling has changed
		AUTO_NAMING,  // auto naming has been toggled
		RANGE_CHANGED, // there has been some changed concerning the value range
		CROSSHAIR_LINES_CHANGED,
	}

	private final RangeAxisConfigChangeType type;
	private final RangeAxisConfig source;

	private ValueSource valueSource = null;
	private Integer index = null;

	private String label = null;

	private Boolean logarithmic = null;
	private Boolean includeZero = null;
	private Boolean autoNaming = null;

	private ValueSourceChangeEvent valueSourceChange = null;
	private Color rangeAxisLineColor;
	private Float rangeAxisLineWidth;

	private ValueRangeChangeEvent valueRangeChange = null;
	private AxisParallelLinesConfigurationChangeEvent crosshairChange;

	/**
	 * Creates a {@link RangeAxisConfigChangeEvent} with {@link RangeAxisConfigChangeType} CLEARED.
	 */
	public RangeAxisConfigChangeEvent(RangeAxisConfig source) {
		this.source = source;
		this.type = RangeAxisConfigChangeType.CLEARED;
	}

	/**
	 * Allowed {@link RangeAxisConfigChangeType}s are VALUE_SOURCE_ADDED, VALUE_SOURCE_REMOVED or
	 * VALUE_SOURCE_MOVED
	 */
	public RangeAxisConfigChangeEvent(RangeAxisConfig source, RangeAxisConfigChangeType type, ValueSource valueSource,
			Integer index) {
		this.type = type;
		if ((type != RangeAxisConfigChangeType.VALUE_SOURCE_ADDED)
				&& (type != RangeAxisConfigChangeType.VALUE_SOURCE_REMOVED)
				&& (type != RangeAxisConfigChangeType.VALUE_SOURCE_MOVED)) {
			throw new RuntimeException(type + " is not allowed calling this constructor.");
		}
		this.source = source;
		this.valueSource = valueSource;
		this.index = index;
	}

	public RangeAxisConfigChangeEvent(RangeAxisConfig source, String label) {
		this.type = RangeAxisConfigChangeType.LABEL;
		this.source = source;
		this.label = label;
	}

	/**
	 * Allowed {@link RangeAxisConfigChangeType}s are INCLUDE_ZERO, AUTO_NAMING or SCALING
	 */
	public RangeAxisConfigChangeEvent(RangeAxisConfig source, RangeAxisConfigChangeType type, Boolean bool) {
		this.type = type;
		if ((type != RangeAxisConfigChangeType.SCALING) && (type != RangeAxisConfigChangeType.AUTO_NAMING)) {
			throw new RuntimeException(type + " is not allowed calling this constructor.");
		}
		this.source = source;
		if (type == RangeAxisConfigChangeType.SCALING) {
			this.logarithmic = bool;
		} else {
			this.autoNaming = bool;
		}
	}

	public RangeAxisConfigChangeEvent(RangeAxisConfig source, ValueRangeChangeEvent valueRangeChange) {
		this.source = source;
		this.type = RangeAxisConfigChangeType.RANGE_CHANGED;
		this.valueRangeChange = valueRangeChange;
	}

	public RangeAxisConfigChangeEvent(RangeAxisConfig source, ValueSourceChangeEvent valueSourceChange) {
		this.source = source;
		this.type = RangeAxisConfigChangeType.VALUE_SOURCE_CHANGED;
		this.valueSourceChange = valueSourceChange;
	}

	public RangeAxisConfigChangeEvent(RangeAxisConfig rangeAxisConfig, AxisParallelLinesConfigurationChangeEvent e) {
		this.type = RangeAxisConfigChangeType.CROSSHAIR_LINES_CHANGED;
		this.source = rangeAxisConfig;
		this.crosshairChange = e;
	}

	public ValueSourceChangeEvent getValueSourceChange() {
		return valueSourceChange;
	}

	/**
	 * @return the valueSource
	 */
	public ValueSource getValueSource() {
		return valueSource;
	}

	/**
	 * @return the index
	 */
	public Integer getIndex() {
		return index;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the logarithmic
	 */
	public Boolean getLogarithmic() {
		return logarithmic;
	}

	/**
	 * @return the includeZero
	 */
	public Boolean getIncludeZero() {
		return includeZero;
	}

	/**
	 * @return the type
	 */
	public RangeAxisConfigChangeType getType() {
		return type;
	}

	/**
	 * @return the valueRangeChange
	 */
	public ValueRangeChangeEvent getValueRangeChange() {
		return valueRangeChange;
	}

	/**
	 * @return the source
	 */
	public RangeAxisConfig getSource() {
		return source;
	}

	@Override
	public ConfigurationChangeType getConfigurationChangeType() {
		return ConfigurationChangeType.RANGE_AXIS_CONFIG_CHANGE;
	}

	public Boolean getAutoNaming() {
		return autoNaming;
	}

	@Override
	public String toString() {
		return getType().toString();
	}

	/**
	 * @return
	 */
	public Color getRangeAxisLineColor() {
		return rangeAxisLineColor;
	}

	/**
	 * @return
	 */
	public Float getRangeAxisLineWidth() {
		return rangeAxisLineWidth;
	}

	public AxisParallelLinesConfigurationChangeEvent getCrosshairChange() {
		return crosshairChange;
	}
}
