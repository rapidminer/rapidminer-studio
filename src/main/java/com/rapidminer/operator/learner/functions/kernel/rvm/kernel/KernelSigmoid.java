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
 * Returns the value of the Sigmoid kernel of both examples.
 * 
 * @author Ingo Mierswa
 */
public class KernelSigmoid extends Kernel {

	private static final long serialVersionUID = 5056175330389455467L;

	/** The parameter a of the sigmoid kernel. */
	private double a = 1.0d;

	/** The parameter b of the sigmoid kernel. */
	private double b = 0.0d;

	/** Constructor(s) */
	public KernelSigmoid(double a, double b) {
		super();
		this.a = a;
		this.b = b;
	}

	public KernelSigmoid() {
		super();
	}

	@Override
	public double eval(double[] x, double[] y) {
		// K = tanh(a(x*y)+b)
		double prod = 0;
		for (int i = 0; i < x.length; i++) {
			prod += x[i] * y[i];
		}
		prod = a * prod + b;
		double e1 = Math.exp(prod);
		double e2 = Math.exp(-prod);
		return ((e1 - e2) / (e1 + e2));
	}

	@Override
	public String toString() {
		return "sigmoid kernel [a = " + a + ", b = " + b + "]";
	}
}
