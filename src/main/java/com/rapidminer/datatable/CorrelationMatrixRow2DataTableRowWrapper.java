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

import com.rapidminer.operator.visualization.dependencies.NumericalMatrix;


/**
 * This class allows to use the rows of a
 * {@link com.rapidminer.operator.visualization.dependencies.NumericalMatrix} as basis for
 * {@link com.rapidminer.datatable.DataTableRow}.
 * 
 * @author Ingo Mierswa
 */
public class CorrelationMatrixRow2DataTableRowWrapper implements DataTableRow {

	private NumericalMatrix matrix;

	private int rowIndex;

	/** Creates a new wrapper. If the Id Attribute is null, the DataTableRow will not contain an Id. */
	public CorrelationMatrixRow2DataTableRowWrapper(NumericalMatrix matrix, int rowIndex) {
		this.matrix = matrix;
		this.rowIndex = rowIndex;
	}

	@Override
	public String getId() {
		return null;
	}

	@Override
	public double getValue(int index) {
		if (index == 0) {
			return rowIndex;
		} else {
			return this.matrix.getValue(rowIndex, index - 1);
		}
	}

	@Override
	public int getNumberOfValues() {
		return matrix.getNumberOfColumns() + 1;
	}
}
