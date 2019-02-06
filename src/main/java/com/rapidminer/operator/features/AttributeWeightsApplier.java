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
package com.rapidminer.operator.features;

import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.AttributeWeightedExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;


/**
 * <p>
 * This operator deselects attributes with a weight value of 0.0. The values of the other numeric
 * attributes will be recalculated based on the weights delivered as {@link AttributeWeights} object
 * in the input.
 * </p>
 * 
 * <p>
 * This operator can hardly be used to select a subset of features according to weights determined
 * by a former weighting scheme. For this purpose the operator
 * {@link com.rapidminer.operator.features.selection.AttributeWeightSelection} should be used which
 * will select only those attribute fulfilling a specified weight relation.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class AttributeWeightsApplier extends Operator {

	private final InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private final InputPort weightsInput = getInputPorts().createPort("weights", AttributeWeights.class);

	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	public AttributeWeightsApplier(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.SUBSET));
	}

	@Override
	public void doWork() throws OperatorException {
		AttributeWeights weights = weightsInput.getData(AttributeWeights.class);
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		AttributeWeightedExampleSet weightedSet = new AttributeWeightedExampleSet(exampleSet, weights);
		ExampleSet result = weightedSet.createCleanClone();

		exampleSetOutput.deliver(result);
	}
}
