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
package com.rapidminer.tools.expression;

/**
 * Class for a function that can be used inside expressions. Functions are contained in a
 * {@link ExpressionParserModule} which can be used in a {@link ExpressionParserBuilder} via
 * {@link ExpressionParserBuilder#withModule(ExpressionParserModule)} or
 * {@link ExpressionParserBuilder#withModules(java.util.List)}. Furthermore, the module containing
 * the {@link Function}s can be registered with the {@link ExpressionRegistry} such that the
 * functions are used in the standard core operators that use an {@link ExpressionParser}.
 *
 * @author Gisa Schaefer
 * @since 6.5.0
 */
public interface Function {

	/**
	 * @return the {@link FunctionDescription} of the function
	 */
	public FunctionDescription getFunctionDescription();

	/**
	 * @return the function name of the function
	 */
	public String getFunctionName();

	/**
	 * Creates an {@link ExpressionEvaluator} for this function with the given inputEvaluators as
	 * arguments.
	 *
	 * @param inputEvaluators
	 *            the {@link ExpressionEvaluators} containing the input arguments
	 * @return an expression evaluator for this function applied to the inputEvaluators
	 * @throws ExpressionParsingException
	 *             if the creation of the ExpressionEvaluator fails, FunctionInputException if the
	 *             cause for the failure is a wrong argument
	 */
	public ExpressionEvaluator compute(ExpressionEvaluator... inputEvaluators) throws ExpressionParsingException;

}
