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
package com.rapidminer.operator.learner.tree;

import com.rapidminer.operator.learner.tree.criterions.ColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.WeightDistribution;
import com.rapidminer.tools.Tools;


/**
 * Calculates the best split point for numerical attributes according to a given criterion.
 *
 * @author Ingo Mierswa, Gisa Schaefer
 */
public class ColumnNumericalSplitter {

	private ColumnCriterion criterion;
	private ColumnExampleTable columnTable;

	public ColumnNumericalSplitter(ColumnExampleTable columnTable, ColumnCriterion criterion) {
		this.criterion = criterion;
		this.columnTable = columnTable;
	}

	/**
	 * Calculates where to best split a numerical attribute by considering all possibilities and the
	 * associated benefits according to the given criterion. If there are missing values, they are
	 * considered as extra class.
	 *
	 * @param selectedExamples
	 *            which of the starting examples are considered sorted such the associated attribute
	 *            values are in ascending order
	 * @param attributeNumber
	 *            indicates which attribute is considered
	 * @return the benefit of the best split
	 */
	public ParallelBenefit getBestSplitBenefit(int[] selectedExamples, int attributeNumber) {

		double bestSplit = Double.NaN;
		double bestSplitBenefit = Double.NEGATIVE_INFINITY;

		if (this.criterion.supportsIncrementalCalculation()) {
			double[] result = incrementalCalculation(columnTable, selectedExamples, attributeNumber);
			bestSplit = result[0];
			bestSplitBenefit = result[1];
		} else {
			final double[] attributeColumn = columnTable.getNumericalAttributeColumn(attributeNumber);
			double lastValue = Double.NaN;
			for (int j : selectedExamples) {

				double currentValue = attributeColumn[j];

				if (!Tools.isEqual(currentValue, lastValue)) {
					double splitValue = (lastValue + currentValue) / 2.0d;
					double benefit = this.criterion.getNumericalBenefit(columnTable, selectedExamples, attributeNumber,
							splitValue);
					if (benefit > bestSplitBenefit) {
						bestSplitBenefit = benefit;
						bestSplit = splitValue;
					}

				}

				lastValue = currentValue;
			}
		}


		if (Double.isNaN(bestSplit)) {
			return null;
		} else {
			return new ParallelBenefit(bestSplitBenefit, attributeNumber, bestSplit);
		}

	}

	/**
	 * Incremental calculation of the best benefit for classification.
	 */
	private double[] incrementalCalculation(ColumnExampleTable columnTable, int[] selectedExamples,
											int attributeNumber) {
		final double[] attributeColumn = columnTable.getNumericalAttributeColumn(attributeNumber);

		double bestSplit = Double.NaN;
		double lastValue = Double.NaN;
		double bestSplitBenefit = Double.NEGATIVE_INFINITY;
		int lastRow = -1;
		WeightDistribution distribution = this.criterion.startIncrementalCalculation(columnTable, selectedExamples,
				attributeNumber);
		for (int j : selectedExamples) {

			double currentValue = attributeColumn[j];
			if (lastRow > -1) {
				this.criterion.updateWeightDistribution(columnTable, lastRow, distribution);
			}
			lastRow = j;
			if (!Tools.isEqual(currentValue, lastValue)) {
				double benefit = this.criterion.getIncrementalBenefit(distribution);

				if (benefit > bestSplitBenefit) {
					bestSplitBenefit = benefit;
					bestSplit = (lastValue + currentValue) / 2.0d;
				}
			}

			lastValue = currentValue;
		}
		return new double[]{bestSplit, bestSplitBenefit};
	}


}
