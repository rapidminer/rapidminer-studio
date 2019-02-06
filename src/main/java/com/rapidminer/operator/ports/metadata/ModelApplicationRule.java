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


/**
 * This rule might be used, if a model is applied on a data set.
 * 
 * @author Sebastian Land
 */
public class ModelApplicationRule extends PassThroughRule {

	private InputPort modelInput;

	public ModelApplicationRule(InputPort inputPort, OutputPort outputPort, InputPort modelInput, boolean mandatory) {
		super(inputPort, outputPort, mandatory);
		this.modelInput = modelInput;
	}

	@Override
	public MetaData modifyMetaData(MetaData metaData) {
		if (metaData instanceof ExampleSetMetaData) {
			MetaData modelMetaData = modelInput.getMetaData();
			if (modelMetaData instanceof ModelMetaData) {
				metaData = ((ModelMetaData) modelMetaData).apply((ExampleSetMetaData) metaData, getInputPort());
			}
		}
		return metaData;
	}

}
