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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Tools;


/**
 * An example set that can be split into subsets by using a {@link Partition}.
 *
 * @author Simon Fischer, Ingo Mierswa, Felix Jungermann
 */
public class SplittedExampleSet extends AbstractExampleSet {

	public static final OperatorVersion VERSION_SAMPLING_CHANGED = new OperatorVersion(5, 1, 2);

	private static final long serialVersionUID = 4573262969007377183L;

	/** Indicates a non-shuffled sampling for partition building. */
	public static final String[] SAMPLING_NAMES = { "linear sampling", "shuffled sampling", "stratified sampling", "automatic" };

	/** Indicates a non-shuffled sampling for partition building. */
	public static final int LINEAR_SAMPLING = 0;

	/** Indicates a shuffled sampling for partition building. */
	public static final int SHUFFLED_SAMPLING = 1;

	/** Indicates a stratified shuffled sampling for partition building. */
	public static final int STRATIFIED_SAMPLING = 2;

	/**
	 * Indicates the usage of the automatic mode, where stratified sampling
	 * is used per default for partition building. If it isn't applicable, shuffled
	 * sampling will be used instead.
	 */
	public static final int AUTOMATIC = 3;

	/** The partition. */
	private Partition partition;

	/** The parent example set. */
	private ExampleSet parent;

	/** Constructs a SplittedExampleSet with the given partition. */
	public SplittedExampleSet(ExampleSet exampleSet, Partition partition) {
		this.parent = (ExampleSet) exampleSet.clone();
		this.partition = partition;
	}

	/**
	 * Constructs a SplittedExampleSet with the given partition. If tryCompose is {@code true} and
	 * exampleSet is a SplittedExampleSet a new Partition is constructed out of the
	 * {@link Partition} of exampleSet and partition in order to prevent nested SplittedExampleSets.
	 *
	 * @param exampleSet
	 *            the parent example set
	 * @param partition
	 *            the partition to use
	 * @param tryCompose
	 *            if {@code true} and exampleSet is a SplittedExampleSet then the partitions are
	 *            composed
	 */
	public SplittedExampleSet(ExampleSet exampleSet, Partition partition, boolean tryCompose) {
		if (tryCompose && exampleSet instanceof SplittedExampleSet) {
			SplittedExampleSet splitted = (SplittedExampleSet) exampleSet;
			this.parent = (ExampleSet) splitted.parent.clone();
			Partition oldPartition = splitted.partition;
			this.partition = Partition.compose(oldPartition, partition);
		} else {
			this.parent = (ExampleSet) exampleSet.clone();
			this.partition = partition;
		}
	}

	/**
	 * Creates an example set that is splitted into two subsets using the given sampling type. If
	 * autoSwitchToShuffled is true, the sampling type is switched from Stratified to Shuffled
	 * automatically without error when no classification label is present. This shouldn't be used,
	 * but it keeps compatibility to previous versions.
	 *
	 *
	 * @throws UserError
	 */
	public SplittedExampleSet(ExampleSet exampleSet, double splitRatio, int samplingType, boolean useLocalRandomSeed,
			int seed, boolean autoSwitchToShuffled) throws UserError {
		this(exampleSet, new double[] { splitRatio, 1 - splitRatio }, samplingType, useLocalRandomSeed, seed,
				autoSwitchToShuffled);
	}

	/**
	 * Creates an example set that is splitted into two subsets using the given sampling type.
	 *
	 * @throws UserError
	 */
	public SplittedExampleSet(ExampleSet exampleSet, double splitRatio, int samplingType, boolean useLocalRandomSeed,
			int seed) throws UserError {
		this(exampleSet, new double[] { splitRatio, 1 - splitRatio }, samplingType, useLocalRandomSeed, seed);
	}

	/**
	 * Creates an example set that is splitted into two subsets using the given sampling type.
	 *
	 * @param tryCompose
	 *            if {@code true} and exampleSet is a SplittedExampleSet then the partitions of the
	 *            exampleSet and the one constructed here are composed in order to prevent nested
	 *            SplittedExampleSets.
	 * @throws UserError
	 */
	public SplittedExampleSet(ExampleSet exampleSet, double splitRatio, int samplingType, boolean useLocalRandomSeed,
			boolean tryCompose, int seed) throws UserError {
		this(exampleSet, new double[] { splitRatio, 1 - splitRatio }, samplingType, useLocalRandomSeed, tryCompose, seed);
	}

