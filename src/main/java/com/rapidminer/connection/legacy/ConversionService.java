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

import com.rapidminer.connection.ConnectionInformation;


/**
 * Implemented by {@link com.rapidminer.connection.ConnectionHandler ConnectionHandler} to offer conversion methods for
 * old connection configurations.
 *
 * @param <T>
 * 		The class of the old connection information
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public interface ConversionService<T> {

	/**
	 * Verifies that the given {@link Object} is of the right type and can be converted to a {@link ConnectionInformation}
	 *
	 * @param oldConnectionObject
	 * 		the {@link Object} that should be tested
	 * @return {@code true} if this {@link Object} can be converted to a {@link ConnectionInformation}
	 */
	boolean canConvert(Object oldConnectionObject);

	/**
	 * Converts the given {@link T} to a {@link ConnectionInformation}
	 *
	 * @param oldConnectionObject
	 * 		the {@link T} that should be converted; must not be {@code null}
	 * @return the {@link ConnectionInformation result} of the conversion
	 * @throws ConversionException
	 * 		if the conversion failed
	 * @throws ClassCastException if {@code oldConnectionObject} is not of type {@link T}
	 */
	ConnectionInformation convert(T oldConnectionObject) throws ConversionException;
}
