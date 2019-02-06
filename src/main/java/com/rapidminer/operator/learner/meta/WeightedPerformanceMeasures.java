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
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;

import java.util.Iterator;
import java.util.logging.Level;


/**
 * This private class cares about <i>weighted</i> performance measures as used by the
 * <code>BayesianBoosting</code> algorithm and the similarly working <code>ModelBasedSampling</code>
 * operator.
 * 
 * @author Martin Scholz
 */
public class WeightedPerformanceMeasures {

	/** This constant is used to express that no examples have been observed. */
	public static final double RULE_DOES_NOT_APPLY = Double.NaN;

	private double[] predictions;

	private double[] labels;

	private double[][] pred_label;

	// The total number of examples without considering any weights:
	private int[][] unweighted_num_pred_label;

	/**
	 * Constructor. Reads an example set, calculates its weighted performance values and caches them
	 * internally for later requests.
	 * 
	 * @param exampleSet
	 *            the <code>ExampleSet</code> this object shall hold the performance measures for
	 */
	public WeightedPerformanceMeasures(ExampleSet exampleSet) throws OperatorException {
		{
			int numberOfClasses = exampleSet.getAttributes().getLabel().getMapping().getValues().size();
			this.labels = new double[numberOfClasses];

			// It is not necessary to interpret the result of the embedded
			// learner as predictions. Especially it not mandatory to have as
			// many "predictions" as labels. However, without any further information
			// let's assume the simple case, namely that the learner tries to
			// predict the label with the result of the model:
			this.predictions = new double[numberOfClasses];

			// This array stores all combinations:
			this.pred_label = new double[this.predictions.length][this.labels.length];

			// The same array for unweighted examples:
			this.unweighted_num_pred_label = new int[this.predictions.length][this.labels.length];
		}

		Iterator<Example> reader = exampleSet.iterator();
		double sumOfWeights = 0;

		while (reader.hasNext()) {
			// crisp base classifier, multi-class prediction problems possible
			Example exa = reader.next();
			double exaW = exa.getWeight();
			sumOfWeights += exaW;
			int eLabel = (int) (exa.getLabel());
			int ePred = (int) (exa.getPredictedLabel());

			if ((ePred >= 0 && ePred < this.predictions.length) && (eLabel >= 0 && eLabel < this.labels.length)) {
				this.unweighted_num_pred_label[ePred][eLabel]++;

				this.labels[eLabel] += exaW;
				this.predictions[ePred] += exaW;
				this.pred_label[ePred][eLabel] += exaW;
			} else { // try to ignore unrecognized labels and predictions
				exa.setWeight(0);
				exa.setLabel(0);
				exa.setPredictedLabel(0);
				// LogService.getGlobal().log("WeightedPerformanceMeasures: Deleted example with illegal label or prediction ("
				// + eLabel + ", " +
				// ePred + ")!", LogService.WARNING);
				LogService
						.getRoot()
						.log(Level.WARNING,
								"com.rapidminer.operator.learner.meta.WeightedPerformanceMeasures.deleted_example_with_illega_label",
								new Object[] { eLabel, ePred });
			}
		}

		if (sumOfWeights > 0) {
			// If sum is 0 all examples have been "explained deterministically"!
			// Otherwise: Normalize!
			for (int i = 0; i < this.predictions.length; i++) {
				this.predictions[i] /= sumOfWeights;
				for (int j = 0; j < this.labels.length; j++) {
					this.pred_label[i][j] /= sumOfWeights;
				}
			}

			for (int j = 0; j < this.labels.length; j++) {
				this.labels[j] /= sumOfWeights;
			}
		} else { // Assign default values to all fields.
			double defaultPredProb = 1 / ((double) this.predictions.length);
			double defaultLabelProb = 1 / ((double) this.labels.length);
			double defaultPredLabelProb = defaultPredProb * defaultLabelProb;

			for (int i = 0; i < this.predictions.length; i++) {
				this.predictions[i] = defaultPredProb;
				for (int j = 0; j < this.labels.length; j++) {
					this.pred_label[i][j] = defaultPredLabelProb;
				}
			}
			for (int j = 0; j < this.labels.length; j++) {
				this.labels[j] = defaultLabelProb;
			}
		}
	}

	/**
	 * Method to query for the unweighted absolute number of covered examples of each class, given a
	 * specific prediction
	 * 
	 * @param prediction
	 *            the value predicted by the model (internal index number)
	 * @return an <code>int[]</code> array with the number of examples of class <code>i</code>
	 *         (internal index number) stored at index <code>i</code>.
	 */
	public int[] getCoveredExamplesNumForPred(int prediction) {
		int length = this.unweighted_num_pred_label.length;
		if (prediction >= 0 && prediction < length) {
			return this.unweighted_num_pred_label[prediction];
		} else {
			return new int[length]; // unknown prediction: no instances covered
		}
	}

	/**
	 * @return the number of classes, namely different values of this object's example set's label
	 *         attribute
	 */
	public int getNumberOfLabels() {
		return this.labels.length;
	}

