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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.studio.internal.Resources;
import com.rapidminer.tools.Tools;


/**
 * Handles selections of attributes and examples of a {@link ColumnExampleTable}. Creates start
 * selections and updates them.
 *
 * @author Gisa Schaefer
 *
 */
public class SelectionCreator {

	private ColumnExampleTable columnTable;

	public SelectionCreator(ColumnExampleTable columnTable) {
		this.columnTable = columnTable;
	}

	/**
	 * Creates an example index start selection for each numerical attribute, or if there is none,
	 * only one.
	 *
	 * @return a map containing for each numerical attribute an example index array such that the
	 *         associated attribute values are in ascending order.
	 */
	public Map<Integer, int[]> getStartSelection() {
		Map<Integer, int[]> selection = new HashMap<>();
		if (columnTable.getNumberOfRegularNumericalAttributes() == 0) {
			selection.put(0, createFullArray(columnTable.getNumberOfExamples()));
		} else {
			Integer[] bigSelectionArray = createFullBigArray(columnTable.getNumberOfExamples());
			for (int j = columnTable.getNumberOfRegularNominalAttributes(); j < columnTable
					.getTotalNumberOfRegularAttributes(); j++) {
				final double[] attributeColumn = columnTable.getNumericalAttributeColumn(j);
				Integer[] startSelection = Arrays.copyOf(bigSelectionArray, bigSelectionArray.length);
				Arrays.sort(startSelection, new Comparator<Integer>() {

					@Override
					public int compare(Integer a, Integer b) {
						return Double.compare(attributeColumn[a], attributeColumn[b]);
					}
				});
				selection.put(j, ArrayUtils.toPrimitive(startSelection));
			}
		}
		return selection;
	}

