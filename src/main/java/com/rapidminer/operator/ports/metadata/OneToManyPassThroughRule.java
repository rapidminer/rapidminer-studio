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
import com.rapidminer.operator.ports.OutputPorts;

import java.util.Collection;


/**
 * A rule which copies meta data from one input port to several output ports.
 * 
 * @author Simon Fischer
 * 
 */
public class OneToManyPassThroughRule implements MDTransformationRule {

	private final InputPort inputPort;
	private final Collection<OutputPort> outputPorts;

	public OneToManyPassThroughRule(InputPort inputPort, OutputPorts outputPorts) {
		this(inputPort, outputPorts.getAllPorts());
	}

	public OneToManyPassThroughRule(InputPort inputPort, Collection<OutputPort> outputPorts) {
		this.inputPort = inputPort;
		this.outputPorts = outputPorts;
	}

	@Override
	public void transformMD() {
		int i = 0;
		for (OutputPort outputPort : outputPorts) {
			MetaData metaData = inputPort.getMetaData();
			if (metaData != null) {
				metaData = metaData.clone();
				metaData.addToHistory(outputPort);
				outputPort.deliverMD(modifyMetaData(metaData, i));
			} else {
				outputPort.deliverMD(null);
			}
			i++;
		}
	}

	/**
	 * Modifies the received meta data before it is passed to the output. Can be used if the
	 * transformation depends on parameters etc. The default implementation just returns the
	 * original. Subclasses may safely modify the meta data, since a copy is used for this method.
	 * 
	 * @param outputIndex
	 *            TODO
	 */
	public MetaData modifyMetaData(MetaData unmodifiedMetaData, int outputIndex) {
		return unmodifiedMetaData;
	}
}
