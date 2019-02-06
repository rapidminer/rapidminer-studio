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

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.distribution.FDistribution;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * This implements an iterative T-Test based selection. First a forward selection is run and all
 * attributes for which the null hypothesis is significantly denied are selected. On this set a
 * backward selection is performed and all attributes for which in the combination the null
 * hypothesis can't be denied are dropped. The next round then starts with the remaining attributes
 * until there's no further change or the maximal number of rounds are exceeded.
 *
 * @author Sebastian Land
 */
public class IterativeTTestLinearRegressionMethod extends TTestLinearRegressionMethod {

	public static final String PARAMETER_MAX_ITERATIONS = "max_iterations";
	public static final String PARAMETER_FORWARD_SELECTION_THRESHOLD = "forward_alpha";
	public static final String PARAMETER_BACKWARD_SELECTION_THRESHOLD = "backward_alpha";

	@Override
	public LinearRegressionResult applyMethod(LinearRegression regression, boolean useBias, double ridge,
			ExampleSet exampleSet, boolean[] isUsedAttribute, int numberOfExamples, int numberOfUsedAttributes,
			double[] means, double labelMean, double[] standardDeviations, double labelStandardDeviation,
			double[] coefficientsOnFullData, double errorOnFullData) throws UndefinedParameterError, ProcessStoppedException {
		int maxIterations = regression.getParameterAsInt(PARAMETER_MAX_ITERATIONS);
		double alphaForward = regression.getParameterAsDouble(PARAMETER_FORWARD_SELECTION_THRESHOLD);
		double alphaBackward = regression.getParameterAsDouble(PARAMETER_BACKWARD_SELECTION_THRESHOLD);

		FDistribution fdistribution;
		// check if the F-distribution can be calculated
		int secondDegreeOfFreedom = exampleSet.size() - coefficientsOnFullData.length;
		if (secondDegreeOfFreedom > 0) {
			fdistribution = new FDistribution(1, secondDegreeOfFreedom);
		} else {
			fdistribution = null;
		}

		double generalCorrelation = regression.getCorrelation(exampleSet, isUsedAttribute, coefficientsOnFullData, useBias);
		generalCorrelation *= generalCorrelation;

		// building data structures
		boolean[] isAllowedToUse = isUsedAttribute;
		// initialize array for checking for change
		boolean[] isLastRoundUsed = new boolean[isUsedAttribute.length];
		boolean[] isToUseNextRound = new boolean[isUsedAttribute.length];
		isUsedAttribute = new boolean[isUsedAttribute.length];

		// do until nothing changes or max rounds exceeded
		int iteration = 0;
		while (iteration == 0 || iteration < maxIterations && isSelectionDiffering(isUsedAttribute, isLastRoundUsed)) {
			System.arraycopy(isUsedAttribute, 0, isLastRoundUsed, 0, isUsedAttribute.length);

			// first do forward selection for all single non-selected and
			// allowed attributes
			int coefficientIndex = 0;
			for (int i = 0; i < isAllowedToUse.length; i++) {
				if (isAllowedToUse[i] && !isUsedAttribute[i]) {
					// check if this not selected one will receive significant coefficient
					isUsedAttribute[i] = true;
					double[] coefficients = regression.performRegression(exampleSet, isUsedAttribute, means, labelMean,
							ridge, useBias);
					// only if it is possible to calculate the probabilities, the p-value for this
					// attribute is checked
					if (fdistribution != null) {
						double pValue = getPValue(coefficients[coefficientIndex], i, regression, useBias, ridge, exampleSet,
								isUsedAttribute, standardDeviations, labelStandardDeviation, fdistribution,
								generalCorrelation);
						if (1.0d - pValue <= alphaForward) {
							isToUseNextRound[i] = true;
						}
					}
					isUsedAttribute[i] = false;
				} else if (isUsedAttribute[i]) {
					coefficientIndex++;
				}
			}

			// now add all that we have remembered to use
			for (int i = 0; i < isUsedAttribute.length; i++) {
				isUsedAttribute[i] |= isToUseNextRound[i];
				isToUseNextRound[i] = false;
			}

			// now we have to deselect all that do not fulfill t-test in combination
			{
				double[] coefficients = regression.performRegression(exampleSet, isUsedAttribute, means, labelMean, ridge,
						useBias);
				isUsedAttribute = filterByPValue(regression, useBias, ridge, exampleSet, isUsedAttribute, means, labelMean,
						standardDeviations, labelStandardDeviation, coefficients, alphaBackward).isUsedAttribute;
			}

			iteration++;
		}

		// calculate result
		LinearRegressionResult result = new LinearRegressionResult();
		result.isUsedAttribute = isUsedAttribute;
		result.coefficients = regression.performRegression(exampleSet, isUsedAttribute, means, labelMean, ridge, useBias);
		result.error = regression.getSquaredError(exampleSet, isUsedAttribute, result.coefficients, useBias);
		return result;
	}

	private boolean isSelectionDiffering(boolean[] isUsedAttribute, boolean[] isLastRoundUsed) {
		for (int i = 0; i < isUsedAttribute.length; i++) {
			if (isUsedAttribute[i] != isLastRoundUsed[i]) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();

		types.add(new ParameterTypeInt(PARAMETER_MAX_ITERATIONS, "The maximal number of rounds for iterative selection.", 1,
				Integer.MAX_VALUE, 10));
		types.add(new ParameterTypeDouble(PARAMETER_FORWARD_SELECTION_THRESHOLD,
				"This is the alpha level for the used t-test for selecting attributes.", 0, 1, 0.05));
		types.add(new ParameterTypeDouble(PARAMETER_BACKWARD_SELECTION_THRESHOLD,
				"This is the alpha level for the used t-test for deselecting attributes.", 0, 1, 0.05));
		return types;
	}

}
