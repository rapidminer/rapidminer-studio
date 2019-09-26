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
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPort;


/**
 * This operator returns the first non-null input it receives.
 * 
 * @author Simon Fischer
 * 
 */
public class IOMerger extends Operator {

	private final OutputPort outputPort = getOutputPorts().createPort("output");
	private final InputPortExtender inputExtender = new InputPortExtender("input", getInputPorts());

	public IOMerger(OperatorDescription description) {
		super(description);
		inputExtender.start();
		getTransformer().addRule(inputExtender.makePassThroughRule(outputPort));
	}

	@Override
	public void doWork() {
		for (InputPort inputPort : inputExtender.getManagedPorts()) {
			IOObject input = inputPort.getRawData();
			if (input != null) {
				outputPort.deliver(input);
				return;
			}
		}
		outputPort.deliver(null);
	}
}
