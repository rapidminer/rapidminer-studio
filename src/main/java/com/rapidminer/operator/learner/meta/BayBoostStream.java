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
import java.util.List;
import java.util.Vector;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Condition;
import com.rapidminer.example.set.ConditionedExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.RunVector;


/**
 * Assumptions:
 * <ol>
 * <li>target label is always boolean</li>
 * <li>goal is to fit a crisp ensemble classifier (use_distribution always off)</li>
 * <li>base classifier weights are always adapted by a single row from first to last</li>
 * <li>no internal bootstrapping</li>
 * </ul>
 *
 * @author Martin Scholz
 *
 * @deprecated This learner is not used anymore.
 */
@Deprecated
public class BayBoostStream extends AbstractMetaLearner {

	private OutputPort runVectorOutput = getOutputPorts().createPort("run vector");

	/**
	 * Class that filters an ExampleSet by the value of a special attribute. The constructor is
	 * provided with the attribute and the selected value. Neither this value nor the values of the
	 * attribute in the data are supposed to change. Please note, that the batch number is compared
	 * with greater or equal, which is comfortable for the case of merging a sequence of batches.
	 */
	public static class BatchFilterCondition implements Condition {

		private static final long serialVersionUID = 7910713773299060449L;

		private final int batchNumber;

		private final Attribute attribute;

		public BatchFilterCondition(Attribute attribute, int batchNumber) {
			this.batchNumber = batchNumber;
			this.attribute = attribute;
		}

		@Override
		public boolean conditionOk(Example example) {
			return example.getValue(attribute) >= this.batchNumber;
		}

		@Override
		public Condition duplicate() {
			return this;
		}
	}

	/**
	 * Name of the variable specifying the maximal number of iterations of the learner.
	 */
	public static final String PARAMETER_BATCH_SIZE = "batch_size";

	/**
	 * Boolean parameter to specify whether the label priors should be equally likely after first
	 * iteration.
	 */
	public static final String PARAMETER_RESCALE_LABEL_PRIORS = "rescale_label_priors";

	/** Parameter name to activate a hold out set for tuning. */
	public static final String PARAMETER_FRACTION_HOLD_OUT_SET = "fraction_hold_out_set";

	/** Discard models with an advantage of less than the specified value. */
	public static final double MIN_ADVANTAGE = 0.02;

	/** Name of the special attribute with additional stream control information. */
	public static final String STREAM_CONTROL_ATTRIB_NAME = "BayBoostStream.StreamControl";

	/**
	 * The probabilistic prediction of soft classifiers is restricted, similar to a confidence
	 * bound. If the lift is close to 0 it is replaced by the minimum lift below. Analogously a
	 * maximum lift value is defined by (1 / MIN_LIFT_RATIO_SOFT_CLASSIFIER).
	 */
	public static final double MIN_LIFT_RATIO_SOFT_CLASSIFIER = 0.2;

	// the accuracy results over time
	private RunVector runVector;

	// field for visualizing performance
	private int currentIteration;

	// A performance measure to be visualized. Not yet implemented!
	private double performance = 0;

	// Backup of the original weights
	private double[] oldWeights;

	private int batchSize;

	/** Constructor. */
	public BayBoostStream(OperatorDescription description) {
		super(description);

		getTransformer().addRule(new GenerateNewMDRule(runVectorOutput, RunVector.class));

		addValue(new ValueDouble("performance", "The performance.") {

			@Override
			public double getDoubleValue() {
				return performance;
			}
		});
		addValue(new ValueDouble("iteration", "The current iteration.") {

			@Override
			public double getDoubleValue() {
				return currentIteration;
			}
		});
	}

