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
 * This transformation returns the remapped value, remapping from the current nominal mapping of the
 * attribute to the given {@link #overlayedMapping}. {@link #transform(Attribute, double)} maps the
 * given double value to a tranformed value that represents the same nominal value in
 * {@link #overlayedMapping} as the given value represents in the current attribute mapping.
 *
 * @author Ingo Mierswa
 */
public class AttributeTransformationRemapping implements AttributeTransformation {

	private static final long serialVersionUID = 1L;

	private NominalMapping overlayedMapping;

	public AttributeTransformationRemapping(NominalMapping overlayedMapping) {
		this.overlayedMapping = overlayedMapping;
	}

	public AttributeTransformationRemapping(AttributeTransformationRemapping other) {
		this.overlayedMapping = (NominalMapping) other.overlayedMapping.clone();
	}

	@Override
	public Object clone() {
		return new AttributeTransformationRemapping(this);
	}

	public void setNominalMapping(NominalMapping mapping) {
		this.overlayedMapping = mapping;
	}

	@Override
	public double transform(Attribute attribute, double value) {
		if (Double.isNaN(value)) {
			return value;
		}
		if (attribute.isNominal()) {
			try {
				String nominalValue = attribute.getMapping().mapIndex((int) value);
				int index = overlayedMapping.getIndex(nominalValue);
				if (index < 0) {
					return Double.NaN;
					// return value;
				} else {
					return index;
				}
			} catch (AttributeTypeException e) {
				return Double.NaN;
				// throw new AttributeTypeException("Attribute '" + attribute.getName() + "': " +
				// e.getMessage());
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
				String nominalValue = overlayedMapping.mapIndex((int) value);
				int newValue = attribute.getMapping().getIndex(nominalValue);
				if (newValue < 0) {
					return value;
				} else {
					return newValue;
				}
			} catch (AttributeTypeException e) {
				return value;
				// throw new AttributeTypeException("Attribute '" + attribute.getName() + "': " +
				// e.getMessage());
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
