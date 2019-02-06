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
import com.rapidminer.tools.Tools;

import java.util.Iterator;


/**
 * A set of weighted performance measures used for subgroup discovery.
 * 
 * @author Martin Scholz Exp $
 */
public class SDReweightMeasures extends WeightedPerformanceMeasures {

	private double gamma;

	private boolean additive = true;

	public SDReweightMeasures(ExampleSet e) throws OperatorException {
		super(e);
	}

	/**
	 * Overwrites method from super class. Examples are reweighted by the additive or multiplicative
	 * heuristic. After reweighting the class priors are rescaled so that P(pos) = P(neg).
	 */
	public boolean reweightExamples(ExampleSet exampleSet, int posIndex, int coveredSubset) throws OperatorException {
		Iterator<Example> reader = exampleSet.iterator();
		Attribute timesCoveredAttrib = null;
		if (this.additive) {
			timesCoveredAttrib = exampleSet.getAttributes().get(SDRulesetInduction.TIMES_COVERED);
		}

		double sumPosWeight = 0;
		double sumNegWeight = 0;
		while (reader.hasNext()) {
			Example example = reader.next();
			double weight = example.getWeight();
			int label = ((int) example.getLabel());
			if (label == posIndex) {
				int predicted = ((int) example.getPredictedLabel());
				if (predicted == coveredSubset) {
					if (this.additive == true) {
						int timesCovered = ((int) example.getValue(timesCoveredAttrib)) + 1;
						weight = this.reweightAdd(weight, timesCovered);
						example.setValue(timesCoveredAttrib, timesCovered);
					} else {
						weight = this.reweightMult(weight);
					}

					example.setWeight(weight);
				}
				sumPosWeight += weight;
			} else {
				sumNegWeight += weight;
			}
		}
		double ratio = sumPosWeight / sumNegWeight;
		if (Tools.isNotEqual(ratio, 1)) {
			reader = exampleSet.iterator();
			while (reader.hasNext()) {
				Example example = reader.next();
				if ((int) (example.getLabel()) != posIndex) {
					example.setWeight(example.getWeight() * ratio);
				}
			}
		}
		return true;
	}

	private double reweightAdd(double w, int timesCovered) {
		// old weight factor: 1/i, new weight factor 1/(i+1)
		return (w * timesCovered) / (timesCovered + 1);
	}

	private double reweightMult(double w) {
		// Weight if covered i times is: \gamma^i.
		// w_{i+1} = w_i * gamma
		return w * gamma;
	}

	public void setGamma(double gamma) {
		this.gamma = gamma;
	}

	public void setAdditive(boolean additive) {
		this.additive = additive;
	}

}
