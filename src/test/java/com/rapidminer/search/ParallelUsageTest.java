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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.search.event.GlobalSearchManagerListener;
import com.rapidminer.search.util.GlobalSearchableTextFakeImpl;


/**
 * Test the global search framework with parallel usage, running 3 threads which add and delete documents from 3 different categories.
 * At the same time 3 clients will use the search and make sure the correct results are found.
 * Also at the same time 3 more threads add and remove event listeners for the categories to make sure synchronization works properly.
 *
 * @author Andreas Timm
 * @since 8.1
 */
public class ParallelUsageTest {

	private static int DOCS_COUNT = 9; // has to be a multiple of the amount of elements in the texts list array
	private static int MIN_AVAILABLE_DOCS = 5; // will make at least this and at most +1 elements available to be searched

	private static final int DURATION_IN_SECONDS = 10;
	private static final int MAX_TRIES = 300;

	private static List<Instance> instances = new ArrayList<>();
	private static List<String[]> texts = new ArrayList<>();

	private static List<Exception> exceptions;
	private static List<Error> errors;


	static {
		// make the entries the same length
		texts.add(new String[]{"asdf", "hjkl", "life"});
		texts.add(new String[]{"qwer", "tzui", "life"});
		texts.add(new String[]{"yxcv", "bnmp", "life"});
	}

	@BeforeClass
	public static void setup() {
		GlobalSearchIndexer.INSTANCE.initialize();

		for (int i = 0; i < texts.size(); i++) {
			int finalI = i;
			GlobalSearchableTextFakeImpl.DocumentProvider provider = new GlobalSearchableTextFakeImpl.DocumentProvider() {
				private int first = 0;
				private int next = 0;

				@Override
				public List<Document> getDocuments() {
					List<Document> docs = new ArrayList<>();
					for (int i = 0; i < MIN_AVAILABLE_DOCS; i++) {
						// create it like
						// 0 asdf
						// 1 hjkl
						// 2 life
						// 3 asdf
						// ..
						docs.add(getDocument(i));
					}
					next = docs.size();
					return docs;
				}

				private Document getDocument(int i) {
					return GlobalSearchUtilities.INSTANCE.createDocument("category" + finalI + "_" + i, texts.get(finalI)[i % texts.get(finalI).length]);
				}

				@Override
				public Document getNext() {
					Document doc = getDocument(next);
					next = (next + 1) % DOCS_COUNT;
					return doc;
				}

				@Override
				public Document getFirst() {
					Document doc = getDocument(first);
					first = (first + 1) % DOCS_COUNT;
					return doc;
				}
			};
			instances.add(new Instance("instance" + i, i, provider));
		}

		for (Instance inst : instances) {
			GlobalSearchRegistry.INSTANCE.registerSearchCategory(inst.getSearchable());
		}
	}

