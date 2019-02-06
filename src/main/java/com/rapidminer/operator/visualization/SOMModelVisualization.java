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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;


/**
 * This class provides an operator for the visualization of arbitrary models with help of the
 * dimensionality reduction via a SOM of both the data set and the given model.
 *
 * @author Sebastian Land
 */
public class SOMModelVisualization extends Operator {

	public static class SOMModelVisualizationResult extends ResultObjectAdapter {

		private static final long serialVersionUID = -6250201023324000922L;

		private ExampleSet exampleSet;
		private Model model;

		public SOMModelVisualizationResult(ExampleSet exampleSet, Model model) {
			this.exampleSet = exampleSet;
			this.model = model;
		}

		@Override
		public String getName() {
			return "ModelVisualization";
		}

		@Override
		public String toString() {
			return "The model visualized via a SOM plot.";
		}

		public ExampleSet getExampleSet() {
			return exampleSet;
		}

		public Model getModel() {
			return model;
		}
	}

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private InputPort modelInput = getInputPorts().createPort("model", Model.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("model");
	private OutputPort visualizationOutput = getOutputPorts().createPort("visualization");

	public SOMModelVisualization(OperatorDescription description) {
		super(description);

		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addPassThroughRule(modelInput, modelOutput);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Model model = modelInput.getData(Model.class);

		exampleSetOutput.deliver(exampleSet);
		modelOutput.deliver(model);
		visualizationOutput.deliver(new SOMModelVisualizationResult(exampleSet, model));
	}
}
