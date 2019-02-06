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
import java.util.Map.Entry;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;


/**
 * The tree model is the model created by all decision trees with nominal label.
 *
 * @author Sebastian Land
 */
public class TreeModel extends TreePredictionModel {

	private static final long serialVersionUID = 4368631725370998591L;

	private Tree root;

	public TreeModel(ExampleSet exampleSet, Tree root) {
		super(exampleSet);
		if (root.isNumerical()) {
			throw new IllegalArgumentException("Only nominal trees allowed");
		}
		this.root = root;
	}

	@Override
	public Tree getRoot() {
		return this.root;
	}

	@Override
	public double predict(Example example) throws OperatorException {
		return predict(example, root);
	}

	private double predict(Example example, Tree node) {
		if (node.isLeaf()) {
			int[] counts = new int[getLabel().getMapping().size()];
			int sum = 0;
			for (Entry<String, Integer> entry : node.getCounterMap().entrySet()) {
				int count = entry.getValue();
				int index = getLabel().getMapping().getIndex(entry.getKey());
				counts[index] = count;
				sum += count;
			}
			for (int i = 0; i < counts.length; i++) {
				example.setConfidence(getLabel().getMapping().mapIndex(i), (double) counts[i] / sum);
			}
			return getLabel().getMapping().getIndex(node.getLabel());
		} else {
			Iterator<Edge> childIterator = node.childIterator();
			while (childIterator.hasNext()) {
				Edge edge = childIterator.next();
				SplitCondition condition = edge.getCondition();
				if (condition.test(example)) {
					return predict(example, edge.getChild());
				}
			}

			// nothing known from training --> use majority class in this node
			String majorityClass = null;
			int majorityCounter = -1;
			int[] counts = new int[getLabel().getMapping().size()];
			int sum = 0;
			for (Entry<String, Integer> entry : node.getSubtreeCounterMap().entrySet()) {
				String className = entry.getKey();
				int count = entry.getValue().intValue();
				int index = getLabel().getMapping().getIndex(className);
				counts[index] = count;
				sum += count;
				if (count > majorityCounter) {
					majorityCounter = count;
					majorityClass = className;
				}
			}

			for (int i = 0; i < counts.length; i++) {
				example.setConfidence(getLabel().getMapping().mapIndex(i), (double) counts[i] / sum);
			}

			if (majorityClass != null) {
				return getLabel().getMapping().getIndex(majorityClass);
			} else {
				return 0;
			}
		}
	}

	@Override
	public String toString() {
		return this.root.toString();
	}
}