	/**
	 * Creates in parallel an example index start selection for each numerical attribute, or if
	 * there is none, only one.
	 *
	 * @param operator
	 *            the operator for which the calculation is done
	 * @return a map containing for each numerical attribute an example index array such that the
	 *         associated attribute values are in ascending order.
	 * @throws OperatorException
	 */
	public Map<Integer, int[]> getStartSelectionParallel(Operator operator) throws OperatorException {
		Map<Integer, int[]> selection = new HashMap<>();
		if (columnTable.getNumberOfRegularNumericalAttributes() == 0) {
			selection.put(0, createFullArray(columnTable.getNumberOfExamples()));
		} else {
			List<Callable<int[]>> tasks = new ArrayList<Callable<int[]>>();
			final Integer[] bigSelectionArray = createFullBigArray(columnTable.getNumberOfExamples());
			for (int j = columnTable.getNumberOfRegularNominalAttributes(); j < columnTable
					.getTotalNumberOfRegularAttributes(); j++) {
				final double[] attributeColumn = columnTable.getNumericalAttributeColumn(j);
				tasks.add(new Callable<int[]>() {

					@Override
					public int[] call() {
						Integer[] startSelection = Arrays.copyOf(bigSelectionArray, bigSelectionArray.length);
						Arrays.sort(startSelection, new Comparator<Integer>() {

							@Override
							public int compare(Integer a, Integer b) {
								return Double.compare(attributeColumn[a], attributeColumn[b]);
							}
						});
						return ArrayUtils.toPrimitive(startSelection);
					}

				});
			}

			List<int[]> results = null;
			try {
				results = Resources.getConcurrencyContext(operator).call(tasks);
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException) {
					throw (RuntimeException) cause;
				} else if (cause instanceof Error) {
					throw (Error) cause;
				} else {
					throw new OperatorException(cause.getMessage(), cause);
				}
			}

			for (int j = columnTable.getNumberOfRegularNominalAttributes(); j < columnTable
					.getTotalNumberOfRegularAttributes(); j++) {
				selection.put(j, results.get(j - columnTable.getNumberOfRegularNominalAttributes()));
			}
		}
		return selection;
	}

	/**
	 * Splits the selected examples according to the bestAttribute and, if the attribute is
	 * numerical, the bestSplitValue.
	 *
	 * @param allSelectedExamples
	 * @param bestAttribute
	 * @param bestSplitValue
	 * @return a collection of maps mapping the numerical attribute number to the sorted array
	 *         containing the selected example numbers
	 */
	public Collection<Map<Integer, int[]>> getSplits(Map<Integer, int[]> allSelectedExamples, int bestAttribute,
			double bestSplitValue) {
		Collection<Map<Integer, int[]>> splits;
		if (columnTable.representsNominalAttribute(bestAttribute)) {
			splits = calculateSplits(allSelectedExamples, bestAttribute);
		} else {
			splits = calculateSplits(allSelectedExamples, bestAttribute, bestSplitValue);
		}
		return splits;
	}

	/**
	 * Splits for every numerical attribute the sorted index array according to the bestSplitValue
	 * at the bestAttribute. Groups by smaller or equal to bestSplitValue, greater than
	 * bestSplitValue and value is NaN.
	 *
	 * @param allSelectedExamples
	 * @param bestAttribute
	 * @param bestSplitValue
	 * @return a list containing first the example number where the value is smaller than
	 *         bestSplitValue, then the ones greater, then the NaNs
	 */
	public Collection<Map<Integer, int[]>> calculateSplits(Map<Integer, int[]> allSelectedExamples, int bestAttribute,
			double bestSplitValue) {
		double[] attributeColumn = columnTable.getNumericalAttributeColumn(bestAttribute);
		List<Map<Integer, int[]>> results = new ArrayList<>(3);
		results.add(0, new HashMap<Integer, int[]>());
		results.add(1, new HashMap<Integer, int[]>());

		boolean existNaNs = false;
		// check if the selectedExamples contain NaN values of the attribute Column - because of
		// sorting they should be at the end
		if (Double
				.isNaN(attributeColumn[allSelectedExamples.get(bestAttribute)[allSelectedExamples.get(bestAttribute).length - 1]])) {
			existNaNs = true;
			results.add(2, new HashMap<Integer, int[]>());
		}
		int maximalLength = getArbitraryValue(allSelectedExamples).length;
		int[] smaller = new int[maximalLength];
		int[] bigger = new int[maximalLength];
		int[] naNs = new int[maximalLength];

		double value;
		for (int i : allSelectedExamples.keySet()) {
			int smallerPosition = 0;
			int biggerPosition = 0;
			int naNsPosition = 0;

			int[] selectedExamples = allSelectedExamples.get(i);
			for (int j : selectedExamples) {
				value = attributeColumn[j];
				if (Double.isNaN(value)) {
					naNs[naNsPosition] = j;
					naNsPosition++;
				} else if (Tools.isLessEqual(value, bestSplitValue)) {
					smaller[smallerPosition] = j;
					smallerPosition++;
				} else {
					bigger[biggerPosition] = j;
					biggerPosition++;
				}
			}
			results.get(0).put(i, Arrays.copyOf(smaller, smallerPosition));
			results.get(1).put(i, Arrays.copyOf(bigger, biggerPosition));
			if (existNaNs) {
				results.get(2).put(i, Arrays.copyOf(naNs, naNsPosition));
			}
		}

		return results;
	}

	/**
	 * Splits for every numerical attribute the sorted index array according to the value at the
	 * best attribute. Groups the splitted arrays by the value at the best attribute.
	 *
	 * @param allSelectedExamples
	 * @param bestAttribute
	 * @return
	 */
	public Collection<Map<Integer, int[]>> calculateSplits(Map<Integer, int[]> allSelectedExamples, int bestAttribute) {
		byte[] attributeColumn = columnTable.getNominalAttributeColumn(bestAttribute);
		Map<Byte, Map<Integer, int[]>> results = new HashMap<>();
		Map<Byte, List<Integer>> valueLists;

		byte value;
		for (int i : allSelectedExamples.keySet()) {
			valueLists = new HashMap<>();
			int[] selectedExamples = allSelectedExamples.get(i);

			for (int j : selectedExamples) {
				// put j in the list associated to its value
				value = attributeColumn[j];
				if (valueLists.containsKey(value)) {
					valueLists.get(value).add(j);
				} else {
					List<Integer> temp = new ArrayList<>();
					temp.add(j);
					valueLists.put(value, temp);
				}
			}

			// store the pair (key, list) as (key, (i,array(list))
			for (Byte key : valueLists.keySet()) {
				List<Integer> list = valueLists.get(key);
				int[] temp = ArrayUtils.toPrimitive(list.toArray(new Integer[list.size()]));
				if (results.containsKey(key)) {
					results.get(key).put(i, temp);
				} else {
					Map<Integer, int[]> toadd = new HashMap<>();
					toadd.put(i, temp);
					results.put(key, toadd);
				}

			}
		}

		return results.values();
	}

	/**
	 * If the bestAttribute is nominal, its number is removed from the selectedAttributes, otherwise
	 * it stays the same.
	 *
	 * @param selectedAttributes
	 * @param bestAttribute
	 * @return
	 */
	public int[] updateRemainingAttributes(int[] selectedAttributes, int bestAttribute) {
		int[] remainingAttributes;
		if (columnTable.representsNominalAttribute(bestAttribute)) {
			remainingAttributes = removeAttribute(bestAttribute, selectedAttributes);
		} else {
			remainingAttributes = selectedAttributes;
		}
		return remainingAttributes;
	}

	/**
	 * Creates a new array containing all entries of selectedAttributes except for
	 * attributeNumberToDelete.
	 *
	 * @param attributeNumberToDelete
	 * @param selectedAttributes
	 * @return
	 */
	public int[] removeAttribute(int attributeNumberToDelete, int[] selectedAttributes) {
		int[] newSelection = new int[selectedAttributes.length - 1];
		int j = 0;
		for (int i : selectedAttributes) {
			if (i != attributeNumberToDelete) {
				newSelection[j] = i;
				j++;
			}
		}
		return newSelection;
	}

	/**
	 * Create a selection array containing all rows, i.e. containing all consecutive numbers
	 * [0..length-1]
	 *
	 * @param length
	 * @return
	 */
	public int[] createFullArray(int length) {
		int[] fullSelection = new int[length];
		for (int i = 0; i < length; i++) {
			fullSelection[i] = i;
		}
		return fullSelection;
	}

	/**
	 * Create an Integer array containing all consecutive numbers [0..length-1]
	 *
	 * @param length
	 * @return
	 */
	public Integer[] createFullBigArray(int length) {
		Integer[] fullSelection = new Integer[length];
		for (int i = 0; i < length; i++) {
			fullSelection[i] = i;
		}
		return fullSelection;
	}

	/**
	 * Returns a value of the map.
	 *
	 * @param map
	 *            a non-empty map
	 * @return
	 */
	public static int[] getArbitraryValue(Map<Integer, int[]> map) {
		return map.values().iterator().next();
	}

}
