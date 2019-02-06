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
package com.rapidminer.datatable;

import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;
import com.rapidminer.tools.container.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * A view on a dataTable which changes the mapping of nominal values such that nominal values or
 * ordered lexically ascending or descending with increasing index values.
 * 
 * Uses lazy initialization, i.e. the mapping is only created on first access.
 * 
 * @author Marius Helf
 * 
 */
public class NominalSortingDataTableMapping implements BidirectionalMappingProvider, DataTableListener {

	/**
	 * Maps values from the parent table to the new values.
	 */
	Map<Double, Double> parentToChildMapping = null;
	Map<Double, Double> childToParentMapping = null;
	private boolean ascending;
	private int columnIdx;
	private final DataTable originalDataTable;

	public NominalSortingDataTableMapping(ValueMappingDataTableView dataTable, int columnIdx, boolean ascending) {
		if (!dataTable.isNominal(columnIdx)) {
			throw new IllegalArgumentException("NominalSortingDataTableMapping can only work on nominal columns.");
		}
		this.ascending = ascending;
		this.columnIdx = columnIdx;
		this.originalDataTable = dataTable.getParent();
		originalDataTable.addDataTableListener(this, true);
	}

	private void updateMapping(DataTable dataTable, int columnIdx, boolean ascending) {
		List<Double> distinctValues = DataStructureUtils.getDistinctDataTableValues(dataTable, columnIdx);
		List<Pair<Double, String>> valueStrings = new LinkedList<Pair<Double, String>>();
		for (double value : distinctValues) {
			valueStrings.add(new Pair<Double, String>(value, dataTable.mapIndex(columnIdx, (int) value)));
		}

		Collections.sort(valueStrings, new DataStructureUtils.PairComparator<Double, String>(ascending));

		parentToChildMapping = new HashMap<Double, Double>();
		childToParentMapping = new HashMap<Double, Double>();
		double idx = 0.0;
		for (Pair<Double, String> entry : valueStrings) {
			parentToChildMapping.put(entry.getFirst(), idx);
			childToParentMapping.put(idx, entry.getFirst());
			idx += 1;
		}
	}

	@Override
	public double mapFromParentValue(double originalValue) {
		if (parentToChildMapping == null) {
			updateMapping(originalDataTable, columnIdx, ascending);
		}
		return parentToChildMapping.get(originalValue);
	}

	@Override
	public void dataTableUpdated(DataTable source) {
		// invalidate mapping (will be recalculated on next access)
		parentToChildMapping = null;
		childToParentMapping = null;
	}

	@Override
	public double mapToParentValue(double value) {
		if (childToParentMapping == null) {
			updateMapping(originalDataTable, columnIdx, ascending);
		}
		return childToParentMapping.get(value);
	}
}
