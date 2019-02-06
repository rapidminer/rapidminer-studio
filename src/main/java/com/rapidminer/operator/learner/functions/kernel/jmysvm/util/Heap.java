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
 * Implements a Heap on n doubles and ints
 * 
 * @author Stefan Rueping
 */
public abstract class Heap {

	protected int the_size;

	protected int last;

	protected double[] heap;

	protected int[] indizes;

	public Heap() {};

	public Heap(int n) {
		the_size = 0;
		init(n);
	};

	public int size() {
		return last; // last = number of elements
	};

	public void init(int n) {
		if (the_size != n) {
			the_size = n;
			heap = new double[n];
			indizes = new int[n];
		}
		;
		last = 0;
	};

	public void clear() {
		the_size = 0;
		last = 0;
		heap = null;
		indizes = null;
	};

	public int[] get_values() {
		return indizes;
	};

	public abstract void add(double value, int index);

	public double top_value() {
		return heap[0];
	};

	public boolean empty() {
		return (last == 0);
	};

	protected abstract void heapify(int start, int size);

};
