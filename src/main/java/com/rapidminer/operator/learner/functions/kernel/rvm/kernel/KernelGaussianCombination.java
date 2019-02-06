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
 * Returns the value of the Gaussian combination kernel of both examples.
 * 
 * @author Ingo Mierswa
 */
public class KernelGaussianCombination extends Kernel {

	private static final long serialVersionUID = -8071778790596969872L;

	/** The parameter sigma1 of the Gaussian combination kernel. */
	private double sigma1 = 1.0d;

	/** The parameter sigma2 of the Gaussian combination kernel. */
	private double sigma2 = 0.0d;

	/** The parameter sigma3 of the Gaussian combination kernel. */
	private double sigma3 = 2.0d;

	/** Constructor(s) */
	public KernelGaussianCombination(double sigma1, double sigma2, double sigma3) {
		super();
		this.sigma1 = sigma1;
		this.sigma2 = sigma2;
		this.sigma3 = sigma3;
	}

	public KernelGaussianCombination() {
		super();
	}

	@Override
	public double eval(double[] x, double[] y) {
		double norm2 = norm2(x, y);
		double exp1 = sigma1 == 0.0d ? 0.0d : Math.exp((-1) * norm2 / sigma1);
		double exp2 = sigma2 == 0.0d ? 0.0d : Math.exp((-1) * norm2 / sigma2);
		double exp3 = sigma3 == 0.0d ? 0.0d : Math.exp((-1) * norm2 / sigma3);
		return exp1 + exp2 - exp3;
	}

	@Override
	public String toString() {
		return "gaussian combination kernel [sigma1 = " + sigma1 + ", sigma2 = " + sigma2 + ", sigma3 = " + sigma3 + "]";
	}
}
