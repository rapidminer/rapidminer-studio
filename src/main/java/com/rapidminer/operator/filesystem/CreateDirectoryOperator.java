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

import java.io.File;
import java.util.List;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDirectory;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * 
 * This operator creates a new directory. If the directory can't be created or it already exists, an
 * exeption will be thrown.
 * 
 * @author Philipp Kersting
 * 
 */

public class CreateDirectoryOperator extends Operator {

	public static final String PARAMETER_LOCATION = "location";
	public static final String PARAMETER_NAME = "name";

	private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	public CreateDirectoryOperator(OperatorDescription description) {
		super(description);

		dummyPorts.start();
		getTransformer().addRule(dummyPorts.makePassThroughRule());

	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeDirectory(PARAMETER_LOCATION, "The parent directory of the new folder.", false);
		type.setPrimary(true);
		types.add(type);
		types.add(new ParameterTypeString(PARAMETER_NAME, "The name of the new directory.", false, false));

		return types;

	}

	@Override
	public void doWork() throws OperatorException {
		String locationName = getParameterAsString(PARAMETER_LOCATION);
		String newDirectoryName = getParameterAsString(PARAMETER_NAME);
		File newDirectory = new File(locationName, newDirectoryName);
		if (newDirectory.exists()) {
			throw new UserError(this, "create_directory.exists", newDirectory);
		} else {
			if (!newDirectory.mkdirs()) {
				throw new UserError(this, "create_directory.failure", newDirectory);
			}
		}
		dummyPorts.passDataThrough();
	}

}
