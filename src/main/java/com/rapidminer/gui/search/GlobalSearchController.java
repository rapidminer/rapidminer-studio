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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Timer;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;

import com.rapidminer.gui.search.model.GlobalSearchModel;
import com.rapidminer.gui.tools.MultiSwingWorker;
import com.rapidminer.search.GlobalSearchCategory;
import com.rapidminer.search.GlobalSearchRegistry;
import com.rapidminer.search.GlobalSearchResult;
import com.rapidminer.search.GlobalSearchResultBuilder;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Controller for the {@link GlobalSearchDialog} and {@link GlobalSearchPanel}.
 *
 * @author Marco Boeck
 * @since 8.1
 */
final class GlobalSearchController {

	/** number of search results that are displayed by default per category if all categories are searched */
	private static final int NUMBER_OF_RESULTS_ALL_CATS = 3;

	/** number of search results that are displayed by default if only a single category is searched */
	private static final int NUMBER_OF_RESULTS_SINGLE_CAT = 10;
	private static final int LOGGING_DELAY = 1000;

	private final GlobalSearchModel model;
	private final GlobalSearchPanel searchPanel;

	private String lastQuery;
	private String lastCategoryFilter;

	/** counts the number of global searches. Always incrementing when a new search is triggered */
	private final AtomicInteger searchCounter;
	private Timer updateTimer;


	/**
	 * Creates a new controller instance for the given panel and model instances.
	 *
	 * @param searchPanel
	 * 		the search panel instance
	 * @param model
	 * 		the model instance
	 */
	GlobalSearchController(final GlobalSearchPanel searchPanel, final GlobalSearchModel model) {
		this.model = model;
		this.searchPanel = searchPanel;
		if (model == null) {
			throw new IllegalArgumentException("model must not be null!");
		}

		this.searchCounter = new AtomicInteger(0);
	}

	/**
	 * Handles loading more results (if there are any).
	 *
	 * @param previousResult
	 * 		the results of the already found results for this category
	 * @param categoryId
	 * 		the category to append to
	 */
	protected void loadMoreRows(final GlobalSearchResult previousResult, final String categoryId) {
		// if user already clicked load more rows, don't do anything
		if (model.isPending(categoryId)) {
			return;
		}

		int resultNumberLimit = lastCategoryFilter != null ? NUMBER_OF_RESULTS_SINGLE_CAT : NUMBER_OF_RESULTS_ALL_CATS;
		if (resultNumberLimit == previousResult.getPotentialNumberOfResults() - previousResult.getNumberOfResults() - 1) {
			resultNumberLimit++;
		}
		searchOrAppendForCategory(previousResult.getLastResult(), lastQuery, categoryId, resultNumberLimit);
	}


	/**
	 * Handles searching.
	 *
	 * @param query
	 * 		the search query entered by the user
	 * @param categoryFilter
	 * 		the category filter or {@code null} if all categories are to be searched
	 */
	protected void handleSearch(final String query, final String categoryFilter) {
		if (query == null || query.trim().isEmpty()) {
			searchPanel.hideComponents();
			searchPanel.requestFocusInWindow();
			return;
		}
		// should not happen unless Global Search initialization failed completely
		if (GlobalSearchRegistry.INSTANCE.getAllSearchCategories().isEmpty()) {
			model.setError(I18N.getGUIMessage("gui.dialog.global_search.no_categories.label"));
			return;
		}

		lastQuery = query;
		lastCategoryFilter = categoryFilter;

		// count new search
		searchCounter.incrementAndGet();

		if (categoryFilter != null) {
			searchOrAppendForCategory(null, query, categoryFilter, NUMBER_OF_RESULTS_SINGLE_CAT);
		} else {
			// no filter, search all -> one search per category for easy grouping and concurrency
			for (GlobalSearchCategory category : GlobalSearchRegistry.INSTANCE.getAllSearchCategories()) {
				searchOrAppendForCategory(null, query, category.getCategoryId(), NUMBER_OF_RESULTS_ALL_CATS);
			}
		}
	}

