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
package com.rapidminer.tools.math.optimization.ec.es;

import com.rapidminer.operator.performance.PerformanceVector;


/**
 * Individuals store information about the value vectors and the fitness.
 * 
 * @author Ingo Mierswa
 */
public class Individual implements Cloneable {

	private double[] values;

	private PerformanceVector fitness;

	private double crowdingDistance;

	public Individual(double[] values) {
		this.values = values;
	}

	@Override
	public Object clone() {
		double[] alphaClone = new double[this.values.length];
		System.arraycopy(this.values, 0, alphaClone, 0, this.values.length);
		Individual clone = new Individual(alphaClone);
		return clone;
	}

	public void setCrowdingDistance(double cd) {
		this.crowdingDistance = cd;
	}

	public double getCrowdingDistance() {
		return crowdingDistance;
	}

	public double[] getValues() {
		return values;
	}

	public void setValues(double[] values) {
		this.values = values;
	}

	public double[] getFitnessValues() {
		double[] fitnessValues = new double[fitness.getSize()];
		for (int i = 0; i < fitnessValues.length; i++) {
			fitnessValues[i] = fitness.getCriterion(i).getFitness();
		}
		return fitnessValues;
	}

	public PerformanceVector getFitness() {
		return fitness;
	}

	public void setFitness(PerformanceVector fitness) {
		this.fitness = fitness;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("values=[");
		for (int i = 0; i < values.length; i++) {
			if (i != 0) {
				result.append(",");
			}
			result.append(values[i]);
		}
		result.append(", fitness: [");
		double[] fitnessValues = getFitnessValues();
		for (double d : fitnessValues) {
			result.append(d);
		}
		result.append("]");
		return result.toString();
	}
}
