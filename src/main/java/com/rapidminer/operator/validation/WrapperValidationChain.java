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

import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;


/**
 * This operator evaluates the performance of feature weighting algorithms including feature
 * selection. The first subprocess contains the algorithm to be evaluated itself. It must return an
 * attribute weights vector which is then applied on the data. The second subprocess must build and
 * return a new model on this data, which is then evaluated during the third subprocess. Hence it
 * must return a performance vector. This performance vector serves as a performance indicator for
 * the actual algorithm.
 * 
 * @author Ingo Mierswa
 */
public abstract class WrapperValidationChain extends OperatorChain implements CapabilityProvider {

	private PerformanceCriterion lastPerformance;

	protected InputPort exampleSetInput = getInputPorts().createPort("example set in");
	private final OutputPort innerWeightingSetSource = getSubprocess(0).getInnerSources().createPort("weighting set source");
	private final InputPort innerAttributeWeightsSink = getSubprocess(0).getInnerSinks().createPort(
			"attribute weights sink", AttributeWeights.class);
	private final OutputPort innerTrainSetSource = getSubprocess(1).getInnerSources().createPort("train set source");
	private final InputPort innerModelSink = getSubprocess(1).getInnerSinks().createPort("model sink", Model.class);
	private final OutputPort innerTestSetSource = getSubprocess(2).getInnerSources().createPort("test set source");
	private final OutputPort innerModelSource = getSubprocess(2).getInnerSources().createPort("model source");
	private final InputPort innerPerformanceSink = getSubprocess(2).getInnerSinks().createPort("performance vector sink",
			PerformanceVector.class);

	protected OutputPort performanceOutput = getOutputPorts().createPort("performance vector out");
	protected OutputPort attributeWeightsOutput = getOutputPorts().createPort("attribute weights out");

	public WrapperValidationChain(OperatorDescription description) {
		super(description, "Attribute Weighting", "Model Building", "Model Evaluation");

		exampleSetInput.addPrecondition(getCapabilityPrecondition());

		getTransformer().addPassThroughRule(exampleSetInput, innerWeightingSetSource);
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addPassThroughRule(exampleSetInput, innerTrainSetSource);
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(1)));
		getTransformer().addPassThroughRule(exampleSetInput, innerTestSetSource);
		getTransformer().addPassThroughRule(innerModelSink, innerModelSource);
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(2)));
		getTransformer().addPassThroughRule(innerPerformanceSink, performanceOutput);
		getTransformer().addPassThroughRule(innerAttributeWeightsSink, attributeWeightsOutput);

		addValue(new ValueDouble("performance", "The last performance (main criterion).") {

			@Override
			public double getDoubleValue() {
				if (lastPerformance != null) {
					return lastPerformance.getAverage();
				} else {
					return Double.NaN;
				}
			}
		});
		addValue(new ValueDouble("variance", "The variance of the last performance (main criterion).") {

			@Override
			public double getDoubleValue() {
				if (lastPerformance != null) {
					return lastPerformance.getVariance();
				} else {
					return Double.NaN;
				}
			}
		});
	}

	/**
	 * This method can be overwritten in order to give a more senseful quickfix.
	 */
	protected Precondition getCapabilityPrecondition() {
		return new CapabilityPrecondition(this, exampleSetInput);
	}

	/**
	 * Can be used by subclasses to set the performance of the example set. Will be used for
	 * plotting only.
	 */
	void setResult(PerformanceCriterion pc) {
		lastPerformance = pc;
	}

	/** Applies the method. */
	AttributeWeights useWeightingMethod(ExampleSet methodTrainingSet) throws OperatorException {
		innerWeightingSetSource.deliver(methodTrainingSet);
		getSubprocess(0).execute();
		return innerAttributeWeightsSink.getData(AttributeWeights.class);
	}

	/** Applies the learner. */
	Model learn(ExampleSet trainingSet) throws OperatorException {
		innerTrainSetSource.deliver(trainingSet);
		getSubprocess(1).execute();
		return innerModelSink.getData(Model.class);
	}

	/** Applies the applier and evaluator. */
	PerformanceVector evaluate(ExampleSet testSet, Model model) throws OperatorException {
		innerTestSetSource.deliver(testSet);
		innerModelSource.deliver(model);
		getSubprocess(2).execute();
		return innerPerformanceSink.getData(PerformanceVector.class);
	}
}
