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
 * Interface for all utility functions.
 * 
 * @author Dirk Dach
 */
public interface Utility {

	public static final String[] UTILITY_TYPES = { "accuracy", "linear", "squared", "binomial", "wracc" };
	public static final int FIRST_TYPE_INDEX = 0;
	public static final int TYPE_ACCURACY = 0;
	public static final int TYPE_LINEAR = 1;
	public static final int TYPE_SQUARED = 2;
	public static final int TYPE_BINOMIAL = 3;
	public static final int TYPE_WRACC = 4;
	public static final int LAST_TYPE_INDEX = 4;

	/** Calculates the utility for the given number of examples,positive examples and hypothesis */
	public double utility(double totalWeight, double totalPositiveWeight, Hypothesis hypo);

	/** Calculates the M-value needed for the GSS algorithm. */
	public double calculateM(double delta, double epsilon);

	/**
	 * Calculates the the unspecific confidence intervall. Uses Chernoff bounds if the number of
	 * random experiments is too small and normal approximatione otherwise.
	 */
	public double confidenceIntervall(double totalWeight, double delta);

	/**
	 * Calculates the the confidence intervall for a specific hypothesis. Uses Chernoff bounds if
	 * the number of random experiments is too small and normal approximation otherwise.
	 */
	public double confidenceIntervall(double totalWeight, double totalPositiveWeight, Hypothesis hypo, double delta);

	/** Returns an upper bound for the utility of refinements for the given hypothesis. */
	public double getUpperBound(double totalWeight, double totalPositiveWeight, Hypothesis hypo, double delta);
}
