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

import Jama.Matrix;
import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.KernelBasisFunction;
import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.KernelRadial;
import com.rapidminer.operator.learner.functions.kernel.rvm.util.SECholeskyDecomposition;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;

import java.util.LinkedList;
import java.util.List;


/**
 * Constructive RVM for regression problems (see bla).
 * 
 * @author Piotr Kasprzak, Ingo Mierswa
 * 
 */
public class ConstructiveRegression extends RVMBase {

	/** Data shared across various methods */

	protected double[][] x;					// Input vectors
	protected double[][] t;					// Target vectors
	protected double[] tVector;				// (One dimensional) target vector

	protected double[][] phi;				// Basis functions evaluated on all input vectors

	protected Matrix PHI_t;					// (Pruned) transposed design matrix PHI^t
	protected double[] alpha;				// Vector of inverse variances for the weights
	protected double beta;					// beta = sigma^{-2} = inverse noise variance
	protected Matrix A;						// Diagonal Matrix consisting of alphas

	protected Matrix SIGMA;					// Covariance matrix of weight posterior distribution
	protected Matrix SIGMA_chol;			// Cholesky factor of the above

	protected Matrix mu;					// Mean of the weight posterior distribution

	protected double s, q;					// Used in the criterium for inclusion / deletion of basis vectors

	protected LinkedList<Integer> basisSet = new LinkedList<Integer>();

	private boolean useLocalRandomSeed;
	private int localRandomSeed;

	/**
	 * Constructor
	 * 
	 * @param localRandomSeed
	 * @param useLocalRandomSeed
	 */
	public ConstructiveRegression(RegressionProblem problem, Parameter parameter, boolean useLocalRandomSeed,
			int localRandomSeed) {
		super(problem, parameter);
		this.useLocalRandomSeed = useLocalRandomSeed;
		this.localRandomSeed = localRandomSeed;
	}

	/** Take a list holding "Double"-objects and return an "double[]" */
	protected double[] convertListToDoubleArray(List<Double> list) {

		double[] array = new double[list.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = list.get(i);
		}

		return array;
	}

	/** Return the inner product of x and y (x.length == y.length assumed) */
	protected double innerProduct(double[] x, double[] y) {
		double sum = 0;
		for (int i = 0; i < x.length; i++) {
			sum += x[i] * y[i];
		}
		return sum;
	}

	/**
	 * Create pruned versions of all important matrices / vectors so that only rows / columns
	 * matching the indices in basisSet are kept.
	 */
	protected void prune(LinkedList<Integer> basisSet) {

		/** Create PHIt */

		double[][] PHI_t_Array = new double[basisSet.size()][];
		for (int j = 0; j < basisSet.size(); j++) {
			PHI_t_Array[j] = phi[basisSet.get(j)];
		}

		PHI_t = new Matrix(PHI_t_Array);

		/** Create diagonal Matrix A */

		A = new Matrix(basisSet.size(), basisSet.size());
		for (int j = 0; j < basisSet.size(); j++) {
			A.set(j, j, alpha[basisSet.get(j)]);
		}
	}

	/**
	 * Update the covariance Matrix of the weight posterior distribution (SIGMA) along with its
	 * cholesky factor:
	 * 
	 * SIGMA = (A + beta * PHI^t * PHI)^{-1}
	 * 
	 * SIGMA_chol with SIGMA_chol * SIGMA_chol^t = SIGMA
	 */
	protected void updateSIGMA() {

		Matrix SIGMA_inv = PHI_t.times(PHI_t.transpose());
		SIGMA_inv.timesEquals(beta);
		SIGMA_inv.plusEquals(A);

		/** Update the factor ... */

		SECholeskyDecomposition CD = new SECholeskyDecomposition(SIGMA_inv.getArray());
		Matrix U = CD.getPTR().times(CD.getL());
		SIGMA_chol = U.inverse();

		/** Update SIGMA */

		SIGMA = (SIGMA_chol.transpose()).times(SIGMA_chol);
	}

