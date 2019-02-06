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
package com.rapidminer.tools.expression.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.rapidminer.tools.expression.DoubleCallable;
import com.rapidminer.tools.expression.ExpressionContext;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.Function;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInput;
import com.rapidminer.tools.expression.Resolver;


/**
 * Provides expression-specific information, such as the function mapping, constants and variables.
 * Stores all {@link Function}s and has access to all variables, dynamic variables and scope
 * constants that can be used during such an evaluation via their {@link Resolver}s.
 *
 * @author Gisa Schaefer
 *
 */
public class SimpleExpressionContext implements ExpressionContext {

	private Map<String, Function> functionMap;

	private List<Resolver> dynamicResolvers;
	private List<Resolver> scopeResolvers;
	private List<Resolver> constantResolvers;

	/**
	 * Creates a {@link ExpressionContext} that uses the given functions and resolvers.
	 *
	 * @param functions
	 *            the functions to use in expressions
	 * @param scopeResolvers
	 *            the scope resolvers to use
	 * @param dynamicResolvers
	 *            the resolvers for dynamic variables to use
	 * @param constantResolvers
	 *            the resolvers for constants
	 */
	public SimpleExpressionContext(List<Function> functions, List<Resolver> scopeResolvers, List<Resolver> dynamicResolvers,
			List<Resolver> constantResolvers) {
		this.scopeResolvers = scopeResolvers;
		this.dynamicResolvers = dynamicResolvers;
		this.constantResolvers = constantResolvers;

		this.functionMap = new LinkedHashMap<>();
		for (Function function : functions) {
			// take the first function with a certain function name if there is more than one
			if (!this.functionMap.containsKey(function.getFunctionName())) {
				this.functionMap.put(function.getFunctionName(), function);
			}
		}
	}

	@Override
	public Function getFunction(String functionName) {
		return functionMap.get(functionName);
	}

	@Override
	public ExpressionEvaluator getVariable(String variableName) {
		// A variable can either be a constant coming from a {@link Resolver} for constants or a
		// dynamic variable. This is done to keep compatibility with the old parser where
		// alpha-numeric strings could stand for constants or attribute values. Attribute values are
		// now a special case of dynamic variables.
		ExpressionEvaluator constant = getConstant(variableName);
		if (constant != null) {
			return constant;
		} else {
			return getDynamicVariable(variableName);
		}
	}

	/**
	 * Looks for the first resolver in the resolvers list that knows the variableName.
	 *
	 * @param variableName
	 *            the name to look for
	 */
	private Resolver getResolverWithKnowledge(List<Resolver> resolvers, String variableName) {
		for (Resolver resolver : resolvers) {
			if (resolver.getVariableType(variableName) != null) {
				return resolver;
			}
		}
		return null;
	}

	/**
	 * Creates an constant {@link ExpressionEvaluator} for the variableName using the resolver.
	 */
	private ExpressionEvaluator getExpressionEvaluator(String variableName, Resolver resolver) {
		ExpressionType type = resolver.getVariableType(variableName);
		switch (type) {
			case DOUBLE:
			case INTEGER:
				return new SimpleExpressionEvaluator(resolver.getDoubleValue(variableName), type);
			case DATE:
				return new SimpleExpressionEvaluator(resolver.getDateValue(variableName), type);
			case STRING:
				return new SimpleExpressionEvaluator(resolver.getStringValue(variableName), type);
			case BOOLEAN:
				return new SimpleExpressionEvaluator(resolver.getBooleanValue(variableName), type);
			default:
				return null;
		}
	}

	@Override
	public ExpressionEvaluator getDynamicVariable(String variableName) {
		Resolver resolver = getResolverWithKnowledge(dynamicResolvers, variableName);
		if (resolver == null) {
			return null;
		}
		return getDynamicExpressionEvaluator(variableName, resolver);
	}

	/**
	 * Creates an non-constant {@link ExpressionEvaluator} for the variableName using the resolver.
	 */
	private ExpressionEvaluator getDynamicExpressionEvaluator(final String variableName, final Resolver resolver) {
		ExpressionType type = resolver.getVariableType(variableName);
		switch (type) {
			case DOUBLE:
			case INTEGER:
				DoubleCallable doubleCallable = new DoubleCallable() {

					@Override
					public double call() throws Exception {
						return resolver.getDoubleValue(variableName);
					}

				};
				return new SimpleExpressionEvaluator(doubleCallable, type, false);
			case DATE:
				Callable<Date> dateCallable = new Callable<Date>() {

					@Override
					public Date call() throws Exception {
						return resolver.getDateValue(variableName);
					}

				};
				return new SimpleExpressionEvaluator(type, dateCallable, false);
			case STRING:
				Callable<String> stringCallable = new Callable<String>() {

					@Override
					public String call() throws Exception {
						return resolver.getStringValue(variableName);
					}

				};
				return new SimpleExpressionEvaluator(stringCallable, type, false);
			case BOOLEAN:
				Callable<Boolean> booleanCallable = new Callable<Boolean>() {

					@Override
					public Boolean call() throws Exception {
						return resolver.getBooleanValue(variableName);
					}

				};
				return new SimpleExpressionEvaluator(booleanCallable, false, type);
			default:
				return null;
		}
	}

	@Override
	public ExpressionEvaluator getScopeConstant(String scopeName) {
		Resolver resolver = getResolverWithKnowledge(scopeResolvers, scopeName);
		if (resolver == null) {
			return null;
		}
		return getExpressionEvaluator(scopeName, resolver);
	}

	@Override
	public String getScopeString(String scopeName) {
		Resolver resolver = getResolverWithKnowledge(scopeResolvers, scopeName);
		if (resolver == null) {
			return null;
		}

		ExpressionType type = resolver.getVariableType(scopeName);
		switch (type) {
			case DOUBLE:
			case INTEGER:
				return resolver.getDoubleValue(scopeName) + "";
			case DATE:
				return resolver.getDateValue(scopeName).toString();
			case STRING:
				return resolver.getStringValue(scopeName);
			case BOOLEAN:
				return resolver.getBooleanValue(scopeName) + "";
			default:
				return null;
		}

	}

	@Override
	public List<FunctionDescription> getFunctionDescriptions() {
		List<FunctionDescription> descriptions = new ArrayList<>(functionMap.size());
		for (Function function : functionMap.values()) {
			descriptions.add(function.getFunctionDescription());
		}
		return descriptions;
	}

	@Override
	public List<FunctionInput> getFunctionInputs() {
		List<FunctionInput> allFunctionInputs = new LinkedList<>();
		for (Resolver resolver : dynamicResolvers) {
			allFunctionInputs.addAll(resolver.getAllVariables());
		}
		for (Resolver resolver : constantResolvers) {
			allFunctionInputs.addAll(resolver.getAllVariables());
		}
		for (Resolver resolver : scopeResolvers) {
			allFunctionInputs.addAll(resolver.getAllVariables());
		}
		return allFunctionInputs;
	}

	@Override
	public ExpressionEvaluator getConstant(String constantName) {
		Resolver resolver = getResolverWithKnowledge(constantResolvers, constantName);
		if (resolver != null) {
			return getExpressionEvaluator(constantName, resolver);
		} else {
			return null;
		}
	}
}
