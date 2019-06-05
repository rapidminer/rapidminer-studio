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

import java.util.Date;
import java.util.concurrent.Callable;

import com.rapidminer.tools.expression.DoubleCallable;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionType;


/**
 * {@link ExpressionEvaluator} that supplies constructors for all admissible combinations of its
 * fields. It checks if the required relationship between {@link ExpressionType} and the Callable
 * that is not {@code null} is satisfied.
 *
 * @author Gisa Schaefer
 *
 */
public class SimpleExpressionEvaluator implements ExpressionEvaluator {

	private final Callable<String> stringCallable;
	private final Callable<Date> dateCallable;
	private final DoubleCallable doubleCallable;
	private final Callable<Boolean> booleanCallable;

	private final ExpressionType type;
	private final boolean isConstant;

	/**
	 * Initializes the fields.
	 */
	protected SimpleExpressionEvaluator(ExpressionType type, Callable<String> stringCallable, DoubleCallable doubleCallable,
			Callable<Boolean> booleanCallable, Callable<Date> dateCallable, boolean isConstant) {
		this.stringCallable = stringCallable;
		this.dateCallable = dateCallable;
		this.doubleCallable = doubleCallable;
		this.booleanCallable = booleanCallable;
		this.type = type;
		this.isConstant = isConstant;
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with the given data where the other callables are
	 * {@code null}. type must be ExpressionType.INTEGER or ExpressionType.DOUBLE.
	 *
	 * @param doubleCallable
	 *            the callable to store
	 * @param type
	 *            the type of the result of the callable, must be ExpressionType.INTEGER or
	 *            ExpressionType.DOUBLE
	 * @param isConstant
	 *            whether the result of the callable is constant
	 */
	public SimpleExpressionEvaluator(DoubleCallable doubleCallable, ExpressionType type, boolean isConstant) {
		this(type, null, doubleCallable, null, null, isConstant);
		if (type != ExpressionType.DOUBLE && type != ExpressionType.INTEGER) {
			throw new IllegalArgumentException("Invalid type " + type + "for Callable");
		}
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with the given data where the other callables are
	 * {@code null}. type must be ExpressionType.STRING.
	 *
	 * @param stringCallable
	 *            the callable to store
	 * @param type
	 *            the type of the result of the callable, must be ExpressionType.STRING
	 * @param isConstant
	 *            whether the result of the callable is constant
	 */
	public SimpleExpressionEvaluator(Callable<String> stringCallable, ExpressionType type, boolean isConstant) {
		this(type, stringCallable, null, null, null, isConstant);
		if (type != ExpressionType.STRING) {
			throw new IllegalArgumentException("Invalid type " + type + "for Callable");
		}
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with the given data where the other callables are
	 * {@code null}. type must be ExpressionType.STRING.
	 *
	 * @param dateCallable
	 *            the callable to store
	 * @param type
	 *            the type of the result of the callable, must be ExpressionType.DATE
	 * @param isConstant
	 *            whether the result of the callable is constant
	 */
	public SimpleExpressionEvaluator(ExpressionType type, Callable<Date> dateCallable, boolean isConstant) {
		this(type, null, null, null, dateCallable, isConstant);
		if (type != ExpressionType.DATE) {
			throw new IllegalArgumentException("Invalid type " + type + "for Callable");
		}
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with the given data where the other callables are
	 * {@code null}. type must be ExpressionType.BOOLEAN.
	 *
	 * @param booleanCallable
	 *            the callable to store
	 * @param type
	 *            the type of the result of the callable, must be ExpressionType.BOOLEAN
	 * @param isConstant
	 *            whether the result of the callable is constant
	 */
	public SimpleExpressionEvaluator(Callable<Boolean> booleanCallable, boolean isConstant, ExpressionType type) {
		this(type, null, null, booleanCallable, null, isConstant);
		if (type != ExpressionType.BOOLEAN) {
			throw new IllegalArgumentException("Invalid type " + type + "for Callable");
		}
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a {@link DoubleCallable} returning constantly
	 * doubleValue. type must be ExpressionType.INTEGER or ExpressionType.DOUBLE.
	 *
	 * @param doubleValue
	 *            the constant double return value
	 * @param type
	 *            the type of the result of the callable, must be ExpressionType.INTEGER or
	 *            ExpressionType.DOUBLE
	 */
	public SimpleExpressionEvaluator(double doubleValue, ExpressionType type) {
		this(makeConstantCallable(doubleValue), type, true);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a {@link Callable<String>} returning constantly
	 * stringValue. type must be ExpressionType.STRING.
	 *
	 * @param stringValue
	 *            the constant String return value
	 * @param type
	 *            the type of the result of the callable, must be ExpressionType.STRING
	 */
	public SimpleExpressionEvaluator(String stringValue, ExpressionType type) {
		this(makeConstantCallable(stringValue), type, true);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a {@link Callable<Boolean>} returning constantly
	 * booleanValue.
	 *
	 * @param booleanValue
	 *            the constant Boolean return value
	 * @param type
	 *            the type of the result of the callable, must be ExpressionType.BOOLEAN
	 */
	public SimpleExpressionEvaluator(Boolean booleanValue, ExpressionType type) {
		this(makeConstantCallable(booleanValue), true, type);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a {@link Callable<Date>} returning constantly
	 * dateValue.
	 *
	 * @param dateValue
	 *            the constant Date return value
	 * @param type
	 *            type the type of the result of the callable, must be ExpressionType.DATE
	 */
	public SimpleExpressionEvaluator(Date dateValue, ExpressionType type) {
		this(type, makeConstantCallable(dateValue), true);
	}

	private static DoubleCallable makeConstantCallable(final double doubleValue) {
		return () -> doubleValue;
	}

	private static <V> Callable<V> makeConstantCallable(V value) {
		return () -> value;
	}

	@Override
	public ExpressionType getType() {
		return type;
	}

	@Override
	public boolean isConstant() {
		return isConstant;
	}

	@Override
	public Callable<String> getStringFunction() {
		return stringCallable;
	}

	@Override
	public Callable<Date> getDateFunction() {
		return dateCallable;
	}

	@Override
	public DoubleCallable getDoubleFunction() {
		return doubleCallable;
	}

	@Override
	public Callable<Boolean> getBooleanFunction() {
		return booleanCallable;
	}

}
