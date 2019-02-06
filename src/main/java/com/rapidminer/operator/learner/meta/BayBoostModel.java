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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.AbstractModel;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.studio.internal.ProcessStoppedRuntimeException;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.Tools;


/**
 * A model for the Bayesian Boosting algorithm by Martin Scholz.
 *
 * @author Martin Scholz
 */
public class BayBoostModel extends PredictionModel implements MetaModel {

	private static final long serialVersionUID = 5821921049035718838L;

	private static final int OPERATOR_PROGRESS_STEPS = 10_000;

	// Holds the models and their weights in array format.
	// Please access with getter methods.
	private final List<BayBoostBaseModelInfo> modelInfo;

	// The classes priors in the training set, starting with index 0.
	private final double[] priors;

	// If set to a value i >= 0 then only the first i models are applied
	private int maxModelNumber = -1;

	private static final String MAX_MODEL_NUMBER = "iteration";

	// turn soft into crisp classifiers
	private static final String CONV_TO_CRISP = "crisp";

	private double threshold = 0.5;

	/**
	 * @param exampleSet
	 *            the example set used for training
	 * @param modelInfos
	 *            a <code>List</code> of <code>Object[2]</code> arrays, each entry holding a model
	 *            and a <code>double[][]</code> array containing weights for all prediction/label
	 *            combinations.
	 * @param priors
	 *            an array of the prior probabilities of labels
	 */
	public BayBoostModel(ExampleSet exampleSet, List<BayBoostBaseModelInfo> modelInfos, double[] priors) {
		super(exampleSet, null, null);
		this.modelInfo = modelInfos;
		this.priors = priors;
	}

	public BayBoostBaseModelInfo getBayBoostBaseModelInfo(int index) {
		return this.modelInfo.get(index);
	}

	/**
	 * Setting the parameter <code>MAX_MODEL_NUMBER</code> allows to discard all but the first n
	 * models for specified n. <code>CONV_TO_CRISP</code> allows to set another threshold than 0.5
	 * for boolean prediction problems.
	 */
	@Override
	public void setParameter(String name, Object value) throws OperatorException {
		String stringValue = (String) value;
		if (name.equalsIgnoreCase(MAX_MODEL_NUMBER)) {
			try {
				this.maxModelNumber = Integer.parseInt(stringValue);
				return;
			} catch (NumberFormatException e) {
			}
		} else if (name.equalsIgnoreCase(CONV_TO_CRISP)) {
			this.threshold = Double.parseDouble(stringValue.trim());
			return;
		} else {
			super.setParameter(name, value);
		}
	}

	/**
	 * Using this setter with a positive value makes the model discard all but the specified number
	 * of base models. A value of -1 turns off this option.
	 */
	public void setMaxModelNumber(int numModels) {
		this.maxModelNumber = numModels;
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

	/** @return the number of embedded models */
	public int getNumberOfModels() {
		if (this.maxModelNumber >= 0) {
			return Math.min(this.maxModelNumber, modelInfo.size());
		} else {
			return modelInfo.size();
		}
	}

	/**
	 * Gets factors for models in the case of general nominal class labels.
	 *
	 * @return a <code>double[]</code> object with the factors to be applied for each class if the
	 *         corresponding rule yields <code>predicted</code>.
	 * @param modelNr
	 *            the number of the model
	 * @param predicted
	 *            the predicted label
	 * @return a <code>double[]</code> with one factor per class label,
	 *         <code>Double.POSITIVE_INFINITY</code> if the rule deterministically predicts a value,
	 *         and <code>RULE_DOES_NOT_APPLY</code> if no prediction can be made.
	 */
	private double[] getFactorsForModel(int modelNr, int predicted) {
		ContingencyMatrix cm = this.modelInfo.get(modelNr).getContingencyMatrix();
		return cm.getLiftRatiosForPrediction(predicted);
	}

	/**
	 * Getter method for prior class probabilities estimated as the relative frequencies in the
	 * training set.
	 *
	 * @param classIndex
	 *            the index of a class, starting with 0
	 * @return the prior probability of the specified class
	 */
	private double getPriorOfClass(int classIndex) {
		return this.priors[classIndex];
	}

	/** Getter for the prior array */
	public double[] getPriors() {
		double[] result = new double[this.priors.length];
		System.arraycopy(this.priors, 0, result, 0, result.length);
		return result;
	}

	/**
	 * Getter method for embedded models
	 *
	 * @param index
	 *            the number of a model part of this boost model
	 * @return binary or nominal decision model for the given classification index.
	 */
	public Model getModel(int index) {
		return this.modelInfo.get(index).getModel();
	}

	/**
	 * Getter method for a specific confusion matrix
	 *
	 * @param index
	 *            the number of the model for which to read the confusion matrix
	 * @return a <code>ConfusionMatrix</code> object
	 */
	public ContingencyMatrix getContingencyMatrix(int index) {
		return this.modelInfo.get(index).getContingencyMatrix();
	}

	/**
	 * Iterates over all models and returns the class with maximum likelihood.
	 *
	 * @param exampleSet
	 *            the set of examples to be classified
	 * @param predictedLabel
	 *            the label that finally holds the predictions
	 */
	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		// Prepare special attributes for storing intermediate results:
		final Attribute[] specialAttributes = this.createSpecialAttributes(exampleSet);
		this.initIntermediateResultAttributes(exampleSet, specialAttributes);

		// initialize progress
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(100);
		}

