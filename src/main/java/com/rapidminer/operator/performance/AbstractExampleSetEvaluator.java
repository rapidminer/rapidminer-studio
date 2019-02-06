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
package com.rapidminer.operator.performance;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.PassThroughRule;


/**
 * Abstract superclass of operators accepting an ExampleSet and producing a PerformanceVector.
 * 
 * @author Simon Fischer
 */
public abstract class AbstractExampleSetEvaluator extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort performanceOutput = getOutputPorts().createPort("performance");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	public AbstractExampleSetEvaluator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new PassThroughRule(exampleSetInput, exampleSetOutput, false));
		getTransformer().addRule(new GenerateNewMDRule(performanceOutput, PerformanceVector.class));
	}

	/** Implements the evaluation. Called by {@link #apply()}. */
	public abstract PerformanceVector evaluate(ExampleSet exampleSet) throws OperatorException;

	@Override
	public void doWork() throws OperatorException {
		ExampleSet input = exampleSetInput.getData(ExampleSet.class);
		performanceOutput.deliver(evaluate(input));
		exampleSetOutput.deliver(input);
	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == exampleSetOutput) {
			return getParameterAsBoolean("keep_example_set");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	protected InputPort getExampleSetInputPort() {
		return exampleSetInput;
	}
}
