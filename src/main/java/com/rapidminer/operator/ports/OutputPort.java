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

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.metadata.MDTransformer;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * An output port which forwards it output to a connected input port. Operators place their output
 * into output ports which take care of forwarding the data to the input ports.
 * 
 * @author Simon Fischer
 */
public interface OutputPort extends Port {

	/**
	 * Connects to an input port.
	 * 
	 * @throws PortException
	 *             if already connected.
	 */
	public void connectTo(InputPort inputPort) throws PortException;

	/**
	 * Disconnects the OutputPort from its InputPort. Note: As a side effect, disconnecting ports
	 * may trigger PortExtenders removing these ports. In order to avoid this behaviour,
	 * {@link #lock()} port first.
	 * 
	 * @throws PortException
	 *             if not connected.
	 */
	public void disconnect() throws PortException;

	/**
	 * Delivers an object to the connected {@link InputPort} or ignores it if the output port is not
	 * connected.
	 */
	public void deliver(IOObject object);

	/** Returns the destination input port. */
	public InputPort getDestination();

	/**
	 * Does the same as {@link #deliver(Object)} except that only meta data is delivered. This
	 * method is called by the Operator's {@link MDTransformer}.
	 */
	public void deliverMD(MetaData md);

	/**
	 * Asks the owning operator
	 * {@link com.rapidminer.operator.Operator#shouldAutoConnect(OutputPort)}.
	 */
	public boolean shouldAutoConnect();
}
