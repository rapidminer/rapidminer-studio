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
package com.rapidminer.search.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.search.AbstractGlobalSearchManager;
import com.rapidminer.search.GlobalSearchDefaultField;
import com.rapidminer.search.GlobalSearchHandlerTest;
import com.rapidminer.search.GlobalSearchManager;
import com.rapidminer.search.GlobalSearchRegistry;
import com.rapidminer.search.GlobalSearchUtilities;
import com.rapidminer.search.GlobalSearchable;


/**
 * The test setup for a GlobalSearchable with contained GlobalSearchManager
 *
 * @author Andreas Timm
 * @since 8.1
 */
public class GlobalSearchableTextFakeImpl implements GlobalSearchable {

	public static final String ROW = "ROW";
	public static final String DATA = "DATA";

	private GlobalSearchManager manager;

	private boolean initialized = false;
	private String CATEGORY_NAME = "texttest";

	public GlobalSearchableTextFakeImpl() {

	}

	@Override
	public GlobalSearchManager getSearchManager() {
		if (manager == null) {
			createManager(null);
		}
		return manager;
	}

	private void createManager(DocumentProvider docprovider) {
		Map<String, String> additionalFieldsDesc = new HashMap<>();
		additionalFieldsDesc.put(ROW, "the row that contains this data");
		additionalFieldsDesc.put(DATA, "the data contained in a row");
		manager = new TextGlobalSearchManagerTest(CATEGORY_NAME, additionalFieldsDesc, docprovider, new GlobalSearchDefaultField(DATA, 0.5f));
	}

	public GlobalSearchableTextFakeImpl(String categoryId, DocumentProvider provider) {
		this(categoryId);
		createManager(provider);
	}

	public GlobalSearchableTextFakeImpl(String categoryId) {
		super();
		CATEGORY_NAME = categoryId;
	}

	public String getCategoryId() {
		return CATEGORY_NAME;
	}


	private void ready() {
		initialized = true;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public class TextGlobalSearchManagerTest extends AbstractGlobalSearchManager {

		private DocumentProvider testDocumentProvider;

		/**
		 * Creates the text test global search manager
		 *
		 * @param categoryId
		 * 		the search category Id which was used to register to the {@link GlobalSearchRegistry}
		 * @param additionalFieldDescriptions
		 * 		see {@link GlobalSearchManager#getAdditionalFieldDescriptions()}. Can be {@code null} if no additional
		 * 		fields are created.
		 * @param additionalDefaultSearchFields
		 * 		see {@link GlobalSearchManager#getAdditionalDefaultSearchFields()}. Can be {@code null} for no additional
		 * 		default search fields.
		 */
		protected TextGlobalSearchManagerTest(String categoryId, Map<String, String> additionalFieldDescriptions, DocumentProvider documentProvider, GlobalSearchDefaultField... additionalDefaultSearchFields) {
			super(categoryId, additionalFieldDescriptions, additionalDefaultSearchFields);
			testDocumentProvider = documentProvider;
		}

		@Override
		protected void init() {
			try {
				addDocumentsToIndex(createTestDocuments());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		protected List<Document> createInitialIndex(ProgressThread progressThread) {
			progressThread.addProgressThreadListener((finishedprogressThread) -> ready());
			try {
				if (testDocumentProvider != null) {
					return testDocumentProvider.getDocuments();
				}
				return createTestDocuments();
			} catch (IOException e) {
				e.printStackTrace();
				return Collections.emptyList();
			}
		}

		public List<Document> createTestDocuments() throws IOException {
			if (testDocumentProvider != null) {
				return testDocumentProvider.getDocuments();
			}
			InputStream inputStream = GlobalSearchHandlerTest.class.getResourceAsStream("/com/rapidminer/search/br.txt");
			List<Document> result = new ArrayList<>();
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			String line = null;
			int row = 0;
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) {
					row++;
					continue;
				}

				Field[] fields = new Field[2];
				fields[0] = GlobalSearchUtilities.INSTANCE.createFieldForIdentifiers(ROW, String.valueOf(row));
				fields[1] = GlobalSearchUtilities.INSTANCE.createFieldForTexts(DATA, line);
				Document document = GlobalSearchUtilities.INSTANCE.createDocument(String.valueOf(row), line.replaceAll(" ", "").substring(0, Math.min(4, line.replaceAll(" ", "").length())), fields);

				result.add(document);
				row++;
			}
			return result;
		}

		public DocumentProvider getTestDocumentProvider() {
			return testDocumentProvider;
		}
	}

	public interface DocumentProvider {
		List<Document> getDocuments();

		Document getNext();

		Document getFirst();
	}
}
