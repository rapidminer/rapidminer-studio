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
package com.rapidminer.operator.meta.branch;

import com.rapidminer.generator.GenerationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParserBuilder;
import com.rapidminer.tools.expression.ExpressionRegistry;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.MacroResolver;


/**
 * This condition will parse the condition value as an expression and return it's boolean value if
 * possible. Otherwise an error will be thrown.
 *
 * @author Sebastian Land
 */
public class ExpressionCondition implements ProcessBranchCondition {

	@Override
	public boolean check(ProcessBranch operator, String value) throws OperatorException {

		if (operator == null) {
			throw new IllegalArgumentException("Operator must not be null");
		}

		// lazy init of ExpressionParser
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		builder.withModules(ExpressionRegistry.INSTANCE.getAll());

		// decide which functions to use
		builder.withCompatibility(operator.getCompatibilityLevel());

		if (operator.getProcess() != null) {
			builder.withProcess(operator.getProcess());
			builder.withScope(new MacroResolver(operator.getProcess().getMacroHandler(), operator));
		}

		// check for errors
		Expression parsedValue;
		try {
			parsedValue = builder.build().parse(value);
		} catch (ExpressionException e) {
			throw new GenerationException(e.getShortMessage());
		}

		try {
			if (parsedValue.getExpressionType() == ExpressionType.DOUBLE
					|| parsedValue.getExpressionType() == ExpressionType.INTEGER) {
				double resultValue = parsedValue.evaluateNumerical();

				if (Tools.isZero(resultValue) || Tools.isEqual(resultValue, 1d)) {
					return Tools.isEqual(resultValue, 1d);
				}
			} else if (parsedValue.getExpressionType() == ExpressionType.BOOLEAN) {
				Boolean resultValue = parsedValue.evaluateBoolean();
				if (resultValue == null) {
					return false;
				} else {
					return resultValue.booleanValue();
				}
			}
			throw new GenerationException("Must return boolean value");
		} catch (ExpressionException e) {
			throw new GenerationException("'Expression: '" + value + "', Error: '" + e.getShortMessage() + "'");
		}

	}
}
