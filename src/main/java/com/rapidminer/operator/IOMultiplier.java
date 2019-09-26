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

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPortExtender;


/**
 * This operator copies its input to all connected output ports.
 * 
 * @author Simon Fischer
 * 
 */
public class IOMultiplier extends Operator {

	private final InputPort inputPort = getInputPorts().createPort("input");
	private final OutputPortExtender outputExtender = new OutputPortExtender("output", getOutputPorts());

	public IOMultiplier(OperatorDescription description) {
		super(description);
		outputExtender.start();
		getTransformer().addRule(outputExtender.makePassThroughRule(inputPort));
	}

	@Override
	public void doWork() {
		IOObject input = inputPort.getRawData();
		if (input != null) {
			for (OutputPort outputPort : outputExtender.getManagedPorts()) {
				outputPort.deliver(input.copy());
			}
		}
	}

	/** The port whose IOObject is copied. */
	public InputPort getInputPort() {
		return inputPort;
	}
}
