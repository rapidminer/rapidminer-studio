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
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;


/**
 * A model that can be applied to an example set by applying it to each example separately. Just as
 * for the usual prediction model, subclasses must provide a constructor getting a label attribute
 * which will be used to invoke the super one-argument constructor.
 *
 * @author Ingo Mierswa, Simon Fischer ingomierswa Exp $
 */
public abstract class SimplePredictionModel extends PredictionModel {

	/**
	 *
	 */
	private static final long serialVersionUID = 6275902545494306001L;

	private static final int OPERATOR_PROGRESS_STEPS = 1000;

	/**
	 * @deprecated Since RapidMiner Studio 6.0.009. Please use the new Constructor
	 *             {@link #SimplePredictionModel(ExampleSet, com.rapidminer.example.set.ExampleSetUtilities.SetsCompareOption, com.rapidminer.example.set.ExampleSetUtilities.TypesCompareOption)}
	 *             which offers the possibility to check for AttributeType and kind of ExampleSet
	 *             before execution.
	 */
	@Deprecated
	protected SimplePredictionModel(ExampleSet exampleSet) {
		this(exampleSet, null, null);
	}

	/**
	 *
	 * @param sizeCompareOperator
	 *            describes the allowed relations between the given ExampleSet and future
	 *            ExampleSets on which this Model will be applied. If this parameter is null no
	 *            error will be thrown and no check will be done.
	 * @param typeCompareOperator
	 *            describes the allowed relations between the types of the attributes of the given
	 *            ExampleSet and the types of future attributes of ExampleSet on which this Model
	 *            will be applied. If this parameter is null no error will be thrown and no check
	 *            will be done.
	 */
	protected SimplePredictionModel(ExampleSet exampleSet, ExampleSetUtilities.SetsCompareOption sizeCompareOperator,
			ExampleSetUtilities.TypesCompareOption typeCompareOperator) {
		super(exampleSet, sizeCompareOperator, typeCompareOperator);
	}

	/**
	 * Applies the model to a single example and returns the predicted class value.
	 */
	public abstract double predict(Example example) throws OperatorException;

	/** Iterates over all examples and applies the model to them. */
	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		Iterator<Example> r = exampleSet.iterator();
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;

		while (r.hasNext()) {
			Example example = r.next();
			example.setValue(predictedLabel, predict(example));
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}
		return exampleSet;
	}
}
