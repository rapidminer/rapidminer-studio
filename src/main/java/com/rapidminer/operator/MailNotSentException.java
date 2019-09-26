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

/**
 * Exception class whose instances are thrown by MailUtilities if mail cannot be send.
 * 
 * @author Nils Woehler
 */
public class MailNotSentException extends Exception {

	private static final long serialVersionUID = 1L;
	private String errorKey;
	private Object[] arguments;

	/**
	 * @param errorKey
	 *            I18N error key that can be used by {@link UserError}s
	 */
	public MailNotSentException(String message, String errorKey) {
		super(message);
		this.errorKey = errorKey;
	}

	/**
	 * @param errorKey
	 *            I18N error key that can be used by {@link UserError}s
	 * @param arguments
	 *            for formatting I18N messages
	 */
	public MailNotSentException(String message, String errorKey, Object... arguments) {
		super(message);
		this.errorKey = errorKey;
		this.arguments = arguments;
	}

	/**
	 * @param errorKey
	 *            I18N error key that can be used by {@link UserError}s
	 * @param arguments
	 *            for formatting I18N messages
	 */
	public MailNotSentException(String message, Throwable cause, String errorKey, Object... arguments) {
		super(message, cause);
		this.errorKey = errorKey;
		this.arguments = arguments;
	}

	/**
	 * @return the errorKey
	 */
	public String getErrorKey() {
		return this.errorKey;
	}

	/**
	 * @return the arguments
	 */
	public Object[] getArguments() {
		return this.arguments;
	}

}
