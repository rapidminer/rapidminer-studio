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

import java.util.Iterator;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;


/**
 * The tree model is the model created by all decision trees with numerical label.
 *
 * @author Gisa Meier
 */
public class RegressionTreeModel extends TreePredictionModel {

	private static final long serialVersionUID = 4368631725370998591L;

	private RegressionTree root;

	public RegressionTreeModel(ExampleSet exampleSet, RegressionTree root) {
		super(exampleSet);
		this.root = root;
	}

	@Override
	public RegressionTree getRoot() {
		return this.root;
	}

	@Override
	public double predict(Example example) throws OperatorException {
		return predict(example, root);
	}

	/**
	 * Recursively predicts the value for the example using the given node.
	 */
	private double predict(Example example, RegressionTree node) {
		if (node.isLeaf()) {
			return node.getValue();
		} else {
			Iterator<Edge> childIterator = node.childIterator();
			while (childIterator.hasNext()) {
				Edge edge = childIterator.next();
				SplitCondition condition = edge.getCondition();
				if (condition.test(example)) {
					return predict(example, (RegressionTree) edge.getChild());
				}
			}

			// nothing known from training --> use average of this node
			return getChildAverage(node);

		}
	}

	/**
	 * Recursively calculates the average of all the children.
	 */
	private double getChildAverage(RegressionTree tree) {
		if (tree.isLeaf()) {
			return tree.getValue();
		}
		double sum = 0;
		for (Iterator<Edge> childIterator = tree.childIterator(); childIterator.hasNext();) {
			sum += getChildAverage((RegressionTree) childIterator.next().getChild());
		}
		return sum / tree.getNumberOfChildren();
	}

	@Override
	public String toString() {
		return this.root.toString();
	}
}
