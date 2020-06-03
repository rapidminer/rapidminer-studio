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

/**
 * Indicating that the given encryption context key was unknown.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class EncryptionContextNotFound extends EncryptionException {

	private final String context;


	/**
	 * Creates a new instance for the given context.
	 *
	 * @param context the context that was not found
	 */
	public EncryptionContextNotFound(String context) {
		super("Unknown encryption context");

		this.context = context;
	}

	/**
	 * Gets the key of the context that was not found.
	 *
	 * @return the context key
	 */
	public String getContext() {
		return context;
	}
}
