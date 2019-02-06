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
package com.rapidminer.operator.features;

import com.rapidminer.operator.performance.PerformanceVector;


/**
 * Individuals contain all necessary informations for feature selection or weighting of example sets
 * for population based search heuristics, including the performance. Each individiual can also
 * handle a crowding distance for multi-objecitve optimization approaches.
 * 
 * @author Ingo Mierswa
 */
public class Individual {

	/** The weight mask. */
	private double[] weights;

	/**
	 * The performance this example set has achieved during evaluation. Null if no evaluation has
	 * been performed so far.
	 */
	private PerformanceVector performanceVector = null;

	/** The crowding distance can used for multiobjective optimization schemes. */
	private double crowdingDistance = Double.NaN;

	/** Creates a new individual by cloning the given values. */
	public Individual(double[] weights) {
		this.weights = weights;
	}

	public double[] getWeights() {
		return this.weights;
	}

	public double[] getWeightsClone() {
		double[] clone = new double[weights.length];
		System.arraycopy(this.weights, 0, clone, 0, this.weights.length);
		return clone;
	}

	public int getNumberOfUsedAttributes() {
		int count = 0;
		for (double d : this.weights) {
			if (d > 0) {
				count++;
			}
		}
		return count;
	}

	public PerformanceVector getPerformance() {
		return performanceVector;
	}

	public void setPerformance(PerformanceVector performanceVector) {
		this.performanceVector = performanceVector;
	}

	public double getCrowdingDistance() {
		return this.crowdingDistance;
	}

	public void setCrowdingDistance(double crowdingDistance) {
		this.crowdingDistance = crowdingDistance;
	}
}
