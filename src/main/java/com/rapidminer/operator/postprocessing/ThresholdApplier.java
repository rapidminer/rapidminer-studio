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
package com.rapidminer.operator.postprocessing;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.tools.Ontology;


/**
 * This operator applies the given threshold to an example set and maps a soft prediction to crisp
 * values. If the confidence for the second class (usually positive for RapidMiner) is greater than
 * the given threshold the prediction is set to this class.
 *
 * @author Ingo Mierswa, Martin Scholz
 */
public class ThresholdApplier extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private InputPort thresholdInput = getInputPorts().createPort("threshold", Threshold.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	public ThresholdApplier(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.VALUE_TYPE,
				Attributes.PREDICTION_NAME, Attributes.CONFIDENCE_NAME));
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Threshold threshold = thresholdInput.getData(Threshold.class);

		Attribute predictedLabel = exampleSet.getAttributes().getPredictedLabel();
		if (predictedLabel == null) {
			throw new UserError(this, 107);
		}

		int zeroIndex = 0;
		int oneIndex = 0;
		try {
			zeroIndex = predictedLabel.getMapping().mapString(threshold.getZeroClass());
		} catch (AttributeTypeException e) {
			throw new UserError(this, 147, threshold.getZeroClass());
		}
		try {
			oneIndex = predictedLabel.getMapping().mapString(threshold.getOneClass());
		} catch (AttributeTypeException e) {
			throw new UserError(this, 147, threshold.getOneClass());
		}

		// create a new example set with a new prediction attribute
		ExampleSet newExampleSet = (ExampleSet) exampleSet.clone();
		Attribute newPredictedLabel = AttributeFactory.createAttribute(predictedLabel.getName(),
				predictedLabel.getValueType());
		zeroIndex = newPredictedLabel.getMapping().mapString(predictedLabel.getMapping().mapIndex(zeroIndex));
		oneIndex = newPredictedLabel.getMapping().mapString(predictedLabel.getMapping().mapIndex(oneIndex));
		newExampleSet.getExampleTable().addAttribute(newPredictedLabel);
		newExampleSet.getAttributes().setPredictedLabel(newPredictedLabel);

		for (Example example : newExampleSet) {
			double oneClassConfidence = example.getConfidence(threshold.getOneClass());
			double crispPrediction = oneClassConfidence > threshold.getThreshold() ? oneIndex : zeroIndex;
			example.setValue(newPredictedLabel, crispPrediction);
		}

		exampleSetOutput.deliver(newExampleSet);
	}
}
