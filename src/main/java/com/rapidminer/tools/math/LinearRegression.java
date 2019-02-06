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
package com.rapidminer.tools.math;

import java.util.logging.Logger;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import com.rapidminer.Process;

import Jama.Matrix;


/**
 * This class can be used to calculate the coefficients of a (weighted) linear regression. It uses
 * the class Matrix from the Jama package for most purposes and the class RealMatrix from Apache for
 * matrix multiplication because it is faster. It is also possible to apply Ridge Regression which
 * is a sort of regularization well suited especially for ill-posed problems. Please note that for
 * the dependent matrix Y only one column is allowed.
 *
 * @author Ingo Mierswa
 */
public class LinearRegression {

	private static Logger logger = Logger.getLogger(Process.class.getName());

	/** Performs a weighted linear ridge regression. */
	public static double[] performRegression(Matrix x, Matrix y, double[] weights, double ridge) {
		Matrix weightedIndependent = new Matrix(x.getRowDimension(), x.getColumnDimension());
		Matrix weightedDependent = new Matrix(x.getRowDimension(), 1);
		for (int i = 0; i < weights.length; i++) {
			double sqrtWeight = Math.sqrt(weights[i]);
			for (int j = 0; j < x.getColumnDimension(); j++) {
				weightedIndependent.set(i, j, x.get(i, j) * sqrtWeight);
			}
			weightedDependent.set(i, 0, y.get(i, 0) * sqrtWeight);
		}
		return performRegression(weightedIndependent, weightedDependent, ridge);
	}

	/** Calculates the coefficients of linear ridge regression. */
	public static double[] performRegression(Matrix a, Matrix b, double ridge) {
		RealMatrix x = MatrixUtils.createRealMatrix(a.getArray());
		RealMatrix y = MatrixUtils.createRealMatrix(b.getArray());
		int numberOfColumns = x.getColumnDimension();
		double[] coefficients = new double[numberOfColumns];
		RealMatrix xTransposed = x.transpose();
		Matrix result;
		boolean finished = false;
		while (!finished) {
			RealMatrix xTx = xTransposed.multiply(x);

			for (int i = 0; i < numberOfColumns; i++) {
				xTx.addToEntry(i, i, ridge);
			}

			RealMatrix xTy = xTransposed.multiply(y);
			coefficients = xTy.getColumn(0);

			try {
				// do not use Apache LUDecomposition for solve instead because it creates different
				// results
				result = new Matrix(xTx.getData()).solve(new Matrix(coefficients, coefficients.length));
				for (int i = 0; i < numberOfColumns; i++) {
					coefficients[i] = result.get(i, 0);
				}
				finished = true;
			} catch (Exception ex) {
				double ridgeOld = ridge;
				if (ridge > 0) {
					ridge *= 10;
				} else {
					ridge = 0.0000001;
				}
				finished = false;
				logger.warning("Error during calculation: " + ex.getMessage() + ": Increasing ridge factor from " + ridgeOld
						+ " to " + ridge);
			}
		}
		return coefficients;
	}

}
