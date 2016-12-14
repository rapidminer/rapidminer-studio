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
package com.rapidminer.example.table.internal;

import com.rapidminer.example.table.internal.IntegerAutoColumn.IntegerAutoChunk;


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

	IntegerAutoSparseChunk(int id, IntegerAutoChunk[] chunks, double defaultValue) {
		super(id, chunks);
		sparse = new IntegerSparseChunk(defaultValue);
	}

	@Override
	double get(int row) {
		return sparse.get(row);
	}

	@Override
	void set(int row, double value) {
		maxSetRow = Math.max(row, maxSetRow);
		if (sparse.set(row, value)) {
			changeToDense(maxSetRow);
		}
	}

	@Override
	void ensure(int size) {
		ensuredSize = size;
		sparse.ensure(size);
	}

	@Override
	void setLast(int row, double value) {
		maxSetRow = row;
		if (sparse.set(row, value)) {
			changeToDense(row);
		}
	}

	private void changeToDense(int max) {
		IntegerAutoChunk dense = new IntegerAutoDenseChunk(id, chunks, ensuredSize);
		dense.complete();
		for (int i = 0; i <= max; i++) {
			dense.set(i, get(i));
		}
		chunks[id] = dense;
	}

}
