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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.math.similarity.BregmanDivergence;


/**
 * The &quot;Kullback-Leibler divergence &quot;.
 * 
 * @author Regina Fritsch
 */
public class KLDivergence extends BregmanDivergence {

	private static final long serialVersionUID = -2151832592670074328L;

	@Override
	public double calculateDistance(double[] value1, double[] value2) {
		double result = 0;
		for (int i = 0; i < value1.length; i++) {
			result += value1[i] * logXToBaseY(value1[i] / value2[i], 2);
		}
		return result;
	}

	@Override
	public void init(ExampleSet exampleSet) throws OperatorException {
		super.init(exampleSet);
		Tools.onlyNumericalAttributes(exampleSet, "value based similarities");
	}

	@Override
	public String toString() {
		return "Kullback-Leibler divergence";
	}
}
