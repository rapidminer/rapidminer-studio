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

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.search.util.GlobalSearchableTextFakeImpl;


/**
 * Tests for methods of class {@link GlobalSearchResultBuilder}
 *
 * @author Andreas Timm
 * @since 8.1
 */
public class GlobalSearchResultBuilderTest {

	private static final int MAX_TRIES = 300;
	private static GlobalSearchableTextFakeImpl searchable;

	@BeforeClass
	public static void setup() {
		GlobalSearchIndexer.INSTANCE.initialize();

		searchable = new GlobalSearchableTextFakeImpl();
		GlobalSearchRegistry.INSTANCE.registerSearchCategory(searchable);
	}

	@Before
	public void waitTillReady() {
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
	}

	@AfterClass
	public static void teardown() {
		GlobalSearchRegistry.INSTANCE.unregisterSearchCategory(searchable);
	}

	@Test
	public void testVariants() throws ParseException {
		final String[] queries = {"carry", "bismillah", "me", "nothing else matters", "nothing AND matters"};
		final boolean[] bools = {Boolean.FALSE, Boolean.TRUE};
		final int maxMaxNumberOfResults = 10;
		ScoreDoc after = null;

		GlobalSearchCategory[] categories = null;
		for (String query : queries) {
			for (boolean highlightResult : bools) {
				for (int maxNumberOfResults = 1; maxNumberOfResults <= maxMaxNumberOfResults; maxNumberOfResults++) {
					for (boolean simpleMode : bools) {
						GlobalSearchResultBuilder gsr = new GlobalSearchResultBuilder(query).setHighlightResult(highlightResult).setMaxNumberOfResults(maxNumberOfResults).setSimpleMode(simpleMode).setSearchOffset(after).setSearchCategories(categories);
						GlobalSearchResult searchResult = gsr.runSearch();
						Assert.assertNotNull(searchResult);
						if (highlightResult) {
							Assert.assertEquals(searchResult.getNumberOfResults(), searchResult.getBestFragments().size());
						} else {
							Assert.assertNull(searchResult.getBestFragments());
						}
						Assert.assertTrue("Should not find more results than expected", searchResult.getNumberOfResults() <= maxMaxNumberOfResults);
					}
				}
			}
		}
	}

	@Test
	public void justSearch() throws ParseException {
		GlobalSearchResult life = new GlobalSearchResultBuilder("life").runSearch();
		int expected = 3;
		Assert.assertEquals("Should have found 3 results for searchQuery 'life'", expected, life.getNumberOfResults());
		Assert.assertEquals("The ROW info for the first document should be 32", "32", life.getResultDocuments().get(0).get(GlobalSearchableTextFakeImpl.ROW));
		Assert.assertEquals("The ROW info for the second document should be 36", "36", life.getResultDocuments().get(1).get(GlobalSearchableTextFakeImpl.ROW));
		Assert.assertEquals("The ROW info for the third document should be 57", "57", life.getResultDocuments().get(2).get(GlobalSearchableTextFakeImpl.ROW));
	}

	@Test(expected = IllegalArgumentException.class)
	public void badMaxNumberResultsMinusDroelf() {
		new GlobalSearchResultBuilder("scara").setMaxNumberOfResults(Integer.MIN_VALUE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void badMaxNumberResultsMinusOne() {
		new GlobalSearchResultBuilder("scara").setMaxNumberOfResults(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void badMaxNumberResultsZerro() {
		new GlobalSearchResultBuilder("scara").setMaxNumberOfResults(0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void badNullQuery() {
		new GlobalSearchResultBuilder(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void badWhitespaceQuery() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 256; i++) {
			if (Character.isWhitespace(i)) {
				sb.append((char) i);
			}
		}
		new GlobalSearchResultBuilder(sb.toString());
	}

	@Test
	public void badScoreDoc() throws ParseException {
		int maxNumberOfResults = 2;
		String searchQueryString = "*er";
		GlobalSearchResult anyer = new GlobalSearchResultBuilder(searchQueryString).setMaxNumberOfResults(maxNumberOfResults).setSearchCategories(GlobalSearchRegistry.INSTANCE.getSearchCategoryById(searchable.getCategoryId())).runSearch();
		Assert.assertEquals("Should have found " + maxNumberOfResults + " results for not simple searchQuery '" + searchQueryString + "' first search", maxNumberOfResults, anyer.getNumberOfResults());
		ScoreDoc after = anyer.getLastResult();

		GlobalSearchResult life = new GlobalSearchResultBuilder("life").setSearchOffset(after).runSearch();
		for (Document doc : life.getResultDocuments()) {
			System.out.println(doc);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void badCategory() {
		new GlobalSearchResultBuilder("no").setSearchCategories(GlobalSearchRegistry.INSTANCE.getSearchCategoryById("thisAintNoRealCategory"));
	}
}
