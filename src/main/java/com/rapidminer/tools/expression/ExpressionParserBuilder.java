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
package com.rapidminer.tools.expression;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.Process;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.tools.expression.internal.ConstantResolver;
import com.rapidminer.tools.expression.internal.SimpleExpressionContext;
import com.rapidminer.tools.expression.internal.antlr.AntlrParser;
import com.rapidminer.tools.expression.internal.function.eval.AttributeEvaluation;
import com.rapidminer.tools.expression.internal.function.eval.Evaluation;
import com.rapidminer.tools.expression.internal.function.eval.TypeConstants;
import com.rapidminer.tools.expression.internal.function.process.MacroValue;
import com.rapidminer.tools.expression.internal.function.process.ParameterValue;
import com.rapidminer.tools.expression.internal.function.statistical.Random;


/**
 * Builder for an {@link ExpressionParser}.
 *
 * @author Gisa Schaefer
 * @since 6.5.0
 */
public class ExpressionParserBuilder {

	/**
	 * The last version which contained the old expression parser with different functions and
	 * different Macro handling.
	 */
	public static final OperatorVersion OLD_EXPRESSION_PARSER_FUNCTIONS = new OperatorVersion(6, 4, 0);

	private Process process;
	private boolean compatibleWithOldParser;

	private List<Function> functions = new LinkedList<>();

	private List<Resolver> scopeResolvers = new LinkedList<>();
	private List<Resolver> dynamicsResolvers = new LinkedList<>();
	private List<Resolver> constantResolvers = new LinkedList<>();

	/**
	 * Builds an {@link ExpressionParser} with the given data.
	 *
	 * @return an expression parser
	 */
	public ExpressionParser build() {
		// add functions with process information
		if (process != null) {
			functions.add(new Random(process));
			functions.add(new ParameterValue(process));
			if (compatibleWithOldParser) {
				functions.add(new MacroValue(process));
			}
		}

		Evaluation evalFunction = null;
		if (!compatibleWithOldParser) {
			// add the eval function, always present except when in compatibility mode with old
			// parser
			evalFunction = new Evaluation();
			functions.add(evalFunction);
		}
		//add the attribute eval function
		AttributeEvaluation attributeEvalFunction = new AttributeEvaluation();
		functions.add(attributeEvalFunction);

		// add eval constants
		constantResolvers.add(new ConstantResolver(TypeConstants.INSTANCE.getKey(), TypeConstants.INSTANCE.getConstants()));

		ExpressionContext context = new SimpleExpressionContext(functions, scopeResolvers, dynamicsResolvers,
				constantResolvers);
		AntlrParser parser = new AntlrParser(context);

		if (!compatibleWithOldParser) {
			// set parser for eval function
			evalFunction.setParser(parser);
		}
		attributeEvalFunction.setContext(context);

		return parser;
	}

	/**
	 * Adds the process which enables process dependent functions.
	 *
	 * @param process
	 *            the process to add
	 * @return the builder
	 */
	public ExpressionParserBuilder withProcess(Process process) {
		this.process = process;
		return this;
	}

	/**
	 * Adds the resolver as a resolver for scope constants (%{scope_constant} in the expression).
	 *
	 * @param resolver
	 *            the resolver to add
	 * @return the builder
	 */
	public ExpressionParserBuilder withScope(Resolver resolver) {
		scopeResolvers.add(resolver);
		return this;
	}

	/**
	 * Adds the resolver as a resolver for dynamic variables ([variable_name] or variable_name in
	 * the expression).
	 *
	 * @param resolver
	 *            the resolver to add
	 * @return the builder
	 */
	public ExpressionParserBuilder withDynamics(Resolver resolver) {
		dynamicsResolvers.add(resolver);
		return this;
	}

	/**
	 * Adds the given module that supplies functions and constant values.
	 *
	 * @param module
	 *            the module to add
	 * @return the builder
	 */
	public ExpressionParserBuilder withModule(ExpressionParserModule module) {
		addModule(module);
		return this;
	}

	/**
	 * Adds all functions of the module to the list of functions and adds a {@link ConstantResolver}
	 * knowing all constants of the module to the list of constant resolver.
	 *
	 * @param module
	 */
	private void addModule(ExpressionParserModule module) {
		List<Constant> moduleConstants = module.getConstants();
		if (moduleConstants != null && !moduleConstants.isEmpty()) {
			constantResolvers.add(new ConstantResolver(module.getKey(), moduleConstants));
		}
		List<Function> moduleFunctions = module.getFunctions();
		if (moduleFunctions != null) {
			functions.addAll(moduleFunctions);
		}
	}

	/**
	 * Adds the given modules that supplies functions and constant values.
	 *
	 * @param modules
	 *            the modules to add
	 * @return the builder
	 */
	public ExpressionParserBuilder withModules(List<ExpressionParserModule> modules) {
		for (ExpressionParserModule module : modules) {
			addModule(module);
		}
		return this;
	}

	/**
	 * Adds the functions that are no longer used after version 6.4 if version is at most 6.4.
	 *
	 * @param version
	 *            the version of the associated operator
	 * @return the builder
	 */
	public ExpressionParserBuilder withCompatibility(OperatorVersion version) {
		if (version.isAtMost(OLD_EXPRESSION_PARSER_FUNCTIONS)) {
			compatibleWithOldParser = true;
		}
		return this;
	}

}
