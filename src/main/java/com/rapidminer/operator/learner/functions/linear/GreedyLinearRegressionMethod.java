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

import java.util.Collections;
import java.util.List;


/**
 * This class implements an internal forward selection for the linear regression. It uses the Akaike
 * Criterion that is maximized roundwise. Each round the attribute minimizing the akaike criterion
 * is deselected.
 *
 * @author Sebastian Land
 */
public class GreedyLinearRegressionMethod implements LinearRegressionMethod {

	@Override
	public LinearRegressionResult applyMethod(LinearRegression regression, boolean useBias, double ridge,
			ExampleSet exampleSet, boolean[] isUsedAttribute, int numberOfExamples, int numberOfUsedAttributes,
			double[] means, double labelMean, double[] standardDeviations, double labelStandardDeviation,
			double[] coefficientsOnFullData, double errorOnFullData) throws UndefinedParameterError, ProcessStoppedException {
		LinearRegressionResult greedyResult = new LinearRegressionResult();
		greedyResult.isUsedAttribute = isUsedAttribute;
		greedyResult.coefficients = coefficientsOnFullData;
		greedyResult.error = errorOnFullData;

		boolean improved = true;
		double akaike = (numberOfExamples - numberOfUsedAttributes) + 2 * numberOfUsedAttributes;
		int currentNumberOfAttributes = numberOfUsedAttributes;

		// loop as long as improvements are found and deselect one attribute each time
		while (improved) {
			boolean[] currentlySelected = isUsedAttribute.clone();
			improved = false;
			currentNumberOfAttributes--;
			// for all remaining attributes: test if are best selection
			for (int i = 0; i < isUsedAttribute.length; i++) {
				if (currentlySelected[i]) {
					// calculate the akaike value without this attribute
					currentlySelected[i] = false;
					double[] currentCoeffs = regression.performRegression(exampleSet, currentlySelected, means, labelMean,
							ridge, useBias);
					double currentError = regression.getSquaredError(exampleSet, currentlySelected, currentCoeffs, useBias);
					double currentAkaike = currentError / errorOnFullData * (numberOfExamples - numberOfUsedAttributes)
							+ 2 * currentNumberOfAttributes;

					// if the value is improved compared to the current best
					if (currentAkaike < akaike) {
						improved = true;
						akaike = currentAkaike;
						System.arraycopy(currentlySelected, 0, greedyResult.isUsedAttribute, 0, currentlySelected.length);
						greedyResult.coefficients = currentCoeffs;
						greedyResult.error = currentError;
					}

					// select it again for calculating other attributes
					currentlySelected[i] = true;
				}
			}
		}
		return greedyResult;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		return Collections.emptyList();
	}
}
