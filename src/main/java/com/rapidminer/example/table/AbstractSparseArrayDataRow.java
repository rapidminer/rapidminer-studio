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
package com.rapidminer.example.table;

import com.rapidminer.example.Tools;


/**
 * Implementation of DataRow that is backed by primitive arrays. Should always be used if more than
 * 50% of the data is sparse. As fast (or even faster than map implementation) but needs
 * considerably less memory.
 * 
 * @author Niraj Aswani, Julien Nioche, Ingo Mierswa, Shevek
 */
public abstract class AbstractSparseArrayDataRow extends DataRow implements SparseDataRow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4946925205115859758L;

	/** Stores the used attribute indices. */
	private int[] x;

	/** Number of inserted elements. */
	private int counter = 0;

	/** Creates an empty sparse array data row with size 0. */
	public AbstractSparseArrayDataRow() {
		this(0);
	}

	/*
	 * Note: DataRowFactory calls this with attributes.size, which is probably wrong if the data row
	 * is meant to be sparse - we never intend to have that many attributes.
	 */

	/** Creates a sparse array data row of the given size. */
	public AbstractSparseArrayDataRow(int size) {
		x = new int[size];
		for (int i = 0; i < x.length; i++) {
			x[i] = Integer.MAX_VALUE;
		}
	}

	// ======================
	// abstract methods
	// ======================

	/*
	 * Implementations of this are not as optimal as the removal of a value from the indexes array,
	 * since the subclass does not have access to the count variable. This could be fixed, but ...
	 * *shrug*. Shevek.
	 */
	protected abstract void removeValue(int index);

	protected abstract void resizeValues(int length);

	protected abstract void setValue(int index, double value);

	protected abstract double getValue(int index);

	/* This could be implemented using get() and set() */
	protected abstract void swapValues(int a, int b);

	/* This could be implemented using get() */
	protected abstract double[] getAllValues();

	/** Sorts the arrays in the given range. */
	private void sort(int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i = off; i < len + off; i++) {
				for (int j = i; j > off && x[j - 1] > x[j]; j--) {
					swap(j, j - 1);
				}
			}
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1); // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) { // Big arrays, pseudomedian of 9
				int s = len / 8;
				l = med3(l, l + s, l + 2 * s);
				m = med3(m - s, m, m + s);
				n = med3(n - 2 * s, n - s, n);
			}
			m = med3(l, m, n); // Mid-size, med of 3
		}
		long v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while (true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v) {
					swap(a++, b);
				}
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v) {
					swap(c, d--);
				}
				c--;
			}
			if (b > c) {
				break;
			}
			swap(b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a - off, b - a);
		vecswap(off, b - s, s);
		s = Math.min(d - c, n - d - 1);
		vecswap(b, n - s, s);

		// Recursively sort non-partition-elements
		if ((s = b - a) > 1) {
			sort(off, s);
		}
		if ((s = d - c) > 1) {
			sort(n - s, s);
		}
	}

	/** Swaps the next n elements from a and b. */
	private void vecswap(int a, int b, int n) {
		for (int i = 0; i < n; i++, a++, b++) {
			swap(a, b);
		}
	}

	private int med3(int a, int b, int c) {
		return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a) : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	protected void swap(int a, int b) {
		int t = x[a];
		x[a] = x[b];
		x[b] = t;
		swapValues(a, b);
	}

	/** Returns the desired data for the given attribute. */
	@Override
	protected double get(int val, double defaultValue) {
		int index = java.util.Arrays.binarySearch(x, val);
		if (index < 0) {
			return defaultValue;
		} else {
			return getValue(index);
		}
	}

	/** Sets the given data for the given attribute. */
	@Override
	protected synchronized void set(int index, double value, double defaultValue) {
		// first search if it is already available
		// we need to replace the value
		// return a negative int if the value is not in the array
		// the list is ALWAYS sorted

		int index1 = java.util.Arrays.binarySearch(x, index);

		if (Tools.isDefault(defaultValue, value)) {
			if (index1 >= 0) { // (old value != deflt) AND new is default -->
								// remove entry from arrays
				System.arraycopy(x, index1 + 1, x, index1, (counter - (index1 + 1)));
				x[counter - 1] = Integer.MAX_VALUE;
				removeValue(index1);
				counter--;
			}
		} else {
			if (index1 < 0) { // a new entry
				if (counter >= x.length) { // need more space
					int newlength = x.length + (x.length >> 1) + 1;
					int[] y = new int[newlength];
					System.arraycopy(x, 0, y, 0, x.length);
					for (int i = x.length; i < y.length; i++) {
						y[i] = Integer.MAX_VALUE;
					}
					x = y;
					resizeValues(newlength);
				}

				// adds the new value at the end of the array
				x[counter] = index;
				setValue(counter, value);

				// compare to the one before him
				if ((counter > 0) && (index < x[counter - 1])) {
					sort(0, x.length);
				}
				counter++;
			} else { // replace existing value
				setValue(index1, value);
			}
		}
	}

	@Override
	public int[] getNonDefaultIndices() {
		trim();
		return this.x;
	}

	@Override
	public double[] getNonDefaultValues() {
		trim();
		return getAllValues();
	}

	/** Does nothing. */
	@Override
	public void ensureNumberOfColumns(int numberOfColumns) {}

	/** Trims the data row to the number of actually used elements. */
	@Override
	public synchronized void trim() {
		if (counter < x.length) {
			int[] y = new int[counter];
			System.arraycopy(x, 0, y, 0, counter);
			x = y;
			resizeValues(counter);
		}
	}

	/** Returns a string representation of the data row. */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < x.length; i++) {
			if (i != 0) {
				result.append(", ");
			}
			result.append(x[i] + ":" + getValue(i));
		}
		result.append(", counter: " + counter);
		return result.toString();
	}
}
