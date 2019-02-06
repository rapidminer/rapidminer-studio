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
 * Specialized similarity that takes the maximum product of two feature values. If this value is
 * zero, the similarity is undefined. This similarity measure is used mainly with features extracted
 * from cluster models.
 *
 * @author Michael Wurst
 */
public class MaxProductSimilarity extends SimilarityMeasure {

	private static final long serialVersionUID = -7476444724888001751L;

	@Override
	public double calculateSimilarity(double[] value1, double[] value2) {
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < value1.length; i++) {
			if (Double.isNaN(value1[i]) || Double.isNaN(value2[i])) {
				continue;
			}
			double v = value2[i] * value1[i];
			if (v > max) {
				max = v;
			}
		}
		if (max > 0.0) {
			return max;
		} else {
			return Double.NaN;
		}
	}

	@Override
	public double calculateDistance(double[] value1, double[] value2) {
		return -calculateSimilarity(value1, value2);
	}

	@Override
	public void init(ExampleSet exampleSet) throws OperatorException {
		super.init(exampleSet);
		Tools.onlyNumericalAttributes(exampleSet, "value based similarities");
	}

	@Override
	public String toString() {
		return "Max product numerical similarity";
	}

}
