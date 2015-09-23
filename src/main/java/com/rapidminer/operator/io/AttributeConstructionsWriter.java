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

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.tools.io.Encoding;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;


/**
 * Writes all attributes of an example set to a file. Each line holds the construction description
 * of one attribute. This file can be read in another process using the
 * {@link com.rapidminer.operator.features.construction.FeatureGenerationOperator} or
 * {@link AttributeConstructionsLoader}.
 * 
 * @author Ingo Mierswa
 */
public class AttributeConstructionsWriter extends AbstractWriter<ExampleSet> {

	/** The parameter name for &quot;Filename for the attribute construction description file.&quot; */
	public static final String PARAMETER_ATTRIBUTE_CONSTRUCTIONS_FILE = "attribute_constructions_file";

	public AttributeConstructionsWriter(OperatorDescription description) {
		super(description, ExampleSet.class);
	}

	/** Writes the attribute set to a file. */
	@Override
	public ExampleSet write(ExampleSet eSet) throws OperatorException {
		File generatorFile = getParameterAsFile(PARAMETER_ATTRIBUTE_CONSTRUCTIONS_FILE, true);
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(generatorFile));
			out.println("<?xml version=\"1.0\" encoding=\"" + Encoding.getEncoding(this) + "\"?>");
			out.println("<constructions version=\"" + RapidMiner.getShortVersion() + "\">");
			for (Attribute attribute : eSet.getAttributes()) {
				out.println("    <attribute name=\"" + attribute.getName() + "\" construction=\""
						+ attribute.getConstruction() + "\"/>");
			}
			out.println("</constructions>");
		} catch (IOException e) {
			throw new UserError(this, e, 303, new Object[] { generatorFile, e.getMessage() });
		} finally {
			if (out != null) {
				out.close();
			}
		}
		return eSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_ATTRIBUTE_CONSTRUCTIONS_FILE,
				"Filename for the attribute construction description file.", "att", false, false));
		types.addAll(Encoding.getParameterTypes(this));
		return types;
	}

}
