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
package com.rapidminer.operator.features.construction;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.AttributeValueFilter;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.NumberParser;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * Generates a new attribute and sets the attribute's values according to the fulfilling of the
 * specified conditions. Sets the attribute value the first value, which condition is matched.
 * 
 * <p>
 * The parameter string must have the form <code>attribute op value</code>, where attribute is a
 * name of an attribute, value is a value the attribute can take and op is one of the binary logical
 * operators similar to the ones known from Java, e.g. greater than or equals. Please note your can
 * define a logical OR of several conditions with || and a logical AND of two conditions with two
 * ampersand. Please note also that for nominal attributes you can define a regular expression for
 * value of the possible equal and not equal checks.
 * </p>
 * 
 * @author Tobias Malbrecht
 */
public class ConditionedFeatureGeneration extends AbstractFeatureConstruction {

	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	public static final String PARAMETER_VALUE_TYPE = "value_type";

	public static final String PARAMETER_VALUES = "values";

	public static final String PARAMETER_CONDITIONS = "conditions";

	public static final String PARAMETER_DEFAULT_VALUE = "default_value";

	public ConditionedFeatureGeneration(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {

		try {
			AttributeMetaData amd = new AttributeMetaData(getParameterAsString(PARAMETER_ATTRIBUTE_NAME),
					getParameterAsInt(PARAMETER_VALUE_TYPE) + 1);
			List<String[]> valueConditionList = getParameterList(PARAMETER_VALUES);
			if (amd.isNominal()) {
				// run through all parameters and adding values
				Set<String> values = new HashSet<String>();
				for (String[] pair : valueConditionList) {
					values.add(pair[0]);
				}
				amd.setValueSet(values, SetRelation.EQUAL);
			} else {
				Range range = new Range();
				String defaultValue = getParameterAsString(PARAMETER_DEFAULT_VALUE);
				try {
					double value = Double.parseDouble(defaultValue);
					range.add(value);
				} catch (NumberFormatException e) {
					addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), "parameter_must_be_numerical",
							PARAMETER_DEFAULT_VALUE));
				}
				boolean threwError = false;
				for (String[] pair : valueConditionList) {
					try {
						double value = Double.parseDouble(pair[0]);
						range.add(value);
					} catch (NumberFormatException e) {
						if (!threwError) {
							addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(),
									"parameter_must_be_numerical", PARAMETER_VALUES));
							threwError = true;
						}
					}
				}
				amd.setValueRange(range, SetRelation.EQUAL);
			}
			metaData.addAttribute(amd);
		} catch (UndefinedParameterError e) {
		}

		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		Attribute attribute = AttributeFactory.createAttribute(getParameterAsString(PARAMETER_ATTRIBUTE_NAME),
				getParameterAsInt(PARAMETER_VALUE_TYPE) + 1);

		double mappedDefaultValue = Double.NaN;
		String defaultValue = getParameterAsString(PARAMETER_DEFAULT_VALUE);
		if (!defaultValue.equals("?")) {
			if (attribute.isNominal()) {
				mappedDefaultValue = attribute.getMapping().mapString(defaultValue);
			} else {
				try {
					mappedDefaultValue = NumberParser.parseDouble(defaultValue);
				} catch (NumberFormatException e) {
					logError("default value has to be ? or numerical for numerical attributes: no feature is generated");
					return exampleSet;
				}
			}
		}

		List<String[]> valueConditionList = getParameterList(PARAMETER_VALUES);
		int numberOfValueConditions = valueConditionList.size();
		String[] values = new String[numberOfValueConditions];
		double[] mappedValues = new double[numberOfValueConditions];
		AttributeValueFilter[] filters = new AttributeValueFilter[numberOfValueConditions];
		Iterator<String[]> iterator = valueConditionList.iterator();
		int j = 0;
		while (iterator.hasNext()) {
			String[] pair = iterator.next();
			values[j] = pair[0];
			if (values[j].equals("?")) {
				mappedValues[j] = Double.NaN;
			} else {
				if (attribute.isNominal()) {
					mappedValues[j] = attribute.getMapping().mapString(values[j]);
				} else {
					try {
						mappedValues[j] = Double.parseDouble(values[j]);
					} catch (NumberFormatException e) {
						logError("values have to be numerical for numerical attributes: no feature is generated");
						return exampleSet;
					}
				}
			}
			filters[j] = new AttributeValueFilter(exampleSet, pair[1]);
			j++;
		}

		exampleSet.getExampleTable().addAttribute(attribute);
		exampleSet.getAttributes().addRegular(attribute);

		for (Example example : exampleSet) {
			example.setValue(attribute, mappedDefaultValue);
			for (int i = 0; i < numberOfValueConditions; i++) {
				AttributeValueFilter filter = filters[i];
				if (filter.conditionOk(example)) {
					example.setValue(attribute, mappedValues[i]);
					break;
				}
			}
		}

		exampleSet.recalculateAllAttributeStatistics();
		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeString(PARAMETER_ATTRIBUTE_NAME, "The name of the generated attribute.");
		type.setExpert(false);
		types.add(type);
		String[] valueTypes = new String[Ontology.VALUE_TYPE_NAMES.length - 1];
		for (int i = 1; i < Ontology.VALUE_TYPE_NAMES.length; i++) {
			valueTypes[i - 1] = Ontology.VALUE_TYPE_NAMES[i];
		}
		type = new ParameterTypeCategory(PARAMETER_VALUE_TYPE, "Value type of the created attribute.", valueTypes, 0);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeList(PARAMETER_VALUES, "Values and conditions.", new ParameterTypeString("result_value",
				"The value of the attribute if the condition matches."), new ParameterTypeString(PARAMETER_CONDITIONS,
				"Value condition.", false));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeString(PARAMETER_DEFAULT_VALUE, "Default value.", "?");
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
