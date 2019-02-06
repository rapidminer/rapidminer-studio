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
package com.rapidminer.search;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;


/**
 * Fetches the search results by using a builder pattern. Call {@link #runSearch()} to execute the search query and
 * get a {@link GlobalSearchResult}.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public final class GlobalSearchResultBuilder {

	private static final int MAX_RESULTS_DEFAULT = 20;
	private static final int MORE_RESULTS_DEFAULT = 0;

	private final String searchTerm;
	private int maxNumberOfResults;
	private int showMoreResults;
	private boolean highlightResult;
	private boolean simpleMode;
	private ScoreDoc after;
	private List<GlobalSearchCategory> categories;

	/**
	 * Creates the search result builder for this search query with defaults for all other options.
	 *
	 * @param searchQuery
	 * 		the search query string, must neither be {@code null} nor empty
	 * @throws IllegalArgumentException
	 * 		if searchQuery is {@code null} or empty
	 * @throws IllegalStateException
	 * 		if {@link GlobalSearchIndexer#isInitialized()} returns {@code false}
	 */
	public GlobalSearchResultBuilder(final String searchQuery) {
		if (searchQuery == null || searchQuery.trim().isEmpty()) {
			throw new IllegalArgumentException("searchQuery must neither be null nor empty!");
		}
		if (!GlobalSearchIndexer.INSTANCE.isInitialized()) {
			throw new IllegalStateException("GlobalSearchIndexer is not initialized, cannot search");
		}
		this.searchTerm = searchQuery;

		//set default values
		this.maxNumberOfResults = MAX_RESULTS_DEFAULT;
		this.showMoreResults = MORE_RESULTS_DEFAULT;
		this.highlightResult = false;
		this.after = null;
		this.categories = null;
	}

	/**
	 * Set the maximum number of results to return.
	 *
	 * <p>By default, {@value #MAX_RESULTS_DEFAULT} are returned.</p>
	 *
	 * @param maxNumberOfResults
	 * 		the maximum number of results
	 * @return the builder instance
	 */
	public GlobalSearchResultBuilder setMaxNumberOfResults(final int maxNumberOfResults) {
		if (maxNumberOfResults <= 0) {
			throw new IllegalArgumentException("maxNumberOfResults must be > 0!");
		}
		this.maxNumberOfResults = maxNumberOfResults;
		return this;
	}

	/**
	 * Set the number of additional to the maximum number results to be returned with this search if there are not more
	 * than maxNumberOfResults + moreResults available.
	 *
	 * <p>By default, {@value #MORE_RESULTS_DEFAULT} additional values are returned.</p>
	 *
	 * @param moreResults
	 * 		the maximum number of additional results
	 * @return the builder instance
	 */
	public GlobalSearchResultBuilder setMoreResults(final int moreResults) {
		if (moreResults < 0) {
			throw new IllegalArgumentException("moreResults must be >= 0!");
		}
		this.showMoreResults = moreResults;
		return this;
	}

	/**
	 * Set if result matches for the {@value GlobalSearchUtilities#FIELD_NAME} field should be returned as {@link
	 * GlobalSearchResult#getBestFragments()}.
	 *
	 * <p>By default, search highlighting is disabled.</p>
	 *
	 * @param highlightResult
	 * 		if {@code true}, the {@link GlobalSearchResult#getBestFragments()} will be created
	 * @return the builder instance
	 */
	public GlobalSearchResultBuilder setHighlightResult(final boolean highlightResult) {
		this.highlightResult = highlightResult;
		return this;
	}

	/**
	 * Set an optional search offset. This can be used for result pagination by using {@link
	 * GlobalSearchResult#getLastResult()} as the offset for the next search.
	 *
	 * <p>By default, there is no offset and the n requested top-ranked documents are
	 * returned.</p>
	 *
	 * @param after
	 * 		<i>Optional</i>. If not {@code null}, then the search results are retrieved from ranks lower than the given
	 * 		{@link ScoreDoc}.
	 * @return the builder instance
	 */
	public GlobalSearchResultBuilder setSearchOffset(final ScoreDoc after) {
		this.after = after;
		return this;
	}

	/**
	 * Define which {@link GlobalSearchCategory}s to include in the search. See {@link GlobalSearchRegistry} for more
	 * information about registered search categories.
	 *
	 * <p>By default, all categories are included.</p>
	 *
	 * @param categories
	 * 		<i>Optional</i>. If {@code null} or not specified, all registered search categories are included in the
	 * 		search. If given, they have to registered at the {@link GlobalSearchRegistry}, otherwise an {@link
	 * 		IllegalArgumentException} will be thrown
	 * @return the builder instance
	 * @throws IllegalArgumentException
	 * 		if categories which are not registered are passed
	 */
	public GlobalSearchResultBuilder setSearchCategories(final GlobalSearchCategory... categories) {
		if (categories != null) {
			for (GlobalSearchCategory category : categories) {
				if (category == null) {
					throw new IllegalArgumentException("search category NULL does not exist!");
				} else if (!GlobalSearchRegistry.INSTANCE.isSearchCategoryRegistered(category.getCategoryId())) {
					throw new IllegalArgumentException("search category " + category.getCategoryId() + " is not registered!");
				}
			}
			this.categories = Arrays.asList(categories);
		}
		return this;
	}

	/**
	 * If simple mode is activated, will try to make the query as user-friendly as possible, e.g. making it a wildcard search, etc.
	 * This may have no effect if the user is using advanced query syntax in the search query himself!
	 * Exact behavior might differ between Studio versions.
	 * <p>
	 * Defaults to {@code false} if not specified.
	 * </p>
	 *
	 * @param simpleMode
	 * 		if {@code true}, will try to make the query automatically user-friendly (e.g. adding wildcards and making the search fuzzy); {@code false} will not make any changes to the query.
	 * 		Note that if a simplified query cannot be parsed, the search will fall back to non-simple mode automatically.
	 * @return the builder instance
	 */
	public GlobalSearchResultBuilder setSimpleMode(final boolean simpleMode) {
		this.simpleMode = simpleMode;
		return this;
	}

	/**
	 * Executes the actual search.
	 *
	 * @return the search result, never {@code null}
	 * @throws ParseException
	 * 		if the searchQuery was invalid
	 */
	public GlobalSearchResult runSearch() throws ParseException {
		return GlobalSearchHandler.INSTANCE.search(searchTerm, categories, simpleMode, maxNumberOfResults, showMoreResults, highlightResult, after);
	}
}
