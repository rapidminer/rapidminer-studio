/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
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
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.search.util.GlobalSearchableTextFakeImpl;


/**
 * Tests for all methods from class {@link GlobalSearchHandler}
 *
 * @author Andreas Timm
 * @since 8.1
 */
public class GlobalSearchHandlerTest {

	private static final int MAX_TRIES = 300;
	private static GlobalSearchableTextFakeImpl searchable;

	@BeforeClass
	public static void setup() {
		GlobalSearchIndexer.INSTANCE.initialize();

		searchable = new GlobalSearchableTextFakeImpl();
		GlobalSearchRegistry.INSTANCE.registerSearchCategory(searchable);
	}

	@Before
	public void waitTillReady() throws ParseException {
		int i = 0;
		while (!searchable.isInitialized()) {
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (i++ > MAX_TRIES) {
				throw new IllegalStateException("waitTillReady did not complete in time.");
			}
		}

		i = 0;
		boolean foundResults = false;
		while (!foundResults) {
			foundResults = new GlobalSearchResultBuilder("life").runSearch().getNumberOfResults() > 0;
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (i++ > MAX_TRIES) {
				throw new IllegalStateException("waitTillReady did not complete in time.");
			}
		}
	}

	@AfterClass
	public static void teardown() {
		GlobalSearchRegistry.INSTANCE.unregisterSearchCategory(searchable);
	}

	@Test
	public void searchNullQuery() throws ParseException {
		List<GlobalSearchCategory> categories = null;
		boolean simpleMode = true;
		int maxNumberOfResults = 23;
		boolean highlightResult = false;
		ScoreDoc after = null;
		final int moreResults = 0;
		GlobalSearchResult searchResult = GlobalSearchHandler.INSTANCE.search(null, categories, simpleMode, maxNumberOfResults, moreResults, highlightResult, after);
		Assert.assertEquals("The result has to be empty since nothing was added to the Global Search", 0, searchResult.getNumberOfResults());
	}

	@Test
	public void searchEmptyQuery() throws ParseException {
		String searchQueryString = "";
		List<GlobalSearchCategory> categories = null;
		boolean simpleMode = true;
		int maxNumberOfResults = 23;
		boolean highlightResult = false;
		ScoreDoc after = null;
		GlobalSearchResult searchResult = GlobalSearchHandler.INSTANCE.search(searchQueryString, categories, simpleMode, maxNumberOfResults, 0, highlightResult, after);
		Assert.assertEquals("The result has to be empty since nothing was added to the Global Search", 0, searchResult.getNumberOfResults());

		searchQueryString = "  \t  \n \t    \r  ";
		searchResult = GlobalSearchHandler.INSTANCE.search(searchQueryString, categories, simpleMode, maxNumberOfResults, 0, highlightResult, after);
		Assert.assertEquals("The result has to be empty since nothing was added to the Global Search", 0, searchResult.getNumberOfResults());
	}

	@Test
	public void testSearch() throws ParseException {
		List<GlobalSearchCategory> categories = null;
		boolean simpleMode = true;
		int maxNumberOfResults = 23;
		boolean highlightResult = false;
		ScoreDoc after = null;

		String searchQueryString = "searchQueryString";
		int expected = 0;
		GlobalSearchResult searchResult = checkGlobalSearchResult(categories, simpleMode, maxNumberOfResults, highlightResult, after, searchQueryString, expected);
		Assert.assertEquals("The result has to be empty, '" + searchQueryString + "' was not added to the Global Search", 0, searchResult.getNumberOfResults());

		searchQueryString = "life";
		expected = 3;
		GlobalSearchResult life = checkGlobalSearchResult(categories, simpleMode, maxNumberOfResults, highlightResult, after, searchQueryString, expected);
		Assert.assertEquals("The ROW info for the first document should be 32", "32", life.getResultDocuments().get(0).get(GlobalSearchableTextFakeImpl.ROW));
		Assert.assertEquals("The ROW info for the second document should be 36", "36", life.getResultDocuments().get(1).get(GlobalSearchableTextFakeImpl.ROW));
		Assert.assertEquals("The ROW info for the third document should be 57", "57", life.getResultDocuments().get(2).get(GlobalSearchableTextFakeImpl.ROW));
		checkGlobalSearchResult(categories, simpleMode, maxNumberOfResults, !highlightResult, after, searchQueryString, expected);

		expected = 7;
		simpleMode = false;
		searchQueryString = "*er";
		checkGlobalSearchResult(categories, simpleMode, maxNumberOfResults, highlightResult, after, searchQueryString, expected);
		checkGlobalSearchResult(categories, simpleMode, maxNumberOfResults, !highlightResult, after, searchQueryString, expected);
	}

