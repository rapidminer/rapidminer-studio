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
package com.rapidminer.operator.learner.lazy;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;


/**
 * The default model sets the prediction of all examples to the mode value in case of nominal labels
 * and to the average value in case of numerical labels.
 *
 * @author Stefan Rueping, Ingo Mierswa
 */
public class DefaultModel extends PredictionModel {

	private static final long serialVersionUID = -1455906287520811107L;

	private static final int OPERATOR_PROGRESS_STEPS = 10_000;

	/** The default prediction. */
	private double value;

	/** The confidence values for all predictions. */
	private double[] confidences;

	/** Can be used to create a default model for regression tasks. */
	public DefaultModel(ExampleSet exampleSet, double value) {
		this(exampleSet, value, null);
	}

	/**
	 * Can be used to create a default model for classification tasks (confidence values should not
	 * be null in this case).
	 */
	public DefaultModel(ExampleSet exampleSet, double value, double[] confidences) {
		super(exampleSet, null, null);
		this.value = value;
		this.confidences = confidences;
	}

	/** Iterates over all examples and applies the model to them. */
	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabelAttribute) throws OperatorException {
		Attribute label = getLabel();
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;
		for (Example example : exampleSet) {
			example.setValue(predictedLabelAttribute, value);
			if (label.isNominal()) {
				for (int i = 0; i < confidences.length; i++) {
					example.setConfidence(predictedLabelAttribute.getMapping().mapIndex(i), confidences[i]);
				}
			}
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}
		return exampleSet;
	}

	@Override
	public String toString() {
		return super.toString() + Tools.getLineSeparator() + "default value: "
				+ (getLabel().isNominal() ? getLabel().getMapping().mapIndex((int) value) : value + "");
	}

	public double getValue() {
		return value;
	}

	public double[] getConfidences() {
		return confidences;
	}
}
