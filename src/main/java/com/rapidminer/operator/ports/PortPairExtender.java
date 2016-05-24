/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * This class observes a set of input and output ports and adds additional ports as needed.
 * Operators probably want to connect these ports by a
 * {@link com.rapidminer.operator.ports.metadata.ManyToManyPassThroughRule}. It guarantees that
 * there is always exactly one pair of in and output pairs which is not connected.
 * 
 * @see PortPairExtender
 * @see MultiPortPairExtender
 * 
 * @author Simon Fischer
 */
public class PortPairExtender implements PortExtender {

	private final String name;
	private final InputPorts inPorts;
	private final OutputPorts outPorts;
	private final List<PortPair> managedPairs = new LinkedList<PortPair>();

	/** If non null, add this meta data as a SimplePrecondition to each generated input port. */
	private final MetaData preconditionMetaData;

	private boolean isChanging = false;

	private int runningId = 0;

	private final Observer<Port> observer = new Observer<Port>() {

		@Override
		public void update(Observable<Port> observable, Port arg) {
			updatePorts();
		}
	};

	private int minNumber = 0;

	/** A pair of ports managed by a PortPairExtender. */
	public static class PortPair {

		private final InputPort inputPort;
		private final OutputPort outputPort;

		private PortPair(InputPort inputPort, OutputPort outputPort) {
			this.inputPort = inputPort;
			this.outputPort = outputPort;
		}

		public InputPort getInputPort() {
			return inputPort;
		}

		public OutputPort getOutputPort() {
			return outputPort;
		}
	}

	public PortPairExtender(String name, InputPorts inPorts, OutputPorts outPorts) {
		this(name, inPorts, outPorts, null);
	}

	/**
	 * Creates a new port pair extender
	 * 
	 * @param name
	 *            The name prefix for all generated ports.
	 * @param inPorts
	 *            Add generated input ports to these InputPorts
	 * @param outPorts
	 *            Add generated output ports to these OutputPorts
	 * @param preconditionMetaData
	 *            If non-null, create a SimplePrecondition for each newly generated input port.
	 */
	public PortPairExtender(String name, InputPorts inPorts, OutputPorts outPorts, MetaData preconditionMetaData) {
		this.name = name;
		this.inPorts = inPorts;
		this.outPorts = outPorts;
		this.preconditionMetaData = preconditionMetaData;
		inPorts.registerPortExtender(this);
		outPorts.registerPortExtender(this);
	}

