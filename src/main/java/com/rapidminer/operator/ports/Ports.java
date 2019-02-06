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
	public String[] getPortNames();

	/** Returns the number of ports. */
	public int getNumberOfPorts();

	/**
	 * Should be used in apply method to retrieve desired ports. name should be a constant defined
	 * in the operator. If a port with the given name does not exist, but a {@link PortExtender}
	 * with a suitable prefix is registered, ports will be created accordingly. In this case, the
	 * user must call {@link #unlockPortExtenders()} to guarantee that ports can be removed again.
	 */
	public T getPortByName(String name);

	/** Should only be used by GUI. */
	public T getPortByIndex(int index);

	/** Returns an immutable view of the ports. */
	public List<T> getAllPorts();

	/**
	 * Add a port and notify the {@link Observer}s.
	 * 
	 * @throws PortException
	 *             if the name is already used.
	 */
	public void addPort(T port) throws PortException;

	/**
	 * Remove a port and notify the {@link Observer}s.
	 * 
	 * @throws PortException
	 *             if port is not registered with this Ports instance.
	 */
	public void removePort(T port) throws PortException;

	/** Removes all ports. */
	public void removeAll() throws PortException;

	/** Returns true if this port is contained within this Ports object. */
	public boolean containsPort(T port);

	/** Returns a textual description of the meta data currently assigned to these ports. */
	public String getMetaDataDescription();

	/** Returns the operator and process to which these ports are attached. */
	public PortOwner getOwner();

	/** Creates a new port and adds it to these Ports. */
	public T createPort(String name);

	/** Creates (and adds) a new port whose {@link Port#simulatesStack()} returns false. */
	public T createPassThroughPort(String name);

	/** Creates a new port and adds it to these Ports if add is true.. */
	public T createPort(String name, boolean add);

	/** Renames the given port. */
	public void renamePort(T port, String newName);

	/**
	 * Clears the input, meta data, and error messages.
	 * 
	 * @see Ports#clear(int)
	 */
	public void clear(int clearFlags);

	/**
	 * This is a backport method to generate IOContainers containing all output objects of the given
	 * ports.
	 */
	public IOContainer createIOContainer(boolean onlyConnected);

	/**
	 * This is a backport method to generate IOContainers containing all output objects of the given
	 * ports.
	 */
	public IOContainer createIOContainer(boolean onlyConnected, boolean omitNullResults);

	/** Re-adds this port as the last port in this collection. */
	public void pushDown(T port);

	/** Disconnects all ports. */
	public void disconnectAll();

	/**
	 * Disconnects all ports with exception to those connections to operators in the given list.
	 */
	public void disconnectAllBut(List<Operator> exception);

	/** Registers a port extender with this ports. */
	public void registerPortExtender(PortExtender extender);

	/** While parsing the process XML file, we may have called loading */
	public void unlockPortExtenders();

	/** Frees memory occupied by references to ioobjects. */
	public void freeMemory();

	/** Returns the number of ports in these Ports that are actually connected. */
	int getNumberOfConnectedPorts();

}
