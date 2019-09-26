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
import com.rapidminer.operator.learner.tree.ColumnExampleTable;


/**
 * This class represents the weight distribution of a sorted attribute column on two sides of a
 * split while going along the column. The left label weights contain the weighted label values to
 * the left of a split point and the right label values contain the ones to the right of the split
 * point. At the start the left label weights are all zero and the right label weights are maximal.
 * At each step from left to right the left and right label weights are updated. If there are
 * missing attribute values, they are counted separately.
 *
 * @author Gisa Schaefer
 *
 */
public class WeightDistribution {

	/** the weighted total occurrences of each label value */
	private double[] totalLabelWeights;

	/** the weighted occurrences of each label value to the right of the split point */
	private double[] leftLabelWeights;

	/** the weighted occurrences of each label value to the right of the split point */
	private double[] rightLabelWeights;

	/** the weighted occurrences of each label value among the missing values */
	private double[] missingsLabelWeights;

	/** the sum of all leftWeights */
	protected double leftWeight;

	/** the sum of all rightWeights */
	protected double rightWeight;

	/** the sum of all totalWeights */
	protected double totalWeight;

	/** the sum of all missingsWeights */
	protected double missingsWeight;

	/** indicates whether there are missing attribute values in the example selection */
	private boolean hasMissings = false;

	/**
	 * Initializes the counting arrays with the start distribution.
	 */
	public WeightDistribution(ColumnExampleTable columnTable, int[] selection, int attributeNumber) {
		calculateLabelWeights(columnTable, selection, attributeNumber);
		leftLabelWeights = new double[totalLabelWeights.length];
		leftWeight = 0;
		totalWeight = getTotalWeight(totalLabelWeights);
		if (hasMissings) {
			missingsWeight = getTotalWeight(missingsLabelWeights);
			rightWeight = totalWeight - missingsWeight;
			rightLabelWeights = arrayDifference(totalLabelWeights, missingsLabelWeights);
		} else {
			missingsWeight = 0;
			rightWeight = totalWeight;
			rightLabelWeights = new double[totalLabelWeights.length];
			System.arraycopy(totalLabelWeights, 0, rightLabelWeights, 0, totalLabelWeights.length);
		}

	}

	protected WeightDistribution() {
		// noop for subclasses
	}

	/**
	 * Calculates the start distributions.
	 */
	private void calculateLabelWeights(ColumnExampleTable columnTable, int[] selection, int attributeNumber) {
		Attribute label = columnTable.getLabel();
		int[] labelColumn = columnTable.getLabelColumn();
		Attribute weightAttribute = columnTable.getWeight();
		double[] weightColumn = columnTable.getWeightColumn();
		totalLabelWeights = new double[label.getMapping().size()];
		missingsLabelWeights = new double[totalLabelWeights.length];
		for (int j : selection) {
			int labelIndex = labelColumn[j];
			double weight = 1.0d;
			if (weightAttribute != null) {
				weight = weightColumn[j];
			}
			totalLabelWeights[labelIndex] += weight;
			if (Double.isNaN(columnTable.getNumericalAttributeColumn(attributeNumber)[j])) {
				hasMissings = true;
				missingsLabelWeights[labelIndex] += weight;
			}
		}

	}

	/**
	 * Increments the left label weights at the given position by the given weight and decrements
	 * the right label weights. Updates the sum of all left and right label weights respectively.
	 *
	 * @param position
	 * @param weight
	 */
	public void increment(int position, double weight) {
		leftLabelWeights[position] += weight;
		rightLabelWeights[position] -= weight;
		leftWeight += weight;
		rightWeight -= weight;
	}

	/**
	 * @return the sum of the weighted label value occurrences to the left of the split point.
	 */
	public double getLeftWeigth() {
		return leftWeight;
	}

	/**
	 * @return the sum of the weighted label value occurrences to the right of the split point.
	 */
	public double getRightWeigth() {
		return rightWeight;
	}

	/**
	 * @return the total sum of the weighted label value occurrences.
	 */
	public double getTotalWeigth() {
		return totalWeight;
	}

	/**
	 * @return the sum of the weighted label value occurrences at missing values of the attribute.
	 */
	public double getMissingsWeigth() {
		return missingsWeight;
	}

	/**
	 * @return the weighted occurrences of each label value to the left of the split point
	 */
	public double[] getLeftLabelWeigths() {
		return leftLabelWeights;
	}

	/**
	 * @return the weighted occurrences of each label value to the right of the split point
	 */
	public double[] getRightLabelWeigths() {
		return rightLabelWeights;
	}

	/**
	 * @return the weighted total occurrences of each label value
	 */
	public double[] getTotalLabelWeigths() {
		return totalLabelWeights;
	}

	/**
	 * @return the weighted occurrences of each label value among the missing values
	 */
	public double[] getMissingsLabelWeigths() {
		return missingsLabelWeights;
	}

	/**
	 * @return <code>true</code> if the attribute has missing values among the current selection
	 */
	public boolean hasMissingValues() {
		return hasMissings;
	}

	/** Returns the sum of the given weights. */
	private double getTotalWeight(double[] weights) {
		double sum = 0.0d;
		for (double w : weights) {
			sum += w;
		}
		return sum;
	}

	/**
	 * Creates an array containing the differences of the entries of the given arrays.
	 *
	 * @param array1
	 * @param array2
	 * @return
	 */
	private double[] arrayDifference(double[] array1, double[] array2) {
		double[] difference = new double[array1.length];
		for (int i = 0; i < array1.length; i++) {
			difference[i] = array1[i] - array2[i];
		}
		return difference;
	}

}
