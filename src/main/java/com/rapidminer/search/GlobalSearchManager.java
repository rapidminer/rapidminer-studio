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

import java.util.Collection;
import java.util.Map;

import org.apache.lucene.document.Document;

import com.rapidminer.search.event.GlobalSearchManagerEventHandler;


/**
 * Manages the search functionality for {@link GlobalSearchable} things.
 * Any implementation should extend {@link AbstractGlobalSearchManager} instead of implementing this interface directly.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public interface GlobalSearchManager {

	/**
	 * Init code goes here. Must be explicitly called, as it is not called automatically.
	 */
	void initialize();

	/**
	 * Returns whether this manager has finished initialization.
	 *
	 * @return {@code true} if initialization is finished successfully; {@code false} otherwise
	 */
	boolean isInitialized();

	/**
	 * Returns the ID of this searchable category. This ID must be unique across RapidMiner Studio!
	 *
	 * @return the category id, never {@code null}
	 */
	String getSearchCategoryId();

	/**
	 * Returns a map of all the custom fields that this {@link GlobalSearchManager} uses together with a human-readable
	 * explanation for its {@link Document}s. Used to get available search syntax information.
	 *
	 * @return the map with additional field information in the format {@code {key, information}} or {@code null} if no
	 * additional fields are created
	 */
	Map<String, String> getAdditionalFieldDescriptions();

	/**
	 * Returns all additional {@link org.apache.lucene.document.Field} keys and their relative weight which should be searched by default, i.e.
	 * when the user did not specify a field in the search query. An example where this is used are operators, where their tags are searched.
	 * By default, only the {@link GlobalSearchUtilities#FIELD_NAME} is searched.
	 * <p>
	 * Note that the higher the weight difference, the more hits of the higher weighted field are favored, i.e. ranked higher. The {@link GlobalSearchUtilities#FIELD_NAME} has a weight of {@code 1f}.
	 * </p>
	 *
	 * @return the keys and their relative weights of the additional fields for this {@link GlobalSearchable} to search or an empty collection if searching only
	 * the default name field is sufficient. Must not return {@code null}!
	 */
	Collection<GlobalSearchDefaultField> getAdditionalDefaultSearchFields();

	/**
	 * Returns the {@link GlobalSearchManagerEventHandler} instance which is used to listen for search document changes.
	 * You can create one for your search category via {@code new GlobalSearchManagerEventHandler(categoryID)}.
	 * <p> <strong>Attention:</strong> All additions/updates/removals of
	 * searchable items need to be made known via this {@link GlobalSearchManagerEventHandler}! Call {@link
	 * GlobalSearchManagerEventHandler#fireDocumentsAdded(Collection)}, {@link GlobalSearchManagerEventHandler#fireDocumentsUpdated(Collection)},
	 * and {@link GlobalSearchManagerEventHandler#fireDocumentsRemoved(Collection)} so that the {@link GlobalSearchHandler} will be
	 * able to find the changes! </p>
	 *
	 * @return the event handler, never {@code null}
	 */
	GlobalSearchManagerEventHandler getSearchManagerEventHandler();

	/**
	 * Returns the {@link GlobalSearchCategory} for this Manager.
	 * If the {@link GlobalSearchCategory#isVisible()} it will be displayed in the Global Search UI as a new category
	 * and it entries will appear in the All Studio search.
	 *
	 * @return the GlobalSearchCategory
	 */
	default GlobalSearchCategory getSearchCategory() {
		return new GlobalSearchCategory(this);
	}

}
