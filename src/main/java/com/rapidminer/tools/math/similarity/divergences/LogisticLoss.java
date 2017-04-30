/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.tools.math.similarity.divergences;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.math.similarity.BregmanDivergence;


/**
 * The &quot;Logistic loss &quot;.
 * 
 * @author Regina Fritsch
 */
public class LogisticLoss extends BregmanDivergence {

	private static final long serialVersionUID = 6209100890792566974L;

	@Override
	public double calculateDistance(double[] value1, double[] value2) {
		return value1[0] * Math.log(value1[0] / value2[0]) + (1 - value1[0]) * Math.log((1 - value1[0]) / (1 - value2[0]));
	}

	@Override
	public void init(ExampleSet exampleSet) throws OperatorException {
		super.init(exampleSet);
		Tools.onlyNumericalAttributes(exampleSet, "value based similarities");
		Attributes attributes = exampleSet.getAttributes();
		if (attributes.size() != 1) {
			throw new OperatorException(
					"The bregman divergence you've choosen is not applicable for the dataset! Proceeding with the 'Squared Euclidean distance' bregman divergence.");
		}
		for (Example example : exampleSet) {
			for (Attribute attribute : attributes) {
				double value = example.getValue(attribute);
				if (value <= 0 || value >= 1) {
					throw new OperatorException(
							"The bregman divergence you've choosen is not applicable for the dataset! Proceeding with the 'Squared Euclidean distance' bregman divergence.");
				}
				;
			}
		}
	}

	@Override
	public String toString() {
		return "Logistic loss";
	}
}
