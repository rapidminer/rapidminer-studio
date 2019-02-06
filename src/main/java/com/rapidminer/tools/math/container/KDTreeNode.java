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

import java.io.Serializable;


/**
 * The node for a KD tree.
 * 
 * @author Sebastian Land
 * 
 * @param <T>
 *            This is the type of value with is stored with the points and retrieved on nearest
 *            neighbour search
 */
public class KDTreeNode<T> implements Serializable {

	private static final long serialVersionUID = -4204535347268139613L;

	private T storeValue;
	private double[] values;
	private KDTreeNode<T> lesserChild;
	private KDTreeNode<T> greaterChild;
	private int comparationDimension;

	public KDTreeNode(double[] values, T storeValue, int comparationDimension) {
		this.values = values;
		this.storeValue = storeValue;
		this.comparationDimension = comparationDimension;
	}

	public KDTreeNode<T> getNearChild(double[] compare) {
		if (compare[comparationDimension] < values[comparationDimension]) {
			return lesserChild;
		} else {
			return greaterChild;
		}
	}

	public KDTreeNode<T> getFarChild(double[] compare) {
		if (compare[comparationDimension] >= values[comparationDimension]) {
			return lesserChild;
		} else {
			return greaterChild;
		}
	}

	public boolean hasNearChild(double[] compare) {
		if (compare[comparationDimension] < values[comparationDimension]) {
			return lesserChild != null;
		} else {
			return greaterChild != null;
		}
	}

	public boolean hasFarChild(double[] compare) {
		if (compare[comparationDimension] >= values[comparationDimension]) {
			return lesserChild != null;
		} else {
			return greaterChild != null;
		}
	}

	public void setChild(KDTreeNode<T> node) {
		if (node.getValues()[comparationDimension] < values[comparationDimension]) {
			lesserChild = node;
		} else {
			greaterChild = node;
		}
	}

	public T getStoreValue() {
		return storeValue;
	}

	public KDTreeNode<T> getLesserChild() {
		return lesserChild;
	}

	public void setLesserChild(KDTreeNode<T> leftChild) {
		this.lesserChild = leftChild;
	}

	public KDTreeNode<T> getGreaterChild() {
		return greaterChild;
	}

	public void setGreaterChild(KDTreeNode<T> rightChild) {
		this.greaterChild = rightChild;
	}

	public double[] getValues() {
		return values;
	}

	public double getCompareValue() {
		return values[comparationDimension];
	}

	public int getCompareDimension() {
		return comparationDimension;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < comparationDimension; i++) {
			buffer.append(values[i] + "  ");
		}
		buffer.append("[");
		buffer.append(values[comparationDimension]);
		buffer.append("]  ");
		for (int i = comparationDimension + 1; i < values.length; i++) {
			buffer.append(values[i] + "  ");
		}
		return buffer.toString();
	}

}
