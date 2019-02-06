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

import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;


/**
 * This class is an implementation of a Ball-Tree for organizing multidimensional datapoints in a
 * fashion supporting the search for nearest neighbours. This is only working well in low to middle
 * number of dimensions. Since the building of the tree is very expensiv, in most cases a linear
 * search strategy will outperform the ballTree in overall performance.
 * 
 * @param <T>
 *            This is the type of value with is stored with the points and retrieved on nearest
 *            neighbour search
 * 
 * @author Sebastian Land
 */
public class BallTree<T extends Serializable> implements GeometricDataCollection<T> {

	private static final long serialVersionUID = 2954882147712365506L;

	private BallTreeNode<T> root;
	private int k;
	private double dimensionFactor;
	private DistanceMeasure distance;
	private int size = 0;
	private ArrayList<T> values = new ArrayList<T>();

	public BallTree(DistanceMeasure distance) {
		this.distance = distance;
	}

	@Override
	public void add(double[] values, T storeValue) {
		this.size++;
		this.values.add(storeValue);
		if (root == null) {
			root = new BallTreeNode<T>(values, 0, storeValue);

			// setting dimension
			k = values.length;
			dimensionFactor = Math.sqrt(Math.PI) / Math.pow(gammaFunction(k / 2), 1d / k);
		} else {
			double totalAncestorIncrease = 0;
			double bestVolumeIncrease = Double.POSITIVE_INFINITY;
			BallTreeNode<T> bestNode = null;  // this node will be made child of new node
			int bestNodeIndex = 0;
			int bestSide = -1; // -1 left, 1 right
			BallTreeNode<T> currentNode = root;
			LinkedList<BallTreeNode<T>> ancestorList = new LinkedList<BallTreeNode<T>>();
			while (true) {
				// calculate ancestor increase if added to this current node
				double deltaAncestorIncrease = getVolumeIncludingPoint(currentNode, values) - getVolume(currentNode);
				totalAncestorIncrease += deltaAncestorIncrease;

				// calculate new Volume if added as left or right child of current
				double leftVolume = getNewVolume(currentNode, currentNode.getLeftChild(), values);
				double rightVolume = getNewVolume(currentNode, currentNode.getRightChild(), values);
				// check if adding as left node is best position till now
				double minVolume = Math.min(leftVolume, rightVolume);
				if (minVolume + totalAncestorIncrease < bestVolumeIncrease) {
					bestVolumeIncrease = minVolume + totalAncestorIncrease;
					bestNode = currentNode;
					bestSide = Double.compare(leftVolume, rightVolume);
					bestNodeIndex = ancestorList.size();
				}

				// adding next father
				ancestorList.add(currentNode);

				// check for termination
				if (currentNode.isLeaf()) {
					break;
				}

				// search for better child
				if (currentNode.hasTwoChilds()) {
					BallTreeNode<T> leftChild = currentNode.getLeftChild();
					double deltaVLeft = getVolumeIncludingPoint(leftChild, values) - getVolume(leftChild);
					BallTreeNode<T> rightChild = currentNode.getRightChild();
					double deltaVRight = getVolumeIncludingPoint(rightChild, values) - getVolume(rightChild);
					BallTreeNode<T> betterChild = (deltaVLeft < deltaVRight) ? leftChild : rightChild;
					currentNode = betterChild;
				} else {
					// or use single if only one present
					currentNode = currentNode.getChild();
				}
			}

			// now adding as specified child from bestFather
			BallTreeNode<T> newNode = new BallTreeNode<T>(values, 0, storeValue);
			if (bestSide < 0) {
				newNode.setChild(bestNode.getLeftChild());
				bestNode.setLeftChild(newNode);
			} else {
				newNode.setChild(bestNode.getRightChild());
				bestNode.setRightChild(newNode);
			}

			// setting radius of new node
			if (!newNode.isLeaf()) {
				newNode.setRadius(distance.calculateDistance(values, newNode.getChild().getCenter())
						+ newNode.getChild().getRadius());
			}

			// correcting radius of all ancestors
			ListIterator<BallTreeNode<T>> iterator = ancestorList.listIterator(bestNodeIndex + 1);
			while (iterator.hasPrevious()) {
				BallTreeNode<T> ancestor = iterator.previous();
				if (ancestor.hasTwoChilds()) {
					BallTreeNode<T> leftChild = ancestor.getLeftChild();
					BallTreeNode<T> rightChild = ancestor.getRightChild();
					ancestor.setRadius(Math.max(
							rightChild.getRadius()
									+ distance.calculateDistance(rightChild.getCenter(), ancestor.getCenter()),
							leftChild.getRadius() + distance.calculateDistance(leftChild.getCenter(), ancestor.getCenter())));
				} else {
					BallTreeNode<T> child = ancestor.getChild();
					ancestor.setRadius(distance.calculateDistance(ancestor.getCenter(), child.getCenter())
							+ child.getRadius());
				}
			}
		}
	}

