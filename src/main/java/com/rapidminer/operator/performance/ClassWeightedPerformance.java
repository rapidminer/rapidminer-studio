/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.operator.performance;

/**
 * Performance criteria implementing this interface are able to calculate a performance measurement
 * based on given class weights.
 * 
 * @author Ingo Mierswa
 * 
 */
public interface ClassWeightedPerformance {

	/**
	 * Sets the weights. Please note that the given array might also be null. Even if the method is
	 * not invoked at all the performance criterion should return an (unweighted) performance
	 * estimation. If the array is not null, the only requirement is that the sum of all weights
	 * must not be 0.
	 */
	public void setWeights(double[] weights);

}
