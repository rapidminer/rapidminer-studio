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
 * Implements a last recently used cache
 * 
 * @author Stefan Rueping
 */
public class Cache {

	/**
	 * Cache rows
	 */
	protected Object[] elements;

	/**
	 * time index for last access
	 */
	long counter;

	/**
	 * number of rows in cache
	 */
	int cache_size;

	/**
	 * the heap
	 */
	long[] last_used;

	int[] index;

	/**
	 * constructor
	 */
	public Cache() {
		cache_size = 0;
		elements = null;
		last_used = null;
		index = null;
		counter = 0;
	};

	/**
	 * constructor + init(size)
	 * 
	 * @param size
	 *            number of elements to be cached
	 */
	public Cache(int size, int dim) {
		cache_size = 0;
		elements = null;
		last_used = null;
		index = null;
		counter = 0;
		init(size);
	};

	/**
	 * initialises the cache
	 * 
	 * @param size
	 *            number of elements to be cached
	 */
	public void init(int size) {
		clean_cache();
		cache_size = size;
		// check if reserved memory big enough
		if (cache_size < 1) {
			cache_size = 1;
		}
		;
		elements = new Object[cache_size];
		last_used = new long[cache_size];
		index = new int[cache_size + 1];
		for (int i = 0; i < cache_size; i++) {
			elements[i] = null;
			last_used[i] = 0;
			index[i] = Integer.MAX_VALUE;
		}
		;
		index[cache_size] = Integer.MAX_VALUE;
	};

	public void shrink(int size, int dim) {
		// create cache with size elements where each element has size dim
		// keep old cache if it fits already (size constant)

		Object[] new_elements = new Object[size];
		long[] new_last_used = new long[size];
		int[] new_index = new int[size + 1];
		int i;
		double[] old_element;
		double[] element;
		if (size < cache_size) {
			// elements that can be copied from old cache
			cache_size = size;
		}
		;
		for (i = 0; i < cache_size; i++) {
			old_element = (double[]) (elements[i]);
			// copy old element j
			if ((old_element != null) && (last_used[i] > 0)) {
				element = new double[dim];
				System.arraycopy(old_element, 0, element, 0, dim);

				// for(j=0;j<dim;j++){
				// element[j] = old_element[j];
				// };
			} else {
				element = null;
			}
			;
			new_elements[i] = element;
			new_last_used[i] = last_used[i];
			new_index[i] = index[i];
			elements[i] = null;
		}
		;
		while (i < size) {
			new_elements[i] = null;
			new_last_used[i] = 0;
			new_index[i] = Integer.MAX_VALUE;
			i++;
		}
		;
		new_index[size] = Integer.MAX_VALUE;
		// overwrite old
		elements = new_elements;
		last_used = new_last_used;
		index = new_index;
		cache_size = size;
	};

	/**
	 * cleans the cache
	 */
	protected void clean_cache() {
		for (int i = 0; i < cache_size; i++) {
			elements[i] = null;
		}
		;
		elements = null;
		last_used = null;
		index = null;
	};

	/**
	 * get element from cache
	 */
	public Object get_element(int i) {
		int pos = 0;
		Object result = null;
		// binary search for i in [low,high]
		pos = lookup(i);
		if (pos == cache_size) {
			pos--;
		}
		;
		if ((index[pos] == i) && (last_used[pos] > 0)) {
			// cache hit
			result = elements[pos];
			counter++;
			last_used[pos] = counter;
		}
		;
		return result;
	};

	public int get_lru_pos() {
		long[] my_last_used = last_used;
		long min_time = my_last_used[cache_size - 1]; // heuristic: empty
														// entries are at the
														// end. Valid as any
														// element may be the
														// min element
		int low = cache_size - 1;
		int k;
		for (k = 0; k < cache_size; k++) {
			// search for last recently used element
			if (my_last_used[k] < min_time) {
				min_time = my_last_used[k];
				low = k;
			}
			;
		}
		;
		return low;
	};

