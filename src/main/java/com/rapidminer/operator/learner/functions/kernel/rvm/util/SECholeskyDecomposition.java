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
package com.rapidminer.operator.learner.functions.kernel.rvm.util;

import Jama.Matrix;
import com.rapidminer.tools.Tools;

import java.util.LinkedList;


/**
 * A modified cholesky factorization.
 * 
 * Given a n x n matrix A this decomposition produces a matrix L, such that L * L' = A + E, with E
 * being a minimal diagonal matrix making the sum of both matrices positive definite (and regular).
 * In contrast to the standard cholesky decomposition (as implemented in e.g. Jama) the matrix A
 * doesn't have to be regular and positive definite as often happens due to numerical instabilities.
 * The determination of the diagonal elements of E is based on Gerschgorin's circle theorem
 * minimizing ||E||_inf.
 * 
 * The a-priori upper bound of ||E||_inf is linear in n;
 * 
 * REFERENCES:
 * 
 * Robert B. Schnabel and Elizabeth Eskow. 1990. "A New Modified Cholesky Factorization," SIAM
 * Journal of Scientific Statistical Computating, 11, 6: 1136-58.
 * 
 * Robert B. Schnabel and Elizabeth Eskow. 1999.
 * "A revised modified Cholesky factorization algorithm," SIAM J. Optim., 9, 4: 1135--1148
 * (electronic)
 * 
 * Jeff Gill and Gary King. 1998. "`Hessian not Invertable.' Help!" manuscript in progress, Harvard
 * University.
 * 
 * @author Piotr Kasprzak
 * 
 */
public class SECholeskyDecomposition {

	/**
	 * The L-factor (lower triangle matrix) of matrix factorization calculated in decompose().
	 * 
	 * NOTE: Due to pivoting the rows and columns DO NOT MATCH with the original matrix A.
	 */

	private Matrix L = null;

	/**
	 * The matrix used to reverse the pivot-transformations used in decompose() to construct L (PTR
	 * = Pivot Transform Reverse):
	 * 
	 * PTR * (L * L') * PTR' = A + E.
	 * 
	 * It's constructed from the reverse sequence of row/column swaps recorded in decompose().
	 */

	private Matrix PTR = null;

	/**
	 * Data structures needed for the recording of the pivot transformations.
	 */
	private static class PivotTransform {

		int pos1;
		int pos2;

		PivotTransform(int pos1, int pos2) {
			this.pos1 = pos1;
			this.pos2 = pos2;
		}
	}

	private LinkedList<PivotTransform> pivotTransformQueue = new LinkedList<PivotTransform>();

	private Matrix E = null;			// The error-matrix E (a diagonal matrix) (with pivoting reversed)
	private double[] E_Diagonal = null;			// The diagonal elements of E (with pivoting still applied)
	private double ENorm = 0;			// Infinity norm (= maximum of diagonals) of E

	private double detL = 0;			// The determinant of L

	private int n = 0;			// Size of A

	/**
	 * Constructors.
	 * 
	 * @param A
	 */

	public SECholeskyDecomposition(double[][] A) {
		this.decompose(new Matrix(A));
	}

	public SECholeskyDecomposition(Matrix A) {
		this.decompose(A);
	}

	/**
	 * Swap rows i<->j and columns i<->j.
	 * 
	 * @param A
	 * @param i
	 * @param j
	 */

