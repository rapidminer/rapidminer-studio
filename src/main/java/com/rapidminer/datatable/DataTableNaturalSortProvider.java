/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import java.util.LinkedList;
import java.util.List;


/**
 * A sort provider which sorts the rows based on the values of one column. Nominal values are sorted
 * by the string values, all other column types use the double values for sorting.
 * 
 * @author Marius Helf
 * 
 */
public class DataTableNaturalSortProvider implements DataTableSortProvider {

	boolean ascending;
	int columnIdx;

	public DataTableNaturalSortProvider(int columnIdx, boolean ascending) {
		super();
		this.ascending = ascending;
		this.columnIdx = columnIdx;
	}

	@Override
	public int[] getIndexMapping(DataTable dataTable) {
		if (columnIdx >= 0 && columnIdx < dataTable.getColumnNumber()) {

			// get list of all values, either nominal or numeric
			List<Pair<Double, ? extends Comparable>> allValues = new LinkedList<Pair<Double, ? extends Comparable>>();
			if (dataTable.isNominal(columnIdx)) {
				double rowIdx = 0;
				for (DataTableRow row : dataTable) {
					allValues.add(new Pair<Double, Comparable>(rowIdx, row.getValue(columnIdx)));
					rowIdx += 1;
				}
			} else {
				double rowIdx = 0;
				for (DataTableRow row : dataTable) {
					allValues.add(new Pair<Double, Comparable>(rowIdx, row.getValue(columnIdx)));
					rowIdx += 1;
				}
			}

			// sort list
			Collections.sort(allValues, new DataStructureUtils.PairComparator(ascending));

			// create mapping from idx and values
			int[] indexMapping = new int[allValues.size()];
			int idx = 0;
			for (Pair<Double, ? extends Comparable> entry : allValues) {
				indexMapping[idx] = entry.getFirst().intValue();
				++idx;
			}
			return indexMapping;
		} else {
			int rowCount = dataTable.getRowNumber();
			int[] indexMapping = new int[rowCount];
			for (int i = 0; i < rowCount; ++i) {
				indexMapping[i] = i;
			}
			return indexMapping;
		}
	}

	@Override
	public DataTableNaturalSortProvider clone() {
		DataTableNaturalSortProvider clone = new DataTableNaturalSortProvider(columnIdx, ascending);
		return clone;
	}
}
