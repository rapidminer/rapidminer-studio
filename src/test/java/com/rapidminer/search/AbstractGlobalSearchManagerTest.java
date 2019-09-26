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

import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.search.event.GlobalSearchManagerListener;


/**
 * Test the existing impl from {@link AbstractGlobalSearchManager}
 *
 * @author Andreas Timm
 * @since 8.1
 */
public class AbstractGlobalSearchManagerTest {

	private static final int MAX_TRIES = 300;
	private static final String TEST = "test";

	private static AbstractGlobalSearchManagerTestImpl searchManagerTest;
	private static GlobalSearchManagerListener globalSearchManagerListenerTest;

	@BeforeClass
	public static void setup() {
		globalSearchManagerListenerTest = Mockito.mock(GlobalSearchManagerListener.class);
		searchManagerTest = new AbstractGlobalSearchManagerTestImpl();
		searchManagerTest.getSearchManagerEventHandler().addEventListener(globalSearchManagerListenerTest);
		searchManagerTest.initialize();
	}

	@Before
	public void waitTillReady() {
		int i = 0;
		while(!searchManagerTest.isInitialized()) {
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
		searchManagerTest.getSearchManagerEventHandler().removeEventListener(globalSearchManagerListenerTest);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addDocumentNullTest() {
		searchManagerTest.addDoc(null);
	}

	@Test
	public void addAndRemoveDocumentTest() throws InterruptedException {
		Document adoc = GlobalSearchUtilities.INSTANCE.createDocument("1", "title");
		Mockito.verify(globalSearchManagerListenerTest, Mockito.times(0)).documentsUpdated(Mockito.anyString(), Mockito.anyCollection());
		searchManagerTest.addDoc(adoc);
		Mockito.verify(globalSearchManagerListenerTest, Mockito.times(1)).documentsUpdated(Mockito.anyString(), Mockito.anyCollection());
		Mockito.verify(globalSearchManagerListenerTest, Mockito.times(0)).documentsRemoved(Mockito.anyString(), Mockito.anyCollection());
		searchManagerTest.remDoc(adoc);
		Mockito.verify(globalSearchManagerListenerTest, Mockito.times(1)).documentsUpdated(Mockito.anyString(), Mockito.anyCollection());
		Mockito.verify(globalSearchManagerListenerTest, Mockito.times(1)).documentsRemoved(Mockito.anyString(), Mockito.anyCollection());
	}

	@Test(expected = IllegalArgumentException.class)
	public void addDocumentsNullTest() {
		searchManagerTest.addDocs(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void remDocumentNullTest() {
		searchManagerTest.remDoc(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void remDocumentsNullTest() {
		searchManagerTest.remDocs(null);
	}


	private static class AbstractGlobalSearchManagerTestImpl extends AbstractGlobalSearchManager {

		protected AbstractGlobalSearchManagerTestImpl(String categoryId, Map<String, String> additionalFieldDescriptions, GlobalSearchDefaultField... additionalDefaultSearchFields) {
			super(categoryId, additionalFieldDescriptions, additionalDefaultSearchFields);
		}

		public AbstractGlobalSearchManagerTestImpl() {
			super(TEST, null);
		}

		@Override
		protected void init() {

		}

		@Override
		protected List<Document> createInitialIndex(ProgressThread progressThread) {
			return null;
		}

		private void addDoc(Document doc) {
			addDocumentToIndex(doc);
		}

		private void addDocs(List<Document> docs) {
			addDocumentsToIndex(docs);
		}

		private void remDoc(Document document) {
			removeDocumentFromIndex(document);
		}

		private void remDocs(List<Document> docs) {
			removeDocumentsFromIndex(docs);
		}
	}
}
