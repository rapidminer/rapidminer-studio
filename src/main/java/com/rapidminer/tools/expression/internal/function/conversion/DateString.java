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
package com.rapidminer.tools.expression.internal.function.conversion;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionParsingException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.ExpressionParserConstants;
import com.rapidminer.tools.expression.internal.SimpleExpressionEvaluator;
import com.rapidminer.tools.expression.internal.function.AbstractFunction;


/**
 *
 * A {@link Function} parsing a date to a string with respect to the size and format.
 *
 * @author Marcel Seifert
 *
 */
public class DateString extends AbstractFunction {

	/**
	 * Constructs an AbstractFunction with {@link FunctionDescription} generated from the arguments
	 * and the function name generated from the description.
	 */
	public DateString() {
		super("conversion.date_str", 3, Ontology.NOMINAL);
	}

	@Override
	public ExpressionEvaluator compute(ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 3) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), 3,
					inputEvaluators.length);
		}
		ExpressionType type = getResultType(inputEvaluators);

		ExpressionEvaluator date = inputEvaluators[0];
		ExpressionEvaluator size = inputEvaluators[1];
		ExpressionEvaluator format = inputEvaluators[2];

		return new SimpleExpressionEvaluator(makeStringCallable(date, size, format), type, isResultConstant(inputEvaluators));
	}

	/**
	 * Builds a String Callable from one date and two string arguments
	 *
	 * @param date
	 *            the input date
	 * @param size
	 *            the input size
	 * @param format
	 *            the input format
	 * @return the resulting callable<String>
	 */
	protected Callable<String> makeStringCallable(final ExpressionEvaluator date, final ExpressionEvaluator size,
			final ExpressionEvaluator format) {

		final Callable<Date> funcDate = date.getDateFunction();
		final Callable<String> funcSize = size.getStringFunction();
		final Callable<String> funcFormat = format.getStringFunction();

		try {
			final Date valueDate = date.isConstant() ? funcDate.call() : null;
			final String valueSize = size.isConstant() ? funcSize.call() : null;
			final String valueFormat = format.isConstant() ? funcFormat.call() : null;

			if (size.isConstant()) {
				if (date.isConstant() && format.isConstant()) {
					return new Callable<String>() {

						@Override
						public String call() throws Exception {
							return compute(valueDate, valueSize, valueFormat);
						}
					};
				} else if (date.isConstant()) {
					return new Callable<String>() {

						@Override
						public String call() throws Exception {
							return compute(valueDate, valueSize, funcFormat.call());
						}

					};

				} else if (format.isConstant()) {
					return new Callable<String>() {

						@Override
						public String call() throws Exception {
							return compute(funcDate.call(), valueSize, valueFormat);
						}

					};

				} else {
					return new Callable<String>() {

						@Override
						public String call() throws Exception {
							return compute(funcDate.call(), valueSize, funcFormat.call());
						}
					};
				}
			} else {
				if (date.isConstant() && format.isConstant()) {

					return new Callable<String>() {

						@Override
						public String call() throws Exception {
							return compute(valueDate, funcSize.call(), valueFormat);
						}
					};
				} else if (date.isConstant()) {
					return new Callable<String>() {

						@Override
						public String call() throws Exception {
							return compute(valueDate, funcSize.call(), funcFormat.call());
						}

					};

				} else if (format.isConstant()) {
					return new Callable<String>() {

						@Override
						public String call() throws Exception {
							return compute(funcDate.call(), funcSize.call(), valueFormat);
						}

					};

				} else {
					return new Callable<String>() {

						@Override
						public String call() throws Exception {
							return compute(funcDate.call(), funcSize.call(), funcFormat.call());
						}
					};
				}
			}
		} catch (ExpressionParsingException e) {
			throw e;
		} catch (Exception e) {
			throw new ExpressionParsingException(e);
		}
	}

	/**
	 * Computes the result for one date and two string input values.
	 *
	 * @param dateDate
	 *            the date
	 * @param sizeString
	 *            from {@link ExpressionParserConstants}
	 * @param formatString
	 *            from {@link ExpressionParserConstants}
	 * @return the result of the computation.
	 */
	protected String compute(Date dateDate, String sizeString, String formatString) {
		if (dateDate == null || sizeString == null || formatString == null) {
			return null;
		}

		String result;
		DateFormat dateFormat;
		int formatting;
		if (sizeString.equals(ExpressionParserConstants.DATE_FORMAT_FULL)) {
			formatting = DateFormat.FULL;
		} else if (sizeString.equals(ExpressionParserConstants.DATE_FORMAT_LONG)) {
			formatting = DateFormat.LONG;
		} else if (sizeString.equals(ExpressionParserConstants.DATE_FORMAT_MEDIUM)) {
			formatting = DateFormat.MEDIUM;
		} else if (sizeString.equals(ExpressionParserConstants.DATE_FORMAT_SHORT)) {
			formatting = DateFormat.SHORT;
		} else {
			throw new FunctionInputException("invalid_argument.date_size", getFunctionName());
		}
		if (formatString.equals(ExpressionParserConstants.DATE_SHOW_DATE_ONLY)) {
			// clone because getDateInstance uses an internal pool which can return the
			// same instance for multiple threads
			dateFormat = (DateFormat) DateFormat.getDateInstance(formatting).clone();
		} else if (formatString.equals(ExpressionParserConstants.DATE_SHOW_TIME_ONLY)) {
			// clone because getDateInstance uses an internal pool which can return the
			// same instance for multiple threads
			dateFormat = (DateFormat) DateFormat.getTimeInstance(formatting).clone();
		} else if (formatString.equals(ExpressionParserConstants.DATE_SHOW_DATE_AND_TIME)) {
			// clone because getDateInstance uses an internal pool which can return the
			// same instance for multiple threads
			dateFormat = (DateFormat) DateFormat.getDateTimeInstance(formatting, formatting).clone();
		} else {
			throw new FunctionInputException("invalid_argument.date_format", getFunctionName());
		}
		result = dateFormat.format(dateDate);
		return result;
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType date = inputTypes[0];
		ExpressionType size = inputTypes[1];
		ExpressionType format = inputTypes[2];
		if (date != ExpressionType.DATE) {
			throw new FunctionInputException("expression_parser.function_wrong_type.argument", 1, getFunctionName(), "date");
		} else if (size != ExpressionType.STRING) {
			throw new FunctionInputException("expression_parser.function_wrong_type.argument", 2, getFunctionName(),
					"string");
		} else if (format != ExpressionType.STRING) {
			throw new FunctionInputException("expression_parser.function_wrong_type.argument", 3, getFunctionName(),
					"string");
		} else {
			return ExpressionType.STRING;
		}
	}

}
