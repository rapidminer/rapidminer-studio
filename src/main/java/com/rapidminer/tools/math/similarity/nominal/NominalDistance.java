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
package com.rapidminer.tools.math.similarity.nominal;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.math.similarity.DistanceMeasure;


/**
 * A distance measure for nominal values accounting a value of one if two values are unequal.
 * 
 * @author Sebastian Land, Michael Wurst
 */
public class NominalDistance extends DistanceMeasure {

	private static final long serialVersionUID = -1239573851325335924L;

	private boolean[] useAttribute;

	@Override
	public double calculateDistance(double[] value1, double[] value2) {
		double sum = 0.0;
		int counter = 0;

		for (int i = 0; i < value1.length; i++) {
			if (useAttribute == null || useAttribute[i]) {
				if ((!Double.isNaN(value1[i])) && (!Double.isNaN(value2[i]))) {
					if (value1[i] != value2[i]) {
						sum = sum + 1.0;
					}
					counter++;
				}
			}
		}

		if (counter > 0) {
			return sum;
		} else {
			return Double.NaN;
		}
	}

	@Override
	public double calculateSimilarity(double[] value1, double[] value2) {
		return -calculateDistance(value1, value2);
	}

	// checking for example set and valid attributes
	@Override
	public void init(ExampleSet exampleSet) throws OperatorException {
		super.init(exampleSet);
		Tools.onlyNominalAttributes(exampleSet, "nominal similarities");
		this.useAttribute = new boolean[exampleSet.getAttributes().size()];
		int i = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNominal()) {
				useAttribute[i] = true;
			}
			i++;
		}
	}

	@Override
	public String toString() {
		return "Nominal distance";
	}
}
