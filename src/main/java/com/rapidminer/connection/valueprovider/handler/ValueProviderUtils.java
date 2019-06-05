/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.connection.valueprovider.handler;

import org.apache.commons.lang.StringUtils;


/**
 * Some utility methods for dealing with injection of values.
 *
 * @author Marco Boeck
 * @since 9.3.0
 */
public enum ValueProviderUtils {
	; // no instance

	/** this is the indicator of a placeholder. Placeholders are used to reference values from other fields. */
	public static final String PLACEHOLDER_INDICATOR = "%";
	/** this is the opening bracket of a placeholder. Placeholders are used to reference values from other fields. */
	public static final String PLACEHOLDER_OPENING = "{";
	/**
	 * this is the prefix of a placeholder. Placeholders are used to reference values from other fields.
	 * The prefix is comprised of the {@link #PLACEHOLDER_INDICATOR} and the {@link #PLACEHOLDER_OPENING}.
	 */
	public static final String PLACEHOLDER_PREFIX = PLACEHOLDER_INDICATOR + PLACEHOLDER_OPENING;
	/** this is the suffix of a placeholder. Placeholders are used to reference values from other fields. */
	public static final String PLACEHOLDER_SUFFIX = "}";


	/**
	 * Wraps the given string as a placeholder, i.e. "%{value}".
	 *
	 * @param key
	 * 		the key of the placeholder, must not be {@code null} or empty
	 * @return the wrapped value
	 */
	public static String wrapIntoPlaceholder(String key) {
		if (StringUtils.trimToNull(key) == null) {
			throw new IllegalArgumentException("key must neither be null nor empty!");
		}

		return PLACEHOLDER_PREFIX + key + PLACEHOLDER_SUFFIX;
	}

}
