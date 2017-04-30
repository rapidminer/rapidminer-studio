/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.tools.expression.internal.function.eval;

import java.util.Date;
import java.util.concurrent.Callable;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.DoubleCallable;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParsingException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.SimpleExpressionEvaluator;
import com.rapidminer.tools.expression.internal.antlr.AntlrParser;
import com.rapidminer.tools.expression.internal.function.AbstractFunction;


/**
 * A {@link Function} that evaluates subexpressions using an {@link AntlrParser}.
 * <p>
 * If the first input of the eval function is constant, the parser is only called once during the
 * callable-creation step and not again when the resulting {@link Expression} is evaluated. This
 * means that the evaluation of the {@link Expressions} generated from {@code eval("4+3")*[att1]},
 * {@code eval("(4+3)*[att1]")} and {@code 7*[att1]} have the same complexity at evaluation time.
 * <p>
 * If the first argument is not constant, then a second argument is needed to determine the result
 * type and the parser is called every time the resulting {@link Expression} is evaluated. In
 * particular, evaluating {@link Expression}s such as {@code eval("(4+3)*"+[att1],REAL)} is way
 * slower than the examples above.
 *
 * @author Gisa Schaefer
 *
 */
public class Evaluation extends AbstractFunction {

	private AntlrParser parser;

	/**
	 * Creates a evaluation {@link Function}. Before this functions
	 * {@link #compute(ExpressionEvaluator...)} method can be called, a parser needs to be set via
	 * {@link #setParser(AntlrParser)}.
	 */
	public Evaluation() {
		super("process.eval", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS, Ontology.ATTRIBUTE_VALUE);
	}

	/**
	 * Sets the parser that this evaluation function should use. This must always be done before
	 * using {@link #compute(ExpressionEvaluator...)}.
	 *
	 * @param parser
	 *            the parser to use
	 */
	public void setParser(AntlrParser parser) {
		this.parser = parser;
	}

	@Override
	public ExpressionEvaluator compute(final ExpressionEvaluator... inputEvaluators) {
		if (parser == null) {
			throw new IllegalStateException("parser must be set in order to evaluate");
		}
		// check for string inputs
		getResultType(inputEvaluators);

		// Note that if inputEvaluators[0] is constant then this does not mean the result of eval is
		// constant, it only means that the return type is constant. Example: 'eval("attribute_1")'
		// has the constant string input "attribute_1", but will result in a non-constant evaluator
		// that returns the different values of the attribute.

		if (inputEvaluators.length == 1) {

			if (!inputEvaluators[0].isConstant()) {
				throw new FunctionInputException("expression_parser.eval.non_constant_single_argument", getFunctionName());
			}
			// if eval has one constant argument compute it once using the parser
			return compute(inputEvaluators[0]);

		} else if (inputEvaluators.length == 2) {

			if (inputEvaluators[0].isConstant()) {
				// if eval has one constant argument and one type argument compute result of first
				// argument once using the parser and check the type
				return computeAndCheckType(inputEvaluators[0], getExpectedReturnType(inputEvaluators[1]));
			} else {
				// if eval has one non-constant argument and one type argument create callables
				// depending on the type that do the same in the case above
				final ExpressionType expectedType = getExpectedReturnType(inputEvaluators[1]);
				switch (expectedType) {
					case DATE:
						return makeDateEvaluator(expectedType, inputEvaluators[0]);
					case DOUBLE:
					case INTEGER:
						return makeDoubleEvaluator(expectedType, inputEvaluators[0]);
					case BOOLEAN:
						return makeBooleanEvaluator(expectedType, inputEvaluators[0]);
					case STRING:
					default:
						return makeStringEvaluator(expectedType, inputEvaluators[0]);
				}
			}

		} else {
			throw new FunctionInputException("expression_parser.function_wrong_input_two", getFunctionName(), 1, 2,
					inputEvaluators.length);
		}

	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		// check if inputs are strings, otherwise throw exception
		for (ExpressionType type : inputTypes) {
			if (!(type == ExpressionType.STRING)) {
				throw new FunctionInputException("expression_parser.function_wrong_type", getFunctionName(), "nominal");
			}
		}
		// is not used to compute the resultType, only used to check the input types
		return null;
	}

