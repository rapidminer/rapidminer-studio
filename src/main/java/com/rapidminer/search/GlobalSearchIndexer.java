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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.rapidminer.RapidMiner;
import com.rapidminer.search.event.GlobalSearchManagerListener;
import com.rapidminer.search.event.GlobalSearchRegistryEvent;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;


/**
 * Takes care of preparing the Global Search index.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public enum GlobalSearchIndexer {

	INSTANCE;

	private Path indexDirectoryPath;

	private final ExecutorService pool = Executors.newFixedThreadPool(2);

	private volatile boolean setupError = false;

	private final AtomicBoolean initialized = new AtomicBoolean(false);

	private IndexWriter indexWriter;

	/** this listener makes sure that updates to each GlobalSearchable are reflected in the index */
	private final GlobalSearchManagerListener searchManagerListener = new GlobalSearchManagerListener() {

		@Override
		public void documentsAdded(final String categoryId, final Collection<Document> addedDocuments) {
			GlobalSearchCategory category = GlobalSearchRegistry.INSTANCE.getSearchCategoryById(categoryId);
			if (category != null) {
				setAdditionalFields(addedDocuments, category);

				pool.submit(() -> addDocuments(category, addedDocuments));
			}
		}

		@Override
		public void documentsUpdated(final String categoryId, final Collection<Document> updatedDocuments) {
			GlobalSearchCategory category = GlobalSearchRegistry.INSTANCE.getSearchCategoryById(categoryId);
			if (category != null) {
				setAdditionalFields(updatedDocuments, category);

				pool.submit(() -> updateDocuments(category, updatedDocuments));
			}
		}

		@Override
		public void documentsRemoved(final String categoryId, final Collection<Document> removedDocuments) {
			GlobalSearchCategory category = GlobalSearchRegistry.INSTANCE.getSearchCategoryById(categoryId);
			if (category != null) {
				pool.submit(() -> removeDocuments(category, removedDocuments));
			}
		}

		/**
		 * Sets the category and unique ID fields.
		 *
		 * @param documents
		 * 		the documents for which to add the fields
		 * @param category
		 * 		the category for which the documents are
		 */
		private void setAdditionalFields(Collection<Document> documents, GlobalSearchCategory category) {
			for (Document doc : documents) {
				// make sure doc has necessary fields
				if (!isDocValid(category.getCategoryId(), doc)) {
					continue;
				}
				// store category id to make searching only for specific categories possible
				doc.add(GlobalSearchUtilities.INSTANCE.createFieldForIdentifiers(GlobalSearchUtilities.FIELD_CATEGORY, category.getCategoryId()));
				doc.add(GlobalSearchUtilities.INSTANCE.createFieldForIdentifiers(GlobalSearchHandler.FIELD_INTERNAL_UNIQUE_ID, createInternalId(category.getCategoryId(), doc)));
			}
		}
	};


	/**
	 * Sets up the search indexer instance. Registers a listener to the {@link GlobalSearchRegistry} to be able to add/remove
	 * documents to/from the index in case search categories are (un)registered.
	 */
	GlobalSearchIndexer() {
		try {
			indexDirectoryPath = FileSystemService.getUserRapidMinerDir().toPath().resolve(FileSystemService.RAPIDMINER_INTERNAL_CACHE_SEARCH_INSTANCE_FULL);
			RapidMiner.addShutdownHook(this::shutdown);
			Files.createDirectory(indexDirectoryPath);
			// set up of Lucene is done in initialize()
		} catch (Exception e) {
			setupError = true;
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.global_search.searchindexer.setup_failed", e);
		}
	}

	/**
	 * Initializes the {@link GlobalSearchIndexer}. Calling multiple times has no effect.
	 */
	public void initialize() {
		if (!initialized.get()) {
			if (setupError) {
				// should not happen at this point, but better be safe
				return;
			}

			// create the single index writer
			try {
				indexWriter = createIndexWriter();
			} catch (Exception e) {
				// could not open the index. Try deleting the cache
				LogService.getRoot().log(Level.INFO, "com.rapidminer.global_search.searchindexer.setup_self_fix_start", e);
				boolean fixed = fixIndexCacheFolder();

				if (fixed) {
					LogService.getRoot().log(Level.INFO, "com.rapidminer.global_search.searchindexer.setup_self_fix_success");
				} else {
					// nothing worked, cannot setup Global Search
					setupError = true;
					LogService.getRoot().log(Level.SEVERE, "com.rapidminer.global_search.searchindexer.setup_failed", e);
					return;
				}
			}

			// add registry listener. If there is a setup error, we do not even get here
			GlobalSearchRegistry.INSTANCE.addEventListener((GlobalSearchRegistryEvent e, GlobalSearchCategory category) -> {
				// new registrations after initial indexing has been started/done
				if (e.getEventType() == GlobalSearchRegistryEvent.RegistrationEvent.SEARCH_CATEGORY_REGISTERED) {
					category.getManager().getSearchManagerEventHandler().addEventListener(searchManagerListener);
				} else if (e.getEventType() == GlobalSearchRegistryEvent.RegistrationEvent.SEARCH_CATEGORY_UNREGISTERED) {
					category.getManager().getSearchManagerEventHandler().removeEventListener(searchManagerListener);
					removeCategory(category);
				}
			});

			initialized.set(true);
		}
	}

	/**
	 * Returns whether the GlobalSearchIndexer was setup successfully.
	 *
	 * @return {@code true} if everything is ready; {@code false} otherwise
	 */
	public boolean isInitialized() {
		return !setupError && initialized.get();
	}

	/**
	 * Removes all documents of a search category from the index.
	 *
	 * @param category
	 * 		the category for which all documents should be removed
	 */
	private void removeCategory(final GlobalSearchCategory category) {
		try {
			Term categoryToDeleteTerm = new Term(GlobalSearchUtilities.FIELD_CATEGORY, category.getCategoryId());
			indexWriter.deleteDocuments(categoryToDeleteTerm);
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.global_search.searchindexer.remove_failed", new Object[]{category.getCategoryId(), e.getMessage()});
		}
	}

	/**
	 * Add the given documents to the index.
	 *
	 * @param category
	 * 		the origin of the search documents
	 * @param documents
	 * 		the documents to add to the index
	 */
	private void addDocuments(final GlobalSearchCategory category, final Collection<Document> documents) {
		try {
			indexWriter.addDocuments(documents);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.global_search.searchindexer.add_failed", new Object[] { category.getCategoryId(), e.getMessage() });
		}
	}


	/**
	 * Updates the documents for the given search category. Call this method if new documents/updated documents should
	 * be made available to the Global Search.
	 *
	 * @param category
	 * 		the search category. Must already be registered to the {@link GlobalSearchRegistry}.
	 * 	@param documents
	 * 		the documents to update on the index
	 * @throws IllegalStateException
	 * 		if the search category is not registered to the {@link GlobalSearchRegistry}
	 */
	private void updateDocuments(final GlobalSearchCategory category, final Collection<Document> documents) {
		for (Document doc : documents) {
			// make sure doc has necessary fields
			if (!isDocValid(category.getCategoryId(), doc)) {
				continue;
			}

			IndexableField field = doc.getField(GlobalSearchHandler.FIELD_INTERNAL_UNIQUE_ID);
			Term termToUpdate = new Term(field.name(), field.stringValue());
			try {
				indexWriter.updateDocument(termToUpdate, doc);
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.global_search.searchindexer.update_failed", new Object[]{category.getCategoryId(), e.getMessage()});
			}
		}
	}

	/**
	 * Removes all documents for the given search category from the index.
	 *
	 * @param category
	 * 		the origin of the search documents
	 * 	@param documents
	 * 		the documents to remove from the index
	 */
	private void removeDocuments(final GlobalSearchCategory category, final Collection<Document> documents) {
		Term[] termsToDelete = new Term[documents.size()];
		int index = 0;
		// these docs will likely not have the internal unique id set -> set it if needed
		for (Document doc : documents) {
			IndexableField field = doc.getField(GlobalSearchHandler.FIELD_INTERNAL_UNIQUE_ID);
			if (field != null) {
				termsToDelete[index++] = new Term(GlobalSearchHandler.FIELD_INTERNAL_UNIQUE_ID, field.stringValue());
			} else {
				termsToDelete[index++] = new Term(GlobalSearchHandler.FIELD_INTERNAL_UNIQUE_ID, createInternalId(category.getCategoryId(), doc));
			}
		}

		try {
			indexWriter.deleteDocuments(termsToDelete);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.global_search.searchindexer.remove_failed", new Object[] { category.getCategoryId(), e.getMessage() });
		}
	}

	/**
	 * Create the internal, application-unique id.
	 *
	 * @param categoryId
	 * 		the category id of the document
	 * @param document
	 * 		the document for which to generate the id
	 * @return the unique id
	 */
	private String createInternalId(final String categoryId, final Document document) {
		String uniqueId = document.getField(GlobalSearchUtilities.FIELD_UNIQUE_ID).stringValue();
		return categoryId + "_" + uniqueId;
	}

	/**
	 * Checks if the given {@link Document} adheres to the standards the Global Search needs. Logs if it is not.
	 *
	 * @param doc
	 * 		the document to check
	 * @return {@code true} if the document is considered valid; {@code false} otherwise
	 */
	private boolean isDocValid(final String categoryId, final Document doc) {
		if (doc.getField(GlobalSearchUtilities.FIELD_UNIQUE_ID) == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.global_search.searchindexer.discarded_document_missing_field",
					new Object[]{ categoryId, GlobalSearchUtilities.FIELD_UNIQUE_ID} );
			return false;
		}
		if (doc.getField(GlobalSearchUtilities.FIELD_NAME) == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.global_search.searchindexer.discarded_document_missing_field",
					new Object[]{ categoryId, GlobalSearchUtilities.FIELD_NAME} );
			return false;
		}

		return true;
	}

	/**
	 * Tries to fix the index folder by deleting it and all its content.
	 * @return {@code true} if the fix was successful, {@code false} otherwise
	 */
	private boolean fixIndexCacheFolder() {
		if (indexDirectoryPath != null && Files.exists(indexDirectoryPath)) {
			try {
				if (Files.isDirectory(indexDirectoryPath)) {
					FileUtils.deleteDirectory(indexDirectoryPath.toFile());
				} else {
					// this should not happen, but in case it does, delete the file
					Files.delete(indexDirectoryPath);
				}

				// a fix was applied, now try if we can use the index
				indexWriter = createIndexWriter();

				// all good, we fixed it!
				return true;
			} catch (Exception e) {
				LogService.getRoot().log(Level.SEVERE, "com.rapidminer.global_search.searchindexer.setup_self_fix_failed", e);
			}
		}

		return false;
	}

	/**
	 * Creates an instance of {@link IndexWriter}.
	 *
	 * @return the writer, never {@code null}
	 * @throws IOException
	 * 		if something goes wrong
	 */
	private IndexWriter createIndexWriter() throws IOException {
		Directory dir = FSDirectory.open(indexDirectoryPath);
		IndexWriterConfig config = new IndexWriterConfig(GlobalSearchUtilities.ANALYZER);
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		return new IndexWriter(dir, config);
	}

	/**
	 * Creates an instance of {@link IndexReader}.
	 *
	 * @return the reader, never {@code null}
	 * @throws IOException
	 * 		if something goes wrong
	 */
	protected IndexReader createIndexReader() throws IOException {
		return DirectoryReader.open(indexWriter, true, false);
	}

	/**
	 * Closes the {@link #indexWriter} and deletes the {@link #indexDirectoryPath}
	 */
	private void shutdown() {
		try (IndexWriter writer = indexWriter) {
			// null safe auto close
		} catch (Exception e) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.global_search.searchindexer.shutdown_failed", e);
		} finally {
			FileUtils.deleteQuietly(indexDirectoryPath.toFile());
		}
	}

}