	public Object get_lru_element() {
		long[] my_last_used = last_used;
		long min_time = my_last_used[cache_size - 1]; // heuristic: empty
														// entries are at the
														// end. Valid as any
														// element may be the
														// min element
		int low = cache_size - 1;
		int k;
		for (k = 0; k < cache_size; k++) {
			// search for last recently used element
			if (my_last_used[k] < min_time) {
				min_time = my_last_used[k];
				low = k;
			}
			;
		}
		;
		return elements[low];
	};

	/**
	 * put element in cache
	 */
	public void put_element(int i, Object o) {
		int low = 0;
		int high = cache_size;
		int pos = 0;
		int j;
		// binary search for i in [low,high]
		high = lookup(i);
		if (high == cache_size) {
			pos = high - 1;
		} else {
			pos = high;
		}
		;
		if ((index[pos] != i) || (last_used[pos] == 0)) {
			// find place to put o in => low
			if (index[pos] == i) {
				low = pos;
			} else {
				low = get_lru_pos();
			}
			;

			// delete low, place Object in high
			Object[] my_elements = elements;
			long[] my_last_used = last_used;
			int[] my_index = index;
			if (high <= low) {
				for (j = low; j > high; j--) {
					my_elements[j] = my_elements[j - 1];
					my_index[j] = my_index[j - 1];
					my_last_used[j] = my_last_used[j - 1];
				}
				;
			} else {
				for (j = low; j < high - 1; j++) {
					my_elements[j] = my_elements[j + 1];
					my_index[j] = my_index[j + 1];
					my_last_used[j] = my_last_used[j + 1];
				}
				;
				high--;
			}
			;
			pos = high;
			my_elements[high] = o;
			my_index[high] = i;
		}
		;
		counter++;
		last_used[pos] = counter;
	};

	protected int lookup(int i) {
		// find row i in cache
		// returns pos of element i if i in cache,
		// returns pos of smallest element larger than i otherwise
		int low;
		int high;
		int med;
		int[] my_index = index;

		low = 0;
		high = cache_size;
		// binary search
		while (low < high) {
			med = (low + high) >>> 1;	// Avoid integer overflow
			if (my_index[med] >= i) {
				high = med;
			} else {
				low = med + 1;
			}
			;
		}
		;
		return high;
	};

	/**
	 * is element at this position cached?
	 */
	public boolean cached(int i) {
		boolean ok;
		int pos = lookup(i);
		if (index[pos] == i) {
			if (last_used[pos] > 0) {
				ok = true;
			} else {
				ok = false;
			}
			;
		} else {
			ok = false;
		}
		;
		return (ok);
	};

	/**
	 * mark element as recently used
	 */
	public void renew(int i) {
		int pos = lookup(i);
		if (index[pos] == i) {
			if (last_used[pos] > 0) {
				counter++;
				last_used[pos] = counter;
			}
			;
		}
		;
	};

	/**
	 * swap elements in cache
	 */
	public void swap(int i, int j) {
		// overwrites entry i with entry j
		// WARNING: only to be used for shrinking!

		// i in cache?
		int pos_i = lookup(i);
		int pos_j = lookup(j);

		if ((index[pos_i] == i) && (index[pos_j] == j)) {
			// swap pos_i and pos_j
			Object dummy = elements[pos_i];
			elements[pos_i] = elements[pos_j];
			elements[pos_j] = dummy;
			last_used[pos_i] = last_used[pos_j];
			last_used[pos_j] = 0;
		} else {
			// mark rows as invalid
			if (index[pos_i] == i) {
				last_used[pos_i] = 0;
			} else if (index[pos_j] == j) {
				last_used[pos_j] = 0;
			}
			;
		}
		;

		// swap i and j in all rows
		double[] my_row;
		double dummy_d;
		for (pos_i = 0; pos_i < cache_size; pos_i++) {
			my_row = (double[]) (elements[pos_i]);
			if (my_row != null) {
				dummy_d = my_row[i];
				my_row[i] = my_row[j];
				my_row[j] = dummy_d;
			}
			;
		}
		;
	};

};