	/**
	 * @return number of predictions or nominal classes predicted by the embedded learner. Not
	 *         necessarily the same as the number of class labels.
	 */
	public int getNumberOfPredictions() {
		return this.predictions.length;
	}

	/**
	 * Method to query for the probability of one of the prediction/label subsets
	 * 
	 * @param label
	 *            the (correct) class label of the example as it comes from the internal index
	 * @param prediction
	 *            the boolean value predicted by the model (premise) (internal index number)
	 * @return the joint probability of label and prediction
	 */
	public double getProbability(int label, int prediction) {
		return this.pred_label[prediction][label];
	}

	/**
	 * Method to query for the &quot;prior&quot; probability of one of the labels.
	 * 
	 * @param label
	 *            the nominal class label
	 * @return the probability of seeing an example with this label
	 */
	public double getProbabilityLabel(int label) {
		return this.labels[label];
	}

	/**
	 * Method to query for the &quot;prior&quot; probability of one of the predictions.
	 * 
	 * @param premise
	 *            the prediction of a model
	 * @return the probability of drawing an example so that the model makes this prediction
	 */
	public double getProbabilityPrediction(int premise) {
		return this.predictions[premise];
	}

	/**
	 * The lift of the rule specified by the nominal variable's indices.
	 * <code>RULE_DOES_NOT_APPLY</code> is returned to indicate that no such example has ever been
	 * observed, <code>Double.POSITIVE_INFINITY</code> is returned if the class membership can
	 * deterministically be concluded from the prediction.
	 * 
	 * Important: In the multi-class case some of the classes might not be observed at all when a
	 * specific rule applies, but still the rule does not necessarily have a deterministic part. In
	 * this case the remaining number of classes is considered to be the complete set of classes
	 * when calculating the default values and lifts! This does not affect the prediction of the
	 * most likely class label, because the classes not observed have a probability of one, the
	 * other estimates increase proportionally. However, to calculate probabilities it is necessary
	 * to normalize the estimates in the class <code>BayBoostModel</code>.
	 * 
	 * @param label
	 *            the true label
	 * @param prediction
	 *            the predicted label
	 * @return the LIFT, which is a value >= 0, positive infinity if all examples with this
	 *         prediction belong to that class (deterministic rule), or
	 *         <code>RULE_DOES_NOT_APPLY</code> if no prediction can be made.
	 */
	public double getLift(int label, int prediction) {
		double prLabel = this.getProbabilityLabel(label);
		double prPred = this.getProbabilityPrediction(prediction);
		double prJoint = this.getProbability(label, prediction);
		if (prPred == 0) {
			return RULE_DOES_NOT_APPLY;
		} else if (prJoint == 0) {
			return (0);
		} else if (Tools.isEqual(prJoint, prPred)) {
			return (Double.POSITIVE_INFINITY);
		}

		double lift = prJoint / (prLabel * prPred);

		return lift;
	}

	/**
	 * The factor to be applied (pn-ratio) for each label if the model yields the specific
	 * prediction.
	 * 
	 * @param prediction
	 *            the predicted class
	 * @return a <code>double[]</code> array containing one factor for each class. The result should
	 *         either consist of well defined lifts >= 0, or all fields should mutually contain the
	 *         constant <code>RULE_DOES_NOT_APPLY</code>.
	 */
	public double[] getPnRatios(int prediction) {
		double[] lifts = new double[this.labels.length];
		for (int i = 0; i < lifts.length; i++) {
			int rapidMinerLabelIndex = i;
			double b = this.getLift(rapidMinerLabelIndex, prediction);
			if (b == 0 || b == Double.POSITIVE_INFINITY) {
				lifts[i] = b;
			} else {
				// In this case the corresponding lift of the remaining classes
				// should also be defined. Using the odds avoids calculating the
				// probability of premises.
				double negLabel = 1 - this.getProbabilityLabel(rapidMinerLabelIndex);
				double probPred = this.getProbabilityPrediction(prediction);
				double probPredLabel = this.getProbability(rapidMinerLabelIndex, prediction);
				double negLabelPred = probPred - probPredLabel;

				double oppositeLift = negLabelPred / (negLabel * probPred);

				// What is stored is
				// Lift( pred -> label) / Lift( pred -> neg(label) ):
				lifts[i] = b / oppositeLift;
			}
		}
		return lifts;
	}

	/**
	 * @return a matrix with one pn-factor per prediction/label combination, or the priors of
	 *         predictions for the case of soft base classifiers.
	 */
	public double[][] createLiftRatioMatrix() {
		int numPredictions = this.getNumberOfPredictions();
		double[][] liftRatioMatrix = new double[numPredictions][];
		for (int i = 0; i < numPredictions; i++) {
			liftRatioMatrix[i] = this.getPnRatios(i);
		}
		return liftRatioMatrix;
	}

	/**
	 * @return a <code>double[]</code> with the prior probabilities of all class labels.
	 */
	public double[] getLabelPriors() {
		double[] priors = new double[this.getNumberOfLabels()];
		for (int i = 0; i < priors.length; i++) {
			priors[i] = this.getProbabilityLabel(i);
		}
		return priors;
	}

