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
package com.rapidminer.tools.usagestats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import com.rapidminer.tools.usagestats.ActionStatisticsCollector.Key;


/**
 * Presents statistics from an {@link ActionStatisticsCollector} as a {@link TableModel}.
 *
 * @author Simon Fischer
 *
 */
class ActionStatisticsTable implements TableModel {

	private Map<Key, Long> statistics;
	private List<Key> sortedKeys = new ArrayList<>();

	ActionStatisticsTable(Map<ActionStatisticsCollector.Key, Long> statistics) {
		this.statistics = statistics;
		sortedKeys.addAll(statistics.keySet());
		sortedKeys.sort((o1, o2) -> {
			int comp;
			comp = o1.getType().compareTo(o2.getType());
			if (comp != 0) {
				return comp;
			}
			comp = o1.getValue().compareTo(o2.getValue());
			if (comp != 0) {
				return comp;
			}
			if (o1.getArgWithIndicators() == null || o2.getArgWithIndicators() == null) {
				return 0;
			} else {
				return o1.getArgWithIndicators().compareTo(o2.getArgWithIndicators());
			}
		});
	}

	@Override
	public String getColumnName(int i) {
		switch (i) {
			case 0:
				return "Type";
			case 1:
				return "Value";
			case 2:
				return "Event";
			case 3:
				return "Count";
			default:
				throw new IllegalArgumentException("Illegal column: " + i);
		}
	}

	@Override
	public int getRowCount() {
		return sortedKeys.size();
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columnIndex < 3 ? String.class : Long.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		final Key key = sortedKeys.get(rowIndex);
		switch (columnIndex) {
			case 0:
				return key.getType();
			case 1:
				return key.getValue();
			case 2:
				return key.getArgWithIndicators();
			case 3:
				return statistics.get(key);
			default:
				throw new IllegalArgumentException("Illegal column: " + columnIndex);
		}
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		throw new UnsupportedOperationException("Table is immutable");
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		// Ignore listeners: no value changes anyway
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		// Ignore listeners: no value changes anyway
	}
}
