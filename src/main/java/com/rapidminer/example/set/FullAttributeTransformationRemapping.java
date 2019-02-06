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
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.table.NominalMapping;


/**
 * This transformation returns the remapped value, remapping from the given {@link #baseMapping} to
 * the current mapping of the attribute. {@link #transform(Attribute, double)} maps the given double
 * value to a tranformed value that represents the same nominal value in the current attribute
 * mapping as the given value represents in the {@link #baseMapping}. This transformation should be
 * used if the mapping of an attribute is changed. In that case, use the old mapping as
 * {@link #baseMapping} in this transformation.
 *
 * @author Ingo Mierswa, Gisa Schaefer
 * @since 7.4
 */
public class FullAttributeTransformationRemapping implements AttributeTransformation {

	private static final long serialVersionUID = 1L;

	private NominalMapping baseMapping;

	public FullAttributeTransformationRemapping(NominalMapping baseMapping) {
		this.baseMapping = baseMapping;
	}

	public FullAttributeTransformationRemapping(FullAttributeTransformationRemapping other) {
		this.baseMapping = (NominalMapping) other.baseMapping.clone();
	}

	@Override
	public Object clone() {
		return new FullAttributeTransformationRemapping(this);
	}

	public void setNominalMapping(NominalMapping mapping) {
		this.baseMapping = mapping;
	}

	@Override
	public double transform(Attribute attribute, double value) {
		if (Double.isNaN(value)) {
			return value;
		}
		if (attribute.isNominal()) {
			try {
				String nominalValue = baseMapping.mapIndex((int) value);
				int index = attribute.getMapping().getIndex(nominalValue);
				if (index < 0) {
					return Double.NaN;
				} else {
					return index;
				}
			} catch (AttributeTypeException e) {
				return Double.NaN;
			}
		} else {
			return value;
		}
	}

	@Override
	public double inverseTransform(Attribute attribute, double value) {
		if (Double.isNaN(value)) {
			return value;
		}
		if (attribute.isNominal()) {
			try {
				String nominalValue = attribute.getMapping().mapIndex((int) value);
				int newValue = baseMapping.getIndex(nominalValue);
				if (newValue < 0) {
					return value;
				} else {
					return newValue;
				}
			} catch (AttributeTypeException e) {
				return value;
			}
		} else {
			return value;
		}
	}

	@Override
	public boolean isReversable() {
		return true;
	}
}
