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
package com.rapidminer.operator.learner.tree;

import com.rapidminer.example.Attribute;


/**
 * Encapsulates some information about the benefit of a split.
 * 
 * 
 * @author Ingo Mierswa
 */
public class Benefit implements Comparable<Benefit> {

	private Attribute attribute;

	private double benefit;

	private double splitValue;

	public Benefit(double benefit, Attribute attribute) {
		this(benefit, attribute, Double.NaN);
	}

	public Benefit(double benefit, Attribute attribute, double splitValue) {
		this.benefit = benefit;
		this.attribute = attribute;
		this.splitValue = splitValue;
	}

	public Attribute getAttribute() {
		return this.attribute;
	}

	public double getSplitValue() {
		return this.splitValue;
	}

	public double getBenefit() {
		return this.benefit;
	}

	@Override
	public String toString() {
		return "Attribute = " + attribute.getName() + ", benefit = " + benefit
				+ (!Double.isNaN(splitValue) ? ", split = " + splitValue : "");
	}

	@Override
	public int compareTo(Benefit o) {
		return -1 * Double.compare(this.benefit, o.benefit);
	}
}