		// Apply all models to the example set, each time updating the
		// intermediate results:
		for (int i = 0; i < this.getNumberOfModels(); i++) {
			Model model = this.getModel(i);
			ExampleSet clonedExampleSet = (ExampleSet) exampleSet.clone();

			// add observer to observe the progress of the model
			Operator dummy = null;
			if (progress != null) {
				try {
					dummy = OperatorService.createOperator("dummy");
				} catch (OperatorCreationException e) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.operator.learner.meta.BayBoostModel.couldnt_create_operator");
				}
				if (dummy != null && model instanceof AbstractModel) {
					final OperatorProgress finalProgress = progress;
					final int finalModelCounter = i;
					((AbstractModel) model).setOperator(dummy);
					((AbstractModel) model).setShowProgress(true);
					OperatorProgress internalProgress = dummy.getProgress();
					internalProgress.setCheckForStop(false);
					internalProgress.addObserver(new Observer<OperatorProgress>() {

						@Override
						public void update(Observable<OperatorProgress> observable, OperatorProgress arg) {
							try {
								finalProgress.setCompleted((int) (arg.getProgress() / (getNumberOfModels() + 1)
										+ 100.0 * finalModelCounter / (getNumberOfModels() + 1)));
							} catch (ProcessStoppedException e) {
								throw new ProcessStoppedRuntimeException();
							}
						}
					}, false);
				}
			}

