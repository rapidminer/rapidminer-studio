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
import java.util.ListIterator;
import java.util.Vector;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.GeneratePredictionModelTransformationRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;


/**
 * Subgroup discovery learner.
 *
 * @author Martin Scholz
 */
public class SDRulesetInduction extends OperatorChain {

	private InputPort exampleSetInput = getInputPorts().createPort("training set", ExampleSet.class);
	private OutputPort trainingInnerSource = getSubprocess(0).getInnerSources().createPort("training set");
	private InputPort modelInnerSink = getSubprocess(0).getInnerSinks().createPort("model", PredictionModel.class);
	private OutputPort modelOutput = getOutputPorts().createPort("model");

	/**
	 * Name of the variable specifying the maximal number of iterations of the learner.
	 */
	public static final String PARAMETER_ITERATIONS = "iterations";

	/** Name of the flag indicating internal bootstrapping. */
	public static final String PARAMETER_RATIO_INTERNAL_BOOTSTRAP = "ratio_internal_bootstrap";

	/**
	 * A parameter whether to discard all rules not lying on the convex hull in ROC space.
	 */
	public static final String PARAMETER_ROC_CONVEX_HULL_FILTER = "ROC_convex_hull_filter";

	/**
	 * Boolean parameter: true for additive reweighting, false for multiplicative.
	 */
	public static final String PARAMETER_ADDITIVE_REWEIGHT = "additive_reweight";

	/**
	 * Boolean parameter to specify whether the label priors should be equally likely after first
	 * iteration.
	 */
	public static final String PARAMETER_GAMMA = "gamma";

	/**
	 * Name of special attribute counting the times an example has been covered by a rule. This
	 * attribute is created for additive reweighting, only.
	 */
	public static final String TIMES_COVERED = "TIMES_COVERED_SPECIAL_ATTRIB";

	/** Discard models with an advantage of less than the specified value. */
	public static final double MIN_ADVANTAGE = 0.001;

	// A performance measure to be visualized. Not yet implemented!
	private double performance = 0;

	// field for visualizing performance
	private int currentIteration;

