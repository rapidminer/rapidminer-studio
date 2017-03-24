/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.Learner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPortExtender;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.GeneratePredictionModelTransformationRule;
import com.rapidminer.operator.ports.metadata.PredictionModelMetaData;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;

import java.util.LinkedList;
import java.util.List;


/**
 * This class uses n+1 inner learners and generates n different models by using the last n learners.
 * The predictions of these n models are taken to create n new features for the example set, which
 * is finally used to serve as an input of the first inner learner.
 * 
 * @author Ingo Mierswa, Helge Homburg
 */
public abstract class AbstractStacking extends OperatorChain implements Learner {

	protected InputPort exampleSetInput = getInputPorts().createPort("training set", ExampleSet.class);
	protected OutputPortExtender baseInputExtender = new OutputPortExtender("training set", getBaseModelLearnerProcess()
			.getInnerSources());
	protected InputPortExtender baseModelExtender = new InputPortExtender("base model", getBaseModelLearnerProcess()
			.getInnerSinks(), new PredictionModelMetaData(PredictionModel.class, new ExampleSetMetaData()), 2);

	protected OutputPort modelOutput = getOutputPorts().createPort("model");

	public AbstractStacking(OperatorDescription description, String... subprocessNames) {
		super(description, subprocessNames);
		baseInputExtender.start();
		baseModelExtender.start();
		getTransformer().addRule(baseInputExtender.makePassThroughRule(exampleSetInput));
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(
				new GeneratePredictionModelTransformationRule(exampleSetInput, modelOutput, PredictionModel.class));
	}

	/** Returns the model name. */
	protected abstract String getModelName();

	protected abstract ExecutionUnit getBaseModelLearnerProcess();

	/** Returns the learner which should be used for stacking. */
	protected abstract Model getStackingModel(ExampleSet stackingLearningSet) throws OperatorException;

	/** Indicates if the old attributes should be kept for learning the stacking model. */
	public abstract boolean keepOldAttributes();

	@Override
	public void doWork() throws OperatorException {
		ExampleSet input = exampleSetInput.getData(ExampleSet.class);
		modelOutput.deliver(learn(input));
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		// learn base models
		baseInputExtender.deliverToAll(exampleSet, false);
		getBaseModelLearnerProcess().execute();
		List<Model> baseModels = baseModelExtender.getData(Model.class, true);

		// create temporary example set for stacking
		ExampleSet stackingLearningSet = (ExampleSet) exampleSet.clone();
		if (!keepOldAttributes()) {
			stackingLearningSet.getAttributes().clearRegular();
		}

		List<Attribute> tempPredictions = new LinkedList<Attribute>();
		int i = 0;
		for (Model baseModel : baseModels) {
			exampleSet = baseModel.apply(exampleSet);
			Attribute predictedLabel = exampleSet.getAttributes().getPredictedLabel();
			// renaming attribute
			predictedLabel.setName("base_prediction" + i);
			// confidences already removed, predicted label is kept in table
			PredictionModel.removePredictedLabel(exampleSet, false, true);
			stackingLearningSet.getAttributes().addRegular(predictedLabel);
			tempPredictions.add(predictedLabel);
			i++;
		}

		// learn stacked model
		Model stackingModel = getStackingModel(stackingLearningSet);

		// remove temporary predictions from table (confidences were already removed)
		PredictionModel.removePredictedLabel(stackingLearningSet);
		for (Attribute tempPrediction : tempPredictions) {
			stackingLearningSet.getAttributes().remove(tempPrediction);
			stackingLearningSet.getExampleTable().removeAttribute(tempPrediction);
		}

		// create and return model
		return new StackingModel(exampleSet, getModelName(), baseModels, stackingModel, keepOldAttributes());
	}

	/** The default implementation throws an exception. */
	@Override
	public PerformanceVector getEstimatedPerformance() throws OperatorException {
		throw new UserError(this, 912, getName(), "estimation of performance not supported.");
	}

	/**
	 * The default implementation throws an exception.
	 */
	@Override
	public AttributeWeights getWeights(ExampleSet exampleSet) throws OperatorException {
		throw new UserError(this, 916, getName(), "calculation of weights not supported.");
	}

	@Override
	public boolean shouldEstimatePerformance() {
		return false;
	}

	@Override
	public boolean shouldCalculateWeights() {
		return false;
	}

	@Override
	public boolean supportsCapability(OperatorCapability c) {
		return true;
	}
}
