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
package com.rapidminer.gui.graphs;

/**
 * A helper class for sorting edges between two vertices according to the edge value.
 * 
 * @author Ingo Mierswa
 */
public class SortableEdge implements Comparable<SortableEdge> {

	public static final int DIRECTION_INCREASE = -1;

	public static final int DIRECTION_DECREASE = 1;

	private String vertex1;

	private String vertex2;

	private double strength;

	private String edgeName;

	private int direction;

	public SortableEdge(String v1, String v2, String edgeName, double strength, int direction) {
		this.vertex1 = v1;
		this.vertex2 = v2;
		this.strength = strength;
		this.edgeName = edgeName;
		this.direction = direction;
	}

	@Override
	public int hashCode() {
		return Double.valueOf(this.strength).hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SortableEdge)) {
			return false;
		}
		return ((SortableEdge) o).strength == this.strength;
	}

	@Override
	public int compareTo(SortableEdge o) {
		return direction * Double.compare(this.strength, o.strength);
	}

	public double getEdgeValue() {
		return this.strength;
	}

	public String getFirstVertex() {
		return this.vertex1;
	}

	public String getSecondVertex() {
		return this.vertex2;
	}

	public String getEdgeName() {
		return this.edgeName;
	}

	@Override
	public String toString() {
		return this.vertex1 + " --> " + this.vertex2 + " [" + this.edgeName + ", w = " + this.strength + "]";
	}
}
