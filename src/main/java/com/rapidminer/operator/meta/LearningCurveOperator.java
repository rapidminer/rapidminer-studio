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
package com.rapidminer.operator.meta;

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.RandomGenerator;


/**
 * This operator first divides the input example set into two parts, a training set and a test set
 * according to the parameter &quot;training_ratio&quot;. It then uses iteratively bigger subsets
 * from the fixed training set for learning (the first subprocess) and calculates the corresponding
 * performance values on the fixed test set (with the second subprocess).
 *
 * @author Ingo Mierswa
 */
public class LearningCurveOperator extends OperatorChain {

	private InputPort exampleSetInput = getInputPorts().createPort("exampleSet", ExampleSet.class);
	private OutputPort trainingSource = getSubprocess(0).getInnerSources().createPort("training set");
	private OutputPort testSource = getSubprocess(1).getInnerSources().createPort("test set");
	private PortPairExtender throughExtender = new PortPairExtender("through", getSubprocess(0).getInnerSinks(),
			getSubprocess(1).getInnerSources());
	private InputPort performanceInnerSink = getSubprocess(1).getInnerSinks().createPort("performance",
			PerformanceVector.class);

	/**
	 * The parameter name for &quot;The fraction of examples which shall be maximal used for
	 * training (dynamically growing), the rest is used for testing (fixed)&quot;
	 */
	public static final String PARAMETER_TRAINING_RATIO = "training_ratio";

	/**
	 * The parameter name for &quot;The fraction of examples which would be additionally used in
	 * each step.&quot;
	 */
	public static final String PARAMETER_STEP_FRACTION = "step_fraction";

	/**
	 * The parameter name for &quot;Starts with this fraction of the training data and iteratively
	 * add step_fraction examples from the training data (-1: use step_fraction).&quot;
	 */
	public static final String PARAMETER_START_FRACTION = "start_fraction";

	/**
	 * The parameter name for &quot;Defines the sampling type of the cross validation (linear =
	 * consecutive subsets, shuffled = random subsets, stratified = random subsets with class
	 * distribution kept constant)&quot;
	 */
	public static final String PARAMETER_SAMPLING_TYPE = "sampling_type";

	private double lastFraction = Double.NaN;

	private double lastPerformance = Double.NaN;

	private double lastDeviation = Double.NaN;

	public LearningCurveOperator(OperatorDescription description) {
		super(description, "Training", "Test");
		throughExtender.start();

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, trainingSource, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				metaData.getNumberOfExamples().reduceByUnknownAmount();
				return super.modifyExampleSet(metaData);
			}
		});
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(throughExtender.makePassThroughRule());
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, testSource, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				metaData.getNumberOfExamples().reduceByUnknownAmount();
				return super.modifyExampleSet(metaData);
			}
		});
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(1)));

		addValue(new ValueDouble("fraction", "The used fraction of data.") {

			@Override
			public double getDoubleValue() {
				return lastFraction;
			}
		});
		addValue(new ValueDouble("performance", "The last performance (main criterion).") {

			@Override
			public double getDoubleValue() {
				return lastPerformance;
			}
		});
		addValue(new ValueDouble("deviation", "The variance of the last performance (main criterion).") {

			@Override
			public double getDoubleValue() {
				return lastDeviation;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet originalExampleSet = exampleSetInput.getData(ExampleSet.class);
		double trainingRatio = getParameterAsDouble(PARAMETER_TRAINING_RATIO);
		double stepFraction = getParameterAsDouble(PARAMETER_STEP_FRACTION);
		double startFraction = getParameterAsDouble(PARAMETER_START_FRACTION);
		int samplingType = getParameterAsInt(PARAMETER_SAMPLING_TYPE);
		boolean useLocalRandomSeed = getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED);
		int localRandomSeed = getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED);

		// init Operator progress
		getProgress().setTotal((int) Math.round((1 - startFraction) / stepFraction));

		// disable checkForStop, will be called in #inApplyLoop() anyway
		getProgress().setCheckForStop(false);

		// fix training and test set
		SplittedExampleSet trainTestSplittedExamples = new SplittedExampleSet(originalExampleSet, trainingRatio,
				samplingType, useLocalRandomSeed, localRandomSeed);
		trainTestSplittedExamples.selectSingleSubset(0);
		this.lastFraction = startFraction;
		while (lastFraction <= 1.0d) {
			// learns a model on the growing example set
			trainTestSplittedExamples.selectSingleSubset(0);
			SplittedExampleSet growingTrainingSet = new SplittedExampleSet(trainTestSplittedExamples, lastFraction,
					samplingType, useLocalRandomSeed, localRandomSeed);
			growingTrainingSet.selectSingleSubset(0);
			// IOContainer input = new IOContainer(new IOObject[] { growingTrainingSet });
			trainingSource.deliver(growingTrainingSet);
			getSubprocess(0).execute();
			// input = getOperator(0).apply(input);

			// apply the learned model on the test set
			trainTestSplittedExamples.selectSingleSubset(1);
			testSource.deliver(trainTestSplittedExamples);
			throughExtender.passDataThrough();
			getSubprocess(1).execute();
			// input = input.append(trainTestSplittedExamples);

			PerformanceVector performance = performanceInnerSink.getData(PerformanceVector.class);
			this.lastPerformance = performance.getMainCriterion().getAverage();
			this.lastDeviation = performance.getMainCriterion().getStandardDeviation();
			this.lastFraction += stepFraction;
			inApplyLoop();
			getProgress().step();
		}
		getProgress().complete();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeDouble(PARAMETER_TRAINING_RATIO,
				"The fraction of examples which shall be maximal used for training (dynamically growing), the rest is used for testing (fixed)",
				0.0d, 1.0d, 0.05);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_STEP_FRACTION,
				"The fraction of examples which would be additionally used in each step.", 0.0d, 1.0d, 0.05);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeDouble(PARAMETER_START_FRACTION,
				"Starts with this fraction of the training data and iteratively add step_fraction examples from the training data.",
				0d, 1.0d, 0.05d));
		types.add(new ParameterTypeCategory(PARAMETER_SAMPLING_TYPE,
				"Defines the sampling type of the cross validation (linear = consecutive subsets, shuffled = random subsets, stratified = random subsets with class distribution kept constant)",
				SplittedExampleSet.SAMPLING_NAMES, SplittedExampleSet.STRATIFIED_SAMPLING));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
