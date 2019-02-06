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
 * Abstract superclass for all instance-averaging functions.
 * 
 * @author Dirk Dach
 */
public abstract class InstanceAveraging extends AbstractUtility {

	/** Constructor */
	public InstanceAveraging(double[] priors, int large) {
		super(priors, large);
	}

	/**
	 * Calculates the the confidence intervall for a specific hypothesis. Uses Chernoff bounds if
	 * the number of random experiments is too small and normal approximation otherwise. This method
	 * is adapted for instance averaging utility types. Every example is considered a random
	 * experiment, because f_inst is evaluated for every example!!! This is the reason why total
	 * weight is used instead of covered weight Should be overwritten by subclasses if they make a
	 * different random experiment.
	 */
	@Override
	public double confidenceIntervall(double totalWeight, double totalPositiveWeight, Hypothesis hypo, double delta) {
		if (totalWeight < large) {
			return confSmallM(totalWeight, delta);
		} else {
			return conf(totalWeight, totalPositiveWeight, hypo, delta);
		}
	}

	/** Calculate confidence intervall without a specific rule for instance averaging functions. */
	@Override
	public double conf(double totalWeight, double delta) {
		return inverseNormal(1 - delta / 2) / (2 * Math.sqrt(totalWeight));
	}

	/** Calculate confidence intervall for a specific rule for instance averaging functions. */
	@Override
	public double conf(double totalWeight, double totalPositiveWeight, Hypothesis hypo, double delta) {
		return inverseNormal(1 - delta / 2) * variance(totalWeight, totalPositiveWeight, hypo);
	}

	/** Calculates the empirical variance. */
	public abstract double variance(double totalWeight, double totalPositiveWeight, Hypothesis hypo);

	/**
	 * Calculate confidence intervall without a specific rule for instance averaging functions and
	 * small m.
	 */
	@Override
	public double confSmallM(double totalWeight, double delta) {
		return Math.sqrt(Math.log(2.0d / delta) / (2 * totalWeight));
	}
}
