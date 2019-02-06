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

/**
 * Encapsulates some information about the benefit of a split.
 *
 *
 * @author Ingo Mierswa, Gisa Schaefer
 */
public class ParallelBenefit implements Comparable<ParallelBenefit> {

	private int attributeNumber;

	private double benefit;

	private double splitValue;

	public ParallelBenefit(double benefit, int attributeNumber) {
		this(benefit, attributeNumber, Double.NaN);
	}

	public ParallelBenefit(double benefit, int attributeNumber, double splitValue) {
		this.benefit = benefit;
		this.attributeNumber = attributeNumber;
		this.splitValue = splitValue;
	}

	public int getAttributeNumber() {
		return this.attributeNumber;
	}

	public double getSplitValue() {
		return this.splitValue;
	}

	public double getBenefit() {
		return this.benefit;
	}

	@Override
	public String toString() {
		return "Attribute number= " + attributeNumber + ", benefit = " + benefit
				+ (!Double.isNaN(splitValue) ? ", split = " + splitValue : "");
	}

	@Override
	public int compareTo(ParallelBenefit o) {
		// needed to guarantee that each calculation gives the same result even if they are done in
		// parallel
		if (Double.compare(this.benefit, o.benefit) == 0) {
			return Integer.compare(this.attributeNumber, o.attributeNumber);
		}
		return -1 * Double.compare(this.benefit, o.benefit);
	}
}
