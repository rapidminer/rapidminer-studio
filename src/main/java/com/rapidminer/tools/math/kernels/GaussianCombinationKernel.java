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
 * Returns the value of the Gaussian combination kernel of both examples.
 * 
 * @author Ingo Mierswa
 */
public class GaussianCombinationKernel extends Kernel {

	private static final long serialVersionUID = 542405909968243049L;

	/** The parameter sigma1 of the Gaussian combination kernel. */
	private double sigma1 = 1.0d;

	/** The parameter sigma2 of the Gaussian combination kernel. */
	private double sigma2 = 0.0d;

	/** The parameter sigma3 of the Gaussian combination kernel. */
	private double sigma3 = 2.0d;

	@Override
	public int getType() {
		return KERNEL_GAUSSIAN_COMBINATION;
	}

	public void setSigma1(double sigma1) {
		this.sigma1 = sigma1;
	}

	public void setSigma2(double sigma2) {
		this.sigma2 = sigma2;
	}

	public void setSigma3(double sigma3) {
		this.sigma3 = sigma3;
	}

	public double getSigma1() {
		return sigma1;
	}

	public double getSigma2() {
		return sigma2;
	}

	public double getSigma3() {
		return sigma3;
	}

	/** Calculates kernel value of vectors x and y. */
	@Override
	public double calculateDistance(double[] x1, double[] x2) {
		double norm2 = norm2(x1, x2);
		double exp1 = sigma1 == 0.0d ? 0.0d : Math.exp((-1) * norm2 / sigma1);
		double exp2 = sigma2 == 0.0d ? 0.0d : Math.exp((-1) * norm2 / sigma2);
		double exp3 = sigma3 == 0.0d ? 0.0d : Math.exp((-1) * norm2 / sigma3);
		return exp1 + exp2 - exp3;
	}

	@Override
	public String getDistanceFormula(double[] x, String[] attributeConstructions) {

		StringBuffer norm2Expression = new StringBuffer();
		boolean first = true;
		for (int i = 0; i < x.length; i++) {
			double value = x[i];
			String valueString = "(" + value + " - " + attributeConstructions[i] + ")";
			if (first) {
				norm2Expression.append(valueString + " * " + valueString);
			} else {
				norm2Expression.append(" + " + valueString + " * " + valueString);
			}
			first = false;
		}

		String exp1 = sigma1 == 0.0d ? "" : "exp(-1 * " + norm2Expression.toString() + " / " + sigma1 + ")";
		String exp2 = sigma2 == 0.0d ? "" : "exp(-1 * " + norm2Expression.toString() + " / " + sigma2 + ")";
		String exp3 = sigma3 == 0.0d ? "" : "exp(-1 * " + norm2Expression.toString() + " / " + sigma3 + ")";

		StringBuffer result = new StringBuffer();
		if (exp1.length() > 0) {
			result.append(exp1);
		}
		if (exp2.length() > 0) {
			if (result.length() > 0) {
				result.append(" + " + exp2);
			} else {
				result.append(exp2);
			}
		}
		if (exp3.length() > 0) {
			if (result.length() > 0) {
				result.append(" - " + exp3);
			} else {
				result.append("-" + exp3);
			}
		}
		return result.toString();
	}

	@Override
	public String toString() {
		return "GaussianCombination Kernel with" + Tools.getLineSeparator() + "  sigma1: " + Tools.formatNumber(getSigma1())
				+ Tools.getLineSeparator() + "  sigma2: " + Tools.formatNumber(getSigma2()) + Tools.getLineSeparator()
				+ "  sigma3: " + Tools.formatNumber(getSigma3()) + Tools.getLineSeparator();
	}
}
