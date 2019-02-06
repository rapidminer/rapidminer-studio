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
package com.rapidminer.tools.net;

import java.io.IOException;


/**
 * Exception thrown if forward is not allowed
 *
 * @author Jonas Wilms-Pfau
 * @since 9.0.0
 */
public class ForbiddenForwardException extends IOException {

	/**
	 * Constructs an {@code ForbiddenForwardException} with the specified detail message.
	 *
	 * @param message
	 * 		message – The detail message (which is saved for later retrieval by the getMessage() method)
	 * @see IOException#IOException(String)
	 */
	public ForbiddenForwardException(String message) {
		super(message);
	}
}
