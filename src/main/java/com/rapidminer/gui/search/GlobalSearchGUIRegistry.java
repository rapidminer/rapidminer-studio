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
package com.rapidminer.gui.search;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.rapidminer.search.GlobalSearchCategory;
import com.rapidminer.search.GlobalSearchResult;
import com.rapidminer.tools.LogService;


/**
 * Register {@link GlobalSearchableGUIProvider} visualization providers here. After being registered here they will be used to
 * visualize search results for the respective {@link GlobalSearchCategory}. You can call {@link
 * #registerSearchVisualizationProvider(GlobalSearchCategory, GlobalSearchableGUIProvider)} to register them.
 * <p>
 *     Do so when #initFinalChecks() is called for plugins, because afterwards the UI for the Global Search will be initialized.
 * </p>
 *
 * <p>
 * If you unregistered the searchable already, you can also unregister the GUI provider by calling {@link #unregisterSearchVisualizationProvider(GlobalSearchCategory)}.
 * </p>
 *
 * @author Marco Boeck
 * @since 8.1
 */
public enum GlobalSearchGUIRegistry {

	INSTANCE;

	private final Map<String, GlobalSearchableGUIProvider> map = new ConcurrentHashMap<>();


	/**
	 * Registers the given {@link GlobalSearchableGUIProvider}. These are used to visualize {@link
	 * GlobalSearchResult}s.
	 *
	 * @param searchCategory
	 * 		The searchable instance
	 * @param guiProvider
	 * 		the gui provider instance
	 * @throws IllegalStateException
	 * 		if registering a guiProvider for a searchable that is already registered
	 */
	public void registerSearchVisualizationProvider(final GlobalSearchCategory searchCategory, final GlobalSearchableGUIProvider guiProvider) {
		if (searchCategory == null) {
			throw new IllegalArgumentException("searchCategory must not be null!");
		}
		if (guiProvider == null) {
			throw new IllegalArgumentException("guiProvider must not be null!");
		}
		String categoryId = searchCategory.getCategoryId();
		if (categoryId == null || categoryId.trim().isEmpty()) {
			throw new IllegalArgumentException("categoryId must not be null or empty!");
		}
		if (map.get(categoryId) != null) {
			throw new IllegalStateException("searchable " + categoryId + " already registered!");
		}

		map.put(categoryId, guiProvider);
		LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.search.GlobalSearchGUIRegistry.provider_added", categoryId);
	}

	/**
	 * Removes the given {@link GlobalSearchableGUIProvider}.
	 *
	 * @param searchCategory
	 * 		The searchable instance
	 * @throws IllegalStateException
	 * 		if registering a guiProvider for a searchable that is already registered
	 */
	public void unregisterSearchVisualizationProvider(final GlobalSearchCategory searchCategory) {
		if (searchCategory == null) {
			throw new IllegalArgumentException("searchCategory must not be null!");
		}
		String categoryId = searchCategory.getCategoryId();
		if (categoryId == null || categoryId.trim().isEmpty()) {
			throw new IllegalArgumentException("categoryId must not be null or empty!");
		}

		GlobalSearchableGUIProvider removedProvider = map.remove(categoryId);
		if (removedProvider != null) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.search.GlobalSearchGUIRegistry.provider_removed", categoryId);
		}
	}

	/**
	 * Returns the searchable gui provider for the given id. If the categoryId is unknown, returns {@code null}!
	 *
	 * @param categoryId
	 * 		the category id for which the registered searchable gui provider should be returned
	 * @return the category or {@code null} if it is not registered
	 */
	public GlobalSearchableGUIProvider getGUIProviderForSearchCategoryById(final String categoryId) {
		return map.get(categoryId);
	}

}
