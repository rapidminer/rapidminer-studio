/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.example.set;

import java.util.Arrays;
import java.util.Objects;


/**
 * Implements a partition that is immutable. A partition is used to divide a table into different parts of arbitrary
 * sizes without actually making a copy of the data. Partitions are used by {@link TableSplitter}s. Partition numbering
 * starts at 0.
 * <p>
 * In contrast to {@link Partition} this implementation returns a new array of selected indices on
 * {@link #selectSubset} and {@link #selectAllSubsetsBut} instead of changing an internal state.
 *
 * @author Gisa Meier
 * @since 9.4.0
 * @see Partition
 */
class ImmutablePartition {

	/** Maps every row to its partition index. */
	private int[] elements;

	/** the number of partitions */
	private int numberOfPartitions;

	/**
	 * Creates a new partition of a given size consisting of {@code ratio.length} sets. The set {@code i} will be of
	 * size {@code size * ratio[i]}, i.e. the sum of all {@code ratio[i]} must be {@code 1}.
	 *
	 * @param ratio
	 * 		array of positive ratios, summing up to 1
	 * @param size
	 * 		the new size
	 * @param builder
	 * 		the {@link PartitionBuilder} to use
	 */
	ImmutablePartition(double[] ratio, int size, PartitionBuilder builder) {
		elements = builder.createPartition(ratio, size);
		numberOfPartitions = ratio.length;
	}

	/**
	 * Creates a new partition of a given size consisting of {@code noPartitions} equally sized sets.
	 *
	 * @param noPartitions
	 * 		the number of partitions
	 * @param size
	 * 		the size to divide between the number of partitions
	 * @param builder
	 * 		the {@link PartitionBuilder} to use
	 */
	ImmutablePartition(int noPartitions, int size, PartitionBuilder builder) {
		double[] ratio = new double[noPartitions];
		for (int i = 0; i < ratio.length; i++) {
			ratio[i] = 1 / (double) noPartitions;
		}
		elements = builder.createPartition(ratio, size);
		numberOfPartitions = ratio.length;
	}

	/**
	 * Creates a partition from the given data. Partition numbering starts at 0.
	 *
	 * @param elements
	 * 		a map from each row to its partition index
	 * @param numberOfPartitions
	 * 		the number of partitions
	 */
	ImmutablePartition(int[] elements, int numberOfPartitions) {
		this.elements = elements;
		this.numberOfPartitions = numberOfPartitions;
	}

	/**
	 * Marks the given subset as selected and returns the selected index array for that case.
	 *
	 * @param i
	 * 		the partition to select
	 * @return the selected indices
	 */
	int[] selectSubset(int i) {
		boolean[] mask = new boolean[numberOfPartitions];
		mask[i] = true;
		return recalculateTableIndices(mask);
	}

	/**
	 * Marks the given subset as deselected and all others as selected. Returns the selected index array.
	 *
	 * @param i
	 * 		the partition not to select
	 * @return the selected indices
	 */
	int[] selectAllSubsetsBut(int i) {
		boolean[] mask = new boolean[numberOfPartitions];
		Arrays.fill(mask, true);
		mask[i] = false;
		return recalculateTableIndices(mask);
	}

	/**
	 * Marks all subsets as selected and returns the selected index array for that case. All indices are selected.
	 *
	 * @return the selected indices
	 */
	int[] selectAllSubsets() {
		boolean[] mask = new boolean[numberOfPartitions];
		Arrays.fill(mask, true);
		return recalculateTableIndices(mask);
	}

	/**
	 * @return the number of subsets.
	 */
	int getNumberOfSubsets() {
		return numberOfPartitions;
	}


	/**
	 * @return the total number of rows.
	 */
	int getTotalSize() {
		return elements.length;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ImmutablePartition that = (ImmutablePartition) o;
		return numberOfPartitions == that.numberOfPartitions &&
				Arrays.equals(elements, that.elements);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(numberOfPartitions);
		result = 31 * result + Arrays.hashCode(elements);
		return result;
	}

	/**
	 * Recalculates the table indices of the currently selected rows.
	 */
	private int[] recalculateTableIndices(boolean[] mask) {
		int length = 0;
		for (int i = 0; i < elements.length; i++) {
			if (mask[elements[i]]) {
				length++;
			}
		}

		int[] tableIndexMap = new int[length];
		int j = 0;
		for (int i = 0; i < elements.length; i++) {
			if (mask[elements[i]]) {
				tableIndexMap[j] = i;
				j++;
			}
		}
		return tableIndexMap;
	}

}
