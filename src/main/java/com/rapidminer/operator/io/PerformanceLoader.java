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

import com.rapidminer.operator.AbstractIOObject;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.UndefinedParameterError;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * Reads a performance vector from a given file. This performance vector must have been written
 * before with a {@link PerformanceWriter}.
 * 
 * @author Ingo Mierswa
 */
public class PerformanceLoader extends AbstractReader<PerformanceVector> {

	/** The parameter name for &quot;Filename for the performance file.&quot; */
	public static final String PARAMETER_PERFORMANCE_FILE = "performance_file";

	public PerformanceLoader(OperatorDescription description) {
		super(description, PerformanceVector.class);
	}

	/** Reads the performance vector from a file. */
	@Override
	public PerformanceVector read() throws OperatorException {
		getParameter(PARAMETER_PERFORMANCE_FILE);
		AbstractIOObject.InputStreamProvider inputStreamProvider = new AbstractIOObject.InputStreamProvider() {

			@Override
			public InputStream getInputStream() throws IOException {
				try {
					return getParameterAsInputStream(PARAMETER_PERFORMANCE_FILE);
				} catch (UndefinedParameterError e) {
					throw new IOException(e);
				} catch (UserError e) {
					throw new IOException(e);
				}
			}
		};
		IOObject performance;
		try {
			performance = AbstractIOObject.read(inputStreamProvider);
		} catch (IOException e) {
			throw new UserError(this, e, 302, getParameter(PARAMETER_PERFORMANCE_FILE), e);
		}
		if (!(performance instanceof PerformanceVector)) {
			throw new UserError(this, 942, new Object[] { getParameter(PARAMETER_PERFORMANCE_FILE), "PerformanceVector",
					performance.getClass().getSimpleName() });
		}

		return (PerformanceVector) performance;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_PERFORMANCE_FILE, "Filename for the performance file.", "per", false));
		return types;
	}
}
