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
package com.rapidminer.tools.math.container;

import com.rapidminer.tools.Tools;

import java.io.Serializable;


/**
 * @author Sebastian Land
 */
public class Range implements Serializable {

	private static final long serialVersionUID = 1L;

	private double lower;
	private double upper;

	public Range() {
		this.lower = Double.NaN;
		this.upper = Double.NaN;
	}

	public Range(double lowerBound, double upperBound) {
		if (lowerBound > upperBound) {
			// TODO: This must be resolved but currently causes some read operators to quit
			// operation.
			// Needs to find solution for this before making it public.
			// throw new IllegalArgumentException("Range was tried to initialized with a " +
			// "lower bound > upper bound. Lower bound = "+lowerBound+" Upper = "+upperBound+".");
		}
		this.lower = lowerBound;
		this.upper = upperBound;
	}

	public Range(Range valueRange) {
		if (valueRange != null) {
			this.lower = valueRange.getLower();
			this.upper = valueRange.getUpper();
		} else {
			this.lower = Double.NEGATIVE_INFINITY;
			this.upper = Double.POSITIVE_INFINITY;
		}

	}

	/**
	 * This method increases the range size, if the value is not lying in between
	 */
	public void add(double value) {
		if (value < lower || Double.isNaN(lower)) {
			lower = value;
		}
		if (value > upper || Double.isNaN(upper)) {
			upper = value;
		}
	}

	public void union(Range range) {
		add(range.getLower());
		add(range.getUpper());
	}

	public boolean contains(double value) {
		return value > lower && value < upper;
	}

	@Override
	public String toString() {
		if (Double.isNaN(lower) || Double.isNaN(upper)) {
			return "\u2205";
		}
		return "[" + Tools.formatIntegerIfPossible(lower) + " \u2013 " + Tools.formatIntegerIfPossible(upper) + "]";
	}

	public double getUpper() {
		return upper;
	}

	public double getLower() {
		return lower;
	}

	public double getSize() {
		return upper - lower;
	}

	public void setRange(double lower, double upper) {
		this.lower = lower;
		this.upper = upper;
	}

	public void setRange(Range theNewRange) {
		setRange(theNewRange.getLower(), theNewRange.getUpper());
	}

	public void setLower(double newLowerBound) {
		this.lower = newLowerBound;
	}

	public void setUpper(double newUpperBound) {
		this.upper = newUpperBound;
	}

	@Override
	public boolean equals(Object range) {
		if (range instanceof Range) {
			Range other = (Range) range;
			return upper == other.upper && lower == other.lower;
		}
		return false;
	}

	public boolean contains(Range range) {
		return (this.lower <= range.lower && this.upper >= range.upper);
	}

}
