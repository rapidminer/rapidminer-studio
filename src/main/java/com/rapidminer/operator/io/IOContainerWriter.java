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

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;

import java.util.List;


/**
 * Dummy operator.
 * 
 * @author Simon Fischer
 */
public class IOContainerWriter extends Operator {

	public static final String PARAMETER_FILENAME = "filename";

	private final PortPairExtender throughExtender = new PortPairExtender("through", getInputPorts(), getOutputPorts());

	public IOContainerWriter(OperatorDescription description) {
		super(description);
		throughExtender.start();
	}

	@Override
	public void doWork() {
		getLogger()
				.warning(
						"This operator is deprecated, does nothing, and should have been replaced during process import by several IOObjectWriters.");
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeFile(PARAMETER_FILENAME, "Name of file to write the output to.", "ioc", false);
		type.setExpert(false);
		types.add(type);
		return types;
	}

}
