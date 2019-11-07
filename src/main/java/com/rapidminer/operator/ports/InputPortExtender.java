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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.adaption.belt.AtPortConverter;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.CollectionPrecondition;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.FlatteningPassThroughRule;
import com.rapidminer.operator.ports.metadata.InputMissingMetaDataError;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.ManyToOnePassThroughRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.metadata.Precondition;


/**
 * Port Extender that can be used if an operator needs to receive an arbitrary number of input
 * objects. Delivered Collections might be unfolded automatically, so that the contained objects are
 * presented as if they would have been forwarded one by one to the operator. This port extender can
 * be configured to throw errors if the wrong type or to few inputs are connected.
 * 
 * @author Simon Fischer, Sebastian Land
 */
public class InputPortExtender extends SinglePortExtender<InputPort> {

	private MetaData desiredMetaData;
	private int numberOfMandatory;

	public InputPortExtender(String name, Ports<InputPort> ports) {
		this(name, ports, null, 0);
	}

	public InputPortExtender(String name, Ports<InputPort> ports, MetaData desiredMetaData, boolean firstIsMandatory) {
		this(name, ports, desiredMetaData, firstIsMandatory ? 1 : 0);
	}

	public InputPortExtender(String name, Ports<InputPort> ports, MetaData desiredMetaData, int numberOfMandatory) {
		super(name, ports);
		this.desiredMetaData = desiredMetaData;
		this.numberOfMandatory = numberOfMandatory;
	}

	@Override
	protected InputPort createPort() {
		InputPort port = super.createPort();
		Precondition precondition = makePrecondition(port, getManagedPorts().size());
		if (precondition != null) {
			port.addPrecondition(new CollectionPrecondition(precondition));
		}
		return port;
	}

	/**
	 * Returns a list of non-null data of all input ports.
	 * 
	 * @param unfold
	 *            If true, collections are added as individual objects rather than as a collection.
	 *            The unfolding is done recursively.
	 * @deprecated use {@link #getData(Class, boolean)}
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public <T extends IOObject> List<T> getData(boolean unfold) {
		List<IOObject> results = new LinkedList<>();
		for (InputPort port : getManagedPorts()) {
			IOObject data = port.getAnyDataOrNull();
			if (data != null) {
				if (unfold && (data instanceof IOObjectCollection)) {
					try {
						unfold((IOObjectCollection<?>) data, results, IOObject.class, port);
					} catch (UserError e) {
						throw new RuntimeException(e.getMessage(), e);
					}
				} else {
					results.add(data);
				}
			}
		}
		return (List<T>) results;
	}

	/**
	 * Returns a list of non-null data of all input ports.
	 * 
	 * @param unfold
	 *            If true, collections are added as individual objects rather than as a collection.
	 *            The unfolding is done recursively.
	 * @throws UserError
	 */
	@SuppressWarnings("unchecked")
	public <T extends IOObject> List<T> getData(Class<T> desiredClass, boolean unfold) throws UserError {
		List<T> results = new ArrayList<T>();
		for (InputPort port : getManagedPorts()) {
			IOObject data = port.getRawData();
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
	 * @param desiredClass
	 *            method will throw unless all non-collection children are of type desired class
	 * @param port
	 *            Used for error message only
	 */
	@SuppressWarnings("unchecked")
	private <T extends IOObject> void unfold(IOObjectCollection<?> collection, List<T> results, Class<T> desiredClass,
			Port port) throws UserError {
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
	private <T extends IOObject> void addSingle(List<T> results, IOObject data, Class<T> desiredClass, Port port)
			throws UserError {
		if (desiredClass.isInstance(data)) {
			results.add(desiredClass.cast(data));
		} else if (AtPortConverter.isConvertible(data.getClass(), desiredClass)) {
			try {
				results.add(desiredClass.cast(AtPortConverter.convert(data, port)));
			} catch (BeltConverter.ConversionException e) {
				throw new UserError(getPorts().getOwner().getOperator(), "table_not_convertible.custom_column",
						e.getColumnName(), e.getType().customTypeID());
			}
		} else {
			throw new UserError(getPorts().getOwner().getOperator(), 156,
					RendererService.getName(data.getClass()), port.getName(),
					RendererService.getName(desiredClass));
		}
	}

	/**
	 * Returns a list of non-null meta data of all input ports.
	 */
	public List<MetaData> getMetaData(boolean unfold) {
		List<MetaData> results = new LinkedList<MetaData>();
		for (InputPort port : getManagedPorts()) {
			MetaData data = port.getMetaData();
			if (data != null) {
				if (unfold && data instanceof CollectionMetaData) {
					results.add(((CollectionMetaData) data).getElementMetaDataRecursive());
				} else {
					results.add(data);
				}
			}
		}
		return results;
	}

	/**
	 * Subclasses might override this method in order to specify preconditions dependent on the
	 * number of port. For example when a parameter lists the input types, etc...
	 */
	protected Precondition makePrecondition(final InputPort port, int portIndex) {
		return makePrecondition(port);
	}

	protected Precondition makePrecondition(final InputPort port) {
		if (desiredMetaData != null) {
			return new Precondition() {

				@Override
				public void assumeSatisfied() {
					if (!getManagedPorts().isEmpty()) {
						getManagedPorts().iterator().next().receiveMD(desiredMetaData);
					}
				}

				@Override
				public void check(MetaData metaData) {
					if (!getManagedPorts().isEmpty()) {
						int portIndex = getManagedPorts().indexOf(port);
						boolean isMandatory = (portIndex < numberOfMandatory);
						// checking if some of the ports received collection
						for (int i = portIndex; i >= 0; i--) {
							MetaData portMetaData = getManagedPorts().get(i).getMetaData();
							if (portMetaData != null) {
								isMandatory &= !portMetaData.isCompatible(new CollectionMetaData(desiredMetaData),
										CompatibilityLevel.VERSION_5);
							}
						}

						// if not: throw error
						if (metaData == null && isMandatory) {
							port.addError(new InputMissingMetaDataError(port, desiredMetaData.getObjectClass(), null));
						}
						if (metaData != null) {
							if (!desiredMetaData.isCompatible(metaData, CompatibilityLevel.VERSION_5)) {
								Collection<MetaDataError> errors = desiredMetaData.getErrorsForInput(port, metaData,
										CompatibilityLevel.VERSION_5);
								for (MetaDataError error : errors) {
									port.addError(error);
								}
							}
						}
					}
				}

				@Override
				public String getDescription() {
					return "requires " + ((numberOfMandatory > 0) ? " at least " + numberOfMandatory + " " : "")
							+ desiredMetaData.getDescription();
				}

				@Override
				public MetaData getExpectedMetaData() {
					return desiredMetaData;
				}

				@Override
				public boolean isCompatible(MetaData input, CompatibilityLevel level) {
					return desiredMetaData.isCompatible(input, level);
				}

			};
		}
		return null;
	}

	public MDTransformationRule makePassThroughRule(OutputPort outputPort) {
		return new ManyToOnePassThroughRule(getManagedPorts(), outputPort);
	}

	public MDTransformationRule makeFlatteningPassThroughRule(OutputPort outputPort) {
		return new FlatteningPassThroughRule(getManagedPorts(), outputPort);
	}

}
