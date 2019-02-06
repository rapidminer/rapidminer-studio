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
package com.rapidminer.tools.math.similarity.numerical;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import java.util.ArrayList;


/**
 * A distance measure based on "Dynamic Time Warping". The DTW distance is mapped to a similarity
 * measure using f(x)= 1 - (x / (1 + x)). Feature weights are also supported.
 * 
 * @author Piotr Kasprzak, Sebastian Land
 */
public class DTWDistance extends DistanceMeasure {

	private static final long serialVersionUID = 1382144431606583122L;

	protected double pointDistance(int i, int j, double[] ts1, double[] ts2) {
		double diff = ts1[i] - ts2[j];
		return (diff * diff);
	}

	@Override
	public double calculateDistance(double[] value1, double[] value2) {
		ArrayList<Double> l1 = new ArrayList<Double>();
		ArrayList<Double> l2 = new ArrayList<Double>();
		int i, j;
		/** Filter NaNs */
		for (i = 0; i < value1.length; i++) {
			double value = value1[i];
			if (!Double.isNaN(value)) {
				l1.add(value);
			}
		}
		for (i = 0; i < value2.length; i++) {
			double value = value2[i];
			if (!Double.isNaN(value)) {
				l2.add(value);
			}
		}
		/** Transform the examples to vectors */
		double[] ts1 = new double[l1.size()];
		double[] ts2 = new double[l2.size()];
		for (i = 0; i < ts1.length; i++) {
			ts1[i] = l1.get(i);
		}
		for (i = 0; i < ts2.length; i++) {
			ts2[i] = l2.get(i);
		}
		/** Build a point-to-point distance matrix */
		double[][] dP2P = new double[ts1.length][ts2.length];
		for (i = 0; i < ts1.length; i++) {
			for (j = 0; j < ts2.length; j++) {
				dP2P[i][j] = pointDistance(i, j, ts1, ts2);
			}
		}
		/** Check for some special cases due to ultra short time series */
		if (ts1.length == 0 || ts2.length == 0) {
			return Double.NaN;
		}
		if (ts1.length == 1 && ts2.length == 1) {
			return (Math.sqrt(dP2P[0][0]));
		}
		/**
		 * Build the optimal distance matrix using a dynamic programming approach
		 */
		double[][] D = new double[ts1.length][ts2.length];
		D[0][0] = dP2P[0][0]; // Starting point
		for (i = 1; i < ts1.length; i++) { // Fill the first column of our
			// distance matrix with optimal
			// values
			D[i][0] = dP2P[i][0] + D[i - 1][0];
		}
		if (ts2.length == 1) { // TS2 is a point
			double sum = 0;
			for (i = 0; i < ts1.length; i++) {
				sum += D[i][0];
			}
			return (Math.sqrt(sum) / ts1.length);
		}
		for (j = 1; j < ts2.length; j++) { // Fill the first row of our
			// distance matrix with optimal
			// values
			D[0][j] = dP2P[0][j] + D[0][j - 1];
		}
		if (ts1.length == 1) { // TS1 is a point
			double sum = 0;
			for (j = 0; j < ts2.length; j++) {
				sum += D[0][j];
			}
			return (Math.sqrt(sum) / ts2.length);
		}
		for (i = 1; i < ts1.length; i++) { // Fill the rest
			for (j = 1; j < ts2.length; j++) {
				double[] steps = { D[i - 1][j - 1], D[i - 1][j], D[i][j - 1] };
				double min = Math.min(steps[0], Math.min(steps[1], steps[2]));
				D[i][j] = dP2P[i][j] + min;
			}
		}
		/**
		 * Calculate the distance between the two time series through optimal alignment.
		 */
		i = ts1.length - 1;
		j = ts2.length - 1;
		int k = 1;
		double dist = D[i][j];
		while (i + j > 2) {
			if (i == 0) {
				j--;
			} else if (j == 0) {
				i--;
			} else {
				double[] steps = { D[i - 1][j - 1], D[i - 1][j], D[i][j - 1] };
				double min = Math.min(steps[0], Math.min(steps[1], steps[2]));
				if (min == steps[0]) {
					i--;
					j--;
				} else if (min == steps[1]) {
					i--;
				} else if (min == steps[2]) {
					j--;
				}
			}
			k++;
			dist += D[i][j];
		}
		return (Math.sqrt(dist) / k);
	}

	@Override
	public double calculateSimilarity(double[] value1, double[] value2) {
		double x = calculateDistance(value1, value2);
		return (1.0 - (x / (1 + x)));
	}

	@Override
	public void init(ExampleSet exampleSet) throws OperatorException {
		super.init(exampleSet);
		Tools.onlyNumericalAttributes(exampleSet, "value based similarities");
	}

	@Override
	public String toString() {
		return "Dynamic Time Warping distance";
	}

}
