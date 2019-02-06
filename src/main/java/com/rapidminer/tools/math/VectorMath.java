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

import java.util.Arrays;


/**
 * This class provides basic operations on vectors like subtraction, multiply and division.
 * 
 * @author Regina Fritsch
 */
public class VectorMath {

	public static final double[] vectorSubtraction(double[] x, double[] y) {
		if (y == null || x == null) {
			throw new RuntimeException("Cannot substract vectors: one vector is null");
		} else if (x.length != y.length) {
			throw new RuntimeException("Cannot substract vectors: incompatible numbers of attributes (" + x.length + " != "
					+ y.length + ")!");
		}
		double[] result = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			result[i] = x[i] - y[i];
		}
		return result;
	}

	public static final double[] vectorAddition(double[] x, double[] y) {
		if (x.length != y.length) {
			throw new RuntimeException("Cannot add vectors: incompatible numbers of attributes (" + x.length + " != "
					+ y.length + ")!");
		}
		double[] result = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			result[i] = x[i] + y[i];
		}
		return result;
	}

	public static final double vectorMultiplication(double[] vec1, double[] vec2) {
		if (vec1.length != vec2.length) {
			throw new RuntimeException("Cannot multiply vectors: incompatible numbers of attributes (" + vec1.length
					+ " != " + vec2.length + ")!");
		}
		double resultEven = 0;
		double resultOdd = 0;
		int until = vec1.length - vec1.length % 2;
		for (int jEven = 0; jEven < until; jEven += 2) {
			int jOdd = jEven + 1;
			resultEven += vec1[jEven] * vec2[jEven];
			resultOdd += vec1[jOdd] * vec2[jOdd];
		}
		if (vec1.length % 2 == 1) {
			return resultEven + resultOdd + vec1[vec1.length - 1] * vec2[vec1.length - 1];
		} else {
			return resultEven + resultOdd;
		}
	}

	public static final double[] vectorMultiplication(double[] x, double y) {
		double result[] = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			result[i] = x[i] * y;
		}
		return result;
	}

	public static final double[] vectorDivision(double[] x, double y) {
		double result[] = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			result[i] = x[i] / y;
		}
		return result;
	}

	public static final double[][] matrixDivision(double[][] x, double y) {
		double result[][] = null;
		if (x.length != 0) {
			result = new double[x.length][x[0].length];
			for (int i = 0; i < x.length; i++) {
				for (int j = 0; j < x[0].length; j++) {
					result[i][j] = x[i][j] / y;
				}
			}
		}
		return result;
	}

	public static double[] squareComponents(final double[] a) {
		return multiplyComponents(a, a);
	}

	public static double[] multiplyComponents(final double[] a, final double[] b) {
		double[] prods = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			prods[i] = a[i] * b[i];
		}
		return prods;
	}

	public static double sum(final double[] a) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i];
		}
		return sum;
	}

	public static double[] subtractComponents(final double[] a, final double[] b) {
		double[] result = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			result[i] = a[i] - b[i];
		}
		return result;
	}

	public static double[] scalarProduct(final double[] a, final double s) {
		double[] result = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			result[i] = a[i] * s;
		}
		return result;
	}

	public static double[] sumComponents(final double[] a, final double[] b) {
		double[] result = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			result[i] = a[i] + b[i];
		}
		return result;
	}

	public double angle(final double[] a, final double[] b) {
		return Math.acos(dot(a, b) / (vectorNorm(a) * vectorNorm(b)));
	}

	/**
	 * Perpendicular bisector of two Points. Works in any dimension. The coefficients are returned
	 * as a Point of one higher dimension (e.g., (A,B,C,D) for an equation of the form Ax + By + Cz
	 * + D = 0).
	 * 
	 * @param a
	 *            the first point
	 * @param a
	 *            the other point
	 * @return the coefficients of the perpendicular bisector
	 */
	public double[] bisector(final double[] a, final double[] b) {
		double[] diff = subtractComponents(a, b);
		double[] sum = sumComponents(a, b);
		double dot = dot(diff, sum);
		double[] result = new double[diff.length + 1];
		System.arraycopy(diff, 0, result, 0, diff.length);
		result[diff.length] = -dot / 2;
		return result;
	}

	public static double vectorNorm(final double[] a) {
		double quadSum = 0;
		for (double comp : a) {
			quadSum += comp * comp;
		}
		return Math.sqrt(quadSum);
	}

	public static double dot(final double[] a, final double[] b) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i] * b[i];
		}
		return sum;
	}

	public static double[] normalize(final double[] array, double lowerBound, double upperBound) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < array.length; i++) {
			if (array[i] < min) {
				min = array[i];
			}
			if (array[i] > max) {
				max = array[i];
			}
		}

		double targetDynamicRange = upperBound - lowerBound;
		double dynamicRange = max - min;
		double[] result = new double[array.length];

		for (int i = 0; i < array.length; i++) {
			result[i] = ((array[i] - min) / dynamicRange) * targetDynamicRange + lowerBound;
		}
		return result;
	}

	/**
	 * This method expands the given vector with the polynomial bases of the given degree. For
	 * example if degree is 2 and the vector x consists of 2 components x1, x2, the result would be:
	 * 1, x1, x2, x1^2, x1x2, x2^2 So the constant 1 will always be added, do not include it into x!
	 */
	public static final double[] polynomialExpansion(double[] x, int degree) {
		double[] result = new double[getPolynomialExpansionSize(x.length, degree)];
		int current = 0;

		// adding constant 1
		result[current++] = 1;

		// adding power degree level
		for (int currentDegree = 1; currentDegree <= degree; currentDegree++) {
			int[] counter = new int[currentDegree];
			while (true) {
				// doing something on current
				double currentResult = x[counter[0]];
				for (int currentCounter = 1; currentCounter < currentDegree; currentCounter++) {
					currentResult *= x[counter[currentCounter]];
				}
				result[current] = currentResult;
				current++;

				// moving counter to next position and if necessary the counter above
				if (!isLastPosition(counter, x.length)) {
					counter[0]++;
					if (counter[0] == x.length) {
						counter[0] = moveCounterAhead(counter, 1, x.length);
					}
				} else {
					break;
				}

			}
		}
		return result;
	}

	private static final int moveCounterAhead(int[] counter, int counterPos, int numberOfComponents) {
		counter[counterPos]++;
		if (counter[counterPos] == numberOfComponents) {
			counter[counterPos] = moveCounterAhead(counter, counterPos + 1, numberOfComponents);
		}
		return counter[counterPos];
	}

	private static final boolean isLastPosition(int[] counter, int numberOfElements) {
		for (int i = 0; i < counter.length; i++) {
			if (counter[i] < numberOfElements - 1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * This method returns the number of dimensions, a vector will have after a polynomial
	 * expansion.
	 */
	public static final int getPolynomialExpansionSize(int numberOfComponents, int degree) {
		// initialize
		int[] result = new int[numberOfComponents];
		Arrays.fill(result, 1);

		// now iterate over degrees
		int totalSum = 1;
		for (int currentDegree = 1; currentDegree <= degree; currentDegree++) {
			for (int i = 0; i < result.length; i++) {
				totalSum += result[i];

				// updating counter for next round
				int sum = 0;
				for (int j = i; j < result.length; j++) {
					sum += result[j];
				}
				result[i] = sum;
			}
		}
		return totalSum;
	}

	/**
	 * This method returns the median value of the given double array. This is a slow
	 * implementation, because one sorting is needed.
	 * 
	 * TODO : Implementing using fast linear time algorithm
	 */
	public static final double getMedian(double[] residuals) {
		double[] copy = residuals.clone();
		Arrays.sort(copy);
		return copy[copy.length / 2];
	}

	/**
	 * This method returns the maximal skalar inside this vector.
	 */
	public static double maximalElement(double[] values) {
		double maximal = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < values.length; i++) {
			maximal = (values[i] > maximal) ? values[i] : maximal;
		}
		return maximal;
	}

	/**
	 * This method returns the smallest skalar inside this vector
	 */
	public static double minimalElement(double[] values) {
		double minimal = Double.POSITIVE_INFINITY;
		for (int i = 0; i < values.length; i++) {
			minimal = (values[i] < minimal) ? values[i] : minimal;
		}
		return minimal;
	}

}
