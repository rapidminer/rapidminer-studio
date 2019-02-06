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
 * Interface for an expression parser that can check the syntax of string expressions and parse
 * those into an {@link Expression}.
 *
 * @author Gisa Schaefer
 * @since 6.5.0
 */
public interface ExpressionParser {

	/**
	 * Parses the expression with the grammar and checks function names and number of arguments.
	 *
	 * @param expression
	 *            the expression to parse
	 * @throws ExpressionException
	 *             if the syntax check failed. The ExpressionException can contain a
	 *             {@link ExpressionParsingException} as cause. see the java doc of
	 *             {@link ExpressionParsingException} for different marker subclasses of special
	 *             error cases.
	 */
	public void checkSyntax(String expression) throws ExpressionException;

	/**
	 * Parses the expression with the grammar and precompiles by evaluating constant parts.
	 *
	 * @param expression
	 *            the expression to parse
	 * @return the generated Expression
	 * @throws ExpressionException
	 *             if the parsing failed. The ExpressionException can contain a
	 *             {@link ExpressionParsingException} as cause. see the java doc of
	 *             {@link ExpressionParsingException} for different marker subclasses of special
	 *             error cases.
	 */
	public Expression parse(String expression) throws ExpressionException;

	/**
	 * Returns the {@link ExpressionContext} known by parser.
	 *
	 * @return the expression context
	 */
	public ExpressionContext getExpressionContext();

}
