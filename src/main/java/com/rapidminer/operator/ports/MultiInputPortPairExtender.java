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

import com.rapidminer.operator.ports.metadata.MDTransformationRule;


/**
 * @author Simon Fischer
 */
public class MultiInputPortPairExtender extends MultiPortPairExtender<OutputPort, InputPort> {

	public MultiInputPortPairExtender(String name, Ports<OutputPort> singlePorts, Ports<InputPort>[] multiPortsList) {
		super(name, singlePorts, multiPortsList);
	}

	public MDTransformationRule makePassThroughRule() {
		return new MDTransformationRule() {

			@Override
			public void transformMD() {
				for (MultiPortPair mpp : getManagedPairs()) {
					mpp.singlePort.deliverMD(mpp.multiPorts.get(0).getMetaData());
				}
			}
		};
	}

	/**
	 * Delivers all data from all managed input ports to the corresponding output port belonging to
	 * the group specified by toIndex.
	 */
	public void passDataThrough(int fromIndex) {
		for (MultiPortPair mpp : getManagedPairs()) {
			mpp.singlePort.deliver(mpp.multiPorts.get(fromIndex).getRawData());
		}
	}

}
