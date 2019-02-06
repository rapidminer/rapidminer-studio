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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.search.event.GlobalSearchManagerEventHandler;
import com.rapidminer.tools.LogService;


/**
 * Takes care of basic functionality that is always identical for Global Search managers.
 * Any {@link GlobalSearchManager} implementation should extend this class instead of implementing the interface directly.
 * <p>
 * After {@link #init()} has happened, additional documents can be added/updated/removed at any time via
 * <ul>
 * <li>{@link #addDocumentToIndex(Document)}</li>
 * <li>{@link #addDocumentsToIndex(List)}</li>
 * <li>{@link #removeDocumentFromIndex(Document)}</li>
 * <li>{@link #removeDocumentsFromIndex(List)}</li>
 * </ul>
 * </p>
 * <p>
 *     <h3>How should {@link Document}s be constructed?</h3>
 *     Most of the things are taken care of automatically, but to actually add content that can be searched, you need to follow the steps below:
 *     <ol>
 *         <li>Create {@link Field}s that should contain the searchable content, use the utility functions in the {@link GlobalSearchUtilities} class, e.g. {@link GlobalSearchUtilities#createFieldForTexts(String, String)}</li>
 *         <li><i>Optional: </i>If you want your results to be sorted in a given way, add the field created via {@link GlobalSearchUtilities#createSortingField(long)} to the document</li>
 *         <li>Call {@link GlobalSearchUtilities#createDocument(String, String, Field...)} and pass the fields you created</li>
 *     </ol>
 * </p>
 *
 * @author Marco Boeck
 * @since 8.1
 */
public abstract class AbstractGlobalSearchManager implements GlobalSearchManager {

	private final GlobalSearchManagerEventHandler eventHandler;

	private final Collection<Document> addedDocuments;
	private final Collection<Document> removedDocuments;

	private final AtomicBoolean initialized;

	private final String categoryId;
	private final Map<String, String> additionalFieldDescriptions;
	private final Collection<GlobalSearchDefaultField> additionalDefaultSearchFields;


	/**
	 * Creates the abstract global search manager which implements a lot of basic functionality required for every manager.
	 *
	 * @param categoryId
	 * 		the search category Id which was used to register to the {@link GlobalSearchRegistry}
	 * @param additionalFieldDescriptions
	 * 		see {@link GlobalSearchManager#getAdditionalFieldDescriptions()}. Can be {@code null} if no additional fields are created.
	 * @param additionalDefaultSearchFields
	 * 		see {@link GlobalSearchManager#getAdditionalDefaultSearchFields()}. Can be {@code null} for no additional default search fields.
	 */
	protected AbstractGlobalSearchManager(String categoryId, Map<String, String> additionalFieldDescriptions, GlobalSearchDefaultField... additionalDefaultSearchFields) {
		if (categoryId == null || categoryId.trim().isEmpty()) {
			throw new IllegalArgumentException("categoryId must not be null or empty!");
		}

		this.categoryId = categoryId;
		this.additionalFieldDescriptions = additionalFieldDescriptions;
		this.additionalDefaultSearchFields = additionalDefaultSearchFields != null ? Collections.unmodifiableList(Arrays.asList(additionalDefaultSearchFields)) : Collections.emptyList();

		initialized = new AtomicBoolean(false);

		eventHandler = new GlobalSearchManagerEventHandler(categoryId);

		addedDocuments = Collections.synchronizedList(new ArrayList<>());
		removedDocuments = Collections.synchronizedList(new ArrayList<>());

	}

	/**
	 * Remove the {@link Document}s which were set in class variable removedDocuments before.
	 */
	private void removeDocuments() {
		List<Document> copyRemovedDocuments;
		synchronized (removedDocuments) {
			copyRemovedDocuments = new ArrayList<>(removedDocuments);
			removedDocuments.clear();
		}

		if (!copyRemovedDocuments.isEmpty()) {
			getSearchManagerEventHandler().fireDocumentsRemoved(copyRemovedDocuments);
		}
	}

	/**
	 * Add {@link Document}s which were set in the addedDocuments class variable before.
	 */
	private void addDocuments() {
		List<Document> copyAddedDocuments;
		synchronized (addedDocuments) {
			copyAddedDocuments = new ArrayList<>(addedDocuments);
			addedDocuments.clear();
		}

		if (!copyAddedDocuments.isEmpty()) {
			// we always update instead of just adding so that new info for same location is stored
			getSearchManagerEventHandler().fireDocumentsUpdated(copyAddedDocuments);
		}
	}

