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
 * This class implements the M5Prime feature selection method for Linear Regression.
 *
 * @author Sebastian Land
 */
public class M5PLinearRegressionMethod implements LinearRegressionMethod {

	@Override
	public LinearRegressionResult applyMethod(LinearRegression regression, boolean useBias, double ridge,
			ExampleSet exampleSet, boolean[] isUsedAttribute, int numberOfExamples, int numberOfUsedAttributes,
			double[] means, double labelMean, double[] standardDeviations, double labelStandardDeviation,
			double[] coefficientsOnFullData, double errorOnFullData) throws UndefinedParameterError, ProcessStoppedException {
		LinearRegressionResult result = new LinearRegressionResult();
		result.isUsedAttribute = isUsedAttribute;
		result.coefficients = coefficientsOnFullData;
		result.error = errorOnFullData;

		// attribute removal as in M5 prime
		boolean improved = true;
		int currentNumberOfAttributes = numberOfUsedAttributes;
		double akaike = (numberOfExamples - numberOfUsedAttributes) + 2 * numberOfUsedAttributes;
		while (improved) {
			improved = false;
			currentNumberOfAttributes--;

			// find the attribute with the smallest standardized coefficient
			double minStadardizedCoefficient = 0;
			int attribute2Deselect = -1;
			int coefficientIndex = 0;
			for (int i = 0; i < isUsedAttribute.length; i++) {
				if (isUsedAttribute[i]) {
					double standardizedCoefficient = Math.abs(coefficientsOnFullData[coefficientIndex]
							* standardDeviations[i] / labelStandardDeviation);
					if ((coefficientIndex == 0) || (standardizedCoefficient < minStadardizedCoefficient)) {
						minStadardizedCoefficient = standardizedCoefficient;
						attribute2Deselect = i;
					}
					coefficientIndex++;
				}
			}

			// check if removing this attribute improves Akaike
			if (attribute2Deselect >= 0) {
				isUsedAttribute[attribute2Deselect] = false;
				double[] currentCoefficients = regression.performRegression(exampleSet, isUsedAttribute, means, labelMean,
						ridge, useBias);
				double currentError = regression.getSquaredError(exampleSet, isUsedAttribute, currentCoefficients, useBias);
				double currentAkaike = currentError / errorOnFullData * (numberOfExamples - numberOfUsedAttributes)
						+ 2 * currentNumberOfAttributes;

				if (currentAkaike < akaike) {
					improved = true;
					akaike = currentAkaike;
					result.coefficients = currentCoefficients;
					result.error = currentError;
				} else {
					isUsedAttribute[attribute2Deselect] = true;
				}
			}
		}
		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		return Collections.emptyList();
	}
}
