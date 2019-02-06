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
package com.rapidminer.operator.learner.subgroups.hypothesis;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;

import java.io.Serializable;


/**
 * A literator for a rule.
 * 
 * @author Tobias Malbrecht
 */
public class Literal implements Serializable {

	private static final long serialVersionUID = 8699112785374243323L;

	private Attribute attribute = null;

	private double value = 0.0d;

	public Literal(Attribute attribute, double value) {
		this.attribute = attribute;
		this.value = value;
	}

	public boolean applicable(Example example) {
		return (example.getValue(attribute) == value);
	}

	public Attribute getAttribute() {
		return attribute;
	}

	public double getValue() {
		return value;
	}

	public String getValueAsString() {
		return attribute.getMapping().mapIndex((int) value);
	}

	@Override
	public int hashCode() {
		return this.attribute.hashCode() ^ Integer.valueOf((int) value);
	}

	public boolean contradicts(Literal otherLiteral) {
		if (this.attribute.equals(otherLiteral.attribute) && (this.value != otherLiteral.value)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object object) {
		if (object == null) {
			return false;
		}
		if (getClass() != object.getClass()) {
			return false;
		}
		Literal otherLiteral = (Literal) object;
		if (attribute.equals(otherLiteral.attribute) && (this.value == otherLiteral.value)) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return attribute.getName() + "=" + attribute.getMapping().mapIndex((int) value);
	}
}
