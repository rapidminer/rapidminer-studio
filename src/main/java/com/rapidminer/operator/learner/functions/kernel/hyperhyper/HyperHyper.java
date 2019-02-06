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
package com.rapidminer.operator.learner.functions.kernel.hyperhyper;

import java.util.List;
import java.util.Vector;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.RandomGenerator;


/**
 * This is a minimal SVM implementation. The model is built with only one positive and one negative
 * example. Typically this operator is used in combination with a boosting method.
 *
 * @author Regina Fritsch
 */
public class HyperHyper extends AbstractLearner {

	public HyperHyper(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		// if no weights available, initialize weights
		if (exampleSet.getAttributes().getWeight() == null) {
			com.rapidminer.example.Tools.createWeightAttribute(exampleSet);
		}

		double weightSum = 0;
		for (Example e : exampleSet) {
			weightSum += e.getWeight();
		}

		Attribute label = exampleSet.getAttributes().getLabel();

		Example x1 = this.rejectionSampling(exampleSet, weightSum);
		Example x2 = null;
		int tries = 0;
		do {
			x2 = this.rejectionSampling(exampleSet, weightSum);
			tries += 1;

			// if one class is much smaller in the exampleSet, split it up from the rest
			if (tries >= 10) {
				Vector<Example> examplesWithWantedLabel = new Vector<Example>();
				for (Example ex : exampleSet) {
					if (ex.getValue(label) != x1.getValue(label)) {
						examplesWithWantedLabel.add(ex);
					}
				}
				RandomGenerator random = RandomGenerator.getRandomGenerator(this);
				if (examplesWithWantedLabel.size() <= 0) {
					throw new UserError(this, 968);
				}
				boolean doSampling = true;
				while (doSampling == true) {
					int index = random.nextInt(examplesWithWantedLabel.size());
					if (random.nextDouble() < examplesWithWantedLabel.get(index).getWeight() / weightSum) {
						x2 = examplesWithWantedLabel.get(index);
						doSampling = false;
					}
				}
			}

		} while (x1.getValue(label) == x2.getValue(label));

		// compute w
		double[] w = new double[x1.getAttributes().size()];
		int i = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			w[i] = x1.getValue(attribute) - x2.getValue(attribute);
			i++;
		}

		// compute b
		double bx1 = 0;
		double bx2 = 0;
		i = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			bx1 += x1.getValue(attribute) * w[i];
			bx2 += x2.getValue(attribute) * w[i];
			i++;
		}
		double b = (bx1 + bx2) * -0.5;

		double[] x1Values = new double[exampleSet.getAttributes().size()];
		int counter = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			x1Values[counter++] = x1.getValue(attribute);
		}

		double[] x2Values = new double[exampleSet.getAttributes().size()];
		counter = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			x2Values[counter++] = x2.getValue(attribute);
		}

		return new HyperModel(exampleSet, b, w, x1Values, x2Values);
	}

	private Example rejectionSampling(ExampleSet exampleSet, double weightSum) throws OperatorException {
		Example example = null;
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		boolean doSampling = true;
		while (doSampling == true) {
			int index = random.nextInt(exampleSet.size());
			if (random.nextDouble() < exampleSet.getExample(index).getWeight() / weightSum) {
				example = exampleSet.getExample(index);
				doSampling = false;
			}
		}
		return example;
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return HyperModel.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		if (capability == OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}
		if (capability == OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		if (capability == OperatorCapability.WEIGHTED_EXAMPLES) {
			return true;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getExampleSetInputPort(),
				HyperHyper.class, null);
	}
}
