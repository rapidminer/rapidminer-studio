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
package com.rapidminer.operator.features.transformation;

import java.util.Arrays;


/**
 * This class holds information about one eigenvector and eigenvalue.
 *
 * @author Ingo Mierswa
 */
public class WeightVector implements ComponentVector {

	private static final long serialVersionUID = -8280081065217261596L;

	private double eigenvalue;
	private double[] weights;

	public WeightVector(double[] weights, double eigenvalue) {
		this.weights = weights;
		this.eigenvalue = eigenvalue;
	}

	public double[] getWeights() {
		return this.weights;
	}

	@Override
	public double getEigenvalue() {
		return this.eigenvalue;
	}

	@Override
	public double[] getVector() {
		return getWeights();
	}

	@Override
	public int compareTo(ComponentVector o) {
		return -1 * Double.compare(this.eigenvalue, o.getEigenvalue());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(eigenvalue);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Arrays.hashCode(weights);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		WeightVector other = (WeightVector) obj;
		if (Double.doubleToLongBits(eigenvalue) != Double.doubleToLongBits(other.eigenvalue)) {
			return false;
		}
		if (!Arrays.equals(weights, other.weights)) {
			return false;
		}
		return true;
	}

}
