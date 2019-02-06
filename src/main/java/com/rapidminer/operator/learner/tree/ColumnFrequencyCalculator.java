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

import com.rapidminer.example.Attribute;
import com.rapidminer.tools.Tools;


/**
 * Provides methods to count label value occurrences.
 *
 * @author Gisa Schaefer
 *
 */
public class ColumnFrequencyCalculator {

	/**
	 * Counts the weighted number of label value occurrences in each splitting class. Each attribute
	 * values is a class, where missing values are an extra class.
	 *
	 * @param columnTable
	 * @param selection
	 * @param attributeNumber
	 * @return
	 */
	public static double[][] getNominalWeightCounts(ColumnExampleTable columnTable, int[] selection, int attributeNumber) {
		Attribute label = columnTable.getLabel();
		int numberOfLabels = label.getMapping().size();
		// maximal as many values as size of the mapping and one more for potential NaNs
		int numberOfValues = columnTable.getNominalAttribute(attributeNumber).getMapping().size() + 1;

		Attribute weightAttribute = columnTable.getWeight();
		double[] weightColumn = columnTable.getWeightColumn();
		byte[] attributeColumn = columnTable.getNominalAttributeColumn(attributeNumber);
		int[] labelColumn = columnTable.getLabelColumn();

		double[][] weightCounts = new double[numberOfValues][numberOfLabels];

		for (int i : selection) {
			int labelIndex = labelColumn[i];
			byte valueIndex = attributeColumn[i];
			double weight = 1.0d;
			if (weightAttribute != null) {
				weight = weightColumn[i];
			}
			weightCounts[valueIndex][labelIndex] += weight;
		}
		return weightCounts;
	}

	/**
	 * Counts the weighted number of label value occurrences in each splitting class. One class
	 * consists of all attribute values smaller than the splitValue, one of those bigger and if
	 * there are missing values they are an extra class.
	 *
	 * @param columnTable
	 * @param selection
	 * @param attributeNumber
	 * @param splitValue
	 * @return
	 */
	public static double[][] getNumericalWeightCounts(ColumnExampleTable columnTable, int[] selection, int attributeNumber,
			double splitValue) {
		Attribute label = columnTable.getLabel();
		int numberOfLabels = label.getMapping().size();

		Attribute weightAttribute = columnTable.getWeight();
		double[] weightColumn = columnTable.getWeightColumn();
		double[] attributeColumn = columnTable.getNumericalAttributeColumn(attributeNumber);
		int[] labelColumn = columnTable.getLabelColumn();

		double[][] weightCounts = new double[3][numberOfLabels];
		for (int i : selection) {
			int labelIndex = labelColumn[i];
			double value = attributeColumn[i];
			double weight = 1.0d;
			if (weightAttribute != null) {
				weight = weightColumn[i];
			}

			if (Double.isNaN(value)) { // Count missings as extra class
				weightCounts[2][labelIndex] += weight;
			} else if (Tools.isLessEqual(value, splitValue)) {
				weightCounts[0][labelIndex] += weight;
			} else {
				weightCounts[1][labelIndex] += weight;
			}
		}

		return weightCounts;
	}

}
