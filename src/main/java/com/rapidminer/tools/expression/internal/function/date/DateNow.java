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
package com.rapidminer.tools.expression.internal.function.date;

import java.util.Calendar;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.SimpleExpressionEvaluator;
import com.rapidminer.tools.expression.internal.function.AbstractFunction;


/**
 * A {@link Function} that returns the current date.
 *
 * @author David Arnu
 *
 */
public class DateNow extends AbstractFunction {

	public DateNow() {
		super("date.date_now", 0, Ontology.DATE_TIME);
	}

	@Override
	public ExpressionEvaluator compute(ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length > 0) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), 0,
					inputEvaluators.length);
		}
		ExpressionType resultType = getResultType(inputEvaluators);
		return new SimpleExpressionEvaluator(Calendar.getInstance().getTime(), resultType);
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {

		return ExpressionType.DATE;
	}

	@Override
	protected boolean isConstantOnConstantInput() {
		return false;
	}
}
