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

import static com.rapidminer.example.set.SplittedExampleSet.AUTOMATIC;
import static com.rapidminer.example.set.SplittedExampleSet.LINEAR_SAMPLING;
import static com.rapidminer.example.set.SplittedExampleSet.SHUFFLED_SAMPLING;
import static com.rapidminer.example.set.SplittedExampleSet.STRATIFIED_SAMPLING;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.reader.NumericReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Tools;


/**
 * A helper to split a {@link Table} into subsets by using a partition. Works similar to {@link SplittedExampleSet}
 * but creates a new {@link Table} on {@link #selectSingleSubset} and {@link #selectAllSubsetsBut} instead of
 * changing the internal state.
 *
 * @author Gisa Meier
 * @since 9.4.0
 * @see SplittedExampleSet
 */
public class TableSplitter {

	/** The partition. */
	private ImmutablePartition partition;

	/** The table. */
	private Table table;

	/** Constructs a helper to split a table with regard to the given partition. */
	private TableSplitter(Table table, ImmutablePartition partition) {
		this.table = table;
		this.partition = partition;
	}

	/**
	 * Constructs a helper to split a table with regard to the given partition elements and number of partitions.
	 *
	 * @param table
	 * 		the table to split
	 * @param partitionElements
	 * 		a map from row to partition index
	 * @param numberOfPartitions
	 * 		the number o partitions
	 */
	public TableSplitter(Table table, int[] partitionElements, int numberOfPartitions){
		this(table, new ImmutablePartition(partitionElements, numberOfPartitions));
	}

	/**
	 * Creates a helper to split a table into two subsets using the given sampling type.
	 *
	 * @param table
	 * 		the table to split
	 * @param splitRatio
	 * 		the ratio for the first set (as value between {@code 0} and {@code 1}), the second set will have the
	 * 		complementary ratio so that they add up to {@code 1}
	 * @param samplingType
	 * 		the sampling type, one of {@link SplittedExampleSet#LINEAR_SAMPLING},
	 * 		{@link SplittedExampleSet#SHUFFLED_SAMPLING}, {@link SplittedExampleSet#STRATIFIED_SAMPLING} or
	 * 		{@link SplittedExampleSet#AUTOMATIC}
	 * @param useLocalRandomSeed
	 * 		whether to use the random seed given as the next parameter
	 * @param seed
	 * 		the local random seed to use
	 * @throws UserError
	 * 		if stratified sampling is selected but no label exists or it is not nominal
	 */
	public TableSplitter(Table table, double splitRatio, int samplingType, boolean useLocalRandomSeed,
						 int seed) throws UserError {
		this(table, new double[]{splitRatio, 1 - splitRatio}, samplingType, useLocalRandomSeed, seed);
	}

	/**
	 * Creates a helper to split a table into two subsets using the given sampling type.
	 *
	 * @param table
	 * 		the table to split
	 * @param splitRatios
	 * 		array of positive ratios, summing up to 1
	 * @param samplingType
	 * 		the sampling type, one of {@link SplittedExampleSet#LINEAR_SAMPLING},
	 * 		{@link SplittedExampleSet#SHUFFLED_SAMPLING}, {@link SplittedExampleSet#STRATIFIED_SAMPLING} or
	 * 		{@link SplittedExampleSet#AUTOMATIC}
	 * @param useLocalRandomSeed
	 * 		whether to use the random seed given as the next parameter
	 * @param seed
	 * 		the local random seed to use
	 * @throws UserError
	 * 		if stratified sampling is selected but no label exists or it is not nominal
	 */
	public TableSplitter(Table table, double[] splitRatios, int samplingType, boolean useLocalRandomSeed,
						 int seed) throws UserError {
		this(table, new ImmutablePartition(splitRatios, table.height(),
				createPartitionBuilder(table, samplingType, useLocalRandomSeed, seed)));
	}

	/**
	 * Creates a helper to split a table into {@code numberOfSubsets} parts with the given sampling type.
	 *
	 * @param table
	 * 		the table to split
	 * @param numberOfSubsets
	 * 		the number of subsets to partition in
	 * @param samplingType
	 * 		the sampling type, one of {@link SplittedExampleSet#LINEAR_SAMPLING},
	 * 		{@link SplittedExampleSet#SHUFFLED_SAMPLING}, {@link SplittedExampleSet#STRATIFIED_SAMPLING} or
	 * 		{@link SplittedExampleSet#AUTOMATIC}
	 * @param useLocalRandomSeed
	 * 		whether to use the random seed given as the next parameter
	 * @param seed
	 * 		the local random seed to use
	 * @throws UserError
	 * 		if stratified sampling is selected but no label exists or it is not nominal
	 */
	public TableSplitter(Table table, int numberOfSubsets, int samplingType, boolean useLocalRandomSeed,
						 int seed) throws UserError {
		this(table, new ImmutablePartition(numberOfSubsets, table.height(),
				createPartitionBuilder(table, samplingType, useLocalRandomSeed, seed)));
	}

	/**
	 * Selects exactly one subset.
	 *
	 * @param index
	 * 		the subset to select
	 * @param context
	 * 		the context to use for the selection
	 * @return a new table with the rows specified by the selected subset
	 */
	public Table selectSingleSubset(int index, Context context) {
		return table.rows(partition.selectSubset(index), true, context);
	}

