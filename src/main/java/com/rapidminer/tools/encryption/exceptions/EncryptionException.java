/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.tools.encryption.exceptions;

import java.security.GeneralSecurityException;


/**
 * Parent exception for all exceptions related to our {@link com.rapidminer.tools.encryption.EncryptionProvider
 * encryption framework}.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class EncryptionException extends GeneralSecurityException {

	/**
	 * Constructs an EncryptionException with no detail message.
	 */
	public EncryptionException() {
		super();
	}

	/**
	 * Constructs an EncryptionException with the specified detail message.
	 *
	 * @param msg the detail message
	 */
	public EncryptionException(String msg) {
		super(msg);
	}

	/**
	 * Creates an EncryptionException with the specified detail message and cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause, can be {@code null}
	 */
	public EncryptionException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates an EncryptionException with the specified cause.
	 *
	 * @param cause the cause, can be {@code null}
	 */
	public EncryptionException(Throwable cause) {
		super(cause);
	}

}
