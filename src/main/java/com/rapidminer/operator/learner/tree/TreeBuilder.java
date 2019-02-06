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

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.tree.criterions.Criterion;


/**
 * Build a tree from an example set.
 *
 * @author Ingo Mierswa
 */
public class TreeBuilder {

	protected Terminator minLeafSizeTerminator;

	private List<Terminator> otherTerminators;

	private int minSizeForSplit = 2;

	private Criterion criterion;

	private NumericalSplitter splitter;

	protected SplitPreprocessing preprocessing = null;

	private Pruner pruner;

	protected LeafCreator leafCreator = new DecisionTreeLeafCreator();

	protected int numberOfPrepruningAlternatives = 0;

	protected boolean usePrePruning = true;

	public TreeBuilder(Criterion criterion, List<Terminator> terminationCriteria, Pruner pruner,
			SplitPreprocessing preprocessing, LeafCreator leafCreator, boolean noPrePruning,
			int numberOfPrepruningAlternatives, int minSizeForSplit, int minLeafSize) {
		this.minLeafSizeTerminator = new MinSizeTermination(minLeafSize);
		this.otherTerminators = terminationCriteria;
		this.otherTerminators.add(this.minLeafSizeTerminator);

		this.usePrePruning = !noPrePruning;
		this.numberOfPrepruningAlternatives = Math.max(0, numberOfPrepruningAlternatives);
		this.minSizeForSplit = minSizeForSplit;

		this.leafCreator = leafCreator;
		this.criterion = criterion;
		this.splitter = new NumericalSplitter(this.criterion);
		this.pruner = pruner;
		this.preprocessing = preprocessing;
	}

	public Tree learnTree(ExampleSet exampleSet) throws OperatorException {

		// grow tree
		Tree root = new Tree((ExampleSet) exampleSet.clone());
		if (shouldStop(exampleSet, 0)) {
			leafCreator.changeTreeToLeaf(root, exampleSet);
		} else {
			buildTree(root, exampleSet, 1);
		}

		// prune
		if (pruner != null) {
			pruner.prune(root);
		}

		return root;
	}

	/**
	 * This method calculates the benefit of the given attribute. This implementation utilizes the
	 * defined {@link Criterion}. Subclasses might want to override this method in order to
	 * calculate the benefit in other ways.
	 */
	public Benefit calculateBenefit(ExampleSet trainingSet, Attribute attribute) throws OperatorException {
		if (attribute.isNominal()) {
			return new Benefit(criterion.getNominalBenefit(trainingSet, attribute), attribute);
		} else {
			// numerical attribute
			double splitValue = splitter.getBestSplit(trainingSet, attribute);
			if (!Double.isNaN(splitValue)) {
				return new Benefit(criterion.getNumericalBenefit(trainingSet, attribute, splitValue), attribute, splitValue);
			} else {
				return null;
			}
		}
	}

	protected boolean shouldStop(ExampleSet exampleSet, int depth) {
		if (usePrePruning && exampleSet.size() < minSizeForSplit) {
			return true;
		} else {
			for (Terminator terminator : otherTerminators) {
				if (terminator.shouldStop(exampleSet, depth)) {
					return true;
				}
			}
			return false;
		}
	}

	protected Vector<Benefit> calculateAllBenefits(ExampleSet trainingSet) throws OperatorException {
		Vector<Benefit> benefits = new Vector<Benefit>();
		for (Attribute attribute : trainingSet.getAttributes()) {
			Benefit currentBenefit = calculateBenefit(trainingSet, attribute);
			if (currentBenefit != null) {
				benefits.add(currentBenefit);
			}
		}
		return benefits;
	}

	protected void buildTree(Tree current, ExampleSet exampleSet, int depth) throws OperatorException {
		// terminate (beginning of recursive method!)
		if (shouldStop(exampleSet, depth)) {
			leafCreator.changeTreeToLeaf(current, exampleSet);
			return;
		}

		// preprocessing
		if (preprocessing != null) {
			exampleSet = preprocessing.preprocess(exampleSet);
		}

		ExampleSet trainingSet = (ExampleSet) exampleSet.clone();

		// calculate all benefits
		Vector<Benefit> benefits = calculateAllBenefits(exampleSet);

		// sort all benefits
		Collections.sort(benefits);

		// try at most k benefits and check if prepruning is fulfilled
		boolean splitFound = false;
		for (int a = 0; a < numberOfPrepruningAlternatives + 1; a++) {
			// break if no benefits are left
			if (benefits.size() <= 0) {
				break;
			}

			// search current best
			Benefit bestBenefit = benefits.remove(0);

			// check if minimum gain was reached
			if (usePrePruning && bestBenefit.getBenefit() <= 0) {
				continue;
			}

			// split by best attribute
			SplittedExampleSet splitted = null;
			Attribute bestAttribute = bestBenefit.getAttribute();
			double bestSplitValue = bestBenefit.getSplitValue();
			if (bestAttribute.isNominal()) {
				splitted = SplittedExampleSet.splitByAttribute(trainingSet, bestAttribute);
			} else {
				splitted = SplittedExampleSet.splitByAttribute(trainingSet, bestAttribute, bestSplitValue);
			}

			// check if children all have the minimum size
			boolean splitOK = true;
			if (usePrePruning) {
				for (int i = 0; i < splitted.getNumberOfSubsets(); i++) {
					splitted.selectSingleSubset(i);
					if (splitted.size() > 0 && minLeafSizeTerminator.shouldStop(splitted, depth)) {
						splitOK = false;
						break;
					}
				}
			}

			// if all have minimum size --> remove nominal attribute and recursive call for each
			// subset
			if (splitOK) {
				if (bestAttribute.isNominal()) {
					splitted.getAttributes().remove(bestAttribute);
				}
				for (int i = 0; i < splitted.getNumberOfSubsets(); i++) {
					splitted.selectSingleSubset(i);
					if (splitted.size() > 0) {
						Tree child = new Tree(splitted.clone());
						SplitCondition condition = null;
						if (bestAttribute.isNominal()) {
							condition = new NominalSplitCondition(bestAttribute, splitted.getExample(0).getValueAsString(
									bestAttribute));
						} else {
							if (i == 0) {
								condition = new LessEqualsSplitCondition(bestAttribute, bestSplitValue);
							} else {
								condition = new GreaterSplitCondition(bestAttribute, bestSplitValue);
							}
						}
						current.addChild(child, condition);
						buildTree(child, splitted, depth + 1);
					}
				}

				// end loop
				splitFound = true;
				break;
			} else {
				continue;
			}
		}

		// no split found --> change to leaf and return
		if (!splitFound) {
			leafCreator.changeTreeToLeaf(current, trainingSet);
		}
	}
}
