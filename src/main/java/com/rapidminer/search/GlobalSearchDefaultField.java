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
package com.rapidminer.search;

/**
 * Simple POJO class that holds both the name and their relative boost of the additional fields that should be searched for a {@link GlobalSearchManager}.
 * <p>
 * Note that the higher the boost difference, the more hits of the higher boosted field are favored, i.e. ranked higher.
 * The {@link GlobalSearchUtilities#FIELD_NAME} has a boost of {@code 1f}.
 * </p>
 *
 * @author Marco Boeck
 * @since 8.1
 */
public final class GlobalSearchDefaultField {

	private final String name;
	private final float boost;


	/**
	 * Creates a new POJO with the given values.
	 *
	 * @param name
	 * 		the name of the {@link org.apache.lucene.document.Field} that should be searched by default
	 * @param boost
	 * 		the relative boost of the field. See {@link GlobalSearchUtilities#DEFAULT_OTHER_FIELD_BOOST} for the default value for fields that are not the name field
	 */
	public GlobalSearchDefaultField(final String name, final float boost) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("name must not be null or empty!");
		}

		this.name = name;
		this.boost = boost;
	}

	/**
	 * Returns the field name.
	 *
	 * @return the name, never {@code null}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the boost of the field.
	 * <p>
	 * Note that the higher the boost difference, the more hits of the higher boosted field are favored, i.e. ranked higher. The {@link GlobalSearchUtilities#FIELD_NAME} has a boost of {@code 1f}.
	 * </p>
	 *
	 * @return the boost
	 */
	public float getBoost() {
		return boost;
	}
}
