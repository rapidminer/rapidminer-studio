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

import java.io.Serializable;


/**
 * This interface defines the methods of an distance measure class. All three methods should return
 * the same distance if equivalent inputs are given. The third method should regard the wrap around,
 * as if there were no bounds, instead point (0,0) should be neighbour of point (n,n)!
 * 
 * @author Sebastian Land
 */
public interface DistanceFunction extends Serializable {

	/**
	 * This method returns the distance between point1 and point2. The dimenson of the points is
	 * represented by the length of the arrays. This method should return the same value as the
	 * method below, if points are equivalent!
	 */
	public double getDistance(double[] point1, double[] point2);

	/**
	 * This method returns the distance between point1 and point2. The dimenson of the points is
	 * represented by the length of the arrays. This method should return the same value as the
	 * method above, if points are equivalent!
	 */
	public double getDistance(int[] point1, int[] point2);

	/**
	 * This method returns the distance between point1 and point2. The dimsion of the points is
	 * represented by the length of the arrays. This method has to be aware of the size of each
	 * dimension! Points on the border are neighbours to the point at the opposite site! As example:
	 * (0, x) is neighbour of (dimensions[0], x), too. Hence, the resulting distance is 1.
	 */
	public double getDistance(int[] point1, int[] point2, int[] dimensions);
}
