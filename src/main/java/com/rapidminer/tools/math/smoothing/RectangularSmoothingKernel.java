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
package com.rapidminer.tools.math.smoothing;

/**
 * This class implements a rectangular kernel. If distance is less than 1, it returns full weight of
 * 1 otherwise 0.
 * 
 * @author Sebastian Land
 */
public class RectangularSmoothingKernel extends SmoothingKernel {

	private static final long serialVersionUID = 1257927748703355495L;

	@Override
	public double getWeight(double distance) {
		return (distance <= 1) ? 1d : 0d;
	}

	@Override
	public String toString() {
		return "Rectangular Smoothing Kernel";
	}

}
