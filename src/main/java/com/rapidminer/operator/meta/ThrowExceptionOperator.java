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
package com.rapidminer.operator.meta;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;

import java.util.List;


/**
 * 
 * This operator throws an exception with an user-defined message.
 * 
 * @author Philipp
 * 
 */

public class ThrowExceptionOperator extends Operator {

	public static final String PARAMETER_MESSAGE = "message";

	private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	public ThrowExceptionOperator(OperatorDescription description) {
		super(description);
		dummyPorts.start();
		getTransformer().addRule(dummyPorts.makePassThroughRule());
	}

	@Override
	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeString(PARAMETER_MESSAGE, "The error message of the exception.", false, false));

		return types;
	}

	@Override
	public void doWork() throws OperatorException {
		dummyPorts.passDataThrough();
		throw new OperatorException(getParameterAsString(PARAMETER_MESSAGE));
	}
}
