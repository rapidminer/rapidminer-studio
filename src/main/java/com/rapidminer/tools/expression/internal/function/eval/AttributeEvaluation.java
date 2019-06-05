/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools.expression.internal.function.eval;

import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionContext;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.Function;


/**
 * A {@link Function} that evaluates subexpressions as attributes.
 * <p>
 * If the first input of the attribute eval function is constant, the attribute is fixed and a fixed {@link
 * ExpressionEvaluator} for that attribute is constructed. This means that the evaluation of the {@link Expression}s
 * generated from {@code attribute("att"+(4+3))}, {@code attribute("att" + 7)} and {@code attribute("att7")} have the
 * same complexity at evaluation time.
 * <p>
 * If the first argument is not constant, then a second argument is needed to determine the result type and the parser
 * is called every time the resulting {@link Expression} is evaluated. In particular, evaluating {@link Expression}s
 * such as {@code attribute("att"+[att1], REAL)} is slower than the examples above.
 *
 * @author Gisa Meier
 * @since 9.3.0
 */
public class AttributeEvaluation extends AbstractEvaluation {

	private ExpressionContext context;

	/**
	 * Creates a evaluation {@link Function}. Before this functions {@link #compute(ExpressionEvaluator...)} method can
	 * be called, a context needs to be set via {@link #setContext(ExpressionContext)}.
	 */
	public AttributeEvaluation() {
		super("process.attribute");
	}

	/**
	 * Sets the context that this evaluation function should use. This must always be done before using {@link
	 * #compute(ExpressionEvaluator...)}.
	 *
	 * @param context
	 * 		the context to use
	 */
	public void setContext(ExpressionContext context) {
		this.context = context;
	}

	@Override
	protected void checkSetup() {
		if (context == null) {
			throw new IllegalStateException("context must be set in order to evaluate");
		}
	}

	@Override
	protected ExpressionEvaluator compute(String expressionString) {
		ExpressionEvaluator evaluator = context.getDynamicVariable(expressionString);
		if (evaluator == null) {
			throw new AttributeEvaluationException(getFunctionName(), expressionString);
		}
		return evaluator;
	}
}
