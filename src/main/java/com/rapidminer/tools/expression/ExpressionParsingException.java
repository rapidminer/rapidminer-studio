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

import org.antlr.v4.runtime.ParserRuleContext;

import com.rapidminer.tools.I18N;


/**
 * {@link RuntimeException} with i18n message or message of the causing exception for exceptions
 * that happen while parsing a string expression into an {@link Expression} or checking the syntax.
 * Specialized exceptions used by the parser itself and by core functions:
 * <ul>
 * <li> {@link UnknownResolverVariableException} used in case resolving a variable via a resolver
 * fails.
 * <p>
 * Following exceptions are subtypes of this exception:</li>
 * <ul>
 * <li>{@link UnknownDynamicVariableException} used for unknown dynamic variables (used as '[var]'
 * in the expression)</li>
 * <li>{@link UnknownVariableException} used for unknown variables (used as 'var' in the expression)
 * </li>
 * <li>{@link UnknownScopeConstantException} used for unknown scope constants (used as '%{const}' or
 * '#{const}' in the expression)</li>
 * </ul>
 * <li>{@link UnknownFunctionException} used if there is no {@link Function} with the name that is
 * requested</li> <li>{@link FunctionInputException} used if a {@link Function} has wrong arguments</li>
 * <li>{@link SubexpressionEvaluationException} used if the {@link Evaluation} function fails to
 * evaluate a subexpression</li> </ul>
 *
 * @author Gisa Schaefer
 *
 */
public class ExpressionParsingException extends RuntimeException {

	private static final long serialVersionUID = 5039785364556274963L;
	private transient ParserRuleContext ctx;

	/**
	 * Creates a parsing exception with message associated to the i18n and the arguments.
	 *
	 * @param i18n
	 *            the i18n error key
	 * @param arguments
	 */
	protected ExpressionParsingException(String i18n, Object... arguments) {
		super(I18N.getErrorMessage(i18n, arguments));
	}

	/**
	 * Wraps an exception into an {@link ExpressionParsingException} and takes its message.
	 *
	 * @param e
	 *            the throwable that is the cause
	 */
	public ExpressionParsingException(Throwable e) {
		super(e.getMessage(), e);
	}

	/**
	 * Creates a parsing exception with message associated to the i18n and the arguments and stores
	 * the error context ctx.
	 *
	 * @param ctx
	 *            the error context
	 * @param i18n
	 *            the i18n error key
	 * @param arguments
	 */
	protected ExpressionParsingException(ParserRuleContext ctx, String i18n, Object... arguments) {
		super(I18N.getErrorMessage(i18n, arguments));
		this.ctx = ctx;
	}

	/**
	 * @return the error context, can be {@code null}
	 */
	public ParserRuleContext getErrorContext() {
		return ctx;
	}

}
