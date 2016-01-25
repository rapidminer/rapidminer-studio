/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.tools.I18N;

import java.text.MessageFormat;
import java.util.ResourceBundle;


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

	private static ResourceBundle messages = I18N.getUserErrorMessagesBundle(); // ErrorBundle();
	private static final MessageFormat formatter = new MessageFormat("");

	public OperatorException(String message) {
		super(message);
	}

	public OperatorException(String message, Throwable cause) {
		super(message, cause);
	}

	public OperatorException(String errorKey, Throwable cause, Object... arguments) {
		super(getErrorMessage(errorKey, arguments), cause);
	}

	public static String getErrorMessage(String identifier, Object[] arguments) {
		String message = getResourceString(identifier, "short", "No message.");
		try {
			formatter.applyPattern(message);
			String formatted = formatter.format(arguments);
			return formatted;
		} catch (Throwable t) {
			return message;
		}

	}

	/**
	 * This returns a resource message of the internationalized error messages identified by an id.
	 * Compared to the legacy method {@link #getResourceString(int, String, String)} this supports a
	 * more detailed identifier. This makes it easier to ensure extensions don't reuse already
	 * defined core errors. It is common sense to add the extensions namespace identifier as second
	 * part of the key, just after error. For example: error.rmx_web.operator.unusable = This
	 * operator {0} is unusable.
	 * 
	 * @param id
	 *            The identifier of the error. "error." will be automatically prepended-
	 * @param key
	 *            The part of the error description that should be shown.
	 * @param deflt
	 *            The default if no resource bundle is available.
	 */
	public static String getResourceString(String id, String key, String deflt) {
		if (messages == null) {
			return deflt;
		}
		try {
			return messages.getString("error." + id + "." + key);
		} catch (java.util.MissingResourceException e) {
			return deflt;
		}
	}
}