			clonedExampleSet = model.apply(clonedExampleSet);
			this.updateEstimates(clonedExampleSet, this.getContingencyMatrix(i), specialAttributes);
			PredictionModel.removePredictedLabel(clonedExampleSet);
			if (progress != null) {
				progress.setCompleted((int) 100.0 * i / (this.getNumberOfModels() + 1));
			}
		}

		// Compute and store probability estimates from the intermediate
		// results:
		Iterator<Example> reader = exampleSet.iterator();
		int progressCounter = 0;
		while (reader.hasNext()) {
			Example example = reader.next();
			this.translateOddsIntoPredictions(example, specialAttributes, getTrainingHeader().getAttributes().getLabel());
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted((int) (100.0 / (this.getNumberOfModels() + 1) * progressCounter / exampleSet.size()
						+ 100.0 * this.getNumberOfModels() / (this.getNumberOfModels() + 1)));
			}
		}

		// Remove the special attributes used for storing intermediate
		// estimates:
		this.cleanUpSpecialAttributes(exampleSet, specialAttributes);

		return exampleSet;
	}

	/** Creates a special attribute for each label to store intermediate results. */
	private Attribute[] createSpecialAttributes(ExampleSet exampleSet) {
		final String attributePrefix = "BayBoostModelPrediction";

		Attribute[] specialAttributes = new Attribute[this.getLabel().getMapping().size()];
		for (int i = 0; i < specialAttributes.length; i++) {
			specialAttributes[i] = com.rapidminer.example.Tools.createSpecialAttribute(exampleSet, attributePrefix + i,
					Ontology.NUMERICAL);
		}
		return specialAttributes;
	}

	/** Removes the provided special labels from the exampleSet and exampleTable. */
	private void cleanUpSpecialAttributes(ExampleSet exampleSet, Attribute[] specialAttributes) {
		for (int i = 0; i < specialAttributes.length; i++) {
			exampleSet.getAttributes().remove(specialAttributes[i]);
			exampleSet.getExampleTable().removeAttribute(specialAttributes[i]);
		}
	}

	private void initIntermediateResultAttributes(ExampleSet exampleSet, Attribute[] specAttrib) {
		// Compute odds ratios from class priors:
		double[] priorOdds = new double[this.priors.length];
		for (int i = 0; i < priorOdds.length; i++) {
			priorOdds[i] = this.priors[i] == 1 ? Double.POSITIVE_INFINITY : this.priors[i] / (1 - this.priors[i]);
		}

		// Initialize each intermediate estimate with the odds ratio of
		// the corresponding class:
		for (int i = 0; i < specAttrib.length; i++) {
			for (Example example : exampleSet) {
				example.setValue(specAttrib[i], priorOdds[i]);
			}
		}
	}

	private void translateOddsIntoPredictions(Example example, Attribute[] specAttrib, Attribute trainingSetLabel) {
		// Turn lift ratio into conditional probabilities:
		double probSum = 0;
		double[] classProb = new double[specAttrib.length];
		int bestIndex = 0;
		for (int n = 0; n < classProb.length; n++) {
			// The probability Prob( C | x ) for class C given the description
			// can
			// be calculated from factor = Prob(C | x) / Prob(neg(C) | x) as
			// Prob( C | x ) = factor / (1 + factor):
			double odds = example.getValue(specAttrib[n]);
			if (Double.isNaN(odds)) {
				logWarning("Found NaN odd ratio estimate.");
				classProb[n] = 1;
			} else {
				classProb[n] = Double.isInfinite(odds) ? 1 : odds / (1 + odds);
			}

			probSum += classProb[n]; // accumulate probabilities, should be 1
			if (classProb[n] > classProb[bestIndex]) {
				bestIndex = n;
			}
		}

		// Normalize probabilities if the sum is not 1.
		// This can happen if the subset defined by a rule does not contain all
		// classes.
		if (probSum != 1.0) {
			for (int k = 0; k < classProb.length; k++) {
				classProb[k] /= probSum;
			}
		}

		// Store the final prediction. All internal computations have used the
		// indices
		// of the stored label. The indices of the new label may be different in
		// case
		// of stored and reloaded models or example sets. For this reason the
		// final
		// predictions are written in terms of the strings, avoiding any
		// assumptions
		// about the mapping.
		final String bestLabel;
		if (this.getLabel().isNominal() && this.getLabel().getMapping().size() == 2 && this.threshold != 0.5) {
			// boolean classification problem --> only in this case a threshold
			// makes sense ...

			// the local indices:
			int posIndex = this.getLabel().getMapping().getPositiveIndex();
			int negIndex = this.getLabel().getMapping().getNegativeIndex();

			// Decide whether threshold is valid, otherwise just use 0.5:
			threshold = this.threshold >= 0 && this.threshold <= 1 ? this.threshold : 0.5;

			// If the threshold is exceeded store the string representing the
			// positive class,
			// otherwise the one for the negative class.
			bestLabel = this.getLabel().getMapping().mapIndex(classProb[posIndex] >= threshold ? posIndex : negIndex);
		} else { // otherwise: just predict the most probable class
			bestLabel = this.getLabel().getMapping().mapIndex(bestIndex);
		}

		// Write the prediction to the example set. In this case the indices of
		// the label currently part of the example set have to be used:
		example.setValue(example.getAttributes().getPredictedLabel(), trainingSetLabel.getMapping().mapString(bestLabel));

		// Set confidence values for all classes:
		for (int k = 0; k < classProb.length; k++) {
			// The locally used attribute indices correspond to the stored
			// label,
			// so the String representation required for setting confidences is
			// derived from the stored label.
			if (Double.isNaN(classProb[k]) || classProb[k] < 0 || classProb[k] > 1) {
				logWarning("Found illegal confidence value: " + classProb[k]);
			}
			example.setConfidence(this.getLabel().getMapping().mapIndex(k), classProb[k]);
		}

	}

	private void updateEstimates(ExampleSet exampleSet, ContingencyMatrix cm, Attribute[] specialAttributes) {
		for (Example example : exampleSet) {
			int predicted = (int) example.getPredictedLabel();

			L: for (int j = 0; j < cm.getNumberOfClasses(); j++) {
				final double liftRatioCurrent = cm.getLiftRatio(j, predicted); // rule: predicted =>
				// j

				// Change the intermediate estimates, take care about
				// deterministic and non-applicable rules:
				if (Double.isNaN(liftRatioCurrent)) { // RULE_DOES_NOT_APPLY,
					logWarning("Ignoring non-applicable model."); // ignore it
					continue L;
				} else if (Double.isInfinite(liftRatioCurrent)) { // Double.POSITIVE_INFINITY
					if (example.getValue(specialAttributes[j]) != 0) {
						for (int k = 0; k < specialAttributes.length; k++) {
							// reset all probabilities to 0
							example.setValue(specialAttributes[k], 0);
						}
						// class is deterministically correct
						example.setValue(specialAttributes[j], liftRatioCurrent);
					} else {
						continue L; // ignore factor, class is already known to
						// be deterministically incorrect
					}
				} else {
					// the "normal" case
					double oldValue = example.getValue(specialAttributes[j]);
					if (Double.isNaN(oldValue)) {
						logWarning("Found NaN value in intermediate odds ratio estimates!");
					}
					if (!Double.isInfinite(oldValue)) {
						example.setValue(specialAttributes[j], oldValue * liftRatioCurrent);
					}
				}
			}
		}
	}

	/**
	 * Helper method to adjust the intermediate products during model application.
	 *
	 * @param products
	 *            the intermediate products, these values are changed by the method
	 * @param liftFactors
	 *            the factor vector that applies for the prediction for the current example
	 *
	 * @return <code>true</code> iff the class is deterministically known after applying this method
	 */
	public static boolean adjustIntermediateProducts(double[] products, double[] liftFactors) {
		L: for (int i = 0; i < liftFactors.length; i++) {
			// Change the intermediate estimates, take care about deterministic
			// and non-applicable rules:
			if (Double.isNaN(liftFactors[i])) { // WeightedPerformanceMeasures.RULE_DOES_NOT_APPLY)
				// LogService.getGlobal().log("Ignoring non-applicable model.", LogService.WARNING);
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.operator.leaner.meta.BayBoostModel.ignoring_non_applicable_model");
				continue L;
			} else if (Double.isInfinite(liftFactors[i])) {
				if (products[i] != 0) {
					for (int j = 0; j < products.length; j++) {
						products[j] = 0; // reset all probabilities to 0
					}
					products[i] = liftFactors[i]; // class is deterministically correct
					return true; // class is known
				} else {
					continue L; // ignore factor, class is already known to be
					// deterministically incorrect
				}
			} else { // the "normal" case
				products[i] *= liftFactors[i];
				if (Double.isNaN(products[i])) {
					// LogService.getGlobal().log("Found NaN value in intermediate odds ratio
					// estimates!",
					// LogService.WARNING);
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.operator.leaner.meta.BayBoostModel.found_nan_value");
				}
			}
		}
		return false;
	}

	/**
	 * This method is only supported for boolean target attributes. It computes a flattened version
	 * of model weights. In constrast to the original version the final predictions are additive
	 * logarithms of the lift ratios, additively rescaled so that the prediction <code>false</code>
	 * of model i produces <code>-i</code> if <code>true</code> produces weight i. This means that
	 * only one weight per model is required. The first component of the returned array is the part
	 * that is independent of any prediction, the i-th component is the weight of model i. The
	 * (log-)linear model predicts depending on whether the linear combination of predictions
	 * (either -1 or 1) is greater than 0 or not. Infinite values are problematic, so a min/max
	 * value is used.
	 *
	 * @return the flattened weights of all models
	 */
	public double[] getModelWeights() throws OperatorException {
		if (this.getLabel().getMapping().size() != 2) {
			throw new UserError(null, 114, "BayBoostModel", this.getLabel());
		}

		int maxWeight = 10;

		final int pos = this.getLabel().getMapping().getPositiveIndex();
		final int neg = this.getLabel().getMapping().getNegativeIndex();
		double[] weights = new double[this.getNumberOfModels() + 1];

		// initialise model independent part
		double odds = this.getPriorOfClass(pos) / this.getPriorOfClass(neg);
		weights[0] = Math.log(odds);

		for (int i = 1; i < weights.length; i++) {

			double logPosRatio, logNegRatio;
			{
				double liftRatiosPos[] = this.getFactorsForModel(i - 1, pos); // lift
				// ratios
				// for
				// pos
				// prediction
				logPosRatio = Math.log(liftRatiosPos[pos]); // factor applied to
				// positive class
				logPosRatio = Math.min(maxWeight, Math.max(-maxWeight, logPosRatio)); // exclude
				// infinity
				// etc.

				double liftRatiosNeg[] = this.getFactorsForModel(i - 1, neg); // lift
				// ratios
				// for
				// neg
				// prediction
				logNegRatio = Math.log(liftRatiosNeg[pos]); // also the factor
				// applied to the
				// positive class
				logNegRatio = Math.min(maxWeight, Math.max(-maxWeight, logNegRatio));
			}

			// Compute the offset part of both predictions.
			// This requires to compare the factors applied to the same
			// (positive in this case) class,
			// one time when the model predicts positive, and one time if it
			// predicts negative.
			double indep = (logPosRatio + logNegRatio) / 2;
			if (Tools.isEqual(indep, maxWeight) || Tools.isEqual(indep, -maxWeight)) {
				// This should not happen. Obviously we found a dummy-model,
				// because the prediction is not required!
				// Do not just shift the lift, but indicate this point by an
				// illegal value:
				logPosRatio = 10 * indep;
				indep = 0;
			}

			// Update model independent part, which is valid if both
			// model-dependent updates are also made:
			weights[0] += indep;
			// Update model independent weights:
			logPosRatio -= indep; // Next step in principle: logNegRatio -=
			// indep, then logNegRatio == logPosRatio.
			// Goal reached: One weight suffices per model.
			weights[i] = logPosRatio;
		}
		return weights;
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
		List<Model> models = new LinkedList<Model>();
		for (int i = 0; i < this.getNumberOfModels(); i++) {
			models.add(getModel(i));
		}
		return models;
	}
}
