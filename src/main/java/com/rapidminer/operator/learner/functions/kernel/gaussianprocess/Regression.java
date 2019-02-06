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

import java.util.TreeSet;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.Kernel;

import Jama.Matrix;


/**
 * Gaussian Process Regression.
 *
 * REFERENCES:
 *
 * Lehel Csato. Gaussian Processes --- Iterative Sparse Approximations. PhD thesis, Aston
 * University, Birmingham, UK, March 2002.
 *
 * TODO: - own Matrix implementation with SE-cholesky-decomposition and the ability to constrain
 * matrix operation on sub matrices - CompositeKernel implementation for kernels based on finite
 * basis functions
 *
 * @author Piotr Kasprzak
 *
 */
public class Regression extends GPBase {

	/** Used to hold a score value with an associated index */

	protected static class Score implements Comparable<Score> {

		double score;

		int index;

		Score(double score, int index) {
			this.score = score;
			this.index = index;
		}

		@Override
		public int compareTo(Score s2) throws NullPointerException {

			if (score < s2.getScore()) {
				return -1;
			}

			if (score == s2.getScore()) {
				return 0;
			}

			return +1;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Score) {
				return score == ((Score) o).score;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Double.valueOf(score).hashCode();
		}

		public double getScore() {
			return score;
		}

		public int getIndex() {
			return index;
		}
	}

	/** Constructor */

	public Regression(RegressionProblem problem, Parameter parameter, Operator operator) {
		super(problem, parameter, operator);
	}

	/**
	 * Compute the (canonical) scalar product between x and y, using only the first d components of
	 * the vectors
	 */

	private double scalarProduct(double[][] x, double[][] y, int d) throws Exception {
		/* Lengths of x and y must be both >= d */
		if (x.length < d || y.length < d) {
			throw new Exception("At least one vector has a too small dimension!");
		}

		double result = 0;

		for (int i = 0; i < d; i++) {
			result += x[i][0] * y[i][0];
		}

		return result;
	}

	/**
	 * Swap the rows / columns of a symmetric n x n matrix, which is represented as a double[][].
	 */

	private void swapRowsAndColumns(double[][] A, int i, int j) {

		/* Dimensions of the matrix */
		int n = A[0].length;

		/* Temps */
		double[] tr;
		double ts;

		/* Swap rows of A */
		tr = A[i];
		A[i] = A[j];
		A[j] = tr;

		/* Swap columns of A */
		for (int k = 0; k < n; k++) {
			ts = A[k][i];
			A[k][i] = A[k][j];
			A[k][j] = ts;
		}
	}

	/** The hard work is done here */

