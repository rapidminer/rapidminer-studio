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
 * Returns the value of the Epanechnikov kernel of both examples.
 * 
 * @author Ingo Mierswa
 */
public class EpanechnikovKernel extends Kernel {

	private static final long serialVersionUID = -4683350345234451645L;

	/** The parameter sigma of the Epanechnikov kernel. */
	private double sigma = 1.0d;

	/** The parameter degree of the Epanechnikov kernel. */
	private double degree = 1;

	@Override
	public int getType() {
		return KERNEL_EPANECHNIKOV;
	}

	public void setSigma(double sigma) {
		this.sigma = sigma;
	}

	public double getSigma() {
		return sigma;
	}

	public double getDegree() {
		return degree;
	}

	public void setDegree(double degree) {
		this.degree = degree;
	}

	/** Calculates kernel value of vectors x and y. */
	@Override
	public double calculateDistance(double[] x1, double[] x2) {
		double expression = norm2(x1, x2) / sigma;
		if (expression > 1) {
			return 0.0d;
		} else {
			double minus = 1.0d - expression;
			return Math.pow(minus, degree);
		}
	}

	@Override
	public String getDistanceFormula(double[] x, String[] attributeConstructions) {
		StringBuffer result = new StringBuffer("pow((1 - (");

		boolean first = true;
		for (int i = 0; i < x.length; i++) {
			double value = x[i];
			String valueString = "(" + value + " - " + attributeConstructions[i] + ")";
			if (first) {
				result.append(valueString + " * " + valueString);
			} else {
				result.append(" + " + valueString + " * " + valueString);
			}
			first = false;
		}
		result.append(")), " + degree + ")");
		return result.toString();
	}

	@Override
	public String toString() {
		return "Epanechnikov Kernel with" + Tools.getLineSeparator() + "  sigma: " + Tools.formatNumber(getSigma())
				+ Tools.getLineSeparator() + "  degree: " + degree;
	}
}
