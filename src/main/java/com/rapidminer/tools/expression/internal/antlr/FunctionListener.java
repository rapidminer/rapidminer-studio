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
package com.rapidminer.tools.expression.internal.antlr;

import com.rapidminer.tools.expression.ExpressionContext;
import com.rapidminer.tools.expression.Function;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.antlr.FunctionExpressionParser.FunctionContext;
import com.rapidminer.tools.expression.internal.antlr.FunctionExpressionParser.OperationExpContext;


/**
 * Antlr ParseTreeListener that checks if the operators in the expression are known, the functions
 * are valid functions and have the right number of arguments.
 *
 * @author Gisa Schaefer
 *
 */
class FunctionListener extends FunctionExpressionParserBaseListener {

	private ExpressionContext lookup;

	/**
	 * Creates a listener that checks if operators and function names in the expression are known by
	 * the lookup and have the right number of arguments.
	 *
	 * @param lookup
	 *            the ExpressionContext that administers the valid functions
	 */
	FunctionListener(ExpressionContext lookup) {
		this.lookup = lookup;
	}

	@Override
	public void enterOperationExp(OperationExpContext ctx) {
		if (ctx.op == null) {
			return;
		} else {
			String operatorName = ctx.op.getText();
			Function function = lookup.getFunction(ctx.op.getText());
			if (function == null) {
				throw new UnknownFunctionException(ctx, "expression_parser.unknown_operator", operatorName);
			}
		}
	}

	@Override
	public void enterFunction(FunctionContext ctx) {
		String functionName = ctx.NAME().getText();
		Function function = lookup.getFunction(functionName);
		if (function == null) {
			throw new UnknownFunctionException(ctx, "expression_parser.unknown_function", functionName);
		}
		int currentInputs = ctx.operationExp().size();
		int expectedInputs = function.getFunctionDescription().getNumberOfArguments();
		if (expectedInputs > FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS && currentInputs != expectedInputs) {
			throw new FunctionInputException(ctx, "expression_parser.function_wrong_input", function.getFunctionName(),
					expectedInputs, currentInputs);
		}
	}

}
