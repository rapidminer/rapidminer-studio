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
package com.rapidminer.operator;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;


/**
 * This operator updates a {@link Model} with an {@link ExampleSet}. Please note that the model must
 * return true for {@link Model#isUpdatable()} in order to be usable with this operator.
 * 
 * @author Ingo Mierswa
 */
public class ModelUpdater extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private InputPort modelInput = getInputPorts().createPort("model", Model.class);

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("model");

	public ModelUpdater(OperatorDescription description) {
		super(description);

		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addPassThroughRule(modelInput, modelOutput);
	}

	/**
	 * Applies the operator and labels the {@link ExampleSet}. The example set in the input is not
	 * consumed.
	 */
	@Override
	public void doWork() throws OperatorException {
		ExampleSet inputExampleSet = exampleSetInput.getData(ExampleSet.class);
		Model model = modelInput.getData(Model.class);
		if (!model.isUpdatable()) {
			throw new UserError(this, 135, model.getClass());
		}

		try {
			model.updateModel(inputExampleSet);
		} catch (UserError e) {
			if (e.getOperator() == null) {
				e.setOperator(this);
			}
			throw e;
		}

		exampleSetOutput.deliver(inputExampleSet);
		modelOutput.deliver(model);
	}
}
