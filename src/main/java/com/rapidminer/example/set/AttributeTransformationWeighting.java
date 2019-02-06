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
import com.rapidminer.example.AttributeWeights;


/**
 * This transformation simply returns the weight-scaled value. It must only be used by
 * {@link AttributeWeightedExampleSet} since this class takes care of reassigning
 * {@link #attributeWeights} after clone.
 * 
 * Usage of this Transformation for nominal attributes is forbidden, since the indices may not be
 * altered!
 * 
 * @author Ingo Mierswa
 */
public class AttributeTransformationWeighting implements AttributeTransformation {

	private static final long serialVersionUID = 1L;

	private AttributeWeights attributeWeights;

	public AttributeTransformationWeighting(AttributeWeights attributeWeights) {
		setAttributeWeights(attributeWeights);
	}

	/** Clone constructor. */
	public AttributeTransformationWeighting(AttributeTransformationWeighting other) {
		// We don't clone here. Weights are re-assigned by AttributeWeightedExampleSet.clone();
		this.attributeWeights = other.attributeWeights;
	}

	@Override
	public Object clone() {
		return new AttributeTransformationWeighting(this);
	}

	public void setAttributeWeights(AttributeWeights weights) {
		this.attributeWeights = weights;
	}

	@Override
	public double inverseTransform(Attribute attribute, double value) {
		double weight = attributeWeights.getWeight(attribute.getName());
		if (!Double.isNaN(weight)) {
			return value / weight;
		} else {
			return value;
		}
	}

	@Override
	public boolean isReversable() {
		return true;
	}

	@Override
	public double transform(Attribute attribute, double value) {
		double weight = attributeWeights.getWeight(attribute.getName());
		if (!Double.isNaN(weight)) {
			return value * weight;
		} else {
			return value;
		}
	}
}
