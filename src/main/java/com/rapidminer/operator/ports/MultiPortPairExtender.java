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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * This class observes a set of input and output ports and adds additional ports as needed to
 * several {@link Ports} collections. This behaviour is needed, e.g., for the ProcessBranch
 * operator. Here, we have an arbitrary number of input ports each of which is linked to two
 * subprocesses ("if" and "else").
 * 
 * @see SinglePortExtender
 * @see PortPairExtender
 * 
 * @author Simon Fischer TODO: Unchecked?
 */
@SuppressWarnings("unchecked")
public class MultiPortPairExtender<S extends Port, M extends Port> implements PortExtender {

	private final String name;
	private final Ports<S> singlePorts;
	private final ArrayList<Ports<M>> multiPortsList;
	private int minNumber = 0;

	private final List<MultiPortPair> managedPairs = new LinkedList<MultiPortPair>();

	private boolean isChanging = false;

	private int runningId = 0;

	private final Observer<Port> observer = new Observer<Port>() {

		@Override
		public void update(Observable<Port> observable, Port arg) {
			updatePorts();
		}
	};

	protected class MultiPortPair {

		protected S singlePort;
		protected ArrayList<M> multiPorts;

		public MultiPortPair(S singlePort, ArrayList<M> multiPorts) {
			this.singlePort = singlePort;
			this.multiPorts = multiPorts;
		}

		public boolean isDisconnected() {
			if (singlePort.isConnected() || singlePort.isLocked()) {
				return false;
			}
			for (Port port : multiPorts) {
				if (port.isConnected() || port.isLocked()) {
					return false;
				}
			}
			return true;
		}
	}

	public MultiPortPairExtender(String name, Ports<S> singlePorts, Ports<M>[] multiPortsList) {
		this.name = name;
		this.singlePorts = singlePorts;
		this.multiPortsList = new ArrayList<Ports<M>>(Arrays.asList(multiPortsList));
		singlePorts.registerPortExtender(this);
		for (Ports<M> ports : multiPortsList) {
			ports.registerPortExtender(this);
		}
	}

	public void addMultiPorts(Ports<M> multiPorts, int index) {
		multiPortsList.add(index, multiPorts);
		int id = 1;
		for (MultiPortPair pair : managedPairs) {
			pair.multiPorts.add(index, multiPorts.createPassThroughPort(name + " " + id));
			id++;
		}
		multiPorts.addObserver(observer, false);
		updatePorts();
	}

	public void removeMultiPorts(int index) {
		Ports<M> oldPorts = multiPortsList.remove(index);
		oldPorts.removeObserver(observer);
		for (MultiPortPair pair : managedPairs) {
			// M oldPort =
			pair.multiPorts.remove(index);
			// oldPorts.removePort(oldPort);
		}
		updatePorts();
	}

	private void updatePorts() {
		if (!isChanging) {
			isChanging = true;
			boolean first = true;
			MultiPortPair foundDisconnected = null;
			Iterator<MultiPortPair> i = managedPairs.iterator();
			while (i.hasNext()) {
				MultiPortPair pair = i.next();
				if (pair.isDisconnected()) {
					// we don't remove the first disconnected port.
					if (first) {
						first = false;
						foundDisconnected = pair;
					} else {
						if (minNumber == 0) {
							deletePorts(pair);
							i.remove();
						}
					}
				}
			}
			if ((foundDisconnected == null) || (managedPairs.size() < minNumber)) {
				do {
					managedPairs.add(createPort());
				} while (managedPairs.size() < minNumber);
			} else {
				if (minNumber == 0) {
					managedPairs.remove(foundDisconnected);
					managedPairs.add(foundDisconnected);
					singlePorts.pushDown(foundDisconnected.singlePort);
					for (int j = 0; j < multiPortsList.size(); j++) {
						multiPortsList.get(j).pushDown(foundDisconnected.multiPorts.get(j));
					}
				}
			}
			fixNames();
			isChanging = false;
		}
	}

	/** Creates an initial port and starts to listen. */
	public void start() {
		managedPairs.add(createPort());
		fixNames();
		singlePorts.addObserver(observer, false);
		for (Ports<M> ports : multiPortsList) {
			ports.addObserver(observer, false);
		}
	}

	private MultiPortPair createPort() {
		runningId++;
		S singlePort = singlePorts.createPassThroughPort(name + " " + runningId);
		ArrayList<M> newMultiPorts = new ArrayList<M>(multiPortsList.size());

		for (Ports<M> ports : multiPortsList) {
			M out = ports.createPassThroughPort(name + " " + runningId);
			newMultiPorts.add(out);
		}
		return new MultiPortPair(singlePort, newMultiPorts);
	}

	private void deletePorts(MultiPortPair pair) {
		singlePorts.removePort(pair.singlePort);
		for (Port multiPort : pair.multiPorts) {
			if (multiPort instanceof OutputPort) {
				if (multiPort.isConnected()) {
					((OutputPort) multiPort).disconnect();
				}
			}
			((Ports<M>) multiPort.getPorts()).removePort((M) multiPort);
		}
	}

	private void fixNames() {
		runningId = 0;
		for (MultiPortPair pair : managedPairs) {
			runningId++;
			singlePorts.renamePort(pair.singlePort, name + "_tmp_" + runningId);
			for (Port port : pair.multiPorts) {
				((Ports<M>) port.getPorts()).renamePort((M) port, name + "_tmp_" + runningId);
			}
		}
		runningId = 0;
		for (MultiPortPair pair : managedPairs) {
			runningId++;
			singlePorts.renamePort(pair.singlePort, name + " " + runningId);
			for (Port port : pair.multiPorts) {
				((Ports<M>) port.getPorts()).renamePort((M) port, name + " " + runningId);
			}
		}
	}

	protected List<MultiPortPair> getManagedPairs() {
		return managedPairs;
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
}
