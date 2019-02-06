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

import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * A port extender automatically creates an additional port if ports are connected. It guarantees
 * that there is always exactly one unconnected port. A port extender works by adding itself as an
 * observer of a {@link Ports} object and creating or deleting ports as necessary on any update.
 * 
 * @author Simon Fischer
 * @see PortPairExtender
 * @see MultiPortPairExtender
 * @param <T>
 *            The type of port that is created.
 */
public class SinglePortExtender<T extends Port> implements PortExtender {

	private final Ports<T> ports;
	private final String name;

	/** The number of ports that are guaranteed to exist. */
	protected int minNumber = 0;

	private boolean isChanging = false;

	/**
	 * List of all Ports which are managed by this Class. Caution: to prevent failures in the GUI
	 * mind that this list must be synchronized with the Ports-field!
	 */
	protected final List<T> managedPorts = new LinkedList<>();

	private int runningId = 0;

	private final Observer<Port> observer = new Observer<Port>() {

		@Override
		public void update(Observable<Port> observable, Port arg) {
			updatePorts();
		}
	};

	/**
	 * 
	 * @param name
	 *            The name prefix for the generated ports. An underscore and a number will be added.
	 * @param ports
	 *            The port to which ports are added.
	 */
	public SinglePortExtender(String name, Ports<T> ports) {
		this.ports = ports;
		this.name = name;
		ports.registerPortExtender(this);
	}

	/**
	 * Deletes all unused (= not connected and not locked) ports, always keeping at least one free
	 * port available. <br/>
	 * Override only in special cases, e.g. when needing to control the number of ports depending on
	 * parameters or other properties of the operator.
	 */
	protected void updatePorts() {
		if (!isChanging) {
			isChanging = true;
			boolean first = true;
			T foundDisconnected = null;
			Iterator<T> i = managedPorts.iterator();
			while (i.hasNext()) {
				T port = i.next();
				if (!port.isConnected() && !port.isLocked()) {
					// we don't remove the first disconnected port.
					if (first) {
						foundDisconnected = port;
						first = false;
					} else {
						if (minNumber == 0) { // we don't remove if guaranteeing ports
							deletePort(port);
							i.remove();
						}
					}
				}
			}
			if ((foundDisconnected == null) || (managedPorts.size() < minNumber)) {
				do {
					managedPorts.add(createPort());
				} while (managedPorts.size() < minNumber);
			} else {
				if (minNumber == 0) {
					managedPorts.remove(foundDisconnected);
					managedPorts.add(foundDisconnected);
					ports.pushDown(foundDisconnected);
				}
			}
			fixNames();
			isChanging = false;
		}
	}

	protected void deletePort(T port) {
		if (port instanceof OutputPort) {
			if (port.isConnected()) {
				((OutputPort) port).disconnect();
			}
		}
		ports.removePort(port);
	}

	protected T createPort() {
		runningId++;
		T port = ports.createPort(name + " " + runningId);
		return port;
	}

	protected void fixNames() {
		runningId = 0;
		for (T port : managedPorts) {
			runningId++;
			ports.renamePort(port, name + "_tmp_" + runningId);
		}
		runningId = 0;
		for (T port : managedPorts) {
			runningId++;
			ports.renamePort(port, name + " " + runningId);
		}

	}

	/** Creates an initial port and starts to listen. */
	public void start() {
		managedPorts.add(createPort());
		fixNames();
		ports.addObserver(observer, false);
	}

	/** Returns an unmodifiable view of the ports created by this port extender. */
	public List<T> getManagedPorts() {
		return Collections.unmodifiableList(managedPorts);
	}

	@Override
	public String getNamePrefix() {
		return name + " ";
	}

	@Override
	public void ensureMinimumNumberOfPorts(int minNumber) {
		this.minNumber = minNumber;
		updatePorts();
	}

	protected Ports<T> getPorts() {
		return ports;
	}
}
