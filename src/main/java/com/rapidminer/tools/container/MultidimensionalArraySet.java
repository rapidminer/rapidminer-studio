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
package com.rapidminer.tools.container;

/**
 * This class represents a multidimensional array whose dimensions have to be specified during
 * construction. The multidimensional array is modeled by an underlying single dimension array.
 * 
 * @author Tobias Malbrecht
 * 
 * @param <E>
 *            the class of the array elements
 */
public class MultidimensionalArraySet<E> {

	/** The underlying single dimension array. */
	private E[] array;

	// /**
	// * The dimensions of the multidimensional array.
	// */
	// private int[] dimensions;

	/**
	 * The number of combinations that is possible with specifying the last following attributes.
	 * These are used for computing the single array index from multidimensional indices.
	 */
	private int[] combinations;

	/**
	 * The constructor.
	 * 
	 * @param dimensions
	 *            int array which contains the size of each dimension
	 */
	@SuppressWarnings("unchecked")
	public MultidimensionalArraySet(int[] dimensions) {
		// this.dimensions = dimensions;
		int splits = dimensions.length;

		int numberOfCombinations = 1;
		combinations = new int[splits];
		for (int i = splits - 1; i > 0; i--) {
			numberOfCombinations *= dimensions[i];
			combinations[i - 1] = numberOfCombinations;
		}
		numberOfCombinations *= dimensions[0];
		combinations[splits - 1] = 1;
		array = (E[]) new Object[numberOfCombinations];
	}

	/**
	 * Returns the array element at the specified single dimension array position.
	 * 
	 * @param index
	 *            the index
	 * @return an element
	 */
	public E get(int index) {
		return array[index];
	}

	/**
	 * Returns the array element at the position specified by the given indices.
	 * 
	 * @param indices
	 *            the indices
	 * @return an element
	 */
	public E get(int[] indices) {
		return array[getIndex(indices)];
	}

	/**
	 * Sets the array element at the specified single dimension array position.
	 * 
	 * @param index
	 *            the index
	 * @param e
	 *            an element
	 */
	public void set(int index, E e) {
		array[index] = e;
	}

	/**
	 * Sets the array element at the position specified by the given indices
	 * 
	 * @param indices
	 *            the indices
	 * @param e
	 *            an element
	 */
	public void set(int[] indices, E e) {
		array[getIndex(indices)] = e;
	}

	/**
	 * Computes the single dimension array index from the given multidimensional indices.
	 * 
	 * @param indices
	 *            the indices
	 * @return the corresponding index
	 */
	private int getIndex(int[] indices) {
		return sumProduct(indices, combinations);
	}

	/**
	 * Computes the multidimensional indices corresponding to a single dimension array index.
	 * 
	 * @param index
	 *            the index
	 * @return the corresponding indices
	 */
	public int[] getIndices(int index) {
		int[] indices = new int[combinations.length];
		int r = index;
		for (int i = 0; i < indices.length; i++) {
			indices[i] = r / combinations[i];
			r = r % combinations[i];
		}
		return indices;
	}

	/**
	 * Returns the number of elements this array can hold.
	 * 
	 * @return size
	 */
	public int size() {
		return array.length;
	}

	// /**
	// * Returns an int array holding the dimensions of the
	// * multidimensional array.
	// *
	// * @return dimensions
	// */
	// public int[] getDimensions() {
	// return dimensions;
	// }

	/**
	 * Calculates the sum product of two int arrays. Used for the calculation of a single dimension
	 * array index from multidimensional indices.
	 */
	private int sumProduct(int[] firstIndices, int[] secondIndices) {
		int firstLength = firstIndices.length;
		int secondLength = secondIndices.length;
		int length = secondLength > firstLength ? firstLength : secondLength;
		int product = 0;
		for (int i = 0; i < length; i++) {
			product += firstIndices[i] * secondIndices[i];
		}
		return product;
	}
}
