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

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;


/**
 * This class provides an empty implementation of {@link FunctionExpressionParserVisitor}, which can
 * be extended to create a visitor which only needs to handle a subset of the available methods.
 *
 * @param <T>
 *            The return type of the visit operation. Use {@link Void} for operations with no return
 *            type.
 */
public class FunctionExpressionParserBaseVisitor<T> extends AbstractParseTreeVisitor<T> implements
		FunctionExpressionParserVisitor<T> {

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation returns the result of calling {@link #visitChildren} on
	 * {@code ctx}.
	 * </p>
	 */
	@Override
	public T visitOperationExp(FunctionExpressionParser.OperationExpContext ctx) {
		return visitChildren(ctx);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation returns the result of calling {@link #visitChildren} on
	 * {@code ctx}.
	 * </p>
	 */
	@Override
	public T visitAtomExp(FunctionExpressionParser.AtomExpContext ctx) {
		return visitChildren(ctx);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation returns the result of calling {@link #visitChildren} on
	 * {@code ctx}.
	 * </p>
	 */
	@Override
	public T visitLowerExp(FunctionExpressionParser.LowerExpContext ctx) {
		return visitChildren(ctx);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation returns the result of calling {@link #visitChildren} on
	 * {@code ctx}.
	 * </p>
	 */
	@Override
	public T visitFunction(FunctionExpressionParser.FunctionContext ctx) {
		return visitChildren(ctx);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation returns the result of calling {@link #visitChildren} on
	 * {@code ctx}.
	 * </p>
	 */
	@Override
	public T visitAttribute(FunctionExpressionParser.AttributeContext ctx) {
		return visitChildren(ctx);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation returns the result of calling {@link #visitChildren} on
	 * {@code ctx}.
	 * </p>
	 */
	@Override
	public T visitScopeConstant(FunctionExpressionParser.ScopeConstantContext ctx) {
		return visitChildren(ctx);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation returns the result of calling {@link #visitChildren} on
	 * {@code ctx}.
	 * </p>
	 */
	@Override
	public T visitIndirectScopeConstant(FunctionExpressionParser.IndirectScopeConstantContext ctx) {
		return visitChildren(ctx);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation returns the result of calling {@link #visitChildren} on
	 * {@code ctx}.
	 * </p>
	 */
	@Override
	public T visitString(FunctionExpressionParser.StringContext ctx) {
		return visitChildren(ctx);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation returns the result of calling {@link #visitChildren} on
	 * {@code ctx}.
	 * </p>
	 */
	@Override
	public T visitVariable(FunctionExpressionParser.VariableContext ctx) {
		return visitChildren(ctx);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation returns the result of calling {@link #visitChildren} on
	 * {@code ctx}.
	 * </p>
	 */
	@Override
	public T visitReal(FunctionExpressionParser.RealContext ctx) {
		return visitChildren(ctx);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation returns the result of calling {@link #visitChildren} on
	 * {@code ctx}.
	 * </p>
	 */
	@Override
	public T visitInteger(FunctionExpressionParser.IntegerContext ctx) {
		return visitChildren(ctx);
	}
}
