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
package com.rapidminer.operator.clustering;

import com.rapidminer.tools.Tools;

import java.io.Serializable;
import java.util.Collection;


/**
 * This class represents a single centroid used for centroid based clustering. It also provides
 * methods for centroid calculation of a number of examples.
 * 
 * @author Sebastian Land
 */
public class Centroid implements Serializable {

	private static final long serialVersionUID = 1L;

	private double[] centroid;

	private double[] centroidSum;
	private int numberOfAssigned = 0;

	public Centroid(int numberOfDimensions) {
		centroid = new double[numberOfDimensions];
		centroidSum = new double[numberOfDimensions];
	}

	public double[] getCentroid() {
		return centroid;
	}

	public void setCentroid(double[] coordinates) {
		this.centroid = coordinates;
	}

	public void assignExample(double[] exampleValues) {
		numberOfAssigned++;
		for (int i = 0; i < exampleValues.length; i++) {
			centroidSum[i] += exampleValues[i];
		}
	}

	public void assignMultipleExamples(double[] summedExampleValuesAndNumberOfExamples) {
		numberOfAssigned = (int) summedExampleValuesAndNumberOfExamples[summedExampleValuesAndNumberOfExamples.length - 1];
		for (int i = 0; i < summedExampleValuesAndNumberOfExamples.length - 1; i++) {
			centroidSum[i] += summedExampleValuesAndNumberOfExamples[i];
		}
	}

	public boolean finishAssign() {
		double[] newCentroid = new double[centroid.length];
		boolean stable = true;
		for (int i = 0; i < centroid.length; i++) {
			newCentroid[i] = centroidSum[i] / numberOfAssigned;
			stable &= Double.compare(newCentroid[i], centroid[i]) == 0;
		}
		centroid = newCentroid;
		centroidSum = new double[centroidSum.length];
		numberOfAssigned = 0;
		return stable;
	}

	/**
	 * This method only returns the first 100 attributes
	 */
	public String toString(Collection<String> dimensionNames) {
		StringBuffer buffer = new StringBuffer();
		int i = 0;
		for (String dimName : dimensionNames) {
			buffer.append(dimName + ":\t");
			buffer.append(Tools.formatNumber(centroid[i]) + Tools.getLineSeparator());
			i++;
			if (i > 100) {
				break;
			}
		}
		return buffer.toString();
	}

}
