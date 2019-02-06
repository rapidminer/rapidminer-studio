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
package com.rapidminer.operator.learner.meta;

import com.rapidminer.operator.Model;

import java.io.Serializable;


/**
 * Stores a base model together with its contingency matrix, which offerers a more convenient access
 * in the context of ensemble classification.
 * 
 * @author Martin Scholz ingomierswa Exp $
 */
public class BayBoostBaseModelInfo implements Serializable {

	private static final long serialVersionUID = 2818741267629650262L;

	// all fields are final, in particular ContingencyMatrix returns clones of
	// the original matrix
	private final Model model;

	private final ContingencyMatrix matrix;

	public BayBoostBaseModelInfo(Model model, ContingencyMatrix matrix) {
		this.model = model;
		this.matrix = matrix;
	}

	public Model getModel() {
		return this.model;
	}

	public ContingencyMatrix getContingencyMatrix() {
		return this.matrix;
	}

	public double getLiftRatio(int trueLabel, int predictedLabel) {
		return matrix.getLiftRatio(trueLabel, predictedLabel);
	}

}
