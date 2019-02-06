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
package com.rapidminer.operator.learner;

import java.util.Iterator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.UserError;


/**
 * <p>
 * A model that can be applied to an example set by applying it to each example separately. In
 * contrast to the usual {@link com.rapidminer.operator.learner.SimplePredictionModel}, this model
 * should only be used for binary classification problems. The method {@link #predict(Example)} must
 * deliver a function value. A value greater than 0 will be mapped to the positive class, a value
 * smaller than 0 to the negative class index. The confidence values will be calculated by the fast
 * and simple scaling suggested by Rueping.
 *
 * @author Ingo Mierswa
 */
public abstract class SimpleBinaryPredictionModel extends PredictionModel {

	private static final long serialVersionUID = 1540861516979781090L;

	private static final int OPERATOR_PROGRESS_STEPS = 2000;

	private double threshold = 0.0d;

	protected SimpleBinaryPredictionModel(ExampleSet exampleSet, double threshold) {
		super(exampleSet, null, null);
		this.threshold = threshold;
	}

	/**
	 * Applies the model to a single example and returns the predicted class value.
	 */
	public abstract double predict(Example example) throws OperatorException;

	/** Iterates over all examples and applies the model to them. */
	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		// checks
		if (!predictedLabel.isNominal()) {
			throw new UserError(null, 101, getName(), predictedLabel.getName());
		}
		if (predictedLabel.getMapping().getValues().size() != 2) {
			throw new UserError(null, 114, getName(), predictedLabel.getName());
		}

		Iterator<Example> r = exampleSet.iterator();
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;

		while (r.hasNext()) {
			Example example = r.next();
			double functionValue = predict(example) - threshold;

			// map prediction
			if (functionValue > 0.0d) {
				example.setValue(predictedLabel, getLabel().getMapping().getPositiveIndex());
			} else {
				example.setValue(predictedLabel, getLabel().getMapping().getNegativeIndex());
			}

			// set confidence values
			example.setConfidence(getLabel().getMapping().mapIndex(predictedLabel.getMapping().getPositiveIndex()),
					1.0d / (1.0d + java.lang.Math.exp(-functionValue)));
			example.setConfidence(getLabel().getMapping().mapIndex(predictedLabel.getMapping().getNegativeIndex()),
					1.0d / (1.0d + java.lang.Math.exp(functionValue)));

			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}

		return exampleSet;
	}
}