	@Before
	public void waitTillReady() {
		int i = 0;
		for (Instance inst : instances) {
			while (!inst.getSearchable().getSearchManager().isInitialized()) {
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

		i = 0;
		while (!GlobalSearchIndexer.INSTANCE.isInitialized()) {
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (i++ > MAX_TRIES) {
				throw new IllegalStateException("waitTillReady did not complete in time.");
			}
		}
		exceptions = Collections.synchronizedList(new ArrayList<>());
		errors = Collections.synchronizedList(new ArrayList<>());

		// so lucene needs some time to get up and running, we can only start the tests if it is ready.
		boolean onefails = true;

		i = 0;
		while (onefails) {
			for (String[] text : texts) {
				for (String query : text) {
					try {
						Thread.sleep(100L);
						onefails = onefails && new GlobalSearchResultBuilder(query).runSearch().getNumberOfResults() == 0;
					} catch (ParseException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
			if (i++ > MAX_TRIES) {
				throw new IllegalStateException("waitTillReady did not complete in time.");
			}
		}
	}

	@AfterClass
	public static void teardown() {
		for (Instance inst : instances) {
			GlobalSearchRegistry.INSTANCE.unregisterSearchCategory(inst.getSearchable());
		}
	}

	@Test
	public void execute() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(instances.size() * 3);
		for (Instance inst : instances) {
			executorService.execute(inst);
			executorService.execute(new Searcher(inst));
			executorService.execute(new ChaosListener());
		}

		executorService.awaitTermination(DURATION_IN_SECONDS, TimeUnit.SECONDS);
		executorService.shutdown();

		for (Exception e : exceptions) {
			e.printStackTrace();
		}
		Assert.assertEquals("parallel execution did not run properly, there should be no Exceptions", 0, exceptions.size());

		synchronized (errors) {
			for (Error e : errors) {
				e.printStackTrace();
			}
		}
		Assert.assertEquals("parallel execution did not run properly, there should be no AssertionErrors", 0, errors.size());
	}

	private static class Instance implements Runnable {
		private final int id;
		private GlobalSearchableTextFakeImpl searchable;

		public Instance(String categoryId, int i, GlobalSearchableTextFakeImpl.DocumentProvider provider) {
			searchable = new GlobalSearchableTextFakeImpl(categoryId, provider);
			id = i;
		}

		public GlobalSearchable getSearchable() {
			return searchable;
		}

		@Override
		public void run() {
			while (true) {
				try {
					GlobalSearchableTextFakeImpl.TextGlobalSearchManagerTest searchManager = (GlobalSearchableTextFakeImpl.TextGlobalSearchManagerTest) searchable.getSearchManager();

					Document nextDoc = searchManager.getTestDocumentProvider().getNext();
					searchManager.addDocumentToIndex(nextDoc);
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
					}

					Document firstDoc = searchManager.getTestDocumentProvider().getFirst();
					searchManager.removeDocumentFromIndex(firstDoc);
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
					}
				} catch (Error error) {
					errors.add(error);
				} catch (Exception e) {
					exceptions.add(e);
				}
			}
		}
	}

	private class Searcher implements Runnable {
		private Instance instance;

		public Searcher(Instance inst) {
			instance = inst;
		}

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(99);
					int sum = 0;
					for (String query : texts.get(instance.id)) {
						GlobalSearchCategory category = GlobalSearchRegistry.INSTANCE.getSearchCategoryById(instance.getSearchable().getSearchManager().getSearchCategoryId());
						GlobalSearchResult searchResult = new GlobalSearchResultBuilder(query).setSearchCategories(category).runSearch();
						try {
							Assert.assertTrue(searchResult.getNumberOfResults() > 0);
							Assert.assertTrue("expecting 1 or 2 results for " + query + ", but got: " + searchResult.getNumberOfResults(), searchResult.getNumberOfResults() < 3);
						} catch (Error error) {
							errors.add(error);
						}
						sum += searchResult.getNumberOfResults();
					}
					Assert.assertTrue("every run should result in exactly 5 or 6 results, but this one has " + sum + " results", sum == MIN_AVAILABLE_DOCS || sum == MIN_AVAILABLE_DOCS + 1);
				} catch (InterruptedException e) {
				} catch (Error error) {
					errors.add(error);
				} catch (Exception e) {
					exceptions.add(e);
				}
			}
		}
	}

	private class ChaosListener implements Runnable, GlobalSearchManagerListener {
		private final Random rnd = new Random();

		@Override
		public void run() {
			while (true) {
				for (Instance in : instances) {
					if (rnd.nextBoolean()) {
						in.getSearchable().getSearchManager().getSearchManagerEventHandler().addEventListener(this);
					}
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				for (Instance in : instances) {
					if (rnd.nextBoolean()) {
						try {
							in.getSearchable().getSearchManager().getSearchManagerEventHandler().removeEventListener(this);
						} catch (IllegalArgumentException e) {

						}
					}
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void documentsAdded(String categoryId, Collection<Document> addedDocuments) {

		}

		@Override
		public void documentsUpdated(String categoryId, Collection<Document> updatedDocuments) {

		}

		@Override
		public void documentsRemoved(String categoryId, Collection<Document> removedDocuments) {

		}
	}
}
