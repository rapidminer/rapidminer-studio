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
package com.rapidminer.connection.util;


/**
 * A generic handler interface. Provides initialization, type information and testing functionality.
 *
 * @param <T>
 * 		the class whose objects are handled
 * @author Jan Czogalla
 * @see GenericHandlerRegistry
 * @since 9.3
 */
public interface GenericHandler<T> {

	/**
	 * Initialize the handler. This should be called once, before registering the handler.
	 */
	void initialize();

	/**
	 * Whether the handler was already initialized
	 */
	boolean isInitialized();

	/**
	 * The type of this handler. This must be unique between all handlers.
	 * If the handler was registered from an {@link com.rapidminer.tools.plugin.Plugin Extension},
	 * the type should be in the form of {@code namespace:type}.
	 */
	String getType();

	/**
	 * Validates the given handled object. This can be used to check if all parameters have sensible values.
	 * This should only take a very small amount of time as opposed to {@link #test(TestExecutionContext)} which might run a longer operation.
	 * Should return a {@link com.rapidminer.connection.util.TestResult.ResultType#NONE ResultType.NONE} result
	 * if the object is {@code null}
	 *
	 * @param object
	 * 		the object to be validated
	 * @see ValidationResult
	 */
	ValidationResult validate(T object);

	/**
	 * Test if the given handled object is configured correctly; should return a
	 * {@link com.rapidminer.connection.util.TestResult.ResultType#NONE ResultType.NONE} result
	 * if the object is {@code null}
	 *
	 * @param testContext
	 * 		the execution context containing the test subject to be tested
	 */
	TestResult test(TestExecutionContext<T> testContext);
}
