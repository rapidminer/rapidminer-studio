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
package com.rapidminer.example.set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeTransformation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * This transformation simply returns the same value.
 * 
 * @author Ingo Mierswa
 */
public class AttributeTransformationReplaceMissing implements AttributeTransformation {

	private static final long serialVersionUID = 1L;

	private Map<String, Double> replacementMap;

	public AttributeTransformationReplaceMissing(Map<String, Double> replacementMap) {
		this.replacementMap = replacementMap;
	}

	public AttributeTransformationReplaceMissing(AttributeTransformationReplaceMissing other) {
		this.replacementMap = new HashMap<String, Double>();
		Iterator<Map.Entry<String, Double>> i = other.replacementMap.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<String, Double> entry = i.next();
			this.replacementMap.put(entry.getKey(), Double.valueOf(entry.getValue()));
		}
	}

	@Override
	public Object clone() {
		return new AttributeTransformationReplaceMissing(this);
	}

	public void setReplacementMap(Map<String, Double> replacementMap) {
		this.replacementMap = replacementMap;
	}

	@Override
	public double inverseTransform(Attribute attribute, double value) {
		return value;
	}

	@Override
	public boolean isReversable() {
		return false;
	}

	@Override
	public double transform(Attribute attribute, double value) {
		if (Double.isNaN(value)) {
			Double replacement = replacementMap.get(attribute.getName());
			if (replacement != null) {
				return replacement;
			} else {
				return value;
			}
		} else {
			return value;
		}
	}
}
