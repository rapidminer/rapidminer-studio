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
 * The standard (slow, non scaling) regression RVM (see bla).
 * 
 * @author Piotr Kasprzak, Ingo Mierswa
 */
public class RVMClassification extends RVMBase {

	/** Constructor */
	public RVMClassification(ClassificationProblem problem, Parameter parameter) {
		super(problem, parameter);
	}

	/** The hard work is done here */
	@Override
	public Model learn() {

		ClassificationProblem problem = (ClassificationProblem) this.problem;

		int numExamples = problem.getProblemSize();
		int numBases = numExamples + 1;

		/** Set iteration control parameters */

		int prune_point = 50;														// Iteration number after which we switch to analytic pruning (in
								// percent)

		prune_point = parameter.maxIterations * prune_point / 100;

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

		double[] alphas = new double[numBases];					// inverse variances for the weights

		for (j = 0; j < numBases; j++) {
			alphas[j] = parameter.initAlpha;
		}

		/** Matrix-esize everything */

		Matrix matrixPHI = new Matrix(PHI);
		Matrix vectorAlpha = new Matrix(alphas, numBases);
		Matrix vectorWeights = new Matrix(numBases, 1, 0.0);
		Matrix prunedVectorWeights = null;

		Matrix matrixU = null;								// Cholesky decomposition of IRLS-Hessian = Sigma (Laplace-Approx.)
		Matrix matrixUInv = null;								// The inverse of the above

		LinkedList<Integer> unprunedIndicesList = null;					// List of indices of unpruned
														// alphas/weights/basisfuntions (= relevance
														// vectors)
		int[] unprunedIndicesArray = null;					// Array of the above, is also used for the reverse
											// mapping of the indices of a pruned
		// vector to the one of a unpruned vector

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
				unprunedIndicesArray[j] = iter.next().intValue();
			}

			Matrix prunedMatrixPHI = matrixPHI.getMatrix(0, matrixPHI.getRowDimension() - 1, unprunedIndicesArray);
			Matrix prunedVectorAlpha = vectorAlpha.getMatrix(unprunedIndicesArray, 0, 0);
			prunedVectorWeights = vectorWeights.getMatrix(unprunedIndicesArray, 0, 0);

			/**
			 * Use the iterative reweighted least algorithm (IRLS) to find the maximum w_mp of the
			 * marginal likelihood with respect to the weights
			 */

			double minGradientChange = 1e-6;											// Convergence criterion for IRLS
			double minLambda = Math.pow(2, -8);								// Maximum overshoot criterion

			Matrix matrixAlphaDiag = new Matrix(prunedVectorAlpha.getRowDimension(), prunedVectorAlpha.getRowDimension(), 0);
			for (j = 0; j < prunedVectorAlpha.getRowDimension(); j++) {
				matrixAlphaDiag.set(j, j, prunedVectorAlpha.get(j, 0));
			}

			Matrix vectorY = prunedMatrixPHI.times(prunedVectorWeights);	// Y = sigmoid(PHI * w);
			for (int k = 0; k < vectorY.getRowDimension(); k++) {
				vectorY.set(k, 0, sigmoid(vectorY.get(k, 0)));
			}

			/** Instead of maximising the marginal likelihood function, we minimize the negative log */

			double dataTerm = 0;
			int[] t = problem.getTargetVectors();
			for (int k = 0; k < t.length; k++) {
				if (t[k] == 1) {
					dataTerm -= Math.log(vectorY.get(k, 0));
				} else {
					dataTerm -= Math.log(1.0 - vectorY.get(k, 0));
				}
			}

			double penaltyTerm = 0;
			for (int k = 0; k < prunedVectorAlpha.getRowDimension(); k++) {
				penaltyTerm += prunedVectorAlpha.get(k, 0) * prunedVectorWeights.get(k, 0) * prunedVectorWeights.get(k, 0);
			}

			double error = (dataTerm + penaltyTerm / 2.0) / problem.getProblemSize();

