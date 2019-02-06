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
package com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel;

/**
 * Anova Kernel
 * 
 * @author Ingo Mierswa
 */
public class KernelAnova extends Kernel {

	private static final long serialVersionUID = -8670034220969832253L;

	private double sigma = 1;
	private double degree = 1;

	/** Class constructor. */
	public KernelAnova() {}

	public void setParameters(double sigma, double degree) {
		this.sigma = sigma;
		this.degree = degree;
	}

	/** Calculates kernel value of vectors x and y. */
	@Override
	public double calculate_K(int[] x_index, double[] x_att, int[] y_index, double[] y_att) {
		double result = 0;
		double tmp;
		int xpos = x_index.length - 1;
		int ypos = y_index.length - 1;
		int zeros = dim;
		while ((xpos >= 0) && (ypos >= 0)) {
			if (x_index[xpos] == y_index[ypos]) {
				tmp = x_att[xpos] - y_att[ypos];
				result += Math.exp(-sigma * tmp * tmp);
				xpos--;
				ypos--;
			} else if (x_index[xpos] > y_index[ypos]) {
				tmp = x_att[xpos];
				result += Math.exp(-sigma * tmp * tmp);
				xpos--;
			} else {
				tmp = y_att[ypos];
				result += Math.exp(-sigma * tmp * tmp);
				ypos--;
			}
			zeros--;
		}
		while (xpos >= 0) {
			tmp = x_att[xpos];
			result += Math.exp(-sigma * tmp * tmp);
			xpos--;
			zeros--;
		}
		while (ypos >= 0) {
			tmp = y_att[ypos];
			result += Math.exp(-sigma * tmp * tmp);
			ypos--;
			zeros--;
		}
		result += zeros;
		return Math.pow(result, degree);
	}

	/** Output as String */
	@Override
	public String toString() {
		return ("anova(s = " + sigma + ", d = " + degree + ")");
	}

	@Override
	public String getDistanceFormula(double[] x, String[] attributeConstructions) {
		StringBuffer result = new StringBuffer();
		result.append("pow((");

		boolean first = true;
		for (int i = 0; i < x.length; i++) {
			double value = x[i];
			String valueString = "(" + value + " - " + attributeConstructions[i] + ")";
			if (first) {
				result.append("exp(-" + sigma + " * " + valueString + " * " + valueString + ")");
			} else {
				result.append(" + exp(-" + sigma + " * " + valueString + " * " + valueString + ")");
			}
			first = false;
		}
		result.append("), " + degree + ")");
		return result.toString();
	}
}
