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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.adaption.belt.AtPortConverter;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.CollectionPrecondition;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;


/**
 * This class observes a set of input and output ports and adds additional ports as needed.
 * Operators probably want to connect these ports by a
 * {@link com.rapidminer.operator.ports.metadata.ManyToManyPassThroughRule}. It guarantees that
 * there is always exactly one pair of in and output pairs which is not connected.
 * <p>
 * Via the different constructors input ports can be customized to accept any input,
 * input of a certain class or multiple inputs of a certain class contained inside of an {@link IOObjectCollection}.
 * <p>
 * In case of multiple inputs per port please use the method {@link #getData(Class, boolean)} to retrieve the data.
 *
 * @see PortPairExtender
 * @see MultiPortPairExtender
 *
 * @author Simon Fischer
 */
public class PortPairExtender implements PortExtender {

	/**
	 * Name prefix for the input ports (and output ports if no output port name has been specified)
	 */
	private final String name;
	/**
	 * Name prefix for the output ports
	 */
	private final String outName;

	private final InputPorts inPorts;
	private final OutputPorts outPorts;
	private final List<PortPair> managedPairs = new LinkedList<>();

	/**
	 * If not {@code null}, add this meta data as a SimplePrecondition or {@link CollectionPrecondition} to each generated input
	 * port.
	 */
	private final MetaData preconditionMetaData;
	/**
	 * If {@code true}, the PortPairExtender will use a {@link CollectionPrecondition} instead of a {@link SimplePrecondition}.
	 */
	private boolean allowCollection;

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
	 * Creates a new port pair extender.
	 *
	 * @param name
	 *            The name prefix for all generated ports. Must not be {@code null}.
	 * @param inPorts
	 *            Add generated input ports to these InputPorts. Must not be {@code null}.
	 * @param outPorts
	 *            Add generated output ports to these OutputPorts. Must not be {@code null}.
	 * @param preconditionMetaData
	 *            If not {@code null}, creates a {@link SimplePrecondition} or {@link CollectionPrecondition} for each
	 *            newly generated input port. If {@code null}, no preconditions will be added to the ports.
	 */
	public PortPairExtender(String name, InputPorts inPorts, OutputPorts outPorts, MetaData preconditionMetaData) {
		this(name, inPorts, outPorts, preconditionMetaData, false);
	}

	/**
	 * Creates a new port pair extender.
	 *
	 * @param name
	 * 		The name prefix for all generated ports. Must not be {@code null}.
	 * @param inPorts
	 * 		Add generated input ports to these InputPorts. Must not be {@code null}.
	 * @param outPorts
	 * 		Add generated output ports to these OutputPorts. Must not be {@code null}.
	 * @param preconditionMetaData
	 * 		If not {@code null}, creates a {@link SimplePrecondition} or {@link CollectionPrecondition} for each newly
	 * 		generated input port. If {@code null}, no preconditions will be added to the ports.
	 * @param allowCollection
	 * 		If {@code true} a CollectionPrecondition a {@link CollectionPrecondition} is used for each newly generated port
	 * 		to optionally allow {@link IOObjectCollection} as input.
	 */
	public PortPairExtender(String name, InputPorts inPorts, OutputPorts outPorts, MetaData preconditionMetaData, boolean allowCollection) {
		this(name, null, inPorts, outPorts, preconditionMetaData, allowCollection);
	}

