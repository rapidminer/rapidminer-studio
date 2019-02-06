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
package com.rapidminer.operator.visualization;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.LiftDataGenerator;

import java.util.List;


/**
 * This operator creates a Lift chart for the given example set and model. The model will be applied
 * on the example set and a lift chart will be produced afterwards.
 * 
 * Please note that a predicted label of the given example set will be removed during the
 * application of this operator.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class LiftChartGenerator extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private InputPort modelInput = getInputPorts().createPort("model", Model.class);

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("model");

	public LiftChartGenerator(OperatorDescription description) {
		super(description);

		exampleSetInput
				.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Attributes.LABEL_NAME, Ontology.NOMINAL));

		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addPassThroughRule(modelInput, modelOutput);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Model model = modelInput.getData(Model.class);

		Tools.hasNominalLabels(exampleSet, getOperatorClassName());
		Attribute label = exampleSet.getAttributes().getLabel();
		if (label.getMapping().size() != 2) {
			throw new UserError(this, 114, getOperatorClassName(), label);
		}

		ExampleSet workingSet = (ExampleSet) exampleSet.clone();
		if (workingSet.getAttributes().getPredictedLabel() != null) {
			PredictionModel.removePredictedLabel(workingSet);
		}

		workingSet = model.apply(workingSet);
		if (workingSet.getAttributes().getPredictedLabel() == null) {
			throw new UserError(this, 107);
		}

		LiftDataGenerator liftDataGenerator = new LiftDataGenerator();
		List<double[]> liftPoints = liftDataGenerator.createLiftDataList(workingSet);
		liftDataGenerator.createLiftChartPlot(liftPoints);

		PredictionModel.removePredictedLabel(workingSet);

		exampleSetOutput.deliver(exampleSet);
		modelOutput.deliver(model);
	}
}