	/**
	 * Evaluates the expression into a String and feeds it to the parser.
	 *
	 * @param subexpressionEvaluator
	 *            the evaluator whose string function call yields the expression string
	 * @return the result of parsing the expression string
	 */
	private ExpressionEvaluator compute(ExpressionEvaluator subexpressionEvaluator) {
		String expressionString = null;
		try {
			expressionString = subexpressionEvaluator.getStringFunction().call();
		} catch (ExpressionParsingException e) {
			throw e;
		} catch (Exception e) {
			throw new ExpressionParsingException(e);
		}
		// if subexpression is a missing nominal don't feed it into the parser
		if (expressionString == null) {
			return new SimpleExpressionEvaluator((String) null, ExpressionType.STRING);
		}
		try {
			return parser.parseToEvaluator(expressionString);
		} catch (ExpressionParsingException | ExpressionException e) {
			throw new SubexpressionEvaluationException(getFunctionName(), expressionString, e);
		}
	}

	/**
	 * Evaluates the expression into a String, feeds it to the parser and checks if the resulting
	 * type is the expected type. If the resulting type is not as expected and the expected type is
	 * String then converts the result to a string evaluator. If the expected type is double and the
	 * result type is integer the result type is changed to double.
	 *
	 * @param subexpressionEvaluator
	 *            the evaluator whose string function call yields the expression string
	 * @param expectedType
	 *            the expected type of the result
	 * @return the result of parsing the expression string
	 */
	private ExpressionEvaluator computeAndCheckType(ExpressionEvaluator subexpressionEvaluator, ExpressionType expectedType) {
		ExpressionEvaluator outEvaluator = compute(subexpressionEvaluator);
		if (outEvaluator.getType() == expectedType) {
			return outEvaluator;
		} else if (expectedType == ExpressionType.DOUBLE && outEvaluator.getType() == ExpressionType.INTEGER) {
			// use same resulting evaluator but with different type
			return new SimpleExpressionEvaluator(outEvaluator.getDoubleFunction(), expectedType, outEvaluator.isConstant());
		} else if (expectedType == ExpressionType.STRING) {
			return convertToStringEvaluator(outEvaluator);
		} else {
			throw new FunctionInputException("expression_parser.eval.type_not_matching", getFunctionName(),
					getConstantName(expectedType), getConstantName(outEvaluator.getType()));
		}
	}

	/**
	 * Converts the outEvaluator into an {@link ExpressionEvaluator} of type string. If outEvaluator
	 * is constant the result is also constant.
	 *
	 * @param outEvaluator
	 *            a evaluator which is not of type String
	 * @return an {@link ExpressionEvaluator} of type String
	 */
	private ExpressionEvaluator convertToStringEvaluator(final ExpressionEvaluator outEvaluator) {
		if (outEvaluator.isConstant()) {
			try {
				String stringValue = getStringValue(outEvaluator);
				return new SimpleExpressionEvaluator(stringValue, ExpressionType.STRING);
			} catch (ExpressionParsingException e) {
				throw e;
			} catch (Exception e) {
				throw new ExpressionParsingException(e);
			}
		} else {
			Callable<String> stringCallable = new Callable<String>() {

				@Override
				public String call() throws Exception {
					return getStringValue(outEvaluator);
				}

			};
			return new SimpleExpressionEvaluator(stringCallable, ExpressionType.STRING, false);
		}
	}

	/**
	 * Calculates the String value that the outEvaluator should return.
	 */
	private String getStringValue(ExpressionEvaluator outEvaluator) throws Exception {
		switch (outEvaluator.getType()) {
			case DOUBLE:
			case INTEGER:
				return doubleToString(outEvaluator.getDoubleFunction().call(),
						outEvaluator.getType() == ExpressionType.INTEGER);
			case BOOLEAN:
				return booleanToString(outEvaluator.getBooleanFunction().call());
			case DATE:
				return dateToString(outEvaluator.getDateFunction().call());
			default:
				// cannot happen
				return null;
		}
	}

	/**
	 * Converts the input to a string with special missing value handling
	 */
	private String dateToString(Date input) {
		if (input == null) {
			return null;
		} else {
			return input.toString();
		}
	}

	/**
	 * Converts the input to a string with special missing value handling.
	 */
	private String booleanToString(Boolean input) {
		if (input == null) {
			return null;
		} else {
			return input.toString();
		}

	}

