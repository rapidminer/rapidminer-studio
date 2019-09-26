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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.PortException;
import com.rapidminer.operator.ports.PortExtender;
import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;


/**
 * @author Simon Fischer
 */
public abstract class AbstractPorts<T extends Port> extends AbstractObservable<Port> implements Ports<T> {

	private final List<T> portList = Collections.synchronizedList(new ArrayList<>());
	private final Map<String, T> portMap = new HashMap<>();
	private String[] portNames;
	private boolean portNamesValid = false;
	private final PortOwner owner;

	private final Observer<Port> delegatingObserver = new Observer<Port>() {

		@Override
		public void update(Observable<Port> observable, Port arg) {
			fireUpdate(arg);
		}
	};

	public AbstractPorts(PortOwner owner) {
		this.owner = owner;
		portNamesValid = false;
	}

	private void updatePortNames() {
		if (!portNamesValid) {
			portNames = new String[portList.size()];
			int i = 0;
			synchronized (portList) {
				for (Port port : portList) {
					portNames[i++] = port.getName();
				}
			}
			portNamesValid = true;
		}
	}

	@Override
	public void addPort(T port) throws PortException {
		if (portMap.containsKey(port.getName())) {
			throw new PortException("Port name already used: " + port.getName());
		}
		assert port.getPorts() == this;
		portList.add(port);
		portMap.put(port.getName(), port);
		portNamesValid = false;
		port.addObserver(delegatingObserver, false);
		fireUpdate(port);
	}

	@Override
	public void removePort(T port) throws PortException {
		if (!portList.contains(port) || port.getPorts() != this) {
			throw new PortException("Cannot remove " + port + ".");
		} else {
			if (port.isConnected()) {
				if (port instanceof OutputPort) {
					((OutputPort) port).disconnect();
				} else {
					((InputPort) port).getSource().disconnect();
				}
			}
			portList.remove(port);
			portMap.remove(port.getName());
			port.removeObserver(delegatingObserver);
			fireUpdate();
		}
	}

	@Override
	public void removeAll() {
		// don't iterate to avoid concurrent modification
		while (getNumberOfPorts() != 0) {
			removePort(getPortByIndex(0));
		}
	}

	@Override
	public int getNumberOfPorts() {
		return portList.size();
	}

	@Override
	public T getPortByIndex(int index) {
		return portList.get(index);
	}

	@Override
	public T getPortByName(String name) {
		T port = portMap.get(name);
		if (port != null) {
			return port;
		} else {
			// LogService.getRoot().fine("Port '"+name+"' does not exist. Checking for extenders.");
			LogService.getRoot().log(Level.FINE, "com.rapidminer.operator.ports.impl.AbstractPorts.port_does_not_exist",
					name);
			if (portExtenders != null) {
				for (PortExtender extender : portExtenders) {
					String prefix = extender.getNamePrefix();
					if (name.startsWith(prefix)) {
						// LogService.getRoot().fine("Found extender with prefix '"+prefix+"'.
						// Trying to extend.");
						LogService.getRoot().log(Level.FINE,
								"com.rapidminer.operator.ports.impl.AbstractPorts.found_extender", prefix);
						try {
							int index = Integer.parseInt(name.substring(prefix.length()));
							extender.ensureMinimumNumberOfPorts(index); // numbering starts at 1
							T secondTry = portMap.get(name);
							if (secondTry == null) {
								// LogService.getRoot().warning("Port extender "+prefix+" did not
								// extend to size "+index+".");
								LogService.getRoot().log(Level.WARNING,
										"com.rapidminer.operator.ports.impl.AbstractPorts.port_extender_did_not_extend",
										new Object[] { prefix, index });
							} else {
								// LogService.getRoot().fine("Port was created. Ports are now:
								// "+getAllPorts());
								LogService.getRoot().log(Level.FINE,
										"com.rapidminer.operator.ports.impl.AbstractPorts.ports_created", getAllPorts());
							}
							return secondTry;
						} catch (NumberFormatException e) {
							// LogService.getRoot().log(Level.WARNING,
							// "Cannot extend "+prefix+": "+e, e);
							LogService.getRoot().log(Level.WARNING,
									I18N.getMessage(LogService.getRoot().getResourceBundle(),
											"com.rapidminer.operator.ports.impl.AbstractPorts.extending_error", prefix, e),
									e);
							return null;
						}
					}
				}
			}
			// no extender found
			return null;
		}
	}

	@Override
	public String[] getPortNames() {
		updatePortNames();
		return portNames;
	}

	@Override
	public List<T> getAllPorts() {
		synchronized (portList) {
			return Collections.unmodifiableList(new ArrayList<>(portList));
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public String getMetaDataDescription() {
		StringBuilder b = new StringBuilder();
		synchronized (portList) {
			for (Port port : portList) {
				b.append(port.getName());
				b.append(": ");
				b.append(port.getMetaData());
				b.append("; ");
			}
		}
		return b.toString();
	}

	@Override
	public PortOwner getOwner() {
		return owner;
	}

	@Override
	public boolean containsPort(T port) {
		return portList.contains(port);
	}

	@Override
	public void renamePort(T port, String newName) {
		if (portMap.containsKey(newName)) {
			throw new PortException("Port name already used: " + port.getName());
		}
		portMap.remove(port.getName());
		((AbstractPort) port).setName(newName);
		portMap.put(newName, port);
	}

	@Override
	public void clear(int clearFlags) {
		for (T port : getAllPorts()) {
			port.clear(clearFlags);
		}
	}

	@Override
	public IOContainer createIOContainer(boolean onlyConnected, boolean omitEmptyResults) {
		Collection<IOObject> output = new LinkedList<>();
		for (Port port : getAllPorts()) {
			if (!onlyConnected || port.isConnected()) {
				IOObject data = port.getRawData();
				if (omitEmptyResults) {
					if (data != null) {
						output.add(data);
					}
				} else {
					output.add(data);
				}
			}
		}
		return new IOContainer(output);
	}

	@Override
	public IOContainer createIOContainer(boolean onlyConnected) {
		return createIOContainer(onlyConnected, true);
	}

	@Override
	public void pushDown(T port) {
		if (!portList.contains(port)) {
			throw new PortException("Cannot push down " + port.getName() + ": port does not belong to " + this);
		}
		portList.remove(port);
		portList.add(port);
	}

	private List<PortExtender> portExtenders;

	@Override
	public void registerPortExtender(PortExtender extender) {
		if (portExtenders == null) {
			portExtenders = new LinkedList<>();
		}
		portExtenders.add(extender);
	}

	@Override
	public void unlockPortExtenders() {
		if (portExtenders != null) {
			for (PortExtender extender : portExtenders) {
				extender.ensureMinimumNumberOfPorts(0);
			}
		}
	}

	@Override
	public void freeMemory() {
		for (Port inputPort : getAllPorts()) {
			inputPort.freeMemory();
		}
	}

	@Override
	public int getNumberOfConnectedPorts() {
		int count = 0;
		for (Port port : getAllPorts()) {
			if (port.isConnected()) {
				count++;
			}
		}
		return count;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		boolean first = true;
		for (String port : getPortNames()) {
			if (first) {
				first = false;
			} else {
				b.append(", ");
			}
			b.append(port);
		}
		return b.toString();
	}
}
