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
package com.rapidminer.operator.preprocessing.normalization;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDReal;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.container.Range;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;


/**
 * This is the normalization method for interquartile range.
 * 
 * @author Brendon Bolin, Sebastian Land
 */
public class IQRNormalizationMethod extends AbstractNormalizationMethod {

	@Override
	public Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd,
			InputPort exampleSetInputPort, ParameterHandler parameterHandler) throws UndefinedParameterError {
		amd.setMean(new MDReal((double) 0));
		amd.setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), SetRelation.SUBSET);
		return Collections.singleton(amd);
	}

	@Override
	public AbstractNormalizationModel getNormalizationModel(ExampleSet exampleSet, Operator operator) throws UserError {
		// IQR Transformation
		IQRNormalizationModel model = new IQRNormalizationModel(exampleSet, calculateMeanSigma(exampleSet, operator));
		return model;
	}

	private HashMap<String, Tupel<Double, Double>> calculateMeanSigma(ExampleSet exampleSet, Operator operator) {
		HashMap<String, Tupel<Double, Double>> attributeMeanSigmaMap = new HashMap<String, Tupel<Double, Double>>();

		boolean checkForNonFinite = operator.getCompatibilityLevel()
				.isAbove(Normalization.BEFORE_NON_FINITE_VALUES_HANDLING);

		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) {
				double values[] = new double[exampleSet.size()];
				int i = 0;
				for (Example example : exampleSet) {
					values[i++] = example.getValue(attribute);
				}

				Arrays.sort(values);

				if (checkForNonFinite) {
					values = removeNonfiniteValues(values);
				}

				double iqSigma = 0;
				double median = 0;
				if (values.length > 0) {
					int lowerQuart = (int) ((values.length + 1) * 0.25 - 1);
					int upperQuart = (int) ((values.length + 1) * 0.75 - 1);

					iqSigma = (values[upperQuart] - values[lowerQuart]) / 1.349;
					if (!checkForNonFinite && !Double.isFinite(iqSigma)) {
						nonFiniteValueWarning(operator, attribute.getName(), iqSigma);
					}

					if (0 == values.length % 2) {
						median = (values[values.length / 2] + values[values.length / 2 - 1]) / 2;
					} else {
						median = values[values.length / 2];
					}
				} else {
					LogService.getRoot()
							.warning("Ignoring " + attribute.getName() + " in Normalization because of no usable values");

				}

				attributeMeanSigmaMap.put(attribute.getName(), new Tupel<Double, Double>(median, iqSigma));
			}
		}
		return attributeMeanSigmaMap;
	}

	/**
	 * Checks the given sorted array for non finite values and returns a cropped version containing
	 * only finite values. Will return an empty array if only non-finite values exist.
	 *
	 * @since 7.6
	 */
	private static double[] removeNonfiniteValues(double[] values) {
		return Arrays.copyOfRange(values, findFirstFinite(values), findLastFinite(values) + 1);
	}

	/**
	 * Returns the first index of the given array whose value is finite if it exists. Will return
	 * {@link #findLastFinite(double[])}{@code +1} if none exist.
	 *
	 * @since 7.6
	 */
	private static int findFirstFinite(double[] values) {
		int index = Arrays.binarySearch(values, -Double.MAX_VALUE);
		if (index < 0) {
			return -index - 1;
		}
		while (index >= 0 && values[index--] > Double.NEGATIVE_INFINITY) {
		}
		return index + 2;
	}

	/**
	 * Returns the last index of the given array whose value is finite if it exists. Will return
	 * {@link #findLastFinite(double[])}{@code -1} if none exist.
	 *
	 * @since 7.6
	 */
	private static int findLastFinite(double[] values) {
		int index = Arrays.binarySearch(values, Double.MAX_VALUE);
		if (index < 0) {
			return -index - 2;
		}
		while (index < values.length && values[index++] < Double.POSITIVE_INFINITY) {
		}
		return index - 2;
	}

	@Override
	public String getName() {
		return "interquartile range";
	}

}
