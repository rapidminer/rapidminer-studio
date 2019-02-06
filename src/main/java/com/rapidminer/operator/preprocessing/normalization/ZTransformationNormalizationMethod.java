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
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;


/**
 * The normalization method for the Z-Transformation
 * 
 * @author Sebastian Land
 */
public class ZTransformationNormalizationMethod extends AbstractNormalizationMethod {

	@Override
	public Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd,
			InputPort exampleSetInputPort, ParameterHandler parameterHandler) throws UndefinedParameterError {
		amd.setMean(new MDReal((double) 0));
		amd.setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), SetRelation.SUBSET);
		return Collections.singleton(amd);
	}

	@Override
	public AbstractNormalizationModel getNormalizationModel(ExampleSet exampleSet, Operator operator) throws UserError {
		// Z-Transformation
		exampleSet.recalculateAllAttributeStatistics();
		HashMap<String, Tupel<Double, Double>> attributeMeanVarianceMap = new HashMap<String, Tupel<Double, Double>>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) {
				double average = exampleSet.getStatistics(attribute, Statistics.AVERAGE);
				double variance = exampleSet.getStatistics(attribute, Statistics.VARIANCE);
				if (!Double.isFinite(average) || !Double.isFinite(variance)) {
					nonFiniteValueWarning(operator, attribute.getName(), average, variance);
				}
				attributeMeanVarianceMap.put(attribute.getName(), new Tupel<Double, Double>(average, variance));
			}
		}
		ZTransformationModel model = new ZTransformationModel(exampleSet, attributeMeanVarianceMap);
		return model;
	}

	@Override
	public String getName() {
		return "Z-transformation";
	}

}
