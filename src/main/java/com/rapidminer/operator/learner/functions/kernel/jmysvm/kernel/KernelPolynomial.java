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

import com.rapidminer.tools.Tools;


/**
 * Polynomial Kernel
 * 
 * @author Stefan Rueping, Ingo Mierswa
 */
public class KernelPolynomial extends Kernel {

	private static final long serialVersionUID = 7385441798122306059L;

	private double degree = 2;

	/**
	 * Class constructor
	 */
	public KernelPolynomial() {};

	/**
	 * Output as String
	 */
	@Override
	public String toString() {
		return ("poly(" + degree + ")");
	}

	public void setDegree(double degree) {
		this.degree = degree;
	}

	/**
	 * Calculates kernel value of vectors x and y
	 */
	@Override
	public double calculate_K(int[] x_index, double[] x_att, int[] y_index, double[] y_att) {
		double prod = innerproduct(x_index, x_att, y_index, y_att);
		double result = prod;
		for (int i = 1; i < degree; i++) {
			result *= prod;
		}
		return result;
	}

	@Override
	public String getDistanceFormula(double[] x, String[] attributeConstructions) {
		StringBuffer innerProductString = new StringBuffer();
		boolean first = true;
		for (int i = 0; i < x.length; i++) {
			double value = x[i];
			if (!Tools.isZero(value)) {
				if (value < 0.0d) {
					if (first) {
						innerProductString.append("-" + Math.abs(value) + " * " + attributeConstructions[i]);
					} else {
						innerProductString.append(" - " + Math.abs(value) + " * " + attributeConstructions[i]);
					}
				} else {
					if (first) {
						innerProductString.append(value + " * " + attributeConstructions[i]);
					} else {
						innerProductString.append(" + " + value + " * " + attributeConstructions[i]);
					}
				}
				first = false;
			}
		}

		StringBuffer result = new StringBuffer("(" + innerProductString.toString() + ")");
		for (int i = 1; i < degree; i++) {
			result.append(" * (" + innerProductString.toString() + ")");
		}
		return result.toString();
	}

	public double getDegree() {
		return degree;
	}
}
