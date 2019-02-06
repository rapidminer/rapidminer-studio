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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
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
 * Merges two nominal values of a given regular attribute. To process special attributes like
 * labels, wrap this operator by an AttributeSubsetPreprocessing operator with the parameter
 * process_special_attributes enabled.
 *
 * @author Ingo Mierswa
 */
public class MergeNominalValues extends AbstractDataProcessing {

	/**
	 * The parameter name for &quot;The name of the nominal attribute which values should be
	 * merged.&quot;
	 */
	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	/** The parameter name for &quot;The first value which should be merged.&quot; */
	public static final String PARAMETER_FIRST_VALUE = "first_value";

	/** The parameter name for &quot;The second value which should be merged.&quot; */
	public static final String PARAMETER_SECOND_VALUE = "second_value";

	public MergeNominalValues(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(
						this, PARAMETER_ATTRIBUTE_NAME)));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		AttributeMetaData targetAttribute = metaData.getAttributeByName(getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		if (targetAttribute != null) {
			Set<String> valueSet = targetAttribute.getValueSet();
			String first = getParameterAsString(PARAMETER_FIRST_VALUE);
			String second = getParameterAsString(PARAMETER_SECOND_VALUE);
			valueSet.remove(first);
			valueSet.remove(second);
			valueSet.add(first + "_" + second);
		}

		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		String name = getParameterAsString(PARAMETER_ATTRIBUTE_NAME);
		Attribute attribute = null;
		for (Attribute current : exampleSet.getAttributes()) {
			if (current.getName().equals(name)) {
				if (!current.isNominal()) {
					throw new UserError(this, 119, new Object[] { name, this.getName() });
				}
				attribute = current;
				break;
			}
		}

		if (attribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME, name);
		} else {
			String firstValue = getParameterAsString(PARAMETER_FIRST_VALUE);
			String secondValue = getParameterAsString(PARAMETER_SECOND_VALUE);
			mergeValues(exampleSet, attribute, firstValue, secondValue);
		}

		return exampleSet;
	}

	private void mergeValues(ExampleSet exampleSet, Attribute attribute, String firstValue, String secondValue)
			throws OperatorException {
		Attribute newAttribute = AttributeFactory.createAttribute(attribute, "merged");
		// clone is necessary here!
		NominalMapping mapping = (NominalMapping) attribute.getMapping().clone();
		mapping.clear();
		newAttribute.setMapping(mapping);

		exampleSet.getExampleTable().addAttribute(newAttribute);
		exampleSet.getAttributes().addRegular(newAttribute);

		double first = attribute.getMapping().mapString(firstValue);
		double second = attribute.getMapping().mapString(secondValue);
		String firstPlusSecondString = firstValue + "_" + secondValue;
		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			double value = example.getValue(attribute);
			if (Double.isNaN(value)) {
				example.setValue(newAttribute, Double.NaN);
			} else if (value == first || value == second) {
				example.setValue(newAttribute, newAttribute.getMapping().mapString(firstPlusSecondString));
			} else {
				example.setValue(newAttribute,
						newAttribute.getMapping().mapString(attribute.getMapping().mapIndex((int) value)));
			}
			checkForStop();
		}
		exampleSet.getAttributes().remove(attribute);
		newAttribute.setName(attribute.getName());
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME,
				"The name of the nominal attribute which values should be merged.", getExampleSetInputPort(), false));
		types.add(new ParameterTypeString(PARAMETER_FIRST_VALUE, "The first value which should be merged.", false));
		types.add(new ParameterTypeString(PARAMETER_SECOND_VALUE, "The second value which should be merged.", false));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), MergeNominalValues.class,
				null);
	}
}
