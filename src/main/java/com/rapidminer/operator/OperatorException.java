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
package com.rapidminer.operator;

import java.util.MissingResourceException;

import com.rapidminer.tools.I18N;


/**
 * Exception class whose instances are thrown by instances of the class {@link Operator} or of one
 * of its subclasses.
 * 
 * This exception also allows using i18n keys, but it is not obligatory to do so. Currently only the
 * .short tag will be used, but this should be changed in future, so adding the other descriptions
 * could be wise.
 * 
 * TODO: Change usage to i18n keys.
 * 
 * @author Ingo Mierswa, Simon Fischer, Sebastian Land
 */
public class OperatorException extends Exception {

	private static final long serialVersionUID = 3626738574540303240L;

	private final String errorIdentifier;
	private final Object[] arguments;

	public OperatorException(String message) {
		super(message);
		this.errorIdentifier = null;
		this.arguments = new Object[0];
	}

	public OperatorException(String message, Throwable cause) {
		super(message, cause);
		this.errorIdentifier = null;
		this.arguments = new Object[0];
	}

	public OperatorException(String errorKey, Throwable cause, Object... arguments) {
		super(getErrorMessage(errorKey, arguments), cause);
		this.errorIdentifier = errorKey;
		this.arguments = arguments;
	}

	/**
	 * The errorKey used to access the error.{errorKey}.short message of the getUserErrorMessage bundle
	 *
	 * @return The error key which was used to create this exception, or {@code null}
	 * @since 9.5.0
	 */
	public String getErrorIdentifier() {
		return errorIdentifier;
	}

	/**
	 * @return the i18n arguments
	 * @since 9.5.0
	 */
	protected Object[] getArguments() {
		return arguments;
	}

	/**
	 * Returns the short error message from the {@link I18N#getUserErrorMessage UserErrorBundle}
	 *
	 * @param identifier part of the error.{identifier}.short i18n key
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string or "No message." if no i18n entry exists
	 */
	public static String getErrorMessage(String identifier, Object[] arguments) {
		return getErrorMessage(identifier, "short", "No message.", arguments);
	}

	/**
	 * This returns a resource message of the internationalized error messages identified by an id.
	 *
	 * @param id
	 *            The identifier of the error. "error." will be automatically prepended-
	 * @param key
	 *            The part of the error description that should be shown.
	 * @param deflt
	 *            The default if no resource bundle is available.
	 */
	public static String getResourceString(String id, String key, String deflt) {
		try {
			return I18N.getUserErrorMessagesBundle().getString("error." + id + "." + key);
		} catch (MissingResourceException | NullPointerException e) {
			return deflt;
		}
	}

	/**
	 * Returns the formatted message from the {@link I18N#getUserErrorMessagesBundle() UserError} bundle
	 * key: error.{identifier}.{type}
	 *
	 * @param identifier the error identifier
	 * @param type
	 * 		the type, either "name", "short" or "long"
	 * @param defaultMessage
	 * 		the message if no entry for the given key exists
	 * @param arguments i18n arguments
	 * @return the message or the defaultMessage
	 * @since 9.5.0
	 */
	protected static String getErrorMessage(String identifier, String type, String defaultMessage, Object... arguments) {
		String message = I18N.getUserErrorMessageOrNull("error." + identifier + "." + type, arguments);
		return message == null ? defaultMessage : message;
	}

}
