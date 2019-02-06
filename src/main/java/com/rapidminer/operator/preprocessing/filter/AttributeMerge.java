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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator merges two attributes by simply concatenating the values and store those new values
 * in a new attribute which will be nominal. If the resulting values are actually numerical, you
 * could simply change the value type afterwards with the corresponding operators.
 *
 * @author Ingo Mierswa
 */
public class AttributeMerge extends AbstractDataProcessing {

	public static final String PARAMETER_FIRST_ATTRIBUTE = "first_attribute";

	public static final String PARAMETER_SECOND_ATTRIBUTE = "second_attribute";

	public static final String PARAMETER_SEPARATOR = "separator";

	public static final String PARAMETER_TRIM_VALUES = "trim_values";

	public AttributeMerge(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(
						this, PARAMETER_FIRST_ATTRIBUTE, PARAMETER_SECOND_ATTRIBUTE)));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		try {
			String attributeName1 = getParameterAsString(PARAMETER_FIRST_ATTRIBUTE);
			String attributeName2 = getParameterAsString(PARAMETER_SECOND_ATTRIBUTE);
			String separation = getParameterAsString(PARAMETER_SEPARATOR);
			AttributeMetaData amd = new AttributeMetaData(attributeName1 + separation + attributeName2, Ontology.NOMINAL,
					null);
			amd.setValueSetRelation(SetRelation.UNKNOWN);
			metaData.addAttribute(amd);

			AttributeMetaData amd1 = metaData.getAttributeByName(attributeName1);
			AttributeMetaData amd2 = metaData.getAttributeByName(attributeName2);
			if (amd1 != null && amd2 != null) {
				if (amd1.isNominal() && amd2.isNominal()) {
					if (amd1.getValueSetRelation() == SetRelation.EQUAL && amd2.getValueSetRelation() == SetRelation.EQUAL) {
						Set<String> valueSet = new HashSet<>();
						for (String value1 : amd1.getValueSet()) {
							for (String value2 : amd2.getValueSet()) {
								valueSet.add(value1 + separation + value2);
							}
						}
						amd.setValueSet(valueSet, SetRelation.SUPERSET);
					}
				}
			}
			return metaData;
		} catch (UndefinedParameterError e) {
			return metaData;
		}
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		String firstAttributeName = getParameterAsString(PARAMETER_FIRST_ATTRIBUTE);
		String secondAttributeName = getParameterAsString(PARAMETER_SECOND_ATTRIBUTE);
		String separatorString = getParameterAsString(PARAMETER_SEPARATOR);
		boolean trimValues = getParameterAsBoolean(PARAMETER_TRIM_VALUES);

		Attribute firstAttribute = exampleSet.getAttributes().get(firstAttributeName);
		if (firstAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_FIRST_ATTRIBUTE, firstAttributeName);
		}

		Attribute secondAttribute = exampleSet.getAttributes().get(secondAttributeName);
		if (secondAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_SECOND_ATTRIBUTE, secondAttributeName);
		}

		Attribute mergedAttribute = AttributeFactory.createAttribute(firstAttribute.getName() + separatorString
				+ secondAttribute.getName(), Ontology.NOMINAL);
		exampleSet.getExampleTable().addAttribute(mergedAttribute);
		exampleSet.getAttributes().addRegular(mergedAttribute);

		for (Example example : exampleSet) {
			double firstValue = example.getValue(firstAttribute);
			double secondValue = example.getValue(secondAttribute);

			if (Double.isNaN(firstValue) || Double.isNaN(secondValue)) {
				example.setValue(mergedAttribute, Double.NaN);
			} else {
				String firstValueString = example.getValueAsString(firstAttribute);
				String secondValueString = example.getValueAsString(secondAttribute);
				String mergedValueString = null;
				if (trimValues) {
					mergedValueString = firstValueString.trim() + separatorString.trim() + secondValueString.trim();
				} else {
					mergedValueString = firstValueString + separatorString + secondValueString;
				}
				double mergedValue = mergedAttribute.getMapping().mapString(mergedValueString);
				example.setValue(mergedAttribute, mergedValue);
			}
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_FIRST_ATTRIBUTE, "The first attribute of this merger.",
				getExampleSetInputPort(), false));
		types.add(new ParameterTypeAttribute(PARAMETER_SECOND_ATTRIBUTE, "The second attribute of this merger.",
				getExampleSetInputPort(), false));

		types.add(new ParameterTypeString(PARAMETER_SEPARATOR,
				"Indicated a string which is used as separation of both values.", "_"));
		types.add(new ParameterTypeBoolean(
				PARAMETER_TRIM_VALUES,
				"Indicates if the two values should be trimmed, i.e. leading and trailing whitespaces should be removed, before the merge is performed.",
				false));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler
				.getResourceConsumptionEstimator(getInputPort(), AttributeMerge.class, null);
	}
}
