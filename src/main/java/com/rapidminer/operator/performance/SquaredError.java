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
package com.rapidminer.operator.performance;

/**
 * The squared error. Sums up the square of the absolute deviations and divides the sum by the
 * number of examples.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class SquaredError extends SimpleCriterion {

	private static final long serialVersionUID = 322984719296835789L;

	public SquaredError() {}

	public SquaredError(SquaredError se) {
		super(se);
	}

	@Override
	public String getName() {
		return "squared_error";
	}

	/** Calculates the error for the current example. */
	@Override
	public double countExample(double label, double predictedLabel) {
		double dif = label - predictedLabel;
		return dif * dif;
	}

	@Override
	public String getDescription() {
		return "Averaged squared error";
	}
}
