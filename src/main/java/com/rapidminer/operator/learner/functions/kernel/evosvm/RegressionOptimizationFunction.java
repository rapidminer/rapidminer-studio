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
package com.rapidminer.operator.learner.functions.kernel.evosvm;

import com.rapidminer.tools.math.kernels.Kernel;


/**
 * This function must be maximized for the search for an optimal hyperplane for regression.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class RegressionOptimizationFunction implements OptimizationFunction {

	private double epsilon;

	public RegressionOptimizationFunction(double epsilon) {
		this.epsilon = epsilon;
	}

	@Override
	public double[] getFitness(double[] alphas, double[] ys, Kernel kernel) {
		int offset = ys.length;
		double matrixSum = 0.0d;
		for (int i = 0; i < ys.length; i++) {
			for (int j = 0; j < ys.length; j++) {
				matrixSum += (alphas[i] - alphas[i + offset]) * (alphas[j] - alphas[j + offset]) * kernel.getDistance(i, j);
			}
		}

		double alphaSum = 0.0d;
		for (int i = 0; i < ys.length; i++) {
			alphaSum += (alphas[i] + alphas[i + offset]);
		}

		double labelSum = 0.0d;
		for (int i = 0; i < ys.length; i++) {
			labelSum += ys[i] * (alphas[i] - alphas[i + offset]);
		}

		return new double[] { ((-0.5d * matrixSum) - (epsilon * alphaSum) + labelSum), 0.0d };
	}
}
