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

import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.Observable;

import java.util.List;
import java.util.Observer;


/**
 * A collection of either input or output {@link Port}s (as defined by generic class T). <br/>
 * Instances of this class serve as a factory and register of ports. Ports can be accessed by names
 * or index. Actual behaviour is defined by specialized interfaces {@link InputPorts} and
 * {@link OutputPorts}. <br/>
 * Implementors of operators are encouraged to generate their ports at construction time and keep
 * private references to them, rather than retrieving them via one of the get methods every time
 * they are needed. The GUI will probably use the getter-methods. <br/>
 * 
 * Instances of this class can be observed by the {@link Observer} pattern.
 * 
 * @author Simon Fischer
 * */
public interface Ports<T extends Port> extends Observable<Port> {

	/** Returns all input port names registered with these ports. */
	String[] getPortNames();

	/** Returns the number of ports. */
	int getNumberOfPorts();

	/**
	 * Should be used in apply method to retrieve desired ports. name should be a constant defined
	 * in the operator. If a port with the given name does not exist, but a {@link PortExtender}
	 * with a suitable prefix is registered, ports will be created accordingly. In this case, the
	 * user must call {@link #unlockPortExtenders()} to guarantee that ports can be removed again.
	 */
	T getPortByName(String name);

	/** Should only be used by GUI. */
	T getPortByIndex(int index);

	/** Returns an immutable view of the ports. */
	List<T> getAllPorts();

	/**
	 * Add a port and notify the {@link Observer}s.
	 * 
	 * @throws PortException
	 *             if the name is already used.
	 */
	void addPort(T port) throws PortException;

	/**
	 * Remove a port and notify the {@link Observer}s.
	 * 
	 * @throws PortException
	 *             if port is not registered with this Ports instance.
	 */
	void removePort(T port) throws PortException;

	/** Removes all ports. */
	void removeAll() throws PortException;

	/** Returns true if this port is contained within this Ports object. */
	boolean containsPort(T port);

	/** Returns a textual description of the meta data currently assigned to these ports. */
	String getMetaDataDescription();

	/** Returns the operator and process to which these ports are attached. */
	PortOwner getOwner();

	/** Creates a new port and adds it to these Ports. */
	T createPort(String name);

	/** Creates (and adds) a new port whose {@link Port#simulatesStack()} returns false. */
	T createPassThroughPort(String name);

	/** Creates a new port and adds it to these Ports if add is true.. */
	T createPort(String name, boolean add);

	/** Renames the given port. */
	void renamePort(T port, String newName);

	/**
	 * Clears the input, meta data, and error messages.
	 * 
	 * @see Ports#clear(int)
	 */
	void clear(int clearFlags);

	/**
	 * This is a backport method to generate IOContainers containing all output objects of the given
	 * ports.
	 */
	IOContainer createIOContainer(boolean onlyConnected);

	/**
	 * This is a backport method to generate IOContainers containing all output objects of the given
	 * ports.
	 */
	IOContainer createIOContainer(boolean onlyConnected, boolean omitNullResults);

	/** Re-adds this port as the last port in this collection. */
	void pushDown(T port);

	/** Disconnects all ports. */
	void disconnectAll();

	/**
	 * Disconnects all ports with exception to those connections to operators in the given list.
	 */
	void disconnectAllBut(List<Operator> exception);

	/** Registers a port extender with this ports. */
	void registerPortExtender(PortExtender extender);

	/** While parsing the process XML file, we may have called loading */
	void unlockPortExtenders();

	/** Frees memory occupied by references to ioobjects. */
	void freeMemory();

	/** Returns the number of ports in these Ports that are actually connected. */
	int getNumberOfConnectedPorts();

}
