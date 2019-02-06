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

import java.util.Iterator;


/**
 * This iterator iterates over all examples of an example set and creates
 * {@link com.rapidminer.datatable.CorrelationMatrixRow2DataTableRowWrapper} objects.
 * 
 * @author Ingo Mierswa
 */
public class CorrelationMatrixRow2DataTableRowIterator implements Iterator<DataTableRow> {

	private NumericalMatrix matrix;

	private int currentRow;

	/**
	 * Creates a new DataTable iterator backed up by examples. If the idAttribute is null the
	 * DataTableRows will not be able to deliver an Id.
	 */
	public CorrelationMatrixRow2DataTableRowIterator(NumericalMatrix matrix) {
		this.matrix = matrix;
		this.currentRow = 0;
	}

	@Override
	public boolean hasNext() {
		return currentRow < matrix.getNumberOfRows();
	}

	@Override
	public DataTableRow next() {
		DataTableRow row = new CorrelationMatrixRow2DataTableRowWrapper(matrix, currentRow);
		currentRow++;
		return row;
	}

	@Override
	public void remove() {
		throw new RuntimeException("CorrelationMatrixRow2DataTableRowIterator: removing rows is not supported!");
	}
}
