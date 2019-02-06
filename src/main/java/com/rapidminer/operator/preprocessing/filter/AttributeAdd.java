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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;

import java.util.List;


/**
 * This operator creates a new attribute for the data set. The new attribute will have the specified
 * name and value type (e.g. nominal or real). Please note that all values are missing right after
 * creation and therefore operators like SetData must be used to change this.
 * 
 * @author Ingo Mierswa
 */
public class AttributeAdd extends AbstractDataProcessing {

	public static final String PARAMETER_NAME = "name";

	public static final String PARAMETER_VALUE_TYPE = "value_type";

	public AttributeAdd(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		AttributeMetaData amd = new AttributeMetaData(getParameterAsString(PARAMETER_NAME),
				getParameterAsInt(PARAMETER_VALUE_TYPE));
		amd.setNumberOfMissingValues(metaData.getNumberOfExamples());
		metaData.addAttribute(amd);
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		String name = getParameterAsString(PARAMETER_NAME);
		int valueType = getParameterAsInt(PARAMETER_VALUE_TYPE);

		Attribute attribute = AttributeFactory.createAttribute(name, valueType);
		exampleSet.getExampleTable().addAttribute(attribute);
		exampleSet.getAttributes().addRegular(attribute);

		for (Example example : exampleSet) {
			example.setValue(attribute, Double.NaN);
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_NAME, "The name of the new attribute.", false));
		types.add(new ParameterTypeCategory(PARAMETER_VALUE_TYPE, "The value type of the new attribute.",
				Ontology.VALUE_TYPE_NAMES, Ontology.REAL));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), AttributeAdd.class, null);
	}
}
