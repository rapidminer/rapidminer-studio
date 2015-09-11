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

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.meta.ParameterSet;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.tools.io.Encoding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;


/**
 * Writes a parameter set into a file. This can be created by one of the parameter optimization
 * operators, e.g. {@link com.rapidminer.operator.meta.GridSearchParameterOptimizationOperator}. It
 * can then be applied to the operators of the process using a
 * {@link com.rapidminer.operator.meta.ParameterSetter}.
 * 
 * @author Simon Fischer, Ingo Mierswa Exp $
 */
public class ParameterSetWriter extends AbstractWriter<ParameterSet> {

	/** The parameter name for &quot;A file containing a parameter set.&quot; */
	public static final String PARAMETER_PARAMETER_FILE = "parameter_file";

	public ParameterSetWriter(OperatorDescription description) {
		super(description, ParameterSet.class);
	}

	@Override
	public ParameterSet write(ParameterSet parameterSet) throws OperatorException {
		File parameterFile = getParameterAsFile(PARAMETER_PARAMETER_FILE, true);
		PrintWriter out = null;
		try {
			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(parameterFile), Encoding.getEncoding(this)));
			parameterSet.writeParameterSet(out, Encoding.getEncoding(this));
		} catch (IOException e) {
			throw new UserError(this, 303, e, new Object[] { parameterFile, e.getMessage() });
		} finally {
			if (out != null) {
				out.close();
			}
		}
		return parameterSet;
	}

	@Override
	protected boolean supportsEncoding() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeFile(PARAMETER_PARAMETER_FILE, "A file containing a parameter set.", "par", false));
		types.addAll(super.getParameterTypes());
		return types;
	}

}
