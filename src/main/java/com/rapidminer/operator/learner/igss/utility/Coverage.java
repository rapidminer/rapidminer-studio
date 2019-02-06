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
 * The instance-averaging utility function Coverage.
 * 
 * @author Dirk Dach
 */
public class Coverage extends InstanceAveraging {

	/** Constructs a new Coverage with the given default probability. */
	public Coverage(double[] priors, int large) {
		super(priors, large);
	}

	/** Calculates the utility for the given number of examples,positive examples and hypothesis. */
	@Override
	public double utility(double totalWeight, double totalPositiveWeight, Hypothesis hypo) {
		return hypo.getPositiveWeight() / totalWeight;
	}

	/** Calculates the empirical variance. */
	@Override
	public double variance(double totalWeight, double totalPositiveWeight, Hypothesis hypo) {
		double mean = this.utility(totalWeight, totalPositiveWeight, hypo);
		double innerTerm = hypo.getPositiveWeight() * Math.pow(1.0d - mean, 2.0d) + (totalWeight - hypo.getPositiveWeight())
				* Math.pow(0.0d - mean, 2.0d);
		return Math.sqrt(innerTerm) / totalWeight;
	}

	/** Returns an upper bound for the utility of refinements for the given hypothesis. */
	@Override
	public double getUpperBound(double totalWeight, double totalPositiveWeight, Hypothesis hypo, double delta) {
		// Never needed.
		return 1.0d;
	}
}
