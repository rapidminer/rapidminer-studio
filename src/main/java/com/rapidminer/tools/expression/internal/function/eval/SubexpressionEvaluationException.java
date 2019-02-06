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
package com.rapidminer.tools.expression.internal.function.eval;

import com.rapidminer.tools.I18N;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParsingException;


/**
 * A {@link ExpressionParsingException} that is thrown when the function {@link Evaluation} fails to
 * evaluate a subexpression. Contains the causing {@link ExpressionParsingException} or
 * {@link ExpressionException} as cause.
 *
 * @author Gisa Schaefer
 *
 */
public class SubexpressionEvaluationException extends ExpressionParsingException {

	private static final long serialVersionUID = -7644715146686931281L;

	/**
	 * Creates a {@link SubexpressionEvaluationException} with the given cause and a message
	 * generated from functionName, subExpression and the message of the cause.
	 *
	 * @param functionName
	 *            the name of the {@link Evaluation} function
	 * @param subExpression
	 *            the subexpression for which the {@link Evaluation} function failed
	 * @param cause
	 *            the cause of the failure
	 */
	SubexpressionEvaluationException(String functionName, String subExpression, Exception cause) {
		super(I18N.getErrorMessage("expression_parser.eval_failed", functionName, subExpression, cause.getMessage()), cause);
	}

}
