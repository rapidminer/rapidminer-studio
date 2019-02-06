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

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.tools.Tools;


/**
 * A tree is a node in a tree model containing several edges to other trees (children) combined with
 * conditions at these edges.
 *
 * Leafs contain the class label which should be predicted.
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class Tree implements Serializable {

	private static final long serialVersionUID = -5930873649086170840L;

	private String label = null;

	private List<Edge> children = new LinkedList<>();

	private Map<String, Integer> counterMap = new LinkedHashMap<>();

	/**
	 * transient because not used for prediction but only for pruning in deprecated tree, {@link PessimisticPruner}.
	 */
	private transient ExampleSet trainingSet = null;

	/**
	 * transient because not used for prediction but only for calculation of attribute weights inside tree operator.
	 */
	private transient double benefit = Double.NaN;


	public Tree(ExampleSet trainingSet) {
		this.trainingSet = trainingSet;
	}

	public ExampleSet getTrainingSet() {
		return this.trainingSet;
	}


	/**
	 * @return {@code true} if the tree has numerical leaves
	 */
	public boolean isNumerical() {
		return false;
	}

	public void addCount(String className, int count) {
		counterMap.put(className, count);
	}


	public int getCount(String className) {
		Integer count = counterMap.get(className);
		if (count == null) {
			return 0;
		} else {
			return count;
		}
	}

	public int getFrequencySum() {
		int sum = 0;
		for (Integer i : counterMap.values()) {
			sum += i;
		}
		return sum;
	}

	public int getSubtreeFrequencySum() {
		if (children.isEmpty()) {
			return getFrequencySum();
		} else {
			int sum = 0;
			for (Edge edge : children) {
				sum += edge.getChild().getSubtreeFrequencySum();
			}
			return sum;
		}
	}

	/**
	 * This returns the class counts from all contained examples by iterating recursively through
	 * the tree.
	 */
	public Map<String, Integer> getSubtreeCounterMap() {
		Map<String, Integer> counterMap = new LinkedHashMap<>();
		fillSubtreeCounterMap(counterMap);
		return counterMap;
	}

	protected void fillSubtreeCounterMap(Map<String, Integer> counterMap) {
		if (children.isEmpty()) {
			// then its leaf: Add all counted frequencies
			for (Map.Entry<String, Integer> entry : this.counterMap.entrySet()) {
				String key = entry.getKey();
				int newValue = entry.getValue();
				if (counterMap.containsKey(key)) {
					newValue += counterMap.get(key);
				}
				counterMap.put(key, newValue);
			}
		} else {
			for (Edge edge : children) {
				edge.getChild().fillSubtreeCounterMap(counterMap);
			}
		}
	}

	public Map<String, Integer> getCounterMap() {
		return counterMap;
	}

	public void setLeaf(String label) {
		this.label = label;
	}

	public void addChild(Tree child, SplitCondition condition) {
		this.children.add(new Edge(child, condition));
		Collections.sort(this.children);
	}

	public void removeChildren() {
		this.children.clear();
	}

	public boolean isLeaf() {
		return children.isEmpty();
	}

	public String getLabel() {
		return this.label;
	}

	/**
	 * Sets the benefit that lead to the creation of children at this node.
	 *
	 * @param benefit
	 *            the benefit
	 */
	public void setBenefit(double benefit) {
		this.benefit = benefit;
	}

	/**
	 * @return the benefit for creating children at this node or NaN if it is not a leaf or the benefit was not set
	 */
	public double getBenefit() {
		return benefit;
	}

	public Iterator<Edge> childIterator() {
		return children.iterator();
	}

	public int getNumberOfChildren() {
		return children.size();
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		toString(null, this, "", buffer);
		return buffer.toString();
	}

	private void toString(SplitCondition condition, Tree tree, String indent, StringBuilder buffer) {
		if (condition != null) {
			buffer.append(condition.toString());
		}
		if (!tree.isLeaf()) {
			Iterator<Edge> childIterator = tree.childIterator();
			while (childIterator.hasNext()) {
				buffer.append(Tools.getLineSeparator());
				buffer.append(indent);
				Edge edge = childIterator.next();
				toString(edge.getCondition(), edge.getChild(), indent + "|   ", buffer);
			}
		} else {
			buffer.append(": ");
			buffer.append(tree.getLabel());
			buffer.append(" " + tree.counterMap.toString());
		}
	}
}
