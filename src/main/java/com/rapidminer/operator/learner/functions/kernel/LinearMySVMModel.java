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
package com.rapidminer.operator.learner.functions.kernel;

import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples.MeanVariance;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.Kernel;


/**
 * An optimized implementation for Linear MySVM Models that only store the coefficients to save
 * memory and apply these weights directly without kernel transformations.
 *
 * @author Tobias Malbrecht
 */
public class LinearMySVMModel extends PredictionModel {

	private static final long serialVersionUID = 2812901947459843681L;

	private static final int OPERATOR_PROGRESS_STEPS = 2000;

	private Map<Integer, MeanVariance> meanVariances;

	private double bias;

	private double[] weights = null;

	public LinearMySVMModel(ExampleSet exampleSet,
			com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples model, Kernel kernel,
			int kernelType) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.meanVariances = model.getMeanVariances();
		this.bias = model.get_b();
		this.weights = new double[model.get_dim()];
		for (int i = 0; i < model.count_examples(); i++) {
			double[] x = model.get_example(i).toDense(model.get_dim());
			double alpha = model.get_alpha(i);
			double y = model.get_y(i);
			if (y != 0.0d) {
				alpha /= y;
			}
			for (int j = 0; j < weights.length; j++) {
				weights[j] += y * alpha * x[j];
			}
		}
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabelAttribute) throws OperatorException {
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		for (Example example : exampleSet) {
			double prediction = bias;
			int a = 0;
			for (Attribute attribute : regularAttributes) {
				double value = example.getValue(attribute);
				MeanVariance meanVariance = meanVariances.get(a);
				if (meanVariance != null) {
					if (meanVariance.getVariance() == 0.0d) {
						value = 0.0d;
					} else {
						value = (value - meanVariance.getMean()) / Math.sqrt(meanVariance.getVariance());
					}
				}
				prediction += weights[a] * value;
				a++;
			}
			if (predictedLabelAttribute.isNominal()) {
				int index = prediction > 0 ? predictedLabelAttribute.getMapping().getPositiveIndex()
						: predictedLabelAttribute.getMapping().getNegativeIndex();
				example.setValue(predictedLabelAttribute, index);
				// set confidence to numerical prediction, such that can be scaled later
				example.setConfidence(predictedLabelAttribute.getMapping().getPositiveString(),
						1.0d / (1.0d + java.lang.Math.exp(-prediction)));
				example.setConfidence(predictedLabelAttribute.getMapping().getNegativeString(),
						1.0d / (1.0d + java.lang.Math.exp(prediction)));
			} else {
				example.setValue(predictedLabelAttribute, prediction);
			}
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}
		return exampleSet;
	}
}
