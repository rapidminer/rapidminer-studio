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

import java.util.Iterator;
import java.util.LinkedList;


/**
 * 
 * The standard (slow, non scaling) regression RVM (see bla).
 * 
 * @author Piotr Kasprzak, Ingo Mierswa
 * 
 */
public class RVMRegression extends RVMBase {

	/** Constructor */
	public RVMRegression(RegressionProblem problem, Parameter parameter) {
		super(problem, parameter);
	}

	/** The hard work is done here */
	@Override
	public Model learn() {

		RegressionProblem problem = (RegressionProblem) this.problem;

		int numExamples = problem.getProblemSize();
		int numBases = numExamples + 1;

		/** Init hyperparameters with more or less sensible values (shouldn't be too important) */

		double initBeta = 1.0 / Math.pow(parameter.initSigma, 2);

		/**
		 * Create design/basis matrix PHI (N x M) with: N: number of examples M: number of examples
		 * + 1 (because of the bias in the first column) PHI(n,m) = phi_m(x_n) = K_m(x_n, x_m) [K
		 * being the kernel; x_n, x_m the n-the/m-th example]
		 */
		double[][] x = problem.getInputVectors();
		KernelBasisFunction[] kernels = problem.getKernels();
		double[][] PHI = new double[numExamples][numBases];
		int i, j;
		for (j = 0; j < numBases - 1; j++) {
			for (i = 0; i < numExamples; i++) {
				PHI[i][j + 1] = kernels[j + 1].eval(x[i]);
			}
		}

		// Set bias
		for (i = 0; i < numExamples; i++) {
			PHI[i][0] = 1.0;
		}

		/** Init weights/alpha/gamma/index vectors */

		double[] alphas = new double[numBases];						// inverse variances for the weights

		for (j = 0; j < numBases; j++) {
			alphas[j] = parameter.initAlpha;
		}

		/** Matrix-esize everything */

		Matrix matrixPHI = new Matrix(PHI);
		Matrix vectorT = new Matrix(problem.getTargetVectors());
		Matrix vectorAlpha = new Matrix(alphas, numBases);
		Matrix vectorPHI_T = (matrixPHI.transpose()).times(vectorT);

		LinkedList<Integer> unprunedIndicesList = null;					// List of indices of unpruned
														// alphas/weights/basisfuntions (= relevance
														// vectors)
		int[] unprunedIndicesArray = null;					// Array of the above, is also used for the reverse
											// mapping of the indices of a pruned
		// vector to the one of a unpruned vector
		Matrix prunedVectorWeights = null;

		/** The main iteration */
		for (i = 1; i <= parameter.maxIterations; i++) {

			// Prune associated basis functions / weights for too big alphas

			unprunedIndicesList = new LinkedList<Integer>();
			for (j = 0; j < numBases; j++) {
				if (vectorAlpha.get(j, 0) >= parameter.alpha_max) {
					// pruned
				} else {
					unprunedIndicesList.add(Integer.valueOf(j));
				}
			}

			unprunedIndicesArray = new int[unprunedIndicesList.size()];
			Iterator<Integer> iter = unprunedIndicesList.iterator();
			for (j = 0; j < unprunedIndicesList.size(); j++) {
				unprunedIndicesArray[j] = iter.next();
			}

			Matrix prunedMatrixPHI = matrixPHI.getMatrix(0, matrixPHI.getRowDimension() - 1, unprunedIndicesArray);
			Matrix prunedVectorPHI_T = vectorPHI_T.getMatrix(unprunedIndicesArray, 0, 0);
			Matrix prunedVectorAlpha = vectorAlpha.getMatrix(unprunedIndicesArray, 0, 0);

			// Calculate mean and covariance matrix of the weight posterior distribution (gaussian)
			// using
			// current hyperparamter estimates. For numeric stability the cholesky decomposition is
			// used.

			Matrix matrixAlphaDiag = new Matrix(prunedVectorAlpha.getRowDimension(), prunedVectorAlpha.getRowDimension(), 0);
			for (j = 0; j < prunedVectorAlpha.getRowDimension(); j++) {
				matrixAlphaDiag.set(j, j, prunedVectorAlpha.get(j, 0));
			}
			Matrix matrixSIGMAInv = (prunedMatrixPHI.transpose()).times(prunedMatrixPHI);		// PHI' *
																							// PHI
			matrixSIGMAInv.timesEquals(initBeta);														// PHI' * PHI * beta
			matrixSIGMAInv.plusEquals(matrixAlphaDiag);												// PHI' * PHI * beta + diag(alpha) =
														// SIGMA^-1

			// Matrix matrixU = (matrixSIGMAInv.chol()).getL(); // U * U' = SIGMA^-1

			SECholeskyDecomposition CD = new SECholeskyDecomposition(matrixSIGMAInv.getArray());
			Matrix matrixU = CD.getPTR().times(CD.getL());

			Matrix matrixUInv = matrixU.inverse();

			// w = mu = beta * SIGMA * PHI' * t
			// = beta * (SIGMA^-1)^-1 * PHI' * t
			// = beta * (U' * U)^-1 * PHI' * t
			// = beta * U^-1 * U'^-1 * PHI' * t

			prunedVectorWeights = ((matrixUInv.transpose()).times(matrixUInv.times(prunedVectorPHI_T))).times(initBeta);

			// Get diagonal elements of covariance matrix SIGMA

			double[] diagSIGMA = new double[matrixUInv.getRowDimension()];
			for (j = 0; j < diagSIGMA.length; j++) {
				double value = 0;
				for (int k = 0; k < diagSIGMA.length; k++) {
					value += matrixUInv.get(k, j) * matrixUInv.get(k, j);
				}
				diagSIGMA[j] = value;
			}

			// Calculate gammas: gamma = 1 - alpha * SIGMA_ii

			double[] gammas = new double[diagSIGMA.length];
			for (j = 0; j < gammas.length; j++) {
				gammas[j] = 1.0 - prunedVectorAlpha.get(j, 0) * diagSIGMA[j];
			}

			// Get log alphas

			double[] logAlphas = new double[prunedVectorAlpha.getRowDimension()];
			for (j = 0; j < logAlphas.length; j++) {
				logAlphas[j] = Math.log(prunedVectorAlpha.get(j, 0));
			}

			// Alpha update: alpha = gamma / mu^2 = gamma / w^2;

			for (j = 0; j < prunedVectorAlpha.getRowDimension(); j++) {
				double newAlpha = gammas[j] / (prunedVectorWeights.get(j, 0) * prunedVectorWeights.get(j, 0));
				prunedVectorAlpha.set(j, 0, newAlpha);
			}

			// Check for iteration abort

			double maxLogAlphaChange = 0;
			for (j = 0; j < logAlphas.length; j++) {
				double change = Math.abs(logAlphas[j] - Math.log(prunedVectorAlpha.get(j, 0)));
				if (change > maxLogAlphaChange) {
					maxLogAlphaChange = change;
				}
			}

			if (maxLogAlphaChange < parameter.min_delta_log_alpha) {
				break;
			}

			// Beta update: beta = 1 / sigma^2 = N - sum(gammas) / norm_l2(t - PHI * mu)^2

			double dataError = 0;
			Matrix dataDelta = vectorT.minus(prunedMatrixPHI.times(prunedVectorWeights));	// t - PHI
																							// * mu
			for (j = 0; j < numExamples; j++) {
				dataError += (dataDelta.get(j, 0) * dataDelta.get(j, 0));					// norm_l2(t - PHI * mu)^2
			}

			double sumGammas = 0;
			for (j = 0; j < gammas.length; j++) {
				sumGammas += gammas[j];														// sum(gammas)
			}

			initBeta = (numExamples - sumGammas) / dataError;

			// update the (unpruned) alpha vector with the corresponding values from the pruned
			// alpha vector

			for (j = 0; j < prunedVectorAlpha.getRowDimension(); j++) {
				vectorAlpha.set(unprunedIndicesArray[j], 0, prunedVectorAlpha.get(j, 0));
			}
		}

		/** Create model */
		double[] finalWeights = new double[unprunedIndicesArray.length];
		KernelBasisFunction[] finalKernels = new KernelBasisFunction[unprunedIndicesArray.length];

		boolean bias = false;

		for (j = 0; j < unprunedIndicesArray.length; j++) {
			finalWeights[j] = prunedVectorWeights.get(j, 0);
			if (unprunedIndicesArray[j] == 0) {
				// bias wasn't pruned
				bias = true;
				finalKernels[j] = new KernelBasisFunction(new KernelRadial());
			} else {
				finalKernels[j] = kernels[unprunedIndicesArray[j]];
			}
		}

		Model model = new Model(finalWeights, finalKernels, bias, true);
		return model;
	}

	/** Identify the RVM */
	@Override
	public String toString() {
		return "Regression-RVM";
	}
}
