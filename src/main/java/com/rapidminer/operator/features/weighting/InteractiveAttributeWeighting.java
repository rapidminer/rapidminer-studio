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
package com.rapidminer.operator.features.weighting;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.dialog.AttributeWeightsDialog;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;


/**
 * This operator shows a window with the currently used attribute weights and allows users to change
 * the weight interactively.
 * 
 * @author Ingo Mierswa
 */
public class InteractiveAttributeWeighting extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private InputPort weightInput = getInputPorts().createPort("weight");

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort weightOutput = getOutputPorts().createPort("weight");

	public InteractiveAttributeWeighting(OperatorDescription description) {
		super(description);

		weightInput.addPrecondition(new SimplePrecondition(weightInput, new MetaData(AttributeWeights.class), false));
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addPassThroughRule(weightInput, weightOutput);
	}

	@Override
	public void doWork() throws OperatorException {
		AttributeWeights weights = weightInput.getDataOrNull(AttributeWeights.class);
		ExampleSet exampleSet = null;
		if (weights == null) {
			log("No feature weights found in input. Trying to find an example set...");
			weights = new AttributeWeights();
			exampleSet = exampleSetInput.getDataOrNull(ExampleSet.class);
			if (exampleSet != null) {
				for (Attribute attribute : exampleSet.getAttributes()) {
					weights.setWeight(attribute.getName(), 1.0d);
				}
				log("ExampleSet found! Initially all attributes will be used with weight 1.");
			} else {
				log("No examples found! Starting dialog without any weights.");
			}
		}

		AttributeWeightsDialog attributeWeightsDialog = new AttributeWeightsDialog(weights);
		attributeWeightsDialog.setVisible(true);

		if (attributeWeightsDialog.isOk()) {
			weights = attributeWeightsDialog.getAttributeWeights();
		}

		weightOutput.deliver(weights);
		exampleSetOutput.deliver(exampleSet);
	}
}
