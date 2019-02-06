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

import java.util.Date;
import java.util.concurrent.Callable;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.DoubleCallable;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionParsingException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.SimpleExpressionEvaluator;
import com.rapidminer.tools.expression.internal.function.AbstractFunction;


/**
 * A {@link Function} that returns the time in milliseconds .
 *
 * @author David Arnu
 *
 */
public class DateMillis extends AbstractFunction {

	public DateMillis() {
		super("date.date_millis", 1, Ontology.INTEGER);
	}

	@Override
	public ExpressionEvaluator compute(ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 1) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), 1,
					inputEvaluators.length);
		}
		ExpressionType resultType = getResultType(inputEvaluators);
		return new SimpleExpressionEvaluator(makeDoubleCallable(inputEvaluators[0]), resultType,
				isResultConstant(inputEvaluators));
	}

	private DoubleCallable makeDoubleCallable(ExpressionEvaluator date) {

		final Callable<Date> funcDate = date.getDateFunction();

		try {
			final Date valueDate = date.isConstant() ? funcDate.call() : null;

			if (date.isConstant()) {
				final double result = compute(valueDate);
				return new DoubleCallable() {

					@Override
					public double call() throws Exception {
						return result;
					}

				};

			} else {
				return new DoubleCallable() {

					@Override
					public double call() throws Exception {
						return compute(funcDate.call());
					}

				};
			}
		} catch (ExpressionParsingException e) {
			throw e;
		} catch (Exception e) {
			throw new ExpressionParsingException(e);
		}

	}

	private double compute(Date valueDate) {
		if (valueDate == null) {
			return Double.NaN;
		} else {
			return valueDate.getTime();
		}
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {

		if (inputTypes[0] != ExpressionType.DATE) {
			throw new FunctionInputException("expression_parser.function_wrong_type", getFunctionName(), "date");
		}
		return ExpressionType.INTEGER;
	}

}
