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

/**
 * Laplace kernel: K(x, y) = exp(-sqrt(lengthScale^{-2} * ||x - y||^2))
 * 
 * @author Piotr Kasprzak, Ingo Mierswa
 */
public class KernelLaplace extends KernelRadial {

	private static final long serialVersionUID = -6119888769441823765L;

	/** Constructor(s) */

	public KernelLaplace() {}

	public KernelLaplace(double lengthScale) {
		super(lengthScale);
	}

	/** evaluate kernel */
	@Override
	public double eval(double[] x, double[] y) {

		double result = Math.exp(-Math.sqrt(Math.pow(lengthScale, -2) * norm2(x, y)));

		return result;
	}

	@Override
	public String toString() {
		return "laplace kernel [lengthScale = " + lengthScale + "]";
	}
}
