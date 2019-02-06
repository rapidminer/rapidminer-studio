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
package com.rapidminer.operator.learner.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.tree.criterions.ColumnCriterion;
import com.rapidminer.studio.internal.Resources;


/**
 * Build a tree from an example set, possibly in parallel. During the tree building process the
 * examples and attributes at a node are represented by numbers coming from numbering all examples
 * and attributes at the beginning. In this numbering all nominal attributes come before all
 * numerical attributes. During the tree growing process the nodes are split into smaller ones.
 *
 * This class should be extended to specify if and how the calculations should be parallelized. By
 * implementing the method {@link #startTree} using {@link #splitNode} one can decide if and in
 * which direction the process of splitting the nodes should be parallelized. By implementing the
 * abstract method {@link #doStartSelectionInParallel()} one can decide if and when the start
 * selection of the examples should be done in parallel. (Note that this only has an effect if there
 * are numerical attributes.)
 *
 * @author Ingo Mierswa, Gisa Schaefer
 */
public abstract class AbstractParallelTreeBuilder {

	final protected Operator operator;

	final protected ColumnTerminator minLeafSizeTerminator;

	final protected List<ColumnTerminator> otherTerminators;

	final protected int minSizeForSplit;

	final protected ColumnCriterion criterion;

	protected BenefitCalculator benefitCalculator;

	protected SelectionCreator selectionCreator;

	final protected AttributePreprocessing preprocessing;

	final protected Pruner pruner;

	final protected ParallelDecisionTreeLeafCreator leafCreator = new ParallelDecisionTreeLeafCreator();

	protected int numberOfPrepruningAlternatives = 0;

	final protected boolean usePrePruning;

	protected ColumnExampleTable columnTable;

	final protected boolean parallelAllowed;

	/**
	 * Initializes the fields.
	 *
	 */
	public AbstractParallelTreeBuilder(Operator operator, ColumnCriterion criterion,
			List<ColumnTerminator> terminationCriteria, Pruner pruner, AttributePreprocessing preprocessing,
			boolean prePruning, int numberOfPrepruningAlternatives, int minSizeForSplit, int minLeafSize,
			boolean parallelAllowed) {
		this.operator = operator;

		this.minLeafSizeTerminator = new ColumnMinSizeTermination(minLeafSize);

		if (terminationCriteria == null) {
			throw new IllegalArgumentException("terminationCriteria must not be null!");
		}
		this.otherTerminators = terminationCriteria;
		if (prePruning) {
			this.otherTerminators.add(this.minLeafSizeTerminator);
		}

		this.usePrePruning = prePruning;
		if (prePruning) {
			this.numberOfPrepruningAlternatives = Math.max(0, numberOfPrepruningAlternatives);
		}
		this.minSizeForSplit = minSizeForSplit;

		if (criterion == null) {
			throw new IllegalArgumentException("criterion must not be null!");
		}
		this.criterion = criterion;
		this.pruner = pruner;
		this.preprocessing = preprocessing;
		this.parallelAllowed = parallelAllowed;
	}

