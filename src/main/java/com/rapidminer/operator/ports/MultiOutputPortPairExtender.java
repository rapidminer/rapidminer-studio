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
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * @author Simon Fischer
 */
public class MultiOutputPortPairExtender extends MultiPortPairExtender<InputPort, OutputPort> {

	public MultiOutputPortPairExtender(String name, Ports<InputPort> singlePorts, Ports<OutputPort>[] multiPortsList) {
		super(name, singlePorts, multiPortsList);
	}

	public MDTransformationRule makePassThroughRule() {
		return new MDTransformationRule() {

			@Override
			public void transformMD() {
				for (MultiPortPair mpp : getManagedPairs()) {
					MetaData md = mpp.singlePort.getMetaData();
					for (OutputPort port : mpp.multiPorts) {
						port.deliverMD(md);
					}
				}
			}
		};
	}

	/**
	 * Do not use this method, it can cause a memory leak. This passes the data to several ports (in
	 * subprocesses). If one of the subprocesses is not executed it will continue to hold the data.
	 * Use {@link #passDataThrough(int)} instead to pass the data to where it will be used.
	 *
	 * @deprecated use {@link #passDataThrough(int)} instead
	 */
	@Deprecated
	public void passDataThrough() {
		for (MultiPortPair mpp : getManagedPairs()) {
			IOObject data = mpp.singlePort.getRawData();
			for (OutputPort port : mpp.multiPorts) {
				port.deliver(data);
			}
		}
	}

	/**
	 * Passes the data from the singlePort of the {@link MultiPortPair} to the i-th port of the
	 * multiPorts.
	 *
	 * @param i
	 *            the port to receive the data
	 * @since 7.5.0
	 */
	public void passDataThrough(int i) {
		for (MultiPortPair mpp : getManagedPairs()) {
			IOObject data = mpp.singlePort.getRawData();
			mpp.multiPorts.get(i).deliver(data);
		}
	}

}
