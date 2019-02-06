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
package com.rapidminer.tools.expression.internal.antlr;

import java.util.Date;

import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParsingException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.UnknownValue;


/**
 * A basic {@link Expression}.
 *
 * @author Gisa Schaefer
 *
 */
class SimpleExpression implements Expression {

	private ExpressionEvaluator evaluator;

	/**
	 * Creates a basic expression based on the evaluator.
	 *
	 * @param evaluator
	 *            the evaluator to use for evaluating the expression
	 */
	SimpleExpression(ExpressionEvaluator evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public ExpressionType getExpressionType() {
		return evaluator.getType();
	}

	@Override
	public Object evaluate() throws ExpressionException {
		try {
			switch (evaluator.getType()) {
				case DOUBLE:
				case INTEGER:
					return evaluator.getDoubleFunction().call();
				case BOOLEAN:
					Boolean booleanResult = evaluator.getBooleanFunction().call();
					return booleanResult == null ? UnknownValue.UNKNOWN_BOOLEAN : booleanResult;
				case DATE:
					Date dateResult = evaluator.getDateFunction().call();
					return dateResult == null ? UnknownValue.UNKNOWN_DATE : dateResult;
				case STRING:
				default:
					String stringResult = evaluator.getStringFunction().call();
					return stringResult == null ? UnknownValue.UNKNOWN_NOMINAL : stringResult;
			}
		} catch (ExpressionException e) {
			throw e;
		} catch (ExpressionParsingException e) {
			throw new ExpressionException(e);
		} catch (Exception e) {
			throw new ExpressionException(e.getLocalizedMessage());
		}
	}

	@Override
	public String evaluateNominal() throws ExpressionException {
		try {
			switch (evaluator.getType()) {
				case BOOLEAN:
					Boolean result = evaluator.getBooleanFunction().call();
					return result == null ? null : result.toString();
				case STRING:
					return evaluator.getStringFunction().call();
				default:
					throw new IllegalArgumentException("Cannot evaluate expression of type " + getExpressionType()
							+ " as nominal");
			}
		} catch (ExpressionException e) {
			throw e;
		} catch (ExpressionParsingException e) {
			throw new ExpressionException(e);
		} catch (Exception e) {
			throw new ExpressionException(e.getLocalizedMessage());
		}
	}

	@Override
	public double evaluateNumerical() throws ExpressionException {
		try {
			switch (evaluator.getType()) {
				case DOUBLE:
				case INTEGER:
					return evaluator.getDoubleFunction().call();
				default:
					throw new IllegalArgumentException("Cannot evaluate expression of type " + getExpressionType()
							+ " as numerical");
			}
		} catch (ExpressionException e) {
			throw e;
		} catch (ExpressionParsingException e) {
			throw new ExpressionException(e);
		} catch (Exception e) {
			throw new ExpressionException(e.getLocalizedMessage());
		}
	}

	@Override
	public Date evaluateDate() throws ExpressionException {
		try {
			switch (evaluator.getType()) {
				case DATE:
					return evaluator.getDateFunction().call();
				default:
					throw new IllegalArgumentException("Cannot evaluate expression of type " + getExpressionType()
							+ " as date");
			}
		} catch (ExpressionException e) {
			throw e;
		} catch (ExpressionParsingException e) {
			throw new ExpressionException(e);
		} catch (Exception e) {
			throw new ExpressionException(e.getLocalizedMessage());
		}
	}

	@Override
	public Boolean evaluateBoolean() throws ExpressionException {
		try {
			switch (evaluator.getType()) {
				case BOOLEAN:
					return evaluator.getBooleanFunction().call();
				default:
					throw new IllegalArgumentException("Cannot evaluate expression of type " + getExpressionType()
							+ " as boolean");
			}
		} catch (ExpressionException e) {
			throw e;
		} catch (ExpressionParsingException e) {
			throw new ExpressionException(e);
		} catch (Exception e) {
			throw new ExpressionException(e.getLocalizedMessage());
		}
	}

}