	/**
	 * Creates a new port pair extender.
	 *
	 * @param name
	 * 		The name prefix for all the input ports. Must not be {@code null}.
	 * @param outName
	 * 		The name prefix for all the ouput ports. If {@code null}, the {@code name} parameter will be reused instead.
	 * @param inPorts
	 * 		Add generated input ports to these InputPorts. Must not be {@code null}.
	 * @param outPorts
	 * 		Add generated output ports to these OutputPorts. Must not be {@code null}.
	 * @param preconditionMetaData
	 * 		If not {@code null}, creates a {@link SimplePrecondition} or {@link CollectionPrecondition} for each newly
	 * 		generated input port. If {@code null}, no preconditions will be added to the ports.
	 * @param allowCollection
	 * 		If {@code true} a {@link CollectionPrecondition} is used for each newly generated
	 * 		port to optionally allow {@link IOObjectCollection} as input.
	 */
	public PortPairExtender(String name, String outName, InputPorts inPorts, OutputPorts outPorts,
							MetaData preconditionMetaData, boolean allowCollection) {
		this.allowCollection = allowCollection;
		this.name = name;
		this.outName = outName != null ? outName : name;
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
			if (foundDisconnected == null || managedPairs.size() < minNumber) {
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
			SimplePrecondition sp = new SimplePrecondition(in, preconditionMetaData, false);
			if (allowCollection) {
				in.addPrecondition(new CollectionPrecondition(sp));
			} else {
				in.addPrecondition(sp);
			}
		}
		OutputPort out = outPorts.createPassThroughPort(outName + " " + runningId);
		return new PortPair(in, out);
	}

	private void deletePorts(PortPair pair) {
		if (pair.outputPort.isConnected()) {
			pair.outputPort.disconnect();
		}
		inPorts.removePort(pair.inputPort);
		outPorts.removePort(pair.outputPort);
	}

	protected void fixNames() {
		runningId = 0;
		for (PortPair pair : managedPairs) {
			runningId++;
			inPorts.renamePort(pair.inputPort, name + "_tmp_" + runningId);
			outPorts.renamePort(pair.outputPort, outName + "_tmp_" + runningId);
		}
		runningId = 0;
		for (PortPair pair : managedPairs) {
			runningId++;
			inPorts.renamePort(pair.inputPort, name + " " + runningId);
			outPorts.renamePort(pair.outputPort, outName + " " + runningId);
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
			IOObject data = pair.inputPort.getRawData();
			pair.outputPort.deliver(data);
		}
	}

