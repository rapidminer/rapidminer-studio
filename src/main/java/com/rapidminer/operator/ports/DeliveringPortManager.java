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

import com.rapidminer.Process;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;


/**
 * This util helps resolving the {@link OutputPort} that is identified by an {@link DeliveringPortIdentifier}
 *
 * @author Andreas Timm
 * @since 9.1
 */
public final class DeliveringPortManager {
	/**
	 * Key for the last delivering port which can be used in {@link IOObject}s
	 */
	public static final String LAST_DELIVERING_PORT = "last_delivering_port";

	/**
	 * Util class, instantiation not supported
	 */
	private DeliveringPortManager() {
		throw new UnsupportedOperationException("Instantiation not supported");
	}

	/**
	 * Store the {@link Operator} and {@link Port} as last delivering port in the user data of the {@link IOObject}.
	 * If the given port is an {@link InputPort}, it's {@link InputPort#getSource() source} will be stored instead.
	 *
	 * @param ioObject
	 * 		which passed through the {@link Port} and should hold this information in its {@link IOObject#getUserData(String)}
	 * @param port
	 * 		the {@link Port} that delivered this ioObject
	 * @see #resolve(Process, DeliveringPortIdentifier)
	 */
	public static void setLastDeliveringPort(IOObject ioObject, Port port) {
		if (port instanceof InputPort) {
			// always store an output port
			port = ((InputPort) port).getSource();
		}
		PortOwner owner = port.getPorts().getOwner();
		OperatorChain portHandler = owner.getPortHandler();
		Operator operator = owner.getOperator();
		int executionUnitId = DeliveringPortIdentifier.DEFAULT_SUBPROCESS_ID;
		if (operator == portHandler) {
			executionUnitId = portHandler.getSubprocesses().indexOf(owner.getConnectionContext());
		}
		ioObject.setUserData(LAST_DELIVERING_PORT, new DeliveringPortIdentifier(operator.getName(), port.getName(), executionUnitId));
	}

	/**
	 * Find an {@link OutputPort} using the information in a {@link DeliveringPortIdentifier} from the currently
	 * visible Process in Studio.
	 *
	 * @param process
	 * 		the {@link Process} in which the {@link OutputPort} should be searched
	 * @param identifier
	 * 		with sufficient information to find the {@link OutputPort}
	 * @return the identified {@link OutputPort} or null
	 */
	public static OutputPort resolve(Process process, DeliveringPortIdentifier identifier) {
		if (identifier == null || identifier.getOperatorName() == null || identifier.getPortName() == null || process == null) {
			return null;
		}
		final Operator operator = process.getOperator(identifier.getOperatorName());
		if (operator == null) {
			return null;
		}
		if (identifier.getSubprocessId() >= 0) {
			if (operator instanceof OperatorChain && identifier.getSubprocessId() < ((OperatorChain) operator).getNumberOfSubprocesses()) {
				return ((OperatorChain) operator).getSubprocess(identifier.getSubprocessId()).getInnerSources().getPortByName(identifier.getPortName());
			}
			return null;
		} else {
			return operator.getOutputPorts().getPortByName(identifier.getPortName());
		}
	}

	/**
	 * Shorthand method to get the last delivering {@link OutputPort} from the given process which was stored in the
	 * ioObject's userdata.
	 *
	 * @param process
	 * 		the {@link Process} that contains the to be returned {@link OutputPort}
	 * @param ioObject
	 * 		the {@link IOObject} that was delivered lately
	 * @return the last registered {@link OutputPort} from the ioObject's userdata, may be null
	 */
	public static OutputPort getLastDeliveringPort(Process process, IOObject ioObject) {
		if (process == null || ioObject == null) {
			return null;
		}
		return resolve(process, (DeliveringPortIdentifier) ioObject.getUserData(LAST_DELIVERING_PORT));
	}
}
