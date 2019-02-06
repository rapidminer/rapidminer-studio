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
package com.rapidminer.operator.learner.meta;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;

import java.util.Iterator;


/**
 * Helper class for the internal AdaBoost implementation.
 * 
 * @author Martin Scholz ingomierswa Exp $
 */
public class AdaBoostPerformanceMeasures extends WeightedPerformanceMeasures {

	private final double errorRate;

	/**
	 * @param exampleSet
	 * @throws OperatorException
	 */
	public AdaBoostPerformanceMeasures(ExampleSet exampleSet) throws OperatorException {
		super(exampleSet);
		int num = this.getNumberOfLabels();
		double correct = 0;
		for (int i = 0; i < num; i++) {
			correct += this.getProbability(i, i);
		}
		this.errorRate = Math.max(0, Math.min(1, 1.0d - correct));
	}

	/** @return the error rate computed by the constructor */
	public double getErrorRate() {
		return this.errorRate;
	}

	/**
	 * This method reweights the example set with respect to the performance measures. Please note
	 * that the weights will not be reset at any time, because they continuously change from one
	 * iteration to the next.
	 * 
	 * @param exampleSet
	 *            <code>ExampleSet</code> to be reweighted
	 * @return the total weight after reweighting.
	 */
	public double reweightExamples(ExampleSet exampleSet) throws OperatorException {
		double reweightRightPred, reweightWrongPred;
		final double err = this.getErrorRate();
		if (err == 0 || err == 1) {
			reweightRightPred = 1;
			reweightWrongPred = 1;
		} else {
			reweightRightPred = Math.sqrt(err / (1.0d - err));
			reweightWrongPred = 1.0d / reweightRightPred;
		}

		double totalWeight = 0;
		Iterator<Example> reader = exampleSet.iterator();
		Attribute weightAttribute = exampleSet.getAttributes().getWeight();
		while (reader.hasNext()) {

			Example example = reader.next();
			int label = (int) example.getLabel();
			int predicted = (int) example.getPredictedLabel();

			double newWeight = example.getValue(weightAttribute)
					* ((label == predicted) ? reweightRightPred : reweightWrongPred);

			example.setValue(weightAttribute, newWeight);
			totalWeight += newWeight;
		}

		return totalWeight;
	}
}
