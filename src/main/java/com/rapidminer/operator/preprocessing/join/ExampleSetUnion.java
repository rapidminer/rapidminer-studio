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
package com.rapidminer.operator.preprocessing.join;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetUnionRule;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.OperatorService;

import java.util.Arrays;


/**
 * This operator performs two steps: first, it build the union set / superset of features of both
 * input example sets where common features are kept and both feature sets are extended in a way
 * that the feature sets are equal for both example sets. The second step then merges both example
 * sets and will deliver the resulting example set.
 * 
 * @author Ingo Mierswa
 */
public class ExampleSetUnion extends Operator {

	private InputPort exampleSet1Input = getInputPorts().createPort("example set 1", ExampleSet.class);
	private InputPort exampleSet2Input = getInputPorts().createPort("example set 2", ExampleSet.class);
	private OutputPort unionOutput = getOutputPorts().createPort("union");

	public ExampleSetUnion(OperatorDescription description) {
		super(description);
		// exampleSet1Input.addPrecondition(new ExampleSetPrecondition(exampleSet1Input, -1,
		// Attributes.ID_NAME));
		// exampleSet2Input.addPrecondition(new ExampleSetPrecondition(exampleSet2Input, -1,
		// Attributes.ID_NAME));
		getTransformer().addRule(new ExampleSetUnionRule(exampleSet1Input, exampleSet2Input, unionOutput, null));
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet1 = (ExampleSet) exampleSet1Input.getData(ExampleSet.class).clone();
		ExampleSet exampleSet2 = (ExampleSet) exampleSet2Input.getData(ExampleSet.class).clone();
		try {
			ExampleSetSuperset supersetOperator = OperatorService.createOperator(ExampleSetSuperset.class);
			supersetOperator.setParameter(ExampleSetSuperset.PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES, "true");
			ExampleSetMerge mergeOperator = OperatorService.createOperator(ExampleSetMerge.class);

			supersetOperator.superset(exampleSet1, exampleSet2);
			unionOutput.deliver(mergeOperator.merge(Arrays.asList(new ExampleSet[] { exampleSet1, exampleSet2 })));

		} catch (OperatorCreationException e) {
			throw new UserError(this, 904, "inner operator", e.getMessage());
		}
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPorts().getPortByIndex(0),
				ExampleSetUnion.class, null);
	}
}
