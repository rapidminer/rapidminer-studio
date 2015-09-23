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
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;


/**
 * Writes the input performance vector in a given file. You also might want to use the
 * {@link com.rapidminer.operator.io.ResultWriter} operator which writes all current results in the
 * main result file.
 * 
 * @author Ingo Mierswa Exp $
 */
public class PerformanceWriter extends AbstractWriter<PerformanceVector> {

	/** The parameter name for &quot;Filename for the performance file.&quot; */
	public static final String PARAMETER_PERFORMANCE_FILE = "performance_file";

	public PerformanceWriter(OperatorDescription description) {
		super(description, PerformanceVector.class);
	}

	/** Writes the attribute set to a file. */
	@Override
	public PerformanceVector write(PerformanceVector performance) throws OperatorException {
		File performanceFile = getParameterAsFile(PARAMETER_PERFORMANCE_FILE, true);

		OutputStream out = null;
		try {
			out = new FileOutputStream(performanceFile);
			performance.write(out);
		} catch (IOException e) {
			throw new UserError(this, e, 303, new Object[] { performanceFile, e.getMessage() });
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					logError("Cannot close stream to file " + performanceFile);
				}
			}
		}

		return performance;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_PERFORMANCE_FILE, "Filename for the performance file.", "per", false));
		return types;
	}
}
