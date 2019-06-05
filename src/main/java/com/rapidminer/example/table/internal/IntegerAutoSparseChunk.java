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

import com.rapidminer.example.table.internal.IntegerAutoColumn.IntegerAutoChunk;
import com.rapidminer.example.utils.ExampleSetBuilder.DataManagement;


/**
 * Sparse {@link IntegerAutoChunk} for integer value data inside a {@link IntegerAutoColumn}.
 *
 * @author Gisa Schaefer
 * @since 7.3.1
 */
final class IntegerAutoSparseChunk extends IntegerAutoChunk {

	private static final long serialVersionUID = 1L;

	private final IntegerSparseChunk sparse;
	private int ensuredSize;
	private int maxSetRow;
	private boolean testingDefaultValue;

	IntegerAutoSparseChunk(int id, IntegerAutoChunk[] chunks, double defaultValue, DataManagement management) {
		super(id, chunks, management);
		if (management == DataManagement.AUTO) {
			sparse = new IntegerHighSparsityChunk(defaultValue);
		} else {
			sparse = new IntegerMediumSparsityChunk(defaultValue);
		}
	}

	/**
	 * When the default value was guessed instead of calculated from the first
	 * {@link AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE} rows, call this to ensure that the guessed
	 * value is the most common value wrt. the first
	 * {@link AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE} rows.
	 */
	void hasGuessedDefault() {
		testingDefaultValue = true;
	}

	@Override
	double get(int row) {
		return sparse.get(row);
	}

	@Override
	void set(int row, double value) {
		maxSetRow = Math.max(row, maxSetRow);
		if (testingDefaultValue && row >= AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE) {
			// we have more then 2048 rows now, so the guessed default value was really the most
			// common one wrt. the first 2048 rows
			sparse.ensure(ensuredSize);
			testingDefaultValue = false;
		}
		if (sparse.set(row, value)) {
			changeToDense(maxSetRow);
		}
	}

	@Override
	void ensure(int size) {
		ensuredSize = size;
		if (testingDefaultValue) {
			// if we are in sparse mode with a guessed default value, we need to ensure that it
			// is really the most common value wrt. the first 2048 values
			sparse.ensure(Math.min(size, AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE));
		} else {
			sparse.ensure(size);
		}
	}

	@Override
	void setLast(int row, double value) {
		maxSetRow = row;
		if (testingDefaultValue && row >= AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE) {
			// we have more then 2048 rows now, so the guessed default value was really the most
			// common one wrt. the first 2048 rows
			sparse.ensure(ensuredSize);
			testingDefaultValue = false;
		}
		if (sparse.set(row, value)) {
			changeToDense(row);
		}
	}

	private void changeToDense(int max) {
		IntegerAutoChunk dense = new IntegerAutoDenseChunk(id, chunks, ensuredSize, management);
		// allow to change back to sparse when default was guessed except when at the threshold
		if (!testingDefaultValue || max == AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE - 1) {
			dense.complete();
		}
		for (int i = 0; i <= max; i++) {
			dense.set(i, get(i));
		}
		chunks[id] = dense;
	}

}
