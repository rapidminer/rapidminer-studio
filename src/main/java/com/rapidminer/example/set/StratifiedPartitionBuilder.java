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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.RandomGenerator;


/**
 * Creates a shuffled and stratified partition for an example set. The example set must have an
 * nominal label. This partition builder can work in two modes:
 * <ol>
 * <li>The first working mode is automatically used for generic types of ratio arrays, especially
 * for those with different sizes. Due to to this fact it however cannot longer be guaranteed that
 * each fold exactly contains the correct number of examples and each class at least once.</li>
 * <li>In contrast to the first mode the correct partition can at least be guaranteed for ratio
 * arrays containing the same ratio value for all folds. The second mode is automatically performed
 * in this case (e.g. for cross validation).</li> </ul>
 *
 * @author Ingo Mierswa
 */
public class StratifiedPartitionBuilder implements PartitionBuilder {

	/** Helper class for sorting according to class values. */
	static class ExampleIndex implements Comparable<ExampleIndex> {

		int exampleIndex;

		String className;

		public ExampleIndex(int exampleIndex, String className) {
			this.exampleIndex = exampleIndex;
			this.className = className;
		}

		@Override
		public int compareTo(ExampleIndex e) {
			return this.className.compareTo(e.className);
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ExampleIndex)) {
				return false;
			} else {
				ExampleIndex other = (ExampleIndex) o;
				return this.exampleIndex == other.exampleIndex;
			}
		}

		@Override
		public int hashCode() {
			return Integer.valueOf(this.exampleIndex).hashCode();
		}

		@Override
		public String toString() {
			return exampleIndex + "(" + className + ")";
		}
	}

	private ExampleSet exampleSet;

	private Random random;

	public StratifiedPartitionBuilder(ExampleSet exampleSet, boolean useLocalRandomSeed, int seed) {
		this.exampleSet = exampleSet;
		this.random = RandomGenerator.getRandomGenerator(useLocalRandomSeed, seed);
	}

	/**
	 * Returns a stratified partition for the given example set. The examples must have an nominal
	 * label.
	 */
	@Override
	public int[] createPartition(double[] ratio, int size) {
		Attribute label = exampleSet.getAttributes().getLabel();

		// typical errors
		if (size != exampleSet.size()) {
			throw new RuntimeException(
					"Cannot create stratified Partition: given size and size of the example set must be equal!");
		}

		if (label == null) {
			throw new RuntimeException("Cannot create stratified Partition: example set must have a label!");
		}

		if (!label.isNominal()) {
			throw new RuntimeException("Cannot create stratified Partition: label of example set must be nominal!");
		}

		double firstValue = ratio[0];
		for (int i = 1; i < ratio.length; i++) {
			if (ratio[i] != firstValue) {
				LogService.getRoot().log(Level.FINE,
						"com.rapidminer.example.set.StratifiedPartitionBuilder.not_all_ratio_values_are_equal");
				return createNonEqualPartition(ratio, size, label);
			}
		}
		LogService.getRoot().log(Level.FINE,
				"com.rapidminer.example.set.StratifiedPartitionBuilder.all_ratio_values_are_equal");
		return createEqualPartition(ratio, size, label);
	}

	/**
	 * Returns a stratified partition for the given example set. The examples must have a nominal
	 * label.
	 */
	private int[] createEqualPartition(double[] ratio, int size, Attribute label) {
		// fill example list with indices and classes
		List<ExampleIndex> examples = new ArrayList<ExampleIndex>(size);
		Iterator<Example> reader = exampleSet.iterator();
		int index = 0;
		while (reader.hasNext()) {
			Example example = reader.next();
			examples.add(new ExampleIndex(index++, example.getNominalValue(label)));
		}
		return createPartitionFromIndices(ratio, examples, size, random);

	}

	/**
	 * Creates a partition based on the examples list.
	 *
	 * @param ratio
	 * 		the desired ratios
	 * @param examples
	 * 		the tuples of row index and class name
	 * @param size
	 * 		the size of the new partition
	 * @param random
	 * 		the random generator for shuffling
	 * @return the partition
	 */
	static int[] createPartitionFromIndices(double[] ratio, List<ExampleIndex> examples, int size, Random random) {
		// shuffling
		Collections.shuffle(examples, random);

		// sort by class
		Collections.sort(examples);

		// divide classes _equal_ into potential partitions
		List<ExampleIndex> newExamples = new ArrayList<ExampleIndex>(size);
		int start = 0;
		int numberOfPartitions = ratio.length;
		while (newExamples.size() < size) {
			for (int i = start; i < examples.size(); i += numberOfPartitions) {
				newExamples.add(examples.get(i));
			}
			start++;
		}

		// build partition starts
		int[] startNewP = new int[ratio.length + 1];
		startNewP[0] = 0;
		double ratioSum = 0;
		for (int i = 1; i < startNewP.length; i++) {
			ratioSum += ratio[i - 1];
			startNewP[i] = (int) Math.round(newExamples.size() * ratioSum);
		}

		// create a simple partition from the stratified shuffled example
		// indices and partition starts
		int[] part = new int[newExamples.size()];
		int p = 0;
		int counter = 0;
		Iterator<ExampleIndex> n = newExamples.iterator();
		while (n.hasNext()) {
			if (counter >= startNewP[p + 1]) {
				p++;
			}
			ExampleIndex exampleIndex = n.next();
			part[exampleIndex.exampleIndex] = p;
			counter++;
		}

		return part;
	}

	/**
	 * Returns a stratified partition for the given example set. The examples must have an nominal
	 * label. In contrast to {@link #createEqualPartition(double[], int, Attribute)} this method
	 * does not require the equal ratio values.
	 */
	private int[] createNonEqualPartition(double[] ratio, int size, Attribute label) {
		// fill list with example indices for each class
		Map<String, List<Integer>> classLists = new LinkedHashMap<String, List<Integer>>();
		Iterator<Example> reader = exampleSet.iterator();
		int index = 0;
		while (reader.hasNext()) {
			Example example = reader.next();
			String value = example.getNominalValue(label);
			List<Integer> classList = classLists.get(value);
			if (classList == null) {
				classList = new LinkedList<Integer>();
				classList.add(index++);
				classLists.put(value, classList);
			} else {
				classList.add(index++);
			}
		}

		return createPartitionsForClasses(ratio, classLists, exampleSet.size(), random);
	}

	/**
	 * Creates a partition that takes the class list into account.
	 *
	 * @param ratio
	 * 		the desired ratios
	 * @param classLists
	 * 		a map from classes to the rows where they appear
	 * @param size
	 * 		the size of the new partition
	 * @param random
	 * 		the random generator for shuffling
	 * @return the partition
	 */
	static int[] createPartitionsForClasses(double[] ratio, Map<String, List<Integer>> classLists, int size,
											Random random) {
		int[] part = new int[size];

		// shuffle each class list and create a partition for each class
		// seperately
		Iterator<List<Integer>> c = classLists.values().iterator();
		while (c.hasNext()) {
			List<Integer> classList = c.next();

			// shuffle
			Collections.shuffle(classList, random);

			// build partition starts
			int[] startNewP = new int[ratio.length + 1];
			startNewP[0] = 0;
			double ratioSum = 0;
			for (int i = 1; i < startNewP.length; i++) {
				ratioSum += ratio[i - 1];
				startNewP[i] = (int) Math.round(classList.size() * ratioSum);
			}

			// create a simple partition from the shuffled example indices and
			// partition starts
			int p = 0;
			int counter = 0;
			Iterator<Integer> n = classList.iterator();
			while (n.hasNext()) {
				if (counter >= startNewP[p + 1]) {
					p++;
				}
				Integer exampleIndex = n.next();
				part[exampleIndex.intValue()] = p;
				counter++;
			}
		}

		return part;
	}
}
