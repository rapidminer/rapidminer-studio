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
package com.rapidminer.operator.util.annotations;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.tools.Ontology;


/**
 * @author Marius Helf
 *
 */
public class DataToAnnotations extends Operator {

	public static final String[] DUPLICATE_HANDLING_LIST = { "overwrite", "ignore", "error" };
	public static final int OVERWRITE_DUPLICATES = 0;
	public static final int IGNORE_DUPLICATES = 1;
	public static final int ERROR_ON_DUPLICATES = 2;

	public static final String PARAMETER_DUPLICATE_HANDLING = "duplicate_annotations";
	public static final String PARAMETER_KEY_ATTRIBUTE = "key_attribute";
	public static final String PARAMETER_VALUE_ATTRIBUTE = "value_attribute";

	private InputPort annotationsInputPort = getInputPorts().createPort("annotations", ExampleSet.class);
	private InputPort objectInputPort = getInputPorts().createPort("object", IOObject.class);
	private OutputPort objectOutputPort = getOutputPorts().createPort("object with annotations");
	private OutputPort annotationsOutputPort = getOutputPorts().createPort("annotations");

	/**
	 * @param description
	 */
	public DataToAnnotations(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(annotationsInputPort, annotationsOutputPort);
		getTransformer().addPassThroughRule(objectInputPort, objectOutputPort);
		annotationsInputPort.addPrecondition(new AttributeSetPrecondition(annotationsInputPort, AttributeSetPrecondition
				.getAttributesByParameter(this, PARAMETER_KEY_ATTRIBUTE)));
		annotationsInputPort.addPrecondition(new AttributeSetPrecondition(annotationsInputPort, AttributeSetPrecondition
				.getAttributesByParameter(this, PARAMETER_VALUE_ATTRIBUTE)));
	}

	@Override
	public void doWork() throws OperatorException {
		IOObject object = objectInputPort.getData(IOObject.class);
		ExampleSet annotationData = annotationsInputPort.getData(ExampleSet.class);

		String keyAttributeName = getParameterAsString(PARAMETER_KEY_ATTRIBUTE);
		String valueAttributeName = getParameterAsString(PARAMETER_VALUE_ATTRIBUTE);

		Attribute keyAttribute = annotationData.getAttributes().get(keyAttributeName);
		Attribute valueAttribute = annotationData.getAttributes().get(valueAttributeName);

		// null checks, type checks on attributes
		if (keyAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_KEY_ATTRIBUTE, keyAttributeName);
		} else if (valueAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_VALUE_ATTRIBUTE, valueAttributeName);
		} else if (!keyAttribute.isNominal()) {
			throw new UserError(this, 103, "Data to Annotations", keyAttributeName);
		} else if (!valueAttribute.isNominal()) {
			throw new UserError(this, 103, "Data to Annotations", valueAttributeName);
		}

		// get index of action for duplicate annotations
		String duplicateActionString = getParameterAsString(PARAMETER_DUPLICATE_HANDLING);
		int duplicateAction = ERROR_ON_DUPLICATES;
		for (int i = 0; i < DUPLICATE_HANDLING_LIST.length; ++i) {
			if (DUPLICATE_HANDLING_LIST[i].equals(duplicateActionString)) {
				duplicateAction = i;
				break;
			}
		}

		// loop all examples and add key/value pairs as annotation
		Annotations annotations = object.getAnnotations();
		for (Example example : annotationData) {
			String key = example.getNominalValue(keyAttribute);
			String value = example.getNominalValue(valueAttribute);
			boolean missingValue = Double.isNaN(example.getValue(valueAttribute));

			if (annotations.containsKey(key)) {
				if (missingValue) {
					annotations.remove(key);
				} else {
					switch (duplicateAction) {
						case OVERWRITE_DUPLICATES:
							// overwrite annotations
							annotations.setAnnotation(key, value);
							break;
						case IGNORE_DUPLICATES:
							// do nothing
							break;
						case ERROR_ON_DUPLICATES:
							// throw user error
							throw new UserError(this, "annotate.duplicate_annotation", key);
					}
				}
			} else if (!missingValue) {
				// add annotation
				annotations.setAnnotation(key, value);
			}
		}

		annotationsOutputPort.deliver(annotationData);
		objectOutputPort.deliver(object);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeAttribute(PARAMETER_KEY_ATTRIBUTE,
				"The attribute which contains the names of the annotations to be created. Should be unique.",
				annotationsInputPort, false, false, Ontology.NOMINAL));
		types.add(new ParameterTypeAttribute(
				PARAMETER_VALUE_ATTRIBUTE,
				"The attribute which contains the values of the annotations to be created. If a value is missing, the respective annotation will be removed.",
				annotationsInputPort, false, false, Ontology.NOMINAL));
		types.add(new ParameterTypeStringCategory(PARAMETER_DUPLICATE_HANDLING,
				"Indicates what should happen if duplicate annotation names are specified.", DUPLICATE_HANDLING_LIST,
				DUPLICATE_HANDLING_LIST[OVERWRITE_DUPLICATES], false));

		return types;
	}
}
