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
import com.rapidminer.tools.container.Tupel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * A simple model which can be used to transform all regular attributes into a value range between
 * the given min and max values.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public class MinMaxNormalizationModel extends AbstractNormalizationModel {

	private static final long serialVersionUID = 5620317015578777169L;

	/** The minimum value for each attribute after normalization. */
	private final double min;

	/** The maximum value for each attribute after normalization. */
	private final double max;
	private final HashMap<String, Tupel<Double, Double>> attributeRanges;
	private final Set<String> attributeNames;

	/** Create a new normalization model. */
	public MinMaxNormalizationModel(ExampleSet exampleSet, double min, double max,
			HashMap<String, Tupel<Double, Double>> attributeRanges) {
		super(exampleSet);
		this.min = min;
		this.max = max;
		this.attributeRanges = attributeRanges;
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
		Tupel<Double, Double> ranges = attributeRanges.get(targetAttribute.getName());
		double minA = ranges.getFirst().doubleValue();
		double maxA = ranges.getSecond().doubleValue();
		if (maxA == minA || min == max) {
			return Math.min(Math.max(minA, min), max);
		} else {
			return (value - minA) / (maxA - minA) * (max - min) + min;
		}
	}

	/**
	 * Returns a nicer name. Necessary since this model is defined as inner class.
	 */
	@Override
	public String getName() {
		return "MinMaxNormalizationModel";
	}

	/** Returns a string representation of this model. */
	@Override
	public String toString() {
		return "Normalize between " + this.min + " and " + this.max;
	}
	
	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public Map<String, Tupel<Double, Double>> getAttributeRanges() {
		return attributeRanges;
	}

	public Set<String> getAttributeNames() {
		return attributeNames;
	}
}
