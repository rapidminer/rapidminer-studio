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
package com.rapidminer.gui.search.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.event.EventListenerList;

import org.apache.lucene.document.Document;

import com.rapidminer.gui.search.event.GlobalSearchCategoryEvent;
import com.rapidminer.gui.search.event.GlobalSearchCategoryEvent.CategoryEvent;
import com.rapidminer.gui.search.event.GlobalSearchEventListener;
import com.rapidminer.gui.search.event.GlobalSearchModelEvent;
import com.rapidminer.gui.search.event.GlobalSearchModelEvent.ModelEvent;
import com.rapidminer.search.GlobalSearchCategory;
import com.rapidminer.search.GlobalSearchRegistry;
import com.rapidminer.search.GlobalSearchResult;


/**
 * The model backing the Global Search result display.
 *
 * @author Marco Boeck
 * @since 8.1
 */
@SuppressWarnings({"WeakerAccess"})
public class GlobalSearchModel {

	/** contains the list of results for the current search per category. This is needed to allow dynamic addition of more results per category on user request */
	private final Map<String, List<GlobalSearchResult>> currentSearchResults;

	private final Map<String, AtomicBoolean> pendingSearch;

	private String errorMessage;

	private String correctionSuggestion;

	/** event listener for this model */
	private final EventListenerList eventListener;


	/**
	 * Creates a new Global Search model backing the {@link com.rapidminer.gui.search.GlobalSearchDialog}.
	 */
	public GlobalSearchModel() {
		this.eventListener = new EventListenerList();

		this.errorMessage = null;
		this.correctionSuggestion = null;

		this.pendingSearch = new HashMap<>();
		this.currentSearchResults = new HashMap<>();
	}

	/**
	 * Adds a {@link com.rapidminer.gui.search.event.GlobalSearchEventListener} which will be informed of all changes to this
	 * model.
	 *
	 * @param listener
	 *            the listener instance to add
	 */
	public void registerEventListener(final GlobalSearchEventListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		eventListener.add(GlobalSearchEventListener.class, listener);
	}

	/**
	 * Removes the {@link com.rapidminer.gui.search.event.GlobalSearchEventListener} from this model.
	 *
	 * @param listener
	 *            the listener instance to remove
	 */
	public void removeEventListener(final GlobalSearchEventListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		eventListener.remove(GlobalSearchEventListener.class, listener);
	}

	/**
	 * Gets whether there is a global search error.
	 *
	 * @return {@code true} if {@link #getError()} will return an error; {@code false} otherwise
	 */
	public boolean hasError() {
		return errorMessage != null;
	}

	/**
	 * Gets the global search error.
	 *
	 * @return the error or {@code null}
	 */
	public String getError() {
		return errorMessage;
	}

	/**
	 * Sets the global error message.
	 *
	 * @param errorMessage
	 * 		the error or {@code null}
	 */
	public void setError(final String errorMessage) {
		this.errorMessage = errorMessage;
		fireModelChanged(ModelEvent.ERROR_STATUS_CHANGED);
	}

	/**
	 * Gets whether there is a global correction suggestion.
	 *
	 * @return {@code true} if {@link #getCorrectionSuggestion()} will return a suggestion; {@code false} otherwise
	 */
	public boolean hasCorrectionSuggestion() {
		return correctionSuggestion != null;
	}

	/**
	 * Gets the global correction suggestion.
	 *
	 * @return the suggestion or {@code null}
	 */
	public String getCorrectionSuggestion() {
		return correctionSuggestion;
	}

	/**
	 * Sets the global correction suggestion.
	 *
	 * @param correctionSuggestion
	 * 		the suggestion or {@code null}
	 */
	public void setCorrectionSuggestion(final String correctionSuggestion) {
		this.correctionSuggestion = correctionSuggestion;
		fireModelChanged(ModelEvent.CORRECTION_SUGGESTION_CHANGED);
	}

	/**
	 * Returns the currently active search categories, i.e. the ones that
	 * <br/>
	 * a) have non-empty results
	 * <br/>
	 * or b) had non-empty results in their previous state and are now pending
	 *
	 * @return the list of categories, can be empty but never {@code null}
	 */
	public List<String> getActiveCategories() {
		List<String> activeCategories = new LinkedList<>();
		for (GlobalSearchCategory category : GlobalSearchRegistry.INSTANCE.getAllSearchCategories()) {
			if (hasCategoryResults(category.getCategoryId())) {
				activeCategories.add(category.getCategoryId());
			}
		}

		return activeCategories;
	}

	/**
	 * Get the rows for the given search category.
	 *
	 * @param categoryId
	 * 		the search category
	 * @return the rows for the given category. Returns an empty collection if no results have been found for this category
	 * or {@code null} if search is currently pending or there was an error.
	 */
	public List<GlobalSearchRow> getRowsForCategory(final String categoryId) {
		List<GlobalSearchResult> results;
		synchronized (currentSearchResults) {
			results = currentSearchResults.get(categoryId);
			if (results == null) {
				return Collections.emptyList();
			} else {
				// copy list
				results = new LinkedList<>(results);
			}
		}

		if (isPending(categoryId) || hasError()) {
			return null;
		}

		List<GlobalSearchRow> rows = new LinkedList<>();
		for (GlobalSearchResult result : results) {
			for (int i = 0; i < result.getResultDocuments().size(); i++) {
				Document resultDoc = result.getResultDocuments().get(i);
				String[] bestFragments = null;
				if (result.getBestFragments() != null) {
					bestFragments = result.getBestFragments().get(i);
				}
				rows.add(new GlobalSearchRow(resultDoc, bestFragments));
			}
		}

		return rows;
	}

