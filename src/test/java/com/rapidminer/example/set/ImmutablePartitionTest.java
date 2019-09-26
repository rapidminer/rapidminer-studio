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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;


/**
 * Tests the {@link ImmutablePartition} by comparing it to {@link Partition}.
 *
 * @author Gisa Meier
 */
@RunWith(Parameterized.class)
public class ImmutablePartitionTest {

	@Parameterized.Parameter
	public String name;

	@Parameterized.Parameter(value = 1)
	public ImmutablePartition partition;

	@Parameterized.Parameter(value = 2)
	public Partition compareToPartition;

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> parameters() {
		return Arrays.asList(new Object[]{"simple",
						new ImmutablePartition(new int[]{1, 0, 0, 2, 0, 2}, 3),
						new Partition(new int[]{1, 0, 0, 2, 0, 2}, 3)},
				new Object[]{"ratio",
						new ImmutablePartition(new double[]{0.2, 0.4, 0.1, 0.3}, 50,
						new ShuffledPartitionBuilder(true, 42)),
						new Partition(new double[]{0.2, 0.4, 0.1, 0.3}, 50,
						new ShuffledPartitionBuilder(true, 42))},
				new Object[]{"equallySized",
						new ImmutablePartition(5, 50, new ShuffledPartitionBuilder(true, 42)),
						new Partition(5, 50, new ShuffledPartitionBuilder(true, 42))});
	}


	@Test
	public void selectSubset() {
		compareToPartition.clearSelection();
		compareToPartition.selectSubset(0);
		assertArrayEquals(getTableIndices(), partition.selectSubset(0));
	}


	@Test
	public void selectAllSubsetsBut() {
		compareToPartition.clearSelection();
		compareToPartition.invertSelection();
		compareToPartition.deselectSubset(2);
		assertArrayEquals(getTableIndices(), partition.selectAllSubsetsBut(2));
	}

	@Test
	public void selectAllSubsets() {
		compareToPartition.clearSelection();
		compareToPartition.invertSelection();
		assertArrayEquals(getTableIndices(), partition.selectAllSubsets());
	}

	@Test
	public void getNumberOfSubsets() {
		assertEquals(compareToPartition.getNumberOfSubsets(), partition.getNumberOfSubsets());
	}

	@Test
	public void getTotalSize() {
		assertEquals(compareToPartition.getTotalSize(), partition.getTotalSize());
	}

	private int[] getTableIndices() {
		int[] tableIndices = new int[compareToPartition.getSelectionSize()];
		for (int i = 0; i < tableIndices.length; i++) {
			tableIndices[i] = compareToPartition.mapIndex(i);
		}
		return tableIndices;
	}
}
