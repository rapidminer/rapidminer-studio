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
package com.rapidminer.tools.math.som;

/**
 * This class provides a distance measure, equivalent to the euclidian distance measure.
 * 
 * @author Sebastian Land
 */
public class EuclideanDistance implements DistanceFunction {

	private static final long serialVersionUID = 1127224325142026906L;

	@Override
	public double getDistance(double[] point1, double[] point2) {
		double distance = 0;
		for (int i = 0; i < point1.length; i++) {
			double current = point1[i] - point2[i];
			distance += current * current;
		}
		distance = Math.sqrt(distance);
		if (!Double.isNaN(distance)) {
			return distance;
		} else {
			distance = 0;
			for (int i = 0; i < point1.length; i++) {
				double currentDistance = (point1[i] - point2[i]);
				if (!Double.isNaN(currentDistance)) {
					distance += currentDistance * currentDistance;
				}
			}
			return Math.sqrt(distance);
		}
	}

	@Override
	public double getDistance(int[] point1, int[] point2) {
		double distance = 0;
		for (int i = 0; i < point1.length; i++) {
			double diff = point1[i] - point2[i];
			distance += diff * diff;
		}
		distance = Math.sqrt(distance);
		return distance;
	}

	/** Integer distance calculation regards the wrap around of the net (hypertorus) */
	@Override
	public double getDistance(int[] point1, int[] point2, int[] dimensions) {
		double distance = 0;
		for (int i = 0; i < point1.length; i++) {
			int coord1 = (point1[i] < 0) ? dimensions[i] + point1[i] : point1[i];	// wrapping around
																					// if incoming
																					// coordinate is
																					// negative
			int coord2 = (point2[i] < 0) ? dimensions[i] + point2[i] : point2[i];
			double diff = Math.min(Math.abs(coord1 - coord2), (coord1 + dimensions[i] - coord2) % dimensions[i]);
			distance += diff * diff;
		}
		distance = Math.sqrt(distance);
		return distance;
	}
}