	/**
	 * Creates a copy of the example set in form of the {@link ColumnExampleTable}, starts the tree
	 * growing procedure and prunes the finished tree.
	 *
	 * @param exampleSet
	 * @return
	 * @throws OperatorException
	 */
	public Tree learnTree(ExampleSet exampleSet) throws OperatorException {

		// preprocess example set before creating the table
		exampleSet = preprocessExampleSet(exampleSet);

		columnTable = new ColumnExampleTable(exampleSet, operator, parallelAllowed);
		benefitCalculator = new BenefitCalculator(columnTable, criterion, operator);
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
	 * Hook for preprocessing the example set before building the {@link ColumnExampleTable}.
	 *
	 * @param exampleSet
	 * @return
	 */
	protected ExampleSet preprocessExampleSet(ExampleSet exampleSet) {
		return exampleSet;
	}

	/**
	 * Creates for every numerical attribute a sorted start selection, possibly in parallel.
	 *
	 * @return
	 * @throws OperatorException
	 */
	protected Map<Integer, int[]> createExampleStartSelection() throws OperatorException {
		Map<Integer, int[]> allSelectedExamples;
		if (doStartSelectionInParallel() && operator != null) {
			allSelectedExamples = selectionCreator.getStartSelectionParallel(operator);
		} else {
			allSelectedExamples = selectionCreator.getStartSelection();
		}
		return allSelectedExamples;
	}

	/**
	 * Decides whether the start selection should be created in parallel.
	 *
	 * @return
	 */
	abstract boolean doStartSelectionInParallel();

	/**
	 * Starts the tree building process for the given parameters.
	 *
	 * @param root
	 * @param allSelectedExamples
	 * @param selectedAttributes
	 * @param depth
	 * @throws OperatorException
	 */
	abstract void startTree(Tree root, Map<Integer, int[]> allSelectedExamples, int[] selectedAttributes, int depth)
			throws OperatorException;

	/**
	 * Splits the node given by the nodeData by calculating the attribute with the best benefit.
	 *
	 * @param nodeData
	 * @param attributeParallel
	 *            if <code>true</code> the calculation of the benefits is done in parallel by
	 *            attributes
	 * @return
	 * @throws OperatorException
	 */
	protected Collection<NodeData> splitNode(NodeData nodeData, boolean attributeParallel) throws OperatorException {
		// check if operator was stopped
		if (operator != null) {
			Resources.getConcurrencyContext(operator).checkStatus();
		}

		Map<Integer, int[]> allSelectedExamples = nodeData.getAllSelectedExamples();
		int[] originalSelectedAttributes = nodeData.getSelectedAttributes();
		Tree current = nodeData.getTree();
		int depth = nodeData.getDepth();

		// terminate
		int[] selectedExamples = SelectionCreator.getArbitraryValue(allSelectedExamples);
		if (shouldStop(selectedExamples, originalSelectedAttributes, depth)) {
			leafCreator.changeTreeToLeaf(current, columnTable, selectedExamples);
			return Collections.emptyList();
		}

		int[] selectedAttributes = originalSelectedAttributes;
		// preprocessing
		if (preprocessing != null) {
			selectedAttributes = preprocessing.preprocess(originalSelectedAttributes);
		}

		// calculate all benefits
		List<ParallelBenefit> benefits = getBenefits(allSelectedExamples, selectedAttributes, attributeParallel);
		// sort all benefits
		Collections.sort(benefits);

		// try at most k benefits and check if prepruning is fulfilled
		for (int a = 0; a < numberOfPrepruningAlternatives + 1; a++) {
			// break if no benefits are left
			if (benefits.isEmpty()) {
				break;
			}

			// search current best
			ParallelBenefit bestBenefit = benefits.remove(0);

			// check if minimum gain was reached when using prepruning and if the benefit results in
			// a split with more than one child
			if (usePrePruning && bestBenefit.getBenefit() <= 0 || !usePrePruning
					&& !(bestBenefit.getBenefit() > Double.NEGATIVE_INFINITY)) {
				break;
			}

			// split by best attribute
			int bestAttribute = bestBenefit.getAttributeNumber();

			double bestSplitValue = bestBenefit.getSplitValue();
			Collection<Map<Integer, int[]>> splits = selectionCreator.getSplits(allSelectedExamples, bestAttribute,
					bestSplitValue);

			// if all have minimum size --> remove nominal attribute and recursive call for each
			// subset
			if (isSplitOK(selectedAttributes, depth, splits)) {
				int[] remainingAttributes = selectionCreator.updateRemainingAttributes(originalSelectedAttributes, bestAttribute);
				LinkedList<NodeData> children = new LinkedList<>();

				int i = 0;
				for (Map<Integer, int[]> split : splits) {
					if (SelectionCreator.getArbitraryValue(split).length > 0) {
						Tree child;
						if (current.isNumerical()) {
							child = new RegressionTree(null);
						} else {
							child = new Tree(null);
						}
						addToParentTree(current, child, bestAttribute, bestSplitValue,
								SelectionCreator.getArbitraryValue(split), i);
						NodeData newNode = new NodeData(child, split, remainingAttributes, depth + 1);
						children.add(newNode);
						i++;
					}
				}
				current.setBenefit(bestBenefit.getBenefit());

				// end loop
				return children;
			}
			// no valid split found - try again
		}

		// no split found --> change to leaf and return
		leafCreator.changeTreeToLeaf(current, columnTable, selectedExamples);
		return Collections.emptyList();
	}

	/**
	 * Checks if the tree building should stop. The terminators are checked and, when prepruning is
	 * activated, the minimal size for a split is checked as well.
	 *
	 * @param selectedExamples
	 * @param selectedAttributes
	 * @param depth
	 * @return
	 */
	protected boolean shouldStop(int[] selectedExamples, int[] selectedAttributes, int depth) {
		if (usePrePruning && selectedExamples.length < minSizeForSplit) {
			return true;
		} else {
			for (ColumnTerminator terminator : otherTerminators) {
				if (terminator.shouldStop(selectedExamples, selectedAttributes, columnTable, depth)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * For each attribute calculate the benefit for splitting there, possibly in parallel if
	 * attributeParalle is <code>true</code>.
	 *
	 * @param allSelectedExamples
	 * @param selectedAttributes
	 * @param attributeParallel
	 * @return
	 * @throws OperatorException
	 */
	protected List<ParallelBenefit> getBenefits(Map<Integer, int[]> allSelectedExamples, int[] selectedAttributes,
			boolean attributeParallel) throws OperatorException {
		List<ParallelBenefit> benefits;
		if (attributeParallel && operator != null) {
			benefits = benefitCalculator.calculateAllBenefitsParallel(allSelectedExamples, selectedAttributes);
		} else {
			benefits = benefitCalculator.calculateAllBenefits(allSelectedExamples, selectedAttributes);
		}
		return benefits;
	}

	/**
	 * Checks in the case of prepruning whether the minimal leaf size is satisfied.
	 *
	 * @param selectedAttributes
	 * @param depth
	 * @param splits
	 * @return
	 */
	private boolean isSplitOK(int[] selectedAttributes, int depth, Collection<Map<Integer, int[]>> splits) {
		// check if children all have the minimum size
		boolean splitOK = true;
		if (usePrePruning) {
			for (Map<Integer, int[]> splitinfo : splits) {
				int[] split = SelectionCreator.getArbitraryValue(splitinfo);
				if (split.length > 0 && minLeafSizeTerminator.shouldStop(split, selectedAttributes, columnTable, depth)) {
					splitOK = false;
					break;
				}
			}
		}
		return splitOK;
	}

	/**
	 * Adds the child tree to the parent tree via an edge describing the split.
	 *
	 * @param parent
	 * @param bestAttribute
	 * @param bestSplitValue
	 * @param counter
	 * @param split
	 * @param child
	 */
	private void addToParentTree(Tree parent, Tree child, int bestAttribute, double bestSplitValue, int[] split, int counter) {
		SplitCondition condition = null;
		if (columnTable.representsNominalAttribute(bestAttribute)) {
			// find the attribute value we are splitting
			Attribute best = columnTable.getNominalAttribute(bestAttribute);
			final byte index = columnTable.getNominalAttributeColumn(bestAttribute)[split[0]];
			String splitValueName;
			// NaNs are represented by the number mapping size
			if (index == best.getMapping().size()) {
				splitValueName = null;
			} else {
				splitValueName = best.getMapping().mapIndex(index);
			}
			condition = new NominalSplitCondition(best, splitValueName);
		} else {
			if (counter == 0) {
				condition = new LessEqualsSplitCondition(columnTable.getNumericalAttribute(bestAttribute), bestSplitValue);
			} else if (counter == 1) {
				condition = new GreaterSplitCondition(columnTable.getNumericalAttribute(bestAttribute), bestSplitValue);
			} else {
				condition = new NumericalMissingSplitCondition(columnTable.getNumericalAttribute(bestAttribute));
			}
		}
		parent.addChild(child, condition);
	}

	/**
	 * Class to bundle the parameters of {@link AbstractParallelTreeBuilder#splitNode(NodeData, boolean)}.
	 */
	protected class NodeData {

		Tree tree;
		Map<Integer, int[]> allSelectedExamples;
		int[] selectedAttributes;
		int depth;

		NodeData(Tree tree, Map<Integer, int[]> allSelectedExamples, int[] selectedAttributes, int depth) {
			this.tree = tree;
			this.allSelectedExamples = allSelectedExamples;
			this.selectedAttributes = selectedAttributes;
			this.depth = depth;
		}

		Tree getTree() {
			return tree;
		}

		Map<Integer, int[]> getAllSelectedExamples() {
			return allSelectedExamples;
		}

		int[] getSelectedAttributes() {
			return selectedAttributes;
		}

		int getDepth() {
			return depth;
		}
	}

}
