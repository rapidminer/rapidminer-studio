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
package com.rapidminer.operator.filesystem;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeFile;

import java.io.File;
import java.util.List;


/**
 * 
 * This Operator moves a file to another location. If the destination is a directory, the file is
 * moved in there. If the target already exists and overwriting is enabled, the existing file will
 * be overwritten. Else, an exception is thrown.
 * 
 * @author Philipp Kersting
 * 
 */

public class MoveFileOperator extends Operator {

	public static final String PARAMETER_FILE = "file";
	public static final String PARAMETER_DESTINATION = "destination";
	public static final String PARAMETER_OVERWRITE = "overwrite";

	private PortPairExtender dummyPorts = new PortPairExtender("through", getInputPorts(), getOutputPorts());

	public MoveFileOperator(OperatorDescription description) {
		super(description);
		dummyPorts.start();
		getTransformer().addRule(dummyPorts.makePassThroughRule());
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeFile(PARAMETER_FILE, "The file to be moved", "*", false, false));
		types.add(new ParameterTypeFile(PARAMETER_DESTINATION, "The destination of the file.", "*", false, false));
		types.add(new ParameterTypeBoolean(PARAMETER_OVERWRITE,
				"Determines whether an already existing file should be overwritten.", false, false));

		return types;
	}

	@Override
	public void doWork() throws OperatorException {
		String fileName = getParameterAsString(PARAMETER_FILE);
		String destination = getParameter(PARAMETER_DESTINATION);
		Boolean overwrite = getParameterAsBoolean(PARAMETER_OVERWRITE);
		File file = new File(fileName);
		File destinationFile = new File(destination);
		if (!destinationFile.getParentFile().exists()) {
			destinationFile.getParentFile().mkdirs();
		}
		if (destinationFile.isDirectory()) {
			destinationFile = new File(destinationFile.getAbsoluteFile() + "/" + new File(fileName).getName());
		}
		if ((!destinationFile.exists() || overwrite) && file.exists()) {

			if (destinationFile.exists() && overwrite) {
				if (!destinationFile.delete()) {
					throw new UserError(this, "303", destinationFile, "Existing file could not be deleted.");
				}
			}
			if (!file.renameTo(destinationFile)) {
				throw new UserError(this, "move_file.failure", file, destinationFile);
			}
		} else if (!file.exists()) {
			throw new UserError(this, "301", file);
		} else if (!destinationFile.isDirectory()) {
			throw new UserError(this, "move_file.exists", destinationFile);
		}
		dummyPorts.passDataThrough();
	}
}