	@Override
	public Model learn() throws Exception {

		RegressionProblem problem = (RegressionProblem) this.problem;

		int numExamples = problem.getProblemSize();
		int inputDim = problem.getInputDimension();

		double[][] x = problem.getInputVectors();
		double[][] y = problem.getTargetVectors();

		double[] x_new; // = new double[inputDim];
		double y_new;

		Kernel kernel = problem.getKernel();

		/* Maximum number of basis vector to use (= size of various vectors + matrices) */

		int dMax = parameter.maxBasisVectors + 1;

		/* Current number of used basis vectors */

		int d = 0;

		/* Alpha parameterizes the mean of the posterior GP. */

		Matrix alpha = new Matrix(dMax, 1);

		/* C parameterizes the covariance matrix of the posterior GP. */

		Matrix C = new Matrix(dMax, dMax);

		/* s is used in the updates of alpha and C. */

		Matrix s;

		/*
		 * Q is the inverse Kernel matrix. It's build up iterativly to prevent a O(d^3) performance
		 * hit.
		 */

		Matrix Q = new Matrix(dMax, dMax);

		/*
		 * The vector k_t+1 = [K(x_1, x_t+1), K(x_2, x_t+1), ..., K(x_t, x_t+1)], holding the
		 * results of using the kernel on the the new input x_t+1 and the rest of the old inputs
		 */

		Matrix k = new Matrix(dMax, 1);

		/*
		 * e_t+1 are the projection coordinates of the associated basis vector of a new input when
		 * using an orthogonal projection (see Chapter 3.1). We are using a KL-optimal projection
		 * (see Chapter 3.3) but the vector e_t+1 is still needed for the upadte of the inverse
		 * kernel gram matrix Q.
		 */

		Matrix e;

		/* The t+1-th unit vector. */

		Matrix u = new Matrix(dMax, 1);

		/* The basis vectors */

		double[][] basisVectors = new double[dMax][inputDim];

		/* Temorary result: C_t * k_t+1 */

		Matrix C_times_k;

		/* Used temporarily */

		Matrix t = new Matrix(dMax, 1);
		// double[] t_row = new double[inputDim];
		// double t_scalar = 0;

		double k_star = 0;
		double nabla = 0;

		/* Some statistics */

		int gamma_projections = 0; // Number of gamma induced projections
		int kl_projections = 0; // Number of KL-based projections (after the maximum
		// number of bvs is exceeded)
		int geometrical_projections = 0; // Number of geometrical projections (to prevent the
		// kernel gram matrix from becoming too badly
		// conditioned)

		/** Process every input/output example */

		for (int i = 0; i < numExamples; i++) {
			checkForStop();
			x_new = x[i];
			y_new = y[i][0];

			/**
			 * Compute k_t+1 = [K(x_1, x_t+1), K(x_2, x_t+1), ..., K(x_t, x_t+1)] and
			 * 
			 * k_star = K(x_t+1, x_t+1)
			 */

			for (int j = 0; j < d; j++) {
				checkForStop();
				k.getArray()[j][0] = kernel.eval(basisVectors[j], x_new);
			}

			k_star = kernel.eval(x_new, x_new);

			/**
			 * Compute the scalars q_new and r_new
			 * 
			 * NOTE:
			 * 
			 * We imply a gaussian likelihood function (a regression model with gaussian noise)
			 * here, which permits us to have analytical expression for q_new / r_new. A more
			 * elaborate Implementation will use integral approximations (e.g. monta carlo based) to
			 * allow for other likelihoods where there are none such analytical exporessions.
			 * 
			 */

			/*
			 * Compute m_t+1, the mean of the marginal of the GP at x_t+1, which is gaussian:
			 * 
			 * m_t+1 = {k_t+1}^T * alpha_t
			 */

			double m = scalarProduct(k.getArray(), alpha.getArray(), d);

			/*
			 * Compute sigma_t+1^2, the covariance^2 of the marginal of the GP at x_t+1, which is
			 * gaussian:
			 * 
			 * {sigma_t+1}^2 = K(x_t+1, x_t+1) + {k_t+1}^T * C_t * k_t+1
			 * 
			 * (see end of chapter 2.4 and chapter 5.1)
			 */

			C_times_k = C.times(k);

			double sigma_2 = k_star + scalarProduct(k.getArray(), C_times_k.getArray(), d);

			/* Compute q_t+1 = (y_t+1 - m_t+1) / ({sigma_0}^2 + {sigma_t+1}^2) */
			double q = (y_new - m) / (problem.sigma_0_2 + sigma_2);

			/* Compute r_t+1 = -1.0 / ({sigma_0}^2 + {sigma_t+1}^2) */
			double r = -1.0 / (problem.sigma_0_2 + sigma_2);

			/** Compute e_t+1 = Q_t * k_t+1 */

			e = Q.times(k);

			/**
			 * Compute the (length of the residual)^2 [if using an orthogonal projection; we need it
			 * for the iterative update of the inverse kernel gram matrix Q]:
			 * 
			 * gamma_t+1 = k_star - {k_t+1}^T * Q_t * k_t+1 (chapter 3.1)
			 * 
			 */

			double gamma = k_star - scalarProduct(k.getArray(), e.getArray(), d);

			/* DEBUG: Check if Q is REALLY the inverse kernel gram matrix */

			Matrix Gram = new Matrix(d, d);
			for (int ii = 0; ii < d; ii++) {
				for (int jj = 0; jj < d; jj++) {
					checkForStop();
					Gram.getArray()[ii][jj] = kernel.eval(basisVectors[ii], basisVectors[jj]);
				}
			}

			// IM: WARNING: the variable was never used !!!
			// Matrix inverseGram = Q.getMatrix(0, d - 1, 0, d - 1);

			// IM: WARNING: the following Matrix was never used !!!
			// Matrix diffGram = Gram.times(inverseGram);

			/**
			 * Check if the length of the residual is too small and if so, use the projection
			 * equations / sparse update (3.29) and proceed to the next input. This should help to
			 * maintain a good numerical condition of Q.
			 * 
			 * nabla_t+1 = (1 + gamma_t+1 * r_t+1)^{-1}
			 * 
			 * s_t+1 = C_t * k_t+1 + e_t+1
			 * 
			 * alpha_t+1 = alpha_t + q_t+1 * nabla_t+1 * s_t+1
			 * 
			 * C_t+1 = C_t + r_t+1 * nabla_t+1 * s_t+1 * {s_t+1}^T
			 */

			if (gamma < parameter.epsilon_tol) {
				nabla = 1.0 / (1 + gamma * r);
				s = C_times_k.plus(e);
				alpha = alpha.plus(s.times(q * nabla));
				C = C.plus((s.times(s.transpose())).times(r * nabla));
				gamma_projections++;
			} else {
				/**
				 * Add the basis vector of the current input to the BV set and update the the
				 * appropriate variables using (2.46), increasing the dimensions of all vectors and
				 * matrices (u_t+1 is the t+1-th unit vector) and also updating Q.
				 * 
				 * s_t+1 = C_t * k_t+1 + u_t+1
				 * 
				 * alpha_t+1 = alpha_t + q_t+1 * s_t+1
				 * 
				 * C_t+1 = C_t + r_t+1 * s_t+1 * {s_t+1}^T
				 */

				/* Build the t+1-th unit vector */

				for (int j = 0; j < dMax; j++) {
					u.getArray()[j][0] = 0;
				}

				u.getArray()[d][0] = 1.0;

				/* Apply the standard online update as described above */

				s = C_times_k.plus(u);
				alpha = alpha.plus(s.times(q));
				C = C.plus((s.times(s.transpose())).times(r));

				/*
				 * We have enlarged our BV-set, so we need to update the inverse kernel Gram matrix.
				 * To prevent a O(d^3) performance hit, we do it iteratively (Appendix C), which
				 * costs us just O(d^2).
				 * 
				 * Q_t+1 = Q_t + {gamma_t+1}^{-1} * (e_t+1 - u_t+1) * (e_t+1 - u_t+1)^T
				 */

				t = e.minus(u);

				Q = Q.plus((t.times(t.transpose())).times(1.0 / gamma));

				/* Remember the added basis vector */

				basisVectors[d] = x_new;

				d++;

				Gram = new Matrix(d, d);
				for (int ii = 0; ii < d; ii++) {
					for (int jj = 0; jj < d; jj++) {
						checkForStop();
						Gram.getArray()[ii][jj] = kernel.eval(basisVectors[ii], basisVectors[jj]);
					}
				}

				// Q.setMatrix(0, d - 1, 0, d - 1, Gram.inverse());

				/* Invert Q via cholesky decomposition */

				Matrix L = Gram.chol().getL();
				Matrix invL = L.inverse();

				Q.setMatrix(0, d - 1, 0, d - 1, invL.transpose().times(invL));
			}

			/**
			 * Check if we have too many basis vectors. If so:
			 *
			 * 1.) For each basis vector i calculate a score epsilon_i (3.26) [approximation of
			 * (3.24)]
			 *
			 * epsilon_t+1(i) = alpha^2(i) / (q(i) + c(i))
			 *
			 * 2.) Find the basis vector with the minimum score 3.) Permutate the GP
			 * parameterisation, so that this basis vector is the last added 4.) Remove the last
			 * basis vector using eqs. (3.19), (3.21) and (3.22)
			 */

			if (d >= dMax) {
				int min_index = getMinScoresKLApprox(alpha, C, Q, d).first().getIndex();
				/*
				 * Permute the relevant vector / matrices, so that the found basis vector is the
				 * last.
				 */
				deleteBV(alpha, C, Q, basisVectors, d - 1, min_index);
				d--;
				kl_projections++;
			}

			/**
			 * Check the geometrical score. All BVs with a score below a certain threshold are
			 * removed.
			 */
			while (d > 0) {
				Score minScore = getMinScoresGeometrical(alpha, C, Q, d).first();
				if (minScore.getScore() > parameter.geometrical_tol) {
					break;
				}
				deleteBV(alpha, C, Q, basisVectors, d - 1, minScore.getIndex());
				d--;
				geometrical_projections++;
			}
		}
		return new Model(kernel, basisVectors, alpha.getMatrix(0, d - 1, 0, 0), C.getMatrix(0, d - 1, 0, d - 1),
				Q.getMatrix(0, d - 1, 0, d - 1), d, true);
	}

