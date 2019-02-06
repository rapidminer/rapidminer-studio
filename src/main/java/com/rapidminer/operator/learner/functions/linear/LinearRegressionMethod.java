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
package com.rapidminer.operator.learner.functions.linear;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;

import java.util.List;


/**
 * This interface is for all classes that implement an integrated attribute selection algorithm for
 * the {@link LinearRegression} operator.
 * 
 * All subclasses need to have an empty constructor for being built by reflection.
 * 
 * @author Sebastian Land
 */
public interface LinearRegressionMethod {

	public static class LinearRegressionResult {

		public double[] coefficients;
		public double error;
		public boolean[] isUsedAttribute;
	}

	/**
	 * This method performs the actual regression. There are passed the linear regression operator
	 * itself as well as data and it's properties. Before this method is called, the linear
	 * regression already has performed a regression on the full data set. This resulted in the
	 * given coefficients. Please note, that if useBias is true, the last coefficient is the bias.
	 * 
	 * @throws UndefinedParameterError
	 * @throws ProcessStoppedException
	 */
	public LinearRegressionResult applyMethod(LinearRegression regression, boolean useBias, double ridge,
			ExampleSet exampleSet, boolean[] isUsedAttribute, int numberOfExamples, int numberOfUsedAttributes,
			double[] means, double labelMean, double[] standardDeviations, double labelStandardDeviation,
			double[] coefficientsOnFullData, double errorOnFullData) throws UndefinedParameterError, ProcessStoppedException;

	/**
	 * This method must return a List of needed Parameters.
	 */
	public List<ParameterType> getParameterTypes();

}
