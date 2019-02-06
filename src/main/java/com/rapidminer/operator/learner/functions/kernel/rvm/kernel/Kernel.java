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
package com.rapidminer.operator.learner.functions.kernel.rvm.kernel;

import java.io.Serializable;


/**
 * Abstract base class for all RVM / GP kernels. Please note that all kernel functions must have a
 * zero argument constructor.
 * 
 * @author Piotr Kasprzak, Ingo Mierswa
 * 
 */
public abstract class Kernel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8434771989480100605L;

	/** Constructor(s) */
	public Kernel() {}

	/** Evaluate kernel */
	public abstract double eval(double[] x, double[] y);

	/** Calculates l2-norm(x, y)^2 = ||x - y||^2 */
	public double norm2(double[] x, double[] y) {
		double result = 0, diff;
		for (int i = 0; i < x.length; i++) {
			diff = x[i] - y[i];
			result += diff * diff;
		}
		return result;
	}
}
