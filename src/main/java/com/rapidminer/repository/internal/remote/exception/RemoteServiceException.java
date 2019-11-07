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
package com.rapidminer.repository.internal.remote.exception;

/**
 * In case a remote connection succeeded but the result is rather unexpected like a bad status or an unsupported
 * response type this Exception can be used to keep return the remaining information from the call.
 *
 * @author Andreas Timm
 * @since 9.5.0
 */
public class RemoteServiceException extends Exception {

	private final String contentType;

	public RemoteServiceException(String message, String contentType, Throwable cause) {
		super(message, cause);
		this.contentType = contentType;
	}

	public String getContentType() {
		return contentType;
	}
}
