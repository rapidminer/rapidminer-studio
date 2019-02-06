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
 * This criterion class can be used to incrementally calculate a benefit.
 * 
 * @author Sebastian Land
 */
public abstract class AbstractCriterion implements Criterion {

	protected double[] labelWeights;
	protected double weight;
	protected double[] totalLabelWeights;
	protected double totalWeight;
	protected Attribute labelAttribute;
	protected Attribute weightAttribute;

	@Override
	public void update(Example example) {
		int labelIndex = (int) example.getValue(labelAttribute);
		if (weightAttribute != null) {
			double currentWeight = example.getValue(weightAttribute);
			labelWeights[labelIndex] += currentWeight;
			weight += currentWeight;
		} else {
			labelWeights[labelIndex] += 1d;
			weight += 1d;
		}
	}

	@Override
	public double[] getOnlineBenefit(Example example) {
		// finding most frequent label till now
		double maxWeight = Double.NEGATIVE_INFINITY;
		int mostFrequentLabelIndex = 0;
		for (int i = 0; i < labelWeights.length; i++) {
			if (labelWeights[i] > maxWeight) {
				mostFrequentLabelIndex = i;
				maxWeight = labelWeights[i];
			}
		}
		return getOnlineBenefit(example, mostFrequentLabelIndex);
	}

	@Override
	public void reinitOnlineCounting(ExampleSet exampleSet) {
		// counting one time all class weights
		labelAttribute = exampleSet.getAttributes().getLabel();
		weightAttribute = exampleSet.getAttributes().getWeight();
		totalLabelWeights = new double[labelAttribute.getMapping().size()];
		totalWeight = 0d;
		if (exampleSet.getAttributes().getWeight() != null) {
			for (Example example : exampleSet) {
				double weight = example.getWeight();
				totalLabelWeights[(int) example.getValue(labelAttribute)] += weight;
			}
		} else {
			for (Example example : exampleSet) {
				totalLabelWeights[(int) example.getValue(labelAttribute)] += 1d;
			}
		}
		for (int i = 0; i < totalLabelWeights.length; i++) {
			totalWeight += totalLabelWeights[i];
		}
		// resetting online counter for subtraction
		labelWeights = new double[labelAttribute.getMapping().size()];
		weight = 0;
	}

}
