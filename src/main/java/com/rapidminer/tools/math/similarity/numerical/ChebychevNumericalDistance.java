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
package com.rapidminer.tools.math.similarity.numerical;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.math.similarity.DistanceMeasure;


/**
 * This measure returns the maximal individual absolute distance of both examples in any component.
 * 
 * @author Sebastian Land, Michael Wurst
 */
public class ChebychevNumericalDistance extends DistanceMeasure {

	private static final long serialVersionUID = -2995153254013795660L;

	@Override
	public double calculateDistance(double[] value1, double[] value2) {
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < value1.length; i++) {
			double v1 = value1[i];
			double v2 = value2[i];
			if ((!Double.isNaN(v1)) && (!Double.isNaN(v2))) {
				double d = Math.abs(v1 - v2);
				if (d > max) {
					max = d;
				}
			}
		}
		if (max > Double.NEGATIVE_INFINITY) {
			return max;
		} else {
			return Double.NaN;
		}
	}

	@Override
	public double calculateSimilarity(double[] value1, double[] value2) {
		return -calculateDistance(value1, value2);
	}

	@Override
	public void init(ExampleSet exampleSet) throws OperatorException {
		super.init(exampleSet);
		Tools.onlyNumericalAttributes(exampleSet, "value based similarities");
	}

	@Override
	public String toString() {
		return "Chebychev numerical distance";
	}

}