	/** Constructor. */
	public SDRulesetInduction(OperatorDescription description) {
		super(description, "Training");
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, trainingInnerSource, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				AttributeMetaData weightAttribute = new AttributeMetaData("weight", Ontology.REAL, Attributes.WEIGHT_NAME);
				weightAttribute.setValueSetRelation(SetRelation.UNKNOWN);
				metaData.addAttribute(weightAttribute);

				AttributeMetaData specialAttribute = new AttributeMetaData(TIMES_COVERED, Ontology.REAL, TIMES_COVERED);
				specialAttribute.setValueSetRelation(SetRelation.UNKNOWN);
				metaData.addAttribute(specialAttribute);
				return metaData;
			}
		});
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer()
				.addRule(new GeneratePredictionModelTransformationRule(exampleSetInput, modelOutput, PredictionModel.class));

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

	public static int getPosIndex(Attribute label) {
		return label.getMapping().getPositiveIndex();
	}

	/**
	 * Creates a weight attribute if not yet done and fills it with an initial value so that
	 * positive and negative examples are equally probable.
	 *
	 * @param exampleSet
	 *            the example set to be prepared
	 */
	private double[] prepareWeights(ExampleSet exampleSet) {
		Attribute weightAttr = com.rapidminer.example.Tools.createWeightAttribute(exampleSet);
		Attribute timesCoveredAttrib = null;
		boolean additive = this.getParameterAsBoolean(PARAMETER_ADDITIVE_REWEIGHT);
		if (additive && (timesCoveredAttrib = exampleSet.getAttributes().get(TIMES_COVERED)) == null) {
			timesCoveredAttrib = com.rapidminer.example.Tools.createSpecialAttribute(exampleSet, TIMES_COVERED,
					Ontology.INTEGER);
			exampleSet.getExampleTable().addAttribute(timesCoveredAttrib);
		}

		Iterator<Example> exRead = exampleSet.iterator();

		int numPos = 0;
		final int positiveClass = getPosIndex(exampleSet.getAttributes().getLabel());
		final int negativeClass = 1 - positiveClass;
		while (exRead.hasNext()) {
			if (exRead.next().getLabel() == positiveClass) {
				numPos++;
			}
		}
		final double[] classPriors = new double[2];
		classPriors[positiveClass] = (double) numPos / exampleSet.size();
		classPriors[negativeClass] = 1.0d - classPriors[positiveClass];
		final double posWeight = 0.5 / classPriors[positiveClass];
		final double negWeight = 0.5 / classPriors[negativeClass];

		exRead = exampleSet.iterator();
		while (exRead.hasNext()) {
			Example example = exRead.next();
			double w = example.getLabel() == positiveClass ? posWeight : negWeight;
			example.setValue(weightAttr, w);
			if (additive) {
				example.setValue(timesCoveredAttrib, 0);
			}
		}

		return classPriors;
	}

	/**
	 * Runs the &quot;embedded&quot; learner on the example set and retuns a model.
	 *
	 * @param exampleSet
	 *            an <code>ExampleSet</code> to train a model for
	 * @return a <code>Model</code>
	 */
	private Model trainModel(ExampleSet exampleSet) throws OperatorException {
		trainingInnerSource.deliver(exampleSet);
		getSubprocess(0).execute();
		return modelInnerSink.getData(Model.class);
	}

	/**
	 * Constructs a <code>Model</code> repeatedly running a weak learner, reweighting the training
	 * example set accordingly, and combining the hypothesis using the available weighted
	 * performance values. If the input contains a model, then this model is used as a starting
	 * point for weighting the examples.
	 */
	@Override
	public void doWork() throws OperatorException {
		// Reads the input example set and initializes its weights.
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		// Check if label is present and fits the learning task
		com.rapidminer.example.Tools.isLabelled(exampleSet);

		Model model = this.trainRuleset(exampleSet, this.prepareWeights(exampleSet));

		modelOutput.deliver(model);
	}

	/** Main method for training the ensemble classifier */
	private SDEnsemble trainRuleset(ExampleSet trainingSet, final double[] classPriors) throws OperatorException {
		// for models and their probability estimates
		Vector<Pair<Model, double[][]>> modelInfo = new Vector<Pair<Model, double[][]>>();

		// check whether to use the complete training set for training
		final double splitRatio = this.getParameterAsDouble(PARAMETER_RATIO_INTERNAL_BOOTSTRAP);
		final boolean bootstrap = splitRatio > 0 && splitRatio < 1.0;
		log(bootstrap ? "Bootstrapping enabled." : "Bootstrapping disabled.");

		// maximum number of iterations
		final int iterations = this.getParameterAsInt(PARAMETER_ITERATIONS);

		final boolean roc_filter = this.getParameterAsBoolean(PARAMETER_ROC_CONVEX_HULL_FILTER);
		List<double[]> rocCurve = null;
		if (roc_filter) {
			rocCurve = new LinkedList<double[]>();
			rocCurve.add(new double[] { 0, 0 });
			rocCurve.add(new double[] { 1, 1 });
		}

		boolean useLocalRandomSeed = getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED);
		int localRandomSeed = getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED);

		for (int i = 0; i < iterations; i++) {
			this.currentIteration = i;
			// int size = trainingSet.getSize();
			ExampleSet splittedSet = trainingSet;

			if (bootstrap == true) {
				splittedSet = new SplittedExampleSet(trainingSet, splitRatio, SplittedExampleSet.SHUFFLED_SAMPLING,
						useLocalRandomSeed, localRandomSeed);
				((SplittedExampleSet) splittedSet).selectSingleSubset(0); // switch to training set
			}

			// train one model per iteration
			Model model = this.trainModel(splittedSet);
			ExampleSet resultSet = null;
			if (bootstrap == true) {
				((SplittedExampleSet) splittedSet).selectSingleSubset(1); // switch to out-of-bag
																			 // set
				resultSet = model.apply(splittedSet); // apply model to all examples
			} else {
				resultSet = model.apply(trainingSet); // apply model to all examples
			}

			// get the weighted performance value of the example set with
			// respect to the model
			SDReweightMeasures wp = new SDReweightMeasures(resultSet);
			final boolean additive = this.getParameterAsBoolean(PARAMETER_ADDITIVE_REWEIGHT);

			wp.setAdditive(additive);
			if (!additive) {
				wp.setGamma(this.getParameterAsDouble(PARAMETER_GAMMA));
			}

			// Calculate the unweighted distributions and the true/false
			// positive rate:
			double[][] modelWeightMatrix = new double[2][2];
			double tpr = 0;
			double fpr = 0;
			boolean defaultRule = false;
			{
				// assuming indexes "0" and "1" for predictions:
				int[][] predClasses = new int[2][];
				predClasses[0] = wp.getCoveredExamplesNumForPred(0);
				predClasses[1] = wp.getCoveredExamplesNumForPred(1);
				int[] rowTotals = new int[2];
				rowTotals[0] = predClasses[0][0] + predClasses[0][1];
				rowTotals[1] = predClasses[1][0] + predClasses[1][1];
				int total = rowTotals[0] + rowTotals[1];

				// Just the distribution for the covered subset is stored.
				// It is not visible which label is explicitly predicted
				// (syntactically)
				// in the rule, so we assume the label that results in higher
				// WRAcc.
				double cov0 = (double) rowTotals[0] / total;
				double cov1 = (double) rowTotals[1] / total;
				double prior0 = ((double) predClasses[0][0] + predClasses[1][0]) / total;
				double prior1 = ((double) predClasses[0][1] + predClasses[1][1]) / total; // used
				// later
				double bias0 = Math.abs((double) predClasses[0][0] / rowTotals[0] - prior0);
				double bias1 = Math.abs((double) predClasses[1][0] / rowTotals[1] - prior0);
				int subset = Double.isNaN(bias1) || cov0 * bias0 >= cov1 * bias1 ? 0 : 1; // WRAcc
				// is
				// coverage
				// *
				// bias

				// The subset not covered by the rule is marked with zero
				// estimates.
				modelWeightMatrix[subset][0] = (double) predClasses[subset][0] / rowTotals[subset];
				modelWeightMatrix[subset][1] = (double) predClasses[subset][1] / rowTotals[subset];

				double ratio0 = (double) predClasses[subset][0] / total / prior0;
				double ratio1 = (double) predClasses[subset][1] / total / prior1;

				// Reweight the example set with respect to the weighted
				// performance values.
				// The last parameter is the positive class. It is selected so
				// that TPr is higher.
				wp.reweightExamples(trainingSet, ratio0 > ratio1 ? 0 : 1, subset);

				// As "positive" and "negative" depend on the explicitly
				// predicted class
				// (which is not visible) we sometimes need to translate tnr
				// into tpr.
				if (roc_filter) {
					tpr = Math.max(ratio0, ratio1);
					fpr = Math.min(ratio0, ratio1);
				}

				defaultRule = cov0 == 0 || cov1 == 0;

			}

			// If activated just keep rules lying on the convex hull in ROC
			// space:
			if (defaultRule == false && (roc_filter == false || this.isOnConvexHull(rocCurve, tpr, fpr))) {
				// Add the new model and its weights to the collection of
				// models:
				modelInfo.add(new Pair<Model, double[][]>(model, modelWeightMatrix));
			}

			inApplyLoop();
		}

		if (roc_filter) {
			StringBuffer message = new StringBuffer(
					"The convex hull in ROC space contains the following points (TPr/FPr):" + Tools.getLineSeparator());
			Iterator<double[]> it = rocCurve.iterator();
			while (it.hasNext()) {
				double[] tpfp = it.next();
				message.append("(" + tpfp[0] + ", " + tpfp[1] + ") ");
			}
			log(message.toString());
		}

		// Build a Model object.
		short combinationMethod = this.getParameterAsBoolean(PARAMETER_ADDITIVE_REWEIGHT) ? SDEnsemble.RULE_COMBINE_ADDITIVE
				: SDEnsemble.RULE_COMBINE_MULTIPLY;
		return new SDEnsemble(trainingSet, modelInfo, classPriors, combinationMethod);
	}

	/*
	 * private void debugMessage(SDReweightMeasures wp) { String message = Tools.getLineSeparator()
	 * + "Model learned - training performance of rule:" + Tools.getLineSeparator() + "TPR: " +
	 * wp.getProbability(0, 0) + " FPR: " + wp.getProbability(1, 0) + " | Positively predicted: " +
	 * (wp.getProbability(1, 0) + wp.getProbability(0, 0)) + Tools.getLineSeparator() + "FNR: " +
	 * wp.getProbability(0, 1) + " TNR: " + wp.getProbability(1, 1) + " | Negatively predicted: " +
	 * (wp.getProbability(0, 1) + wp.getProbability(1, 1)) + Tools.getLineSeparator() +
	 * "Positively labeled: " + (wp.getProbability(0, 0) + wp.getProbability(0, 1)) +
	 * Tools.getLineSeparator() + "Negatively labeled: " + (wp.getProbability(1, 0) +
	 * wp.getProbability(1, 1));
	 *
	 * LogService.getGlobal().log(message, LogService.STATUS); }
	 */

	private boolean isOnConvexHull(List<double[]> rocCurve, double tpr, double fpr) {
		if (tpr <= 0 || tpr > 1 || fpr < 0 || fpr >= 1) {
			return false;
		}

		ListIterator<double[]> iter = rocCurve.listIterator();
		double slope = Double.POSITIVE_INFINITY;
		boolean fprGreater = true;
		while (fprGreater) {
			double[] current = iter.next();
			fprGreater = fpr > current[1];

			if (fprGreater) {
				double newSlope = (tpr - current[0]) / (fpr - current[1]);
				if (newSlope >= slope) {
					iter.remove();
				} else {
					slope = newSlope; // slope connecting the new point to the
					// candidate
					double finalSlope = (1 - current[0]) / (1 - current[1]); // connection
					// new
					// point
					// to
					// (1,1)
					if (slope <= finalSlope) { // slope needs to be greater
						// than connection to (1,1)
						return false; // candidate lies below
					}
				}
			} else if (fpr == current[1]) { // no slope defined
				if (tpr > current[0]) {
					rocCurve.set(iter.previousIndex(), new double[] { tpr, fpr });
				} else {
					return false;
				}
			} else { // The last slope is still available. It must be higher
				// than the next one!
				double nextSlope = (current[0] - tpr) / (current[1] - fpr);
				if (slope > nextSlope) {
					rocCurve.add(iter.previousIndex(), new double[] { tpr, fpr });
				} else {
					return false;
				}
			}
		}

		slope = (1 - tpr) / (1 - fpr); // slope of connecting line between
		// candidate and (1,1)
		iter = rocCurve.listIterator(rocCurve.size());
		while (iter.hasPrevious()) {
			double[] current = iter.previous();
			if (current[1] <= fpr) {
				return true; // done.
			}
			double newSlope = (current[0] - tpr) / (current[1] - fpr); // slope
			// new
			// point
			// to
			// candidate
			if (current[1] < 1 && newSlope <= slope) { // needs to be
				// greater than last
				// slope
				iter.remove();
			} else {
				slope = newSlope;
			}
		}
		return true;
	}

	/**
	 * Adds the parameters &quot;number of iterations&quot; and &quot;model file&quot;.
	 */
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_ITERATIONS, "The maximum number of iterations.", 1,
				Integer.MAX_VALUE, 10);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeDouble(PARAMETER_RATIO_INTERNAL_BOOTSTRAP,
				"Fraction of examples used for training (internal bootstrapping). If activated (value < 1) only the rest is used to estimate the biases.",
				0, 1, 0.7));
		types.add(new ParameterTypeBoolean(PARAMETER_ROC_CONVEX_HULL_FILTER,
				"A parameter whether to discard all rules not lying on the convex hull in ROC space.", true));
		types.add(new ParameterTypeBoolean(PARAMETER_ADDITIVE_REWEIGHT,
				"If enabled then resampling is done by additive reweighting, otherwise by multiplicative reweighting.",
				true));
		types.add(new ParameterTypeDouble(PARAMETER_GAMMA,
				"Factor used for multiplicative reweighting. Has no effect in case of additive reweighting.", 0, 1, 0.9));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
