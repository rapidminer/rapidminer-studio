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
package com.rapidminer.connection.legacy;


/**
 * Exception that is thrown by {@link ConversionService#convert(Object)} if the conversion to a
 * @link com.rapidminer.connection.ConnectionInformation ConnectionInformation} failed.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3.0
 */
public class ConversionException extends Exception {

	/**
	 * @see Exception#Exception(String)
	 */
	public ConversionException(String message) {
		this(message, null);
	}

	/**
	 * @see Exception#Exception(String, Throwable)
	 */
	public ConversionException(String message, Exception cause) {
		super(message, cause);
	}
}