	/**
	 * @return the number of classes with strictly positive weight
	 */
	public int getNumberOfNonEmptyClasses() {
		int nonEmpty = 0;
		for (int i = 0; i < this.getNumberOfLabels(); i++) {
			if (this.getProbabilityLabel(i) > 0) {
				nonEmpty++;
			}
		}
		return nonEmpty;
	}

	/** converts the deprecated representation into the new form */
	public ContingencyMatrix getContingencyMatrix() {

		if (this.pred_label.length == 0 || this.pred_label[0].length == 0) {
			return new ContingencyMatrix(new double[0][0]); // doesn't make sense
		}

		double[][] matrix = new double[this.pred_label[0].length][this.pred_label.length];
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {

				final double predLabelJi = this.pred_label[j][i];
				// Errors like this are hard to find, so this is worth a warning message:
				if (Double.isNaN(predLabelJi) || predLabelJi < 0 || predLabelJi > 1) {
					// LogService.getGlobal().log("Found illegal value in contingency matrix!",
					// LogService.WARNING);
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.operator.learner.meta.WeightedPerformanceMeasures.found_illegal_value");
				}

				matrix[i][j] = predLabelJi;
			}
		}

		return new ContingencyMatrix(matrix);
	}

	/**
	 * Helper method of the <code>BayesianBoosting</code> operator
	 * 
	 * This method reweights the example set with respect to the
	 * <code>WeightedPerformanceMeasures</code> object. Please note that the weights will not be
	 * reset at any time, because they continuously change from one iteration to the next. This
	 * method does not change the priors of the classes.
	 * 
	 * @param exampleSet
	 *            <code>ExampleSet</code> to be reweighted
	 * @param cm
	 *            the <code>ContingencyMatrix</code> as e.g. returned by
	 *            <code>WeightedPerformanceMeasures</code>
	 * @param allowMarginalSkews
	 *            indicates whether the weight of covered and uncovered subsets are allowed to
	 *            change.
	 * @return the total weight
	 */
	public static double reweightExamples(ExampleSet exampleSet, ContingencyMatrix cm, boolean allowMarginalSkews)
			throws OperatorException {
		Iterator<Example> reader = exampleSet.iterator();
		double totalWeight = 0;

		Attribute labelAttribute = exampleSet.getAttributes().getLabel();
		Attribute predictedLabel = exampleSet.getAttributes().getPredictedLabel();
		Attribute weightAttribute = exampleSet.getAttributes().getWeight();

		while (reader.hasNext()) {
			Example example = reader.next();
			int label = (int) example.getValue(labelAttribute);

			int predicted = (int) example.getValue(predictedLabel);
			double lift = cm.getLift(label, predicted);

			if (Double.isNaN(lift) || lift < 0) {
				// == RULE_DOES_NOT_APPLY || serious error
				// LogService.getGlobal().log("Applied rule with an illegal lift of "
				// + lift + " during reweighting!", LogService.WARNING);
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.operator.learner.meta.WeightedPerformanceMeasures.applied_rule_with_illegal_lift",
						lift);
			} else if (lift == 0 || Double.isInfinite(lift)) {
				// In both cases the model predicts deterministically, so we can
				// remove the example from further investigation.
				// lift = 0: model misclassifies, cannot happen for the original
				// training set, but in other contexts
				// Infinite: model classifies correctly
				example.setValue(weightAttribute, 0);
			} else {
				// this is the normal setting, just make sure that the weights are ok

				double weight = example.getValue(weightAttribute);
				double newWeight;

				if (Double.isNaN(weight) || Double.isInfinite(weight) || weight < 0) {
					// Infinite, NaN, and negative weights cannot be processed any further
					// in a meaningful way!
					// LogService.getGlobal().log("Found illegal weight: " + weight,
					// LogService.WARNING);
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.operator.learner.meta.WeightedPerformanceMeasures.found_illegal_weight", weight);
					newWeight = 0; // try to continue anyway
				} else if (weight == 0) {
					// nothing to do
					continue;
				} else if (allowMarginalSkews) {
					double prec = cm.getPrecision(label, predicted); // ~ Acc = 1 - epsilon
					double invPrec = 1 - prec; // epsilon
					double beta = invPrec / prec; // beta = epsilon / ( 1 - epsilon)

					// Sanity check: prec > 0 because lift > 0, beta has to be a regular double >= 0
					if (prec <= 0 || invPrec < 0 || Double.isInfinite(beta) || Double.isNaN(beta)) {
						// LogService.getGlobal().log(("Reweighting uses invalid value:"
						// + "Precision is " + prec + ", inverse precision is " + invPrec
						// + ", beta is " + beta), LogService.WARNING);
						LogService
								.getRoot()
								.log(Level.WARNING,
										"com.rapidminer.operator.learner.meta.WeightedPerformanceMeasures.reweighting_uses_invalid_value",
										new Object[] { prec, invPrec, beta });
					}
					newWeight = weight * Math.sqrt(beta);
				} else {
					newWeight = weight / lift;
				}

				// set the new weight and remember the
				example.setValue(weightAttribute, newWeight);
				totalWeight += newWeight;
			}
		}
		return totalWeight;
	}

}
