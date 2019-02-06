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
package com.rapidminer.tools.math.similarity.numerical;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.math.similarity.SimilarityMeasure;


/**
 * Cosine similarity that supports feature weights. If both vectors are empty or null vectors, NaN
 * is returned.
 * 
 * @author Michael Wurst
 */
public class CosineSimilarity extends SimilarityMeasure {

	private static final long serialVersionUID = 2856052490402674777L;

	@Override
	public double calculateSimilarity(double[] value1, double[] value2) {
		double sum = 0.0;
		double sum1 = 0.0;
		double sum2 = 0.0;
		for (int i = 0; i < value1.length; i++) {
			double v1 = value1[i];
			double v2 = value2[i];
			if ((!Double.isNaN(v1)) && (!Double.isNaN(v2))) {
				sum += v2 * v1;
				sum1 += v1 * v1;
				sum2 += v2 * v2;
			}
		}
		if ((sum1 > 0) && (sum2 > 0)) {
			double result = sum / (Math.sqrt(sum1) * Math.sqrt(sum2));
			// result can be > 1 (or -1) due to rounding errors for equal vectors, but must be
			// between -1 and 1
			return Math.min(Math.max(result, -1d), 1d);
			// return result;
		} else if (sum1 == 0 && sum2 == 0) {
			return 1d;
		} else {
			return 0d;
		}
	}

	@Override
	public double calculateDistance(double[] value1, double[] value2) {
		return Math.acos(calculateSimilarity(value1, value2));
	}

	@Override
	public void init(ExampleSet exampleSet) throws OperatorException {
		super.init(exampleSet);
		Tools.onlyNumericalAttributes(exampleSet, "value based similarities");
	}

	@Override
	public String toString() {
		return "Cosine similarity";
	}

}
