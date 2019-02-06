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

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;


/**
 *
 *
 * @author Jan Czogalla
 *
 */
public class SparseChunkTest {

	@Test
	public void fillWithDefaults() {
		IntegerHighSparsityChunk iasc = new IntegerHighSparsityChunk(0);
		DoubleHighSparsityChunk dasc = new DoubleHighSparsityChunk(0);
		for (int i = 0; i < 100; i++) {
			iasc.set(i, 0);
			dasc.set(i, 0);
		}
		for (int i = 0; i < 100; i++) {
			assertEquals(0, iasc.get(i), 0);
			assertEquals(0, dasc.get(i), 0);
		}
	}

	@Test
	public void fillWithDefaultsNaN() {
		IntegerHighSparsityChunk iasc = new IntegerHighSparsityChunk(Double.NaN);
		DoubleHighSparsityChunk dasc = new DoubleHighSparsityChunk(Double.NaN);
		for (int i = 0; i < 100; i++) {
			iasc.set(i, Double.NaN);
			dasc.set(i, Double.NaN);
		}
		for (int i = 0; i < 100; i++) {
			assertEquals(Double.NaN, iasc.get(i), 0);
			assertEquals(Double.NaN, dasc.get(i), 0);
		}
	}

	@Test
	public void fillDense() {
		IntegerHighSparsityChunk iasc = new IntegerHighSparsityChunk(0);
		DoubleHighSparsityChunk dasc = new DoubleHighSparsityChunk(0);
		for (int i = 0; i < 100; i++) {
			iasc.set(i, 1);
			dasc.set(i, 1);
		}
		for (int i = 0; i < 100; i++) {
			assertEquals(1, iasc.get(i), 0);
			assertEquals(1, dasc.get(i), 0);
		}
	}

	@Test
	public void fillDenseNaN() {
		IntegerHighSparsityChunk iasc = new IntegerHighSparsityChunk(0);
		DoubleHighSparsityChunk dasc = new DoubleHighSparsityChunk(0);
		for (int i = 0; i < 100; i++) {
			iasc.set(i, Double.NaN);
			dasc.set(i, Double.NaN);
		}
		for (int i = 0; i < 100; i++) {
			assertEquals(Double.NaN, iasc.get(i), 0);
			assertEquals(Double.NaN, dasc.get(i), 0);
		}
	}

	@Test
	public void fillSparse() {
		IntegerHighSparsityChunk iasc = new IntegerHighSparsityChunk(0);
		DoubleHighSparsityChunk dasc = new DoubleHighSparsityChunk(0);
		for (int i = 0; i < 100; i += 10) {
			iasc.set(i, 1);
			dasc.set(i, 1);
		}
		for (int i = 0; i < 100; i++) {
			assertEquals(i % 10 == 0 ? 1 : 0, iasc.get(i), 0);
			assertEquals(i % 10 == 0 ? 1 : 0, dasc.get(i), 0);
		}
	}

	@Test
	public void fillOverThreshold() {
		IntegerHighSparsityChunk iasc = new IntegerHighSparsityChunk(0);
		DoubleHighSparsityChunk dasc = new DoubleHighSparsityChunk(0);
		int nonDefaultValues = (int) (100 * AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY
				/ (1 - AutoColumnUtils.THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY)) + 1;
		iasc.ensure(100 + nonDefaultValues);
		dasc.ensure(100 + nonDefaultValues);
		for (int i = 0; i < 100; i++) {
			assertEquals(false, iasc.set(i, 0));
			assertEquals(false, dasc.set(i, 0));
		}
		for (int i = 0; i < nonDefaultValues; i++) {
			assertEquals(false, iasc.set(i + 100, i));
			assertEquals(false, dasc.set(i + 100, i));
		}
		assertEquals(true, iasc.set(nonDefaultValues, 3));
		assertEquals(true, dasc.set(nonDefaultValues, 3));
	}

	@Test
	public void removeIndicesBackToFront() {
		IntegerHighSparsityChunk iasc = new IntegerHighSparsityChunk(0);
		DoubleHighSparsityChunk dasc = new DoubleHighSparsityChunk(0);
		for (int i = 0; i < 1000; i += 10) {
			iasc.set(i, 1);
			dasc.set(i, 1);
		}
		for (int i = 990; i >= 0; i -= 10) {
			iasc.set(i, 0);
			dasc.set(i, 0);
		}
		for (int i = 0; i < 1000; i++) {
			assertEquals(0, iasc.get(i), 0);
			assertEquals(0, dasc.get(i), 0);
		}
	}

	@Test
	public void removeIndicesFrontToBack() {
		IntegerHighSparsityChunk iasc = new IntegerHighSparsityChunk(0);
		DoubleHighSparsityChunk dasc = new DoubleHighSparsityChunk(0);
		for (int i = 0; i < 1000; i += 10) {
			iasc.set(i, 1);
			dasc.set(i, 1);
		}
		for (int i = 0; i < 1000; i += 10) {
			iasc.set(i, 0);
			dasc.set(i, 0);
		}
		for (int i = 0; i < 1000; i++) {
			assertEquals(0, iasc.get(i), 0);
			assertEquals(0, dasc.get(i), 0);
		}
	}

	@Test
	public void addAndRemoveRandomOrder() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		int[] indices = new int[1000];
		Arrays.setAll(indices, i -> random.nextInt(0, 1000));
		IntegerHighSparsityChunk iasc = new IntegerHighSparsityChunk(0);
		DoubleHighSparsityChunk dasc = new DoubleHighSparsityChunk(0);
		for (int i : indices) {
			iasc.set(i, 1);
			dasc.set(i, 1);
		}
		for (int i : indices) {
			iasc.set(i, 0);
			dasc.set(i, 0);
		}
		for (int i : indices) {
			assertEquals(0, iasc.get(i), 0);
			assertEquals(0, dasc.get(i), 0);
		}
	}
}
