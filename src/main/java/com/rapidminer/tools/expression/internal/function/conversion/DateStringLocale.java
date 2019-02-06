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
import java.util.Locale;
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
 * A {@link Function} parsing a date to a string with respect to the size, format and locale.
 *
 * @author Marcel Seifert
 *
 */
public class DateStringLocale extends AbstractFunction {

	/**
	 * Constructs an AbstractFunction with {@link FunctionDescription} generated from the arguments
	 * and the function name generated from the description.
	 */
	public DateStringLocale() {
		super("conversion.date_str_loc", 4, Ontology.NOMINAL);
	}

	@Override
	public ExpressionEvaluator compute(ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 4) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), 4,
					inputEvaluators.length);
		}
		ExpressionType type = getResultType(inputEvaluators);

		ExpressionEvaluator date = inputEvaluators[0];
		ExpressionEvaluator size = inputEvaluators[1];
		ExpressionEvaluator format = inputEvaluators[2];
		ExpressionEvaluator locale = inputEvaluators[3];

		return new SimpleExpressionEvaluator(makeStringCallable(date, size, format, locale), type,
				isResultConstant(inputEvaluators));
	}

	/**
	 * Builds a String Callable from one date and three string arguments
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
			final ExpressionEvaluator format, final ExpressionEvaluator locale) {

		final Callable<Date> funcDate = date.getDateFunction();
		final Callable<String> funcSize = size.getStringFunction();
		final Callable<String> funcFormat = format.getStringFunction();
		final Callable<String> funcLocale = locale.getStringFunction();

		try {
			final Date valueDate = date.isConstant() ? funcDate.call() : null;
			final String valueSize = size.isConstant() ? funcSize.call() : null;
			final String valueFormat = format.isConstant() ? funcFormat.call() : null;
			final String valueLocale = locale.isConstant() ? funcLocale.call() : null;

			if (locale.isConstant()) {
				if (size.isConstant()) {
					if (date.isConstant() && format.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(valueDate, valueSize, valueFormat, valueLocale);
							}
						};
					} else if (date.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(valueDate, valueSize, funcFormat.call(), valueLocale);
							}

						};

					} else if (format.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcDate.call(), valueSize, valueFormat, valueLocale);
							}

						};

					} else {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcDate.call(), valueSize, funcFormat.call(), valueLocale);
							}
						};
					}
				} else {
					if (date.isConstant() && format.isConstant()) {

						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(valueDate, funcSize.call(), valueFormat, valueLocale);
							}
						};
					} else if (date.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(valueDate, funcSize.call(), funcFormat.call(), valueLocale);
							}

						};

					} else if (format.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcDate.call(), funcSize.call(), valueFormat, valueLocale);
							}

						};

					} else {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcDate.call(), funcSize.call(), funcFormat.call(), valueLocale);
							}
						};
					}
				}
			} else {
				if (size.isConstant()) {
					if (date.isConstant() && format.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(valueDate, valueSize, valueFormat, funcLocale.call());
							}
						};
					} else if (date.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(valueDate, valueSize, funcFormat.call(), funcLocale.call());
							}

						};

					} else if (format.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcDate.call(), valueSize, valueFormat, funcLocale.call());
							}

						};

					} else {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcDate.call(), valueSize, funcFormat.call(), funcLocale.call());
							}
						};
					}
				} else {
					if (date.isConstant() && format.isConstant()) {

						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(valueDate, funcSize.call(), valueFormat, funcLocale.call());
							}
						};
					} else if (date.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(valueDate, funcSize.call(), funcFormat.call(), funcLocale.call());
							}

						};

					} else if (format.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcDate.call(), funcSize.call(), valueFormat, funcLocale.call());
							}

						};

					} else {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcDate.call(), funcSize.call(), funcFormat.call(), funcLocale.call());
							}
						};
					}
				}
			}
		} catch (ExpressionParsingException e) {
			throw e;
		} catch (Exception e) {
			throw new ExpressionParsingException(e);
		}
	}

	/**
	 * Computes the result for one date and three string input values.
	 *
	 * @param dateDate
	 *            the date
	 * @param sizeString
	 *            the sizeString from {@link ExpressionParserConstants}
	 * @param formatString
	 *            the formatString from {@link ExpressionParserConstants}
	 * @param localeString
	 *            the locale string
	 * @return the result of the computation.
	 */
	protected String compute(Date dateDate, String sizeString, String formatString, String localeString) {
		if (dateDate == null || sizeString == null || formatString == null || localeString == null) {
			return null;
		}

		String result;
		DateFormat dateFormat;
		Locale locale = new Locale(localeString);
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
			dateFormat = (DateFormat) DateFormat.getDateInstance(formatting, locale).clone();
		} else if (formatString.equals(ExpressionParserConstants.DATE_SHOW_TIME_ONLY)) {
			// clone because getDateInstance uses an internal pool which can return the
			// same instance for multiple threads
			dateFormat = (DateFormat) DateFormat.getTimeInstance(formatting, locale).clone();
		} else if (formatString.equals(ExpressionParserConstants.DATE_SHOW_DATE_AND_TIME)) {
			// clone because getDateInstance uses an internal pool which can return the
			// same instance for multiple threads
			dateFormat = (DateFormat) DateFormat.getDateTimeInstance(formatting, formatting, locale).clone();
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
		ExpressionType locale = inputTypes[3];
		if (date != ExpressionType.DATE) {
			throw new FunctionInputException("expression_parser.function_wrong_type.argument", 1, getFunctionName(), "date");
		} else if (size != ExpressionType.STRING) {
			throw new FunctionInputException("expression_parser.function_wrong_type.argument", 2, getFunctionName(),
					"string");
		} else if (format != ExpressionType.STRING) {
			throw new FunctionInputException("expression_parser.function_wrong_type.argument", 3, getFunctionName(),
					"string");
		} else if (locale != ExpressionType.STRING) {
			throw new FunctionInputException("expression_parser.function_wrong_type.argument", 4, getFunctionName(),
					"string");
		} else {
			return ExpressionType.STRING;
		}
	}

}
