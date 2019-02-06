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
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;


/**
 * <p>
 * Creates weights from the standard deviations of all attributes. The values can be normalized by
 * the average, the minimum, or the maximum of the attribute.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class StandardDeviationWeighting extends AbstractWeighting {

	/**
	 * The parameter name for &quot;Indicates if the standard deviation should be divided by the
	 * minimum, maximum, or average of the attribute.&quot;
	 */
	public static final String PARAMETER_NORMALIZE = "normalize";

	private static final String[] NORMALIZATIONS = { "none", "average", "minimum", "maximum" };

	private static final int NONE = 0;

	private static final int AVERAGE = 1;

	private static final int MINIMUM = 2;

	private static final int MAXIMUM = 3;

	public StandardDeviationWeighting(OperatorDescription description) {
		super(description, false);
	}

	@Override
	protected AttributeWeights calculateWeights(ExampleSet exampleSet) throws OperatorException {
		exampleSet.recalculateAllAttributeStatistics();

		int normalization = getParameterAsInt(PARAMETER_NORMALIZE);
		AttributeWeights weights = new AttributeWeights();

		for (Attribute attribute : exampleSet.getAttributes()) {
			double data = Math.sqrt(exampleSet.getStatistics(attribute, Statistics.VARIANCE));
			switch (normalization) {
				case NONE:
					break;
				case AVERAGE:
					data /= exampleSet.getStatistics(attribute, Statistics.AVERAGE);
					break;
				case MINIMUM:
					data /= exampleSet.getStatistics(attribute, Statistics.MINIMUM);
					break;
				case MAXIMUM:
					data /= exampleSet.getStatistics(attribute, Statistics.MAXIMUM);
					break;
				default:
					break;
			}
			data = Math.abs(data);
			weights.setWeight(attribute.getName(), data);
		}

		return weights;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NUMERICAL_ATTRIBUTES:
			case NO_LABEL:
				return true;
			default:
				return false;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(
				PARAMETER_NORMALIZE,
				"Indicates if the standard deviation should be divided by the minimum, maximum, or average of the attribute.",
				NORMALIZATIONS, 0);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