	/**
	 * Update the mean of the weight posterior distribution (mu):
	 * 
	 * mu = beta * SIGMA * PHI^t * t
	 */
	protected void updateMu() {
		mu = SIGMA.times(PHI_t.times(new Matrix(t)));
		mu.timesEquals(beta);
	}

	/**
	 * Compute the scalars s_m, q_m which are part of the criterium for inclusion / deletion of the
	 * given basis m:
	 * 
	 * S_m = beta * phi^t_m * phi_m - beta^2 * phi^t_m * PHI * SIGMA * PHI^t * phi_m Q_m = beta *
	 * phi^t_m * t - beta^2 * phi^t_m * PHI * SIGMA * PHI^t * t
	 * 
	 * s_m = alpha_m * S_m / (alpha_m - S_m) q_m = alpha_m * Q_m / (alpha_m - S_m)
	 */
	protected void updateCriteriumScalars(int selectedBasis) {

		Matrix SigmaStuff = (PHI_t.transpose()).times(SIGMA.times(PHI_t));

		double S = beta
				* innerProduct(phi[selectedBasis], phi[selectedBasis])
				- beta
				* beta
				* innerProduct(phi[selectedBasis],
						SigmaStuff.times(new Matrix(phi[selectedBasis], phi[selectedBasis].length)).getRowPackedCopy());

		double Q = beta * innerProduct(phi[selectedBasis], tVector) - beta * beta
				* innerProduct(phi[selectedBasis], SigmaStuff.times(new Matrix(t)).getRowPackedCopy());

		s = alpha[selectedBasis] * S / (alpha[selectedBasis] - S);
		q = alpha[selectedBasis] * Q / (alpha[selectedBasis] - S);
	}

	/**
	 * Reestimate alpha by setting it to the value which maximizes the marginal likelihood:
	 * 
	 * alpha_i = s^2_i / (q^2_i - s_i)
	 */
	protected void reestimateAlpha(int selectedBasis) {
		alpha[selectedBasis] = s * s / (q * q - s);
	}

	/**
	 * Include a basis function into the model.
	 */
	protected void includeBasis(int selectedBasis) {
		basisSet.add(Integer.valueOf(selectedBasis));
		reestimateAlpha(selectedBasis);
	}

	/**
	 * Delete a basis function from the model.
	 */
	protected void deleteBasis(int selectedBasis) {
		basisSet.remove(Integer.valueOf(selectedBasis));
		alpha[selectedBasis] = -1.0d;
	}

	/**
	 * Update beta (same as for the "normal" regression rvm)
	 */
	protected void updateBeta() {

		/** Calculate gammas && their sum: gamma_i = 1 - alpha_i * SIGMA_ii */

		double[] gammas = new double[basisSet.size()];
		for (int j = 0; j < basisSet.size(); j++) {
			gammas[j] = 1.0d - alpha[basisSet.get(j)] * SIGMA.get(j, j);
		}

		double sumGammas = 0;
		for (int j = 0; j < gammas.length; j++) {
			sumGammas += gammas[j];
		}

		/** Calculate delta = t - PHI * mu */

		Matrix DELTA = (new Matrix(t)).minus(PHI_t.transpose().times(mu));

		/** beta = N - sum_i(gamma_i) / norm_l2(DELTA)^2 */

		beta = x.length - sumGammas / innerProduct(DELTA.getRowPackedCopy(), DELTA.getRowPackedCopy());
	}

