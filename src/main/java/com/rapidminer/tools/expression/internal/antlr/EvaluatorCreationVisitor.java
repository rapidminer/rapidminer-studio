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
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.Function;
import com.rapidminer.tools.expression.internal.SimpleExpressionEvaluator;
import com.rapidminer.tools.expression.internal.antlr.FunctionExpressionParser.AttributeContext;
import com.rapidminer.tools.expression.internal.antlr.FunctionExpressionParser.FunctionContext;
import com.rapidminer.tools.expression.internal.antlr.FunctionExpressionParser.IndirectScopeConstantContext;
import com.rapidminer.tools.expression.internal.antlr.FunctionExpressionParser.IntegerContext;
import com.rapidminer.tools.expression.internal.antlr.FunctionExpressionParser.LowerExpContext;
import com.rapidminer.tools.expression.internal.antlr.FunctionExpressionParser.OperationExpContext;
import com.rapidminer.tools.expression.internal.antlr.FunctionExpressionParser.RealContext;
import com.rapidminer.tools.expression.internal.antlr.FunctionExpressionParser.ScopeConstantContext;
import com.rapidminer.tools.expression.internal.antlr.FunctionExpressionParser.StringContext;
import com.rapidminer.tools.expression.internal.antlr.FunctionExpressionParser.VariableContext;


/**
 * Visitor that recursively builds an {@link ExpressionEvaluator}. Specifies what should happen at
 * every node of the {@link ParseTree}.
 *
 * @author Gisa Schaefer
 *
 */
class EvaluatorCreationVisitor extends FunctionExpressionParserBaseVisitor<ExpressionEvaluator> {

	private final ExpressionContext lookUp;

	/**
	 * Creates a Visitor that recursively builds an {@link ExpressionEvaluator}.
	 *
	 * @param lookUp
	 *            the {@link ExpressionContext} for looking up functions, variables and scope
	 *            constants
	 */
	EvaluatorCreationVisitor(ExpressionContext lookUp) {
		this.lookUp = lookUp;
	}

	@Override
	public ExpressionEvaluator visitOperationExp(OperationExpContext ctx) {
		if (ctx.op == null) {
			return visit(ctx.atomExp());
		} else {

			if (ctx.operationExp().size() == 1) {
				ExpressionEvaluator right = visit(ctx.operationExp(0));

				String operatorName = ctx.op.getText();
				Function function = lookUp.getFunction(ctx.op.getText());
				if (function == null) {
					throw new UnknownFunctionException(ctx, "expression_parser.unknown_operator", operatorName);
				}
				return function.compute(right);

			} else {
				ExpressionEvaluator left = visit(ctx.operationExp(0));
				ExpressionEvaluator right = visit(ctx.operationExp(1));

				String operatorName = ctx.op.getText();
				Function function = lookUp.getFunction(ctx.op.getText());
				if (function == null) {
					throw new UnknownFunctionException(ctx, "expression_parser.unknown_operator", operatorName);
				}
				return function.compute(left, right);
			}
		}
	}

	@Override
	public ExpressionEvaluator visitLowerExp(LowerExpContext ctx) {
		return visit(ctx.operationExp());
	}

	@Override
	public ExpressionEvaluator visitFunction(FunctionContext ctx) {

		int numberOfInner = ctx.operationExp().size();
		ExpressionEvaluator[] innerEvaluators = new ExpressionEvaluator[numberOfInner];
		for (int i = 0; i < numberOfInner; i++) {
			innerEvaluators[i] = visit(ctx.operationExp(i));
		}

		String functionName = ctx.NAME().getText();
		Function function = lookUp.getFunction(functionName);
		if (function == null) {
			throw new UnknownFunctionException(ctx, "expression_parser.unknown_function", functionName);
		}

		return function.compute(innerEvaluators);
	}

	@Override
	public ExpressionEvaluator visitAttribute(AttributeContext ctx) {
		String attributeName = getAttributeName(ctx.getText());
		ExpressionEvaluator attributeEvaluator = lookUp.getDynamicVariable(attributeName);
		if (attributeEvaluator == null) {
			throw new UnknownDynamicVariableException(ctx, "expression_parser.unknown_attribute", attributeName);
		}
		return attributeEvaluator;
	}

