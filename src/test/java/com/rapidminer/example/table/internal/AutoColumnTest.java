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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.rapidminer.example.utils.ExampleSetBuilder.DataManagement;


/**
 * Tests for the different auto columns: {@link DoubleAutoColumn}, {@link IntegerAutoColumn},
 * {@link IntegerIncompleteAutoColumn} and {@link DoubleIncompleteAutoColumn}.
 *
 * @author Gisa Schaefer
 * @since 7.3
 */
public class AutoColumnTest {

	@Test
	public void doubleAutoColumnStayDense() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 500;
		Column column = new DoubleAutoColumn(size - 400, DataManagement.AUTO);
		for (int i = 0; i < size - 400; i++) {
			column.setLast(i, i);
		}
		column.ensure(size);
		for (int i = size - 400; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleAutoColumnTwoDenseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + 1;
		Column column = new DoubleAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleAutoColumnEnsure() {
		Column column = new DoubleAutoColumn(1, DataManagement.AUTO);
		column.ensure(AutoColumnUtils.CHUNK_SIZE * 2 + 1);
	}

	@Test
	public void doubleAutoColumnTwoSparseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		DoubleAutoColumn column = new DoubleAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size; i++) {
			column.setLast(i, 0.123);
		}
		assertEquals(0.123, column.get(99), 0);
		assertEquals(0.123, column.get(size - 1), 0);
	}

	@Test
	public void doubleAutoColumnSparse() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleAutoColumn(size, DataManagement.AUTO);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleAutoColumnSparse2ndWrite() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleAutoColumn(size, DataManagement.AUTO);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
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
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		Column column = new DoubleAutoColumn(max, DataManagement.AUTO);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < max; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
	}

	@Test
	public void doubleAutoColumnSparseAndBackEnsure() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY));
		Column column = new DoubleAutoColumn(max, DataManagement.AUTO);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < max; i++) {
			column.setLast(i, i);
		}
		column.ensure(max + 10);
		for (int i = max; i < max + 10; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
		assertEquals(max + 10 - 1, column.get(max + 10 - 1), 0);
	}

	@Test
	public void doubleMemoryColumnStayDense() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 500;
		Column column = new DoubleAutoColumn(size - 400, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size - 400; i++) {
			column.setLast(i, i);
		}
		column.ensure(size);
		for (int i = size - 400; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleAutoColumnSparseToDenseThreshold() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		// chose values such that switch from sparse to dense happens at THRESHOLD_CHECK_FOR_SPARSE-1
		column.setLast(0, 1);
		for (int i = 1; i < AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE - 1; i++) {
			column.setLast(i, (i - 1) % 2);
		}
		for (int i = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE - 1; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleMemoryColumnTwoDenseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + 1;
		Column column = new DoubleAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleMemoryColumnTwoSparseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		DoubleAutoColumn column = new DoubleAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size; i++) {
			column.setLast(i, 0.123);
		}
		assertEquals(0.123, column.get(99), 0);
		assertEquals(0.123, column.get(size - 1), 0);
	}

	@Test
	public void doubleMemoryColumnSparse() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		int changeMin = (int) (size * (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_MAXIMAL_DENSITY)) + 1;
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_DENSITY)) + 1;
		change = Math.max(change, changeMin);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleMemoryColumnSparse2ndWrite() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		int changeMin = (int) (size * (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_MAXIMAL_DENSITY)) + 1;
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_DENSITY)) + 1;
		change = Math.max(change, changeMin);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
		}
		// up to here it is sparse
		// now overwriting values and changing back to dense
		for (int i = 0; i < 15; i++) {
			column.set(i, i);
		}

		for (int i = 0; i < 15; i++) {
			assertEquals(i, column.get(i), 0);
		}
		// check that everything is copied from sparse back to dense
		for (int i = 15; i < change; i++) {
			assertEquals(1, column.get(i), 0);
		}
		for (int i = change; i < size; i++) {
			assertEquals(i, column.get(i), 0);
		}
	}

	@Test
	public void doubleMemoryColumnSparseAndBack() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_MAXIMAL_DENSITY)) + 1;
		Column column = new DoubleAutoColumn(max, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i++) {
			column.setLast(i, i);
		}
		// here sparse, then back
		for (int i = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i < max; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
	}

	@Test
	public void doubleMemoryColumnSparseAndBackEnsure() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_MAXIMAL_DENSITY));
		Column column = new DoubleAutoColumn(max, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < max; i++) {
			column.setLast(i, i);
		}
		// here sparse, then back
		column.ensure(max + 10);
		for (int i = max; i < max + 10; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
		assertEquals(max + 10 - 1, column.get(max + 10 - 1), 0);
	}

	@Test
	public void doubleIncompleteMemoryColumnSparse2ndWrite() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleIncompleteAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		int changeMin = (int) (size * (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_MAXIMAL_DENSITY)) + 1;
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_DENSITY)) + 1;
		change = Math.max(change, changeMin);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
		}
		// up to here it is sparse
		// now overwriting values and changing back to dense
		for (int i = 0; i < 15; i++) {
			column.set(i, i);
		}

		for (int i = 0; i < 15; i++) {
			assertEquals(i, column.get(i), 0);
		}
		// check that everything is copied from sparse back to dense
		for (int i = 15; i < change; i++) {
			assertEquals(1, column.get(i), 0);
		}
		for (int i = change; i < size; i++) {
			assertEquals(i, column.get(i), 0);
		}
	}

	@Test
	public void doubleIncompleteMemoryColumnSparseAndBack() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_MAXIMAL_DENSITY)) + 1;
		Column column = new DoubleIncompleteAutoColumn(max, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i++) {
			column.setLast(i, i);
		}
		// sparse here, then back to dense
		for (int i = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i < max; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
	}

	@Test
	public void doubleIncompleteMemoryColumnSparseAndBackEnsure() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_MAXIMAL_DENSITY));
		Column column = new DoubleIncompleteAutoColumn(max, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < max; i++) {
			column.setLast(i, i);
		}
		// here sparse
		column.ensure(max + 10);
		for (int i = max; i < max + 10; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
		assertEquals(max + 10 - 1, column.get(max + 10 - 1), 0);
	}

	@Test
	public void doubleIncompleteMemoryColumnStayDense() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 500;
		Column column = new DoubleIncompleteAutoColumn(size - 400, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size - 400; i++) {
			column.setLast(i, i);
		}
		column.ensure(size);
		for (int i = size - 400; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleIncompleteAutoColumnSparseToDenseThreshold() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleIncompleteAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		// chose values such that switch from sparse to dense happens at THRESHOLD_CHECK_FOR_SPARSE-1
		column.setLast(0, 1);
		for (int i = 1; i < AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE - 1; i++) {
			column.setLast(i, (i - 1) % 2);
		}
		for (int i = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE - 1; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleIncompleteMemoryColumnTwoDenseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + 1;
		Column column = new DoubleIncompleteAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleIncompleteMemoryColumnTwoSparseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new DoubleIncompleteAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size; i++) {
			column.setLast(i, 0.123);
		}
		assertEquals(0.123, column.get(99), 0);
		assertEquals(0.123, column.get(size - 1), 0);
	}

	@Test
	public void doubleIncompleteMemoryColumnSparse() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleIncompleteAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		int changeMin = (int) (size * (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_MAXIMAL_DENSITY)) + 1;
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_DENSITY)) + 1;
		change = Math.max(change, changeMin);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleIncompleteColumnSparse2ndWrite() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleIncompleteAutoColumn(size, DataManagement.AUTO);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
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
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		Column column = new DoubleIncompleteAutoColumn(max, DataManagement.AUTO);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i++) {
			column.setLast(i, i);
		}
		// here sparse, then back
		for (int i = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i < max; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
	}

	@Test
	public void doubleIncompleteAutoColumnSparseAndBackEnsure() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		Column column = new DoubleIncompleteAutoColumn(max, DataManagement.AUTO);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < max; i++) {
			column.setLast(i, i);
		}
		column.ensure(max + 10);
		for (int i = max; i < max + 10; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
		assertEquals(max + 10 - 1, column.get(max + 10 - 1), 0);
	}

	@Test
	public void doubleIncompleteAutoColumnStayDense() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 500;
		Column column = new DoubleIncompleteAutoColumn(size - 400, DataManagement.AUTO);
		for (int i = 0; i < size - 400; i++) {
			column.setLast(i, i);
		}
		column.ensure(size);
		for (int i = size - 400; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleIncompleteAutoColumnTwoDenseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + 1;
		Column column = new DoubleIncompleteAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void doubleIncompleteAutoColumnEnsure() {
		Column column = new DoubleIncompleteAutoColumn(1, DataManagement.AUTO);
		column.ensure(AutoColumnUtils.CHUNK_SIZE * 2 + 1);
	}

	@Test
	public void doubleIncompleteAutoColumnTwoSparseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new DoubleIncompleteAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size; i++) {
			column.setLast(i, 0.123);
		}
		assertEquals(0.123, column.get(99), 0);
		assertEquals(0.123, column.get(size - 1), 0);
	}

	@Test
	public void doubleIncompleteAutoColumnSparse() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new DoubleIncompleteAutoColumn(size, DataManagement.AUTO);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteMemoryColumnStayDense() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 500;
		Column column = new IntegerIncompleteAutoColumn(size - 400, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size - 400; i++) {
			column.setLast(i, i);
		}
		column.ensure(size);
		for (int i = size - 400; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteMemoryColumnTwoDenseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + 1;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteMemoryColumnTwoSparseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size; i++) {
			column.setLast(i, 2);
		}
		assertEquals(2, column.get(99), 0);
		assertEquals(2, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteMemoryColumnSparse() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteMemoryColumnSparse2ndWrite() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
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
	public void integerIncompleteMemoryColumnSparseAndBack() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_MAXIMAL_DENSITY)) + 1;
		Column column = new IntegerIncompleteAutoColumn(max, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i++) {
			column.setLast(i, i);
		}
		// here sparse
		for (int i = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i < max; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
	}

	@Test
	public void integerIncompleteMemoryColumnSparseAndBackEnsure() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_MAXIMAL_DENSITY));
		Column column = new IntegerIncompleteAutoColumn(max, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < max; i++) {
			column.setLast(i, i);
		}
		column.ensure(max + 10);
		for (int i = max; i < max + 10; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
		assertEquals(max + 10 - 1, column.get(max + 10 - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnStayDense() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 500;
		Column column = new IntegerIncompleteAutoColumn(size - 400, DataManagement.AUTO);
		for (int i = 0; i < size - 400; i++) {
			column.setLast(i, i);
		}
		column.ensure(size);
		for (int i = size - 400; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnTwoDenseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + 1;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnEnsure() {
		Column column = new IntegerIncompleteAutoColumn(1, DataManagement.AUTO);
		column.ensure(AutoColumnUtils.CHUNK_SIZE * 2 + 1);
	}

	@Test
	public void integerIncompleteAutoColumnTwoSparseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size; i++) {
			column.setLast(i, 2);
		}
		assertEquals(2, column.get(99), 0);
		assertEquals(2, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnSparse() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.AUTO);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnSparse2ndWrite() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.AUTO);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
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
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		Column column = new IntegerIncompleteAutoColumn(max, DataManagement.AUTO);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i++) {
			column.setLast(i, i);
		}
		// here sparse, then back
		for (int i = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i < max; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnSparseAndBackEnsure() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY));
		Column column = new IntegerIncompleteAutoColumn(max, DataManagement.AUTO);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < max; i++) {
			column.setLast(i, i);
		}
		column.ensure(max + 10);
		for (int i = max; i < max + 10; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
		assertEquals(max + 10 - 1, column.get(max + 10 - 1), 0);
	}

	@Test
	public void integerAutoColumnStayDense() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 500;
		Column column = new IntegerAutoColumn(size - 400, DataManagement.AUTO);
		for (int i = 0; i < size - 400; i++) {
			column.setLast(i, i);
		}
		column.ensure(size);
		for (int i = size - 400; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerAutoColumnTwoDenseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + 1;
		Column column = new IntegerAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerAutoColumnEnsure() {
		Column column = new IntegerAutoColumn(1, DataManagement.AUTO);
		column.ensure(AutoColumnUtils.CHUNK_SIZE * 2 + 1);
	}

	@Test
	public void integerAutoColumnTwoSparseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size; i++) {
			column.setLast(i, 2);
		}
		assertEquals(2, column.get(99), 0);
		assertEquals(2, column.get(size - 1), 0);
	}

	@Test
	public void integerAutoColumnSparse() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerAutoColumn(size, DataManagement.AUTO);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerAutoColumnSparseToDenseThreshold() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		//chose switchpoint such that switch from sparse to dense happens at THRESHOLD_CHECK_FOR_SPARSE-1
		int switchPoint =
				(int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
						* (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_DENSITY)) - 101;
		column.setLast(0, 1);
		for (int i = 1; i < switchPoint; i++) {
			column.setLast(i, 0);
		}
		for (int i = switchPoint; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerAutoColumnSparse2ndWrite() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerAutoColumn(size, DataManagement.AUTO);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
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
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		Column column = new IntegerAutoColumn(max, DataManagement.AUTO);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i++) {
			column.setLast(i, i);
		}
		// here sparse
		for (int i = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i < max; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
	}

	@Test
	public void integerAutoColumnSparseAndBackEnsure() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY));
		Column column = new IntegerAutoColumn(max, DataManagement.AUTO);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < max; i++) {
			column.setLast(i, i);
		}
		column.ensure(max + 10);
		for (int i = max; i < max + 10; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
		assertEquals(max + 10 - 1, column.get(max + 10 - 1), 0);
	}

	@Test
	public void integerAutoColumnSparseNaN() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size - 2; i++) {
			column.setLast(i, 2);
		}
		column.setLast(size - 2, Double.NaN);
		column.setLast(size - 1, Double.NaN);
		assertEquals(2, column.get(size - 4), 0);
		assertEquals(Double.NaN, column.get(size - 1), 0);
	}

	@Test
	public void integerAutoColumnSparseNaNDefault() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size - 2; i++) {
			column.setLast(i, Double.NaN);
		}
		column.setLast(size - 2, 2);
		column.setLast(size - 1, 2);
		assertEquals(Double.NaN, column.get(size - 4), 0);
		assertEquals(2, column.get(size - 1), 0);
	}

	@Test
	public void integerAutoColumnDenseNaN() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size - 2; i++) {
			column.setLast(i, i);
		}
		column.setLast(size - 2, Double.NaN);
		column.setLast(size - 1, Double.NaN);
		assertEquals(size - 4, column.get(size - 4), 0);
		assertEquals(Double.NaN, column.get(size - 1), 0);
	}

	@Test
	public void integerMemoryColumnStayDense() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 500;
		Column column = new IntegerAutoColumn(size - 400, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size - 400; i++) {
			column.setLast(i, i);
		}
		column.ensure(size);
		for (int i = size - 400; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerMemoryColumnTwoDenseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + 1;
		Column column = new IntegerAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(99, column.get(99), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerMemoryColumnTwoSparseChunks() {
		int size = AutoColumnUtils.CHUNK_SIZE + AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size; i++) {
			column.setLast(i, 2);
		}
		assertEquals(2, column.get(99), 0);
		assertEquals(2, column.get(size - 1), 0);
	}

	@Test
	public void integerMemoryColumnSparse() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(size - 1, column.get(size - 1), 0);
	}

	@Test
	public void integerMemoryColumnSparse2ndWrite() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		int change = (int) (size * (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_MAXIMAL_DENSITY)) + 1;
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < size; i++) {
			column.setLast(i, i);
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
	public void integerMemoryColumnSparseAndBack() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_MAXIMAL_DENSITY)) + 1;
		Column column = new IntegerAutoColumn(max, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i++) {
			column.setLast(i, i);
		}
		// here sparse
		for (int i = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1; i < max; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
	}

	@Test
	public void integerMemoryColumnSparseAndBackEnsure() {
		int change = (int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
				* (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_DENSITY)) + 1;
		int max = (int) (change / (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_MAXIMAL_DENSITY));
		Column column = new IntegerAutoColumn(max, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < change; i++) {
			column.setLast(i, 1);
		}
		for (int i = change; i < max; i++) {
			column.setLast(i, i);
		}
		column.ensure(max + 10);
		for (int i = max; i < max + 10; i++) {
			column.setLast(i, i);
		}
		assertEquals(1, column.get(change - 1), 0);
		assertEquals(max - 1, column.get(max - 1), 0);
		assertEquals(max + 10 - 1, column.get(max + 10 - 1), 0);
	}

	@Test
	public void integerMemoryColumnSparseNaN() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size - 2; i++) {
			column.setLast(i, 2);
		}
		column.setLast(size - 2, Double.NaN);
		column.setLast(size - 1, Double.NaN);
		assertEquals(2, column.get(size - 4), 0);
		assertEquals(Double.NaN, column.get(size - 1), 0);
	}

	@Test
	public void integerMemoryColumnSparseNaNDefault() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size - 2; i++) {
			column.setLast(i, Double.NaN);
		}
		column.setLast(size - 2, 2);
		column.setLast(size - 1, 2);
		assertEquals(Double.NaN, column.get(size - 4), 0);
		assertEquals(2, column.get(size - 1), 0);
	}

	@Test
	public void integerMemoryColumnDenseNaN() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size - 2; i++) {
			column.setLast(i, i);
		}
		column.setLast(size - 2, Double.NaN);
		column.setLast(size - 1, Double.NaN);
		assertEquals(size - 4, column.get(size - 4), 0);
		assertEquals(Double.NaN, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnSparseNaN() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size - 2; i++) {
			column.setLast(i, 2);
		}
		column.setLast(size - 2, Double.NaN);
		column.setLast(size - 1, Double.NaN);
		assertEquals(2, column.get(size - 4), 0);
		assertEquals(Double.NaN, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnSparseNaNDefault() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size - 2; i++) {
			column.setLast(i, Double.NaN);
		}
		column.setLast(size - 2, 2);
		column.setLast(size - 1, 2);
		assertEquals(Double.NaN, column.get(size - 4), 0);
		assertEquals(2, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteAutoColumnDenseNaN() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.AUTO);
		for (int i = 0; i < size - 2; i++) {
			column.setLast(i, i);
		}
		column.setLast(size - 2, Double.NaN);
		column.setLast(size - 1, Double.NaN);
		assertEquals(size - 4, column.get(size - 4), 0);
		assertEquals(Double.NaN, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteMemoryColumnSparseNaN() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size - 2; i++) {
			column.setLast(i, 2);
		}
		column.setLast(size - 2, Double.NaN);
		column.setLast(size - 1, Double.NaN);
		assertEquals(2, column.get(size - 4), 0);
		assertEquals(Double.NaN, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteMemoryColumnSparseNaNDefault() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size - 2; i++) {
			column.setLast(i, Double.NaN);
		}
		column.setLast(size - 2, 2);
		column.setLast(size - 1, 2);
		assertEquals(Double.NaN, column.get(size - 4), 0);
		assertEquals(2, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteMemoryColumnDenseNaN() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 1;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		for (int i = 0; i < size - 2; i++) {
			column.setLast(i, i);
		}
		column.setLast(size - 2, Double.NaN);
		column.setLast(size - 1, Double.NaN);
		assertEquals(size - 4, column.get(size - 4), 0);
		assertEquals(Double.NaN, column.get(size - 1), 0);
	}

	@Test
	public void integerIncompleteColumnSparseToDenseThreshold() {
		int size = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE + 200;
		Column column = new IntegerIncompleteAutoColumn(size, DataManagement.MEMORY_OPTIMIZED);
		//chose switchpoint such that switch from sparse to dense happens at THRESHOLD_CHECK_FOR_SPARSE-1
		int switchPoint =
				(int) (AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE
						* (1 - AutoColumnUtils.THRESHOLD_INTEGER_MEDIUM_SPARSITY_DENSITY)) - 101;
		column.setLast(0, 1);
		for (int i = 1; i < switchPoint; i++) {
			column.setLast(i, 0);
		}
		for (int i = switchPoint; i < size; i++) {
			column.setLast(i, i);
		}
		assertEquals(size - 1, column.get(size - 1), 0);
	}

}