			for (j = 0; j < 25; j++) {

				/**
				 * Calculate IRLS-Hessian: Hessian = X' * W * X = PHI' * W_IRLS * PHI = (W_IRLS *
				 * PHI)' * PHI
				 */

				Matrix matrixIRLSWeights = new Matrix(prunedMatrixPHI.getRowDimension(), prunedMatrixPHI.getRowDimension(),
						0);
				for (int k = 0; k < matrixIRLSWeights.getRowDimension(); k++) {
					matrixIRLSWeights.set(k, k, vectorY.get(k, 0) * (1.0 - vectorY.get(k, 0)));
				}

				Matrix matrixHessian = prunedMatrixPHI.transpose().times(matrixIRLSWeights).times(prunedMatrixPHI);
				matrixHessian.plusEquals(matrixAlphaDiag);

				/** Calculate the IRLS-gradient */

				Matrix vectorE = new Matrix(vectorY.getRowDimension(), 1, 0.0);
				for (int k = 0; k < vectorY.getRowDimension(); k++) {
					vectorE.set(k, 0, t[k] - vectorY.get(k, 0));
				}

				Matrix vectorPenalty = (Matrix) prunedVectorAlpha.clone();
				for (int k = 0; k < vectorPenalty.getRowDimension(); k++) {
					vectorPenalty.set(k, 0, vectorPenalty.get(k, 0) * prunedVectorWeights.get(k, 0));
				}

				Matrix vectorGradient = prunedMatrixPHI.transpose().times(vectorE).minus(vectorPenalty);

				/** Check for badly conditioned hessian */

				// if (j == 0) {
				// double cond = matrixHessian.cond();
				// if (cond < Math.pow(2, -52)) { // Compare condition to mantissa-precision of the
				// "double"-type
				// LogService.logMessage("(IRLS) ill-conditioned hession: cond = " + (new
				// Double(cond)).toString(), LogService.STATUS);
				// LogService.logMessage("(IRLS) returning back to hyperparameter estimation ...",
				// LogService.STATUS);
				//
				// /** Let's hope we have something to continue with ;) ... */
				// break;
				// }
				// }

				/** Get inverse factor of hessian */

				SECholeskyDecomposition CD = new SECholeskyDecomposition(matrixHessian.getArray());
				matrixU = CD.getPTR().times(CD.getL());

				matrixUInv = matrixU.inverse();

				/** Check for IRLS-convergence */

				if (j >= 2 && (vectorGradient.normF() / prunedVectorWeights.getRowDimension()) < minGradientChange) {
					break;
				}

				/** Make an IRLS step */

				Matrix vectorDeltaWeights = matrixUInv.transpose().times((matrixUInv.times(vectorGradient)));
				double lambda = 1;

				/** Prevent Maximum overshooting */

				while (lambda > minLambda) {

					/** w_new = w + labmda * w_delta */

					Matrix vectorNewWeights = ((Matrix) prunedVectorWeights.clone()).plus(vectorDeltaWeights.times(lambda));

					/** Recalculate error with respect to w_new */

					vectorY = prunedMatrixPHI.times(vectorNewWeights);			// Y = sigmoid(PHI * w);
					for (int k = 0; k < vectorY.getRowDimension(); k++) {
						vectorY.set(k, 0, sigmoid(vectorY.get(k, 0)));
					}

					dataTerm = 0;
					for (int k = 0; k < t.length; k++) {
						if (t[k] == 1) {
							dataTerm -= Math.log(vectorY.get(k, 0));
						} else {
							dataTerm -= Math.log(1.0 - vectorY.get(k, 0));
						}
					}

					penaltyTerm = 0;
					for (int k = 0; k < prunedVectorAlpha.getRowDimension(); k++) {
						penaltyTerm += prunedVectorAlpha.get(k, 0) * vectorNewWeights.get(k, 0) * vectorNewWeights.get(k, 0);
					}

					double error_new = (dataTerm + penaltyTerm / 2.0) / problem.getProblemSize();

					/** If overshot, back off */

					if (error_new > error) {
						lambda = lambda / 2.0;
						continue;
					}

					/** Everything's nice, we can continue with the next IRLS-step */

					prunedVectorWeights = vectorNewWeights;
					break;
				}
			}

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

		Model model = new Model(finalWeights, finalKernels, bias, false);
		return model;
	}

	/** Sigmoid link function */
	public double sigmoid(double x) {
		return 1.0 / (1.0 + Math.exp(-x));
	}

	/** Identify the RVM */
	@Override
	public String toString() {
		return "Classification-RVM";
	}
}
