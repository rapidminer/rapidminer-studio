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
import com.rapidminer.tools.expression.ExpressionParsingException;


/**
 * A {@link ExpressionParsingException} that is thrown when the function {@link AttributeEvaluation} fails to
 * find the attribute specified by the inner expression.
 *
 * @author Gisa Meier
 * @since 9.3.0
 */
public class AttributeEvaluationException extends ExpressionParsingException {

	private static final long serialVersionUID = -7644715146786931281L;

	/**
	 * Creates a {@link AttributeEvaluationException} with a message generated from functionName and subExpression.
	 *
	 * @param functionName
	 *            the name of the {@link AttributeEvaluation} function
	 * @param subExpression
	 *            the subexpression for which the {@link AttributeEvaluation} function failed
	 */
	AttributeEvaluationException(String functionName, String subExpression) {
		super(I18N.getErrorMessage("expression_parser.attribute_eval_failed", functionName, subExpression));
	}

}
