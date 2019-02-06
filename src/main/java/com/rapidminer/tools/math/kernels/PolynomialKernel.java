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
package com.rapidminer.tools.math.kernels;

import com.rapidminer.tools.Tools;


/**
 * Returns the value of a Polynomial kernel of both examples.
 * 
 * @author Ingo Mierswa
 */
public class PolynomialKernel extends Kernel {

	private static final long serialVersionUID = 1296800822192183260L;

	/** The parameter degree of the polynomial kernel. */
	private double degree = 3;

	/** The parameter shift of the polynomial kernel. */
	private double shift = 1.0d;

	@Override
	public int getType() {
		return KERNEL_POLYNOMIAL;
	}

	public double getDegree() {
		return degree;
	}

	/** Sets the used polynomial parameters. */
	public void setPolynomialParameters(double degree, double shift) {
		this.degree = degree;
		this.shift = shift;
	}

	/** Subclasses must implement this method. */
	@Override
	public double calculateDistance(double[] x1, double[] x2) {
		double prod = innerProduct(x1, x2) + shift;
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

	@Override
	public String toString() {
		return "Polynomial Kernel with" + Tools.getLineSeparator() + "  shift: " + Tools.formatNumber(shift)
				+ Tools.getLineSeparator() + "  degree: " + degree;
	}
}
