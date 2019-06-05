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

import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParsingException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.Function;
import com.rapidminer.tools.expression.internal.SimpleExpressionEvaluator;
import com.rapidminer.tools.expression.internal.antlr.AntlrParser;

/**
 * A {@link Function} that evaluates subexpressions using an {@link AntlrParser}.
 * <p>
 * If the first input of the eval function is constant, the parser is only called once during the
 * callable-creation step and not again when the resulting {@link Expression} is evaluated. This
 * means that the evaluation of the {@link Expression}s generated from {@code eval("4+3")*[att1]},
 * {@code eval("(4+3)*[att1]")} and {@code 7*[att1]} have the same complexity at evaluation time.
 * <p>
 * If the first argument is not constant, then a second argument is needed to determine the result
 * type and the parser is called every time the resulting {@link Expression} is evaluated. In
 * particular, evaluating {@link Expression}s such as {@code eval("(4+3)*"+[att1],REAL)} is way
 * slower than the examples above.
 *
 * @author Gisa Schaefer
 *
 */
public class Evaluation extends AbstractEvaluation {

	private AntlrParser parser;

	/**
	 * Creates a evaluation {@link Function}. Before this functions
	 * {@link #compute(ExpressionEvaluator...)} method can be called, a parser needs to be set via
	 * {@link #setParser(AntlrParser)}.
	 */
	public Evaluation() {
		super("process.eval");
	}

	/**
	 * Sets the parser that this evaluation function should use. This must always be done before
	 * using {@link #compute(ExpressionEvaluator...)}.
	 *
	 * @param parser
	 *            the parser to use
	 */
	public void setParser(AntlrParser parser) {
		this.parser = parser;
	}

	@Override
	protected void checkSetup() {
		if (parser == null) {
			throw new IllegalStateException("parser must be set in order to evaluate");
		}
	}

	@Override
	protected ExpressionEvaluator compute(String expressionString) {
		// if subexpression is a missing nominal don't feed it into the parser
		if (expressionString == null) {
			return new SimpleExpressionEvaluator((String) null, ExpressionType.STRING);
		}
		try {
			return parser.parseToEvaluator(expressionString);
		} catch (ExpressionParsingException | ExpressionException e) {
			throw new SubexpressionEvaluationException(getFunctionName(), expressionString, e);
		}

	}


}
