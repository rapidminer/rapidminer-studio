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

import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.learner.tree.ColumnExampleTable;
import com.rapidminer.operator.learner.tree.MinimalGainHandler;


/**
 * Calculates the sum of squares of differences between label value and average with respect to split. Can only be used
 * for numerical label. Does not allow incremental calculation.
 *
 * @author Philipp Schlunder, Gisa Meier
 * @since 8.0
 */
public class LeastSquareColumnCriterion implements MinimalGainHandler, ColumnCriterion {

	private double minimalGain = 0d;

	/**
	 * Called via reflection by {@link AbstractColumnCriterion#createColumnCriterion}.
	 */
	public LeastSquareColumnCriterion() {}

	public LeastSquareColumnCriterion(double minimalGain) {
		this.minimalGain = minimalGain;
	}

	@Override
	public double getNominalBenefit(ColumnExampleTable columnTable, int[] selection, int attributeNumber) {
		byte[] attributeValues = columnTable.getNominalAttributeColumn(attributeNumber);
		NominalMapping mapping = columnTable.getNominalAttribute(attributeNumber).getMapping();
		// maximal as many values as size of the mapping and one more for potential NaNs
		int classes = mapping.size() + 1;
		double[] averages = new double[classes];
		double[] counts = new double[classes];
		double[] labelValues = calculateSumsAndCounts(columnTable, selection, attributeValues,
				columnTable.getWeightColumn(), averages, counts);
		double totalAverage = getTotalSum(averages);
		double totalCount = 0;
		for (int i = 0; i < averages.length; i++) {
			double count = counts[i];
			if (count > 0) {
				averages[i] /= count;
			}
			totalCount += count;
		}
		if (totalCount > 0) {
			totalAverage /= totalCount;
		}
		double residual = 0;
		double totalResidual = 0;
		for (int selected : selection) {
			residual += Math.pow(labelValues[selected] - averages[attributeValues[selected]], 2);
			totalResidual += Math.pow(labelValues[selected] - totalAverage, 2);

		}
		// normalize to be consistent with other benefits
		if (totalResidual == 0 || (totalResidual - residual) / totalResidual < minimalGain) {
			return 0;
		}
		return (totalResidual - residual) / (totalResidual * selection.length);
	}


	/**
	 * Calculates the weighted sums and weighted count for all entries.
	 */
	double[] calculateSumsAndCounts(ColumnExampleTable columnTable, int[] selection, byte[] attributeValues,
									double[] weights, double[] averages, double[] counts) {
		double[] labelValues = columnTable.getNumericalLabelColumn();
		for (int selected : selection) {
			double weight = 1.0;
			if (weights != null) {
				weight = weights[selected];
			}
			averages[attributeValues[selected]] += labelValues[selected] * weight;
			counts[attributeValues[selected]] += weight;
		}
		return labelValues;
	}

	/**
	 * Calculates the sum over all entries.
	 */
	double getTotalSum(double[] sums) {
		double totalSum = 0;
		for (double sum : sums) {
			totalSum += sum;
		}
		return totalSum;
	}

	@Override
	public double getNumericalBenefit(ColumnExampleTable columnTable, int[] selection, int attributeNumber,
									  double splitValue) {
		double firstSum = 0;
		double secondSum = 0;
		double missingSum = 0;
		double firstCount = 0;
		double secondCount = 0;
		double missingCount = 0;
		double[] numericalAttributeColumn = columnTable.getNumericalAttributeColumn(attributeNumber);
		double[] label = columnTable.getNumericalLabelColumn();
		double[] weights = columnTable.getWeightColumn();
		for (int selected : selection) {
			double value = numericalAttributeColumn[selected];
			double weight = 1.0;
			if (weights != null) {
				weight = weights[selected];
			}
			if (Double.isNaN(value)) {
				missingSum += label[selected] * weight;
				missingCount += weight;
			} else if (value <= splitValue) {
				firstSum += label[selected] * weight;
				firstCount += weight;
			} else {
				secondSum += label[selected] * weight;
				secondCount += weight;
			}
		}

		double firstAverage = 0;
		if (firstCount > 0) {
			firstAverage = firstSum / firstCount;
		}
		double secondAverage = 0;
		if (secondCount > 0) {
			secondAverage = secondSum / secondCount;
		}
		double missingAverage = 0;
		if (missingCount > 0) {
			missingAverage = missingSum / missingCount;
		}
		double totalSum = firstSum + secondSum + missingSum;
		double totalAverage = totalSum / (firstCount + secondCount + missingCount);

		double[] sums = {firstSum, secondSum, missingSum, totalSum};
		double[] averages = {firstAverage, secondAverage, missingAverage, totalAverage};

		return calculateNumericalBenefit(selection, splitValue, numericalAttributeColumn, label, sums, averages);
	}

	/**
	 * Calculates the actual numerical benefit from the given parameters. Returns 0 for benefits smaller than
	 * {@link #minimalGain}.
	 *
	 * @return the numerical benefit
	 * @since 9.4.1
	 */
	protected double calculateNumericalBenefit(int[] selection, double splitValue, double[] numericalAttributeColumn, double[] label, double[] sums, double[] averages) {
		double firstResidual = 0;
		double secondResidual = 0;
		double missingResidual = 0;
		double totalResidual = 0;

		for (int selected : selection) {
			double value = numericalAttributeColumn[selected];
			if (Double.isNaN(value)) {
				missingResidual += Math.pow(label[selected] - averages[2], 2);
			} else if (value <= splitValue) {
				firstResidual += Math.pow(label[selected] - averages[0], 2);
			} else {
				secondResidual += Math.pow(label[selected] - averages[1], 2);
			}
			totalResidual += Math.pow(label[selected] - averages[3], 2);
		}

		double residual = firstResidual + secondResidual + missingResidual;
		if (totalResidual == 0 || (totalResidual - residual) / totalResidual < minimalGain) {
			return 0;
		}
		return (totalResidual - residual) / (totalResidual * selection.length);
	}


	@Override
	public boolean supportsIncrementalCalculation() {
		return false;
	}

	@Override
	public WeightDistribution startIncrementalCalculation(ColumnExampleTable columnTable, int[] selection,
														  int attributeNumber) {
		return null;
	}

	@Override
	public void updateWeightDistribution(ColumnExampleTable columnTable, int row, WeightDistribution distribution) {
		// not used, see supportsIncrementalCalculation
	}

	@Override
	public double getIncrementalBenefit(WeightDistribution distribution) {
		// not used, see supportsIncrementalCalculation
		return 0;
	}

	@Override
	public double getBenefit(double[][] weightCounts) {
		// does only make sense for nominal label
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMinimalGain(double minimalGain) {
		this.minimalGain = minimalGain;
	}

}