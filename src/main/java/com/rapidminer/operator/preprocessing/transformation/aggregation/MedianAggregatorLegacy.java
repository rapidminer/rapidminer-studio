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
package com.rapidminer.operator.preprocessing.transformation.aggregation;

import java.util.ArrayList;
import java.util.List;


/**
 * This is an {@link Aggregator} for the {@link MeanAggregationFunction} It uses a linear time
 * algorithm for computing the median, but also the memory consumption will grow (double) linearly
 * with the size of the dataset.
 *
 *
 * @author Sebastian Land
 */
public class MedianAggregatorLegacy extends NumericalAggregator {

	private static final int BUFFER_SIZE = 65536;

	private static class MedianListElement {

		private double[] elements = new double[BUFFER_SIZE];
	}

	private static class WeightedMedianListElement {

		private double[] elements = new double[BUFFER_SIZE];
		private double[] weights = new double[BUFFER_SIZE];
	}

	private List<MedianListElement> elements = null;
	private List<WeightedMedianListElement> weightedElements = null;
	private int currentIndex = 0;
	private int count = 0;
	private MedianListElement currentElement = null;
	private WeightedMedianListElement currentWeightedElement = null;

	public MedianAggregatorLegacy(AggregationFunction function) {
		super(function);
	}

	private List<WeightedMedianListElement> getWeightedElements() {
		if (weightedElements == null) {
			weightedElements = new ArrayList<WeightedMedianListElement>();
		}
		return weightedElements;
	}

	@Override
	public void count(double value) {
		currentIndex = count % BUFFER_SIZE;
		if (currentIndex == 0) {
			if (elements == null) {
				elements = new ArrayList<MedianListElement>();
			}

			currentElement = new MedianListElement();
			elements.add(currentElement);
		}

		currentElement.elements[currentIndex] = value;

		count++;
	}

	@Override
	public void count(double value, double weight) {
		currentIndex = count % BUFFER_SIZE;
		if (currentIndex == 0) {
			currentWeightedElement = new WeightedMedianListElement();
			getWeightedElements().add(currentWeightedElement);
		}

		currentWeightedElement.elements[currentIndex] = value;
		currentWeightedElement.weights[currentIndex] = weight;

		count++;
	}

	@Override
	public double getValue() {
		// first derive full copy of all values into one single array
		double[] allValues = new double[count];
		double[] allWeights = null;
		boolean useWeights = false;

		// median of counting is NaN
		if (count == 0) {
			return Double.NaN;
		}

		if (elements != null) {
			// Copy all full elements
			for (int i = 0; i < elements.size() - 1; i++) {
				System.arraycopy(elements.get(i).elements, 0, allValues, i * BUFFER_SIZE, BUFFER_SIZE);
			}

			// for the last only copy the filled values
			int numberOfValues = count % BUFFER_SIZE;
			System.arraycopy(elements.get(elements.size() - 1).elements, 0, allValues, (elements.size() - 1) * BUFFER_SIZE,
					numberOfValues);
		} else {
			useWeights = true;
			allWeights = new double[count];

			List<WeightedMedianListElement> weightedElements = getWeightedElements();

			// Copy all full elements including weights
			for (int i = 0; i < weightedElements.size() - 1; i++) {
				System.arraycopy(weightedElements.get(i).elements, 0, allValues, i * BUFFER_SIZE, BUFFER_SIZE);
				System.arraycopy(weightedElements.get(i).weights, 0, allWeights, i * BUFFER_SIZE, BUFFER_SIZE);
			}

			// for the last only copy the filled values
			int numberOfValues = count % BUFFER_SIZE;
			System.arraycopy(weightedElements.get(weightedElements.size() - 1).elements, 0, allValues,
					(weightedElements.size() - 1) * BUFFER_SIZE, numberOfValues);
			System.arraycopy(weightedElements.get(weightedElements.size() - 1).weights, 0, allWeights,
					(weightedElements.size() - 1) * BUFFER_SIZE, numberOfValues);
		}

		// now going through
		double pivotValue = allValues[allValues.length / 2];
		int cutWeightLeft = 0;
		int cutWeightRight = 0;
		int countLeft = 0;
		int countRight = 0;
		int weightLeft = 0;
		int weightRight = 0;
		int weightEqual = 0;
		// find pivot position
		while (true) {
			if (!useWeights) {
				for (double allValue : allValues) {
					if (allValue < pivotValue) {
						countLeft++;
						weightLeft++;
					} else if (allValue > pivotValue) {
						countRight++;
						weightRight++;
					} else {
						weightEqual++;
					}
				}
			} else {
				for (int i = 0; i < allValues.length; i++) {
					if (allValues[i] < pivotValue) {
						countLeft++;
						weightLeft += allWeights[i];
					} else if (allValues[i] > pivotValue) {
						countRight++;
						weightRight += allWeights[i];
					} else {
						weightEqual += allWeights[i];
					}
				}
			}
			// check whether we can abort as we have found the median element
			int difference = Math.abs(cutWeightLeft + weightLeft - cutWeightRight - weightRight);
			if (difference <= weightEqual // the pivot element is the median value
					|| allValues.length == 1) {
				break;
			}

			// decide whether we will go to left or right: Median is in larger part
			boolean useLeft = weightLeft + cutWeightLeft > weightRight + cutWeightRight;

			// copy respective values
			double[] newValues = new double[useLeft ? countLeft : countRight];
			int count = 0;
			if (!useWeights) {
				for (double allValue : allValues) {
					if (useLeft && allValue < pivotValue || !useLeft && allValue > pivotValue) {
						newValues[count] = allValue;
						count++;
					}
				}
			} else {
				double[] newWeights = new double[useLeft ? countLeft : countRight];
				for (int i = 0; i < allValues.length; i++) {
					if (useLeft && allValues[i] < pivotValue || !useLeft && allValues[i] > pivotValue) {
						newValues[count] = allValues[i];
						newWeights[count] = allWeights[i];
						count++;
					}
				}
				allWeights = newWeights;
			}
			allValues = newValues;
			pivotValue = allValues[allValues.length / 2];
			if (useLeft) {
				cutWeightRight += weightRight + weightEqual;
			} else {
				cutWeightLeft += weightLeft + weightEqual;
			}
			countLeft = 0;
			countRight = 0;
			weightLeft = 0;
			weightRight = 0;
			weightEqual = 0;
		}

		return pivotValue;
	}
}
