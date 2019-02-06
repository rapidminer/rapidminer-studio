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
 * The info gain criterion for rule learning.
 * 
 * @author Ingo Mierswa
 */
public class InfoGainCriterion extends AbstractCriterion {

	private static double LOG_FACTOR = 1d / Math.log(2);

	@Override
	public double[] getBenefit(ExampleSet coveredSet, ExampleSet uncoveredSet, String labelName) {
		double coveredAccuracy = 0.0d;
		double coveredCoverage = 0.0d;
		Attribute weightAttribute = coveredSet.getAttributes().getWeight();
		Attribute labelAttribute = coveredSet.getAttributes().getLabel();
		int labelIndex = labelAttribute.getMapping().getIndex(labelName);
		for (Example e : coveredSet) {
			double weight = 1;
			if (weightAttribute != null) {
				weight = e.getValue(weightAttribute);
			}
			coveredCoverage += weight;
			if (e.getValue(labelAttribute) == labelIndex) {
				coveredAccuracy += weight;
			}
		}

		double uncoveredAccuracy = 0.0d;
		double uncoveredCoverage = 0.0d;
		weightAttribute = uncoveredSet.getAttributes().getWeight();
		labelAttribute = uncoveredSet.getAttributes().getLabel();
		labelIndex = labelAttribute.getMapping().getIndex(labelName);
		for (Example e : uncoveredSet) {
			double weight = 1;
			if (weightAttribute != null) {
				weight = e.getValue(weightAttribute);
			}
			uncoveredCoverage += weight;
			if (e.getValue(labelAttribute) == labelIndex) {
				uncoveredAccuracy += weight;
			}
		}

		double defaultAccuracy = (coveredAccuracy + uncoveredAccuracy) / (coveredCoverage + uncoveredCoverage);
		double infoGain = coveredAccuracy
				* (log2((coveredAccuracy + 1.0d) / (coveredCoverage + 1.0d)) - log2(defaultAccuracy));

		return new double[] { infoGain, coveredSet.size() };
	}

	@Override
	public double[] getOnlineBenefit(Example example, int labelIndex) {
		double coveredAccuracy = labelWeights[labelIndex];
		double coveredWeight = weight;
		double uncoveredAccuracy = totalLabelWeights[labelIndex] - labelWeights[labelIndex];
		double uncoveredWeight = totalWeight - weight;

		double defaultAccuracy = (coveredAccuracy + uncoveredAccuracy) / (coveredWeight + uncoveredWeight);
		double infoGain = coveredAccuracy
				* (log2((coveredAccuracy + 1.0d) / (coveredWeight + 1.0d)) - log2(defaultAccuracy));
		double reverseInfoGain = uncoveredAccuracy
				* (log2((uncoveredAccuracy + 1.0d) / (uncoveredWeight + 1.0d)) - log2(defaultAccuracy));
		return new double[] { infoGain, coveredWeight, reverseInfoGain, uncoveredWeight };
	}

	private double log2(double value) {
		return Math.log(value) * LOG_FACTOR;
	}
}
