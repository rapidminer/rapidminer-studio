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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.OutputPort;


/**
 * Assigns a predefined meta data object to an output port. Useful if operators newly generate
 * IOObjects. If the meta data changes dynamically, can be modified by overriding
 * {@link #modifyMetaData(MetaData)}.
 *
 * @author Simon Fischer
 */
public class GenerateNewMDRule implements MDTransformationRule {

	private OutputPort outputPort;
	private MetaData unmodifiedMetaData;

	public GenerateNewMDRule(OutputPort outputPort, Class<? extends IOObject> clazz) {
		this(outputPort, ExampleSet.class.equals(clazz) ? new ExampleSetMetaData() : new MetaData(clazz));
	}

	public GenerateNewMDRule(OutputPort outputPort, MetaData unmodifiedMetaData) {
		this.outputPort = outputPort;
		this.unmodifiedMetaData = unmodifiedMetaData;
	}

	@Override
	public void transformMD() {
		MetaData clone = this.unmodifiedMetaData.clone();
		clone.addToHistory(outputPort);
		outputPort.deliverMD(modifyMetaData(clone));
	}

	/**
	 * Modifies the standard meta data before it is passed to the output. Can be used if the
	 * transformation depends on parameters etc. The default implementation just returns the
	 * original. Subclasses may safely modify the meta data, since a copy is used for this method.
	 */
	public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
		return unmodifiedMetaData;
	}

	/**
	 * @return a clone of the unmodified meta data object
	 */
	public MetaData getUnmodifiedMetaData() {
		return unmodifiedMetaData.clone();
	}

	/**
	 * @return the {@link OutputPort} the MD rule is for
	 */
	public OutputPort getOutputPort() {
		return outputPort;
	}

}
