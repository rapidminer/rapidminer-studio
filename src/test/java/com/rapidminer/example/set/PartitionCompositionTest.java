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
package com.rapidminer.example.set;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * Test the method {@link Partition#compose(Partition, Partition)}.
 *
 * @author Gisa Schaefer
 */
public class PartitionCompositionTest {

	@Test
	public void compositionTest1PartionSelected() {
		Partition outer = new Partition(new int[] { 1, 0, 0, 2, 0, 2 }, 3);
		outer.clearSelection();
		outer.selectSubset(0);
		Partition inner = new Partition(new int[] { 1, 0, 1 }, 2);

		Partition composition = Partition.compose(outer, inner);

		assertEquals(inner.getNumberOfSubsets(), composition.getNumberOfSubsets());
		assertEquals(inner.getSelectionSize(), composition.getSelectionSize());

		assertEquals(1, composition.mapIndex(0));
		assertEquals(2, composition.mapIndex(1));
		assertEquals(4, composition.mapIndex(2));

		composition.clearSelection();
		composition.selectSubset(1);
		assertEquals(2, composition.getSelectionSize());
	}

	@Test
	public void compositionTest2PartitionsSelected() {
		Partition outer = new Partition(new int[] { 1, 0, 0, 2, 0, 2 }, 3);
		outer.clearSelection();
		outer.selectSubset(1);
		outer.selectSubset(2);
		Partition inner = new Partition(new int[] { 1, 0, 1 }, 2);

		Partition composition = Partition.compose(outer, inner);

		assertEquals(inner.getNumberOfSubsets(), composition.getNumberOfSubsets());
		assertEquals(inner.getSelectionSize(), composition.getSelectionSize());

		assertEquals(0, composition.mapIndex(0));
		assertEquals(3, composition.mapIndex(1));
		assertEquals(5, composition.mapIndex(2));

		composition.clearSelection();
		composition.selectSubset(1);
		assertEquals(2, composition.getSelectionSize());
	}

	@Test
	public void compositionTestSizeNotMatching() {
		Partition outer = new Partition(new int[] { 1, 0, 0, 2, 0, 2 }, 3);
		Partition inner = new Partition(new int[] { 1, 0, 1 }, 2);

		Partition composition = Partition.compose(outer, inner);

		assertEquals(inner.getNumberOfSubsets(), composition.getNumberOfSubsets());
		assertEquals(inner.getSelectionSize(), composition.getSelectionSize());

		assertEquals(0, composition.mapIndex(0));
		assertEquals(1, composition.mapIndex(1));
		assertEquals(2, composition.mapIndex(2));

	}

}
