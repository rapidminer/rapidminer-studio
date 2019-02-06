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

import java.util.Collection;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.learner.CapabilityCheck;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.RandomGenerator;


/**
 * Operator chain that splits an {@link ExampleSet} into a training and test sets similar to
 * XValidation, but returns the test set predictions instead of a performance vector. The inner two
 * operators must be a learner returning a {@link Model} and an operator or operator chain that can
 * apply this model (usually a model applier)
 *
 * @author Stefan Rueping, Ingo Mierswa, Sebastian Land
 */
public class XVPrediction extends OperatorChain implements CapabilityProvider {

	/** The parameter name for &quot;Number of subsets for the crossvalidation.&quot; */
	public static final String PARAMETER_NUMBER_OF_VALIDATIONS = "number_of_validations";

	/**
	 * The parameter name for &quot;Set the number of validations to the number of examples. If set
	 * to true, number_of_validations is ignored.&quot;
	 */
	public static final String PARAMETER_LEAVE_ONE_OUT = "leave_one_out";

	/** The parameter name for &quot;Defines the sampling type of the cross validation.&quot; */
	public static final String PARAMETER_SAMPLING_TYPE = "sampling_type";

	private int number;

	private int iteration;

	private final InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);

	private final OutputPort trainingProcessExampleSource = getSubprocess(0).getInnerSources().createPort("training");
	private final InputPort trainingProcessModelSink = getSubprocess(0).getInnerSinks().createPort("model");

	// training -> testing
	private final PortPairExtender throughExtender = new PortPairExtender("through", getSubprocess(0).getInnerSinks(),
			getSubprocess(1).getInnerSources());

	// testing
	private final OutputPort applyProcessModelSource = getSubprocess(1).getInnerSources().createPort("model");
	private final OutputPort applyProcessExampleSource = getSubprocess(1).getInnerSources().createPort("unlabelled data");
	private final InputPort applyProcessExampleInnerSink = getSubprocess(1).getInnerSinks().createPort("labelled data");

	// output
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("labelled data");

	public XVPrediction(OperatorDescription description) {
		super(description, "Training", "Model Application");

		exampleSetInput.addPrecondition(new CapabilityPrecondition(this, exampleSetInput));

		throughExtender.start();

		getTransformer().addRule(
				new ExampleSetPassThroughRule(exampleSetInput, trainingProcessExampleSource, SetRelation.EQUAL) {

					@Override
					public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
						try {
							metaData.setNumberOfExamples(getTrainingSetSize(metaData.getNumberOfExamples()));
						} catch (UndefinedParameterError e) {
						}
						return super.modifyExampleSet(metaData);
					}
				});
		getTransformer().addRule(
				new ExampleSetPassThroughRule(exampleSetInput, applyProcessExampleSource, SetRelation.EQUAL) {

					@Override
					public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
						try {
							metaData.setNumberOfExamples(getTestSetSize(metaData.getNumberOfExamples()));
						} catch (UndefinedParameterError e) {
						}
						return super.modifyExampleSet(metaData);
					}
				});
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(new PassThroughRule(trainingProcessModelSink, applyProcessModelSource, false));
		getTransformer().addRule(throughExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(1)));
		getTransformer().addPassThroughRule(applyProcessExampleInnerSink, exampleSetOutput);

		addValue(new ValueDouble("iteration", "The number of the current iteration.") {

			@Override
			public double getDoubleValue() {
				return iteration;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet inputSet = exampleSetInput.getData(ExampleSet.class);

		// check capabilities and produce errors if they are not fulfilled
		CapabilityCheck check = new CapabilityCheck(this, false);
		check.checkLearnerCapabilities(this, inputSet);

		if (getParameterAsBoolean(PARAMETER_LEAVE_ONE_OUT)) {
			number = inputSet.size();
		} else {
			number = getParameterAsInt(PARAMETER_NUMBER_OF_VALIDATIONS);
		}
		log("Starting " + number + "-fold cross validation prediction");

		// init Operator progress
		getProgress().setTotal(number + 1);

		// disable checkForStop, will be called in #inApplyLoop() anyway
		getProgress().setCheckForStop(false);

		// creating predicted label
		ExampleSet resultSet = (ExampleSet) inputSet.clone();
		Attribute predictedLabel = PredictionModel.createPredictedLabel(resultSet, inputSet.getAttributes().getLabel());
		Collection<String> predictedLabelValues = null;
		if (predictedLabel.isNominal()) {
			predictedLabelValues = predictedLabel.getMapping().getValues();
		}

		// Split training / test set
		int samplingType = getParameterAsInt(PARAMETER_SAMPLING_TYPE);
		SplittedExampleSet splittedSet = new SplittedExampleSet(inputSet, number, samplingType,
				getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
				getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED), getCompatibilityLevel().isAtMost(
						SplittedExampleSet.VERSION_SAMPLING_CHANGED));

		getProgress().setCompleted(1);

		for (iteration = 0; iteration < number; iteration++) {
			splittedSet.selectAllSubsetsBut(iteration);
			trainingProcessExampleSource.deliver(splittedSet);
			getSubprocess(0).execute();
			// IOContainer learnResult = getLearner().apply(new IOContainer(new IOObject[] {
			// splittedSet }));

			splittedSet.selectSingleSubset(iteration);
			applyProcessExampleSource.deliver(splittedSet);
			throughExtender.passDataThrough();
			applyProcessModelSource.deliver(trainingProcessModelSink.getData(IOObject.class));
			getSubprocess(1).execute();

			ExampleSet predictedSet = applyProcessExampleInnerSink.getData(ExampleSet.class);
			for (int i = 0; i < splittedSet.size(); i++) {
				Example predictedExample = predictedSet.getExample(i);
				// setting label in inputSet
				Example resultExample = resultSet.getExample(splittedSet.getActualParentIndex(i));
				resultExample.setValue(predictedLabel, predictedExample.getPredictedLabel());
				if (predictedLabel.isNominal()) {
					for (String s : predictedLabelValues) {
						resultExample.setConfidence(s, predictedExample.getConfidence(s));
					}
				}
			}
			// PredictionModel.removePredictedLabel(predictedSet);
			inApplyLoop();
			getProgress().step();
		}

		exampleSetOutput.deliver(resultSet);
		getProgress().complete();
	}

	protected MDInteger getTestSetSize(MDInteger originalSize) throws UndefinedParameterError {
		if (getParameterAsBoolean(PARAMETER_LEAVE_ONE_OUT)) {
			return new MDInteger(1);
		} else {
			return originalSize.multiply(1d / getParameterAsDouble(PARAMETER_NUMBER_OF_VALIDATIONS));
		}
	}

	protected MDInteger getTrainingSetSize(MDInteger originalSize) throws UndefinedParameterError {
		if (getParameterAsBoolean(PARAMETER_LEAVE_ONE_OUT)) {
			return originalSize.add(-1);
		} else {
			return originalSize.multiply(1d - 1d / getParameterAsDouble(PARAMETER_NUMBER_OF_VALIDATIONS));
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeBoolean(
				PARAMETER_LEAVE_ONE_OUT,
				"Set the number of validations to the number of examples. If set to true, number_of_validations is ignored.",
				false, false));

		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_VALIDATIONS,
				"Number of subsets for the crossvalidation.", 2, Integer.MAX_VALUE, 10, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LEAVE_ONE_OUT, false, false));
		types.add(type);

		types.add(new ParameterTypeCategory(PARAMETER_SAMPLING_TYPE, "Defines the sampling type of the cross validation.",
				SplittedExampleSet.SAMPLING_NAMES, SplittedExampleSet.STRATIFIED_SAMPLING, false));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NO_LABEL:
				return false;
			case NUMERICAL_LABEL:
				try {
					return getParameterAsInt(PARAMETER_SAMPLING_TYPE) != SplittedExampleSet.STRATIFIED_SAMPLING;
				} catch (UndefinedParameterError e) {
					return false;
				}
			default:
				return true;
		}
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return new OperatorVersion[] { SplittedExampleSet.VERSION_SAMPLING_CHANGED };
	}
}
