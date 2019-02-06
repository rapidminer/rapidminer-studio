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
package com.rapidminer.operator.learner.functions.kernel.gaussianprocess;

import Jama.Matrix;
import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.Kernel;

import java.io.Serializable;


/**
 * The learned model.
 * 
 * @author Piotr Kasprzak, Ingo Mierswa
 * 
 */
public class Model implements Serializable {

	private static final long serialVersionUID = 1695426983570182267L;

	/* Kernel used */
	private Kernel kernel;

	/* The basis vectors used */
	private double[][] basisVectors;

	/* Parameterisation of the approximate GP */

	// private Matrix C; // GP-covariance parameterisation

	private Matrix alpha; // GP-mean parameterisation

	// private Matrix Q; // Inverse kernel gram matrix, could be useful later

	private boolean regression; // Regression or classification model

	/**
	 * Other variables (can be derived from the variables above)
	 */

	/*
	 * The vector k_t+1 = [K(x_1, x_t+1), K(x_2, x_t+1), ..., K(x_t, x_t+1)], holding the results of
	 * using the kernel on the the new input x_t+1 and the rest of the old inputs
	 */

	private Matrix k;

	/* Temorary result: C_t * k_t+1 */

	// IM: WARNING: the variable was never used !!!
	// private Matrix C_times_k;
	private int d; // Number of basis vectors (= dimension of all vectors / matrices)

	private int inputDim; // Dimension of the input vectors

	/** Constructors */

	public Model(Kernel kernel, double[][] basisVectors, Matrix alpha, Matrix C, Matrix Q, int d, boolean regression) {

		this.kernel = kernel;
		this.basisVectors = basisVectors;

		this.alpha = alpha;
		// this.C = C;
		// this.Q = Q;

		this.regression = regression;

		this.d = d;
		inputDim = basisVectors[0].length; // Dimension of the input vectors

		k = new Matrix(d, 1);

		/* Temorary result: C_t * k_t+1 */

		// IM: WARNING: the variable was never used !!!
		// C_times_k = new Matrix(d, 1);
	}

	public int getNumberOfBasisVectors() {
		return basisVectors.length;
	}

	public int getInputDim() {
		return inputDim;
	}

	public double[] getBasisVector(int i) {
		return this.basisVectors[i];
	}

	public double getBasisVectorValue(int i, int j) {
		return this.basisVectors[i][j];
	}

	/**
	 * Compute the (canonical) scalar product between x and y, using only the first d components of
	 * the vectors
	 */

	private double scalarProduct(double[][] x, double[][] y, int d) {
		/* Lengths of x and y must be both >= d */
		// if (x.length < d || y.length < d)
		// throw new Exception("At least one vector has a too small dimension!");

		double result = 0;

		for (int i = 0; i < d; i++) {
			result += x[i][0] * y[i][0];
		}

		return result;
	}

	/**
	 * Apply the model to a (new) input vector x_t+1 in order to get a prediction, which - as a
	 * GP-marignal at x_t+1 - is a one-dimensional gaussian distribution with mean m and covariance
	 * sigma^2 (2.22, the parameterisation lemma). Returns only the function value, the mapping to a
	 * classification must be done by the invoking method.
	 */

	public double applyToVector(double[] x_new) {

		double m = 0; // The mean of the marginalisation, we want to compute
		// IM: Warning: the following variable was never used !!!
		// double sigma_2 = 0; // The according covariance

		double prediction = 0; // The prediction we return (will be =mean)

		// IM: WARNING: the variable k_star was never used !!!
		// double k_star = 0; // = Kernel(x_new, x_new)

		/**
		 * Compute k_t+1 = [K(x_1, x_t+1), K(x_2, x_t+1), ..., K(x_t, x_t+1)] and
		 * 
		 * k_star = K(x_t+1, x_t+1)
		 */

		for (int j = 0; j < d; j++) {
			k.getArray()[j][0] = kernel.eval(basisVectors[j], x_new);
		}

		// IM: WARNING: the variable k_star was never used !!!
		// k_star = kernel.eval(x_new, x_new);

		/**
		 * Compute m_t+1, the mean of the marginal of the GP at x_t+1, which is gaussian:
		 * 
		 * m_t+1 = {k_t+1}^T * alpha_t
		 * 
		 */

		m = scalarProduct(k.getArray(), alpha.getArray(), d);

		/**
		 * Compute sigma_t+1^2, the covariance^2 of the marginal of the GP at x_t+1, which is
		 * gaussian:
		 * 
		 * {sigma_t+1}^2 = K(x_t+1, x_t+1) + {k_t+1}^T * C_t * k_t+1
		 * 
		 * (see end of chapter 2.4 and chapter 5.1)
		 */

		// IM: WARNING: the variable was never used !!!
		// C_times_k = C.times(k);
		// IM: WARNING: the following variable was never used !!!
		// sigma_2 = k_star + scalarProduct(k.getArray(), C_times_k.getArray(), d);
		prediction = m;

		/*
		 * IM: should be done by invoking method if (!regression) { if (prediction > 0.0) prediction
		 * = 1.0; else prediction = 0.0; }
		 */

		return prediction;
	}

	/**
	 * Apply the model to all input vectors
	 */
	public double[] apply(double[][] inputVectors) throws Exception {
		double[] prediction = new double[inputVectors.length];
		for (int i = 0; i < inputVectors.length; i++) {
			prediction[i] = applyToVector(inputVectors[i]);
			if (!regression) {
				if (prediction[i] > 0.0) {
					prediction[i] = 1.0;
				} else {
					prediction[i] = 0.0;
				}
			}
		}

		return prediction;
	}
}
