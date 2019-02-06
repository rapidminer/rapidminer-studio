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
package com.rapidminer.tools.math;

/**
 * A peak with a index and a value. The compare method returns 1 for the peak with higher value.
 * 
 * @author Ingo Mierswa
 */
public class Peak implements Comparable<Peak> {

	private double index;

	private double magnitude;

	public Peak(double index, double magnitude) {
		this.index = index;
		this.magnitude = magnitude;
	}

	public double getIndex() {
		return index;
	}

	public double getMagnitude() {
		return magnitude;
	}

	@Override
	public int compareTo(Peak p) {
		return (-1) * Double.compare(this.magnitude, p.magnitude);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Peak)) {
			return false;
		} else {
			Peak p = (Peak) o;
			return (this.index == p.index) && (this.magnitude == p.magnitude);
		}
	}

	@Override
	public int hashCode() {
		return Double.valueOf(this.index).hashCode() ^ Double.valueOf(this.magnitude).hashCode();
	}

	@Override
	public String toString() {
		return index + ": " + magnitude;
	}
}
