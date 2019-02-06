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
package com.rapidminer.operator.learner.tree.criterions;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.learner.tree.FrequencyCalculator;


/**
 * Calculates the accuracies for the given split if the children predict the majority classes.
 *
 * @author Ingo Mierswa
 */
public class AccuracyCriterion extends AbstractCriterion {

	private FrequencyCalculator frequencyCalculator = new FrequencyCalculator();

	@Override
	public double getNominalBenefit(ExampleSet exampleSet, Attribute attribute) {
		double[][] weightCounts = frequencyCalculator.getNominalWeightCounts(exampleSet, attribute);
		return getBenefit(weightCounts);
	}

	@Override
	public double getNumericalBenefit(ExampleSet exampleSet, Attribute attribute, double splitValue) {
		double[][] weightCounts = frequencyCalculator.getNumericalWeightCounts(exampleSet, attribute, splitValue);
		return getBenefit(weightCounts);
	}

	@Override
	public double getBenefit(double[][] weightCounts) {
		double sum = 0.0d;
		for (int v = 0; v < weightCounts.length; v++) {
			int maxIndex = -1;
			double maxValue = Double.NEGATIVE_INFINITY;
			double currentSum = 0.0d;
			for (int l = 0; l < weightCounts[v].length; l++) {
				if (weightCounts[v][l] > maxValue) {
					maxIndex = l;
					maxValue = weightCounts[v][l];
				}
				currentSum += weightCounts[v][l];
			}
			sum += weightCounts[v][maxIndex] / currentSum;
		}
		return sum;
	}

	@Override
	public boolean supportsIncrementalCalculation() {
		return true;
	}

	@Override
	public double getIncrementalBenefit() {
		int maxIndex = -1;
		double maxValue = Double.NEGATIVE_INFINITY;
		double currentSum = 0.0d;
		for (int j = 0; j < leftLabelWeights.length; j++) {
			if (leftLabelWeights[j] > maxValue) {
				maxIndex = j;
				maxValue = leftLabelWeights[j];
			}
			currentSum += leftLabelWeights[j];
		}
		double sum = leftLabelWeights[maxIndex] / currentSum;
		maxIndex = -1;
		maxValue = Double.NEGATIVE_INFINITY;
		currentSum = 0.0d;
		for (int j = 0; j < rightLabelWeights.length; j++) {
			if (rightLabelWeights[j] > maxValue) {
				maxIndex = j;
				maxValue = rightLabelWeights[j];
			}
			currentSum += rightLabelWeights[j];
		}
		return sum + rightLabelWeights[maxIndex] / currentSum;
	}
}
