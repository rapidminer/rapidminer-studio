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
package com.rapidminer.operator.preprocessing.normalization;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.SimpleAttributes;
import com.rapidminer.example.table.ViewAttribute;
import com.rapidminer.tools.Ontology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * This model is able to transform the data in a way, every transformed attribute of an example
 * contains the proportion of the total sum of this attribute over all examples.
 * 
 * @author Sebastian Land
 */
public class ProportionNormalizationModel extends AbstractNormalizationModel {

	private static final long serialVersionUID = 5620317015578777169L;

	private HashMap<String, Double> attributeSums;
	private Set<String> attributeNames;

	/** Create a new normalization model. */
	public ProportionNormalizationModel(ExampleSet exampleSet, HashMap<String, Double> attributeSums) {
		super(exampleSet);
		this.attributeSums = attributeSums;
		attributeNames = new HashSet<String>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) {
				attributeNames.add(attribute.getName());
			}
		}
	}

	@Override
	public Attributes getTargetAttributes(ExampleSet viewParent) {
		SimpleAttributes attributes = new SimpleAttributes();
		// add special attributes to new attributes
		Iterator<AttributeRole> roleIterator = viewParent.getAttributes().allAttributeRoles();
		while (roleIterator.hasNext()) {
			AttributeRole role = roleIterator.next();
			if (role.isSpecial()) {
				attributes.add(role);
			}
		}
		// add regular attributes
		for (Attribute attribute : viewParent.getAttributes()) {
			if (!attribute.isNumerical() || !attributeNames.contains(attribute.getName())) {
				attributes.addRegular(attribute);
			} else {
				// giving new attributes old name: connection to rangesMap
				attributes.addRegular(new ViewAttribute(this, attribute, attribute.getName(), Ontology.NUMERICAL, null));
			}
		}
		return attributes;
	}

	@Override
	public double getValue(Attribute targetAttribute, double value) {
		Double sum = attributeSums.get(targetAttribute.getName());
		if (sum == null) {
			return value;
		}
		return value / sum;
	}

	/**
	 * Returns a nicer name. Necessary since this model is defined as inner class.
	 */
	@Override
	public String getName() {
		return "Proportional normalization model";
	}

	/** Returns a string representation of this model. */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Normalizes all attributes proportional to their respective total sum. Attributes sums: \n");
		for (Entry<String, Double> entry : attributeSums.entrySet()) {
			buffer.append(entry.getKey() + ": " + entry.getValue().doubleValue() + "\n");
		}
		return buffer.toString();
	}
	
	public Set<String> getAttributeNames() {
		return attributeNames;
	}
	
	public Map<String, Double> getAttributeSums() {
		return attributeSums;
	}

}
