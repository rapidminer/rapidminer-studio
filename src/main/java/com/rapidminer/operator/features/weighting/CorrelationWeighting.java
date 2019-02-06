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
package com.rapidminer.operator.features.weighting;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.math.MathFunctions;


/**
 * 
 * This class provides a weighting scheme based upon correlation. It calculates the correlation of
 * each attribute with the label attribute and returns the absolute or squared value as its weight.
 * 
 * Please keep in mind, that polynomial classes provide no information about their ordering, so that
 * the weights are more or less random, because depending on the internal numerical representation
 * of the classes. Binominal labels work because of the 0-1 coding, as do numerical.
 * 
 * @author Sebastian Land
 */
public class CorrelationWeighting extends AbstractWeighting {

	private static final int PROGRESS_UPDATE_STEPS = 200_000;
	
	public static final String PARAMETER_SQUARED_CORRELATION = "squared_correlation";

	/**
	 * @param description
	 */
	public CorrelationWeighting(OperatorDescription description) {
		super(description, true);

	}

	@Override
	public AttributeWeights calculateWeights(ExampleSet exampleSet) throws OperatorException {
		Attributes attributes = exampleSet.getAttributes();
		Attribute labelAttribute = attributes.getLabel();
		boolean useSquaredCorrelation = getParameterAsBoolean(PARAMETER_SQUARED_CORRELATION);

		AttributeWeights weights = new AttributeWeights(exampleSet);
		getProgress().setTotal(attributes.size());
		int progressCounter = 0;
		int exampleSetSize = exampleSet.size();
		int exampleCounter = 0;
		for (Attribute attribute : attributes) {
			double correlation = MathFunctions.correlation(exampleSet, labelAttribute, attribute, useSquaredCorrelation);
			weights.setWeight(attribute.getName(), Math.abs(correlation));
			progressCounter++;
			exampleCounter += exampleSetSize;
			if(exampleCounter > PROGRESS_UPDATE_STEPS) {
				exampleCounter = 0;
				getProgress().setCompleted(progressCounter);
			}
		}

		return weights;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_LABEL:
			case NUMERICAL_LABEL:
			case BINOMINAL_ATTRIBUTES:
			case NUMERICAL_ATTRIBUTES:
				return true;
			default:
				return false;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_SQUARED_CORRELATION,
				"Indicates if the squared correlation should be calculated.", false));
		return types;
	}

}