	/** The hard work is done here */
	@Override
	public Model learn() {

		RegressionProblem problem = (RegressionProblem) this.problem;

		int numExamples = problem.getProblemSize();
		int numBases = numExamples + 1;

		/** Set iteration control parameters */

		// int monIts = 1;

		/** Init hyperparameters with more or less sensible values (shouldn't be too important) */

		// beta = Math.pow(parameter.initSigma, -2);
		beta = Math.pow(0.5, -2);

		/**
		 * Create m x n matrix (= PHI^t) with all basis functions (each evaluated at all input
		 * vectors)
		 */

		x = problem.getInputVectors();
		KernelBasisFunction[] kernels = problem.getKernels();
		phi = new double[numBases][numExamples];

		int i, j;

		for (j = 0; j < numBases - 1; j++) {
			for (i = 0; i < numExamples; i++) {
				phi[j + 1][i] = kernels[j + 1].eval(x[i]);
			}
		}

		// Set bias
		for (i = 0; i < numExamples; i++) {
			phi[0][i] = 1.0;
		}

		/** Init target vector */

		t = problem.getTargetVectors();
		tVector = new double[t.length];
		for (i = 0; i < t.length; i++) {
			tVector[i] = t[i][0];
		}

		/** Initialize all alphas to be out-of-model (= -1.0) */

		alpha = new double[numBases];
		for (i = 0; i < alpha.length; i++) {
			alpha[i] = -1.0d;
		}

		/** Init basisSet */

		int selectedBasis = RandomGenerator.getRandomGenerator(useLocalRandomSeed, localRandomSeed).nextInt();
		basisSet.add(Integer.valueOf(selectedBasis));

		/** Init alphas (model hyperparameters: inverse variances for the weights) */

		double normPhiSquare = innerProduct(phi[selectedBasis], phi[selectedBasis]);
		alpha[selectedBasis] = normPhiSquare / (innerProduct(phi[selectedBasis], tVector) / normPhiSquare - 1.0d / beta);

		/** The main iteration */
		for (i = 1; i <= parameter.maxIterations; i++) {

			// get 'old' log alphas
			double[] logAlphas = new double[alpha.length];
			for (j = 0; j < logAlphas.length; j++) {
				double value = Math.log(alpha[j]);
				if (Double.isNaN(value)) {
					value = 0.0d;
				}
				logAlphas[j] = value;
			}

			prune(basisSet);

			updateSIGMA();
			updateMu();

			/** beta update */
			updateBeta();

			/** Select a random basis */
			// selectedBasis = RandomGenerator.getGlobalRandomGenerator().nextInt(numBases);
			selectedBasis = i % numBases;

			/** Test for inclusion / deletion */
			updateCriteriumScalars(selectedBasis);
			double theta = q * q - s;

			if (theta > 0) {
				if (alpha[selectedBasis] > 0) {
					/** Basis already in the model => re-estimate alpha */
					reestimateAlpha(selectedBasis);
				} else {
					/** Basis not in the model => include it */
					includeBasis(selectedBasis);
				}
			} else if (alpha[selectedBasis] > 0) {
				/** Basis is part of the model => delete it */
				deleteBasis(selectedBasis);
			}

			// check for iteration abort
			double maxLogAlphaChange = 0;
			for (j = 0; j < logAlphas.length; j++) {
				double newValue = Math.log(alpha[j]);
				if (Double.isNaN(newValue)) {
					newValue = 0.0d;
				}
				double change = Math.abs(logAlphas[j] - newValue);
				if (change > maxLogAlphaChange) {
					maxLogAlphaChange = change;
				}
			}
			if (Tools.isNotEqual(maxLogAlphaChange, 0.0d) && (maxLogAlphaChange < parameter.min_delta_log_alpha)) {
				break;
			}
		}

		/** Create model */
		double[] finalWeights = new double[basisSet.size()];
		KernelBasisFunction[] finalKernels = new KernelBasisFunction[basisSet.size()];

		boolean bias = false;

		for (j = 0; j < basisSet.size(); j++) {
			finalWeights[j] = mu.get(j, 0);
			if (basisSet.get(j) == 0) {
				// bias wasn't pruned
				bias = true;
				finalKernels[j] = new KernelBasisFunction(new KernelRadial());
			} else {
				finalKernels[j] = kernels[basisSet.get(j)];
			}
		}

		Model model = new Model(finalWeights, finalKernels, bias, true);
		return model;
	}

	/** Identify the RVM */
	@Override
	public String toString() {
		return "Constructive-Regression-RVM";
	}
}
