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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;

import java.util.List;


/**
 * Passes meta data through and recursively unfolds element meta data if CollectionMetaData is
 * found.
 * 
 * @author Simon Fischer
 * 
 */
public class FlatteningPassThroughRule implements MDTransformationRule {

	private final List<InputPort> inputPorts;
	private final OutputPort outputPort;

	public FlatteningPassThroughRule(List<InputPort> inputPorts, OutputPort outputPort) {
		this.inputPorts = inputPorts;
		this.outputPort = outputPort;
	}

	@Override
	public void transformMD() {
		for (InputPort inputPort : inputPorts) {
			MetaData metaData = inputPort.getMetaData();
			if (metaData != null) {
				if (metaData instanceof CollectionMetaData) {
					metaData = ((CollectionMetaData) metaData).getElementMetaDataRecursive();
				}
				if (metaData != null) {
				metaData = metaData.clone();
				metaData.addToHistory(outputPort);
				outputPort.deliverMD(modifyMetaData(metaData));
				return;
			}
		}
		}
		outputPort.deliverMD(null);
	}

	protected MetaData modifyMetaData(MetaData metaData) {
		return metaData;
	}
}
