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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A POJO to represent a generic validation result. Consists of a {@link ResultType} and a message connected to the result type.
 * In case of a failure, a map of parameter names/error_i18n keys can be retrieved using {@link #getParameterErrorMessages()}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ValidationResult extends TestResult {

	private static final ValidationResult NULL_VALIDATION = new ValidationResult(TestResult.nullable());
	/** use this i18n constant when the validation succeeded */
	public static final String I18N_KEY_SUCCESS = "validation.success";
	/** use this i18n constant when the validation failed */
	public static final String I18N_KEY_FAILURE = "validation.failed";
	/** use this i18n constant as an error indicator when the user did not put in a value */
	public static final String I18N_KEY_VALUE_MISSING = "validation.value_missing";
	/** use this i18n constant as an error indicator when a value collapses to {@code null} due to placeholders */
	public static final String I18N_KEY_VALUE_MISSING_PLACEHOLDER = "validation.value_missing_placeholder";
	/** use this i18n constant as an error indicator when a value provider is not working properly */
	public static final String I18N_KEY_VALUE_NOT_INJECTABLE = "validation.value_not_injectable";
	/** Ordering for ResultTypes when merging */
	public static final List<ResultType> RESULT_TYPES_ORDER = Collections.unmodifiableList(Arrays.asList(ResultType.NONE, ResultType.SUCCESS, ResultType.NOT_SUPPORTED, ResultType.FAILURE));


	private ValidationResult(TestResult result) {
		super(result.getType(), result.getMessageKey(), result.getParameterErrorMessages(), result.getArguments());
	}

	/**
	 * Create a successful validation result.
	 *
	 * @param messageKey
	 * 		the message i18n key, never null
	 */
	public static ValidationResult success(String messageKey) {
		return new ValidationResult(TestResult.success(messageKey));
	}

	/**
	 * Create a failure validation result.
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
	public static ValidationResult failure(String messageKey, Map<String, String> parameterErrors, Object... arguments) {
		return new ValidationResult(TestResult.failure(messageKey, parameterErrors, arguments));
	}

	/**
	 * Get a validation result that indicates the object to validate was {@code null}.
	 */
	public static ValidationResult nullable() {
		return NULL_VALIDATION;
	}

	/**
	 * Merge all the given {@link ValidationResult ValidationResults} pessimistically, keep all the parameter errors but
	 * only the worst result message based on the order
	 * <p>
	 * ResultType.NONE < ResultType.SUCCESS < ResultType.NOT_SUPPORTED < ResultType.FAILURE
	 * </p>
	 * Parameter errors of higher ranked {@link ValidationResult ValidationResults} will overwrite existing ones
	 *
	 * @param validationResults
	 * 		all the validationResults to be merged.
	 * @return the resulting {@link ValidationResult} with all the parameter errors
	 */
	public static ValidationResult merge(ValidationResult... validationResults) {
		if (validationResults == null || validationResults.length == 0) {
			return nullable();
		}

		Map<String, String> mergedErrors = new HashMap<>();

		Arrays.sort(validationResults, Comparator.comparingInt(o -> RESULT_TYPES_ORDER.indexOf(o.getType())));
		ValidationResult worstVR = validationResults[validationResults.length - 1];
		for (ValidationResult vr : validationResults) {
			mergedErrors.putAll(vr.getParameterErrorMessages());
		}
		return new ValidationResult(new TestResult(worstVR.getType(), worstVR.getMessageKey(), mergedErrors, worstVR.getArguments()));
	}
}
