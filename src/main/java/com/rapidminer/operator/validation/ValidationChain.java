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
package com.rapidminer.operator.validation;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.math.AverageVector;


/**
 * Abstract superclass of operator chains that split an {@link ExampleSet} into a training and test
 * set and return a performance vector. The two inner operators must be a learner returning a
 * {@link Model} and an operator or operator chain that can apply this model and returns a
 * {@link PerformanceVector}. Hence the second inner operator usually is an operator chain
 * containing a model applier and a performance evaluator.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public abstract class ValidationChain extends OperatorChain implements CapabilityProvider {

	/**
	 * The parameter name for &quot;Indicates if a model of the complete data set should be
	 * additionally build after estimation.&quot;
	 */
	public static final String PARAMETER_CREATE_COMPLETE_MODEL = "create_complete_model";

	// input
	protected final InputPort trainingSetInput = getInputPorts().createPort("training", ExampleSet.class);

	// training
	protected final OutputPort trainingProcessExampleSetOutput = getSubprocess(0).getInnerSources().createPort("training");
	private final InputPort trainingProcessModelInput = getSubprocess(0).getInnerSinks().createPort("model", Model.class);

	// training -> testing
	private final PortPairExtender throughExtender = new PortPairExtender("through", getSubprocess(0).getInnerSinks(),
			getSubprocess(1).getInnerSources());

	// testing
	private final OutputPort applyProcessModelOutput = getSubprocess(1).getInnerSources().createPort("model");
	private final OutputPort applyProcessExampleSetOutput = getSubprocess(1).getInnerSources().createPort("test set");
	private final PortPairExtender applyProcessPerformancePortExtender = new PortPairExtender("averagable",
			getSubprocess(1).getInnerSinks(), getOutputPorts(), new MetaData(AverageVector.class));

	// output
	protected final OutputPort modelOutput = getOutputPorts().createPort("model");
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("training");

	private double lastMainPerformance = Double.NaN;
	private double lastMainVariance = Double.NaN;
	private double lastMainDeviation = Double.NaN;

	private double lastFirstPerformance = Double.NaN;
	private double lastSecondPerformance = Double.NaN;
	private double lastThirdPerformance = Double.NaN;

	public ValidationChain(OperatorDescription description) {
		super(description, "Training", "Testing");
		throughExtender.start();

		trainingSetInput.addPrecondition(getCapabilityPrecondition());

		applyProcessPerformancePortExtender.ensureMinimumNumberOfPorts(1);

		InputPort inputPort = applyProcessPerformancePortExtender.getManagedPairs().iterator().next().getInputPort();
		inputPort.addPrecondition(new SimplePrecondition(inputPort, new MetaData(PerformanceVector.class)));
		applyProcessPerformancePortExtender.start();

		getTransformer().addRule(
				new ExampleSetPassThroughRule(trainingSetInput, trainingProcessExampleSetOutput, SetRelation.EQUAL) {

					@Override
					public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
						try {
							metaData.setNumberOfExamples(getTrainingSetSize(metaData.getNumberOfExamples()));
						} catch (UndefinedParameterError e) {
						}
						return super.modifyExampleSet(metaData);
					}
				});
		getTransformer()
				.addRule(new ExampleSetPassThroughRule(trainingSetInput, applyProcessExampleSetOutput, SetRelation.EQUAL) {

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
		getTransformer().addRule(new PassThroughRule(trainingProcessModelInput, applyProcessModelOutput, false));
		getTransformer().addRule(new PassThroughRule(trainingProcessModelInput, modelOutput, false));
		getTransformer().addRule(throughExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(1)));
		getTransformer().addRule(applyProcessPerformancePortExtender.makePassThroughRule());
		getTransformer().addPassThroughRule(trainingSetInput, exampleSetOutput);

		addValue(new ValueDouble("performance", "The last performance average (main criterion).") {

			@Override
			public double getDoubleValue() {
				return lastMainPerformance;
			}
		});
		addValue(new ValueDouble("variance", "The variance of the last performance (main criterion).") {

			@Override
			public double getDoubleValue() {
				return lastMainVariance;
			}
		});
		addValue(new ValueDouble("deviation", "The standard deviation of the last performance (main criterion).") {

			@Override
			public double getDoubleValue() {
				return lastMainDeviation;
			}
		});

		addValue(new ValueDouble("performance1", "The last performance average (first criterion).") {

			@Override
			public double getDoubleValue() {
				return ValidationChain.this.lastFirstPerformance;
			}
		});
		addValue(new ValueDouble("performance2", "The last performance average (second criterion).") {

			@Override
			public double getDoubleValue() {
				return ValidationChain.this.lastSecondPerformance;
			}
		});
		addValue(new ValueDouble("performance3", "The last performance average (third criterion).") {

			@Override
			public double getDoubleValue() {
				return ValidationChain.this.lastThirdPerformance;
			}
		});
	}

	/**
	 * This method can be overwritten in order to give a more senseful quickfix.
	 */
	protected Precondition getCapabilityPrecondition() {
		return new CapabilityPrecondition(this, trainingSetInput);
	}

	protected abstract MDInteger getTrainingSetSize(MDInteger originalSize) throws UndefinedParameterError;

	protected abstract MDInteger getTestSetSize(MDInteger originalSize) throws UndefinedParameterError;

	@Override
	public boolean shouldAutoConnect(OutputPort outputPort) {
		if (outputPort == modelOutput) {
			return getParameterAsBoolean(PARAMETER_CREATE_COMPLETE_MODEL);
		} else if (outputPort == exampleSetOutput) {
			return getParameterAsBoolean("keep_example_set");
		} else {
			return super.shouldAutoConnect(outputPort);
		}
	}

	/**
	 * This is the main method of the validation chain and must be implemented to estimate a
	 * performance of inner operators on the given example set. The implementation can make use of
	 * the provided helper methods in this class.
	 */
	public abstract void estimatePerformance(ExampleSet inputSet) throws OperatorException;

	/**
	 * Returns the first subprocess (or operator chain), i.e. the learning operator (chain).
	 *
	 * @throws OperatorException
	 */
	protected void executeLearner() throws OperatorException {
		getSubprocess(0).execute();
	}

	/**
	 * Returns the second encapsulated inner operator (or operator chain), i.e. the application and
	 * evaluation operator (chain)
	 *
	 * @throws OperatorException
	 */
	protected void executeEvaluator() throws OperatorException {
		getSubprocess(1).execute();
	}

	/** Can be used by subclasses to set the performance of the example set. */
	private final void setResult(PerformanceVector pv) {
		this.lastMainPerformance = Double.NaN;
		this.lastMainVariance = Double.NaN;
		this.lastMainDeviation = Double.NaN;
		this.lastFirstPerformance = Double.NaN;
		this.lastSecondPerformance = Double.NaN;
		this.lastThirdPerformance = Double.NaN;

		if (pv != null) {
			// main result
			PerformanceCriterion mainCriterion = pv.getMainCriterion();
			if (mainCriterion == null && pv.size() > 0) { // use first if no main criterion was
				// defined
				mainCriterion = pv.getCriterion(0);
			}
			if (mainCriterion != null) {
				this.lastMainPerformance = mainCriterion.getAverage();
				this.lastMainVariance = mainCriterion.getVariance();
				this.lastMainDeviation = mainCriterion.getStandardDeviation();
			}

			if (pv.size() >= 1) {
				PerformanceCriterion criterion = pv.getCriterion(0);
				if (criterion != null) {
					this.lastFirstPerformance = criterion.getAverage();
				}
			}

			if (pv.size() >= 2) {
				PerformanceCriterion criterion = pv.getCriterion(1);
				if (criterion != null) {
					this.lastSecondPerformance = criterion.getAverage();
				}
			}

			if (pv.size() >= 3) {
				PerformanceCriterion criterion = pv.getCriterion(2);
				if (criterion != null) {
					this.lastThirdPerformance = criterion.getAverage();
				}
			}
		}
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet eSet = trainingSetInput.getData(ExampleSet.class);
		estimatePerformance(eSet);

		// Generate complete model, if desired
		if (modelOutput.isConnected()) {
			learnFinalModel(eSet);
			getProgress().complete();
			modelOutput.deliver(trainingProcessModelInput.getData(IOObject.class));
		}
		exampleSetOutput.deliver(eSet);

		// set last result for plotting purposes. This is an average value and
		// actually not the last performance value!
		boolean success = false;
		for (IOObject result : applyProcessPerformancePortExtender.getOutputData(IOObject.class)) {
			if (result instanceof PerformanceVector) {
				setResult((PerformanceVector) result);
				success = true;
				break;
			}
		}
		if (!success) {
			getLogger().warning("No performance vector found among averagable results. Performance will not be loggable.");
		}
	}

	/** Applies the learner (= first encapsulated inner operator). for building the final model. */
	protected void learnFinalModel(ExampleSet trainingSet) throws OperatorException {
		learn(trainingSet);
	}

	/** Applies the learner (= first encapsulated inner operator). */
	protected final void learn(ExampleSet trainingSet) throws OperatorException {
		trainingProcessExampleSetOutput.deliver(trainingSet);
		executeLearner();
	}

	/**
	 * Applies the applier and evaluator (= second subprocess). In order to reuse possibly created
	 * predicted label attributes, we do the following: We compare the predicted label of
	 * <code>testSet</code> before and after applying the inner operator. If it changed, the
	 * predicted label is removed again. No outer operator could ever see it. The same applies for
	 * the confidence attributes in case of classification learning.
	 */
	protected final void evaluate(ExampleSet testSet) throws OperatorException {
		Attribute predictedBefore = testSet.getAttributes().getPredictedLabel();

		applyProcessExampleSetOutput.deliver(testSet);
		applyProcessModelOutput.deliver(trainingProcessModelInput.getData(IOObject.class));
		throughExtender.passDataThrough();

		executeEvaluator();

		Tools.buildAverages(applyProcessPerformancePortExtender);

		Attribute predictedAfter = testSet.getAttributes().getPredictedLabel();
		// remove predicted label and confidence attributes if there is a new prediction which is
		// not equal to an old one
		if (predictedAfter != null
				&& (predictedBefore == null || predictedBefore.getTableIndex() != predictedAfter.getTableIndex())) {
			PredictionModel.removePredictedLabel(testSet);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_CREATE_COMPLETE_MODEL,
				"Indicates if a model of the complete data set should be additionally build after estimation.", false);
		type.setDeprecated();
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
