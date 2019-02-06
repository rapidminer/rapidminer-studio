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

import com.rapidminer.operator.learner.tree.FrequencyCalculator;
import com.rapidminer.operator.learner.tree.MinimalGainHandler;


/**
 * Calculates the Gini index for the given split.
 *
 * @author Ingo Mierswa, Gisa Schaefer
 */
public class GiniIndexColumnCriterion extends AbstractColumnCriterion implements MinimalGainHandler {

	private FrequencyCalculator frequencyCalculator = new FrequencyCalculator();
	private double minimalGain = 0.1;

	public GiniIndexColumnCriterion() {}

	public GiniIndexColumnCriterion(double minimalGain) {
		this.minimalGain = minimalGain;
	}

	@Override
	public void setMinimalGain(double minimalGain) {
		this.minimalGain = minimalGain;
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

		int differentValues = 0;
		double gain = 0;
		for (int v = 0; v < weightCounts.length; v++) {
			double[] partitionWeights = weightCounts[v];
			double partitionWeight = frequencyCalculator.getTotalWeight(partitionWeights);
			if (partitionWeight > 0) {
				differentValues++;
				gain += getGiniIndex(partitionWeights, partitionWeight) * partitionWeight / totalClassWeight;
			}
		}
		// if the attribute has only one value left, discourage a split
		if (differentValues < 2) {
			return Double.NEGATIVE_INFINITY;
		}

		// check if gain is enough
		if (totalEntropy - gain < minimalGain * totalEntropy) {
			return 0;
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
	public double getIncrementalBenefit(WeightDistribution distribution) {
		double totalGiniEntropy = getGiniIndex(distribution.getTotalLabelWeigths(), distribution.getTotalWeigth());
		double gain = getGiniIndex(distribution.getLeftLabelWeigths(), distribution.getLeftWeigth())
				* distribution.getLeftWeigth() / distribution.getTotalWeigth();
		gain += getGiniIndex(distribution.getRightLabelWeigths(), distribution.getRightWeigth())
				* distribution.getRightWeigth() / distribution.getTotalWeigth();
		if (distribution.hasMissingValues()) {
			gain += getGiniIndex(distribution.getMissingsLabelWeigths(), distribution.getMissingsWeigth())
					* distribution.getMissingsWeigth() / distribution.getTotalWeigth();
		}
		// check if gain is enough
		if (totalGiniEntropy - gain < minimalGain * totalGiniEntropy) {
			return 0;
		}
		return totalGiniEntropy - gain;
	}
}
