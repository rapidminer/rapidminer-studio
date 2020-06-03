/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.gui.viewer.metadata.model;

import java.util.Map;

import org.jfree.chart.JFreeChart;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Statistics.Result;
import com.rapidminer.belt.column.Statistics.Statistic;
import com.rapidminer.belt.table.Table;
import com.rapidminer.gui.viewer.metadata.BeltColumnStatisticsPanel;


/**
 * Model for {@link BeltColumnStatisticsPanel}s which are backed by an object {@link Column}.
 * 
 * @author Marco Boeck, Gisa Meier
 * @since 9.7.0
 */
public class BeltObjectColumnStatisticsModel extends AbstractBeltColumnStatisticsModel {

	/**
	 * Creates a new {@link BeltObjectColumnStatisticsModel}.
	 */
	public BeltObjectColumnStatisticsModel(Table table, String columnName) {
		super(table, columnName);

	}

	@Override
	public void updateStatistics(Map<String, Map<Statistic, Result>> allStatistics) {
		Map<Statistic, Result> statistics = allStatistics.get(getColumnName());
		missing = getTableOrNull().height() - statistics.get(Statistic.COUNT).getNumeric();

		fireStatisticsChangedEvent();
	}

	@Override
	public Type getType() {
		return Type.OTHER_OBJECT;
	}

	@Override
	public JFreeChart getChartOrNull(int index) {
		//no charts supported
		return null;
	}

	@Override
	public void prepareCharts() {
		//no charts supported
	}



}