	/**
	 * Initializes this Global Search manager. Calling multiple times has no additional effect after the first time.
	 */
	@Override
	public final void initialize() {
		if (!initialized.get()) {
			try {
				init();

				addDocuments();
				removeDocuments();
				// no error? Set initialized
				initialized.set(true);
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.search.AbstractGlobalSearchManager.error.init", e);
			}

			// do the fast indexing in a progress thread
			ProgressThread pg = new ProgressThread("global_search.manager.init_index." + getSearchCategoryId()) {
				@Override
				public void run() {
					try {
						List<Document> initialDocuments = createInitialIndex(this);

						// add initial documents.
						getSearchManagerEventHandler().fireDocumentsUpdated(initialDocuments);
					} catch (Exception e) {
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.search.AbstractGlobalSearchManager.error.init", e);
					} finally {
						getProgressListener().complete();
					}
				}
			};
			pg.start();
		}
	}

	@Override
	public boolean isInitialized() {
		return initialized.get();
	}

	@Override
	public String getSearchCategoryId() {
		return categoryId;
	}

	@Override
	public Map<String, String> getAdditionalFieldDescriptions() {
		return additionalFieldDescriptions;
	}

	@Override
	public Collection<GlobalSearchDefaultField> getAdditionalDefaultSearchFields() {
		return additionalDefaultSearchFields;
	}

	@Override
	public GlobalSearchManagerEventHandler getSearchManagerEventHandler() {
		return eventHandler;
	}

	/**
	 * Adds the given document to/Updates the given document on the index.
	 * <p>
	 *     Will not fire until {@link #init()} is done.
	 * </p>
	 *
	 * @param toAdd
	 * 		the document to add to the index
	 */
	protected void addDocumentToIndex(final Document toAdd) {
		if (toAdd == null) {
			throw new IllegalArgumentException("toAdd must not be null!");
		}

		addedDocuments.add(toAdd);

		// only fire once init is done
		if (initialized.get()) {
			addDocuments();
		}
	}

	/**
	 * Adds the given documents to/Updates the given documents on the index.
	 * <p>
	 *     Will not fire until {@link #init()} is done.
	 * </p>
	 *
	 * @param toAdd
	 * 		the documents to add to the index
	 */
	protected void addDocumentsToIndex(final List<Document> toAdd) {
		if (toAdd == null) {
			throw new IllegalArgumentException("toAdd must not be null!");
		}

		addedDocuments.addAll(toAdd);

		// only fire once init is done
		if (initialized.get()) {
			addDocuments();
		}
	}

	/**
	 * Deletes the given document from the index.
	 * <p>
	 *     Will not fire until {@link #init()} is done.
	 * </p>
	 *
	 * @param toDelete
	 * 		the document to remove from the index
	 */
	protected void removeDocumentFromIndex(final Document toDelete) {
		if (toDelete == null) {
			throw new IllegalArgumentException("toDelete must not be null!");
		}

		removedDocuments.add(toDelete);

		// only fire once init is done
		if (initialized.get()) {
			removeDocuments();
		}
	}

	/**
	 * Deletes the given documents from the index.
	 * <p>
	 *     Will not fire until {@link #init()} is done.
	 * </p>
	 *
	 * @param toDelete
	 * 		the documents to remove from the index
	 */
	protected void removeDocumentsFromIndex(final List<Document> toDelete) {
		if (toDelete == null) {
			throw new IllegalArgumentException("toDelete must not be null!");
		}

		removedDocuments.addAll(toDelete);

		// only fire once init is done
		if (initialized.get()) {
			removeDocuments();
		}
	}

	/**
	 * Put all the initialization here. This method should return fast to not block Studio start. See {@link #createInitialIndex(ProgressThread)} for a place to put initial search indexing.
	 */
	protected abstract void init();

	/**
	 * Put all the quick <strong>(!)</strong> initial indexing here. This method should return fast so that the user can search as soon as possible.
	 * If you need more expensive indexing, use {@link ProgressThread}s and call {@link #addDocumentsToIndex(List)} later.
	 * <p>
	 * Called in a {@link com.rapidminer.gui.tools.ProgressThread}. When running in GUI mode, the i18n key that is used is {@code gui.progress.global_search.manager.init_index.{category_id}.label}
	 * </p>
	 * <p>
	 * Any error handling needs to be done internally. This is not expected to throw.
	 * </p>
	 *
	 * @param progressThread
	 * 		this can be used to call {@link ProgressThread#checkCancelled()} to check for user stop request
	 * 		and to notify Studio about the progress of the initialization via {@link ProgressThread#getProgressListener()}.
	 * @return any documents that should be part of the Global Search index straight from the beginning. It is valid that the returned list is empty.
	 */
	protected abstract List<Document> createInitialIndex(final ProgressThread progressThread);
}