	private void updatePorts() {
		if (!isChanging) {
			isChanging = true;
			boolean first = true;
			PortPair foundDisconnected = null;
			Iterator<PortPair> i = managedPairs.iterator();
			while (i.hasNext()) {
				PortPair pair = i.next();
				if (!pair.inputPort.isConnected() && !pair.inputPort.isLocked() && !pair.outputPort.isConnected()
						&& !pair.outputPort.isLocked()) {
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
					inPorts.pushDown(foundDisconnected.getInputPort());
					outPorts.pushDown(foundDisconnected.getOutputPort());
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
		inPorts.addObserver(observer, false);
		outPorts.addObserver(observer, false);
	}

	private PortPair createPort() {
		runningId++;
		InputPort in = inPorts.createPassThroughPort(name + " " + runningId);
		if (preconditionMetaData != null) {
			in.addPrecondition(new SimplePrecondition(in, preconditionMetaData, false));
		}
		OutputPort out = outPorts.createPassThroughPort(name + " " + runningId);
		return new PortPair(in, out);
	}

	private void deletePorts(PortPair pair) {
		if (pair.outputPort.isConnected()) {
			pair.outputPort.disconnect();
		}
		inPorts.removePort(pair.inputPort);
		outPorts.removePort(pair.outputPort);
	}

	private void fixNames() {
		runningId = 0;
		for (PortPair pair : managedPairs) {
			runningId++;
			inPorts.renamePort(pair.inputPort, name + "_tmp_" + runningId);
			outPorts.renamePort(pair.outputPort, name + "_tmp_" + runningId);
		}
		runningId = 0;
		for (PortPair pair : managedPairs) {
			runningId++;
			inPorts.renamePort(pair.inputPort, name + " " + runningId);
			outPorts.renamePort(pair.outputPort, name + " " + runningId);
		}
	}

	/**
	 * The generated rule copies all meta data from the generated input ports to all generated
	 * output ports.
	 */
	public MDTransformationRule makePassThroughRule() {
		return new MDTransformationRule() {

			@Override
			public void transformMD() {
				for (PortPair pair : managedPairs) {
					MetaData inData = pair.inputPort.getMetaData();
					if (inData != null) {
						inData = transformMetaData(inData.clone());
						inData.addToHistory(pair.getOutputPort());
						pair.outputPort.deliverMD(inData);
					} else {
						pair.outputPort.deliverMD(null);
					}
				}
			}
		};
	}

	protected MetaData transformMetaData(MetaData md) {
		return md;
	}

	/** Passes the actual data from the output ports to their connected input ports. */
	public void passDataThrough() {
		for (PortPair pair : managedPairs) {
			IOObject data = pair.inputPort.getAnyDataOrNull();
			pair.outputPort.deliver(data);
		}
	}

	/** Does the same as {@link #passDataThrough()} but copies the IOObjects. */
	public void passCloneThrough() {
		for (PortPair pair : managedPairs) {
			IOObject data = pair.inputPort.getAnyDataOrNull();
			if (data != null) {
				pair.outputPort.deliver(data.copy());
			} else {
				pair.outputPort.deliver(null);
			}
		}
	}

	/** Returns an unmodifiable view of all port pairs managed by this port extender. */
	public List<PortPair> getManagedPairs() {
		return Collections.unmodifiableList(managedPairs);
	}

	/**
	 * Returns a list of all non-null data delivered to the input ports created by this port
	 * extender.
	 * 
	 * @throws UserError
	 * @deprecated use {@link #getData(Class))}
	 */
	@Deprecated
	public <T extends IOObject> List<T> getData() throws UserError {
		List<T> results = new LinkedList<T>();
		for (PortPair pair : managedPairs) {
			T data = pair.inputPort.<T> getDataOrNull();
			if (data != null) {
				results.add(data);
			}
		}
		return results;
	}

	public <T extends IOObject> List<T> getData(Class<T> desiredClass) throws UserError {
		List<T> results = new LinkedList<T>();
		for (PortPair pair : managedPairs) {
			T data = pair.inputPort.<T> getDataOrNull(desiredClass);
			if (data != null) {
				results.add(data);
			}
		}
		return results;
	}

	/**
	 * Returns a list of all non-null data delivered to the input ports created by this port
	 * extender.
	 * 
	 * @throws UserError
	 * @deprecated use {@link #getOutputData(Class)}
	 */
	@Deprecated
	public <T extends IOObject> List<T> getOutputData() throws UserError {
		List<T> results = new LinkedList<T>();
		for (PortPair pair : managedPairs) {
			T data = pair.outputPort.<T> getDataOrNull();
			if (data != null) {
				results.add(data);
			}
		}
		return results;
	}

	public <T extends IOObject> List<T> getOutputData(Class<T> desiredClass) throws UserError {
		List<T> results = new LinkedList<T>();
		for (PortPair pair : managedPairs) {
			T data = pair.outputPort.<T> getDataOrNull(desiredClass);
			if (data != null) {
				results.add(data);
			}
		}
		return results;
	}

	/**
	 * This method is a convenient method for delivering several IOObjects. But keep in mind that
	 * you cannot deliver more IObjects than you received first hand. First objects in list will be
	 * delivered on the first port. If input ports are not connected or got not delivered an objects
	 * unequal null, the corresponding output port is skipped.
	 */
	public void deliver(List<? extends IOObject> ioObjectList) {
		Iterator<PortPair> portIterator = getManagedPairs().iterator();
		for (IOObject object : ioObjectList) {
			PortPair pair = portIterator.next();
			IOObject data = pair.inputPort.getAnyDataOrNull();
			while (data == null) {
				data = portIterator.next().inputPort.getAnyDataOrNull();
			}
			pair.outputPort.deliver(object);
		}

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
