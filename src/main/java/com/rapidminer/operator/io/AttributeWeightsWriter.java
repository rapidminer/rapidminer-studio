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
import com.rapidminer.tools.io.Encoding;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


/**
 * Writes the weights of all attributes of an example set to a file. Therefore a
 * {@link AttributeWeights} object is needed in the input of this operator. Each line holds the name
 * of one attribute and its weight. This file can be read in another process using the
 * {@link AttributeWeightsLoader} and the
 * {@link com.rapidminer.operator.features.AttributeWeightsApplier}.
 * 
 * @author Ingo Mierswa
 */
public class AttributeWeightsWriter extends AbstractWriter<AttributeWeights> {

	/** The parameter name for &quot;Filename for the attribute weight file.&quot; */
	public static final String PARAMETER_ATTRIBUTE_WEIGHTS_FILE = "attribute_weights_file";

	public AttributeWeightsWriter(OperatorDescription description) {
		super(description, AttributeWeights.class);
	}

	/** Writes the attribute set to a file. */
	@Override
	public AttributeWeights write(AttributeWeights weights) throws OperatorException {
		File weightFile = getParameterAsFile(PARAMETER_ATTRIBUTE_WEIGHTS_FILE, true);

		try {
			weights.writeAttributeWeights(weightFile, Encoding.getEncoding(this));
		} catch (IOException e) {
			throw new UserError(this, e, 303, new Object[] { weightFile, e.getMessage() });
		}

		return weights;
	}

	@Override
	protected boolean supportsEncoding() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeFile(PARAMETER_ATTRIBUTE_WEIGHTS_FILE, "Filename for the attribute weight file.", "wgt",
				false));
		types.addAll(super.getParameterTypes());
		return types;
	}
}
