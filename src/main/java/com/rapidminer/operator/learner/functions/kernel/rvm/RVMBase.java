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
 * Defines the interface for the various RVM-implementations
 * 
 * @author Piotr Kasprzak, Ingo Mierswa
 */
abstract public class RVMBase {

	protected Problem problem;				// The problem to be learned
	protected Parameter parameter;				// Various parameters influencing the learning process

	protected Model model = null;			// The learned model

	/** Constructor */
	public RVMBase(Problem problem, Parameter parameter) {
		this.problem = problem;
		this.parameter = parameter;
	}

	/** Does the hard work of learning the model from the inputs */
	abstract public Model learn();

	/** Get the learned model */
	public Model getModel() {
		return model;
	}
}
