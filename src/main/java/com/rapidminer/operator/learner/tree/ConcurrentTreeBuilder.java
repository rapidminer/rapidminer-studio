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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.tree.criterions.ColumnCriterion;
import com.rapidminer.studio.internal.Resources;


/**
 * Build a tree from an example set in parallel. The benefit calculation for the attributes is
 * parallelized until the nodes are too small. The remaining nodes are split in parallel.
 *
 * @author Gisa Schaefer
 */
public class ConcurrentTreeBuilder extends AbstractParallelTreeBuilder {

	private static final int MINIMAL_EXAMPLES_FOR_GROWING_PARALLEL = 5000;

	private static final int MINIMAL_EXAMPLES_FOR_SORTING_PARALLEL = 10000;

	/**
	 * Pipes the arguments to the super constructor and sets an additional parameter allowing
	 * parallel table creation.
	 */
	public ConcurrentTreeBuilder(Operator operator, ColumnCriterion criterion, List<ColumnTerminator> terminationCriteria,
			Pruner pruner, AttributePreprocessing preprocessing, boolean noPrePruning, int numberOfPrepruningAlternatives,
			int minSizeForSplit, int minLeafSize) {
		super(operator, criterion, terminationCriteria, pruner, preprocessing, noPrePruning, numberOfPrepruningAlternatives,
				minSizeForSplit, minLeafSize, true);

	}

	@Override
	void startTree(Tree root, Map<Integer, int[]> allSelectedExamples, int[] selectedAttributes, int depth)
			throws OperatorException {

		// start the tree building attribute parallel
		NodeData rootNode = new NodeData(root, allSelectedExamples, selectedAttributes, depth);
		Deque<NodeData> queue = new ArrayDeque<>();
		queue.push(rootNode);
		List<NodeData> tooSmallList = new LinkedList<>();

		while (!queue.isEmpty()) {
			NodeData nextNode = queue.pop();
			if (nodeIsTooSmall(nextNode)) {
				tooSmallList.add(nextNode);
			} else {
				queue.addAll(splitNode(nextNode, true));
			}
		}

		// only small nodes are left, split them in parallel
		List<Callable<Void>> todo = new LinkedList<>();
		for (final NodeData node : tooSmallList) {
			Callable<Void> task = new Callable<Void>() {

				@Override
				public Void call() throws OperatorException {
					Deque<NodeData> queue = new ArrayDeque<>();
					queue.push(node);
					while (!queue.isEmpty()) {
						queue.addAll(splitNode(queue.pop(), false));
					}
					return null;
				}

			};
			todo.add(task);
		}
		if (operator != null && Resources.getConcurrencyContext(operator).getParallelism() > 1) {
			try {
				Resources.getConcurrencyContext(operator).call(todo);
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof OperatorException) {
					throw (OperatorException) cause;
				} else if (cause instanceof RuntimeException) {
					throw (RuntimeException) cause;
				} else if (cause instanceof Error) {
					throw (Error) cause;
				} else {
					throw new OperatorException(cause.getMessage(), cause);
				}
			}
		} else {
			for (Callable<Void> task : todo) {
				try {
					task.call();
				} catch (Exception e) {
					if (e instanceof OperatorException) {
						throw (OperatorException) e;
					} else if (e instanceof RuntimeException) {
						throw (RuntimeException) e;
					}
					// nothing else possible
				}
			}
		}

	}

	/**
	 * Decides whether the node is to small to do the splitting attribute parallel.
	 *
	 * @param nodeData
	 * @return
	 */
	boolean nodeIsTooSmall(NodeData nodeData) {
		return nodeData.getSelectedAttributes().length < 2
				|| SelectionCreator.getArbitraryValue(nodeData.getAllSelectedExamples()).length < MINIMAL_EXAMPLES_FOR_GROWING_PARALLEL;
	}

	@Override
	boolean doStartSelectionInParallel() {
		return columnTable.getNumberOfExamples() > MINIMAL_EXAMPLES_FOR_SORTING_PARALLEL
				&& columnTable.getNumberOfRegularNumericalAttributes() > 1;
	}

}
