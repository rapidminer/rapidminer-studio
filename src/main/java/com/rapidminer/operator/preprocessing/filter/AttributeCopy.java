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
package com.rapidminer.operator.preprocessing.filter;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * Adds a copy of a single attribute to the given example set.
 *
 * @author Ingo Mierswa
 */
public class AttributeCopy extends AbstractDataProcessing {

	/**
	 * The parameter name for &quot;The name of the nominal attribute to which values should be
	 * added.&quot;
	 */
	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	/**
	 * The parameter name for &quot;The name of the new (copied) attribute. If this parameter is
	 * missing, simply the same name with an appended number is used.&quot;
	 */
	public static final String PARAMETER_NEW_NAME = "new_name";

	public AttributeCopy(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(
						this, PARAMETER_ATTRIBUTE_NAME)));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {

		AttributeMetaData attributeByName = metaData.getAttributeByName(getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		if (attributeByName != null) {
			AttributeMetaData newAttribute = attributeByName.copy();
			newAttribute.setRole(null);
			if (isParameterSet(PARAMETER_NEW_NAME)) {
				newAttribute.setName(getParameterAsString(PARAMETER_NEW_NAME));
			} else {
				newAttribute.setName("copy(" + newAttribute.getName() + ")");
			}
			metaData.addAttribute(newAttribute);
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		Attribute attribute = exampleSet.getAttributes().get(getParameterAsString(PARAMETER_ATTRIBUTE_NAME));

		// some checks
		if (attribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME, getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		}

		// copy attribute
		Attribute newAttribute = AttributeFactory.createAttribute(attribute);
		String newName = getParameterAsString(PARAMETER_NEW_NAME);
		if (newName != null) {
			newAttribute.setName(newName);
		} else {
			newAttribute.setName("copy(" + attribute.getName() + ")");
		}
		exampleSet.getExampleTable().addAttribute(newAttribute);
		exampleSet.getAttributes().addRegular(newAttribute);

		// copy data
		for (Example e : exampleSet) {
			e.setValue(newAttribute, e.getValue(attribute));
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME,
				"The name of the nominal attribute to which values should be added.", getExampleSetInputPort(), false));
		types.add(new ParameterTypeString(
				PARAMETER_NEW_NAME,
				"The name of the new (copied) attribute. If this parameter is missing, simply the same name with an appended number is used.",
				true, false));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), AttributeCopy.class, null);
	}
}