	private void swapRowsAndColumns(double[][] A, int i, int j, boolean isSymmetric, int offset) {

		double tmp;
		int k;

		i += offset;
		j += offset;

		if (!isSymmetric) {

			/** Matrix is n x n standard (the easy case). */

			double[] tr;

			tr = A[i];								// Swap rows of A
			A[i] = A[j];
			A[j] = tr;

			for (k = offset; k < n; k++) {				// Swap columns of A
				tmp = A[k][i];
				A[k][i] = A[k][j];
				A[k][j] = tmp;
			}

		} else {

			/** Matrix is symmetric, only the lower triangle is used */

			int t;

			double[] ti = new double[n];
			double[] tj = new double[n];

			if (i > j) {								// Make i < j
				t = i;
				i = j;
				j = t;
			}

			for (k = offset; k < i; k++) {				// Save i-th row / column
				ti[k] = A[i][k];
			}

			for (k = i; k < n; k++) {
				ti[k] = A[k][i];
			}

			tmp = ti[i];							// simulate row / column swap
			ti[i] = ti[j];
			ti[j] = tmp;

			for (k = offset; k < j; k++) {				// safe j-th row / column
				tj[k] = A[j][k];
			}

			for (k = j; k < n; k++) {
				tj[k] = A[k][j];
			}

			tmp = tj[i];							// simulate row / column swap
			tj[i] = tj[j];
			tj[j] = tmp;

			for (k = offset; k < i; k++) {				// Set i-th row (complete), j-th row (first part)
				A[i][k] = tj[k];
				A[j][k] = ti[k];
			}

			for (k = i; k < j; k++) {					// Set i-th column (first part), j-th row (second & final
										// part)
				A[k][i] = tj[k];
				A[j][k] = ti[k];
			}

			for (k = j; k < n; k++) {					// Set i-th column (second & final part), j-th column
										// (complete)
				A[k][i] = tj[k];
				A[k][j] = ti[k];
			}
		}
	}

	/**
	 * Do the hard work.
	 * 
	 * @param MA_orig
	 */

