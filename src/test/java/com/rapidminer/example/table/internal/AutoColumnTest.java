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

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * Tests for the different auto columns: {@link DoubleAutoColumn} and
 * {@link DoubleIncompleteAutoColumn}.
 *
 * @author Gisa Schaefer
 * @since 7.3
 */
public class AutoColumnTest {

	@Test
	public void doubleAutoColumnStayDense() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 500;
		Column column = new DoubleAutoColumn(size - 400);
		for (int i = 0; i < size - 400; i++) {
			column.append(i);
		}
		column.ensure(size);
		for (int i = size - 400; i < size; i++) {
			column.append(i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleAutoColumnTwoDenseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + 1;
		Column column = new DoubleAutoColumn(size);
		for (int i = 0; i < size; i++) {
			column.append(i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleAutoColumnTwoSparseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		DoubleAutoColumn column = new DoubleAutoColumn(size);
		for (int i = 0; i < size; i++) {
			column.append(0.123);
		}
		assertEquals(0.123, column.get(99), 0);
		assertEquals(0.123, column.get(size - 1), 0);
	}

	@Test
	public void doubleAutoColumnSparse() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleAutoColumn(size);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < size; i++) {
			column.append(i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleAutoColumnSparse2ndWrite() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleAutoColumn(size);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < size; i++) {
			column.append(i);
		}
		// up to here it is sparse
		// now overwriting values and changing back to dense
		for (int i = 0; i < 10; i++) {
			column.set(i, i);
		}

		for (int i = 0; i < 10; i++) {
			assertEquals(i, column.get(i), 0);
		}
		// check that everything is copied from sparse back to dense
		for (int i = 10; i < change; i++) {
			assertEquals(1, column.get(i), 0);
		}
		for (int i = change; i < size; i++) {
			assertEquals(i, column.get(i), 0);
		}
	}

	@Test
	public void doubleAutoColumnSparseAndBack() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE * (1 - AutoColumnUtils.THRESHOLD_SPARSE_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		Column column = new DoubleAutoColumn(max);
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < max; i++) {
			column.append(i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
	}

	@Test
	public void doubleAutoColumnSparseAndBackEnsure() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE * (1 - AutoColumnUtils.THRESHOLD_SPARSE_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		Column column = new DoubleAutoColumn(max);
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < max; i++) {
			column.append(i);
		}
		column.ensure(max + 10);
		for (int i = max; i < max + 10; i++) {
			column.append(i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
		assertEquals(max + 10 - 1, column.get(max + 10 - 1), 0);
	}

	@Test
	public void doubleIncompleteColumnSparse2ndWrite() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleIncompleteAutoColumn(size);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < size; i++) {
			column.append(i);
		}
		// up to here it is sparse
		// now overwriting values and changing back to dense
		for (int i = 0; i < 10; i++) {
			column.set(i, i);
		}

		for (int i = 0; i < 10; i++) {
			assertEquals(i, column.get(i), 0);
		}
		// check that everything is copied from sparse back to dense
		for (int i = 10; i < change; i++) {
			assertEquals(1, column.get(i), 0);
		}
		for (int i = change; i < size; i++) {
			assertEquals(i, column.get(i), 0);
		}
	}

	@Test
	public void doubleIncompleteAutoColumnSparseAndBack() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE * (1 - AutoColumnUtils.THRESHOLD_SPARSE_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		Column column = new DoubleIncompleteAutoColumn(max);
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < max; i++) {
			column.append(i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
	}

	@Test
	public void doubleIncompleteAutoColumnSparseAndBackEnsure() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE * (1 - AutoColumnUtils.THRESHOLD_SPARSE_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		Column column = new DoubleIncompleteAutoColumn(max);
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < max; i++) {
			column.append(i);
		}
		column.ensure(max + 10);
		for (int i = max; i < max + 10; i++) {
			column.append(i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
		assertEquals(max + 10 - 1, column.get(max + 10 - 1), 0);
	}

	@Test
	public void doubleIncompleteAutoColumnStayDense() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 500;
		Column column = new DoubleIncompleteAutoColumn(size - 400);
		for (int i = 0; i < size - 400; i++) {
			column.append(i);
		}
		column.ensure(size);
		for (int i = size - 400; i < size; i++) {
			column.append(i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleIncompleteAutoColumnTwoDenseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + 1;
		Column column = new DoubleIncompleteAutoColumn(size);
		for (int i = 0; i < size; i++) {
			column.append(i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleIncompleteAutoColumnTwoSparseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new DoubleIncompleteAutoColumn(size);
		for (int i = 0; i < size; i++) {
			column.append(0.123);
		}
		assertEquals(0.123, column.get(99), 0);
		assertEquals(0.123, column.get(size - 1), 0);
	}

	@Test
	public void doubleIncompleteAutoColumnSparse() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleIncompleteAutoColumn(size);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < size; i++) {
			column.append(i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnStayDense() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 500;
		Column column = new IntegerIncompleteAutoColumn(size - 400);
		for (int i = 0; i < size - 400; i++) {
			column.append(i);
		}
		column.ensure(size);
		for (int i = size - 400; i < size; i++) {
			column.append(i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnTwoDenseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + 1;
		Column column = new IntegerIncompleteAutoColumn(size);
		for (int i = 0; i < size; i++) {
			column.append(i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnTwoSparseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerIncompleteAutoColumn(size);
		for (int i = 0; i < size; i++) {
			column.append(2);
		}
		assertEquals(2, column.get(99), 0);
		assertEquals(2, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnSparse() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerIncompleteAutoColumn(size);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < size; i++) {
			column.append(i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnSparse2ndWrite() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerIncompleteAutoColumn(size);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < size; i++) {
			column.append(i);
		}
		// up to here it is sparse
		// now overwriting values and changing back to dense
		for (int i = 0; i < 10; i++) {
			column.set(i, i);
		}

		for (int i = 0; i < 10; i++) {
			assertEquals(i, column.get(i), 0);
		}
		// check that everything is copied from sparse back to dense
		for (int i = 10; i < change; i++) {
			assertEquals(1, column.get(i), 0);
		}
		for (int i = change; i < size; i++) {
			assertEquals(i, column.get(i), 0);
		}
	}

	@Test
	public void integerIncompleteAutoColumnSparseAndBack() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE * (1 - AutoColumnUtils.THRESHOLD_SPARSE_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		Column column = new IntegerIncompleteAutoColumn(max);
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < max; i++) {
			column.append(i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnSparseAndBackEnsure() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE * (1 - AutoColumnUtils.THRESHOLD_SPARSE_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		Column column = new IntegerIncompleteAutoColumn(max);
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < max; i++) {
			column.append(i);
		}
		column.ensure(max + 10);
		for (int i = max; i < max + 10; i++) {
			column.append(i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
		assertEquals(max + 10 - 1, column.get(max + 10 - 1), 0);
	}

	@Test
	public void integerAutoColumnStayDense() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 500;
		Column column = new IntegerAutoColumn(size - 400);
		for (int i = 0; i < size - 400; i++) {
			column.append(i);
		}
		column.ensure(size);
		for (int i = size - 400; i < size; i++) {
			column.append(i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerAutoColumnTwoDenseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + 1;
		Column column = new IntegerAutoColumn(size);
		for (int i = 0; i < size; i++) {
			column.append(i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerAutoColumnTwoSparseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerAutoColumn(size);
		for (int i = 0; i < size; i++) {
			column.append(2);
		}
		assertEquals(2, column.get(99), 0);
		assertEquals(2, column.get(size - 1), 0);
	}

	@Test
	public void integerAutoColumnSparse() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerAutoColumn(size);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < size; i++) {
			column.append(i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerAutoColumnSparse2ndWrite() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerAutoColumn(size);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < size; i++) {
			column.append(i);
		}
		// up to here it is sparse
		// now overwriting values and changing back to dense
		for (int i = 0; i < 10; i++) {
			column.set(i, i);
		}

		for (int i = 0; i < 10; i++) {
			assertEquals(i, column.get(i), 0);
		}
		// check that everything is copied from sparse back to dense
		for (int i = 10; i < change; i++) {
			assertEquals(1, column.get(i), 0);
		}
		for (int i = change; i < size; i++) {
			assertEquals(i, column.get(i), 0);
		}
	}

	@Test
	public void integerAutoColumnSparseAndBack() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE * (1 - AutoColumnUtils.THRESHOLD_SPARSE_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		Column column = new IntegerAutoColumn(max);
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < max; i++) {
			column.append(i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
	}

	@Test
	public void integerAutoColumnSparseAndBackEnsure() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE * (1 - AutoColumnUtils.THRESHOLD_SPARSE_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_SPARSE_MAXIMAL_DENSITY)) + 1;
		Column column = new IntegerAutoColumn(max);
		for (int i = 0; i < change; i++) {
			column.append(1);
		}
		for (int i = change; i < max; i++) {
			column.append(i);
		}
		column.ensure(max + 10);
		for (int i = max; i < max + 10; i++) {
			column.append(i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
		assertEquals(max + 10 - 1, column.get(max + 10 - 1), 0);
	}

	@Test
	public void integerAutoColumnSparseNaN() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerAutoColumn(size);
		for (int i = 0; i < size - 2; i++) {
			column.append(2);
		}
		column.append(Double.NaN);
		column.append(Double.NaN);
		assertEquals(2, column.get(size - 4), 0);
		assertEquals(Double.NaN, column.get(size - 1), 0);
	}

	@Test
	public void integerAutoColumnSparseNaNDefault() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerAutoColumn(size);
		for (int i = 0; i < size - 2; i++) {
			column.append(Double.NaN);
		}
		column.append(2);
		column.append(2);
		assertEquals(Double.NaN, column.get(size - 4), 0);
		assertEquals(2, column.get(size - 1), 0);
	}

	@Test
	public void integerAutoColumnDenseNaN() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerAutoColumn(size);
		for (int i = 0; i < size - 2; i++) {
			column.append(i);
		}
		column.append(Double.NaN);
		column.append(Double.NaN);
		assertEquals(size - 4, column.get(size - 4), 0);
		assertEquals(Double.NaN, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnSparseNaN() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerIncompleteAutoColumn(size);
		for (int i = 0; i < size - 2; i++) {
			column.append(2);
		}
		column.append(Double.NaN);
		column.append(Double.NaN);
		assertEquals(2, column.get(size - 4), 0);
		assertEquals(Double.NaN, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnSparseNaNDefault() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerIncompleteAutoColumn(size);
		for (int i = 0; i < size - 2; i++) {
			column.append(Double.NaN);
		}
		column.append(2);
		column.append(2);
		assertEquals(Double.NaN, column.get(size - 4), 0);
		assertEquals(2, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnDenseNaN() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerIncompleteAutoColumn(size);
		for (int i = 0; i < size - 2; i++) {
			column.append(i);
		}
		column.append(Double.NaN);
		column.append(Double.NaN);
		assertEquals(size - 4, column.get(size - 4), 0);
		assertEquals(Double.NaN, column.get(size - 1), 0);
	}

}
