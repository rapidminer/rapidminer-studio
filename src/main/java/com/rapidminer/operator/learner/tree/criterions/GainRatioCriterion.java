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
 * The gain ratio divides the information gain by the prior split info in order to prevent id-like
 * attributes to be selected as the best.
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class GainRatioCriterion extends InfoGainCriterion {

	private static double LOG_FACTOR = 1d / Math.log(2);

	private FrequencyCalculator frequencyCalculator = new FrequencyCalculator();

	public GainRatioCriterion() {}

	public GainRatioCriterion(double minimalGain) {
		super(minimalGain);
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
		double gain = super.getBenefit(weightCounts);

		double splitInfo = getSplitInfo(weightCounts);

		if (splitInfo == 0) {
			return gain;
		} else {
			return gain / splitInfo;
		}
	}

	protected double getSplitInfo(double[][] weightCounts) {
		double[] splitCounts = new double[weightCounts.length];
		for (int v = 0; v < weightCounts.length; v++) {
			for (int l = 0; l < weightCounts[v].length; l++) {
				splitCounts[v] += weightCounts[v][l];
			}
		}

		double totalSplitCount = 0.0d;
		for (double w : splitCounts) {
			totalSplitCount += w;
		}

		double splitInfo = 0.0d;
		for (int v = 0; v < splitCounts.length; v++) {
			if (splitCounts[v] > 0) {
				double proportion = splitCounts[v] / totalSplitCount;
				splitInfo -= Math.log(proportion) * LOG_FACTOR * proportion;
			}
		}
		return splitInfo;
	}

	protected double getSplitInfo(double[] partitionWeights, double totalWeight) {
		double splitInfo = 0;
		for (double partitionWeight : partitionWeights) {
			if (partitionWeight > 0) {
				double partitionProportion = partitionWeight / totalWeight;
				splitInfo += partitionProportion * Math.log(partitionProportion) * LOG_FACTOR;
			}
		}
		return -splitInfo;
	}

	@Override
	public boolean supportsIncrementalCalculation() {
		return true;
	}

	@Override
	public double getIncrementalBenefit() {
		double gain = getEntropy(totalLabelWeights, totalWeight);
		gain -= getEntropy(leftLabelWeights, leftWeight) * leftWeight / totalWeight;
		gain -= getEntropy(rightLabelWeights, rightWeight) * rightWeight / totalWeight;
		double splitInfo = getSplitInfo(new double[] { leftWeight, rightWeight }, totalWeight);
		if (splitInfo == 0) {
			return gain;
		} else {
			return gain / splitInfo;
		}
	}
}