	/**
	 * Returns the volume of the ball if the new node is added as child of father and new father of
	 * child with center as center. Child might be null, then the radius is 0
	 */
	private double getNewVolume(BallTreeNode<T> father, BallTreeNode<T> child, double[] center) {
		if (child == null) {
			return 0;
		}
		return Math.pow((distance.calculateDistance(center, child.getCenter()) + child.getRadius()) * dimensionFactor, k);
	}

	@Override
	public Collection<T> getNearestValues(int k, double[] values) {
		BoundedPriorityQueue<Tupel<Double, BallTreeNode<T>>> priorityQueue = getNearestNodes(k, values);
		LinkedList<T> neighboursList = new LinkedList<T>();
		for (Tupel<Double, BallTreeNode<T>> tupel : priorityQueue) {
			neighboursList.add((tupel.getSecond()).getStoreValue());
		}
		return neighboursList;
	}

	@Override
	public Collection<Tupel<Double, T>> getNearestValueDistances(int k, double[] values) {
		BoundedPriorityQueue<Tupel<Double, BallTreeNode<T>>> priorityQueue = getNearestNodes(k, values);
		LinkedList<Tupel<Double, T>> neighboursList = new LinkedList<Tupel<Double, T>>();
		for (Tupel<Double, BallTreeNode<T>> tupel : priorityQueue) {
			neighboursList.add(new Tupel<Double, T>(tupel.getFirst(), tupel.getSecond().getStoreValue()));
		}
		return neighboursList;

	}

	private BoundedPriorityQueue<Tupel<Double, BallTreeNode<T>>> getNearestNodes(int k, double[] values) {
		Stack<BallTreeNode<T>> nodeStack = new Stack<BallTreeNode<T>>();
		Stack<Integer> sideStack = new Stack<Integer>();
		// first doing initial search for nearest Node
		traverseTree(nodeStack, sideStack, root, values);
		// creating data structure for finding k nearest values
		BoundedPriorityQueue<Tupel<Double, BallTreeNode<T>>> priorityQueue = new BoundedPriorityQueue<Tupel<Double, BallTreeNode<T>>>(
				k);

		// now work on stack
		while (!nodeStack.isEmpty()) {
			// put top element into priorityQueue
			BallTreeNode<T> currentNode = nodeStack.pop();
			Integer currentSide = sideStack.pop();
			Tupel<Double, BallTreeNode<T>> currentTupel = new Tupel<Double, BallTreeNode<T>>(distance.calculateDistance(
					currentNode.getCenter(), values), currentNode);
			priorityQueue.add(currentTupel);
			// now check if far children has to be regarded
			if (currentNode.hasTwoChilds()) {
				BallTreeNode<T> otherChild = (currentSide < 0) ? currentNode.getRightChild() : currentNode.getLeftChild();
				if (!priorityQueue.isFilled()
						|| priorityQueue.peek().getFirst().doubleValue() + otherChild.getRadius() > distance
								.calculateDistance(values, otherChild.getCenter())) {
					// if needs to be checked, traverse tree to not visited leaf
					traverseTree(nodeStack, sideStack, otherChild, values);
				}
			}
			// go on, until stack is empty
		}
		return priorityQueue;
	}

	private void traverseTree(Stack<BallTreeNode<T>> stack, Stack<Integer> sideStack, BallTreeNode<T> root, double[] values) {
		BallTreeNode<T> currentNode = root;
		stack.push(currentNode);
		while (!currentNode.isLeaf()) {
			if (currentNode.hasTwoChilds()) {
				double distanceLeft = distance.calculateDistance(currentNode.getLeftChild().getCenter(), values);
				double distanceRight = distance.calculateDistance(currentNode.getRightChild().getCenter(), values);
				currentNode = (distanceLeft < distanceRight) ? currentNode.getLeftChild() : currentNode.getRightChild();
				sideStack.push(Double.compare(distanceLeft, distanceRight));
			} else {
				currentNode = currentNode.getChild();
				sideStack.push(0);
			}
			stack.push(currentNode);
		}
		sideStack.push(0);
	}

	private double getVolumeIncludingPoint(BallTreeNode<T> node, double[] point) {
		return Math.pow(Math.max(node.getRadius(), distance.calculateDistance(point, node.getCenter())) * dimensionFactor,
				k);
	}

	private double getVolume(BallTreeNode<T> node) {
		return Math.pow(node.getRadius() * dimensionFactor, k);
	}

	private double gammaFunction(int n) {
		double result = 1;
		for (int i = 2; i < n; i++) {
			result *= i;
		}
		return result;
	}

	public SimpleDataTable getVisualization() {
		SimpleDataTable table = new SimpleDataTable("BallTree", new String[] { "x", "y", "radius" });
		fillTable(table, root);
		return table;
	}

	private void fillTable(SimpleDataTable table, BallTreeNode<T> node) {
		table.add(new SimpleDataTableRow(new double[] { node.getCenter()[0], node.getCenter()[1], node.getRadius() }));
		if (node.hasLeftChild()) {
			fillTable(table, node.getLeftChild());
		}
		if (node.hasRightChild()) {
			fillTable(table, node.getRightChild());
		}
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
