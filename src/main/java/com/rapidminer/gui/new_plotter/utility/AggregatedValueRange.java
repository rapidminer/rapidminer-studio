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
package com.rapidminer.gui.new_plotter.utility;

import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.new_plotter.listener.events.ValueRangeChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueRangeChangeEvent.ValueRangeChangeType;

import java.util.LinkedList;
import java.util.List;


/**
 * A value range which holds a list of other value ranges. If one range in that list accepts a row,
 * also this value range accepts the row.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class AggregatedValueRange extends AbstractValueRange {

	private List<ValueRange> subRanges = new LinkedList<ValueRange>();

	public void addSubRange(ValueRange subRange) {
		subRanges.add(subRange);
		fireValueRangeChanged(new ValueRangeChangeEvent(this, ValueRangeChangeType.RESET));
	}

	@Override
	public boolean keepRow(DataTableRow row) {
		for (ValueRange subRange : subRanges) {
			if (subRange.keepRow(row)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns getValue() from the first subRange in the list of sub ranges, or NaN if the list is
	 * empty.
	 * 
	 * @see com.rapidminer.gui.new_plotter.utility.ValueRange#getValue()
	 */
	@Override
	public double getValue() {
		if (!subRanges.isEmpty()) {
			return subRanges.get(0).getValue();
		} else {
			return Double.NaN;
		}
	}

	/**
	 * Returns the result of definesUpperLowerBound() on the first subrange, or false if the list is
	 * empty.
	 * 
	 * @see com.rapidminer.gui.new_plotter.utility.ValueRange#definesUpperLowerBound()
	 */
	@Override
	public boolean definesUpperLowerBound() {
		if (!subRanges.isEmpty()) {
			return subRanges.get(0).definesUpperLowerBound();
		} else {
			return false;
		}
	}

	/**
	 * Returns the upper bound of the first sub range if applicable, or Double.NaN otherwise.
	 * 
	 * @see com.rapidminer.gui.new_plotter.utility.ValueRange#definesUpperLowerBound()
	 */
	@Override
	public double getUpperBound() {
		if (!subRanges.isEmpty()) {
			return subRanges.get(0).getUpperBound();
		} else {
			return Double.NaN;
		}
	}

	/**
	 * Returns the lower bound of the first sub range if applicable, or Double.NaN otherwise.
	 * 
	 * 
	 * @see com.rapidminer.gui.new_plotter.utility.ValueRange#getLowerBound()
	 */
	@Override
	public double getLowerBound() {
		if (!subRanges.isEmpty()) {
			return subRanges.get(0).getLowerBound();
		} else {
			return Double.NaN;
		}
	}

	@Override
	public ValueRange clone() {
		AggregatedValueRange clone = new AggregatedValueRange();
		for (ValueRange subRange : subRanges) {
			clone.addSubRange(subRange.clone());
		}
		return clone;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof AggregatedValueRange)) {
			return false;
		}

		AggregatedValueRange tempObj = (AggregatedValueRange) obj;

		if (tempObj.getLowerBound() != getLowerBound()) {
			return false;
		}

		if (tempObj.getUpperBound() != getUpperBound()) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Aggregated:{");
		for (ValueRange subRange : subRanges) {
			builder.append(subRange.toString()).append(",");
		}
		builder.append("}");
		return builder.toString();
	}
}
