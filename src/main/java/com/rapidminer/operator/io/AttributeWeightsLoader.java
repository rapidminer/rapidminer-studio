/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.io;

import com.rapidminer.example.AttributeWeights;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;

import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * Reads the weights for all attributes of an example set from a file and creates a new
 * {@link AttributeWeights} IOObject. This object can be used for scaling the values of an example
 * set with help of the {@link com.rapidminer.operator.features.AttributeWeightsApplier} operator.
 * 
 * @author Ingo Mierswa
 */
public class AttributeWeightsLoader extends AbstractReader<AttributeWeights> {

	/** The parameter name for &quot;Filename of the attribute weights file.&quot; */
	public static final String PARAMETER_ATTRIBUTE_WEIGHTS_FILE = "attribute_weights_file";

	public AttributeWeightsLoader(OperatorDescription description) {
		super(description, AttributeWeights.class);
	}

	/** Writes the attribute set to a file. */
	@Override
	public AttributeWeights read() throws OperatorException {
		File weightFile = getParameterAsFile(PARAMETER_ATTRIBUTE_WEIGHTS_FILE);
		AttributeWeights result = null;
		try {
			result = AttributeWeights.load(weightFile);
		} catch (IOException e) {
			throw new UserError(this, e, 302, new Object[] { weightFile, e.getMessage() });
		}
		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_ATTRIBUTE_WEIGHTS_FILE, "Filename of the attribute weights file.", "wgt",
				false, false));
		return types;
	}
}
