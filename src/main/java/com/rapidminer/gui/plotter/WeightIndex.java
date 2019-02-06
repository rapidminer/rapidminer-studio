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
package com.rapidminer.gui.plotter;

/**
 * The weight index is used by several plotters to keep track of weights for specific data table
 * indices.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class WeightIndex implements Comparable<WeightIndex> {

	private int index;
	private double weight;

	public WeightIndex(int index, double weight) {
		this.index = index;
		this.weight = weight;
	}

	public int getIndex() {
		return this.index;
	}

	public double getWeight() {
		return this.weight;
	}

	@Override
	public int compareTo(WeightIndex wi) {
		return -1 * Double.compare(this.weight, wi.weight);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof WeightIndex)) {
			return false;
		} else {
			WeightIndex other = (WeightIndex) o;
			return (this.index == other.index) && (this.weight == other.weight);
		}
	}

	@Override
	public int hashCode() {
		return Integer.valueOf(this.index).hashCode() ^ Double.valueOf(this.weight).hashCode();
	}
}
