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

import java.util.Collection;


/**
 * Delivers meta data received in the first input that is not null to the output port. TODO: Make
 * intersection of meta data. This is all we can guarantee.
 * 
 * @author Simon Fischer
 */
public class ManyToOnePassThroughRule implements MDTransformationRule {

	private OutputPort outputPort;
	private Collection<InputPort> inputPorts;

	public ManyToOnePassThroughRule(Collection<InputPort> inputPorts, OutputPort outputPort) {
		this.inputPorts = inputPorts;
		this.outputPort = outputPort;
	}

	@Override
	public void transformMD() {
		for (InputPort inputPort : inputPorts) {
			MetaData metaData = inputPort.getMetaData();
			if (metaData != null) {
				metaData = metaData.clone();
				metaData.addToHistory(outputPort);
				outputPort.deliverMD(modifyMetaData(metaData));
				return;
			}
		}
		outputPort.deliverMD(null);
	}

	/**
	 * Modifies the standard meta data before it is passed to the output. Can be used if the
	 * transformation depends on parameters etc. The default implementation just returns the
	 * original. Subclasses may safely modify the meta data, since a copy is used for this method.
	 */
	public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
		return unmodifiedMetaData;
	}
}