	private GlobalSearchResult checkGlobalSearchResult(List<GlobalSearchCategory> categories, boolean simpleMode, int maxNumberOfResults, boolean highlightResult, ScoreDoc after, String searchQueryString, int expected) throws ParseException {
		GlobalSearchResult life = GlobalSearchHandler.INSTANCE.search(searchQueryString, categories, simpleMode, maxNumberOfResults, 0, highlightResult, after);
		Assert.assertEquals("Should have found " + expected + " results for " + (simpleMode ? "simple" : "advanced") + " searchQuery '" + searchQueryString + "'" + (after != null ? " after given ScoreDoc" : ""), expected, life.getNumberOfResults());
		if (highlightResult) {
			Assert.assertEquals("Should have found " + expected + " highlight results for " + (simpleMode ? "simple" : "advanced") + " searchQuery '" + searchQueryString + "'" + (after != null ? " after given ScoreDoc" : ""), expected, life.getBestFragments().size());
		} else {
			Assert.assertNull("No highlights expected", life.getBestFragments());
		}
		return life;
	}

	@Test
	public void searchInCategory() throws ParseException {
		List<GlobalSearchCategory> categories = new ArrayList<>(1);
		categories.add(GlobalSearchRegistry.INSTANCE.getSearchCategoryById(searchable.getSearchManager().getSearchCategoryId()));
		boolean highlightResult = false;
		int expected = 3;

		String searchQueryString = "life";
		checkGlobalSearchResult(categories, true, 23, highlightResult, null, searchQueryString, expected);
		checkGlobalSearchResult(categories, true, 23, !highlightResult, null, searchQueryString, expected);
	}

	@Test
	public void searchAfter() throws ParseException {
		int maxNumberOfResults = 2;
		String searchQueryString = "*er";
		boolean simpleMode = false;
		boolean highlightResult = false;
		List<GlobalSearchCategory> categories = null;
		ScoreDoc after = null;
		checkGlobalSearchResult(categories, simpleMode, maxNumberOfResults, highlightResult, after, searchQueryString, maxNumberOfResults);
		GlobalSearchResult anyer = checkGlobalSearchResult(categories, simpleMode, maxNumberOfResults, !highlightResult, after, searchQueryString, maxNumberOfResults);
		Assert.assertEquals("Should have found " + maxNumberOfResults + " results for not simple searchQuery '" + searchQueryString + "' first search", maxNumberOfResults, anyer.getNumberOfResults());

		after = anyer.getLastResult();
		checkGlobalSearchResult(categories, simpleMode, maxNumberOfResults, highlightResult, after, searchQueryString, maxNumberOfResults);
		anyer = checkGlobalSearchResult(categories, simpleMode, maxNumberOfResults, !highlightResult, after, searchQueryString, maxNumberOfResults);
		Assert.assertEquals("Should have found " + maxNumberOfResults + " results for not simple searchQuery '" + searchQueryString + "' after previous search last result", maxNumberOfResults, anyer.getNumberOfResults());

		after = anyer.getLastResult();
		checkGlobalSearchResult(categories, simpleMode, maxNumberOfResults, highlightResult, after, searchQueryString, maxNumberOfResults);
		anyer = checkGlobalSearchResult(categories, simpleMode, maxNumberOfResults, !highlightResult, after, searchQueryString, maxNumberOfResults);
		Assert.assertEquals("Should have found " + maxNumberOfResults + " results for not simple searchQuery '" + searchQueryString + "' after previous search last result", maxNumberOfResults, anyer.getNumberOfResults());

		after = anyer.getLastResult();
		int expected = 1;
		checkGlobalSearchResult(categories, simpleMode, maxNumberOfResults, highlightResult, after, searchQueryString, expected);
		anyer = checkGlobalSearchResult(categories, simpleMode, maxNumberOfResults, !highlightResult, after, searchQueryString, expected);
		Assert.assertEquals("Should have found " + 0 + " results for not simple searchQuery '" + searchQueryString + "' after previous search last result", expected, anyer.getNumberOfResults());
	}

	@Test
	public void useAdvancedSyntax() throws ParseException {
		String searchQueryString = "let AND go";
		boolean simpleMode = true;
		boolean highlightResult = false;
		int expected = 8;
		checkGlobalSearchResult(null, simpleMode, 35, highlightResult, null, searchQueryString, expected);
		checkGlobalSearchResult(null, simpleMode, 35, !highlightResult, null, searchQueryString, expected);
	}

	@Test
	public void formatFindings() throws ParseException {
		String searchQueryString = "magnifico";
		boolean simpleMode = true;
		boolean highlightResult = true;
		int expected = 1;
		checkGlobalSearchResult(null, simpleMode, 35, highlightResult, null, searchQueryString, expected);
		checkGlobalSearchResult(null, simpleMode, 35, !highlightResult, null, searchQueryString, expected);
	}
}
