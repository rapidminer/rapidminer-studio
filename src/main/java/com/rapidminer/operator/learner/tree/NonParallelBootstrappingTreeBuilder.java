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
package com.rapidminer.operator.learner.tree;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.tree.criterions.ColumnCriterion;


/**
 * Builds a tree, not in parallel with a shared {@link ColumnExampleTable}. A boostrapping is applied before starting
 * the procedure. In each splitting step the attribute selection is preprocessed. The preprocessings must be non-null.
 * The splitting of numeric attributes also be done randomly instead of checking each possible split to find the best.
 *
 * @author Gisa Meier
 * @since 8.0
 */
public class NonParallelBootstrappingTreeBuilder extends NonParallelTreeBuilder {

	private final int startSelectionSeed;
	private final int randomBenefitSeed;
	private final ColumnExampleTable sharedColumnTable;
	private final boolean randomSplits;

	/**
	 * Checks that the preprocessing and column table are not null. Stores the column table, whether to use random
	 * splits and the seeds and pipes the other parameters to the super constructor.
	 */
	public NonParallelBootstrappingTreeBuilder(Operator operator, ColumnCriterion criterion,
			List<ColumnTerminator> terminationCriteria, Pruner pruner, AttributePreprocessing preprocessing,
			boolean prePruning, int numberOfPrepruningAlternatives, int minSizeForSplit, int minLeafSize,
			Random seedProvider, ColumnExampleTable sharedColumnTable, boolean randomSplits) {
		super(operator, criterion, terminationCriteria, pruner, preprocessing, prePruning, numberOfPrepruningAlternatives,
				minSizeForSplit, minLeafSize);
		// the preprocessing must be non-zero
		if (preprocessing == null) {
			throw new IllegalArgumentException("preprocessing must not be null");
		}
		this.startSelectionSeed = seedProvider.nextInt(Integer.MAX_VALUE);
		this.randomBenefitSeed = seedProvider.nextInt();
		if (sharedColumnTable == null) {
			throw new IllegalArgumentException("column table must not be null");
		}
		this.sharedColumnTable = sharedColumnTable;
		this.randomSplits = randomSplits;
	}


	@Override
	public Tree learnTree(ExampleSet exampleSet) throws OperatorException {
		//reuse one table for several trees
		columnTable = sharedColumnTable;
		if (randomSplits) {
			benefitCalculator = new RandomBenefitCalculator(columnTable, criterion, operator, randomBenefitSeed);
		} else {
			benefitCalculator = new BenefitCalculator(columnTable, criterion, operator);
		}
		selectionCreator = new SelectionCreator(columnTable);

		Map<Integer, int[]> allSelectedExamples = createExampleStartSelection();
		int[] selectedExamples = SelectionCreator.getArbitraryValue(allSelectedExamples);
		int[] selectedAttributes = selectionCreator.createFullArray(columnTable.getTotalNumberOfRegularAttributes());

		// grow tree
		boolean isNominal = exampleSet.getAttributes().getLabel().isNominal();
		Tree root;
		if (isNominal) {
			root = new Tree(null);
		} else {
			root = new RegressionTree(null);
		}
		if (shouldStop(selectedExamples, selectedAttributes, 0)) {
			leafCreator.changeTreeToLeaf(root, columnTable, selectedExamples);
		} else {
			startTree(root, allSelectedExamples, selectedAttributes, 1);
		}

		// prune
		if (pruner != null) {
			pruner.prune(root);
		}

		return root;
	}

	/**
	 * Create a start selection that is a random selection of rows. This has the same effect as doing a bootstrapping on
	 * the column table.
	 */
	@Override
	protected Map<Integer, int[]> createExampleStartSelection() {
		Map<Integer, int[]> selection = new HashMap<>();
		if (columnTable.getNumberOfRegularNumericalAttributes() == 0) {
			selection.put(0, createFullRandomArray(columnTable.getNumberOfExamples()));
		} else {
			Integer[] bigSelectionArray = createFullBigRandomArray(columnTable.getNumberOfExamples());
			for (int j = columnTable.getNumberOfRegularNominalAttributes(); j < columnTable
					.getTotalNumberOfRegularAttributes(); j++) {
				final double[] attributeColumn = columnTable.getNumericalAttributeColumn(j);
				Integer[] startSelection = Arrays.copyOf(bigSelectionArray, bigSelectionArray.length);
				Arrays.sort(startSelection, Comparator.comparingDouble(a -> attributeColumn[a]));
				selection.put(j, ArrayUtils.toPrimitive(startSelection));
			}
		}
		return selection;
	}

	/**
	 * Create a random selection array containing all rows, i.e. containing random numbers from [0..length-1]
	 */
	private int[] createFullRandomArray(int length) {
		Random random = new Random(startSelectionSeed);
		int[] fullSelection = new int[length];
		for (int i = 0; i < length; i++) {
			fullSelection[i] = random.nextInt(length);
		}
		return fullSelection;
	}

	/**
	 * Create an Integer array containing random numbers from [0..length-1]
	 */
	private Integer[] createFullBigRandomArray(int length) {
		Random random = new Random(startSelectionSeed);
		Integer[] fullSelection = new Integer[length];
		for (int i = 0; i < length; i++) {
			fullSelection[i] = random.nextInt(length);
		}
		return fullSelection;
	}

}
