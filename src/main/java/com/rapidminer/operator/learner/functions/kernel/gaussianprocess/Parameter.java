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
package com.rapidminer.operator.learner.functions.kernel.gaussianprocess;

/**
 * Holds the GP parameters
 * 
 * @author Piotr Kasprzak
 * 
 */
public class Parameter {

	// supported GP types

	private static class GPType {

		private String gpType = null;

		GPType(String gpType) {
			this.gpType = gpType;
		}

		@Override
		public String toString() {
			return this.gpType;
		}

	}

	public static final GPType TYPE_GAUSS_REGRESSION = new GPType("Regression");

	/** The parameters to be chosen */

	/* Type of the GP Learner */
	public GPType type = TYPE_GAUSS_REGRESSION;

	/* Maximum number of basis vectors to use */
	public int maxBasisVectors = 100;

	/*
	 * Tolerance value: we project the current basis vector if it has a orthogonal distance to the
	 * linear span of the other basis vectors smaller than epsilon_tol
	 */
	public double epsilon_tol = 1e-7;

	public double geometrical_tol = 1e-7;
}