	/** Does the same as {@link #passDataThrough()} but copies the IOObjects. */
	public void passCloneThrough() {
		for (PortPair pair : managedPairs) {
			IOObject data = pair.inputPort.getRawData();
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
	 * Returns a list of all non-{@code null} data delivered to the input ports created by this port
	 * extender.
	 *
	 * @throws UserError
	 * @deprecated use {@link #getData(Class))}
	 */
	@Deprecated
	public <T extends IOObject> List<T> getData() throws UserError {
		List<T> results = new LinkedList<>();
		for (PortPair pair : managedPairs) {
			T data = pair.inputPort.<T> getDataOrNull();
			if (data != null) {
				results.add(data);
			}
		}
		return results;
	}

	/**
	 * Returns a list of all non-{@code null} data delivered to the input ports created by this port extender and casts
	 * the data to the desired class.
	 *
	 * @param desiredClass
	 * 		The class the data should be casted to.
	 * @return Non-{@code null} data delivered to the output ports created by this port extender. If there is nc data
	 * the List will be empty but never {@code null}.
	 * @throws UserError
	 * 		If data is not of the requested type.
	 */
	public <T extends IOObject> List<T> getData(Class<T> desiredClass) throws UserError {
		List<T> results = new LinkedList<>();
		for (PortPair pair : managedPairs) {
			T data = pair.inputPort.<T>getDataOrNull(desiredClass);
			if (data != null) {
				results.add(data);
			}
		}
		return results;
	}

	/**
	 * Returns a list of all non-{@code null} data delivered to the output ports created by this port
	 * extender.
	 *
	 * @throws UserError
	 * @deprecated use {@link #getOutputData(Class)}
	 */
	@Deprecated
	public <T extends IOObject> List<T> getOutputData() throws UserError {
		List<T> results = new LinkedList<>();
		for (PortPair pair : managedPairs) {
			T data = pair.outputPort.<T> getDataOrNull();
			if (data != null) {
				results.add(data);
			}
		}
		return results;
	}

	/**
	 * Returns a list of all non-{@code null} data delivered to the output ports created by this port extender and casts
	 * the data to the desired class.
	 *
	 * @param desiredClass
	 * 		The class the data should be casted to.
	 * @return Non-{@code null} data delivered to the output ports created by this port extender. If there is nc data
	 * the List will be empty but never {@code null}.
	 * @throws UserError
	 * 		If data is not of the requested type.
	 */
	public <T extends IOObject> List<T> getOutputData(Class<T> desiredClass) throws UserError {
		List<T> results = new LinkedList<>();
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
	 * unequal {@code null}, the corresponding output port is skipped.
	 */
	public void deliver(List<? extends IOObject> ioObjectList) {
		Iterator<PortPair> portIterator = getManagedPairs().iterator();
		for (IOObject object : ioObjectList) {
			PortPair pair = portIterator.next();
			IOObject data = pair.inputPort.getRawData();
			while (data == null) {
				data = portIterator.next().inputPort.getRawData();
			}
			pair.outputPort.deliver(object);
		}

	}

	@Override
	public String getNamePrefix() {
		return name + " ";
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * ATTENTION: Make sure to call <strong>after</strong> {@link #start()}, otherwise you will end
	 * up with n+1 ports!
	 * </p>
	 */
	@Override
	public void ensureMinimumNumberOfPorts(int minNumber) {
		this.minNumber = minNumber;
		updatePorts();
	}

	/**
	 * Returns a list of non-{@code null} data of all input ports.
	 *
	 * @param unfold
	 *            If {@code true}, collections are added as individual objects rather than as a collection.
	 *            The unfolding is done recursively.
	 * @throws UserError
	 */
	@SuppressWarnings("unchecked")
	public <T extends IOObject> List<T> getData(Class<T> desiredClass, boolean unfold) throws UserError {
		List<T> results = new ArrayList<>();
		for (PortPair port : managedPairs) {
			IOObject data = port.getInputPort().getRawData();
			if (data != null) {
				if (unfold && data instanceof IOObjectCollection) {
					unfold((IOObjectCollection<?>) data, results, desiredClass, port);
				} else {
					addSingle(results, data, desiredClass, port);
				}
			}
		}
		return results;
	}

	/**
	 * Unfolds the given IOObjectCollection recursively.
	 *
	 * @param desiredClass
	 * 		method will throw unless all non-collection children are of type desired class
	 * @param port
	 * 		Used for error message only
	 */
	@SuppressWarnings("unchecked")
	private <T extends IOObject> void unfold(IOObjectCollection<?> collection, List<T> results, Class<T> desiredClass,
											 PortPair port) throws UserError {
		for (IOObject obj : collection.getObjects()) {
			if (obj instanceof IOObjectCollection) {
				unfold((IOObjectCollection<?>) obj, results, desiredClass, port);
			} else {
				addSingle(results, obj, desiredClass, port);
			}
		}
	}

	/**
	 * Adds the data to the results list if it is of the desired class or convertible to it. Throws an user error
	 * otherwise.
	 */
	private <T extends IOObject> void addSingle(List<T> results, IOObject data, Class<T> desiredClass, PortPair port)
			throws UserError {
		if (desiredClass.isInstance(data)) {
			results.add(desiredClass.cast(data));
		} else if (AtPortConverter.isConvertible(data.getClass(), desiredClass)) {
			try {
				results.add(desiredClass.cast(AtPortConverter.convert(data, port.getInputPort())));
			} catch (BeltConverter.ConversionException e) {
				throw new UserError(inPorts.getOwner().getOperator(), "table_not_convertible.custom_column",
						e.getColumnName(), e.getType().customTypeID());
			}
		} else {
			throw new UserError(inPorts.getOwner().getOperator(), 156,
					RendererService.getName(data.getClass()), port.getInputPort().getName(),
					RendererService.getName(desiredClass));
		}
	}



}
