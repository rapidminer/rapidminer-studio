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
package com.rapidminer.tools.math;

import com.rapidminer.tools.Tools;


/**
 * This class provides some helper tool for calculations around contingency tables like chi-squared
 * tests etc.
 * 
 * @author Ingo Mierswa
 */
public class ContingencyTableTools {

	/**
	 * This method calculates the chi-squared statistic for the given contingency table.
	 */
	public static double getChiSquaredStatistics(double[][] matrix, boolean useYatesCorrection) {
		int numberOfRows = matrix.length;
		int numberOfColumns = matrix[0].length;
		double[] rowSums = new double[numberOfRows];
		double[] columnSums = new double[numberOfColumns];
		double totalSum = 0;
		for (int row = 0; row < numberOfRows; row++) {
			for (int column = 0; column < numberOfColumns; column++) {
				rowSums[row] += matrix[row][column];
				columnSums[column] += matrix[row][column];
				totalSum += matrix[row][column];
			}
		}

		int degreesOfFreedom = (numberOfRows - 1) * (numberOfColumns - 1);
		boolean useYates = true;
		if ((degreesOfFreedom > 1) || (!useYatesCorrection)) {
			useYates = false;
		} else if (degreesOfFreedom <= 0) {
			return 0;
		}

		double expectedValue = 0;
		double chiSquaredValue = 0.0;
		for (int row = 0; row < numberOfRows; row++) {
			if (rowSums[row] > 0) {
				for (int column = 0; column < numberOfColumns; column++) {
					if (columnSums[column] > 0) {
						expectedValue = (columnSums[column] * rowSums[row]) / totalSum;
						chiSquaredValue += getChiSquaredForEntry(matrix[row][column], expectedValue, useYates);
					}
				}
			}
		}

		return chiSquaredValue;
	}

	/**
	 * This method calculates the chi squared value for a single matrix entry in a contingency
	 * table. The given frequencies are the actual and the expected frequencies in the entry.
	 */
	private static double getChiSquaredForEntry(double actualFrequency, double expectedFrequency, boolean useYatesCorrection) {
		if (expectedFrequency <= 0) {
			return 0;
		}
		double difference = Math.abs(actualFrequency - expectedFrequency);

		if (useYatesCorrection) {
			difference -= 0.5;
			if (difference < 0) {
				difference = 0;
			}
		}

		return (difference * difference / expectedFrequency);
	}

	/**
	 * This method deletes all zero rows and columns.
	 */
	public static double[][] deleteEmpty(double[][] matrix) {
		int numberOfRows = matrix.length;
		int numberOfColumns = matrix[0].length;
		double[] rowSums = new double[numberOfRows];
		double[] columnSums = new double[numberOfColumns];
		for (int row = 0; row < numberOfRows; row++) {
			for (int column = 0; column < numberOfColumns; column++) {
				rowSums[row] += matrix[row][column];
				columnSums[column] += matrix[row][column];
			}
		}

		int nonZeroRowCounter = 0;
		for (int row = 0; row < numberOfRows; row++) {
			if (rowSums[row] > 0) {
				nonZeroRowCounter++;
			}
		}

		int nonZeroColumnCounter = 0;
		for (int column = 0; column < numberOfColumns; column++) {
			if (columnSums[column] > 0) {
				nonZeroColumnCounter++;
			}
		}

		double[][] result = new double[nonZeroRowCounter][nonZeroColumnCounter];
		int rowIndex = 0;
		for (int row = 0; row < numberOfRows; row++) {
			if (rowSums[row] > 0) {
				int columnIndex = 0;
				for (int column = 0; column < numberOfColumns; column++) {
					if (columnSums[column] > 0) {
						result[rowIndex][columnIndex] = matrix[row][column];
						columnIndex++;
					}
				}
				rowIndex++;
			}
		}

		return result;
	}

	/**
	 * Calculates the symmetrical uncertainty.
	 */
	public static double symmetricalUncertainty(double matrix[][]) {
		// columns
		double columnSum = 0.0d;
		double columnEntropy = 0.0d;
		double totalSum = 0;
		for (int i = 0; i < matrix[0].length; i++) {
			columnSum = 0;
			for (int j = 0; j < matrix.length; j++) {
				columnSum += matrix[j][i];
			}
			columnEntropy += entropy(columnSum);
			totalSum += columnSum;
		}
		columnEntropy -= entropy(totalSum);

		// rows
		double rowSum = 0.0d;
		double rowEntropy = 0.0d;
		double entropyForRows = 0.0d;
		for (int i = 0; i < matrix.length; i++) {
			rowSum = 0;
			for (int j = 0; j < matrix[0].length; j++) {
				rowSum += matrix[i][j];
				entropyForRows += entropy(matrix[i][j]);
			}
			rowEntropy += entropy(rowSum);
		}

		entropyForRows -= rowEntropy;
		rowEntropy -= entropy(totalSum);
		double informationGain = columnEntropy - entropyForRows;

		if (Tools.isEqual(columnEntropy, 0) || Tools.isEqual(rowEntropy, 0)) {
			return 0;
		}
		return 2.0d * (informationGain / (columnEntropy + rowEntropy));
	}

	private static double entropy(double value) {
		if (Tools.isZero(value)) {
			return 0;
		} else {
			return value * Math.log(value);
		}
	}
}
