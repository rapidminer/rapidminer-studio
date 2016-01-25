/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.tools.Tools;

import java.io.File;
import java.util.List;


/**
 * 
 * This operator deletes the selected file. If the file doesn't exist and the corresponding checkbox
 * is activated, an exception is thrown.
 * 
 * @author Philipp Kersting
 * 
 */

public class DeleteFileOperator extends Operator {

	public static final String PARAMETER_FILE = "file";
	public static final String PARAMETER_NO_FILE_ERROR = "fail_if_missing";

	private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	public DeleteFileOperator(OperatorDescription description) {
		super(description);
		dummyPorts.start();
		getTransformer().addRule(dummyPorts.makePassThroughRule());
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeFile(PARAMETER_FILE, "The file to delete.", "*", false, false));
		types.add(new ParameterTypeBoolean(PARAMETER_NO_FILE_ERROR,
				"Determines whether an exception should be generated if the file is not found.", false, false));

		return types;
	}

	@Override
	public void doWork() throws OperatorException {
		String fileName = getParameterAsString(PARAMETER_FILE);
		File file = new File(fileName);
		if (file.exists()) {
			if (!Tools.delete(file)) {
				throw new UserError(this, "delete_file.failure", file);
			}
		} else if (!file.exists() && getParameterAsBoolean(PARAMETER_NO_FILE_ERROR)) {
			throw new UserError(this, "301", file);
		}

		dummyPorts.passDataThrough();
	}
}
