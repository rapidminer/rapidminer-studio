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

import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.tools.Observable;


/**
 * Operators in a process are connected via input and output ports. Whenever an operator generates
 * output (or meta data about output), it is delivered to the connected input port. <br/>
 * This interface defines all behavior and properies common to input and output ports. This is
 * basically names, description etc., as well as adding messages about problems in the process setup
 * and quick fixes.
 * <p>
 * Implementations should extend {@link com.rapidminer.operator.ports.impl.AbstractPort}.
 * <p>
 * Idea for self reference generics: <a href="https://www.sitepoint.com/self-types-with-javas-generics/">Self Types with Java's Generics</a>
 *
 * @param <S>
 * 		self reference type; responsible for the type of {@code ports}
 * @param <O>
 * 		opposite reference type; indicates what other type to connect to
 * @author Simon Fischer, Jan Czogalla
 * @see Ports
 */
public interface Port<S extends Port<S, O>, O extends Port<O, S>> extends Observable<Port<S, O>>, PortBase {

	int CLEAR_META_DATA_ERRORS = 1 << 0;
	int CLEAR_METADATA = 1 << 1;
	int CLEAR_DATA = 1 << 2;
	int CLEAR_SIMPLE_ERRORS = 1 << 3;
	int CLEAR_REAL_METADATA = 1 << 4;
	int CLEAR_ALL = CLEAR_META_DATA_ERRORS | CLEAR_METADATA | CLEAR_DATA | CLEAR_SIMPLE_ERRORS
			| CLEAR_REAL_METADATA;

	/** Clears all error types. */
	int CLEAR_ALL_ERRORS = CLEAR_META_DATA_ERRORS | CLEAR_SIMPLE_ERRORS;

	/** Clears all meta data, real and inferred. */
	int CLEAR_ALL_METADATA = CLEAR_METADATA | CLEAR_REAL_METADATA;

	/** A human readable, unique (operator scope) name for the port. */
	String getName();

	/**
	 * Returns the meta data currently assigned to this port.
	 * 
	 * @deprecated use {@link #getMetaData(Class)} instead
	 */
	@Deprecated
	MetaData getMetaData();

	/**
	 * Returns the last object delivered to the connected {@link InputPort} or received from the connected {@link
	 * OutputPort}. Never throws an exception but instead returns {@code null} if there is no data.
	 *
	 * @return the data at the port or {@code null}
	 * @since 9.4
	 */
	default IOObject getRawData(){
		// default method for compatibility, overwritten by {@link AbstractPort}
		return getAnyDataOrNull();
	}

	/**
	 * Returns the last object delivered to the connected {@link InputPort} or received from the
	 * connected {@link OutputPort}. Never throws an exception. Converts {@link com.rapidminer.adaption.belt.IOTable}s
	 * to {@link com.rapidminer.example.ExampleSet}s.
	 *
	 * @deprecated since 9.4, use {@link #getRawData()} or {@link #getDataAsOrNull(Class)} instead.
	 */
	@Deprecated
	IOObject getAnyDataOrNull();

	/** Returns the set of ports to which this port belongs. */
	Ports<S> getPorts();

	/** Gets a three letter abbreviation of the port's name. */
	String getShortName();

	/** Returns a human readable description of the ports pre/ and postconditions. */
	String getDescription();

	/** Returns true if connected to another Port. */
	boolean isConnected();

	/**
	 * Connects to another port. The opposite must be of type {@code O} which should be different from this type.
	 *
	 * @param opposite
	 * 		the port to connect to
	 * @throws PortException
	 * 		if one of the ports is already connected or the ports are not in the same {@link ExecutionUnit connection context}
	 * @since 9.7
	 */
	void connectTo(O opposite) throws PortException;

	/**
	 * Checks whether this port can be connected to the specified other port. This can be used to make sure that a
	 * connection can only be established between one {@link InputPort} and one {@link OutputPort}.
	 *
	 * @param other
	 * 		the port to check
	 * @return {@code true} if the ports can be connected
	 * @since 9.7
	 */
	boolean canConnectTo(Port other);

	/**
	 * Disconnects the OutputPort from its InputPort. Note: As a side effect, disconnecting ports
	 * may trigger PortExtenders removing these ports. In order to avoid this behaviour,
	 * {@link #lock()} port first.
	 *
	 * Moved from {@link OutputPort} in 9.7
	 *
	 * @throws PortException
	 *             if not connected.
	 */
	void disconnect() throws PortException;

	/**
	 * If connected, locks the connected ports, disconnects them and returns the result of {@link #getOpposite()}.
	 *
	 * @return the (locked) opposite
	 * @throws PortException
	 * 		if this operator is not connected
	 */
	default O lockDisconnectAndGet() throws PortException {
		return disconnectAndGet(true);
	}

	/**
	 * If connected, may lock the connected ports, disconnects them and returns the result of {@link #getOpposite()}.
	 *
	 * @param lock
	 * 		whether to lock the port pair before disconnecting
	 * @return the (locked) opposite
	 * @throws PortException
	 * 		if this operator is not connected
	 */
	default O disconnectAndGet(boolean lock) throws PortException {
		if (!isConnected()) {
			throw new PortException(this, "Not connected");
		}
		O p = getOpposite();
		if (lock) {
			lock();
			p.lock();
		}
		disconnect();
		return p;
	}

	/**
	 * Returns the output port of this connection.
	 *
	 * Moved from {@link InputPort} in 9.7
	 */
	OutputPort getSource();

	/**
	 *  Returns the input port of this connection.
	 *
	 *  Moved from {@link OutputPort} in 9.7
	 */
	InputPort getDestination();

	/**
	 * Get the opposite connected port if connected
	 *
	 * @since 9.7
	 */
	O getOpposite();

	/** Report an error in the current process setup. */
	void addError(MetaDataError metaDataError);

	/**
	 * Clears data, meta data and errors at this port.
	 *
	 * @param clearFlags
	 *            disjunction of the CLEAR_XX constants.
	 */
	void clear(int clearFlags);

	/** Returns the string "OperatorName.PortName". */
	String getSpec();

	/**
	 * Indicates whether {@link ExecutionUnit#autoWire()} should simulate the pre RM 5.0 stack
	 * behaviour of {@link IOContainer}. Normally, ports should return true here. However, ports
	 * created by {@link PortPairExtender}s should return false here, since (most of the time) they
	 * only pass data through rather adding new IOObjects to the IOContainer. TODO: delete
	 *
	 * @deprecated The above reasoning turned out to be unnecessary if implementations in other
	 *             places are correct. We always simulate the stack now. This method is only called
	 *             from within {@link ExecutionUnit#autoWire(CompatibilityLevel, InputPorts,
	 *             LinkedList<OutputPort>)} to keep a reference, but it has no effect on the
	 *             auto-wiring process. We keep this method until the end of the alpha test phase of
	 *             Vega.
	 */
	@Deprecated
	boolean simulatesStack();

	/**
	 * Locks the port so port extenders do not remove the port if disconnected. unlocks it.
	 */
	void lock();

	/**
	 * @see #lock()
	 */
	void unlock();

	/**
	 * @see #lock()
	 */
	boolean isLocked();

	/** Releases of any hard reference to IOObjects held by this class. */
	void freeMemory();
}
