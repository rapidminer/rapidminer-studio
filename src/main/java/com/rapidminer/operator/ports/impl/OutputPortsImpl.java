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
package com.rapidminer.operator.ports.impl;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.PortOwner;

import java.util.List;


/**
 * @author Simon Fischer
 */
public class OutputPortsImpl extends AbstractPorts<OutputPort> implements OutputPorts {

	public OutputPortsImpl(PortOwner owner) {
		super(owner);
	}

	@Override
	public OutputPort createPort(String name) {
		return createPort(name, true);
	}

	@Override
	public OutputPort createPort(String name, boolean add) {
		OutputPort out = new OutputPortImpl(this, name, true);
		if (add) {
			addPort(out);
		}
		return out;
	}

	@Override
	public OutputPort createPassThroughPort(String name) {
		OutputPort in = new OutputPortImpl(this, name, false);
		addPort(in);
		return in;
	}

	@Override
	public void disconnectAll() {
		disconnectAllBut(null);
	}

	@Override
	public void disconnectAllBut(List<Operator> exceptions) {
		boolean success;
		disconnect: do {
			success = false;
			for (OutputPort port : getAllPorts()) {
				if (port.isConnected()) {
					InputPort destination = port.getDestination();
					boolean isException = false;
					if (exceptions != null) {
						Operator destOp = destination.getPorts().getOwner().getOperator();
						if (exceptions.contains(destOp)) {
							isException = true;
						}
					}
					if (!isException) {
						port.disconnect();
						success = true;
						continue disconnect;
					}
				}
			}
		} while (success);
	}
}
