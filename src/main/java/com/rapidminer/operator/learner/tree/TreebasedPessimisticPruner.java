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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.rapidminer.tools.math.MathFunctions;


/**
 * This class provides a pruner based on some heuristic statistics. It cuts the tree to reduce
 * overfitting. The pruning only uses the information of the tree structure and not the example sets
 * that can be saved in tree nodes.
 *
 * @author Sebastian Land, Ingo Mierswa, Gisa Schaefer
 */
public class TreebasedPessimisticPruner implements Pruner {

	private static final double PRUNE_PREFERENCE = 0.001;

	private double confidenceLevel;

	public TreebasedPessimisticPruner(double confidenceLevel, LeafCreator leafCreator) {
		this.confidenceLevel = confidenceLevel;
	}

	@Override
	public void prune(Tree root) {
		Iterator<Edge> childIterator = root.childIterator();
		while (childIterator.hasNext()) {
			pruneChild(childIterator.next().getChild());
		}
	}

	/**
	 * Prunes the tree given by currentNode recursively.
	 *
	 * @param currentNode
	 */
	private void pruneChild(Tree currentNode) {
		// going down to fathers of leafs
		if (!currentNode.isLeaf()) {
			Iterator<Edge> childIterator = currentNode.childIterator();
			while (childIterator.hasNext()) {
				pruneChild(childIterator.next().getChild());
			}
			if (!childrenHaveChildren(currentNode)) {
				// calculating error estimate for leafs
				double leafsErrorEstimate = 0;
				int examplesCurrentNode = currentNode.getSubtreeFrequencySum();
				childIterator = currentNode.childIterator();
				Set<String> classSet = new HashSet<String>();
				// calculate sum of pessimistic errors of the child nodes
				while (childIterator.hasNext()) {
					Tree leafNode = childIterator.next().getChild();
					classSet.add(leafNode.getLabel());
					int examples = leafNode.getFrequencySum();
					double currentErrorRate = getErrorNumber(leafNode, leafNode.getLabel()) / (double) examples;
					leafsErrorEstimate += pessimisticErrors(examples, currentErrorRate, confidenceLevel)
							* ((double) examples / (double) examplesCurrentNode);
				}

				// calculating error estimate for current node
				if (classSet.size() <= 1) {
					changeToLeaf(currentNode);
				} else {
					String currentNodeLabel = prunedLabel(currentNode);
					double currentErrorRate = getErrorNumber(currentNode, currentNodeLabel) / (double) examplesCurrentNode;
					double nodeErrorEstimate = pessimisticErrors(examplesCurrentNode, currentErrorRate, confidenceLevel);
					// if currentNode error level is less than children: prune
					if (nodeErrorEstimate - PRUNE_PREFERENCE <= leafsErrorEstimate) {
						changeToLeaf(currentNode);
					}
				}
			}
		}
	}

	/**
	 * Checks if the children of the node have child nodes, i.e. are not leaves
	 *
	 * @param node
	 * @return
	 */
	private boolean childrenHaveChildren(Tree node) {
		Iterator<Edge> iterator = node.childIterator();
		while (iterator.hasNext()) {
			if (!iterator.next().getChild().isLeaf()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes the children of the node and adds the information a leaf must contain.
	 *
	 * @param node
	 */
	private void changeToLeaf(Tree node) {
		Map<String, Integer> counterMap = node.getSubtreeCounterMap();
		int maximum = 0;
		String label = "";
		for (String entry : counterMap.keySet()) {
			int number = counterMap.get(entry);
			node.addCount(entry, number); // needed since the counterMap of node does not get
			// changed by calling getSubtreeCounterMap
			if (number > maximum) {
				maximum = number;
				label = entry;
			}
		}
		node.removeChildren();
		node.setLeaf(label);
	}

	/**
	 * Counts how many examples represented in the node have a label different from label.
	 *
	 * @param node
	 * @param label
	 * @return
	 */
	private int getErrorNumber(Tree node, String label) {
		Map<String, Integer> counterMap;
		if (node.isLeaf()) {
			counterMap = node.getCounterMap();
		} else {
			counterMap = node.getSubtreeCounterMap();
		}
		int errors = 0;
		for (String entry : counterMap.keySet()) {
			if (!label.equals(entry)) {
				errors += counterMap.get(entry);
			}
		}
		return errors;
	}

	/**
	 * Calculates the label a node would have if it became a leaf.
	 *
	 * @param node
	 *            a node that is not a leaf
	 * @return the majority label
	 */
	public String prunedLabel(Tree node) {
		Map<String, Integer> counterMap = node.getSubtreeCounterMap();
		int maximum = 0;
		String label = "";
		for (String entry : counterMap.keySet()) {
			int number = counterMap.get(entry);
			if (number > maximum) {
				maximum = number;
				label = entry;
			}
		}
		return label;
	}

	/**
	 * Calculates the pessimistic number of errors, using some confidence level.
	 *
	 * @param numberOfExamples
	 * @param errorRate
	 * @param confidenceLevel
	 * @return
	 */
	public double pessimisticErrors(double numberOfExamples, double errorRate, double confidenceLevel) {
		if (errorRate < 1E-6) {
			return errorRate + numberOfExamples * (1.0 - Math.exp(Math.log(confidenceLevel) / numberOfExamples));
		} else if (errorRate + 0.5 >= numberOfExamples) {
			return errorRate + 0.67 * (numberOfExamples - errorRate);
		} else {
			double coefficient = MathFunctions.normalInverse(1 - confidenceLevel);
			coefficient *= coefficient;
			double pessimisticRate = (errorRate + 0.5 + coefficient / 2.0d + Math.sqrt(coefficient
					* ((errorRate + 0.5) * (1 - (errorRate + 0.5) / numberOfExamples) + coefficient / 4.0d)))
					/ (numberOfExamples + coefficient);
			return numberOfExamples * pessimisticRate;
		}
	}
}
