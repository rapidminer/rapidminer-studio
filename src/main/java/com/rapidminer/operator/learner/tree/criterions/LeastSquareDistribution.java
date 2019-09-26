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

import com.rapidminer.operator.learner.tree.ColumnExampleTable;
import com.rapidminer.tools.math.MathFunctions;


/**
 * This class represents the least square distribution of a sorted attribute column on two sides of a split while going
 * along the column. The left sum and count contain the weighted label values to the left of a split point and the right
 * sum and count contain the ones to the right of the split point. At the start the left sum and count are all zero and
 * the right ones are maximal. At each step from left to right the left and right label weights are updated. If there
 * are missing attribute values, they are counted separately.
 *
 * @author Gisa Meier
 * @since 9.4.1
 */
public class LeastSquareDistribution extends WeightDistribution {

	private double leftCount = 0;
	private double missingsCount = 0;
	private double totalCount;
	private double totalResidual = 0;
	private int selectionLength;

	/**
	 * Initializes the counting arrays with the start distribution.
	 */
	LeastSquareDistribution(ColumnExampleTable columnTable, int[] selection, int attributeNumber) {
		super();
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
				missingsWeight += label[selected] * weight;
				missingsCount += weight;
			} else {
				totalWeight += label[selected] * weight;
				totalCount += weight;
			}
		}

		totalWeight += leftWeight + missingsWeight;
		totalCount += leftCount + missingsCount;

		double totalAverage = totalWeight / totalCount;
		for (int selected : selection) {
			totalResidual += MathFunctions.square(label[selected] - totalAverage);
		}
		selectionLength = selection.length;
	}

	@Override
	public void increment(int position, double weight) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Increments the left sum and count by the given value and weight and decrements the right sum and count.
	 *
	 * @param value
	 * 		the value to move to the left
	 * @param weight
	 * 		the weight associated to the value
	 */
	public void increment(double value, double weight) {
		leftCount += weight;
		leftWeight += value * weight;
	}


	/**
	 * @return the total residual
	 */
	double getTotalResidual() {
		return totalResidual;
	}

	/**
	 * @return the residual difference
	 */
	double getResidualDifference() {
		double leftAverage = 0;
		if (leftCount > 0) {
			leftAverage = leftWeight / leftCount;
		}
		double rightAverage = 0;
		double secondCount = totalCount - leftCount - missingsCount;
		double secondSum = totalWeight - leftWeight - missingsWeight;
		if (secondCount > 0) {
			rightAverage = secondSum / secondCount;
		}
		double missingAverage = 0;
		if (missingsCount > 0) {
			missingAverage = missingsWeight / missingsCount;
		}
		double totalAverage = totalWeight / totalCount;
		return LeastSquareDistributionColumnCriterion.getResidualDifference(leftWeight, leftAverage, secondSum, rightAverage,
				missingsWeight, missingAverage, totalWeight, totalAverage);
	}

	/**
	 * @return the number of selected rows for which this distribution is calculated
	 */
	double getLength() {
		return selectionLength;
	}
}
