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
package com.rapidminer.operator.learner.functions.kernel.rvm;

/**
 * Holds the RVM parameters
 * 
 * @author Piotr Kasprzak, Ingo Mierswa
 * 
 */
public class Parameter {

	// supported RVM types
	private static class RVMType {

		private String rvmType = null;

		RVMType(String rvmType) {
			this.rvmType = rvmType;
		}

		@Override
		public String toString() {
			return this.rvmType;
		}

	}

	public final RVMType TYPE_REGRESSION = new RVMType("Regression-RVM");
	public final RVMType TYPE_CLASSIFICATION = new RVMType("Classifictaion-RVM");

	// the parameters to be chosen

	public RVMType type = TYPE_REGRESSION;

	public double min_delta_log_alpha = 1e-3;				// Abort iteration if largest log alpha change is
												// smaller than this
	public double alpha_max = 1e12;				// Prune basis function if its alpha is bigger than this

	public int maxIterations = 10000;			// Maximum number of iterations

	public double initAlpha = 1.0;				// Initial values for alpha_i hyperparameters
	public double initSigma = 1.0;				// Initial values for sigma = sqrt(variance)
}
