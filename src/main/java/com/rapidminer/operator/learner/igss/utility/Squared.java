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
 * The utility function Squared.
 * 
 * @author Dirk Dach
 */
public class Squared extends AbstractUtility {

	/** Constructs a new Squared with the given default probability. */
	public Squared(double[] priors, int large) {
		super(priors, large);
	}

	/** Calculates the utility for the given number of examples,positive examples and hypothesis. */
	@Override
	public double utility(double totalWeight, double totalPositiveWeight, Hypothesis hypo) {
		double g = hypo.getCoveredWeight() / totalWeight;
		double p = hypo.getPositiveWeight() / hypo.getCoveredWeight();
		if (hypo.getPrediction() == Hypothesis.POSITIVE_CLASS) {
			return g * g * (p - this.priors[Hypothesis.POSITIVE_CLASS]);
		} else {
			return g * g * (p - this.priors[Hypothesis.NEGATIVE_CLASS]);
		}
	}

	/** Calculate confidence intervall without a specific rule. */
	@Override
	public double conf(double totalWeight, double delta) {
		double inverseNormal = inverseNormal(1 - delta / 2);
		return 3.0d / (2.0d * Math.sqrt(totalWeight)) * inverseNormal + (totalWeight + Math.sqrt(totalWeight))
				/ (4.0d * totalWeight * Math.sqrt(totalWeight)) * Math.pow(inverseNormal, 2.0d)
				+ Math.pow(inverseNormal, 3.0d) / (8.0d * totalWeight * Math.sqrt(totalWeight));
	}

	/** Calculate confidence intervall for a specific rule. */
	@Override
	public double conf(double totalWeight, double totalPositiveWeight, Hypothesis hypo, double delta) {
		double g = hypo.getCoveredWeight() / totalWeight;
		double p = hypo.getPositiveWeight() / hypo.getCoveredWeight();
		double sg = variance(g, totalWeight);
		double sp = variance(p, hypo.getCoveredWeight());
		double inverseNormal = inverseNormal(1.0d - delta / 2.0d);
		return 2.0d * sg * inverseNormal + sg * sg * Math.pow(inverseNormal, 2.0d) + sp * inverseNormal + 2.0d * sg * sp
				* Math.pow(inverseNormal, 2.0d) + sp * sg * sg * Math.pow(inverseNormal, 3.0d);
	}

	/** Calculates the variance for a binomial distribution. */
	private double variance(double p, double totalWeight) {
		return (p * (1 - p)) / totalWeight;
	}

	/** Calculate confidence intervall without a specific rule for small m. */
	@Override
	public double confSmallM(double totalWeight, double delta) {
		double term = Math.log(4.0d / delta) / (2.0d * totalWeight);
		return Math.pow(term, 1.5d) + 3.0d * term + 3.0d * Math.sqrt(term);
	}

	/** Returns an upper bound for the utility of refinements for the given hypothesis. */
	@Override
	public double getUpperBound(double totalWeight, double totalPositiveWeight, Hypothesis hypo, double delta) {
		double p0;
		if (hypo.getPrediction() == Hypothesis.POSITIVE_CLASS) {
			p0 = this.priors[Hypothesis.POSITIVE_CLASS];
		} else {
			p0 = this.priors[Hypothesis.NEGATIVE_CLASS];
		}
		Utility cov = new Coverage(this.priors, this.large);
		Hypothesis h = hypo.clone();
		h.setCoveredWeight(hypo.getPositiveWeight()); // all fp become tn
		double g = cov.utility(totalWeight, totalPositiveWeight, h);
		double conf = cov.confidenceIntervall(totalWeight, delta);
		return ((g + conf) * (g + conf) * (1.0d - p0));
	}
}
