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
package com.rapidminer.operator.preprocessing.join;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetUnionRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator gets two example sets and adds new features to each of both example sets so that
 * both example sets consist of the same set of features. This set is the union or the superset of
 * both original feature sets. The values of the new features are set to missing. This operator only
 * works on the regular attributes and will not change, add, or otherwise modify the existing
 * special attributes.
 * 
 * @author Ingo Mierswa, Marius Helf
 */
public class ExampleSetSuperset extends Operator {

	public static final String PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES = "include_special_attributes";

	private InputPort exampleSet1Input = getInputPorts().createPort("example set 1", ExampleSet.class);
	private InputPort exampleSet2Input = getInputPorts().createPort("example set 2", ExampleSet.class);
	private OutputPort supersetOutput1 = getOutputPorts().createPort("superset 1");
	private OutputPort supersetOutput2 = getOutputPorts().createPort("superset 2");

	public ExampleSetSuperset(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new ExampleSetUnionRule(exampleSet1Input, exampleSet2Input, supersetOutput1, null) {

			@Override
			protected void transformAddedAttributeMD(ExampleSetMetaData emd, AttributeMetaData newAttribute) {
				newAttribute.setValueSetRelation(SetRelation.UNKNOWN);
				newAttribute.setNumberOfMissingValues(emd.getNumberOfExamples());
			}
		});
		getTransformer().addRule(new ExampleSetUnionRule(exampleSet2Input, exampleSet1Input, supersetOutput2, null) {

			@Override
			protected void transformAddedAttributeMD(ExampleSetMetaData emd, AttributeMetaData newAttribute) {
				newAttribute.setValueSetRelation(SetRelation.UNKNOWN);
				newAttribute.setNumberOfMissingValues(emd.getNumberOfExamples());
			}
		});
	}

	public void superset(ExampleSet exampleSet1, ExampleSet exampleSet2) throws OperatorException {
		boolean includeSpecials = getParameterAsBoolean(PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES);

		// determine attributes missing in ES 1
		List<Attribute> newAttributesForES1 = findMissingAttributes(exampleSet1, exampleSet2, includeSpecials);

		// determine attributes missing in ES 2
		List<Attribute> newAttributesForES2 = findMissingAttributes(exampleSet2, exampleSet1, includeSpecials);

		// add new attributes to ES 1
		addNewAttributes(exampleSet1, exampleSet2, includeSpecials, newAttributesForES1);

		// add new attributes to ES 2
		addNewAttributes(exampleSet2, exampleSet1, includeSpecials, newAttributesForES2);
	}

	private void addNewAttributes(ExampleSet exampleSet1, ExampleSet exampleSet2, boolean includeSpecials,
			List<Attribute> newAttributesForES1) throws UserError {
		for (Attribute attribute : newAttributesForES1) {
			exampleSet1.getExampleTable().addAttribute(attribute);
			exampleSet1.getAttributes().addRegular(attribute);
			if (includeSpecials) {
				if (exampleSet2.getAttributes().getRole(attribute.getName()) == null) {
					throw new UserError(this, "superset.special_not_found", attribute.getName());
				}
				// set correct role
				exampleSet1.getAttributes().setSpecialAttribute(attribute,
						exampleSet2.getAttributes().getRole(attribute.getName()).getSpecialName());
			}
		}

		// set all values to missing for ES 1
		for (Attribute attribute : newAttributesForES1) {
			for (Example example : exampleSet1) {
				example.setValue(attribute, Double.NaN);
			}
		}
	}

	private List<Attribute> findMissingAttributes(ExampleSet exampleSet1, ExampleSet exampleSet2, boolean includeSpecials)
			throws UserError {
		List<Attribute> newAttributesForES1 = new LinkedList<Attribute>();
		Iterator<Attribute> iterator;
		if (includeSpecials) {
			iterator = exampleSet2.getAttributes().allAttributes();
		} else {
			iterator = exampleSet2.getAttributes().iterator();
		}
		while (iterator.hasNext()) {
			Attribute attribute = iterator.next();
			Attribute correspondingAttribute = exampleSet1.getAttributes().get(attribute.getName());
			if (correspondingAttribute == null) {
				newAttributesForES1.add(AttributeFactory.createAttribute(attribute.getName(), attribute.getValueType()));
			} else {
				// Attribute already present in both sets. Check if roles are the same:
				if (includeSpecials) {
					String thisRole = exampleSet2.getAttributes().getRole(attribute).getSpecialName();
					String otherRole = exampleSet1.getAttributes().getRole(correspondingAttribute).getSpecialName();
					if (!(thisRole == null ? otherRole == null : thisRole.equals(otherRole))) {
						throw new UserError(this, "superset.incompatible_roles", correspondingAttribute.getName(),
								thisRole != null ? thisRole : "regular", otherRole != null ? otherRole : "regular");
					}
				}
			}
		}
		return newAttributesForES1;
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet1 = (ExampleSet) exampleSet1Input.getData(ExampleSet.class).clone();
		ExampleSet exampleSet2 = (ExampleSet) exampleSet2Input.getData(ExampleSet.class).clone();
		superset(exampleSet1, exampleSet2);
		supersetOutput1.deliver(exampleSet1);
		supersetOutput2.deliver(exampleSet2);
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPorts().getPortByIndex(0),
				ExampleSetSuperset.class, null);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeBoolean(
				PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES,
				"Indicates if special attributes are to be considered. Note that an error will be thrown if two differently named or typed attributes with the same role exist.",
				false);
		types.add(type);

		return types;
	}
}
