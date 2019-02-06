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
package com.rapidminer.operator.learner.rules;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;


/**
 * Calculates the accuracy benefit.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class AccuracyCriterion extends AbstractCriterion {

	@Override
	public double[] getBenefit(ExampleSet coveredSet, ExampleSet uncoveredSet, String labelName) {
		double labelSum = 0;
		double totalSum = 0;
		Attribute weightAttribute = coveredSet.getAttributes().getWeight();
		Attribute labelAttribute = coveredSet.getAttributes().getLabel();
		double labelValue = labelAttribute.getMapping().getIndex(labelName);
		for (Example e : coveredSet) {
			double weight = 1.0d;
			if (weightAttribute != null) {
				weight = e.getValue(weightAttribute);
			}
			totalSum += weight;
			if (e.getValue(labelAttribute) == labelValue) {
				labelSum += weight;
			}
		}
		return new double[] { labelSum / totalSum, totalSum };
	}

	@Override
	public double[] getOnlineBenefit(Example example, int labelIndex) {
		double accuracy = labelWeights[labelIndex] / weight;
		double reverseAccuracy = (totalLabelWeights[labelIndex] - labelWeights[labelIndex]) / (totalWeight - weight);
		return new double[] { accuracy, weight, reverseAccuracy, totalWeight - weight };
	}
}
