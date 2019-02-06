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
package com.rapidminer.operator.learner.igss.hypothesis;

import com.rapidminer.example.Attribute;

import java.io.Serializable;


/**
 * Objects of this class represent a literal in a conjunctive rule.
 * 
 * @author Dirk Dach
 */
public class Literal implements Serializable {

	private static final long serialVersionUID = 8699112785374243703L;

	/** The attribute tested in this literal. */
	private Attribute attribute;

	/** The value of the attribute for this literal. */
	private int value;

	/** The literals' index (1.dimension) in the allLiterals[][] array of the class ConjunctiveRule. */
	private int index;

	/** Counts the total number literals that have been constructed. */
	private static int numberOfLiterals;

	/** Constructs a new Literal. */
	public Literal(Attribute a, int v) {
		attribute = a;
		value = v;
		numberOfLiterals++;
	}

	/** Constructs a new Literal. */
	public Literal(Attribute a, int v, int i) {
		attribute = a;
		value = v;
		index = i;
		numberOfLiterals++;
	}

	/** Returns the attribute of this literals. */
	public Attribute getAttribute() {
		return attribute;
	}

	/** Returns the index of the value of this literals' attribute. */
	public int getValue() {
		return value;
	}

	/**
	 * Returns the literals' index(1.dimension) in the allLiterals[][] array of the class
	 * ConjunctiveRule.
	 */
	public int getIndex() {
		return index;
	}

	/** Returns true if both attributes and both values are equal. */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Literal)) {
			return false;
		}
		Literal otherLiteral = (Literal) o;
		if ((this.attribute.equals(otherLiteral.attribute)) && (this.value == otherLiteral.value)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.attribute.hashCode() ^ Integer.valueOf(this.value);
	}

	/** Returns a String represenation of this Literal. */
	@Override
	public String toString() {
		String str = this.attribute.getMapping().mapIndex(value);
		return "(" + attribute.getName() + "=" + str + ")";
	}
}
