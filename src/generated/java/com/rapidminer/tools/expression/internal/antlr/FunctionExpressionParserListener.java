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

import org.antlr.v4.runtime.tree.ParseTreeListener;


/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link FunctionExpressionParser}.
 */
public interface FunctionExpressionParserListener extends ParseTreeListener {

	/**
	 * Enter a parse tree produced by {@link FunctionExpressionParser#operationExp}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void enterOperationExp(FunctionExpressionParser.OperationExpContext ctx);

	/**
	 * Exit a parse tree produced by {@link FunctionExpressionParser#operationExp}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void exitOperationExp(FunctionExpressionParser.OperationExpContext ctx);

	/**
	 * Enter a parse tree produced by {@link FunctionExpressionParser#atomExp}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void enterAtomExp(FunctionExpressionParser.AtomExpContext ctx);

	/**
	 * Exit a parse tree produced by {@link FunctionExpressionParser#atomExp}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void exitAtomExp(FunctionExpressionParser.AtomExpContext ctx);

	/**
	 * Enter a parse tree produced by {@link FunctionExpressionParser#lowerExp}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void enterLowerExp(FunctionExpressionParser.LowerExpContext ctx);

	/**
	 * Exit a parse tree produced by {@link FunctionExpressionParser#lowerExp}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void exitLowerExp(FunctionExpressionParser.LowerExpContext ctx);

	/**
	 * Enter a parse tree produced by {@link FunctionExpressionParser#function}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void enterFunction(FunctionExpressionParser.FunctionContext ctx);

	/**
	 * Exit a parse tree produced by {@link FunctionExpressionParser#function}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void exitFunction(FunctionExpressionParser.FunctionContext ctx);

	/**
	 * Enter a parse tree produced by {@link FunctionExpressionParser#attribute}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void enterAttribute(FunctionExpressionParser.AttributeContext ctx);

	/**
	 * Exit a parse tree produced by {@link FunctionExpressionParser#attribute}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void exitAttribute(FunctionExpressionParser.AttributeContext ctx);

	/**
	 * Enter a parse tree produced by {@link FunctionExpressionParser#scopeConstant}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void enterScopeConstant(FunctionExpressionParser.ScopeConstantContext ctx);

	/**
	 * Exit a parse tree produced by {@link FunctionExpressionParser#scopeConstant}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void exitScopeConstant(FunctionExpressionParser.ScopeConstantContext ctx);

	/**
	 * Enter a parse tree produced by {@link FunctionExpressionParser#indirectScopeConstant}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void enterIndirectScopeConstant(FunctionExpressionParser.IndirectScopeConstantContext ctx);

	/**
	 * Exit a parse tree produced by {@link FunctionExpressionParser#indirectScopeConstant}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void exitIndirectScopeConstant(FunctionExpressionParser.IndirectScopeConstantContext ctx);

	/**
	 * Enter a parse tree produced by {@link FunctionExpressionParser#string}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void enterString(FunctionExpressionParser.StringContext ctx);

	/**
	 * Exit a parse tree produced by {@link FunctionExpressionParser#string}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void exitString(FunctionExpressionParser.StringContext ctx);

	/**
	 * Enter a parse tree produced by {@link FunctionExpressionParser#variable}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void enterVariable(FunctionExpressionParser.VariableContext ctx);

	/**
	 * Exit a parse tree produced by {@link FunctionExpressionParser#variable}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void exitVariable(FunctionExpressionParser.VariableContext ctx);

	/**
	 * Enter a parse tree produced by {@link FunctionExpressionParser#real}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void enterReal(FunctionExpressionParser.RealContext ctx);

	/**
	 * Exit a parse tree produced by {@link FunctionExpressionParser#real}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void exitReal(FunctionExpressionParser.RealContext ctx);

	/**
	 * Enter a parse tree produced by {@link FunctionExpressionParser#integer}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void enterInteger(FunctionExpressionParser.IntegerContext ctx);

	/**
	 * Exit a parse tree produced by {@link FunctionExpressionParser#integer}.
	 * 
	 * @param ctx
	 *            the parse tree
	 */
	void exitInteger(FunctionExpressionParser.IntegerContext ctx);
}
