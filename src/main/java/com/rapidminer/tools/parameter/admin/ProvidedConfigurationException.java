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
package com.rapidminer.tools.parameter.admin;

import java.util.Objects;


/**
 * Thrown if the provided configuration could not be read during startup
 *
 * @author Jonas Wilms-Pfau
 * @since 9.0.0
 */
public class ProvidedConfigurationException extends RuntimeException {

	private static final String DEFAULT_TITLE = "Administrative restrictions configuration broken";
	private static final String DEFAULT_MESSAGE = "<html><div style='width:500;'>Your system administrator has tried to restrict some features of RapidMiner Studio. Unfortunately, the restriction configuration cannot be read. Studio will not be able to start for security reasons until this issue is resolved.</div></html>";

	private final String dialogTitle;
	private final String dialogMessage;

	/**
	 * Constructs a new ProvidedConfigurationException
	 *
	 * @param message the detail message
	 * @param cause the cause of this exception, might be {@code null}
	 * @see RuntimeException#RuntimeException(String, Throwable)
	 */
	public ProvidedConfigurationException(String message, Throwable cause) {
		this(message, cause, DEFAULT_TITLE, DEFAULT_MESSAGE);
	}

	/**
	 * Constructs a new ProvidedConfigurationException that contains information for a simple dialog
	 *
	 * @param message the detail message
	 * @param cause the cause of this exception, might be {@code null}
	 * @see RuntimeException#RuntimeException(String, Throwable)
	 */
	public ProvidedConfigurationException(String message, Throwable cause, String dialogTitle, String dialogMessage) {
		super(message, cause);
		this.dialogTitle = Objects.toString(dialogTitle, DEFAULT_TITLE);
		this.dialogMessage = Objects.toString(dialogMessage, DEFAULT_MESSAGE);
	}

	/**
	 * Returns the error dialog title
	 *
	 * @return error dialog title
	 */
	public String getDialogTitle() {
		return dialogTitle;
	}

	/**
	 * Returns the error dialog message
	 *
	 * @return error dialog message
	 */
	public String getDialogMessage() {
		return dialogMessage;
	}
}