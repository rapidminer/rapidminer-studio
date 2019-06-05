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
package com.rapidminer.operator.ports;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * This extender is just for operators which don't have any real input. It just should ensure the
 * correct execution order. And throw a warning if nothing is connected.
 * 
 * @author Sebastian Land
 * 
 */
public class DummyPortPairExtender extends PortPairExtender {

	public DummyPortPairExtender(String name, InputPorts inPorts, OutputPorts outPorts) {
		super(name, inPorts, outPorts);
	}

	/**
	 * The generated rule copies all meta data from the generated input ports to all generated
	 * output ports. Unlike the PortPairExtender, it warns if nothing is connected.
	 */
	@Override
	public MDTransformationRule makePassThroughRule() {
		return () -> {
			boolean somethingConnected = false;
			for (PortPair pair : getManagedPairs()) {
				// testing if connected for execution order
				somethingConnected |= pair.getInputPort().isConnected() || pair.getOutputPort().isConnected();
				// transforming meta data.
				MetaData inData = pair.getInputPort().getMetaData();
				if (inData != null) {
					inData = transformMetaData(inData.clone());
					inData.addToHistory(pair.getOutputPort());
					pair.getOutputPort().deliverMD(inData);
				} else {
					pair.getOutputPort().deliverMD(null);
				}
			}
			if (!somethingConnected) {
				PortOwner owner = getManagedPairs().get(0).getInputPort().getPorts().getOwner();
				Operator operator = owner.getOperator();
				// check all other ports, too
				InputPorts inputPorts = operator.getInputPorts();
				OutputPorts outputPorts = operator.getOutputPorts();
				if ((inputPorts.getNumberOfPorts() == getManagedPairs().size() || inputPorts.getNumberOfConnectedPorts() == 0)
						&& (outputPorts.getNumberOfPorts() == getManagedPairs().size() || outputPorts.getNumberOfConnectedPorts() == 0)) {
					operator.addError(new SimpleProcessSetupError(Severity.WARNING, owner, "execution_order_undefined"));
				}
			}
		};
	}
}
