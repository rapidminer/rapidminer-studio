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
 * Calculates the Gini index for the given split.
 *
 * @author Ingo Mierswa
 */
public class GiniIndexCriterion extends AbstractCriterion {

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
		// calculate information amount WITHOUT this attribute
		double[] classWeights = new double[weightCounts[0].length];
		for (int l = 0; l < classWeights.length; l++) {
			for (int v = 0; v < weightCounts.length; v++) {
				classWeights[l] += weightCounts[v][l];
			}
		}

		double totalClassWeight = frequencyCalculator.getTotalWeight(classWeights);

		double totalEntropy = getGiniIndex(classWeights, totalClassWeight);

		double gain = 0;
		for (int v = 0; v < weightCounts.length; v++) {
			double[] partitionWeights = weightCounts[v];
			double partitionWeight = frequencyCalculator.getTotalWeight(partitionWeights);
			gain += getGiniIndex(partitionWeights, partitionWeight) * partitionWeight / totalClassWeight;
		}
		return totalEntropy - gain;
	}

	private double getGiniIndex(double[] labelWeights, double totalWeight) {
		double sum = 0.0d;
		for (int i = 0; i < labelWeights.length; i++) {
			double frequency = labelWeights[i] / totalWeight;
			sum += frequency * frequency;
		}
		return 1.0d - sum;
	}

	@Override
	public boolean supportsIncrementalCalculation() {
		return true;
	}

	@Override
	public double getIncrementalBenefit() {
		double totalGiniEntropy = getGiniIndex(totalLabelWeights, totalWeight);
		double gain = getGiniIndex(leftLabelWeights, leftWeight) * leftWeight / totalWeight;
		gain += getGiniIndex(rightLabelWeights, rightWeight) * rightWeight / totalWeight;
		return totalGiniEntropy - gain;
	}
}
