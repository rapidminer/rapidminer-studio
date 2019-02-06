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
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.AbstractValueProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;


/**
 * This operator creates new attributes from nominal attributes where the new attributes contain
 * only substrings of the original values. Please note that the counting starts with 1 and that the
 * first and the last character will be included in the resulting substring. For example, the value
 * is &quot;RapidMiner&quot; and the first index is set to 6 and the last index is set to 9 the
 * result will be &quot;Mine&quot;. If the last index is larger than the length of the word, the
 * resulting substrings will end with the last character.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class AttributeValueSubstring extends AbstractValueProcessing {

	public static final String PARAMETER_FIRST = "first_character_index";

	public static final String PARAMETER_LAST = "last_character_index";

	public AttributeValueSubstring(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException {
		int firstIndex = getParameterAsInt(PARAMETER_FIRST);
		int lastIndex = getParameterAsInt(PARAMETER_LAST);

		LinkedHashMap<Attribute, Attribute> attributeMap = new LinkedHashMap<Attribute, Attribute>();
		for (Attribute oldAttribute : exampleSet.getAttributes()) {
			Attribute newAttribute = AttributeFactory.createAttribute(oldAttribute.getValueType());
			attributeMap.put(oldAttribute, newAttribute);
			for (String value : oldAttribute.getMapping().getValues()) {
				int actualFirst = firstIndex - 1;
				int actualLast = lastIndex <= value.length() ? lastIndex : value.length();
				String substringValue = value.substring(actualFirst, actualLast);
				if (substringValue.length() > 0) {
					newAttribute.getMapping().mapString(substringValue);
				}
			}
		}

		for (Entry<Attribute, Attribute> entry : attributeMap.entrySet()) {
			Attribute oldAttribute = entry.getKey();
			Attribute newAttribute = entry.getValue();
			exampleSet.getExampleTable().addAttribute(newAttribute);
			exampleSet.getAttributes().addRegular(newAttribute);
			for (Example example : exampleSet) {
				double value = example.getValue(oldAttribute);
				if (Double.isNaN(value)) {
					example.setValue(newAttribute, Double.NaN);
				} else {
					String stringValue = oldAttribute.getMapping().mapIndex((int) value);
					int actualFirst = firstIndex - 1;
					int actualLast = lastIndex <= stringValue.length() ? lastIndex : stringValue.length();
					if (lastIndex < firstIndex) {
						example.setValue(newAttribute, Double.NaN);
					} else {
						String substringValue = stringValue.substring(actualFirst, actualLast);
						if (substringValue.length() == 0) {
							example.setValue(newAttribute, Double.NaN);
						} else {
							example.setValue(newAttribute, substringValue);
						}
					}
				}
			}
			exampleSet.getAttributes().remove(oldAttribute);
			newAttribute.setName(oldAttribute.getName());
		}
		return exampleSet;
	}

	@Override
	public ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData emd) throws UndefinedParameterError {
		int firstIndex = getParameterAsInt(PARAMETER_FIRST);
		int lastIndex = getParameterAsInt(PARAMETER_LAST);

		for (AttributeMetaData amd : emd.getAllAttributes()) {
			Set<String> valueSet = new TreeSet<String>();
			for (String value : amd.getValueSet()) {
				int actualFirst = firstIndex - 1;
				int actualLast = lastIndex <= value.length() ? lastIndex : value.length();
				String substringValue = value.substring(actualFirst, actualLast);
				if (substringValue.length() > 0) {
					valueSet.add(substringValue);
				}
			}
			amd.setValueSet(valueSet, SetRelation.SUBSET);
		}
		return emd;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NOMINAL };
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(
				PARAMETER_FIRST,
				"The index of the first character of the substring which should be kept (counting starts with 1, 0: start with beginning of value).",
				1, Integer.MAX_VALUE, 1);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(
				PARAMETER_LAST,
				"The index of the last character of the substring which should be kept (counting starts with 1, 0: end with end of value).",
				1, Integer.MAX_VALUE, Integer.MAX_VALUE);
		type.setExpert(false);
		types.add(type);
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				AttributeValueSubstring.class, null);
	}
}
