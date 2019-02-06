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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeRegexp;


/**
 * <p>
 * This operator is able to create feature weights based on regular expressions defined for the
 * feature names. For example, the user can map all features with a name starting with "Att" to the
 * weight 0.5 by using the regular expression "Att.*". Alternatively, the specified weight may be
 * considered as weight sum for all attributes matching the corresponding regular expression and may
 * be equally distributed among these attributes. All other feature weights whose feature names are
 * not covered by one of the regular expressions are set to the default weight.
 * </p>
 * 
 * <p>
 * Please note that the weights defined in the regular expression list are set in the order as they
 * are defined in the list, i.e. weights can overwrite weights set before.
 * </p>
 * 
 * @author Thomas Beckers, Ingo Mierswa, Tobias Malbrecht
 */
public class NameBasedWeighting extends AbstractWeighting {

	public static final String PARAMETER_ATTRIBUTE_NAME_REGEX = "name_regex_to_weights";

	public static final String PARAMETER_DISTRIBUTE_WEIGHTS = "distribute_weights";

	public static final String PARAMETER_DEFAULT_WEIGHT = "default_weight";

	public NameBasedWeighting(OperatorDescription description) {
		super(description, false);
	}

	@Override
	protected AttributeWeights calculateWeights(ExampleSet exampleSet) throws OperatorException {
		boolean distributeWeights = getParameterAsBoolean(PARAMETER_DISTRIBUTE_WEIGHTS);

		// init all weights with the default weight
		double defaultWeight = getParameterAsDouble(PARAMETER_DEFAULT_WEIGHT);
		AttributeWeights attributeWeights = new AttributeWeights();
		for (Attribute attribute : exampleSet.getAttributes()) {
			attributeWeights.setWeight(attribute.getName(), defaultWeight);
		}

		List<String[]> parameterList = getParameterList(PARAMETER_ATTRIBUTE_NAME_REGEX);
		for (String[] entry : parameterList) {
			Pattern pattern = Pattern.compile(entry[0]);
			double weight = Double.valueOf(entry[1]);
			if (distributeWeights) {
				int count = 0;
				for (Attribute attribute : exampleSet.getAttributes()) {
					if (pattern.matcher(attribute.getName()).matches()) {
						count++;
					}
				}
				for (Attribute attribute : exampleSet.getAttributes()) {
					if (pattern.matcher(attribute.getName()).matches()) {
						attributeWeights.setWeight(attribute.getName(), weight / count);
					}
				}
			} else {
				for (Attribute attribute : exampleSet.getAttributes()) {
					if (pattern.matcher(attribute.getName()).matches()) {
						attributeWeights.setWeight(attribute.getName(), weight);
					}
				}
			}
		}

		return attributeWeights;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeList(PARAMETER_ATTRIBUTE_NAME_REGEX,
				"This list maps different regular expressions for the feature names to the specified weights.",
				new ParameterTypeRegexp("attributes",
						"This regular expression specifies the attributes to assign the weight.") {

					private static final long serialVersionUID = -6782736474400842376L;

					@Override
					public LinkedList<String> getPreviewList() {
						LinkedList<String> previewList = new LinkedList<>();
						MetaData md = getExampleSetInputPort().getMetaData();
						if (md instanceof ExampleSetMetaData) {
							for (AttributeMetaData amd : ((ExampleSetMetaData) md).getAllAttributes()) {
								previewList.add(amd.getName());
							}
						}
						return previewList;
					}
				}, new ParameterTypeDouble("weight", "The new weight for all this attributes.", Double.NEGATIVE_INFINITY,
						Double.MAX_VALUE, false));
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);
		types.add(new ParameterTypeBoolean(
				PARAMETER_DISTRIBUTE_WEIGHTS,
				"If enabled, the weights specified in the list are split and distributed equally among the attributes matching the corresponding regular expressions.",
				false));
		types.add(new ParameterTypeDouble(
				PARAMETER_DEFAULT_WEIGHT,
				"This default weight is used for all features not covered by any of the regular expressions given in the list.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0d));
		return types;
	}
}
