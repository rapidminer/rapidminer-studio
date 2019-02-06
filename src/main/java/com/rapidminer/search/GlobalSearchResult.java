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

import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;


/**
 * POJO for Global Search results. Contains all relevant information of the search and its results. Use {@link
 * GlobalSearchResultBuilder} to acquire {@link GlobalSearchResult}s.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public final class GlobalSearchResult {

	private String query;
	private List<Document> results;
	private ScoreDoc lastResult;
	private long maxPotentialResults;
	private List<String[]> bestFragments;

	/**
	 * Creates a new search results container.
	 *
	 * @param results
	 * 		the list of documents the search found
	 * @param query
	 * 		the query string that was used
	 * @param lastResult
	 * 		the last result if more exist than have been returned. Can be {@code null}
	 * @param maxPotentialResults
	 * 		the total number of result hits, or {code -1} if results are empty
	 * @param bestFragments
	 * 		the highlighted best fragments of the {@link GlobalSearchUtilities#FIELD_NAME} content, can be {@code null} if highlighting was disabled. Can also contain {@code null} elements.
	 */
	GlobalSearchResult(final List<Document> results, final String query, final ScoreDoc lastResult, final long maxPotentialResults, final List<String[]> bestFragments) {
		this.query = query;
		this.results = results;
		this.lastResult = lastResult;
		this.maxPotentialResults = maxPotentialResults;
		this.bestFragments = bestFragments;
	}

	/**
	 * Returns a list of the actual result documents of this search.
	 *
	 * @return the results which may be empty, never {@code null}
	 */
	public List<Document> getResultDocuments() {
		return results;
	}

	/**
	 * The last result. If more potential hits exist than results have been returned, you can pass this as an offset
	 * to the next search call (see {@link GlobalSearchResultBuilder#setSearchOffset(ScoreDoc)}) to fetch the next results.
	 *
	 * @return the lowest ranked result or {@code null} if the results are empty
	 */
	public ScoreDoc getLastResult() {
		return lastResult;
	}

	/**
	 * The number of results included in this {@link GlobalSearchResult}.
	 * See {@link #getPotentialNumberOfResults()} for how many potential results can been found
	 *
	 * @return the number of hits contained in this result
	 */
	public int getNumberOfResults() {
		return getResultDocuments().size();
	}

	/**
	 * Returns the total number of potential hits for the search query.
	 *
	 * @return the total number of hits or {@code -1} if the results are empty
	 */
	public long getPotentialNumberOfResults() {
		return maxPotentialResults;
	}

	/**
	 * The original query string.
	 * @return the query, never {@code null}
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Returns the highlighted best fragments of the {@link GlobalSearchUtilities#FIELD_NAME} content. Order is one-to-one same as the {@link #getResultDocuments()}.
	 *
	 * @return the html-formatted, highlighted best result fragments for the name field or {@code null} if no match was
	 * found, same order as {@link #getResultDocuments()}. Can also be {@code null} if highlighting was
	 * disabled. Use {@link com.rapidminer.gui.search.GlobalSearchGUIUtilities#createHTMLHighlightFromString(String, String[])} to create an HTML snippet highlighting the best matches.
	 */
	public List<String[]> getBestFragments() {
		return bestFragments;
	}

	/**
	 * Returns an empty result.
	 *
	 * @param query
	 * 		the query which delivered the empty result
	 * @return an empty result, never {@code null}
	 */
	protected static GlobalSearchResult createEmptyResult(final String query) {
		return new GlobalSearchResult(Collections.emptyList(), query, null, -1, null);
	}
}