	/**
	 * Return the scores of all BVs.
	 *
	 * The score used is a mean based approximation to the true KL-distance between the full GP and
	 * the GP without the current BV for which the score is calculated.
	 */

	private TreeSet<Score> getMinScoresKLApprox(Matrix alpha, Matrix C, Matrix Q, int d) {
		/* The scores used to decide which basis vector to remove */
		TreeSet<Score> scores = new TreeSet<Score>();

		/* Compute the basis vector with minimal score */
		for (int j = 0; j < d; j++) {
			double score = alpha.getArray()[j][0] * alpha.getArray()[j][0] / (Q.getArray()[j][j] + C.getArray()[j][j]);
			scores.add(new Score(score, j));
		}

		return scores;
	}

	/**
	 * Return the scores of all BVs.
	 *
	 * The score used is based on the euclidean distance between the current BV and it's orthogonal
	 * projection into the span of all the other BVs.
	 *
	 * This score is especially important since it can (must!) be used to prevent the kernel gram
	 * matrix from becoming too badly conditioned.
	 */

	private TreeSet<Score> getMinScoresGeometrical(Matrix alpha, Matrix C, Matrix Q, int d) {
		/* The scores used to decide which basis vector to remove */
		TreeSet<Score> scores = new TreeSet<Score>();

		/* Compute the basis vector with minimal score */
		for (int j = 0; j < d; j++) {
			double score = 1.0 / Q.getArray()[j][j];
			scores.add(new Score(score, j));
		}

		return scores;
	}

