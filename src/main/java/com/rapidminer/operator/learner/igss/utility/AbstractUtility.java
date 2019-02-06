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
package com.rapidminer.operator.learner.igss.utility;

import com.rapidminer.operator.learner.igss.hypothesis.Hypothesis;


/**
 * Abstract superclass for all utility functions.
 * 
 * @author Dirk Dach
 */
public abstract class AbstractUtility implements Utility {

	/** The prior probability of the two classes of the label. */
	protected double[] priors;

	/** The number of covered examples before normal approximation is used. */
	protected int large;

	/** Constructor for all utilities. */
	public AbstractUtility(double[] priors, int large) {
		this.priors = new double[priors.length];
		System.arraycopy(priors, 0, this.priors, 0, 2);
		this.large = large;
	}

	/** Calculates the M-value needed for the GSS algorithm. */
	@Override
	public double calculateM(double delta, double epsilon) {
		double i = 1;
		// perfomance: start with step=10000
		while (conf(i, delta) > epsilon / 2.0d) {
			i = i + 10000;
		}
		if (i > 1) { // i=i+10000 has been executed at least once.
			i = i - 10000;
		}

		while (conf(i, delta) > (epsilon / 2.0d)) {
			i++;
		}
		return Math.ceil(i);
	}

	/**
	 * Calculates the the unspecific confidence intervall. Uses Chernoff bounds if the number of
	 * random experiments is too small and normal approximatione otherwise. Considers the number of
	 * examples as the number of random experiments. problematic for g*(p-p0)) hypothesis, that only
	 * cover a small amount of examples. No normal approximation should be used in this case.
	 */
	@Override
	public double confidenceIntervall(double totalWeight, double delta) {
		if (totalWeight < large) {
			return confSmallM(totalWeight, delta);
		} else {
			return conf(totalWeight, delta);
		}
	}

	/**
	 * Calculates the the confidence intervall for a specific hypothesis. Uses Chernoff bounds if
	 * the number of random experiments is too small and normal approximation otherwise. This method
	 * is adapted for g*(p-p0) utility types. Every example for that the rule is applicable is one
	 * random experiment. Should be overwritten by subclasses if they make a different random
	 * experiment.
	 */
	@Override
	public double confidenceIntervall(double totalWeight, double totalPositiveWeight, Hypothesis hypo, double delta) {
		if (hypo.getCoveredWeight() < large) {
			return confSmallM(totalWeight, delta);
		} else {
			return conf(totalWeight, totalPositiveWeight, hypo, delta);
		}
	}

	/** Calculates the confidence intervall for small numbers of examples. */
	public abstract double confSmallM(double totalWeight, double delta);

	/** Calculates the normal approximation of the confidence intervall. */
	public abstract double conf(double totalWeight, double delta);

	/** Calculates the normal approximation of the confidence intervall for a specific hypothesis. */
	public abstract double conf(double totalWeight, double totalPositiveWeight, Hypothesis hypo, double delta);

	/** Calculates the inverse of the normal distribution, e.g.inverseNormal(0.95)==1.64. */
	public double inverseNormal(double p) {
		// Coefficients in rational approximations
		double[] a = { -3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02, 1.383577518672690e+02,
				-3.066479806614716e+01, 2.506628277459239e+00 };

		double[] b = { -5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02, 6.680131188771972e+01,
				-1.328068155288572e+01 };

		double[] c = { -7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00, -2.549732539343734e+00,
				4.374664141464968e+00, 2.938163982698783e+00 };

		double[] d = { 7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00 };

		// Define break-points.
		double plow = 0.02425;
		double phigh = 1 - plow;

		// Rational approximation for lower region:
		if (p < plow) {
			double q = Math.sqrt(-2 * Math.log(p));
			return (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
					/ ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1);
		}

		// Rational approximation for upper region:
		if (phigh < p) {
			double q = Math.sqrt(-2 * Math.log(1 - p));
			return -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
					/ ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1);
		}

		// Rational approximation for central region:
		double q = p - 0.5;
		double r = q * q;
		return (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q
				/ (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1);
	}

}
