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
package com.rapidminer.operator.features.weighting;

import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;


/**
 * This operator creates a new attribute weights IOObject from a given example set. The result is a
 * vector of attribute weights containing the weight 1.0 for each of the input attributes.
 * 
 * @author Ingo Mierswa
 */
public class ExampleSet2AttributeWeights extends AbstractWeighting {

	public ExampleSet2AttributeWeights(OperatorDescription description) {
		super(description, false);
	}

	@Override
	protected AttributeWeights calculateWeights(ExampleSet exampleSet) throws OperatorException {
		return new AttributeWeights(exampleSet);
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		return true;
	}
}
