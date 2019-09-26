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

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionContext;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParser;
import com.rapidminer.tools.expression.ExpressionParsingException;


/**
 * Parser using antlr.
 *
 * @author Gisa Schaefer
 *
 */
public class AntlrParser implements ExpressionParser {

	private ExpressionContext lookup;

	/**
	 * Creates a Parser that parses using antlr.
	 *
	 * @param lookUp
	 *            the {@link ExpressionContext} for looking up functions, variables and macros
	 */
	public AntlrParser(ExpressionContext lookup) {
		this.lookup = lookup;
	}

	/**
	 * Parses the expression using antlr, aborts the parsing on the first error.
	 *
	 * @param expression
	 *            an expression, not {@code null}
	 * @return a {@link ParseTree} for further processing
	 * @throws ExpressionException
	 */
	ParseTree parseExpression(String expression) throws ExpressionException {
		if (expression == null) {
			throw new IllegalArgumentException("expression must not be null");
		}
		ANTLRInputStream in = new ANTLRInputStream(expression);
		FunctionExpressionLexer lexer = new CapitulatingFunctionExpressionLexer(in);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		FunctionExpressionParser parser = new FunctionExpressionParser(tokens);

		parser.removeErrorListeners();
		lexer.removeErrorListeners();
		ExpressionErrorListener listener = new ExpressionErrorListener();
		parser.addErrorListener(listener);
		lexer.addErrorListener(listener);
		parser.setErrorHandler(new CapitulatingErrorStrategy());

		try {
			ParseTree tree = parser.operationExp();
			if (listener.containsError()) {
				throw new ExpressionException(listener.getErrorMessage(), listener.getErrorLine());
			} else {
				return tree;
			}
		} catch (CapitulatingRuntimeException e) {
			if (listener.containsError()) {
				throw new ExpressionException(listener.getErrorMessage(), listener.getErrorLine());
			} else {
				// cannot happen since the parser and lexer always register the error before trying
				// to recover
				throw new ExpressionException("Unknown error");
			}
		}

	}

	@Override
	public void checkSyntax(String expression) throws ExpressionException {
		ParseTree tree = parseExpression(expression);
		ParseTreeWalker walker = new ParseTreeWalker();
		FunctionListener listener = new FunctionListener(lookup);
		try {
			walker.walk(listener, tree);
		} catch (ExpressionParsingException e) {
			throw new ExpressionException(e);
		}
	}

	@Override
	public Expression parse(String expression) throws ExpressionException {
		try {
			ExpressionEvaluator evaluator = parseToEvaluator(expression);
			return new SimpleExpression(evaluator);
		} catch (ExpressionParsingException e) {
			throw new ExpressionException(e);
		}
	}

	/**
	 * Parses the expression to a tree and creates an {@link ExpressionEvaluator} out of it.
	 *
	 * @param expression
	 *            the expression to parse
	 * @return the ExpressionEvaluator for the result
	 * @throws ExpressionParsingException
	 *             if the creation of the ExpressionEvaluator failed
	 * @throws ExpressionException
	 *             if the parsing failed
	 */
	public ExpressionEvaluator parseToEvaluator(String expression) throws ExpressionParsingException, ExpressionException {
		ParseTree tree = parseExpression(expression);
		return new EvaluatorCreationVisitor(lookup).visit(tree);
	}

	@Override
	public ExpressionContext getExpressionContext() {
		return lookup;
	}
}
