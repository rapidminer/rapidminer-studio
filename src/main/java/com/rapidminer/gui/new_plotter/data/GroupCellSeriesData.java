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

import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.ValueSource.SeriesUsageType;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Contains a list of {@link GroupCellKeyAndData}.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class GroupCellSeriesData implements Iterable<GroupCellKeyAndData> {

	private List<GroupCellKeyAndData> groupCellSeriesData = new LinkedList<GroupCellKeyAndData>();

	public void addGroupCell(GroupCellKeyAndData groupCellKeyAndData) {
		groupCellSeriesData.add(groupCellKeyAndData);
	}

	public void clear() {
		groupCellSeriesData.clear();
	}

	public int groupCellCount() {
		return groupCellSeriesData.size();
	}

	public boolean isEmpty() {
		return groupCellSeriesData.isEmpty();
	}

	public Set<Double> getDistinctValues(SeriesUsageType usageType, PlotDimension dimension) {
		Set<Double> distinctValuesSet = new HashSet<Double>();
		for (GroupCellKeyAndData groupCellKeyAndData : groupCellSeriesData) {
			GroupCellData groupCellData = groupCellKeyAndData.getData();
			for (double value : groupCellData.getDataForUsageType(usageType).get(dimension)) {
				distinctValuesSet.add(value);
			}
		}
		return distinctValuesSet;
	}

	@Override
	public synchronized Iterator<GroupCellKeyAndData> iterator() {
		Iterator<GroupCellKeyAndData> i = null;
		synchronized (groupCellSeriesData) {
			i = groupCellSeriesData.iterator();
		}
		return i;
	}

	public GroupCellKeyAndData getGroupCellKeyAndData(int seriesIdx) {
		return groupCellSeriesData.get(seriesIdx);
	}
}
