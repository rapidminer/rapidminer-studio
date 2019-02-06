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
package com.rapidminer.operator.performance;

/**
 * The root-mean-squared error. Mean-squared error is the most commonly used measure of success of
 * numeric prediction, and root mean-squared error is the square root of mean-squared-error, take to
 * give it the same dimensions as the predicted values themselves. This method exaggerates the
 * prediction error - the difference between prediction value and actual value of a test case - of
 * test cases in which the prediction error is larger than the others. If this number is
 * significantly greater than the mean absolute error, it means that there are test cases in which
 * the prediction error is significantly greater than the average prediction error.
 * 
 * @author Ingo Mierswa, Simon Fischer Exp $
 */
public class RootMeanSquaredError extends SimpleCriterion {

	private static final long serialVersionUID = -4425511584684855855L;

	public RootMeanSquaredError() {}

	public RootMeanSquaredError(RootMeanSquaredError sc) {
		super(sc);
	}

	@Override
	public String getName() {
		return "root_mean_squared_error";
	}

	/** Calculates the error for the current example. */
	@Override
	public double countExample(double label, double predictedLabel) {
		double dif = label - predictedLabel;
		return dif * dif;
	}

	/** Applies a square root to the given value. */
	@Override
	public double transform(double value) {
		return Math.sqrt(value);
	}

	@Override
	public String getDescription() {
		return "Averaged root-mean-squared error";
	}
}
