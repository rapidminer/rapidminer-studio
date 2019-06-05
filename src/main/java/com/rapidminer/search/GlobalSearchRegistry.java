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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.rapidminer.search.event.GlobalSearchRegistryEvent;
import com.rapidminer.search.event.GlobalSearchRegistryEventListener;
import com.rapidminer.tools.LogService;


/**
 * Register {@link GlobalSearchable} items here. Items registered here will be found when using the Global Search feature. You
 * can call {@link #registerSearchCategory(GlobalSearchable)} to register things for the Global Search feature. If you don't
 * want them to be searchable anymore, you can unregister them by calling {@link #unregisterSearchCategory(GlobalSearchable)}. <p>
 * To listen for registration changes, add a {@link GlobalSearchRegistryEventListener} by calling {@link
 * #addEventListener(GlobalSearchRegistryEventListener)}. </p>
 *
 * @author Marco Boeck
 * @since 8.1
 */
@SuppressWarnings("unused")
public enum GlobalSearchRegistry {

	INSTANCE;


	private final Map<String, GlobalSearchCategory> map = new ConcurrentHashMap<>();

	private final List<GlobalSearchRegistryEventListener> listeners = Collections.synchronizedList(new ArrayList<>());


	/**
	 * Adds a {@link GlobalSearchRegistryEventListener} which will be informed of all changes to this registry. To remove it
	 * again, call {@link #removeEventListener(GlobalSearchRegistryEventListener)}.
	 *
	 * @param listener
	 * 		the listener instance to add
	 */
	public void addEventListener(final GlobalSearchRegistryEventListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		listeners.add(listener);
	}

	/**
	 * Removes the {@link GlobalSearchRegistryEventListener} from this registry.
	 *
	 * @param listener
	 * 		the listener instance to remove
	 */
	public void removeEventListener(final GlobalSearchRegistryEventListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		listeners.remove(listener);
	}

	/**
	 * Registers the given {@link GlobalSearchable}. Will call {@link AbstractGlobalSearchManager#init()}.
	 * These are high-level search categories which are presented in the global Search UI.
	 *
	 * @param searchable
	 * 		The searchable instance which is asked to return indexable items
	 * @throws IllegalStateException
	 * 		if registering a searchable with an ID that is already registered
	 */
	public void registerSearchCategory(final GlobalSearchable searchable) {
		if (searchable == null) {
			throw new IllegalArgumentException("searchable must not be null!");
		}
		GlobalSearchManager searchManager = searchable.getSearchManager();
		if (searchManager == null) {
			throw new IllegalArgumentException("searchable must not return null for the GlobalSearchManager!");
		}
		if (searchManager.getAdditionalDefaultSearchFields() == null) {
			throw new IllegalArgumentException("getAdditionalDefaultSearchFields() must not return null!");
		}
		GlobalSearchCategory addedCategory = searchManager.getSearchCategory();
		if (addedCategory == null) {
			throw new IllegalArgumentException("getSearchCategory() must not return null for the GlobalSearchManager!");
		}
		String categoryId = addedCategory.getCategoryId();
		if (categoryId == null || categoryId.trim().isEmpty()) {
			throw new IllegalArgumentException("categoryId must not be null or empty!");
		}
		if (map.get(categoryId) != null) {
			throw new IllegalStateException("searchable " + categoryId + " already registered!");
		}
		if (!GlobalSearchIndexer.INSTANCE.isInitialized()) {
			throw new IllegalStateException("GlobalSearchIndexer not initialized, Global Search disabled!");
		}

		searchManager.initialize();
		map.put(categoryId, addedCategory);
		fireRegistryEvent(GlobalSearchRegistryEvent.RegistrationEvent.SEARCH_CATEGORY_REGISTERED, addedCategory);
		LogService.getRoot().log(Level.INFO, "com.rapidminer.search.GlobalSearchRegistry.category_added", addedCategory.getCategoryId());
	}

	/**
	 * Unregisters the given {@link GlobalSearchable}. If the given {@link GlobalSearchCategory} has already been unregistered, has
	 * no effect.
	 *
	 * @param searchable
	 * 		The searchable instance which should no longer provide search results
	 */
	public void unregisterSearchCategory(final GlobalSearchable searchable) {
		if (searchable == null) {
			throw new IllegalArgumentException("searchable must not be null!");
		}
		String categoryId = searchable.getSearchManager().getSearchCategoryId();
		if (categoryId == null || categoryId.trim().isEmpty()) {
			throw new IllegalArgumentException("categoryId must not be null or empty!");
		}

		GlobalSearchCategory removedCategory = map.remove(searchable.getSearchManager().getSearchCategoryId());
		if (removedCategory != null) {
			fireRegistryEvent(GlobalSearchRegistryEvent.RegistrationEvent.SEARCH_CATEGORY_UNREGISTERED, removedCategory);
			LogService.getRoot().log(Level.INFO, "com.rapidminer.search.GlobalSearchRegistry.category_removed", removedCategory.getCategoryId());
		}
	}

	/**
	 * Returns all registered search categories.
	 *
	 * @return the registered search categories, never {@code null}
	 */
	public List<GlobalSearchCategory> getAllSearchCategories() {
		return new ArrayList<>(map.values());
	}

	/**
	 * Returns the search category specified by the given id. If the categoryId is unknown, returns {@code null}!
	 *
	 * @param categoryId
	 * 		the category id for which the registered search category should be returned.
	 * @return the category or {@code null} if it is not registered
	 */
	public GlobalSearchCategory getSearchCategoryById(String categoryId) {
		return map.get(categoryId);
	}

	/**
	 * Checks if the given search category id is registered.
	 *
	 * @param categoryId
	 * 		the id, must not be {@code null}
	 * @return {@code true} if the search category is registered; {@code false} otherwise
	 */
	protected boolean isSearchCategoryRegistered(String categoryId) {
		return map.containsKey(categoryId);
	}


	/**
	 * Fires the given {@link GlobalSearchRegistryEvent}.
	 *
	 * @param type
	 * 		the event type
	 * @param category
	 * 		the event origin {@link GlobalSearchCategory} instance
	 */
	private void fireRegistryEvent(final GlobalSearchRegistryEvent.RegistrationEvent type, final GlobalSearchCategory category) {
		synchronized (listeners) {
			for (GlobalSearchRegistryEventListener listener : listeners) {
				listener.searchCategoryRegistrationChanged(new GlobalSearchRegistryEvent(type), category);
			}
		}
	}
}
