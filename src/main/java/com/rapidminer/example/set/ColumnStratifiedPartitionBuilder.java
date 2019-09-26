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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.reader.ObjectReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Table;
import com.rapidminer.example.set.StratifiedPartitionBuilder.ExampleIndex;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.RandomGenerator;


/**
 * Creates a shuffled and stratified partition for a {@link Column}. The table must have an
 * nominal label. This partition builder can work in two modes:
 * <ul>
 * <li>The first working mode is automatically used for generic types of ratio arrays, especially
 * for those with different sizes. Due to to this fact it however cannot longer be guaranteed that
 * each fold exactly contains the correct number of rows and each class at least once.</li>
 * <li>In contrast to the first mode the correct partition can at least be guaranteed for ratio
 * arrays containing the same ratio value for all folds. The second mode is automatically performed
 * in this case (e.g. for cross validation).</li> </ul>
 * <p>
 * Works analogously to {@link StratifiedPartitionBuilder} just for {@link Table}s instead of example sets.
 *
 * @author Ingo Mierswa, Gisa Meier
 * @since 9.4.0
 * @see StratifiedPartitionBuilder
 */
class ColumnStratifiedPartitionBuilder implements PartitionBuilder {

	private final Column column;

	private final Random random;

	/**
	 * Creates a partition builder which considers the class distribution for the given nominal column.
	 *
	 * @param column
	 * 		the nominal column to partition by
	 * @param useLocalRandomSeed
	 * 		specifies whether to use the given seed to create a random generator
	 * @param seed
	 * 		the seed for the random generator
	 * @throws IllegalArgumentException
	 * 		if column is not nominal
	 */
	ColumnStratifiedPartitionBuilder(Column column, boolean useLocalRandomSeed, int seed) {

		if (column.type().id() != Column.TypeId.NOMINAL) {
			throw new IllegalArgumentException("splitting column must be nominal");
		}
		this.column = column;
		this.random = RandomGenerator.getRandomGenerator(useLocalRandomSeed, seed);
	}

	/**
	 * Returns a stratified partition for the given nominal column. The size must equal the size of the column.
	 *
	 * @throws IllegalArgumentException
	 * 		if the given size is not the size of the column
	 */
	@Override
	public int[] createPartition(double[] ratio, int size) {
		// typical errors
		if (size != column.size()) {
			throw new IllegalArgumentException(
					"Cannot create stratified Partition: given size and size of the column must be equal!");
		}

		double firstValue = ratio[0];
		for (int i = 1; i < ratio.length; i++) {
			if (ratio[i] != firstValue) {
				LogService.getRoot().log(Level.FINE,
						"com.rapidminer.example.set.StratifiedPartitionBuilder.not_all_ratio_values_are_equal");
				return createNonEqualPartition(ratio);
			}
		}
		LogService.getRoot().log(Level.FINE,
				"com.rapidminer.example.set.StratifiedPartitionBuilder.all_ratio_values_are_equal");
		return createEqualPartition(ratio, size);
	}

	/**
	 * Returns a stratified partition for the given example set. The examples must have a nominal
	 * label.
	 */
	private int[] createEqualPartition(double[] ratio, int size) {
		// fill example list with indices and classes
		List<ExampleIndex> examples = new ArrayList<>(size);
		ObjectReader<String> reader = Readers.objectReader(column, String.class);
		int index = 0;
		while (reader.hasRemaining()) {
			examples.add(new ExampleIndex(index++, reader.read()));
		}

		return StratifiedPartitionBuilder.createPartitionFromIndices(ratio, examples, column.size(), random);
	}

	/**
	 * Returns a stratified partition for the given example set. The examples must have an nominal
	 * label. In contrast to {@link #createEqualPartition(double[], int)} this method
	 * does not require the equal ratio values.
	 */
	private int[] createNonEqualPartition(double[] ratio) {
		// fill list with example indices for each class
		Map<String, List<Integer>> classLists = new LinkedHashMap<>();
		ObjectReader<String> reader = Readers.objectReader(column, String.class);
		int index = 0;
		while (reader.hasRemaining()) {
			String value = reader.read();
			List<Integer> classList = classLists.get(value);
			if (classList == null) {
				classList = new LinkedList<>();
				classList.add(index++);
				classLists.put(value, classList);
			} else {
				classList.add(index++);
			}
		}

		return StratifiedPartitionBuilder.createPartitionsForClasses(ratio, classLists, column.size(), random);
	}
}
