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
package com.rapidminer.operator.ports.quickfix;

import java.util.logging.Level;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortException;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.LogService;


/**
 * Connects the last operator in a process to the process result ports.
 *
 * @author Marco Boeck
 * @since 8.2
 */
public class ConnectLastOperatorToOutputPortsQuickFix extends AbstractQuickFix {

	private final Operator lastOperator;

	public ConnectLastOperatorToOutputPortsQuickFix(Operator lastOperator) {
		super(MAX_RATING, false, "connect_to_result_ports");
		this.lastOperator = lastOperator;
	}

	@Override
	public void apply() {
		connectPorts(true);
	}

	/**
	 * Tries to connect the output ports of the last operator.
	 *
	 * @param skipPortsWithoutMetaData
	 * 		if {@code true}, ports that have no metadata will not be connected
	 */
	private void connectPorts(final boolean skipPortsWithoutMetaData) {
		int index = 0;
		InputPorts innerSinks = lastOperator.getProcess().getRootOperator().getSubprocess(0).getInnerSinks();
		for (OutputPort outputPort : lastOperator.getOutputPorts().getAllPorts()) {
			if (!outputPort.isConnected() && outputPort.shouldAutoConnect()) {
				try {
					if (skipPortsWithoutMetaData && outputPort.getMetaData(MetaData.class) == null) {
						continue;
					}
					InputPort inputPort;
					// search first free input port
					do {
						// stop if no more free ports are available
						// this will not stop for inner sinks that have a port extender, since those always add new ports
						if (index >= innerSinks.getNumberOfPorts()) {
							return;
						}
						inputPort = innerSinks.getPortByIndex(index++);
					} while (inputPort.isConnected());
					outputPort.connectTo(inputPort);
				} catch (PortException | IncompatibleMDClassException e) {
					// cannot happen because the port is not connected yet and we use the top-level class - ignore
					LogService.getRoot().log(Level.WARNING, "Error while connecting port", e);
				}
			}
		}

		// nothing was connected, and we skipped ports w/o metadata? Try again once, this time connect ports w/o metadata
		if (index == 0 && skipPortsWithoutMetaData) {
			connectPorts(false);
		}
	}
}
