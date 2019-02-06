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
package com.rapidminer.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Registry for custom entries to the <em>Run</em> menu(s). Entries must be provided via
 * {@link MenuItemFactory} instances.
 *
 * @author Michael Knopf
 * @since 7.0.0
 */
public enum RunActionRegistry {

	/** Singleton instance. */
	INSTANCE;

	private List<MenuItemFactory> factories = new ArrayList<>();

	/**
	 * Registers a new {@link MenuItemFactory}.
	 *
	 * @param factory
	 *            the new factory
	 */
	public void register(MenuItemFactory factory) {
		if (factory == null) {
			throw new IllegalArgumentException("Factory must not be null!");
		}
		factories.add(factory);
	}

	/**
	 * Returns an unmodifiable list of a registered factories.
	 *
	 * @return the list of factories
	 */
	public List<MenuItemFactory> getFacories() {
		return Collections.unmodifiableList(factories);
	}
}
