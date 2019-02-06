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

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;

import java.util.List;


/**
 * @author Simon Fischer
 */
public class InputPortsImpl extends AbstractPorts<InputPort> implements InputPorts {

	public InputPortsImpl(PortOwner owner) {
		super(owner);
	}

	@Override
	public void checkPreconditions() {
		for (InputPort port : getAllPorts()) {
			port.checkPreconditions();
		}
	}

	@Override
	public InputPort createPort(String name) {
		return createPort(name, true);
	}

	@Override
	public InputPort createPort(String name, boolean add) {
		InputPort in = new InputPortImpl(this, name, true);
		if (add) {
			addPort(in);
		}
		return in;
	}

	@Override
	public InputPort createPassThroughPort(String name) {
		InputPort in = new InputPortImpl(this, name, false);
		addPort(in);
		return in;
	}

	@Override
	public InputPort createPort(String name, Class<? extends IOObject> clazz) {
		return createPort(name, new MetaData(clazz));
	}

	@Override
	public InputPort createPort(String name, MetaData metaData) {
		InputPort in = createPort(name);
		in.addPrecondition(new SimplePrecondition(in, metaData));
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
			for (InputPort port : getAllPorts()) {
				if (port.isConnected()) {
					OutputPort source = port.getSource();
					boolean isException = false;
					if (exceptions != null) {
						Operator sourceOp = source.getPorts().getOwner().getOperator();
						if (exceptions.contains(sourceOp)) {
							isException = true;
						}
					}
					if (!isException) {
						source.disconnect();
						success = true;
						continue disconnect;
					}
				}
			}
		} while (success);
	}
}
