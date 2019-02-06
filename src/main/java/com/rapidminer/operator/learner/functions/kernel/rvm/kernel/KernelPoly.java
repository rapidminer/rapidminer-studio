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
 * Laplace kernel: K(x, y) = (lengthScale^{-2} * (x * y) + bias)^order
 * 
 * @author Piotr Kasprzak
 */
public class KernelPoly extends KernelRadial {

	private static final long serialVersionUID = -118526840262643388L;

	/** Polynomial order */
	protected double degree = 2;

	/** Bias */
	protected double bias = 0;

	/** Constructor(s) */

	public KernelPoly() {}

	public KernelPoly(double lengthScale, double bias, double degree) {
		super(lengthScale);
		this.bias = bias;
		this.degree = degree;
	}

	/** evaluate kernel */
	@Override
	public double eval(double[] x, double[] y) {

		double result = 0;

		for (int i = 0; i < x.length; i++) {
			result += x[i] * y[i];
		}

		result = Math.pow(Math.pow(lengthScale, -2) * result + bias, degree);

		return result;
	}

	@Override
	public String toString() {
		return "poly kernel [lengthScale = " + lengthScale + ", bias = " + bias + ", order = " + degree + "]";
	}
}
