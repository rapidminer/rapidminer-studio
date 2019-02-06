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
 * 
 * This class implements a McLain smoothing kernel with fixed epsilon on 1 for normalization, so
 * that it returns a weight of 1 on distance 0.
 * 
 * @author Sebastian Land
 * 
 */
public class McLainSmoothingKernel extends SmoothingKernel {

	private static final long serialVersionUID = -5396004335809012646L;

	@Override
	public double getWeight(double distance) {
		double toSquare = distance + 1;
		return 1d / (toSquare * toSquare);
	}

	@Override
	public String toString() {
		return "McLain Smoothing Kernel";
	}

}