	/**
	 * Deletes the enclosing [ ], unescapes [ ] and \
	 *
	 * @param text
	 *            a ATTRIBUTE as defined in FunctionExpressionLexer.g4
	 * @return
	 */
	private String getAttributeName(String text) {
		String attributeName = text.substring(1, text.length() - 1);
		return attributeName.replace("\\[", "[").replace("\\]", "]").replace("\\\\", "\\");
	}

	@Override
	public ExpressionEvaluator visitVariable(VariableContext ctx) {
		String name = ctx.getText();
		ExpressionEvaluator variableEvaluator = lookUp.getVariable(name);
		if (variableEvaluator == null) {
			throw new UnknownVariableException(ctx, "expression_parser.unknown_variable", name);
		}
		return variableEvaluator;
	}

	@Override
	public ExpressionEvaluator visitScopeConstant(ScopeConstantContext ctx) {
		String scopeConstantName = getScopeConstantName(ctx.getText());
		ExpressionEvaluator scopeConstantEvaluator = lookUp.getScopeConstant(scopeConstantName);
		if (scopeConstantEvaluator == null) {
			throw new UnknownScopeConstantException(ctx, "expression_parser.unknown_scope", scopeConstantName);
		}
		return scopeConstantEvaluator;
	}

	/**
	 * Deletes the enclosing %{ } or #{ }, unescapes {,} and \
	 *
	 * @param text
	 *            a SCOPE_CONSTANT or INDIRECT_SCOPE_CONSTANT as defined in
	 *            FunctionExpressionLexer.g4
	 * @return
	 */
	private String getScopeConstantName(String text) {
		String scopeName = text.substring(2, text.length() - 1);
		return scopeName.replace("\\{", "{").replace("\\}", "}").replace("\\\\", "\\");
	}

	@Override
	public ExpressionEvaluator visitIndirectScopeConstant(IndirectScopeConstantContext ctx) {
		String scopeConstantName = getScopeConstantName(ctx.getText());
		String attributeName = lookUp.getScopeString(scopeConstantName);
		if (attributeName == null) {
			throw new UnknownScopeConstantException(ctx, "expression_parser.unknown_scope", scopeConstantName);
		}
		ExpressionEvaluator attributeEvaluator = lookUp.getDynamicVariable(attributeName);
		if (attributeEvaluator == null) {
			throw new UnknownDynamicVariableException(ctx, "expression_parser.unknown_attribute_in_scope", attributeName,
					scopeConstantName);
		}
		return attributeEvaluator;
	}

	@Override
	public ExpressionEvaluator visitString(StringContext ctx) {
		String stringValue = getStringValue(ctx.getText());
		return new SimpleExpressionEvaluator(stringValue, ExpressionType.STRING);
	}

	/**
	 * Unescapes escaped sequences and replaces tabs and newlines by spaces.
	 *
	 * @param text
	 *            a STRING as defined in FunctionExpressionLexer.g4
	 * @return
	 */
	private String getStringValue(String text) {
		// delete leading and trailing "
		text = text.substring(1, text.length() - 1);
		// unescape
		text = text.replace("\\\"", "\"").replace("\\\\", "\\");
		// replace \\u**** by associated unicode character
		int pos = text.indexOf("\\u");
		while (pos >= 0) {
			text = text.substring(0, pos) + (char) Integer.parseInt(text.substring(pos + 2, pos + 6), 16)
					+ text.substring(pos + 6, text.length());
			pos = text.indexOf("\\u");
		}
		// replace tabs and line breaks by spaces
		text = text.replace("\t", " ").replace("\r\n", " ").replace("\n", " ");
		return text;
	}

	@Override
	public ExpressionEvaluator visitReal(RealContext ctx) {
		double doubleValue = Double.parseDouble(ctx.getText());
		return new SimpleExpressionEvaluator(doubleValue, ExpressionType.DOUBLE);

	}

	@Override
	public ExpressionEvaluator visitInteger(IntegerContext ctx) {
		double doubleValue = Double.parseDouble(ctx.getText());
		return new SimpleExpressionEvaluator(doubleValue, ExpressionType.INTEGER);
	}

}