	/**
	 * Sets the given search result for a category. To append to an existing search result (i.e. make more rows visible), call {@link #appendResultForCategory(String, GlobalSearchResult)}.
	 *
	 * @param categoryId
	 * 		the search category
	 * @param result
	 * 		the result to set
	 */
	public void setResultForCategory(final String categoryId, final GlobalSearchResult result) {
		synchronized (currentSearchResults) {
			List<GlobalSearchResult> existingList = currentSearchResults.get(categoryId);
			if (existingList == null) {
				existingList = new LinkedList<>();
				currentSearchResults.put(categoryId, existingList);
			} else {
				existingList.clear();
			}
			existingList.add(result);
		}

		fireCategoryChanged(CategoryEvent.CATEGORY_ROWS_CHANGED, categoryId, result);
	}

	/**
	 * Appends the given search result to a category. If no previous search results exist, will behave as if called {@link #setResultForCategory(String, GlobalSearchResult)}.
	 *
	 * @param categoryId
	 * 		the search category
	 * @param result
	 * 		the result to append
	 */
	public void appendResultForCategory(final String categoryId, final GlobalSearchResult result) {
		synchronized (currentSearchResults) {
			List<GlobalSearchResult> existingList = currentSearchResults.get(categoryId);
			if (existingList == null) {
				setResultForCategory(categoryId, result);
			} else {
				existingList.add(result);
				fireCategoryChanged(CategoryEvent.CATEGORY_ROWS_APPENDED, categoryId, result);
			}
		}
	}

	/**
	 * Returns if a given category has search results or did have search results and is now in pending.
	 * In other words, whether a category
	 * <br/>
	 * a) has non-empty results
	 * <br/>
	 * or b) had non-empty results in their previous state and is now pending
	 *
	 *
	 * @param categoryId
	 * 		the search category
	 * @return {@code true} if there are results for the search category or it did have results and is now pending; {@code false} if neither is the case
	 */
	public boolean hasCategoryResults(final String categoryId) {
		synchronized (currentSearchResults) {
			List<GlobalSearchResult> resultList = currentSearchResults.get(categoryId);
			return !(resultList == null || resultList.isEmpty() || resultList.get(0).getNumberOfResults() == 0);
		}
	}

	/**
	 * Gets the pending state for a category.
	 *
	 * @param categoryId
	 * 		the search category
	 * @return {@code true} if a search is currently running for this category, {@code false} if no search is running
	 */
	public boolean isPending(final String categoryId) {
		synchronized (pendingSearch) {
			return pendingSearch.get(categoryId) != null && pendingSearch.get(categoryId).get();
		}
	}

	/**
	 * Gets whether there is at Least one category that is in pending state.
	 *
	 * @return {@code true} if a search is currently running for any category, {@code false} if no search is running
	 */
	public boolean isAnyCategoryPending() {
		boolean anyPending = false;
		synchronized (pendingSearch) {
			for (GlobalSearchCategory category : GlobalSearchRegistry.INSTANCE.getAllSearchCategories()) {
				String categoryId = category.getCategoryId();
				anyPending |= pendingSearch.get(categoryId) != null && pendingSearch.get(categoryId).get();
			}
		}

		return anyPending;
	}

	/**
	 * Sets the pending state for a category.
	 *
	 * @param categoryId
	 * 		the search category
	 * @param pending
	 * 		{@code true} if a search is currently running for this category, {@code false} if no search is running
	 */
	public void setPending(final String categoryId, final boolean pending) {
		synchronized (pendingSearch) {
			AtomicBoolean isPending = pendingSearch.get(categoryId);
			if (isPending == null) {
				isPending = new AtomicBoolean(pending);
				pendingSearch.put(categoryId, isPending);
			} else {
				isPending.getAndSet(pending);
			}
		}

		fireCategoryChanged(CategoryEvent.CATEGORY_PENDING_STATUS_CHANGED, categoryId, null);
	}

	/**
	 * Clears the search results for all categories.
	 */
	public void clearAllCategories() {
		synchronized (currentSearchResults) {
			currentSearchResults.clear();
		}
		fireModelChanged(ModelEvent.ALL_CATEGORIES_REMOVED);
	}

	/**
	 * Fires the given {@link ModelEvent}.
	 *
	 * @param type
	 *            the event type
	 */
	private void fireModelChanged(final ModelEvent type) {
		// Notify the listeners
		for (GlobalSearchEventListener listener : eventListener.getListeners(GlobalSearchEventListener.class)) {
			listener.modelChanged(new GlobalSearchModelEvent(type));
		}
	}

	/**
	 * Fires the given {@link CategoryEvent}.
	 *
	 * @param type
	 * 		the event type
	 * @param categoryId
	 * 		the search category id that was changed
	 * 	@param result the search result that caused the change, or {code null} if it was a pending state change
	 */
	private void fireCategoryChanged(final CategoryEvent type, final String categoryId, final GlobalSearchResult result) {
		// Notify the listeners
		for (GlobalSearchEventListener listener : eventListener.getListeners(GlobalSearchEventListener.class)) {
			listener.categoryChanged(categoryId, new GlobalSearchCategoryEvent(type), result);
		}
	}
}
