/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;

import java.util.Collection;


/**
 * A port that receives data from a connected output port. Operators query their input ports for
 * data.
 * 
 * @author Simon Fischer
 */
public interface InputPort extends Port<InputPort, OutputPort> {

	@Override
	default void connectTo(OutputPort opposite) throws PortException {
		opposite.connectTo(this);
	}

	@Override
	default boolean canConnectTo(Port other) {
		return other instanceof OutputPort;
	}

	/**
	 * Receives data from the output port. Throws an {@link PortException} if data has already been
	 * set. (Called by {@link OutputPort#deliver(IOObject)} . *
	 */
	void receive(IOObject object);

	/** Does the same as {@link #receive(IOObject)} but only with meta data. */
	void receiveMD(MetaData metaData);

	/** Returns the meta data received by {@link #receiveMD(MetaData)}. */
	@Override
	MetaData getMetaData();

	/**
	 * @return the same as {@link #getSource()}
	 */
	@Override
	default OutputPort getOpposite() {
		return getSource();
	}

	/**
	 * @return this port
	 */
	@Override
	default InputPort getDestination() {
		return this;
	}

	/** Adds a precondition to this input port. */
	void addPrecondition(Precondition precondition);

	/** Returns a collection (view) of all preconditions assigned to this InputPort. */
	Collection<Precondition> getAllPreconditions();

	/** Checks all registered preconditions. */
	void checkPreconditions();

	/** Returns true if the given input is compatible with the preconditions. */
	boolean isInputCompatible(MetaData input, CompatibilityLevel level);

	/** Returns a human readable representation of the preconditions. */
	String getPreconditionDescription();

	/**
	 * This will add the given listener to this port. It is informed whenever the method
	 * {@link #receiveMD(MetaData)} is called.
	 */
	void registerMetaDataChangeListener(MetaDataChangeListener listener);

	/**
	 * Removes the given listener again.
	 */
	void removeMetaDataChangeListener(MetaDataChangeListener listener);

	/**
	 * Delegates to {@link #getSource() getSource().disconnect()} if connected.
	 *
	 * @throws PortException if this port is not connected
	 * @since 9.7
	 */
	@Override
	default void disconnect() throws PortException {
		OutputPort outputPort = getSource();
		if (outputPort == null) {
			throw new PortException(this, "Not connected.");
		}
		outputPort.disconnect();
	}
}