	private void decompose(Matrix MA_orig) {

		double mach_eps = 2.23e-16;							// Machine precision
		double tau = Math.pow(mach_eps, 1.0 / 3.0);	// Cubic root of macheps
		double tau_mod = Math.pow(mach_eps, 2.0 / 3.0);
		double mu = 0.1;								// Part of the relaxation introduced in revised SE90 that allows
		// small negative diagonal elements in next submatrix.
		// \mu \in [0,1]

		n = MA_orig.getRowDimension();
		E_Diagonal = new double[n];

		Matrix MA = (Matrix) MA_orig.clone();			// Don't override the original Matrix
		Matrix ML = new Matrix(n, n, 0);				// L matrix factor

		double[][] A = MA.getArray();
		double[][] L = ML.getArray();

		double delta_prev = 0;								// Previous diagonal element of E
		double delta = 0;								// Current delta
		boolean phaseone = true;								// We start in phase one

		int i, j, k;

		j = 0;													// Current iteration

		/** Do some sanity checks on A. */

		if (n != MA.getColumnDimension()) {
			// TODO: ERROR: Matrix not square
		}

		/** Determine the maximum magnitude of the diagonal elements of A. */

		double gamma = 0;

		for (i = 0; i < n; i++) {
			if (Math.abs(A[i][i]) > gamma) {
				gamma = Math.abs(A[i][i]);
			}
		}

		/** PHASE ONE, matrix A potentially positive definite */

		while (j < n && phaseone) {

			/**
			 * Find the pivot element for current submatrix and the minimum of the diagonal
			 * elements.
			 */

			int pivot = j;									// Pivot index
			double pivot_v = Double.NEGATIVE_INFINITY;				// Pivot value

			double min = Double.POSITIVE_INFINITY;

			for (i = j; i < n; i++) {

				if (A[i][i] > pivot_v) {
					pivot_v = A[i][i];
					pivot = i;
				}

				if (A[i][i] < min) {
					min = A[i][i];
				}
			}

			/**
			 * Check for end of phase one, continue if - pivot numerically > 0 - no large negative
			 * eigenvalue expected in the next submatrix
			 */

			if (pivot_v < tau_mod * gamma || min < -mu * pivot_v) {
				phaseone = false;
				break;
			}

			/** Excecute the pivoting. */

			if (pivot != j) {

				swapRowsAndColumns(A, 0, pivot - j, true, j);
				swapRowsAndColumns(L, j, pivot, false, 0);

				pivotTransformQueue.add(new PivotTransform(pivot, j));
			}

			/**
			 * Do a lookahead to check if the diagonal elements of the next submatrix become too
			 * negative.
			 */

			min = Double.POSITIVE_INFINITY;
			double value;

			for (i = j + 1; i < n; i++) {

				value = A[i][i] - A[i][j] * A[i][j] / A[j][j];
				if (value < min) {
					min = value;
				}
			}

			if (min < -mu * gamma) {
				phaseone = false;
				break;
			}

			/** Perform a iteration of standard cholesky factorization. */

			E_Diagonal[j] = 0;									// Current delta is 0

			L[j][j] = Math.sqrt(A[j][j]);						// Calculate the j-th diagonal element of L
			for (i = j + 1; i < n; i++) {							// Calculate the rest of jth-column (below the diagonal)
											// of L
				L[i][j] = A[i][j] / L[j][j];
				for (k = j + 1; k <= i; k++) {					// Update the i-th row of the next submatrix (in A)
					A[i][k] = A[i][k] - L[i][j] * L[k][j];
				}
			}

			j++;
		}

		/** PHASE TWO, matrix A NOT positive definite. */

		if (!phaseone && j == n - 1) {

			/** Special case: last diagonal element negative. */

			delta = -A[j][j] + Math.max(tau * -A[j][j] / (1.0 - tau), tau_mod * gamma);
			A[j][j] += delta;
			L[j][j] = Math.sqrt(A[j][j]);
			E_Diagonal[j] = delta;

		}

		if (!phaseone && j < n - 1) {

			k = j;												// k + 1 == number of iteration performed in phase one

			/** Calculate lower Gerschgorin bounds of A_k. */

			double[] g = new double[n - k];

			for (i = k; i < n; i++) {

				double sum_l = 0, sum_r = 0;
				int l;

				for (l = k; l < i; l++) {						// Left side of the diagonal element
					sum_l += Math.abs(A[i][l]);
				}
				for (l = i + 1; l < n; l++) {						// Right side of the diagonal element (using symmetry)
					sum_r += Math.abs(A[l][i]);
				}
				g[i - k] = A[i][i] - sum_l - sum_r;
			}

			/** The main iteration of PHASE TWO. */

			for (j = k; j < n - 2; j++) {

				/** Pivot on maximum lower Gerschgorin bound. */

				int gmax = j;
				double gmax_v = Double.NEGATIVE_INFINITY;

				for (i = j; i < n; i++) {
					if (g[i - k] > gmax_v) {
						gmax_v = g[i - k];
						gmax = i;
					}
				}

				/** Excecute the pivoting. */

				if (gmax != j) {

					swapRowsAndColumns(A, 0, gmax - j, true, j);
					swapRowsAndColumns(L, j, gmax, false, 0);

					pivotTransformQueue.add(new PivotTransform(gmax, j));
				}

				/** Calculate the delta to be added to the diagnoal element (= E_jj) */

				double normj = 0;
				for (i = j + 1; i < n; i++) {
					normj += Math.abs(A[i][j]);
				}
				delta = Math.max(0, Math.max(-A[j][j] + Math.max(normj, tau_mod * gamma), delta_prev));
				if (delta > 0) {
					A[j][j] += delta;
					delta_prev = delta;
				}

				E_Diagonal[j] = delta;

				/**
				 * Update Gerschgorin lower bounds, not too sure this is correct, but hey, no risk,
				 * no fun.
				 */
				/*
				 * Shevek notes that this uses floating point equality and therefore the risk is
				 * higher than might have been assumed.
				 */
				if (Tools.isNotEqual(A[j][j], normj)) {
					double t = 1.0 - normj / A[j][j];
					for (i = j + 1; i < n; i++) {
						g[i - k] += t * Math.abs(A[i][j]);
					}
				}

				/** Perform a iteration of standard cholesky factorization. */

				L[j][j] = Math.sqrt(A[j][j]);						// Calculate the j-th diagonal element of L
				for (i = j + 1; i < n; i++) {							// Calculate the rest of jth-column (below the
												// diagonal) of L
					L[i][j] = A[i][j] / L[j][j];
					for (int l = j + 1; l <= i; l++) {					// Update the i-th row of the next submatrix
														// (in A)
						A[i][l] = A[i][l] - L[i][j] * L[l][j];
					}
				}

			}

			/** Process the final 2 x 2 submatrix. */

			double[][] S = new double[2][2];

			S[0][0] = A[n - 2][n - 2];
			S[1][1] = A[n - 1][n - 1];
			S[1][0] = S[0][1] = A[n - 1][n - 2];

			Matrix MS = new Matrix(S);
			double[] evs = MS.eig().getRealEigenvalues();

			double lambda_lo = evs[0];
			double lambda_hi = evs[1];

			delta = Math.max(0, Math.max(
					-lambda_lo + Math.max(tau * (lambda_hi - lambda_lo) / (1.0 - tau), tau_mod * gamma), delta_prev));

			if (delta > 0) {
				A[n - 2][n - 2] += delta;
				A[n - 1][n - 1] += delta;
				delta_prev = delta;
			}

			L[n - 2][n - 2] = Math.sqrt(A[n - 2][n - 2]);
			L[n - 1][n - 2] = A[n - 1][n - 2] / L[n - 2][n - 2];
			L[n - 1][n - 1] = Math.sqrt(A[n - 1][n - 1] - L[n - 1][n - 2] * L[n - 1][n - 2]);

			E_Diagonal[n - 2] = E_Diagonal[n - 1] = delta;
		}

		this.L = ML;

		/** Because delta_i+1 >= delta_i, delta_prev == ||E||_inf */
		this.ENorm = delta_prev;
	}

