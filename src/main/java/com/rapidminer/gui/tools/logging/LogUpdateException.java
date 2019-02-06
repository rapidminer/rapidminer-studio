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
package com.rapidminer.gui.tools.logging;

/**
 * Thrown by {@link LogModel}s when the update call fails.
 * 
 * @author Marco Boeck
 * 
 */
public class LogUpdateException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new {@link LogUpdateException} with the given error message.
	 * 
	 * @param message
	 */
	public LogUpdateException(String message) {
		super(message);
	}

	/**
	 * Constructs a new {@link LogUpdateException} with the given error message and cause.
	 * 
	 * @param message
	 * @param cause
	 */
	public LogUpdateException(String message, Throwable cause) {
		super(message, cause);
	}
}
