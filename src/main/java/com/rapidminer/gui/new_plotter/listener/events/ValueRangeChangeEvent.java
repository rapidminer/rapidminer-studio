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

import com.rapidminer.gui.new_plotter.utility.ValueRange;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ValueRangeChangeEvent implements ConfigurationChangeEvent {

	public enum ValueRangeChangeType {
		UPPER_BOUND, LOWER_BOUND, USE_UPPER_BOUND, USE_LOWER_BOUND, RESET,
	}

	private final ValueRange source;
	private final ValueRangeChangeType type;

	private Double upperBound = null;
	private Double lowerBound = null;
	private boolean useLowerBound;
	private boolean useUpperBound;

	/**
	 * Allowed {@link ValueRangeChangeType}s are RESET or ABOUT_TO_CHANGE_AUTORANGE
	 */
	public ValueRangeChangeEvent(ValueRange source, ValueRangeChangeType type) {
		if ((type != ValueRangeChangeType.RESET)) {
			throw new RuntimeException(type + " is not allowed calling this constructor.");
		}
		this.source = source;
		this.type = type;
	}

	/**
	 * Allowed {@link ValueRangeChangeType}s are UPPER_BOUND or LOWER_BOUND
	 */
	public ValueRangeChangeEvent(ValueRange source, ValueRangeChangeType type, Double bound) {
		if ((type != ValueRangeChangeType.UPPER_BOUND) && (type != ValueRangeChangeType.LOWER_BOUND)) {
			throw new RuntimeException(type + " is not allowed calling this constructor.");
		}
		this.type = type;
		this.source = source;
		if (type == ValueRangeChangeType.UPPER_BOUND) {
			upperBound = bound;
		} else {
			lowerBound = bound;
		}
	}

	public ValueRangeChangeEvent(ValueRange source, ValueRangeChangeType type, boolean useBoundary) {
		if ((type != ValueRangeChangeType.USE_UPPER_BOUND) && (type != ValueRangeChangeType.USE_LOWER_BOUND)) {
			throw new RuntimeException(type + " is not allowed calling this constructor.");
		}
		this.type = type;
		this.source = source;
		if (type == ValueRangeChangeType.USE_LOWER_BOUND) {
			useLowerBound = useBoundary;
		} else {
			useUpperBound = useBoundary;
		}
	}

	/**
	 * @return the source
	 */
	public ValueRange getSource() {
		return source;
	}

	/**
	 * @return the type
	 */
	public ValueRangeChangeType getType() {
		return type;
	}

	/**
	 * @return the upperBound
	 */
	public Double getUpperBound() {
		return upperBound;
	}

	/**
	 * @return the lowerBound
	 */
	public Double getLowerBound() {
		return lowerBound;
	}

	@Override
	public ConfigurationChangeType getConfigurationChangeType() {
		return ConfigurationChangeType.VALUE_RANGE_CHANGE;
	}

	/**
	 * @return the useLowerBound
	 */
	public boolean getUseLowerBound() {
		return useLowerBound;
	}

	/**
	 * @return the useUpperBound
	 */
	public boolean getUseUpperBound() {
		return useUpperBound;
	}

	@Override
	public String toString() {
		return getType().toString();
	}
}