	/**
	 * Build the Pivot-Transform-Reverse matrix PTR.
	 * 
	 * We start with a n x n identity matrix and for each recorded pivot transformation swap the
	 * according rows, reversing the sequence and starting with the last pivot transformation.
	 */

	private void buildPTR() {

		double[] temp_row; // = new double[n];
		double[][] PTRA;

		int k;
		PivotTransform pt;

		PTR = Matrix.identity(n, n);
		PTRA = PTR.getArray();

		k = pivotTransformQueue.size();

		while (k-- > 0) {
			pt = pivotTransformQueue.removeLast();

			temp_row = PTRA[pt.pos1];
			PTRA[pt.pos1] = PTRA[pt.pos2];
			PTRA[pt.pos2] = temp_row;
		}
	}

	/**
	 * Return the lower triangle matrix factor of the cholesky decomposition.
	 */

	public Matrix getL() {
		return L;
	}

	/**
	 * Return the matrix PTR with PTR * (L * L') * PTR' = A + E.
	 */

	public Matrix getPTR() {
		if (PTR == null) {
			buildPTR();
		}
		return PTR;
	}

	/**
	 * Return the diagonal of error-matrix E with the pivoting still applied.
	 */
	public double[] getE_Diagonal() {
		return E_Diagonal;
	}

	/**
	 * Return the error-matrix E with pivoting reversed.
	 */

	public Matrix getE() {

		if (E == null) {

			/* Create n x 1 vector with the diagonal elements of E */
			Matrix diagVector = new Matrix(E_Diagonal.clone(), n);

			/* Use PTR matrix to reverse the pivot transformations on this vector */
			if (PTR == null) {
				buildPTR();
			}
			diagVector = PTR.times(diagVector);

			E = new Matrix(n, n, 0.0);

			/*
			 * Because E is a diagonal matrix we can now easily build it from the pivot adjusted
			 * vector
			 */
			for (int i = 0; i < n; i++) {
				E.set(i, i, diagVector.get(i, 0));
			}
		}

		return this.E;
	}

	/**
	 * Return the infinity norm of matrix E.
	 */

	public double getENorm() {
		return this.ENorm;
	}

	/**
	 * Return the determinant of L.
	 */

	public double getDetL() {

		if (detL == 0) {

			double det = 1;
			for (int i = 0; i < L.getRowDimension(); i++) {
				det *= L.get(i, i);
			}

			detL = det;
		}

		return detL;
	}

	/**
	 * Return the determinant of A.
	 */

	public double getDetA() {
		return (getDetL() * getDetL());
	}
}
