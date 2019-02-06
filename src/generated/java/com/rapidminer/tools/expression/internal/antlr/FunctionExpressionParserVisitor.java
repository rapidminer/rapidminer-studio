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
// Generated from FunctionExpressionParser.g4 by ANTLR 4.5
package com.rapidminer.tools.expression.internal.antlr;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;


/**
 * This interface defines a complete generic visitor for a parse tree produced by
 * {@link FunctionExpressionParser}.
 *
 * @param <T>
 *            The return type of the visit operation. Use {@link Void} for operations with no return
 *            type.
 */
public interface FunctionExpressionParserVisitor<T> extends ParseTreeVisitor<T> {

	/**
	 * Visit a parse tree produced by {@link FunctionExpressionParser#operationExp}.
	 * 
	 * @param ctx
	 *            the parse tree
	 * @return the visitor result
	 */
	T visitOperationExp(FunctionExpressionParser.OperationExpContext ctx);

	/**
	 * Visit a parse tree produced by {@link FunctionExpressionParser#atomExp}.
	 * 
	 * @param ctx
	 *            the parse tree
	 * @return the visitor result
	 */
	T visitAtomExp(FunctionExpressionParser.AtomExpContext ctx);

	/**
	 * Visit a parse tree produced by {@link FunctionExpressionParser#lowerExp}.
	 * 
	 * @param ctx
	 *            the parse tree
	 * @return the visitor result
	 */
	T visitLowerExp(FunctionExpressionParser.LowerExpContext ctx);

	/**
	 * Visit a parse tree produced by {@link FunctionExpressionParser#function}.
	 * 
	 * @param ctx
	 *            the parse tree
	 * @return the visitor result
	 */
	T visitFunction(FunctionExpressionParser.FunctionContext ctx);

	/**
	 * Visit a parse tree produced by {@link FunctionExpressionParser#attribute}.
	 * 
	 * @param ctx
	 *            the parse tree
	 * @return the visitor result
	 */
	T visitAttribute(FunctionExpressionParser.AttributeContext ctx);

	/**
	 * Visit a parse tree produced by {@link FunctionExpressionParser#scopeConstant}.
	 * 
	 * @param ctx
	 *            the parse tree
	 * @return the visitor result
	 */
	T visitScopeConstant(FunctionExpressionParser.ScopeConstantContext ctx);

	/**
	 * Visit a parse tree produced by {@link FunctionExpressionParser#indirectScopeConstant}.
	 * 
	 * @param ctx
	 *            the parse tree
	 * @return the visitor result
	 */
	T visitIndirectScopeConstant(FunctionExpressionParser.IndirectScopeConstantContext ctx);

	/**
	 * Visit a parse tree produced by {@link FunctionExpressionParser#string}.
	 * 
	 * @param ctx
	 *            the parse tree
	 * @return the visitor result
	 */
	T visitString(FunctionExpressionParser.StringContext ctx);

	/**
	 * Visit a parse tree produced by {@link FunctionExpressionParser#variable}.
	 * 
	 * @param ctx
	 *            the parse tree
	 * @return the visitor result
	 */
	T visitVariable(FunctionExpressionParser.VariableContext ctx);

	/**
	 * Visit a parse tree produced by {@link FunctionExpressionParser#real}.
	 * 
	 * @param ctx
	 *            the parse tree
	 * @return the visitor result
	 */
	T visitReal(FunctionExpressionParser.RealContext ctx);

	/**
	 * Visit a parse tree produced by {@link FunctionExpressionParser#integer}.
	 * 
	 * @param ctx
	 *            the parse tree
	 * @return the visitor result
	 */
	T visitInteger(FunctionExpressionParser.IntegerContext ctx);
}
