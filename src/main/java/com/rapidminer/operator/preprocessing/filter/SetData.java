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

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
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
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator simply sets the value for the specified example and attribute to the given value.
 *
 * If offers the possibility to define more than one attribute value pair.
 *
 *
 * @author Ingo Mierswa, Sebastian Land
 */
public class SetData extends AbstractDataProcessing {

	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	public static final String PARAMETER_EXAMPLE_INDEX = "example_index";

	public static final String PARAMETER_COUNT_BACKWARDS = "count_backwards";

	public static final String PARAMETER_VALUE = "value";

	public static final String PARAMETER_ADDITIONAL_VALUES = "additional_values";

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public SetData(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(
						this, PARAMETER_ATTRIBUTE_NAME)));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		// performing meta data propagation for single attribute definition
		if (isParameterSet(PARAMETER_VALUE) && isParameterSet(PARAMETER_ATTRIBUTE_NAME)) {
			AttributeMetaData targetAttribute = metaData.getAttributeByName(getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
			setMetaData(getParameterAsString(PARAMETER_VALUE), targetAttribute, "parameter_must_be_numerical",
					new Object[] { PARAMETER_VALUE });
		}

		// now doing same for all other defined values
		List<String[]> list = getParameterList(PARAMETER_ADDITIONAL_VALUES);
		for (String[] pair : list) {
			AttributeMetaData targetAttribute = metaData.getAttributeByName(pair[0]);
			setMetaData(pair[1], targetAttribute, "parameter_list_must_be_numerical",
					new Object[] { PARAMETER_ADDITIONAL_VALUES });
		}

		return metaData;
	}

	private void setMetaData(String value, AttributeMetaData targetAttribute, String i18nCode, Object[] i18nArguments) {
		if (targetAttribute != null) {
			if (targetAttribute.isNominal()) {
				targetAttribute.getValueSet().add(value);
			} else {
				try {
					targetAttribute.getValueRange().add(Double.parseDouble(value));
				} catch (NumberFormatException e) {
					// add warning
					addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), i18nCode, i18nArguments));
				}
			}
		}
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		Attributes attributes = exampleSet.getAttributes();

		// searching example by index
		int exampleIndex = getParameterAsInt(PARAMETER_EXAMPLE_INDEX);
		if (exampleIndex == 0) {
			throw new UserError(this, 207, new Object[] { "0", PARAMETER_EXAMPLE_INDEX,
					"only positive or negative indices are allowed" });
		}
		if (getParameterAsBoolean(PARAMETER_COUNT_BACKWARDS)) {
			exampleIndex = exampleSet.size() - exampleIndex;
		} else {
			exampleIndex--;
		}
		if (exampleIndex >= exampleSet.size()) {
			throw new UserError(this, 110, exampleIndex);
		}
		Example example = exampleSet.getExample(exampleIndex);

		// now set single value of first parameter
		if (isParameterSet(PARAMETER_ATTRIBUTE_NAME) && isParameterSet(PARAMETER_VALUE)) {
			String attributeName = getParameter(PARAMETER_ATTRIBUTE_NAME);
			String value = getParameterAsString(PARAMETER_VALUE);

			setData(example, attributeName, value, attributes, PARAMETER_ATTRIBUTE_NAME);
		}

		// now set each defined additional value.
		List<String[]> list = getParameterList(PARAMETER_ADDITIONAL_VALUES);
		for (String[] pair : list) {
			setData(example, pair[0], pair[1], attributes, PARAMETER_ADDITIONAL_VALUES);
		}

		return exampleSet;
	}

	private void setData(Example example, String attributeName, String value, Attributes attributes, String paramKey)
			throws UserError {
		Attribute attribute = attributes.get(attributeName);
		if (attribute == null) {
			throw new AttributeNotFoundError(this, paramKey, attributeName);
		}

		if (attribute.isNominal()) {
			example.setValue(attribute, attribute.getMapping().mapString(value));
		} else {
			try {
				double doubleValue = Double.parseDouble(value);
				example.setValue(attribute, doubleValue);
			} catch (NumberFormatException e) {
				throw new UserError(this, 211, PARAMETER_VALUE, value);
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_EXAMPLE_INDEX,
				"The index of the example for which the value should be set. Counting starts at 1.", 1, Integer.MAX_VALUE,
				false);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(
				PARAMETER_COUNT_BACKWARDS,
				"If checked, the last counting order is inverted and hence the last example is addressed by index 1, the before last by index 2 and so on.",
				false, true));

		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME,
				"The name of the attribute for which the value should be set.", getExampleSetInputPort(), true, false));
		types.add(new ParameterTypeString(PARAMETER_VALUE, "The value which should be set.", true, false));

		types.add(new ParameterTypeList(PARAMETER_ADDITIONAL_VALUES,
				"This list allows to set additional values of the addressed example.", new ParameterTypeAttribute(
						PARAMETER_ATTRIBUTE_NAME, "The name of the attribute for which the value should be set.",
						getExampleSetInputPort(), true), new ParameterTypeString(PARAMETER_VALUE,
						"The value which should be set.", true, false), false));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		if (getCompatibilityLevel().isAbove(VERSION_MAY_WRITE_INTO_DATA)) {
			return true;
		} else {
			// old version: true only if original output port is connected
			return isOriginalOutputConnected();
		}
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), SetData.class, null);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}
}
