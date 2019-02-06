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
import java.util.Iterator;
import java.util.Map;


/**
 * This is the normalization model for the IQR normalization method.
 * 
 * @author Brendon Bolin, Sebastian Land
 */
public class IQRNormalizationModel extends AbstractNormalizationModel {

	private static final long serialVersionUID = 4333931155320624490L;

	private HashMap<String, Tupel<Double, Double>> attributeMeanSigmaMap;

	public IQRNormalizationModel(ExampleSet exampleSet, HashMap<String, Tupel<Double, Double>> attributeMeanSigmaMap) {
		super(exampleSet);
		this.attributeMeanSigmaMap = attributeMeanSigmaMap;
	}

	/**
	 * Returns a nicer name. Necessary since this model is defined as inner class.
	 */
	@Override
	public String getName() {
		return "IQR-Transformation";
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
			if (!attribute.isNumerical() || !attributeMeanSigmaMap.containsKey(attribute.getName())) {
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
		Tupel<Double, Double> meanSigmaTupel = attributeMeanSigmaMap.get(targetAttribute.getName());
		if (meanSigmaTupel == null) {
			return value;
		}
		double sigma = meanSigmaTupel.getSecond().doubleValue();
		if (sigma <= 0) {
			return 0;
		} else {
			return (value - meanSigmaTupel.getFirst().doubleValue()) / sigma;
		}
	}
	
	public Map<String, Tupel<Double, Double>> getAttributeMeanSigmaMap() {
		return attributeMeanSigmaMap;
	}
}
