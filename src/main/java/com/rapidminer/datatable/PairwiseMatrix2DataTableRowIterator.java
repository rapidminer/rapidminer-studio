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
 * This iterator iterates over examples of a numerical matrix and creates
 * {@link com.rapidminer.datatable.CorrelationMatrixRow2DataTableRowWrapper} objects. If matrix is
 * symetrical, it will iterate only over the pairs of the lower left triangle of the matrix, sparing
 * the diagonal. Otherwise it will return all pairs.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public class PairwiseMatrix2DataTableRowIterator implements Iterator<DataTableRow> {

	private NumericalMatrix matrix;

	private int firstAttribute;

	private int secondAttribute;

	private boolean showSymetrically;

	/**
	 * Creates a new DataTable iterator for the given numerical matrix. If the idAttribute is null
	 * the DataTableRows will not be able to deliver an Id.
	 */
	public PairwiseMatrix2DataTableRowIterator(NumericalMatrix matrix) {
		this(matrix, true);
	}

	/**
	 * Creates a new iterator that will show the matrix symetrically only if matrix is symetrically
	 * and parameter showSymetrically is true.
	 */
	public PairwiseMatrix2DataTableRowIterator(NumericalMatrix matrix, boolean showSymetrically) {
		this.showSymetrically = showSymetrically;
		this.matrix = matrix;
		this.firstAttribute = 0;
		if (matrix.isSymmetrical()) {
			this.secondAttribute = 1;
		} else {
			this.secondAttribute = 0;
		}
	}

	@Override
	public boolean hasNext() {
		return (firstAttribute < matrix.getNumberOfRows()) && (secondAttribute < matrix.getNumberOfColumns());
	}

	@Override
	public DataTableRow next() {
		DataTableRow row = new PairwiseMatrix2DataTableRowWrapper(matrix, firstAttribute, secondAttribute);
		if (matrix.isSymmetrical() && showSymetrically) {
			secondAttribute++;
			if (secondAttribute >= matrix.getNumberOfColumns()) {
				firstAttribute++;
				secondAttribute = firstAttribute + 1;
			}
		} else {
			secondAttribute++;
			if (secondAttribute >= matrix.getNumberOfColumns()) {
				secondAttribute = 0;
				firstAttribute++;
			}
		}
		return row;
	}

	@Override
	public void remove() {
		throw new RuntimeException("PairwiseCorrelation2DataTableRowIterator: removing rows is not supported!");
	}
}
