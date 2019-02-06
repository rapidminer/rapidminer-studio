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
 * This class allows to use the entries of a
 * {@link com.rapidminer.operator.visualization.dependencies.NumericalMatrix} as basis for
 * {@link com.rapidminer.datatable.DataTableRow}.
 * 
 * @author Ingo Mierswa
 */
public class PairwiseMatrix2DataTableRowWrapper implements DataTableRow {

	private NumericalMatrix matrix;

	private int firstIndex;

	private int secondIndex;

	/** Creates a new wrapper. */
	public PairwiseMatrix2DataTableRowWrapper(NumericalMatrix matrix, int firstIndex, int secondIndex) {
		this.matrix = matrix;
		this.firstIndex = firstIndex;
		this.secondIndex = secondIndex;
	}

	@Override
	public String getId() {
		return "value(" + this.matrix.getColumnName(firstIndex) + ", " + this.matrix.getColumnName(secondIndex) + ") = "
				+ this.matrix.getValue(firstIndex, secondIndex);
	}

	@Override
	public double getValue(int index) {
		if (index == 0) {
			return firstIndex;
		} else if (index == 1) {
			return secondIndex;
		} else {
			return this.matrix.getValue(firstIndex, secondIndex);
		}
	}

	@Override
	public int getNumberOfValues() {
		return 3;
	}
}