	/**
	 * Overrides the method of the super class.
	 */
	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		switch (lc) {
			case NUMERICAL_LABEL:
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
				return false;
			default:
				return true;
		}
	}

	protected void prepareWeights(ExampleSet exampleSet) {
		Attribute weightAttr = exampleSet.getAttributes().getWeight();
		if (weightAttr == null) {
			this.oldWeights = null;
			com.rapidminer.example.Tools.createWeightAttribute(exampleSet);
		} else { // Back up old weights
			this.oldWeights = new double[exampleSet.size()];
			Iterator<Example> reader = exampleSet.iterator();

			for (int i = 0; reader.hasNext() && i < oldWeights.length; i++) {
				Example example = reader.next();
				if (example != null) {
					this.oldWeights[i] = example.getWeight();
					example.setWeight(1);
				}
			}
		}
	}

	private void restoreOldWeights(ExampleSet exampleSet) {
		if (this.oldWeights != null) { // need to reset weights
			Iterator<Example> reader = exampleSet.iterator();
			int i = 0;
			while (reader.hasNext() && i < this.oldWeights.length) {
				reader.next().setWeight(this.oldWeights[i++]);
			}
		} else { // need to delete the weights attribute
			Attribute weight = exampleSet.getAttributes().getWeight();
			exampleSet.getAttributes().remove(weight);
			exampleSet.getExampleTable().removeAttribute(weight);
		}
	}

	/**
	 * Constructs a <code>Model</code> repeatedly running a weak learner, reweighting the training
	 * example set accordingly, and combining the hypothesis using the available weighted
	 * performance values.
	 */
	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		this.runVector = new RunVector();
		BayBoostModel ensembleNewBatch = null;
		BayBoostModel ensembleExtBatch = null;
		final Vector<BayBoostBaseModelInfo> modelInfo = new Vector<BayBoostBaseModelInfo>(); // for
		// models
		// and
		// their
		// probability
		// estimates
		Vector<BayBoostBaseModelInfo> modelInfo2 = new Vector<BayBoostBaseModelInfo>();
		this.currentIteration = 0;
		int firstOpenBatch = 1;

		// prepare the stream control attribute
		final Attribute streamControlAttribute;
		{
			Attribute attr = null;
			if ((attr = exampleSet.getAttributes().get(STREAM_CONTROL_ATTRIB_NAME)) == null) {
				streamControlAttribute = com.rapidminer.example.Tools.createSpecialAttribute(exampleSet,
						STREAM_CONTROL_ATTRIB_NAME, Ontology.INTEGER);
			} else {
				streamControlAttribute = attr;
				logWarning(
						"Attribute with the (reserved) name of the stream control attribute exists. It is probably an old version created by this operator. Trying to recycle it... ");
				// Resetting the stream control attribute values by overwriting
				// them with 0 avoids (unlikely)
				// problems in case the same ExampleSet is passed to this
				// operator over and over again:
				Iterator<Example> e = exampleSet.iterator();
				while (e.hasNext()) {
					e.next().setValue(streamControlAttribute, 0);
				}
			}
		}

		// and the weight attribute
		if (exampleSet.getAttributes().getWeight() == null) {
			this.prepareWeights(exampleSet);
		}

		boolean estimateFavoursExtBatch = true;
		// *** The main loop, one iteration per batch: ***
		Iterator<Example> reader = exampleSet.iterator();
		final double holdOutRatio = this.getParameterAsDouble(PARAMETER_FRACTION_HOLD_OUT_SET);
		batchSize = this.getParameterAsInt(PARAMETER_BATCH_SIZE);
		boolean rescaleLabelPriors = this.getParameterAsBoolean(PARAMETER_RESCALE_LABEL_PRIORS);
		while (reader.hasNext()) {
			// increment batch number, collect batch and evaluate performance of
			// current model on batch
			double[] classPriors = this.prepareBatch(++this.currentIteration, reader, streamControlAttribute);

			ConditionedExampleSet trainingSet = new ConditionedExampleSet(exampleSet,
					new BatchFilterCondition(streamControlAttribute, this.currentIteration));

			final EstimatedPerformance estPerf;

			// Step 1: apply the ensemble model to the current batch (prediction
			// phase), evaluate and store result
			if (ensembleExtBatch != null) {
				// apply extended batch model first:
				trainingSet = (ConditionedExampleSet) ensembleExtBatch.apply(trainingSet);
				this.performance = evaluatePredictions(trainingSet); // unweighted
				// performance;

				// then apply new batch model:
				trainingSet = (ConditionedExampleSet) ensembleNewBatch.apply(trainingSet);
				double newBatchPerformance = evaluatePredictions(trainingSet);

				// heuristic: use extended batch model for predicting
				// unclassified instances
				if (estimateFavoursExtBatch == true) {
					estPerf = new EstimatedPerformance("accuracy", this.performance, trainingSet.size(), false);
				} else {
					estPerf = new EstimatedPerformance("accuracy", newBatchPerformance, trainingSet.size(), false);
				}

				// final double[] ensembleWeights;

				// continue with the better model:
				if (newBatchPerformance > this.performance) {
					this.performance = newBatchPerformance;
					firstOpenBatch = Math.max(1, this.currentIteration - 1);
					// ensembleWeights = ensembleNewBatch.getModelWeights();
				} else {
					modelInfo.clear();
					modelInfo.addAll(modelInfo2);
					// ensembleWeights = ensembleExtBatch.getModelWeights();
				}

			} else if (ensembleNewBatch != null) {
				trainingSet = (ConditionedExampleSet) ensembleNewBatch.apply(trainingSet);
				this.performance = evaluatePredictions(trainingSet);
				firstOpenBatch = Math.max(1, this.currentIteration - 1);
				estPerf = new EstimatedPerformance("accuracy", this.performance, trainingSet.size(), false);
			} else {
				estPerf = null; // no model ==> no prediction performance
			}

			if (estPerf != null) {
				PerformanceVector perf = new PerformanceVector();
				perf.addAveragable(estPerf);
				this.runVector.addVector(perf);
			}

			// *** retraining phase ***
			// Step 2: First reconstruct the initial weighting, if necessary.
			if (rescaleLabelPriors == true) {
				this.rescalePriors(trainingSet, classPriors);
			}

			estimateFavoursExtBatch = true;
			// Step 3: Find better weights for existing models and continue
			// training
			if (modelInfo.size() > 0) {

				modelInfo2 = new Vector<BayBoostBaseModelInfo>();
				for (BayBoostBaseModelInfo bbbmi : modelInfo) {
					modelInfo2.add(bbbmi); // BayBoostBaseModelInfo objects
					// cannot be changed, no deep copy
					// required
				}

				// separate hold out set
				Vector<Example> holdOutExamples = new Vector<Example>();
				if (holdOutRatio > 0) {
					RandomGenerator random = RandomGenerator.getRandomGenerator(this);
					Iterator<Example> randBatchReader = trainingSet.iterator();
					while (randBatchReader.hasNext()) {
						Example example = randBatchReader.next();
						if (random.nextDoubleInRange(0, 1) <= holdOutRatio) {
							example.setValue(streamControlAttribute, 0);
							holdOutExamples.add(example);
						}
					}
					// TODO: create new example set
					// trainingSet.updateCondition();
				}

				// model 1: train one more base classifier
				boolean trainingExamplesLeft = this.adjustBaseModelWeights(trainingSet, modelInfo);
				if (trainingExamplesLeft) {
					// "trainingExamplesLeft" needs to be checked to avoid
					// exceptions.
					// Anyway, learning does not make sense, otherwise.
					if (!this.trainAdditionalModel(trainingSet, modelInfo)) {
					}
				}
				ensembleNewBatch = new BayBoostModel(exampleSet, modelInfo, classPriors);

				// model 2: remove last classifier, extend batch, train on
				// extended batch
				ExampleSet extendedBatch = // because of the ">=" condition it
						// is sufficient to remember the
						// opening batch
						new ConditionedExampleSet(exampleSet,
								new BatchFilterCondition(streamControlAttribute, firstOpenBatch));
				classPriors = this.prepareExtendedBatch(extendedBatch);
				if (rescaleLabelPriors == true) {
					this.rescalePriors(extendedBatch, classPriors);
				}
				modelInfo2.remove(modelInfo2.size() - 1);
				trainingExamplesLeft = this.adjustBaseModelWeights(extendedBatch, modelInfo2);
				// If no training examples are left: no need and chance to
				// continue training.
				if (trainingExamplesLeft == false) {
					ensembleExtBatch = new BayBoostModel(exampleSet, modelInfo2, classPriors);
				} else {
					boolean success = this.trainAdditionalModel(extendedBatch, modelInfo2);
					if (success) {
						ensembleExtBatch = new BayBoostModel(exampleSet, modelInfo2, classPriors);
					} else {
						ensembleExtBatch = null;
						estimateFavoursExtBatch = false;
					}
				}

				if (holdOutRatio > 0) {
					Iterator<Example> hoEit = holdOutExamples.iterator();
					while (hoEit.hasNext()) {
						hoEit.next().setValue(streamControlAttribute, this.currentIteration);
					}
					// TODO: create new example set
					// trainingSet.updateCondition();

					if (ensembleExtBatch != null) {
						trainingSet = (ConditionedExampleSet) ensembleNewBatch.apply(trainingSet);
						hoEit = holdOutExamples.iterator();
						int errors = 0;
						while (hoEit.hasNext()) {
							Example example = hoEit.next();
							if (example.getPredictedLabel() != example.getLabel()) {
								errors++;
							}
						}
						double newBatchErr = (double) errors / holdOutExamples.size();

						trainingSet = (ConditionedExampleSet) ensembleExtBatch.apply(trainingSet);
						hoEit = holdOutExamples.iterator();
						errors = 0;
						while (hoEit.hasNext()) {
							Example example = hoEit.next();
							if (example.getPredictedLabel() != example.getLabel()) {
								errors++;
							}
						}
						double extBatchErr = (double) errors / holdOutExamples.size();

						estimateFavoursExtBatch = extBatchErr <= newBatchErr;

						if (estimateFavoursExtBatch) {
							ensembleExtBatch = this.retrainLastWeight(ensembleExtBatch, trainingSet, holdOutExamples);
						} else {
							ensembleNewBatch = this.retrainLastWeight(ensembleNewBatch, trainingSet, holdOutExamples);
						}
					} else {
						ensembleNewBatch = this.retrainLastWeight(ensembleNewBatch, trainingSet, holdOutExamples);
					}
				}
			} else {
				this.trainAdditionalModel(trainingSet, modelInfo);
				ensembleNewBatch = new BayBoostModel(exampleSet, modelInfo, classPriors);
				ensembleExtBatch = null;
				estimateFavoursExtBatch = false;
			}
		}
		this.restoreOldWeights(exampleSet);
		return ensembleExtBatch == null ? ensembleNewBatch : ensembleExtBatch;
	}

	private BayBoostModel retrainLastWeight(BayBoostModel ensemble, ExampleSet exampleSet, Vector<Example> holdOutSet)
			throws OperatorException {
		this.prepareExtendedBatch(exampleSet); // method fits by chance
		int modelNum = ensemble.getNumberOfModels();
		Vector<BayBoostBaseModelInfo> modelInfo = new Vector<BayBoostBaseModelInfo>();
		double[] priors = ensemble.getPriors();
		for (int i = 0; i < modelNum - 1; i++) {
			Model model = ensemble.getModel(i);
			ContingencyMatrix cm = ensemble.getContingencyMatrix(i);
			modelInfo.add(new BayBoostBaseModelInfo(model, cm));
			exampleSet = model.apply(exampleSet);
			WeightedPerformanceMeasures.reweightExamples(exampleSet, cm, false);
		}
		Model latestModel = ensemble.getModel(modelNum - 1);
		exampleSet = latestModel.apply(exampleSet);

		// quite ugly:
		double[] weights = new double[holdOutSet.size()];
		Iterator<Example> it = holdOutSet.iterator();
		int index = 0;
		while (it.hasNext()) {
			Example example = it.next();
			weights[index++] = example.getWeight();
		}
		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			reader.next().setWeight(0);
		}
		it = holdOutSet.iterator();
		index = 0;
		while (it.hasNext()) {
			Example example = it.next();
			example.setWeight(weights[index++]);
		}

		WeightedPerformanceMeasures wp = new WeightedPerformanceMeasures(exampleSet);
		modelInfo.add(new BayBoostBaseModelInfo(latestModel, wp.getContingencyMatrix()));

		return new BayBoostModel(exampleSet, modelInfo, priors);
	}

	/** Overwrite to also return the performance (run-) vector */
	@Override
	public void doWork() throws OperatorException {
		super.doWork();
		runVectorOutput.deliver(runVector);
	}

	/**
	 * Computes the weighted class priors of the boolean target attribute and shifts weights so that
	 * the priors are equal afterwards.
	 */
	private void rescalePriors(ExampleSet exampleSet, double[] classPriors) {
		// The weights of class i are calculated as
		// (1 / #classes) / (#rel_freq_class_i)
		double[] weights = new double[2];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = 1.0d / (weights.length * classPriors[i]);
		}

		Iterator<Example> exRead = exampleSet.iterator();
		while (exRead.hasNext()) {
			Example example = exRead.next();
			example.setWeight(weights[(int) example.getLabel()]);
		}
	}

	/**
	 * Runs the &quot;embedded&quot; learner on the example set and retuns a model.
	 *
	 * @param exampleSet
	 *            an <code>ExampleSet</code> to train a model for
	 * @return a <code>Model</code>
	 */
	private Model trainBaseModel(ExampleSet exampleSet) throws OperatorException {
		Model model = applyInnerLearner(exampleSet);
		createOrReplacePredictedLabelFor(exampleSet);
		return model;
	}

	/**
	 * The preparation part collecting the examples of a batch, computing priors and resetting
	 * weights to 1.
	 *
	 * @param currentBatchNum
	 *            the batch number to be assigned to the examples
	 * @param reader
	 *            the <code>Iterator<Example></code> with the cursor on the current point in the
	 *            stream.
	 * @param batchAttribute
	 *            the attribute to write the batch number to
	 * @return the class priors of the batch
	 */
	private double[] prepareBatch(int currentBatchNum, Iterator<Example> reader, Attribute batchAttribute) {
		int batchCount = 0;
		// Read and classify examples from stream, as long as the buffer (next
		// batch)
		// is not full. Examples are weighted at this point, in order to
		// simulate sampling.
		int[] classCount = new int[2];
		while (batchCount++ < batchSize && reader.hasNext()) {
			Example example = reader.next();
			example.setValue(batchAttribute, currentBatchNum);
			example.setWeight(1);
			classCount[(int) example.getLabel()]++;
		}
		double[] classPriors = new double[2];
		classPriors[0] = (double) classCount[0] / --batchCount;
		classPriors[1] = (double) classCount[1] / batchCount;
		return classPriors;
	}

	/**
	 * Similar to prepareBatch, but for extended batches.
	 *
	 * @param extendedBatch
	 *            containing the extended batch
	 * @return the class priors of the batch
	 */
	private double[] prepareExtendedBatch(ExampleSet extendedBatch) {
		int[] classCount = new int[2];
		Iterator<Example> reader = extendedBatch.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			example.setWeight(1);
			classCount[(int) example.getLabel()]++;
		}
		double[] classPriors = new double[2];
		int sum = classCount[0] + classCount[1];
		classPriors[0] = (double) classCount[0] / sum;
		classPriors[1] = (double) classCount[1] / sum;
		return classPriors;
	}

	/** returns the accuracy of the predictions for the given example set */
	private double evaluatePredictions(ExampleSet exampleSet) {
		Iterator<Example> reader = exampleSet.iterator();
		int count = 0;
		int correct = 0;
		while (reader.hasNext()) {
			count++;
			Example example = reader.next();
			if (example.getLabel() == example.getPredictedLabel()) {
				correct++;
			}
		}
		return (double) correct / count;
	}

	/*
	 * Uses the training set (current batch) to update current model stored in modelInfo.
	 */
	private boolean trainAdditionalModel(ExampleSet trainingSet, Vector<BayBoostBaseModelInfo> modelInfo)
			throws OperatorException {
		Model model = this.trainBaseModel(trainingSet);
		trainingSet = model.apply(trainingSet);

		// get the weighted performance value of the example set with
		// respect to the new model
		WeightedPerformanceMeasures wp = new WeightedPerformanceMeasures(trainingSet);
		// debugMessage(wp);
		if (this.isModelUseful(wp.getContingencyMatrix()) == false) {
			// If the model is not considered to be useful then discard it.
			log("Discard model because of low advantage on training data.");
			return false;
		} else { // Add the new model and its weights to the collection of
			// models:
			modelInfo.add(new BayBoostBaseModelInfo(model, wp.getContingencyMatrix()));
			return true;
		}
	}

	/**
	 * This helper method takes as input the traing set and the set of models trained so far. It
	 * re-estimates the model weights one by one, which means that it changes the contents of the
	 * modelInfo container. Works with crisp base classifiers, only!
	 *
	 * @param exampleSet
	 *            the training set to be used to tune the weights
	 * @param modelInfo
	 *            the <code>Vector</code> of <code>Model</code>s, each with its biasMatrix
	 * @return <code>true</code> iff the <code>ExampleSet</code> contains at least one example that
	 *         is not yet explained deterministically (otherwise: nothing left to learn)
	 */
	private boolean adjustBaseModelWeights(ExampleSet exampleSet, Vector<BayBoostBaseModelInfo> modelInfo)
			throws OperatorException {

		for (int j = 0; j < modelInfo.size(); j++) {
			BayBoostBaseModelInfo consideredModelInfo = modelInfo.get(j);
			Model consideredModel = consideredModelInfo.getModel();
			ContingencyMatrix cm = consideredModelInfo.getContingencyMatrix();
			// double[][] oldBiasMatrix = (double[][]) consideredModelInfo[1];

			BayBoostStream.createOrReplacePredictedLabelFor(exampleSet);
			exampleSet = consideredModel.apply(exampleSet);
			if (!exampleSet.getAttributes().getPredictedLabel().isNominal()) {
				// Only the case of nominal base classifiers is supported!
				throw new UserError(this, 101, "BayBoostStream base learners", exampleSet.getAttributes().getLabel());
			}

			WeightedPerformanceMeasures wp = new WeightedPerformanceMeasures(exampleSet);
			ContingencyMatrix cmNew = wp.getContingencyMatrix();
			// double[][] newBiasMatrix = wp.createLiftRatioMatrix();
			if (isModelUseful(cm) == false) {
				modelInfo.remove(j);
				j--;
				log("Discard base model because of low advantage.");
			} else {
				consideredModelInfo = new BayBoostBaseModelInfo(consideredModel, cmNew);
				modelInfo.set(j, consideredModelInfo);

				boolean stillUncoveredExamples = WeightedPerformanceMeasures.reweightExamples(exampleSet, cmNew, false) > 0;
				if (stillUncoveredExamples == false) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Helper method to decide whether a model improves the training error enough to be considered.
	 *
	 * @param cm
	 *            the contingency matrix
	 * @return <code>true</code> iff the advantage is high enough to consider the model to be useful
	 */
	private boolean isModelUseful(ContingencyMatrix cm) {
		for (int row = 0; row < cm.getNumberOfPredictions(); row++) {
			for (int col = 0; col < cm.getNumberOfClasses(); col++) {
				if (Math.abs(cm.getLift(row, col) - 1) > MIN_ADVANTAGE) {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * private static void debugMessage(WeightedPerformanceMeasures wp) { String message =
	 * Tools.getLineSeparator() + "Model learned - training performance of base learner:" +
	 * Tools.getLineSeparator() + "TPR: " + wp.getProbability(0, 0) + " FPR: " +
	 * wp.getProbability(1, 0) + " | Positively predicted: " + (wp.getProbability(1, 0) +
	 * wp.getProbability(0, 0)) + Tools.getLineSeparator() + "FNR: " + wp.getProbability(0, 1) +
	 * " TNR: " + wp.getProbability(1, 1) + " | Negatively predicted: " + (wp.getProbability(0, 1) +
	 * wp.getProbability(1, 1)) + Tools.getLineSeparator() + "Positively labelled: " +
	 * (wp.getProbability(0, 0) + wp.getProbability(0, 1)) + Tools.getLineSeparator() +
	 * "Negatively labelled: " + (wp.getProbability(1, 0) + wp.getProbability(1, 1));
	 *
	 * LogService.getGlobal().log(message, LogService.STATUS); }
	 */

	/**
	 * Helper method replacing <code>Model.createPredictedLabel(ExampleSet)</code> in order to lower
	 * memory consumption.
	 */
	private static void createOrReplacePredictedLabelFor(ExampleSet exampleSet) {
		Attribute predictedLabel = exampleSet.getAttributes().getPredictedLabel();
		if (predictedLabel != null) { // remove old predicted label
			exampleSet.getAttributes().remove(predictedLabel);
			exampleSet.getExampleTable().removeAttribute(predictedLabel);
		}
	}

	/**
	 * Adds the parameters &quot;rescale label priors&quot; and &quot;weighted batch size&quot;.
	 */
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_RESCALE_LABEL_PRIORS,
				"Specifies whether the proportion of labels should be equal by construction after first iteration .",
				false));
		types.add(new ParameterTypeInt(PARAMETER_BATCH_SIZE,
				"Size of the batches. Minimum number of examples used to train a model.", 1, Integer.MAX_VALUE, 100));
		types.add(new ParameterTypeDouble(PARAMETER_FRACTION_HOLD_OUT_SET,
				"Rel. size of hold out set for ensemble selection. Set to 0 to turn off.", 0, 1.0, 0));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
