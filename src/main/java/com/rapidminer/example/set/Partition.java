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
package com.rapidminer.example.set;

import com.rapidminer.tools.LogService;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;


/**
 * Implements a partition. A partition is used to divide an example set into different parts of
 * arbitrary sizes without actually make a copy of the data. Partitions are used by
 * {@link SplittedExampleSet}s. Partition numbering starts at 0.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class Partition implements Cloneable, Serializable {

	private static final long serialVersionUID = 6126334515107973287L;

	/** Mask for the selected partitions. */
	private boolean[] mask;

	/** Size of the individual partitions. */
	private int[] partitionSizes;

	/** Maps every example to its partition index. */
	private int[] elements;

	/** Indicates the position of the last element for each partition. */
	private int[] lastElementIndex;

	/**
	 * Maps every example index to the true index of the data row in the example table.
	 */
	private int[] tableIndexMap = null;

	/**
	 * Creates a new partition of a given size consisting of <tt>ratio.length</tt> sets. The set
	 * <i>i</i> will be of size of <i>size x ratio[i]</i>, i.e. the sum of all <i>ratio[i]</i> must
	 * be 1. Initially all partitions are selected.
	 */
	public Partition(double ratio[], int size, PartitionBuilder builder) {
		init(ratio, size, builder);
	}

	/**
	 * Creates a new partition of a given size consisting of <i>noPartitions</i> equally sized sets.
	 * Initially all partitions are selected.
	 */
	public Partition(int noPartitions, int size, PartitionBuilder builder) {
		double[] ratio = new double[noPartitions];
		for (int i = 0; i < ratio.length; i++) {
			ratio[i] = 1 / (double) noPartitions;
		}
		init(ratio, size, builder);
	}

	/** Creates a partition from the given one. Partition numbering starts at 0. */
	public Partition(int[] elements, int numberOfPartitions) {
		init(elements, numberOfPartitions);
	}

	/** Clone constructor. */
	private Partition(Partition p) {
		this.partitionSizes = new int[p.partitionSizes.length];
		System.arraycopy(p.partitionSizes, 0, this.partitionSizes, 0, p.partitionSizes.length);

		this.mask = new boolean[p.mask.length];
		System.arraycopy(p.mask, 0, this.mask, 0, p.mask.length);

		this.elements = new int[p.elements.length];
		System.arraycopy(p.elements, 0, this.elements, 0, p.elements.length);

		this.lastElementIndex = new int[p.lastElementIndex.length];
		System.arraycopy(p.lastElementIndex, 0, this.lastElementIndex, 0, p.lastElementIndex.length);

		recalculateTableIndices();
	}

	/**
	 * Creates a partition from the given ratios. The partition builder is used for creation.
	 */
	private void init(double[] ratio, int size, PartitionBuilder builder) {
		// LogService.getGlobal().log("Create new partition using a '" +
		// builder.getClass().getName() + "'.", LogService.STATUS);
		LogService.getRoot().log(Level.FINE, "com.rapidminer.example.set.Partition.creating_new_partition_using",
				builder.getClass().getName());
		elements = builder.createPartition(ratio, size);
		init(elements, ratio.length);
	}

	/** Private initialization method used by constructors. */
	private void init(int[] newElements, int noOfPartitions) {
		// LogService.getGlobal().log("Create new partition with " + newElements.length +
		// " elements and " + noOfPartitions + " partitions.",
		// LogService.STATUS);
		LogService.getRoot().log(Level.FINE, "com.rapidminer.example.set.Partition.creating_new_partition_with",
				new Object[] { newElements.length, noOfPartitions });
		partitionSizes = new int[noOfPartitions];
		lastElementIndex = new int[noOfPartitions];
		elements = newElements;
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] >= 0) {
				partitionSizes[elements[i]]++;
				lastElementIndex[elements[i]] = i;
			}
		}

		// select all partitions
		mask = new boolean[noOfPartitions];
		for (int i = 0; i < mask.length; i++) {
			mask[i] = true;
		}

		recalculateTableIndices();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Partition)) {
			return false;
		}

		Partition other = (Partition) o;

		for (int i = 0; i < mask.length; i++) {
			if (this.mask[i] != other.mask[i]) {
				return false;
			}
		}

		for (int i = 0; i < elements.length; i++) {
			if (this.elements[i] != other.elements[i]) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		int hc = 17;
		int hashMultiplier = 59;

		hc = hc * hashMultiplier + this.mask.length;
		for (int i = 1; i < mask.length; i <<= 1) {
			hc = hc * hashMultiplier + Boolean.valueOf(this.mask[i]).hashCode();
		}

		hc = hc * hashMultiplier + this.elements.length;
		for (int i = 1; i < elements.length; i <<= 1) {
			hc = hc * hashMultiplier + Integer.valueOf(this.elements[i]).hashCode();
		}

		return hc;
	}

	/**
	 * Returns true if the last possible index stored in lastElementIndex for all currently selected
	 * partitions is not yet reached. Might be used to prune iterations (especially useful for
	 * linear partitions).
	 */
	public boolean hasNext(int index) {
		for (int p = 0; p < mask.length; p++) {
			if (mask[p]) {
				if (index <= lastElementIndex[p]) {
					return true;
				}
			}
		}
		return false;
	}

	/** Clears the selection, i.e. deselects all subsets. */
	public void clearSelection() {
		this.mask = new boolean[mask.length];
		recalculateTableIndices();
	}

	public void invertSelection() {
		for (int i = 0; i < mask.length; i++) {
			mask[i] = !mask[i];
		}
		recalculateTableIndices();
	};

	/** Marks the given subset as selected. */
	public void selectSubset(int i) {
		this.mask[i] = true;
		recalculateTableIndices();
	}

	/** Marks the given subset as deselected. */
	public void deselectSubset(int i) {
		this.mask[i] = false;
		recalculateTableIndices();
	}

	/** Returns the number of subsets. */
	public int getNumberOfSubsets() {
		return partitionSizes.length;
	}

	/** Returns the number of selected elements. */
	public int getSelectionSize() {
		int s = 0;
		for (int i = 0; i < partitionSizes.length; i++) {
			if (mask[i]) {
				s += partitionSizes[i];
			}
		}
		return s;
	}

	/** Returns the total number of examples. */
	public int getTotalSize() {
		return elements.length;
	}

	/**
	 * Returns true iff the example with the given index is selected according to the current
	 * selection mask.
	 */
	public boolean isSelected(int index) {
		return mask[elements[index]];
	}

	/**
	 * Recalculates the example table indices of the currently selected examples.
	 */
	private void recalculateTableIndices() {
		List<Integer> indices = new LinkedList<Integer>();
		for (int i = 0; i < elements.length; i++) {
			if (mask[elements[i]]) {
				indices.add(i);
			}
		}
		tableIndexMap = new int[indices.size()];
		Iterator<Integer> i = indices.iterator();
		int counter = 0;
		while (i.hasNext()) {
			tableIndexMap[counter++] = i.next();
		}
	}

	/**
	 * Returns the actual example table index of the i-th example of the currently selected subset.
	 */
	public int mapIndex(int index) {
		return tableIndexMap[index];
	}

	@Override
	public String toString() {
		StringBuffer str = new StringBuffer("(");
		for (int i = 0; i < partitionSizes.length; i++) {
			str.append((i != 0 ? "/" : "") + partitionSizes[i]);
		}
		str.append(")");
		return str.toString();
	}

	@Override
	public Object clone() {
		return new Partition(this);
	}
}
