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
package com.rapidminer.operator.clustering.clusterer;

/**
 * This class implements an symmetrical matrix for distances, thus saving half the memory by saving
 * only the upper right triangle
 * 
 * @author Sebastian Land
 */
public class DistanceMatrix {

	private double[][] matrix;
	private int size;

	public DistanceMatrix(int size) {
		this.size = size;
		matrix = new double[size][];
		for (int i = 0; i < size; i++) {
			for (int j = i + 1; j < size; j++) {
				matrix[i] = new double[j];
			}

		}
	}

	public void set(int x, int y, double d) {
		if (x < y) {
			matrix[x][y - x - 1] = d;
		}
		if (x > y) {
			matrix[y][x - y - 1] = d;
		}
	}

	public int getWidth() {
		return size;
	}

	public int getHeight() {
		return size;
	}

	public double get(int x, int y) {
		if (x < y) {
			return matrix[x][y - x - 1];
		}
		if (x > y) {
			return matrix[y][x - y - 1];
		}
		return 0;
	}
}