	/**
	 * Selects all but one subset.
	 *
	 * @param index
	 * 		the subset not to select
	 * @param context
	 * 		the context to use for the selection
	 * @return a new table with the rows specified by the selected subsets
	 */
	public Table selectAllSubsetsBut(int index, Context context) {
		return table.rows(partition.selectAllSubsetsBut(index), true, context);
	}

	/**
	 * Selects all subsets.
	 *
	 * @return the whole table
	 */
	public Table selectAllSubsets() {
		// partition.selectAllSubsets() creates an identity row selection without any reordering, so
		// table.rows(partition.selectAllSubsets(), true, context) is the same as table
		return table;
	}

	/** Returns the number of subsets. */
	public int getNumberOfSubsets() {
		return partition.getNumberOfSubsets();
	}

	/**
	 * Works only for nominal and integer columns. If <i>k</i> is the number of different values, this method creates a
	 * helper to split a table into <i>k</i> subsets according to the value of the given column.
	 *
	 * @param table
	 * 		the table to create a splitter for
	 * @param columnName
	 * 		the name of the nominal or integer column to split by
	 * @return a splitter to split into <i>k</i> parts
	 * @throws IllegalArgumentException
	 * 		if the table does not contain the columnName
	 * @throws UnsupportedOperationException
	 * 		if the specified column is not numeric readable
	 * @see SplittedExampleSet#splitByAttribute(ExampleSet, Attribute)
	 */
	public static TableSplitter splitByAttribute(Table table, String columnName) {
		Column column = table.column(columnName);
		int[] elements = new int[table.height()];
		int i = 0;
		Map<Integer, Integer> indexMap = new HashMap<>();
		int currentIndex = 0;
		for (NumericReader reader = Readers.numericReader(column); reader.hasRemaining(); ) {
			int value = (int) reader.read();
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
		ImmutablePartition partition = new ImmutablePartition(elements, maxNumber);
		return new TableSplitter(table, partition);
	}


	/**
	 * Works only for real-value columns. Returns a helper to split a table into two parts containing all rows
	 * providing a greater (smaller) value for the given columnName than the given value. The first partition contains
	 * all examples providing a smaller or the same value than the given one.
	 *
	 * @param table
	 * 		the table to create a splitter for
	 * @param columnName
	 * 		the name of the numeric column to split by
	 * @param value
	 * 		the value to split by
	 * @return a splitter to split into two parts
	 * @throws IllegalArgumentException
	 * 		if the table does not contain the columnName
	 * @throws UnsupportedOperationException
	 * 		if the specified column is not numeric readable
	 * @see SplittedExampleSet#splitByAttribute(ExampleSet, Attribute, double)
	 */
	public static TableSplitter splitByAttribute(Table table, String columnName, double value) {
		Column column = table.column(columnName);
		int[] elements = new int[table.height()];
		int i = 0;
		for (NumericReader reader = Readers.numericReader(column); reader.hasRemaining(); ) {
			double currentValue = reader.read();
			if (Tools.isLessEqual(currentValue, value)) {
				elements[i++] = 0;
			} else {
				elements[i++] = 1;
			}
		}
		ImmutablePartition partition = new ImmutablePartition(elements, 2);
		return new TableSplitter(table, partition);
	}


	/**
	 * Creates the partition builder for the given sampling type.
	 *
	 * @throws UserError
	 * 		if stratified sampling is selected but no label exists or it is not nominal
	 */
	private static PartitionBuilder createPartitionBuilder(Table table, int samplingType,
														   boolean useLocalRandomSeed, int seed) throws UserError {
		switch (samplingType) {
			case LINEAR_SAMPLING:
				return new SimplePartitionBuilder();
			case SHUFFLED_SAMPLING:
				return new ShuffledPartitionBuilder(useLocalRandomSeed, seed);
			case STRATIFIED_SAMPLING:
			case AUTOMATIC:
			default:
				List<String> labels = table.select().withMetaData(ColumnRole.LABEL).labels();
				if (!labels.isEmpty() && table.column(labels.get(0)).type().id() == Column.TypeId.NOMINAL) {
					return new ColumnStratifiedPartitionBuilder(table.column(labels.get(0)), useLocalRandomSeed, seed);
				} else {
					if (samplingType == AUTOMATIC &&
							(labels.isEmpty() || table.column(labels.get(0)).type().id() != Column.TypeId.NOMINAL)) {
						return new ShuffledPartitionBuilder(useLocalRandomSeed, seed);
					}
					//throw same errors as done in SplittedExampleSet#createPartitionBuilder
					throwOnNotNominalLabel(labels, table);
					return new ShuffledPartitionBuilder(useLocalRandomSeed, seed);
				}
		}
	}

	/**
	 * Throws a user error if there is no label or it is not nominal. The errors are the same as thrown by {@link
	 * com.rapidminer.example.Tools#hasNominalLabels}.
	 */
	private static void throwOnNotNominalLabel(List<String> labels, Table table) throws UserError {
		if (labels.isEmpty()) {
			throw new UserError(null, 105);
		}
		if (table.column(labels.get(0)).type().id() != Column.TypeId.NOMINAL) {
			throw new UserError(null, 101, "stratified sampling", labels.get(0));
		}
	}

}
