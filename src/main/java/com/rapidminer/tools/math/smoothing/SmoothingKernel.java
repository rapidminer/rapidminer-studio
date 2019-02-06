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

import java.io.Serializable;


/**
 * This class provides the functionality of a smoothing kernel, which returns weights depended on a
 * norm distance. Additionally, this class provides a convenient method for normalizing the
 * distances to 1.
 * 
 * @author Sebastian Land
 */
public abstract class SmoothingKernel implements Serializable {

	private static final long serialVersionUID = 6368830159821896801L;

	public double getWeight(double distance, double normalizationConstant) {
		return getWeight(distance / normalizationConstant);
	}

	public abstract double getWeight(double distance);
}
