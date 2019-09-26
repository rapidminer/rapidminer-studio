/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.learner.tree.criterions;

import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.learner.tree.ColumnExampleTable;
import com.rapidminer.tools.math.MathFunctions;


/**
 * Calculates the sum of squares of differences between label value and average with respect to split. Can only be used
 * for numerical label. Does allow incremental calculation in contrast to {@link LeastSquareColumnCriterion} by using
 * that {@code totalResidual - residual} in the calculation of the {@link LeastSquareColumnCriterion} both terms contain
 * the squared sums of all label values which cancel each other out.
 *
 * @author Gisa Meier
 * @since 9.4.1
 */
public class LeastSquareDistributionColumnCriterion extends LeastSquareColumnCriterion {

	private double minimalGain = 0d;

	/**
	 * Called via reflection by {@link AbstractColumnCriterion#createColumnCriterion}.
	 */
	public LeastSquareDistributionColumnCriterion() {
	}

	public LeastSquareDistributionColumnCriterion(double minimalGain) {
		setMinimalGain(minimalGain);
	}

	@Override
	public void setMinimalGain(double minimalGain) {
		this.minimalGain = minimalGain;
	}

	@Override
	public double getNominalBenefit(ColumnExampleTable columnTable, int[] selection, int attributeNumber) {
		byte[] attributeValues = columnTable.getNominalAttributeColumn(attributeNumber);
		NominalMapping mapping = columnTable.getNominalAttribute(attributeNumber).getMapping();
		// maximal as many values as size of the mapping and one more for potential NaNs
		int classes = mapping.size() + 1;
		double[] sums = new double[classes];
		double[] counts = new double[classes];
		double[] labelValues = calculateSumsAndCounts(columnTable, selection, attributeValues,
				columnTable.getWeightColumn(), sums, counts);
		double totalAverage = getTotalSum(sums);
		double totalCount = getTotalSum(counts);
		if (totalCount > 0) {
			totalAverage /= totalCount;
		}

		double totalResidual = 0;
		for (int selected : selection) {
			totalResidual += MathFunctions.square(labelValues[selected] - totalAverage);
		}
		if (totalResidual == 0) {
			return 0;
		}

		double difference = -totalCount * totalAverage * totalAverage;
		for (int i = 0; i < classes; i++) {
			double sum = sums[i];
			double average = sum;
			double count = counts[i];
			if (count > 0) {
				average /= count;
			}
			difference += sum * average;
		}

		// normalize to be consistent with other benefits
		if (difference / totalResidual < minimalGain) {
			return 0;
		}
		return (difference) / (totalResidual * selection.length);
	}

	@Override
	protected double calculateNumericalBenefit(int[] selection, double splitValue, double[] numericalAttributeColumn, double[] label, double[] sums, double[] averages) {
		double totalResidual = 0;
		for (int selected : selection) {
			totalResidual += MathFunctions.square(label[selected] - averages[3]);
		}
		if (totalResidual == 0) {
			return 0;
		}

		double residualDifference = getResidualDifference(sums[0], averages[0], sums[1], averages[1],
				sums[2], averages[2], sums[3], averages[3]);
		// normalize to be consistent with other benefits
		if (residualDifference / totalResidual < minimalGain) {
			return 0;
		}
		return residualDifference / (totalResidual * selection.length);
	}


	@Override
	public boolean supportsIncrementalCalculation() {
		return true;
	}

	@Override
	public LeastSquareDistribution startIncrementalCalculation(ColumnExampleTable columnTable,
																		 int[] selection, int attributeNumber) {
		return new LeastSquareDistribution(columnTable, selection, attributeNumber);
	}

	@Override
	public void updateWeightDistribution(ColumnExampleTable columnTable, int row,
											 WeightDistribution distribution) {
		double weight = 1;
		if (columnTable.getWeight() != null) {
			weight = columnTable.getWeightColumn()[row];
		}
		double label = columnTable.getNumericalLabelColumn()[row];
		((LeastSquareDistribution) distribution).increment(label, weight);
	}

	@Override
	public double getIncrementalBenefit(WeightDistribution distribution) {
		LeastSquareDistribution lsDistribution = (LeastSquareDistribution) distribution;
		double totalResidual = lsDistribution.getTotalResidual();

		double difference = lsDistribution.getResidualDifference();
		if (totalResidual == 0 || (difference) / totalResidual < minimalGain) {
			return 0;
		}
		return (difference) / (totalResidual * lsDistribution.getLength());
	}

	/**
	 * Calculates the difference of the residuals. Most terms cancel and the calculation can be shortened.
	 */
	static double getResidualDifference(double firstSum, double firstAverage, double secondSum, double secondAverage,
										double missingSum, double missingAverage, double totalSum, double totalAverage) {
		// totalResidual - residual
		// = sum_t (l - totalAverage)^2 - sum_f (l - firstAverage)^2 - sum_s (l - secondAverage)^2 - sum_m (l - missingAverage)^2
		// = (sum_t l^2 - sum_f l^2 - sum_s l^2 - sum_m l^2) - 2* totalSum * totalAverage + totalCount * totalAverage^2
		//   + 2 * firstSum * firstAverage - firstCount * firstAverage^2 + 2 * secondSum * secondAverage
		//   - secondCount * secondAverage^2  + 2 * missingSum * missingAverage - missingCount * missingAverage^2
		// = - 2 * totalSum * totalAverage + totalSum * totalAverage + 2 * firstSum * firstAverage - firstSum * firstAverage
		//   + 2 * secondSum * secondAverage - secondSum * secondAverage + 2 * missingSum * missingAverage - missingSum * missingAverage
		return -totalSum * totalAverage + firstSum * firstAverage + secondSum * secondAverage + missingSum * missingAverage;
	}

}