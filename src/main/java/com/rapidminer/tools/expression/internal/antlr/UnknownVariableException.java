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

import org.antlr.v4.runtime.ParserRuleContext;

import com.rapidminer.tools.expression.ExpressionParsingException;
import com.rapidminer.tools.expression.internal.UnknownResolverVariableException;


/**
 * A {@link ExpressionParsingException} that is thrown when a variable (used as 'variable' in the
 * expression) is unknown.
 *
 * @author Gisa Schaefer
 *
 */
public class UnknownVariableException extends UnknownResolverVariableException {

	private static final long serialVersionUID = 4596282974568142330L;

	/**
	 * Creates a parsing exception with message associated to the i18n and the arguments.
	 *
	 * @param i18n
	 *            the i18n error key
	 * @param arguments
	 */
	UnknownVariableException(String i18n, Object... arguments) {
		super(i18n, arguments);
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
	UnknownVariableException(ParserRuleContext ctx, String i18n, Object... arguments) {
		super(ctx, i18n, arguments);
	}

}
