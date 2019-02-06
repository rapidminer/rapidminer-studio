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
package com.rapidminer.operator.learner.functions.kernel.rvm;

import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.KernelBasisFunction;


/**
 * Holds the data defining the regression / classification problem to be learned. - All input
 * vectors are assumed to have the same dimension. - All target vectors are assumed to have the same
 * dimension
 * 
 * @author Piotr Kasprzak, Ingo Mierswa
 */
public abstract class Problem {

	private double[][] x;						// Input vectors

	private KernelBasisFunction[] kernels;		// Kernels to be used

	/** Problem types */

	/** Constructor */
	public Problem(double[][] x, KernelBasisFunction[] kernels) {
		this.x = x;
		this.kernels = kernels;
	}

	/** Getters */
	public int getProblemSize() {
		return x.length;
	}

	public int getInputDimension() {
		return x[0].length;
	}

	public double[][] getInputVectors() {
		return x;
	}

	public KernelBasisFunction[] getKernels() {
		return kernels;
	}

	abstract public int getTargetDimension();
}
