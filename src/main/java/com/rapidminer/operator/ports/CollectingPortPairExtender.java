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

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * This port pair extender offers the ability to collect data at its input ports over several
 * iterations and deliver it to its output ports as a collection.<br/>
 * 
 * Usage: call {@link #reset()} before beginning the loop and {@link #collect()} in every iteration.
 * 
 * 
 * @author Simon Fischer
 */
public class CollectingPortPairExtender extends PortPairExtender {

	public CollectingPortPairExtender(String name, InputPorts inPorts, OutputPorts outPorts) {
		super(name, inPorts, outPorts);
	}

	@Override
	protected MetaData transformMetaData(MetaData md) {
		return new CollectionMetaData(md);
	}

	/**
	 * Resets all output ports by delivering empty collection to all output ports for which the
	 * input port is connected and clearing others.
	 */
	public void reset() {
		for (PortPair pair : getManagedPairs()) {
			if (pair.getInputPort().isConnected()) {
				pair.getOutputPort().deliver(new IOObjectCollection<IOObject>());
			} else {
				pair.getOutputPort().clear(Port.CLEAR_DATA);
			}
		}
	}

	/**
	 * For all input ports that have data, this data is added to the collection currently assigned
	 * to the output port.
	 */
	@SuppressWarnings("unchecked")
	public void collect() {
		synchronized (this) {
			for (PortPair pair : getManagedPairs()) {
				IOObject data = pair.getInputPort().getRawData();
				if (data != null) {
					IOObject output = pair.getOutputPort().getRawData();
					if (output == null) { // first iteration
						IOObjectCollection<IOObject> collection = new IOObjectCollection<>();
						collection.add(data);
						pair.getOutputPort().deliver(collection);
					} else if (output instanceof IOObjectCollection) {
						((IOObjectCollection<IOObject>) output).add(data);
						pair.getOutputPort().deliver(output); // necessary to trigger updates
					} else {
						pair.getOutputPort()
								.getPorts()
								.getOwner()
								.getOperator()
								.getLogger()
								.warning(
										"Cannot collect output at " + pair.getOutputPort().getSpec() + ": data is of type "
												+ output.getClass().getName() + ".");
					}
				}
			}
		}
	}
}
