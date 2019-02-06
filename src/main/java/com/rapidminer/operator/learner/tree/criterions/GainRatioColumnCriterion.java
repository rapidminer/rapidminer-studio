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



/**
 * The gain ratio divides the information gain by the prior split info in order to prevent id-like
 * attributes to be selected as the best.
 *
 * @author Sebastian Land, Ingo Mierswa, Gisa Schaefer
 */
public class GainRatioColumnCriterion extends InfoGainColumnCriterion {

	private static double LOG_FACTOR = 1d / Math.log(2);

	public GainRatioColumnCriterion() {}

	public GainRatioColumnCriterion(double minimalGain) {
		super(minimalGain);
	}

	@Override
	public double getBenefit(double[][] weightCounts) {
		double gain = super.getBenefit(weightCounts, false);

		double splitInfo = getSplitInfo(weightCounts);

		if (splitInfo == 0) { // if splitInfo is zero then gain is either zero or - infinity
			return gain;
		} else {
			double ratio = gain / splitInfo;
			if (ratio < minimalGain) {
				ratio = 0;
			}
			return ratio;
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
	public double getIncrementalBenefit(WeightDistribution distribution) {
		double gain = getEntropy(distribution.getTotalLabelWeigths(), distribution.getTotalWeigth());
		gain -= getEntropy(distribution.getLeftLabelWeigths(), distribution.getLeftWeigth()) * distribution.getLeftWeigth()
				/ distribution.getTotalWeigth();
		gain -= getEntropy(distribution.getRightLabelWeigths(), distribution.getRightWeigth())
				* distribution.getRightWeigth() / distribution.getTotalWeigth();
		double splitInfo;
		if (distribution.hasMissingValues()) {
			gain -= getEntropy(distribution.getMissingsLabelWeigths(), distribution.getMissingsWeigth())
					* distribution.getMissingsWeigth() / distribution.getTotalWeigth();
			splitInfo = getSplitInfo(new double[] { distribution.getLeftWeigth(), distribution.getRightWeigth(),
					distribution.getMissingsWeigth() }, distribution.getTotalWeigth());
		} else {
			splitInfo = getSplitInfo(new double[] { distribution.getLeftWeigth(), distribution.getRightWeigth() },
					distribution.getTotalWeigth());
		}
		if (splitInfo == 0) {
			return gain;
		} else {
			if (gain / splitInfo < minimalGain) {
				return 0;
			}
			return gain / splitInfo;
		}
	}
}