	/**
	 * Converts the input to a string with special missing value handling and integers represented
	 * as integers if possible.
	 */
	private String doubleToString(double input, boolean isInteger) {
		if (Double.isNaN(input)) {
			return null;
		}
		if (isInteger && input == (int) input) {
			return "" + (int) input;
		} else {
			return "" + input;
		}
	}

	/**
	 * Converts the ExpressionType to the name of the constant that the user should use to mark this
	 * type.
	 *
	 * @param type
	 *            an {@link ExpressionType}
	 * @return the string name of the constant associated to this type
	 */
	private String getConstantName(ExpressionType type) {
		return TypeConstants.INSTANCE.getNameForType(type);
	}

	/**
	 * Converts the type constant passed by the user to an {@link ExpressionType}.
	 *
	 * @param expressionEvaluator
	 *            the evaluator holding the type constant.
	 * @return
	 */
	private ExpressionType getExpectedReturnType(ExpressionEvaluator expressionEvaluator) {
		if (!expressionEvaluator.isConstant()) {
			String validTypeArguments = TypeConstants.INSTANCE.getValidConstantsString();
			throw new FunctionInputException("expression_parser.eval.type_not_constant", getFunctionName(),
					validTypeArguments);
		}
		String typeString = null;
		try {
			typeString = expressionEvaluator.getStringFunction().call();
		} catch (ExpressionParsingException e) {
			throw e;
		} catch (Exception e) {
			throw new ExpressionParsingException(e);
		}

		ExpressionType expectedType = TypeConstants.INSTANCE.getTypeForName(typeString);
		if (expectedType == null) {
			String validTypeArguments = TypeConstants.INSTANCE.getValidConstantsString();
			throw new FunctionInputException("expression_parser.eval.invalid_type", typeString, getFunctionName(),
					validTypeArguments);
		}
		return expectedType;
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a date callable that calls
	 * {@link #computeAndCheckType(ExpressionEvaluator, ExpressionType)}.
	 */
	private ExpressionEvaluator makeDateEvaluator(final ExpressionType expectedType, final ExpressionEvaluator inputEvaluator) {
		Callable<Date> dateCallable = new Callable<Date>() {

			@Override
			public Date call() throws Exception {
				ExpressionEvaluator subExpressionEvaluator = computeAndCheckType(inputEvaluator, expectedType);
				return subExpressionEvaluator.getDateFunction().call();
			}

		};
		return new SimpleExpressionEvaluator(expectedType, dateCallable, false);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a boolean callable that calls
	 * {@link #computeAndCheckType(ExpressionEvaluator, ExpressionType)}.
	 */
	private ExpressionEvaluator makeBooleanEvaluator(final ExpressionType expectedType,
			final ExpressionEvaluator inputEvaluator) {
		Callable<Boolean> booleanCallable = new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				ExpressionEvaluator subExpressionEvaluator = computeAndCheckType(inputEvaluator, expectedType);
				return subExpressionEvaluator.getBooleanFunction().call();
			}

		};
		return new SimpleExpressionEvaluator(booleanCallable, false, expectedType);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a double callable that calls
	 * {@link #computeAndCheckType(ExpressionEvaluator, ExpressionType)}.
	 */
	private ExpressionEvaluator makeDoubleEvaluator(final ExpressionType expectedType,
			final ExpressionEvaluator inputEvaluator) {
		DoubleCallable doubleCallable = new DoubleCallable() {

			@Override
			public double call() throws Exception {
				ExpressionEvaluator subExpressionEvaluator = computeAndCheckType(inputEvaluator, expectedType);
				return subExpressionEvaluator.getDoubleFunction().call();
			}

		};
		return new SimpleExpressionEvaluator(doubleCallable, expectedType, false);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a string callable that calls
	 * {@link #computeAndCheckType(ExpressionEvaluator, ExpressionType)}.
	 */
	private ExpressionEvaluator makeStringEvaluator(final ExpressionType expectedType,
			final ExpressionEvaluator inputEvaluator) {
		Callable<String> stringCallable = new Callable<String>() {

			@Override
			public String call() throws Exception {
				ExpressionEvaluator subExpressionEvaluator = computeAndCheckType(inputEvaluator, expectedType);
				return subExpressionEvaluator.getStringFunction().call();
			}

		};
		return new SimpleExpressionEvaluator(stringCallable, expectedType, false);
	}

}
