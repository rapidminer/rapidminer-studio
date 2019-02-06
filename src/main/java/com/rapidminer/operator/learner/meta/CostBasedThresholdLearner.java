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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.optimization.ec.es.ESOptimization;
import com.rapidminer.tools.math.optimization.ec.es.Individual;


/**
 * <p>
 * This operator uses a set of class weights and also allows a weight for the fact that an example
 * is not classified at all (marked as unknown). Based on the predictions of the model of the inner
 * learner this operator optimized a set of thresholds regarding the defined weights.
 * </p>
 *
 * <p>
 * This operator might be very useful in cases where it is better to not classify an example then to
 * classify it in a wrong way. This way, it is often possible to get very high accuracies for the
 * remaining examples (which are actually classified) for the cost of having some examples which
 * must still be manually classified.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class CostBasedThresholdLearner extends AbstractMetaLearner {

	/**
	 * The parameter name for &quot;The weights for all classes (first column: class names, second
	 * column: weight), empty: using 1 for all classes. The costs for not classifying at all are
	 * defined with class name '?'.&quot;
	 */
	public static final String PARAMETER_CLASS_WEIGHTS = "class_weights";

	public static final String PARAMETER_ALLOW_UNKOWN_PREDICTIONS = "allow_unkown_predictions";

	/**
	 * The parameter name for &quot;Use this cost value for predicting an example as unknown (-1:
	 * use same costs as for correct class).&quot;
	 */
	public static final String PARAMETER_PREDICT_UNKNOWN_COSTS = "predict_unknown_costs";

	/**
	 * The parameter name for &quot;Use this amount of input data for model learning and the rest
	 * for threshold optimization.&quot;
	 */
	public static final String PARAMETER_TRAINING_RATIO = "training_ratio";

	/** The parameter name for &quot;Defines the number of optimization iterations.&quot; */
	public static final String PARAMETER_NUMBER_OF_ITERATIONS = "number_of_iterations";

	public CostBasedThresholdLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		Attribute label = exampleSet.getAttributes().getLabel();
		List<String[]> classWeights = getParameterList(PARAMETER_CLASS_WEIGHTS);

		// some checks
		com.rapidminer.example.Tools.hasNominalLabels(exampleSet, getOperatorClassName());

		if (classWeights.size() == 0) {
			throw new UndefinedParameterError(PARAMETER_CLASS_WEIGHTS, this);
		}

		// derive possible class weights
		double unknownWeight = getParameterAsBoolean(PARAMETER_ALLOW_UNKOWN_PREDICTIONS) ? getParameterAsDouble(PARAMETER_PREDICT_UNKNOWN_COSTS)
				: -1d;
		double[] weights = new double[label.getMapping().size()];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = 1.0d;
		}

		Iterator<String[]> i = classWeights.iterator();
		while (i.hasNext()) {
			String[] classWeightArray = i.next();
			String className = classWeightArray[0];
			double classWeight = Double.valueOf(classWeightArray[1]);
			int index = label.getMapping().getIndex(className);
			if (index == -1) {
				throw new UserError(this, 955, className);
			} else {
				weights[index] = classWeight;
			}
		}

		// logging
		List<String> weightList = new LinkedList<>();
		for (double d : weights) {
			weightList.add(Tools.formatIntegerIfPossible(d));
		}
		log("Used class weights --> " + weightList + ", unknown weight: " + Tools.formatIntegerIfPossible(unknownWeight));

		return calculateThresholdModel(exampleSet, weights, unknownWeight);
	}

	private Model calculateThresholdModel(ExampleSet exampleSet, final double[] classWeights, final double unknownWeight)
			throws OperatorException {
		SplittedExampleSet trainingSet = new SplittedExampleSet(exampleSet, getParameterAsDouble(PARAMETER_TRAINING_RATIO),
				SplittedExampleSet.STRATIFIED_SAMPLING,
				getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
				getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED));
		trainingSet.selectSingleSubset(0);
		Model innerModel = applyInnerLearner(trainingSet);
		trainingSet.selectSingleSubset(1);
		final ExampleSet appliedTrainingSet = innerModel.apply(trainingSet);
		final Attribute label = appliedTrainingSet.getAttributes().getLabel();

		int numberOfGenerations = getParameterAsInt(PARAMETER_NUMBER_OF_ITERATIONS);
		ESOptimization optimization = new ESOptimization(0.0d, 1.0d, 5, classWeights.length,
				ESOptimization.INIT_TYPE_RANDOM, numberOfGenerations, Math.max(1, numberOfGenerations / 10),
				ESOptimization.TOURNAMENT_SELECTION, 0.4, true, ESOptimization.GAUSSIAN_MUTATION, 0.9, false, false,
				RandomGenerator.getRandomGenerator(this), this, this) {

			@Override
			public PerformanceVector evaluateIndividual(Individual individual) throws OperatorException {
				double costs = 0.0d;
				double[] thresholds = individual.getValues();
				for (Example example : appliedTrainingSet) {
					int predictionIndex = (int) example.getPredictedLabel();
					String className = label.getMapping().mapIndex(predictionIndex);
					double confidence = example.getConfidence(className);
					// confident...
					if (confidence > thresholds[predictionIndex]) {
						// wrong -> malus
						if (example.getLabel() != example.getPredictedLabel()) {
							costs += classWeights[(int) example.getLabel()];
						} else {
							// correct -> bonus
							// costs -= classWeights[(int)example.getLabel()];
						}
						// not so confident...
					} else {
						double usedWeight = unknownWeight;
						if (unknownWeight < 0.0d) {
							usedWeight = classWeights[(int) example.getLabel()];
						}
						// correct -> malus
						if (example.getLabel() == example.getPredictedLabel()) {
							costs += usedWeight;
						} else {
							// wrong -> bonus
							// costs -= usedWeight;
						}
					}
				}
				PerformanceVector performanceVector = new PerformanceVector();
				performanceVector.addCriterion(new EstimatedPerformance("Costs", costs, 1, true));
				return performanceVector;
			}
		};

		optimization.optimize();
		PredictionModel.removePredictedLabel(appliedTrainingSet);

		double[] bestValues = optimization.getBestValuesEver();

		return new ThresholdModel(appliedTrainingSet, innerModel, bestValues);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeList(
				PARAMETER_CLASS_WEIGHTS,
				"The weights for all classes, empty: using 1 for all classes. The costs for not classifying at all are defined with class name '?'.",
				new ParameterTypeString("class_name", "The name of the class."), new ParameterTypeDouble("weight",
						"The weight for this class.", 0.0d, Double.POSITIVE_INFINITY, 1.0d));
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);

		types.add(new ParameterTypeBoolean(
				PARAMETER_ALLOW_UNKOWN_PREDICTIONS,
				"This indicates if unkown predictions are allowed. If checked, the costs for unkown predictions must be specified.",
				false));
		type = new ParameterTypeDouble(PARAMETER_PREDICT_UNKNOWN_COSTS,
				"Use this cost value for predicting an example as unknown.", 0d, Double.POSITIVE_INFINITY, 1d);
		type.setExpert(false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_ALLOW_UNKOWN_PREDICTIONS, true, true));
		types.add(type);

		types.add(new ParameterTypeDouble(PARAMETER_TRAINING_RATIO,
				"Use this amount of input data for model learning and the rest for threshold optimization.", 0.0d, 1.0d,
				0.7d));
		types.add(new ParameterTypeInt(PARAMETER_NUMBER_OF_ITERATIONS, "Defines the number of optimization iterations.", 1,
				Integer.MAX_VALUE, 200));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NUMERICAL_LABEL:
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
				return false;
			default:
				return true;
		}
	}
}