	/**
	 * Creates an example set that is splitted into n subsets with the given sampling type.
	 *
	 * @throws UserError
	 */
	public SplittedExampleSet(ExampleSet exampleSet, double[] splitRatios, int samplingType, boolean useLocalRandomSeed,
			int seed) throws UserError {
		this(exampleSet, new Partition(splitRatios, exampleSet.size(),
				createPartitionBuilder(exampleSet, samplingType, useLocalRandomSeed, seed, true)));
	}

	/**
	 * Creates an example set that is splitted into n subsets with the given sampling type.
	 * 
	 * @param tryCompose
	 *            if {@code true} and exampleSet is a SplittedExampleSet then the partitions of the
	 *            exampleSet and the one constructed here are composed in order to prevent nested
	 *            SplittedExampleSets.
	 * @throws UserError
	 */
	public SplittedExampleSet(ExampleSet exampleSet, double[] splitRatios, int samplingType, boolean useLocalRandomSeed,
			boolean tryCompose, int seed) throws UserError {
		this(exampleSet, new Partition(splitRatios, exampleSet.size(),
				createPartitionBuilder(exampleSet, samplingType, useLocalRandomSeed, seed, true)), tryCompose);
	}

	/**
	 * Creates an example set that is splitted into n subsets with the given sampling type. If
	 * autoSwitchToShuffled is true, the sampling type is switched from Stratified to Shuffled
	 * automatically without error when no classification label is present. This shouldn't be used,
	 * but it keeps compatibility to previous versions.
	 *
	 * @throws UserError
	 */
	public SplittedExampleSet(ExampleSet exampleSet, double[] splitRatios, int samplingType, boolean useLocalRandomSeed,
			int seed, boolean autoSwitchToShuffled) throws UserError {
		this(exampleSet, new Partition(splitRatios, exampleSet.size(),
				createPartitionBuilder(exampleSet, samplingType, useLocalRandomSeed, seed, autoSwitchToShuffled)));
	}

	/**
	 * Creates an example set that is splitted into <i>numberOfSubsets</i> parts with the given
	 * sampling type.
	 *
	 * @throws UserError
	 */
	public SplittedExampleSet(ExampleSet exampleSet, int numberOfSubsets, int samplingType, boolean useLocalRandomSeed,
			int seed) throws UserError {
		this(exampleSet, new Partition(numberOfSubsets, exampleSet.size(),
				createPartitionBuilder(exampleSet, samplingType, useLocalRandomSeed, seed, true)));
	}

	/**
	 * Creates an example set that is splitted into <i>numberOfSubsets</i> parts with the given
	 * sampling type. If autoSwitchToShuffled is true, the sampling type is switched from Stratified
	 * to Shuffled automatically without error when no classification label is present. This
	 * shouldn't be used, but it keeps compatibility to previous versions.
	 *
	 * @throws UserError
	 */
	public SplittedExampleSet(ExampleSet exampleSet, int numberOfSubsets, int samplingType, boolean useLocalRandomSeed,
			int seed, boolean autoSwitchToShuffled) throws UserError {
		this(exampleSet, new Partition(numberOfSubsets, exampleSet.size(),
				createPartitionBuilder(exampleSet, samplingType, useLocalRandomSeed, seed, autoSwitchToShuffled)));
	}

	/** Clone constructor. */
	public SplittedExampleSet(SplittedExampleSet exampleSet) {
		this.parent = (ExampleSet) exampleSet.parent.clone();
		this.partition = (Partition) exampleSet.partition.clone();
	}

	@Override
	public boolean equals(Object o) {
		if (!super.equals(o)) {
			return false;
		}
		if (!(o instanceof SplittedExampleSet)) {
			return false;
		}
		return this.partition.equals(((SplittedExampleSet) o).partition);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ partition.hashCode();
	}

	/**
	 * Creates the partition builder for the given sampling type. If autoSwitchToShuffled is true,
	 * it will be changed to shuffled sampling if Stratified
	 *
	 * @throws UserError
	 */
	private static PartitionBuilder createPartitionBuilder(ExampleSet exampleSet, int samplingType,
			boolean useLocalRandomSeed, int seed, boolean autoSwitchToShuffled) throws UserError {
		PartitionBuilder builder = null;
		switch (samplingType) {
			case LINEAR_SAMPLING:
				builder = new SimplePartitionBuilder();
				break;
			case SHUFFLED_SAMPLING:
				builder = new ShuffledPartitionBuilder(useLocalRandomSeed, seed);
				break;
			case STRATIFIED_SAMPLING:
			case AUTOMATIC:
			default:
				Attribute label = exampleSet.getAttributes().getLabel();
				if (label != null && label.isNominal()) {
					builder = new StratifiedPartitionBuilder(exampleSet, useLocalRandomSeed, seed);
				} else {
					if ((autoSwitchToShuffled || samplingType == AUTOMATIC) && (label == null || !label.isNominal())) {
							exampleSet.getLog().logWarning(
									"Example set has no nominal label: using shuffled partition instead of stratified partition");
							return new ShuffledPartitionBuilder(useLocalRandomSeed, seed);
						}

					com.rapidminer.example.Tools.hasNominalLabels(exampleSet, "stratified sampling");
					builder = new ShuffledPartitionBuilder(useLocalRandomSeed, seed);
				}
				break;
		}
		return builder;

	}

