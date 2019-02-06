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
package com.rapidminer.example.table.internal;

import java.util.Arrays;

import com.rapidminer.example.table.internal.AutoColumnUtils.DensityResult;
import com.rapidminer.example.table.internal.IntegerIncompleteAutoColumn.IntegerIncompleteChunk;
import com.rapidminer.example.utils.ExampleSetBuilder.DataManagement;


/**
 * Dense {@link IntegerIncompleteChunk} for integer value data.
 *
 * @author Gisa Schaefer
 * @since 7.3.1
 */
final class IntegerIncompleteDenseChunk extends IntegerIncompleteChunk {

	private static final long serialVersionUID = 1L;

	private boolean undecided;
	private int ensuredSize;
	private int setCalls = 0;
	private int maxSetRow = 0;

	private int[] data = AutoColumnUtils.EMPTY_INTEGER_ARRAY;

	IntegerIncompleteDenseChunk(int id, IntegerIncompleteChunk[] chunks, int size, DataManagement management) {
		this(id, chunks, size, false, management);
	}

	IntegerIncompleteDenseChunk(int id, IntegerIncompleteChunk[] chunks, int size, boolean stayDense,
			DataManagement management) {
		super(id, chunks, management);
		this.undecided = !stayDense;
		ensure(size);
	}

	@Override
	double get(int row) {
		int value = data[row];
		return value == AutoColumnUtils.INTEGER_NAN ? Double.NaN : value;
	}

	@Override
	void set(int row, double value) {
		data[row] = Double.isNaN(value) ? AutoColumnUtils.INTEGER_NAN : (int) value;
		setCalls++;
		maxSetRow = Math.max(maxSetRow, row);

		if (row == AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE - 1 && undecided && isSetUsedToAppend()) {
			undecided = false;
			checkSparse();
		}
	}

	/**
	 * @return whether the calls {@link #set(int, double)} probably came in order
	 */
	private boolean isSetUsedToAppend() {
		return setCalls - 1 == maxSetRow;
	}

	@Override
	void ensure(int size) {
		ensuredSize = size;
		data = Arrays.copyOf(data, size);
	}

	/**
	 * Finds the most frequent value in the values set until now. If this value if frequent enough,
	 * it changes to a sparse representation.
	 */
	private void checkSparse() {
		DensityResult result = AutoColumnUtils.checkDensity(data);
		double thresholdDensity = management == DataManagement.AUTO ? AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_DENSITY
				: AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_DENSITY;

		if (result.density < thresholdDensity) {
			double defaultValue = result.mostFrequentValue;
			IntegerIncompleteChunk sparse = new IntegerIncompleteSparseChunk(id, chunks, defaultValue, management);
			sparse.ensure(ensuredSize);
			boolean isNaN = Double.isNaN(defaultValue);
			for (int i = 0; i < AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE; i++) {
				double value = data[i];
				// only set non-default values
				if (isNaN ? value != AutoColumnUtils.INTEGER_NAN : value != defaultValue) {
					sparse.set(i, value);
				}
			}
			chunks[id] = sparse;
		}
	}
}
