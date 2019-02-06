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
package com.rapidminer.tools.expression.internal.function.process;

import com.rapidminer.Process;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.function.AbstractArbitraryStringInputStringOutputFunction;


/**
 * Function for getting a parameter of a certain operator.
 *
 * @author Gisa Schaefer
 *
 */
public class ParameterValue extends AbstractArbitraryStringInputStringOutputFunction {

	private final Process process;

	/**
	 * Creates a function that looks up a operator parameter in the process.
	 *
	 * @param process
	 *            the process where to find the operator
	 */
	public ParameterValue(Process process) {
		super("process.param", 2);
		this.process = process;
	}

	@Override
	protected void checkNumberOfInputs(int length) {
		int expectedInput = getFunctionDescription().getNumberOfArguments();
		if (length != expectedInput) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), expectedInput,
					length);
		}
	}

	@Override
	protected String compute(String... values) {
		try {
			Operator operator = process.getOperator(values[0]);
			if (operator == null) {
				throw new FunctionInputException("expression_parser.parameter_value_wrong_operator", getFunctionName());
			}
			return operator.getParameter(values[1]);
		} catch (UndefinedParameterError e) {
			throw new FunctionInputException("expression_parser.parameter_value_wrong_parameter", getFunctionName());
		}
	}

}