	/**
	 * Toggles the search "All Studio" category button in the GlobalSearchPanel as if the user clicked it.
	 */
	protected void searchAllCategories() {
		searchPanel.searchAll();
	}

	/**
	 * Gets the last category filter.
	 *
	 * @return the category id or {@code null} if "All Studio" was last searched
	 */
	protected String getLastCategoryFilter() {
		return lastCategoryFilter;
	}

	/**
	 * Get the model backing the Global Search UI.
	 *
	 * @return the model, never {@code null}
	 */
	protected GlobalSearchModel getModel() {
		return model;
	}

	/**
	 * Searches in the given category or appends new search results to the given category.
	 *
	 * @param offset
	 * 		appends results found after this one. If {@code null}, it's treated as a new search
	 * @param query
	 * 		the query to search for
	 * @param categoryId
	 * 		the category in which to search
	 * @param resultNumberLimit
	 * 		the number of results per search
	 */
	private void searchOrAppendForCategory(final ScoreDoc offset, final String query, final String categoryId, int resultNumberLimit) {
		final int searchCountSnapshot = searchCounter.get();
		MultiSwingWorker<GlobalSearchResult, Void> worker = new MultiSwingWorker<GlobalSearchResult, Void>() {

			@Override
			protected GlobalSearchResult doInBackground() throws Exception {
				model.setPending(categoryId, true);
				GlobalSearchResultBuilder builder = new GlobalSearchResultBuilder(query.trim()).setMaxNumberOfResults(resultNumberLimit).setMoreResults(1).setHighlightResult(true);
				builder.setSearchCategories(GlobalSearchRegistry.INSTANCE.getSearchCategoryById(categoryId)).setSearchOffset(offset).setSimpleMode(true);
				return builder.runSearch();
			}

			@Override
			protected void done() {
				// if another search has been started before this one could return its result, discard these results
				if (searchCounter.get() > searchCountSnapshot) {
					return;
				}

				try {
					model.setPending(categoryId, false);
					GlobalSearchResult result = get();
					// no error when grabbing result? Good, search worked, reset error on model and provide results to it
					model.setError(null);

					if (updateTimer != null) {
						updateTimer.stop();
					}
					updateTimer = new Timer(LOGGING_DELAY, e -> {
						logSearch(result);
						updateTimer = null;
					});
					updateTimer.setRepeats(false);
					updateTimer.start();

					if (offset != null) {
						model.appendResultForCategory(categoryId, result);
					} else {
						model.setResultForCategory(categoryId, result);
					}
				} catch (ExecutionException e) {
					handleSearchError(e);
				} catch (InterruptedException e) {
					// don't care, but don't silently swallow it anyway
					Thread.currentThread().interrupt();
				}
			}

			/**
			 * Log the given result to the usage statistics.
			 *
			 * @param result from a search in the global search framework
			 */
			private void logSearch(GlobalSearchResult result) {
				if (result != null) {
					String searchTerm = lastQuery;
					long numResults = result.getPotentialNumberOfResults();
					ActionStatisticsCollector.getInstance().logGlobalSearch(ActionStatisticsCollector.VALUE_TIMEOUT, searchTerm, categoryId, numResults);
				}
			}
		};
		worker.start();
	}

	/**
	 * Handles an error while searching.
	 *
	 * @param e
	 * 		the exception
	 */
	private void handleSearchError(ExecutionException e) {
		String message = null;
		Throwable cause = e.getCause();
		if (cause instanceof ParseException) {
			message = cause.getMessage() != null ? cause.getMessage() : cause.toString();
		} else if (cause == null) {
			message = e.getMessage();
		}

		model.clearAllCategories();
		model.setError(message);
	}

	/**
	 * @return the latest executed search query
	 */
	public String getLastQuery() {
		return lastQuery;
	}
}
