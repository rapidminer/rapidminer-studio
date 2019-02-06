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
package com.rapidminer.operator.postprocessing;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.AbstractExampleSetProcessing;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.Tupel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Generates predictions from confidence attributes.
 *
 * @author Tobias Malbrecht
 */
public class GeneratePredictionOperator extends AbstractExampleSetProcessing {

	public static final String PARAMETER_PREDICTION_NAME = "prediction_name";

	public GeneratePredictionOperator(OperatorDescription description) {
		super(description);
		getExampleSetInputPort().addPrecondition(
				new ExampleSetPrecondition(getExampleSetInputPort(), Attributes.CONFIDENCE_NAME, Ontology.NUMERICAL));
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// searching confidence attributes
		Map<Attribute, String> confidenceAttributes = new LinkedHashMap<Attribute, String>();
		for (Iterator<AttributeRole> iterator = exampleSet.getAttributes().specialAttributes(); iterator.hasNext();) {
			AttributeRole role = iterator.next();
			if (role.getSpecialName().matches(Attributes.CONFIDENCE_NAME + "_.*")) {
				confidenceAttributes.put(role.getAttribute(),
						role.getSpecialName().replaceAll("^" + Attributes.CONFIDENCE_NAME + "_(.*)$", "$1"));
			}
		}

		if (confidenceAttributes.size() > 0) {
			String predictionName = getParameterAsString(PARAMETER_PREDICTION_NAME);
			String attributeName = "prediction(" + predictionName + ")";
			Attribute predictionAttribute = AttributeFactory.createAttribute(attributeName, Ontology.NOMINAL);

			// check if an attribute with the resulting name already exists
			Attribute oldAttribute = exampleSet.getAttributes().get(attributeName);
			if (oldAttribute != null) {
				if (exampleSet.getAttributes().getSpecial(Attributes.PREDICTION_NAME) == oldAttribute) {
					// remove it iff it is the prediction attribute (since it would be removed later
					// anyway, but causes an error if not removed here)
					exampleSet.getAttributes().remove(oldAttribute);
				} else {
					// otherwise throw an error
					throw new UserError(this, 152, attributeName);
				}
			}

			for (String value : confidenceAttributes.values()) {
				predictionAttribute.getMapping().mapString(value);
			}
			exampleSet.getExampleTable().addAttribute(predictionAttribute);
			exampleSet.getAttributes().addRegular(predictionAttribute);
			exampleSet.getAttributes().setSpecialAttribute(predictionAttribute, Attributes.PREDICTION_NAME);
			for (Example example : exampleSet) {
				ArrayList<Tupel<Double, String>> labelConfidences = new ArrayList<Tupel<Double, String>>(
						confidenceAttributes.size());
				for (Map.Entry<Attribute, String> entry : confidenceAttributes.entrySet()) {
					labelConfidences.add(new Tupel<Double, String>(example.getValue(entry.getKey()), entry.getValue()));
				}
				Collections.sort(labelConfidences);
				example.setValue(
						predictionAttribute,
						predictionAttribute.getMapping().mapString(
								labelConfidences.get(labelConfidences.size() - 1).getSecond()));
			}
		}

		return exampleSet;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_PREDICTION_NAME, "The name of the label that should be predicted.",
				false, false));
		return types;
	}
}
