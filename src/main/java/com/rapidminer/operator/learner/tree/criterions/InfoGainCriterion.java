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
import com.rapidminer.operator.learner.tree.MinimalGainHandler;


/**
 * This criterion implements the well known information gain in order to calculate the benefit of a
 * split. The information gain is defined as the change in entropy from a prior state to a state
 * that takes some information as given by the entropy.
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class InfoGainCriterion extends AbstractCriterion implements MinimalGainHandler {

	private static double LOG_FACTOR = 1d / Math.log(2);

	private FrequencyCalculator frequencyCalculator = new FrequencyCalculator();

	private double minimalGain = 0.1;

	public InfoGainCriterion() {}

	public InfoGainCriterion(double minimalGain) {
		this.minimalGain = minimalGain;
	}

	@Override
	public void setMinimalGain(double minimalGain) {
		this.minimalGain = minimalGain;
	}

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
		int numberOfValues = weightCounts.length;
		int numberOfLabels = weightCounts[0].length;

		// calculate entropies
		double[] entropies = new double[numberOfValues];
		double[] totalWeights = new double[numberOfValues];
		for (int v = 0; v < numberOfValues; v++) {
			for (int l = 0; l < numberOfLabels; l++) {
				totalWeights[v] += weightCounts[v][l];
			}

			for (int l = 0; l < numberOfLabels; l++) {
				if (weightCounts[v][l] > 0) {
					double proportion = weightCounts[v][l] / totalWeights[v];
					entropies[v] -= Math.log(proportion) * LOG_FACTOR * proportion;
				}
			}
		}

		// calculate information amount WITH this attribute
		double totalWeight = 0.0d;
		for (double w : totalWeights) {
			totalWeight += w;
		}

		double information = 0.0d;
		for (int v = 0; v < numberOfValues; v++) {
			information += totalWeights[v] / totalWeight * entropies[v];
		}

		// calculate information amount WITHOUT this attribute
		double[] classWeights = new double[numberOfLabels];
		for (int l = 0; l < numberOfLabels; l++) {
			for (int v = 0; v < numberOfValues; v++) {
				classWeights[l] += weightCounts[v][l];
			}
		}

		double totalClassWeight = 0.0d;
		for (double w : classWeights) {
			totalClassWeight += w;
		}

		double classEntropy = 0.0d;
		for (int l = 0; l < numberOfLabels; l++) {
			if (classWeights[l] > 0) {
				double proportion = classWeights[l] / totalClassWeight;
				classEntropy -= Math.log(proportion) * LOG_FACTOR * proportion;
			}
		}

		// calculate and return information gain
		double informationGain = classEntropy - information;
		if (informationGain < minimalGain * classEntropy) {
			informationGain = 0;
		}
		return informationGain;
	}

	protected double getEntropy(double[] labelWeights, double totalWeight) {
		double entropy = 0;
		for (int i = 0; i < labelWeights.length; i++) {
			if (labelWeights[i] > 0) {
				double proportion = labelWeights[i] / totalWeight;
				entropy -= Math.log(proportion) * LOG_FACTOR * proportion;
			}
		}
		return entropy;
	}

	@Override
	public boolean supportsIncrementalCalculation() {
		return true;
	}

	@Override
	public double getIncrementalBenefit() {
		double totalEntropy = getEntropy(totalLabelWeights, totalWeight);
		double gain = getEntropy(leftLabelWeights, leftWeight) * leftWeight / totalWeight;
		gain += getEntropy(rightLabelWeights, rightWeight) * rightWeight / totalWeight;
		return totalEntropy - gain;
	}
}
