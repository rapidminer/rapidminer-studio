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

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * The model for the internal Bagging implementation.
 *
 * @author Martin Scholz, Ingo Mierswa
 */
public class BaggingModel extends PredictionModel implements MetaModel {

	private static final long serialVersionUID = -4691755811263523354L;

	/** Holds the models. */
	private List<Model> models;

	public BaggingModel(ExampleSet exampleSet, List<Model> models) {
		super(exampleSet, null, null);
		this.models = models;
	}

	/** @return the number of embedded models */
	public int getNumberOfModels() {
		return this.models.size();
	}

	/**
	 * Getter method for embedded models
	 *
	 * @param index
	 *            the number of a model part of this boost model
	 * @return binary or nominal decision model
	 */
	public Model getModel(int index) {
		return this.models.get(index);
	}

	/**
	 * Iterates over all models and averages confidences.
	 *
	 * @param origExampleSet
	 *            the set of examples to be classified
	 */
	@Override
	public ExampleSet performPrediction(ExampleSet origExampleSet, Attribute predictedLabel) throws OperatorException {
		if (predictedLabel.isNominal()) {
			// nominal prediction
			final String attributePrefix = "BaggingModelPrediction";
			final int numLabels = predictedLabel.getMapping().size();
			final Attribute[] specialAttributes = new Attribute[numLabels];
			for (int i = 0; i < numLabels; i++) {
				specialAttributes[i] = com.rapidminer.example.Tools.createSpecialAttribute(origExampleSet,
						attributePrefix + i, Ontology.NUMERICAL);
			}

			for (int i = 0; i < specialAttributes.length; i++) {
				for (Example example : origExampleSet) {
					example.setValue(specialAttributes[i], 0);
				}
			}

			OperatorProgress progress = null;
			if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
				progress = getOperator().getProgress();
				progress.setTotal(this.getNumberOfModels());
			}
			for (int modelNr = 0; modelNr < this.getNumberOfModels(); modelNr++) {
				Model model = this.getModel(modelNr);
				ExampleSet exampleSet = (ExampleSet) origExampleSet.clone();
				exampleSet = model.apply(exampleSet);
				updateEstimates(exampleSet, modelNr, specialAttributes);
				PredictionModel.removePredictedLabel(exampleSet);
				if (progress != null) {
					progress.step();
				}
			}

			// Turn prediction weights into confidences and a crisp prediction:
			this.evaluateSpecialAttributes(origExampleSet, specialAttributes);

			// Clean up attributes:
			for (int i = 0; i < numLabels; i++) {
				origExampleSet.getAttributes().remove(specialAttributes[i]);
				origExampleSet.getExampleTable().removeAttribute(specialAttributes[i]);
			}

			return origExampleSet;
		} else {
			// numerical prediction
			double[] predictionSums = new double[origExampleSet.size()];
			OperatorProgress progress = null;
			if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
				progress = getOperator().getProgress();
				progress.setTotal(this.getNumberOfModels());
			}
			for (Model model : models) {
				ExampleSet resultSet = model.apply((ExampleSet) origExampleSet.clone());
				int index = 0;
				Attribute innerPredictedLabel = resultSet.getAttributes().getPredictedLabel();
				for (Example example : resultSet) {
					predictionSums[index++] += example.getValue(innerPredictedLabel);
				}
				PredictionModel.removePredictedLabel(resultSet);
				if (progress != null) {
					progress.step();
				}
			}

			int index = 0;
			for (Example example : origExampleSet) {
				example.setValue(predictedLabel, predictionSums[index++] / models.size());
			}

			return origExampleSet;
		}
	}

	private void updateEstimates(ExampleSet exampleSet, int modelNr, Attribute[] specialAttributes) {
		final int numModels = this.getNumberOfModels();
		final int numClasses = this.getLabel().getMapping().size();

		for (Example example : exampleSet) {

			for (int i = 0; i < numClasses; i++) {
				String consideredPrediction = this.getLabel().getMapping().mapIndex(i);
				double confidence = example.getConfidence(consideredPrediction);
				double value = example.getValue(specialAttributes[i]);
				value += confidence / numModels;
				example.setValue(specialAttributes[i], value);
				value = example.getValue(specialAttributes[i]);
			}

		}
	}

	private void evaluateSpecialAttributes(ExampleSet exampleSet, Attribute[] specialAttributes) {
		Attribute predictedLabel = exampleSet.getAttributes().getPredictedLabel();
		for (Example example : exampleSet) {
			int bestLabel = 0;
			double bestConf = -1;
			for (int n = 0; n < specialAttributes.length; n++) {
				double curConf = example.getValue(specialAttributes[n]);
				String curPredS = this.getLabel().getMapping().mapIndex(n);
				example.setConfidence(curPredS, curConf);

				if (curConf > bestConf) {
					bestConf = curConf;
					bestLabel = n;
				}
			}
			example.setValue(predictedLabel,
					predictedLabel.getMapping().mapString(this.getLabel().getMapping().mapIndex(bestLabel)));
		}
	}

	/** @return a <code>String</code> representation of this boosting model. */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString() + Tools.getLineSeparator() + "Number of inner models: "
				+ this.getNumberOfModels() + Tools.getLineSeparators(2));
		for (int i = 0; i < this.getNumberOfModels(); i++) {
			Model model = this.getModel(i);
			result.append((i > 0 ? Tools.getLineSeparator() : "") + "Embedded model #" + i + ":" + Tools.getLineSeparator()
					+ model.toResultString());
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
		return models;
	}
}
