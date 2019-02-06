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
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;


/**
 * This operator creates new attributes from nominal attributes where the new attributes contain the
 * trimmed original values, i.e. leading and trailing spaces will be removed.
 * 
 * @author Ingo Mierswa, Helge Homburg, Tobias Malbrecht
 */
public class AttributeValueTrim extends AbstractValueProcessing {

	public AttributeValueTrim(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException {
		LinkedHashMap<Attribute, Attribute> attributeMap = new LinkedHashMap<Attribute, Attribute>();
		for (Attribute oldAttribute : exampleSet.getAttributes()) {
			Attribute newAttribute = AttributeFactory.createAttribute(oldAttribute.getValueType());
			attributeMap.put(oldAttribute, newAttribute);
			for (String value : oldAttribute.getMapping().getValues()) {
				String trimmedValue = value.trim();
				if (trimmedValue.length() > 0) {
					newAttribute.getMapping().mapString(trimmedValue);
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
					String trimmedValue = oldAttribute.getMapping().mapIndex((int) value).trim();
					if (trimmedValue.length() == 0) {
						example.setValue(newAttribute, Double.NaN);
					} else {
						example.setValue(newAttribute, newAttribute.getMapping().mapString(trimmedValue));
					}
				}
			}
			exampleSet.getAttributes().remove(oldAttribute);
			newAttribute.setName(oldAttribute.getName());
		}

		return exampleSet;
	}

	@Override
	public ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData emd) {
		for (AttributeMetaData amd : emd.getAllAttributes()) {
			Set<String> valueSet = new TreeSet<String>();
			for (String value : amd.getValueSet()) {
				String trimmedValue = value.trim();
				if (trimmedValue.length() > 0) {
					valueSet.add(trimmedValue);
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
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), AttributeValueTrim.class,
				null);
	}
}
