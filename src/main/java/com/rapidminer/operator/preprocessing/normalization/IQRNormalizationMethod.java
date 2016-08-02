/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
		IQRNormalizationModel model = new IQRNormalizationModel(exampleSet, calculateMeanSigma(exampleSet));
		return model;
	}

	private HashMap<String, Tupel<Double, Double>> calculateMeanSigma(ExampleSet exampleSet) {
		HashMap<String, Tupel<Double, Double>> attributeMeanSigmaMap = new HashMap<String, Tupel<Double, Double>>();

		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) {
				double values[] = new double[exampleSet.size()];
				int i = 0;
				for (Example example : exampleSet) {
					values[i++] = example.getValue(attribute);
				}

				Arrays.sort(values);

				int lowerQuart = (int) (((values.length + 1) * 0.25) - 1);
				int upperQuart = (int) (((values.length + 1) * 0.75) - 1);

				double iqSigma = (values[upperQuart] - values[lowerQuart]) / 1.349;
				double median = 0;

				if (0 == (exampleSet.size() % 2)) {
					if (exampleSet.size() > 1) {
						median = (values[exampleSet.size() / 2] + values[(exampleSet.size() / 2) - 1]) / 2;
					}
				} else {
					median = values[exampleSet.size() / 2];
				}

				attributeMeanSigmaMap.put(attribute.getName(), new Tupel<Double, Double>(median, iqSigma));
			}
		}
		return attributeMeanSigmaMap;
	}

	@Override
	public String getName() {
		return "interquartile range";
	}

}