	/**
	 * Delete the given BV from the BV set by adjusting the parametrisation of the GP using eqs.
	 * (3.19), (3.21) and (3.22):
	 * 
	 * alpha_t+1 = alpha^{(r)} - alpha^star / (c^star + q^star) * (Q^star + C^star)
	 * 
	 * C_t+1 = C^{(r)} + Q^star * Q^star^T / q^star - (Q^star + C^star) * (Q^star + C^star)^T /
	 * (q^star + c^star)
	 * 
	 * Q_t+1 = Q^{(r)} - Q^star * Q^star^T / q^star
	 * 
	 */

	private void deleteBV(Matrix alpha, Matrix C, Matrix Q, double[][] basisVectors, int d, int index) {

		int inputDim = basisVectors[0].length; // Dimension of input vectors
		int dMax = basisVectors.length; // Maximium number of BVs

		double[] t_row;
		double t_scalar = 0;

		/* Permutate alpha */
		t_scalar = alpha.getArray()[index][0];
		alpha.getArray()[index][0] = alpha.getArray()[d][0];
		alpha.getArray()[d][0] = t_scalar;

		/* Permutate C */
		swapRowsAndColumns(C.getArray(), index, d);

		/* Permutate Q */
		swapRowsAndColumns(Q.getArray(), index, d);

		/* Permutate basis vector ordering */
		t_row = basisVectors[index];
		basisVectors[index] = basisVectors[d];
		basisVectors[d] = t_row;

		/* Apply the pruning eqs. */

		double alpha_star = alpha.getArray()[d][0];
		double c_star = C.getArray()[d][d];
		double q_star = Q.getArray()[d][d];

		double[][] C_star = new double[dMax][1];
		Matrix vector_C_star = new Matrix(C_star);

		double[][] Q_star = new double[dMax][1];
		Matrix vector_Q_star = new Matrix(Q_star);

		for (int j = 0; j < d; j++) {
			C_star[j][0] = C.getArray()[j][d];
			Q_star[j][0] = Q.getArray()[j][d];
		}

		alpha.minusEquals((vector_Q_star.plus(vector_C_star)).times(alpha_star / (c_star + q_star)));

		C.plusEquals((vector_Q_star.times(vector_Q_star.transpose())).times(1.0 / q_star));
		C.minusEquals(((vector_Q_star.plus(vector_C_star)).times((vector_Q_star.plus(vector_C_star)).transpose()))
				.times(1.0 / (q_star + c_star)));

		Q.minusEquals((vector_Q_star.times(vector_Q_star.transpose())).times(1.0 / q_star));

		/* Prune projected rows / columns */

		alpha.getArray()[d][0] = 0;

		for (int j = 0; j <= d; j++) {

			Q.getArray()[d][j] = 0; // Clear d-th row
			C.getArray()[d][j] = 0;

			Q.getArray()[j][d] = 0; // Clear d-th column
			C.getArray()[j][d] = 0;
		}
	}

	/** Identify the GP */
	@Override
	public String toString() {
		return "Gauss-Regression-GP";
	}
}
