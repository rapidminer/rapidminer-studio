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
package com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel;

import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExample;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.util.Cache;

import java.io.Serializable;


/**
 * Abstract base class for all kernels.
 * 
 * @author Stefan Rueping, Ingo Mierswa
 */
public abstract class Kernel implements Serializable {

	private static final long serialVersionUID = 6086202515099260920L;

	/**
	 * Container for the examples, parameters etc.
	 */
	protected SVMExamples the_examples;

	/**
	 * dimension of the examples
	 */
	protected int dim;

	/**
	 * Kernel cache
	 */
	protected transient Cache kernel_cache;

	/**
	 * Number of elements in cache
	 */
	protected int kernel_cache_size;

	/**
	 * Size of cache in MB
	 */
	protected int cache_MB;

	/**
	 * number of examples after shrinking
	 */
	protected int examples_total;

	/**
	 * Class constructor
	 */
	public Kernel() {};

	/**
	 * Output as String
	 */
	@Override
	public String toString() {
		return ("abstract kernel class");
	};

	/**
	 * Init the kernel
	 * 
	 * @param examples
	 *            Container for the examples.
	 */
	public void init(SVMExamples examples, int cacheSizeMB) {
		the_examples = examples;
		examples_total = the_examples.count_examples();
		dim = the_examples.get_dim();
		init_kernel_cache(cacheSizeMB);
	};

	/**
	 * Calculates kernel value of vectors x and y
	 */
	public abstract double calculate_K(int[] x_index, double[] x_att, int[] y_index, double[] y_att);

	public abstract String getDistanceFormula(double[] x, String[] attributeNames);

	/**
	 * calculate inner product
	 */
	public double innerproduct(int[] x_index, double[] x_att, int[] y_index, double[] y_att) {
		double result = 0;
		int xpos = x_index.length - 1;
		int ypos = y_index.length - 1;

		while ((xpos >= 0) && (ypos >= 0)) {
			if (x_index[xpos] == y_index[ypos]) {
				result += x_att[xpos] * y_att[ypos];
				xpos--;
				ypos--;
			} else if (x_index[xpos] > y_index[ypos]) {
				xpos--;
			} else {
				ypos--;
			}
			;
		}
		;

		return result;
	};

	/**
	 * calculate ||x-y||^2
	 */
	public double norm2(int[] x_index, double[] x_att, int[] y_index, double[] y_att) {
		double result = 0;
		double tmp;
		int xpos = x_index.length - 1;
		int ypos = y_index.length - 1;

		while ((xpos >= 0) && (ypos >= 0)) {
			if (x_index[xpos] == y_index[ypos]) {
				tmp = x_att[xpos] - y_att[ypos];
				result += tmp * tmp;
				xpos--;
				ypos--;
			} else if (x_index[xpos] > y_index[ypos]) {
				tmp = x_att[xpos];
				result += tmp * tmp;
				xpos--;
			} else {
				tmp = y_att[ypos];
				result += tmp * tmp;
				ypos--;
			}
			;
		}
		;
		while (xpos >= 0) {
			tmp = x_att[xpos];
			result += tmp * tmp;
			xpos--;
		}
		;
		while (ypos >= 0) {
			tmp = y_att[ypos];
			result += tmp * tmp;
			ypos--;
		}
		;

		return result;
	};

	/**
	 * Gets a kernel row
	 */
	public double[] get_row(int i) {
		double[] result = null;
		result = ((double[]) kernel_cache.get_element(i));
		if (result == null) {
			// get last cache element, don't assign new memory
			result = (double[]) kernel_cache.get_lru_element();
			if (result == null) {
				result = new double[examples_total];
			}
			;
			calculate_K_row(result, i);
			kernel_cache.put_element(i, result);
		}
		;
		return result;
	};

	/**
	 * Inits the kernel cache.
	 * 
	 * @param size
	 *            of the cache in MB
	 */
	public void init_kernel_cache(int size) {
		cache_MB = size;
		// array of train_size doubles
		kernel_cache_size = size * 1048576 / 4 / examples_total;
		if (kernel_cache_size < 1) {
			kernel_cache_size = 1;
		}
		;
		if (kernel_cache_size > the_examples.count_examples()) {
			kernel_cache_size = the_examples.count_examples();
		}
		;
		kernel_cache = new Cache(kernel_cache_size, examples_total);
	};

	public int getCacheSize() {
		return cache_MB;
	}

	/**
	 * Sets the number of examples to new value
	 */
	public void set_examples_size(int new_examples_total) {
		// number of rows that fit into cache:
		int new_kernel_cache_size = cache_MB * 1048576 / 4 / new_examples_total;
		if (new_kernel_cache_size < 1) {
			new_kernel_cache_size = 1;
		}
		;
		if (new_kernel_cache_size > new_examples_total) {
			new_kernel_cache_size = new_examples_total;
		}
		;

		// kernel_cache = new Cache(kernel_cache_size);

		if (new_examples_total < examples_total) {
			// keep cache
			kernel_cache.shrink(new_kernel_cache_size, new_examples_total);
		} else if (new_examples_total > examples_total) {
			kernel_cache.init(new_kernel_cache_size);
		}
		;
		kernel_cache_size = new_kernel_cache_size;
		examples_total = new_examples_total;
	};

	/**
	 * Calculate K(i,j)
	 */
	public double calculate_K(int i, int j) {
		int[] x_index;
		double[] x_att;
		int[] y_index;
		double[] y_att;
		x_index = the_examples.index[i];
		x_att = the_examples.atts[i];
		y_index = the_examples.index[j];
		y_att = the_examples.atts[j];

		return calculate_K(x_index, x_att, y_index, y_att);
	};

	public double calculate_K(SVMExample x, SVMExample y) {
		return calculate_K(x.index, x.att, y.index, y.att);
	};

	public double[] calculate_K_row(double[] result, int i) {
		int[] x_index;
		double[] x_att;
		int[] y_index;
		double[] y_att;
		x_index = the_examples.index[i];
		x_att = the_examples.atts[i];

		for (int k = 0; k < examples_total; k++) {
			y_index = the_examples.index[k];
			y_att = the_examples.atts[k];
			result[k] = calculate_K(x_index, x_att, y_index, y_att);
		}
		;
		return result;
	};

	/**
	 * swap two training examples
	 * 
	 * @param pos1
	 * @param pos2
	 */
	public void swap(int pos1, int pos2) {
		// called after container swap
		kernel_cache.swap(pos1, pos2);
	}
}
