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

import java.util.Collections;
import java.util.Map;


/**
 * A POJO to represent a generic test result. Consists of a {@link ResultType}, a messageKey which will be used to get
 * the i18n result message, and an optional map of parameter i18n error message keys if the test result was caused by specific parameters.
 *
 * @author Jan Czogalla, Marco Boeck
 * @since 9.3
 */
public class TestResult {

	/** Enum of results for {@link TestResult} */
	public enum ResultType {
		/** Indicates that there is no testing procedure */
		NOT_SUPPORTED,
		/** Indicates a successful test */
		SUCCESS,
		/** Indicates a failed test */
		FAILURE,
		/** Indicates that no test could be performed */
		NONE
	}

	private static final TestResult NULL_TEST = new TestResult(ResultType.NONE, "test.object_null", null);
	/** use this i18n constant when the test succeeded */
	public static final String I18N_KEY_SUCCESS = "test.success";
	/** use this i18n constant for generic test fails */
	public static final String I18N_KEY_FAILED = "test.connection_failed";
	/** use this i18n constant when the test failed because of injection problems */
	public static final String I18N_KEY_INJECTION_FAILURE = "test.injection_failed";
	/** use this i18n constant when the test functionality is not implemented */
	public static final String I18N_KEY_NOT_IMPLEMENTED = "test.not_implemented";

	private ResultType type;
	private String messageKey;
	private Map<String, String> parameterErrorMessages;
	private Object[] arguments;


	/**
	 * Public access to this object should be done via the static factory methods.
	 *
	 * @param type
	 * 		the type of the result
	 * @param messageKey
	 * 		the i18n key that will be used to find a human-readable message
	 * @param parameterErrorMessages
	 * 		Optional. Can contain i18n keys for individual parameter error messages if the test result is based on e.g.
	 * 		missing/wrong values for one or more individual parameters. Format is: {@code group.parameter - i18nKey}
	 * @param arguments
	 * 		optional arguments for the i18n message
	 */
	public TestResult(ResultType type, String messageKey, Map<String, String> parameterErrorMessages, Object... arguments) {
		if (type == null) {
			throw new IllegalArgumentException("type must not be null!");
		}
		if (messageKey == null || messageKey.trim().isEmpty()) {
			throw new IllegalArgumentException("messageKey must not be null or empty!");
		}
		if (parameterErrorMessages == null) {
			parameterErrorMessages = Collections.emptyMap();
		}

		this.type = type;
		this.messageKey = messageKey;
		this.parameterErrorMessages = parameterErrorMessages;
		this.arguments = arguments;
	}

	/** Gets the result type */
	public ResultType getType() {
		return type;
	}

	/**
	 * Gets the result message i18n key.
	 * @return the key or message, never {@code null}
	 */
	public String getMessageKey() {
		return messageKey;
	}

	/**
	 * Gets the result message arguments, if any.
	 *
	 * @return the arguments or an empty array if there are none
	 */
	public Object[] getArguments() {
		return arguments;
	}

	/**
	 * Get the map of message i18n keys associated with their respective parameters. Each entry has the following
	 * format: {@code group.key - i18nkey}. The i18n key provided here will become part of a composite key before it is
	 * being looked up in the i18n file.
	 *
	 * @return the map which may be empty but never {@code null}
	 */
	public Map<String, String> getParameterErrorMessages() {
		return parameterErrorMessages;
	}

	/**
	 * Create a successful test result.
	 *
	 * @param messageKey
	 * 		the message i18n key, never {@code null}
	 * @return the test result instance
	 */
	public static TestResult success(String messageKey) {
		return new TestResult(ResultType.SUCCESS, messageKey, null);
	}

	/**
	 * Create a failure test result.
	 *
	 * @param messageKey
	 * 		the message i18n key, never {@code null}
	 * @param arguments
	 * 		optional additional arguments for the i18n
	 * @return the test result instance
	 */
	public static TestResult failure(String messageKey, Object... arguments) {
		return failure(messageKey, null, arguments);
	}

	/**
	 * Create a failure test result.
	 *
	 * @param messageKey
	 * 		the message i18n key, never {@code null}
	 * @param parameterErrors
	 * 		Optional, can be used to indicate one or more errors directly mapped to parameters (e.g. missing value, wrong
	 * 		type, etc). Each entry in the map must have the following format: {@code group.key - i18nkey}. Can be {@code
	 * 		null}
	 * @param arguments
	 * 		optional additional arguments for the i18n
	 * @return the test result instance
	 */
	public static TestResult failure(String messageKey, Map<String, String> parameterErrors, Object... arguments) {
		return new TestResult(ResultType.FAILURE, messageKey, parameterErrors, arguments);
	}

	/** Get a test result that indicates the object to test was {@code null}. */
	public static TestResult nullable() {
		return NULL_TEST;
	}
}
