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
 * Things that should be searchable via the Global Search feature must implement this interface. Register them to the {@link
 * GlobalSearchRegistry} so they are automatically included in the search.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public interface GlobalSearchable {

	/**
	 * Returns the {@link GlobalSearchManager} instance for this searchable item.
	 *
	 * @return the search manager instance, never {@code null}
	 */
	GlobalSearchManager getSearchManager();
}
