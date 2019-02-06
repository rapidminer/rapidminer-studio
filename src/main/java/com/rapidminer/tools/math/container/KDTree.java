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
package com.rapidminer.tools.math.container;

import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;


/**
 * This class is an implementation of a KD-Tree for organizing multidimensional datapoints in a
 * fashion supporting the search for nearest neighbours. This is only working well in low
 * dimensions.
 * 
 * @author Sebastian Land
 * 
 * @param <T>
 *            This is the type of value with is stored with the points and retrieved on nearest
 *            neighbour search
 */
public class KDTree<T extends Serializable> implements GeometricDataCollection<T> {

	private static final long serialVersionUID = -8531805333989991725L;
	private KDTreeNode<T> root;
	private int k; // the number of dimensions
	private DistanceMeasure distance;
	private int size = 0;
	private ArrayList<T> values = new ArrayList<T>();

	public KDTree(int numberOfDimensions, DistanceMeasure distance) {
		this.k = numberOfDimensions;
		this.distance = distance;
	}

	@Override
	public void add(double[] values, T storeValue) {
		this.size++;
		this.values.add(storeValue);
		if (root == null) {
			this.root = new KDTreeNode<T>(values, storeValue, 0);
		} else {
			int currentDimension = 0;
			int depth = 0;
			KDTreeNode<T> currentNode = root;
			KDTreeNode<T> childNode = null;
			// running through tree until empty leaf found: Add new node with given values
			while (true) {
				childNode = currentNode.getNearChild(values);
				if (childNode == null) {
					break;
				} else {
					currentNode = childNode;
					depth++;
					currentDimension = depth % k;
				}
			}
			currentNode.setChild(new KDTreeNode<T>(values, storeValue, currentDimension));
		}
	}

	@Override
	public Collection<T> getNearestValues(int k, double[] values) {
		BoundedPriorityQueue<Tupel<Double, KDTreeNode<T>>> priorityQueue = getNearestNodes(k, values);
		LinkedList<T> neighboursList = new LinkedList<T>();
		for (Tupel<Double, KDTreeNode<T>> tupel : priorityQueue) {
			neighboursList.add(tupel.getSecond().getStoreValue());
		}
		return neighboursList;
	}

	@Override
	public Collection<Tupel<Double, T>> getNearestValueDistances(int k, double[] values) {
		BoundedPriorityQueue<Tupel<Double, KDTreeNode<T>>> priorityQueue = getNearestNodes(k, values);
		LinkedList<Tupel<Double, T>> neighboursList = new LinkedList<Tupel<Double, T>>();
		for (Tupel<Double, KDTreeNode<T>> tupel : priorityQueue) {
			neighboursList.add(new Tupel<Double, T>(tupel.getFirst(), tupel.getSecond().getStoreValue()));
		}
		return neighboursList;
	}

	private BoundedPriorityQueue<Tupel<Double, KDTreeNode<T>>> getNearestNodes(int k, double[] values) {
		Stack<KDTreeNode<T>> nodeStack = new Stack<KDTreeNode<T>>();
		// first doing initial search for nearest Node
		nodeStack = traverseTree(nodeStack, root, values);

		// creating data structure for finding k nearest values
		BoundedPriorityQueue<Tupel<Double, KDTreeNode<T>>> priorityQueue = new BoundedPriorityQueue<Tupel<Double, KDTreeNode<T>>>(
				k);

		// now work on stack
		while (!nodeStack.isEmpty()) {
			// put top element into priorityQueue
			KDTreeNode<T> currentNode = nodeStack.pop();
			Tupel<Double, KDTreeNode<T>> currentTupel = new Tupel<Double, KDTreeNode<T>>(distance.calculateDistance(
					currentNode.getValues(), values), currentNode);
			priorityQueue.add(currentTupel);
			// now check if far children has to be regarded
			if (!priorityQueue.isFilled()
					|| priorityQueue.peek().getFirst().doubleValue() > currentNode.getCompareValue()
							- values[currentNode.getCompareDimension()]) {
				// if needs to be checked, traverse tree to nearest leaf
				if (currentNode.hasFarChild(values)) {
					traverseTree(nodeStack, currentNode.getFarChild(values), values);
				}
			}

			// go on, until stack is empty
		}
		return priorityQueue;
	}

	private Stack<KDTreeNode<T>> traverseTree(Stack<KDTreeNode<T>> stack, KDTreeNode<T> root, double[] values) {
		KDTreeNode<T> currentNode = root;
		stack.push(currentNode);
		while (currentNode.hasNearChild(values)) {
			currentNode = currentNode.getNearChild(values);
			stack.push(currentNode);
		}
		return stack;
	}

	@Override
	public Collection<Tupel<Double, T>> getNearestValueDistances(double withinDistance, double[] values) {
		throw new RuntimeException("Not supported method");
	}

	@Override
	public Collection<Tupel<Double, T>> getNearestValueDistances(double withinDistance, int butAtLeastK, double[] values) {
		throw new RuntimeException("Not supported method");
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public T get(int index) {
		return values.get(index);
	}

	@Override
	public Iterator<T> iterator() {
		return values.iterator();
	}
}
