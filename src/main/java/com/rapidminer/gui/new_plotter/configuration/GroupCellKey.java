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

import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;
import com.rapidminer.gui.new_plotter.utility.ValueRange;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Wrapper for a map of Dimension to ValueRange. Implements an equals() method which is based on
 * instance comparison of map entries instead of reference comparison.
 * 
 * What is a group cell? The grouping of several dimension axes results a multi dimensional matrix,
 * where each matrix cell contains data. Each cell is identified by exactly one group of each
 * dimension. A group in turn is resembled by a ValueRange object. Thus a map from Dimension to
 * ValueRange resembles the key of this multi dimensional matrix.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class GroupCellKey implements Cloneable {

	Map<PlotDimension, ValueRange> groupCell;

	public GroupCellKey() {
		groupCell = new HashMap<DimensionConfig.PlotDimension, ValueRange>();
	}

	public GroupCellKey(Map<PlotDimension, ValueRange> groupCell) {
		this.groupCell = groupCell;
	}

	public ValueRange getRangeForDimension(PlotDimension d) {
		return groupCell.get(d);
	}

	public void setRangeForDimension(PlotDimension d, ValueRange range) {
		groupCell.put(d, range);
	}

	public void removeRangeForDimension(PlotDimension d) {
		groupCell.remove(d);
	}

	public int size() {
		return groupCell.size();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof GroupCellKey)) {
			return false;
		}

		GroupCellKey otherKey = (GroupCellKey) obj;
		Map<?, ?> otherMap = otherKey.groupCell;

		if (otherMap.size() != groupCell.size()) {
			return false;
		}

		for (Entry<PlotDimension, ValueRange> entry : groupCell.entrySet()) {
			Object otherValue = otherMap.get(entry.getKey());
			Object thisValue = entry.getValue();
			if (!(thisValue == null ? otherValue == null : thisValue.equals(otherValue))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		return groupCell.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GroupCellKey: ");
		builder.append(groupCell.toString());
		return builder.toString();
	}

	public String getNiceString(PlotConfiguration plotConfiguration) {
		StringBuilder builder = new StringBuilder();
		boolean firstEntry = true;
		for (Entry<PlotDimension, ValueRange> entry : groupCell.entrySet()) {
			ValueRange range = entry.getValue();
			if (range != null) {
				if (firstEntry) {
					firstEntry = false;
				} else {
					builder.append("; ");
				}
				PlotDimension dimension = entry.getKey();
				String dimensionAttribute = plotConfiguration.getDimensionConfig(dimension).getDataTableColumn().getName();
				builder.append(dimensionAttribute);
				builder.append(" = ");
				builder.append(range.toString());
			}
		}
		return builder.toString();
	}

	@Override
	public Object clone() {
		return new GroupCellKey(DataStructureUtils.getMapClone(groupCell));
	}
}
