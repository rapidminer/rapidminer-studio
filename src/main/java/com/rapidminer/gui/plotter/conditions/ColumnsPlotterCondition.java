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
package com.rapidminer.gui.plotter.conditions;

import com.rapidminer.datatable.DataTable;


/**
 * This condition accepts data tables with a number of columns in the specified range (including the
 * boundaries).
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColumnsPlotterCondition implements PlotterCondition {

	private int minColumns;
	private int maxColumns;

	public ColumnsPlotterCondition(int maxColumns) {
		this(0, maxColumns);
	}

	public ColumnsPlotterCondition(int minColumns, int maxColumns) {
		this.minColumns = minColumns;
		this.maxColumns = maxColumns;
	}

	@Override
	public boolean acceptDataTable(DataTable dataTable) {
		int numberOfColumns = dataTable.getNumberOfColumns();
		if ((numberOfColumns >= minColumns) && (numberOfColumns <= maxColumns)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String getRejectionReason(DataTable dataTable) {
		return "Data table must have between " + minColumns + " and " + maxColumns + " columns, was "
				+ dataTable.getNumberOfColumns() + ".";
	}
}
