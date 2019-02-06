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
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;
import com.rapidminer.gui.new_plotter.utility.SingleValueValueRange;
import com.rapidminer.gui.new_plotter.utility.ValueRange;

import java.text.DateFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Creates one group for each distinct value in the data. No matter if it is nominal or numeric.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class DistinctValueGrouping extends AbstractValueGrouping {

	private final GroupingType type = GroupingType.DISTINCT_VALUES;

	public DistinctValueGrouping(DataTableColumn dataTableColumn, boolean isCategorical, DateFormat dateFormat) {
		super(dataTableColumn, isCategorical, dateFormat);
	}

	/**
	 * Copy ctor.
	 */
	public DistinctValueGrouping(DistinctValueGrouping other) {
		super(other.getDataTableColumn(), other.isCategorical(), other.getDateFormat());
	}

	@Override
	protected List<ValueRange> createGroupingModel(DataTable dataTable, double upperBound, double lowerBound) {
		List<Double> sortedValues = new LinkedList<Double>();

		int columnIdx = DataTableColumn.getColumnIndex(dataTable, getDataTableColumn());
		sortedValues.addAll(DataStructureUtils.getDistinctDataTableValues(dataTable, columnIdx));

		Collections.sort(sortedValues);

		List<ValueRange> groupingModel = new LinkedList<ValueRange>();
		for (Double value : sortedValues) {
			if (value >= lowerBound && value <= upperBound) {
				groupingModel.add(new SingleValueValueRange(value, dataTable, columnIdx));
			}
		}

		if (!getDataTableColumn().isNominal()) {
			applyAdaptiveVisualRounding(groupingModel, getDataTableColumn().isDate());
		}

		return groupingModel;
	}

	@Override
	protected void applyAdaptiveVisualRounding(List<ValueRange> valueGroups, boolean valuesAreDates) {
		DateFormat dateFormat = getDateFormat();
		if (valuesAreDates) {
			for (ValueRange valueGroup : valueGroups) {
				SingleValueValueRange r = (SingleValueValueRange) valueGroup;
				r.setDateFormat(dateFormat);
			}
		} else {

			// apply adaptive rounding
			SingleValueValueRange previous = null;
			SingleValueValueRange current = null;
			SingleValueValueRange next = null;
			for (ValueRange valueGroup : valueGroups) {
				next = (SingleValueValueRange) valueGroup;
				if (previous != null) {
					int precisionLower = DataStructureUtils.getOptimalPrecision(previous.getValue(), current.getValue());
					int precisionUpper = DataStructureUtils.getOptimalPrecision(current.getValue(), next.getValue());
					int precision = Math.min(precisionLower, precisionUpper);
					current.setVisualPrecision(precision);
				} else if (current != null) {
					int precisionUpper = DataStructureUtils.getOptimalPrecision(current.getValue(), next.getValue());
					int precision = precisionUpper;
					current.setVisualPrecision(precision);
				}
				previous = current;
				current = next;
			}

			if (previous != null) {
				// even if eclipse states that this code is dead, it is not! (eclipse bug)
				int precisionLower = DataStructureUtils.getOptimalPrecision(previous.getValue(), current.getValue());
				int precision = precisionLower;
				next.setVisualPrecision(precision);
			}
		}
	}

	@Override
	public GroupingType getGroupingType() {
		return type;
	}

	@Override
	public ValueGrouping clone() {
		return new DistinctValueGrouping(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof DistinctValueGrouping)) {
			return false;
		}

		DistinctValueGrouping tempObj = (DistinctValueGrouping) obj;

		if (tempObj.isCategorical() != isCategorical()) {
			return false;
		}

		return true;
	}

	@Override
	public boolean definesUpperLowerBounds() {
		return false;
	}
}
