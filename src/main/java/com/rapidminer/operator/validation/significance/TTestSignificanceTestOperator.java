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
package com.rapidminer.operator.validation.significance;

import org.apache.commons.math3.distribution.FDistribution;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.report.Readable;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.SignificanceTestResult;


/**
 * Determines if the null hypothesis (all actual mean values are the same) holds for the input
 * performance vectors. This operator uses a simple (pairwise) t-test to determine the probability
 * that the null hypothesis is wrong. Since a t-test can only be applied on two performance vectors
 * this test will be applied to all possible pairs. The result is a significance matrix. However,
 * pairwise t-test may introduce a larger type I error. It is recommended to apply an additional
 * ANOVA test to determine if the null hypothesis is wrong at all.
 *
 * @author Ingo Mierswa
 */
public class TTestSignificanceTestOperator extends SignificanceTestOperator {

	/** The result for a paired t-test. */
	public static class TTestSignificanceTestResult extends SignificanceTestResult implements Readable {

		private static final long serialVersionUID = -5412090499056975997L;

		private final PerformanceVector[] allVectors;

		private final double[][] probMatrix;

		private double alpha = 0.05d;

		public TTestSignificanceTestResult(PerformanceVector[] allVectors, double[][] probMatrix, double alpha) {
			this.allVectors = allVectors;
			this.probMatrix = probMatrix;
			this.alpha = alpha;
		}

		@Override
		public String getName() {
			return "Pairwise t-Test";
		}

		/** Returns NaN since no single probability will be delivered. */
		@Override
		public double getProbability() {
			return Double.NaN;
		}

		@Override
		public String toString() {
			StringBuffer result = new StringBuffer();
			result.append("Probabilities for random values with the same result:" + Tools.getLineSeparator());
			for (int i = 0; i < allVectors.length; i++) {
				for (int j = 0; j < allVectors.length; j++) {
					if (!Double.isNaN(probMatrix[i][j])) {
						result.append(Tools.formatNumber(probMatrix[i][j]) + "\t");
					} else {
						result.append("-----\t");
					}
				}
				result.append(Tools.getLineSeparator());
			}
			result.append("Values smaller than alpha=" + Tools.formatNumber(alpha)
					+ " indicate a probably significant difference between the mean values!" + Tools.getLineSeparator());
			result.append("List of performance values:" + Tools.getLineSeparator());
			for (int i = 0; i < allVectors.length; i++) {
				result.append(i + ": " + Tools.formatNumber(allVectors[i].getMainCriterion().getAverage()) + " +/- "
						+ Tools.formatNumber(Math.sqrt(allVectors[i].getMainCriterion().getVariance()))
						+ Tools.getLineSeparator());
			}
			return result.toString();
		}

		@Override
		public boolean isInTargetEncoding() {
			return false;
		}

		public PerformanceVector[] getAllVectors() {
			return allVectors;
		}

		public double[][] getProbMatrix() {
			return this.probMatrix;
		}

		public double getAlpha() {
			return this.alpha;
		}
	}

	public TTestSignificanceTestOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public SignificanceTestResult performSignificanceTest(PerformanceVector[] allVectors, double alpha) {
		double[][] resultMatrix = new double[allVectors.length][allVectors.length];
		for (int i = 0; i < allVectors.length; i++) {
			for (int j = 0; j < i + 1; j++) {
				resultMatrix[i][j] = Double.NaN; // fill lower triangle with
			}
			// NaN --> empty in result
			// string
			for (int j = i + 1; j < allVectors.length; j++) {
				resultMatrix[i][j] = getProbability(allVectors[i].getMainCriterion(), allVectors[j].getMainCriterion());
			}
		}
		return new TTestSignificanceTestResult(allVectors, resultMatrix, alpha);
	}

	private double getProbability(PerformanceCriterion pc1, PerformanceCriterion pc2) {
		double totalDeviation = ((pc1.getAverageCount() - 1) * pc1.getVariance() + (pc2.getAverageCount() - 1)
				* pc2.getVariance())
				/ (pc1.getAverageCount() + pc2.getAverageCount() - 2);
		double factor = 1.0d / (1.0d / pc1.getAverageCount() + 1.0d / pc2.getAverageCount());
		double diff = pc1.getAverage() - pc2.getAverage();
		double t = factor * diff * diff / totalDeviation;
		int secondDegreeOfFreedom = pc1.getAverageCount() + pc2.getAverageCount() - 2;
		double prob;
		// make sure the F-distribution is well defined
		if (secondDegreeOfFreedom > 0) {
			FDistribution fDist = new FDistribution(1, secondDegreeOfFreedom);
			prob = 1 - fDist.cumulativeProbability(t);
		} else {
			// in this case the probability cannot calculated correctly and a 1 is returned, as
			// this result is not significant
			prob = 1;
		}

		return prob;
	}

	@Override
	public int getMinSize() {
		return 2;
	}

	@Override
	public int getMaxSize() {
		return Integer.MAX_VALUE;
	}
}
