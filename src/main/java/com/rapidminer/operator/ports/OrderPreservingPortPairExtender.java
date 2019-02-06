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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * Same as {@link PortPairExtender} with the only difference being that empty ports are not
 * compressed but instead deliver {@code null}.
 *
 * @author Marco Boeck
 * @since 7.4
 *
 */
public class OrderPreservingPortPairExtender extends PortPairExtender {

	public OrderPreservingPortPairExtender(String name, InputPorts inPorts, OutputPorts outPorts) {
		super(name, inPorts, outPorts);
	}

	public OrderPreservingPortPairExtender(String name, InputPorts inPorts, OutputPorts outPorts,
			MetaData preconditionMetaData) {
		super(name, inPorts, outPorts, preconditionMetaData);
	}

	/**
	 * Returns the data that has been forwarded to the input ports as ordered list. If a port did
	 * not receive any data, the list will contain null unlike the {@link #getData(Class)} method of
	 * the original PortPairExtender.
	 */
	public <T extends IOObject> List<T> getDataOrNull(Class<T> desiredClass) throws UserError {
		List<T> results = new LinkedList<T>();
		for (PortPair pair : getManagedPairs()) {
			T data = pair.getInputPort().<T> getDataOrNull(desiredClass);
			results.add(data);
		}
		return results;
	}

	@Override
	public void deliver(List<? extends IOObject> ioObjectList) {
		Iterator<PortPair> portIterator = getManagedPairs().iterator();
		for (IOObject object : ioObjectList) {
			if (portIterator.hasNext()) {
				PortPair pair = portIterator.next();
				pair.getOutputPort().deliver(object);
			}
		}
	}
}