	/** Adds the given subset. */
	public void selectAdditionalSubset(int index) {
		partition.selectSubset(index);
	}

	/** Selects exactly one subset. */
	public void selectSingleSubset(int index) {
		partition.clearSelection();
		partition.selectSubset(index);
	}

	/** Selects all but one subset. */
	public void selectAllSubsetsBut(int index) {
		partition.clearSelection();
		for (int i = 0; i < partition.getNumberOfSubsets(); i++) {
			if (i != index) {
				partition.selectSubset(i);
			}
		}
	}

	/** Selects all subsets. */
	public void selectAllSubsets() {
		partition.clearSelection();
		for (int i = 0; i < partition.getNumberOfSubsets(); i++) {
			partition.selectSubset(i);
		}
	}

	/** Inverts the current selection */
	public void invertSelection() {
		partition.invertSelection();
	}

	/** Clears the current selection */
	public void clearSelection() {
		partition.clearSelection();
	}

	/** Returns the number of subsets. */
	public int getNumberOfSubsets() {
		return partition.getNumberOfSubsets();
	}

	/** Returns an example reader that splits all examples that are not selected. */
	@Override
	public Iterator<Example> iterator() {
		return new IndexBasedExampleSetReader(this);
	}

	@Override
	public int size() {
		return partition.getSelectionSize();
	}

	/**
	 * Searches i-th example in the currently selected partition. This is done in constant time.
	 */
	@Override
	public Example getExample(int index) {
		int actualIndex = partition.mapIndex(index);
		return this.parent.getExample(actualIndex);
	}

	/** Returns the index of the example in the parent example set. */
	public int getActualParentIndex(int index) {
		return partition.mapIndex(index);
	}

	@Override
	public ExampleTable getExampleTable() {
		return parent.getExampleTable();
	}

	@Override
	public Attributes getAttributes() {
		return this.parent.getAttributes();
	}

	@Override
	public Annotations getAnnotations() {
		return parent.getAnnotations();
	}

	// -------------------- Factory methods --------------------

	/**
	 * Works only for nominal and integer attributes. If <i>k</i> is the number of different values,
	 * this method splits the example set into <i>k</i> subsets according to the value of the given
	 * attribute.
	 */
	public static SplittedExampleSet splitByAttribute(ExampleSet exampleSet, Attribute attribute) {
		int[] elements = new int[exampleSet.size()];
		int i = 0;
		Map<Integer, Integer> indexMap = new HashMap<>();
		int currentIndex = 0;
		for (Example example : exampleSet) {
			int value = (int) example.getValue(attribute);
			Integer indexObject = indexMap.get(value);
			if (indexObject == null) {
				indexMap.put(value, currentIndex);
				currentIndex++;
			}
			int intValue = indexMap.get(value);
			elements[i++] = intValue;
		}

		int maxNumber = indexMap.size();
		indexMap.clear();
		Partition partition = new Partition(elements, maxNumber);
		return new SplittedExampleSet(exampleSet, partition);
	}

	/**
	 * Works only for real-value attributes. Returns an example set splitted into two parts
	 * containing all examples providing a greater (smaller) value for the given attribute than the
	 * given value. The first partition contains all examples providing a smaller or the same value
	 * than the given one.
	 */
	public static SplittedExampleSet splitByAttribute(ExampleSet exampleSet, Attribute attribute, double value) {
		int[] elements = new int[exampleSet.size()];
		Iterator<Example> reader = exampleSet.iterator();
		int i = 0;
		while (reader.hasNext()) {
			Example example = reader.next();
			double currentValue = example.getValue(attribute);
			if (Tools.isLessEqual(currentValue, value)) {
				elements[i++] = 0;
			} else {
				elements[i++] = 1;
			}
		}
		Partition partition = new Partition(elements, 2);
		return new SplittedExampleSet(exampleSet, partition);
	}

	@Override
	public void cleanup() {
		parent.cleanup();
	}
}
