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
 * Returns the value of the Multiquadric kernel of both examples.
 * 
 * @author Ingo Mierswa
 */
public class KernelMultiquadric extends Kernel {

	private static final long serialVersionUID = -5537408210606781153L;

	/** The parameter sigma of the Multiquadric kernel. */
	private double sigma = 1.0d;

	/** The parameter shift of the multiquadric kernel. */
	private double shift = 1.0d;

	/** Constructor(s) */
	public KernelMultiquadric(double sigma, double shift) {
		super();
		this.sigma = sigma;
		this.shift = shift;
	}

	public KernelMultiquadric() {
		super();
	}

	@Override
	public double eval(double[] x, double[] y) {
		return Math.sqrt((norm2(x, y) / sigma) + (shift * shift));
	}

	@Override
	public String toString() {
		return "multiquadric kernel [sigma = " + sigma + ", shift = " + shift + "]";
	}
}
