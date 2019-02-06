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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;


/**
 * MultiModels are used for multi class learning tasks. A MultiModel contains a set of Models that
 * can handle only two-class decisions. In this case, the models must be regression models which are
 * combined.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class MultiModelByRegression extends PredictionModel implements MetaModel {

	private static final long serialVersionUID = 4526668088304067678L;

	private static final int OPERATOR_PROGRESS_STEPS = 5000;

	private Model[] models;

	public MultiModelByRegression(ExampleSet exampleSet, Model[] models) {
		super(exampleSet, null, null);
		this.models = models;
	}

	public int getNumberOfModels() {
		return models.length;
	}

	/** Returns a binary decision model for the given classification index. */
	public Model getModel(int index) {
		return models[index];
	}

	/**
	 * Iterates over all classes of the label and applies one model for each class. For each example
	 * the predicted label is determined by choosing the model with the highest confidence.
	 */
	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabelAttribute) throws OperatorException {
		// initialize progress
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(100);
		}

		ExampleSet[] eSet = new ExampleSet[getNumberOfModels()];

		for (int i = 0; i < getNumberOfModels(); i++) {
			Model model = getModel(i);
			eSet[i] = (ExampleSet) exampleSet.clone();
			eSet[i] = model.apply(eSet[i]);
			if (progress != null) {
				progress.setCompleted((int) (50.0 * (i + 1) / getNumberOfModels()));
			}
		}

		List<Iterator<Example>> reader = new ArrayList<Iterator<Example>>(eSet.length);
		for (int r = 0; r < eSet.length; r++) {
			reader.add(eSet[r].iterator());
		}

		Iterator<Example> originalReader = exampleSet.iterator();
		Attribute predictedLabel = exampleSet.getAttributes().getPredictedLabel();
		int progressCounter = 0;
		while (originalReader.hasNext()) {
			double bestLabel = Double.NaN;
			double highestFunctionValue = Double.NEGATIVE_INFINITY;
			for (int k = 0; k < reader.size(); k++) {
				double functionValue = reader.get(k).next().getPredictedLabel();
				if (functionValue > highestFunctionValue) {
					highestFunctionValue = functionValue;
					bestLabel = k;
				}
			}
			Example example = originalReader.next();
			example.setPredictedLabel(bestLabel);
			example.setConfidence(predictedLabel.getMapping().mapIndex((int) bestLabel), 1.0d);

			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted((int) (50 + 50.0 * progressCounter / exampleSet.size()));
			}
		}

		return exampleSet;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString() + Tools.getLineSeparator());
		for (int i = 0; i < models.length; i++) {
			result.append((i > 0 ? Tools.getLineSeparator() : "") + models[i].toString());
		}
		return result.toString();
	}

	@Override
	public List<String> getModelNames() {
		List<String> names = new LinkedList<String>();
		for (int i = 0; i < this.getNumberOfModels(); i++) {
			names.add("Model " + (i + 1));
		}
		return names;
	}

	@Override
	public List<Model> getModels() {
		return Arrays.asList(models);
	}
}
