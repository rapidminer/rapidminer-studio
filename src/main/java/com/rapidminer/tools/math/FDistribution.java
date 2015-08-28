/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools.math;

/**
 * The FDistribution depends on two given degrees of freedom. It can be used to calculate the
 * probability from the result of a significance test (i.e. the probability that a null hypothesis
 * can be hold).
 * 
 * @author Ingo Mierswa
 */
public class FDistribution {

	private static final double[] GAMMA_COEFFICIENTS = new double[] { 76.18009172947146, -86.50532032941677,
			25.01409824083091, -1.231739572450155, 0.1208650973866179E-2, -0.5395239384953E-5 };

	private int degreeOfFreedom1 = 0;

	private int degreeOfFreedom2 = 0;

	public FDistribution(int degreeOfFreedom1, int degreeOfFreedom2) {
		this.degreeOfFreedom1 = degreeOfFreedom1;
		this.degreeOfFreedom2 = degreeOfFreedom2;
	}

	/** This method returns the probability that a value is greater than the given one. */
	public double getProbabilityForValue(double value) {
		return betaInverse(degreeOfFreedom1 * value / (degreeOfFreedom1 * value + degreeOfFreedom2),
				0.5d * degreeOfFreedom1, 0.5d * degreeOfFreedom2);
	}

	private double lnGamma(double c) {
		double xx = c;
		double yy = c;
		double tmp = xx + 5.5 - (xx + 0.5) * Math.log(xx + 5.5);
		double ser = 1.000000000190015;
		for (int i = 0; i < GAMMA_COEFFICIENTS.length; i++) {
			yy++;
			ser += (GAMMA_COEFFICIENTS[i] / yy);
		}
		return Math.log(2.5066282746310005 * ser / xx) - tmp;
	}

	private double lnBeta(double a, double b) {
		return (lnGamma(a) + lnGamma(b) - lnGamma(a + b));
	}

	/** Returns the result for the inverse beta function. */
	private double betaInverse(double x1, double p, double q) {
		double beta = lnBeta(p, q);
		double acu = 1e-14;
		if (p <= 0 || q <= 0) {
			return -1;
		}
		if (x1 <= 0 || x1 >= 1) {
			return -1;
		}
		double psq = p + q;
		double cx = 1 - x1;
		double x2 = Double.NaN;
		double pp = Double.NaN;
		double qq = Double.NaN;
		boolean index;
		if (p < psq * x1) {
			x2 = cx;
			cx = x1;
			pp = q;
			qq = p;
			index = true;
		} else {
			x2 = x1;
			pp = p;
			qq = q;
			index = false;
		}
		double term = 1;
		int ai = 1;
		double betain = 1;
		double ns = qq + cx * psq;
		double rx = x2 / cx;
		double temp = qq - ai;
		if (ns == 0) {
			rx = x2;
		}

		while (temp > acu && temp > acu * betain) {
			term = term * temp * rx / (pp + ai);
			betain = betain + term;
			temp = Math.abs(term);
			if (temp > acu && temp > acu * betain) {
				ai++;
				ns--;
				if (ns >= 0) {
					temp = qq - ai;
					if (ns == 0) {
						rx = x2;
					}
				} else {
					temp = psq;
					psq += 1;
				}
			}
		}
		betain *= Math.exp(pp * Math.log(x2) + (qq - 1) * Math.log(cx) - beta) / pp;
		if (index) {
			betain = 1 - betain;
		}

		return betain;
	}
}
