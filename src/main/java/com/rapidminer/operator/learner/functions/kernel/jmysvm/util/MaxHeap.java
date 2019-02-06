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
package com.rapidminer.operator.learner.functions.kernel.jmysvm.util;

/**
 * Implements a MaxHeap on n doubles and ints
 * 
 * @author Stefan Rueping
 */
public class MaxHeap extends Heap {

	public MaxHeap(int n) {
		the_size = 0;
		init(n);
	};

	@Override
	public final void add(double value, int index) {
		if (last < the_size) {
			heap[last] = value;
			indizes[last] = index;
			last++;
			if (last == the_size) {
				for (int j = last; j > 0; j--) {
					heapify(j - 1, last + 1 - j);
				}
				;
			}
			;
		} else if (value >= heap[0]) {
			heap[0] = value;
			indizes[0] = index;
			heapify(0, last);
		}
		;
	};

	@Override
	protected final void heapify(int start, int size) {
		double[] my_heap = heap;
		boolean running = true;
		int pos = 1;
		int left, right, largest;
		double dummyf;
		int dummyi;
		start--; // other variables counted from 1
		while (running) {
			left = 2 * pos;
			right = left + 1;
			if ((left <= size) && (my_heap[left + start] < my_heap[start + pos])) {
				largest = left;
			} else {
				largest = pos;
			}
			;
			if ((right <= size) && (my_heap[start + right] < my_heap[start + largest])) {
				largest = right;
			}
			;
			if (largest == pos) {
				running = false;
			} else {
				dummyf = my_heap[start + pos];
				dummyi = indizes[start + pos];
				my_heap[start + pos] = my_heap[start + largest];
				indizes[start + pos] = indizes[start + largest];
				my_heap[start + largest] = dummyf;
				indizes[start + largest] = dummyi;
				pos = largest;
			}
			;
		}
		;
	};
};
