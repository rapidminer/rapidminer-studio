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
package com.rapidminer.tools.math.similarity.divergences;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.math.similarity.BregmanDivergence;


/**
 * The &quot;Logarithmic loss &quot;.
 *
 * @author Regina Fritsch
 */
public class LogarithmicLoss extends BregmanDivergence {

	private static final long serialVersionUID = 871453359959645339L;

	@Override
	public void init(ExampleSet exampleSet) throws OperatorException {
		super.init(exampleSet);
		Tools.onlyNumericalAttributes(exampleSet, "value based similarities");
		Attributes attributes = exampleSet.getAttributes();
		if (attributes.size() != 1) {
			throw new UserError(null, "inapplicable_bregman_divergence.multiple", toString());
		}
		for (Attribute attribute : attributes) {
			for (Example example : exampleSet) {
				if (example.getValue(attribute) <= 0) {
					throw new UserError(null, "inapplicable_bregman_divergence", toString());
				}
			}
		}
	}

	@Override
	public double calculateDistance(double[] value1, double[] value2) {
		return value1[0] * Math.log(value1[0] / value2[0]) - (value1[0] - value2[0]);
	}

	@Override
	public String toString() {
		return "Logarithmic loss";
	}
}
