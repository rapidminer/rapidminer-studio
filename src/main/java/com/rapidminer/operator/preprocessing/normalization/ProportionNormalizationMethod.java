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
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDReal;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.math.container.Range;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;


/**
 * A normalization method for bringing the sum of all attribute values to 1.
 * 
 * @author Sebastian Land
 * 
 */
public class ProportionNormalizationMethod extends AbstractNormalizationMethod {

	@Override
	public Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd,
			InputPort exampleSetInputPort, ParameterHandler parameterHandler) throws UndefinedParameterError {
		if (amd.getValueSetRelation() == SetRelation.EQUAL) {
			if (emd.getNumberOfExamples().isKnown()) {
				amd.setMean(new MDReal(1d / emd.getNumberOfExamples().getValue()));
			} else {
				amd.setMean(new MDReal());
			}
			Range range = amd.getValueRange();
			if (range.getLower() < 0d) {
				exampleSetInputPort.addError(new SimpleMetaDataError(Severity.WARNING, exampleSetInputPort,
						"attribute_contains_negative_values", amd.getName(), getName()));
			}
		} else {
			// set to unknown
			amd.setMean(new MDReal());
			amd.setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), SetRelation.UNKNOWN);
		}
		return Collections.singleton(amd);
	}

	@Override
	public AbstractNormalizationModel getNormalizationModel(ExampleSet exampleSet, Operator operator) {
		// calculating attribute sums
		Attributes attributes = exampleSet.getAttributes();
		double[] attributeSum = new double[attributes.size()];
		for (Example example : exampleSet) {
			int i = 0;
			for (Attribute attribute : attributes) {
				if (attribute.isNumerical()) {
					attributeSum[i] += example.getValue(attribute);
				}
				i++;
			}
		}
		HashMap<String, Double> attributeSums = new HashMap<String, Double>();
		int i = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) {
				attributeSums.put(attribute.getName(), attributeSum[i]);
			}
			i++;
		}

		return new ProportionNormalizationModel(exampleSet, attributeSums);
	}

	@Override
	public String getName() {
		return "proportion transformation";
	}

}
