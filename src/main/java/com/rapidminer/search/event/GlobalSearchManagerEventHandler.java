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
package com.rapidminer.search.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;

import com.rapidminer.search.GlobalSearchManager;
import com.rapidminer.search.GlobalSearchable;
import com.rapidminer.search.GlobalSearchUtilities;


/**
 * Handles events relevant to searching for {@link GlobalSearchManager}s, e.g. addition/updates/removal
 * of documents. Must be called by the {@link GlobalSearchable} whenever something changes and thus the
 * search index should be updated. Needs the search category id (as defined in the {@link
 * GlobalSearchManager}).
 *
 * <p><strong>Attention: </strong>Please make sure that each {@link Document} has the following fields: <ul> <li>{@link
 * org.apache.lucene.document.TextField} with the key {@value GlobalSearchUtilities#FIELD_NAME} which contains the
 * name/title/header for a search document.</li> <li>{@link org.apache.lucene.document.StringField} with the key {@value
 * GlobalSearchUtilities#FIELD_UNIQUE_ID} which contains a unique id across the entire search category! This is necessary to
 * either update or remove documents from the search later on.</li> </ul>
 * </p>
 * You can use {@link GlobalSearchUtilities} to create documents.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public class GlobalSearchManagerEventHandler {

	private final List<GlobalSearchManagerListener> listeners = Collections.synchronizedList(new ArrayList<>());

	private final String categoryId;

	/**
	 * Hidden to prevent instantiation without category id.
	 */
	@SuppressWarnings("unused")
	private GlobalSearchManagerEventHandler() {
		throw new UnsupportedOperationException("must be created with a category id!");
	}

	/**
	 * Creates a new search manager event handler for the given search manager instance.
	 *
	 * @param categoryId
	 * 		the search category id for this event handler instance, must not be {@code null}
	 */
	public GlobalSearchManagerEventHandler(String categoryId) {
		this.categoryId = categoryId;
	}

	/**
	 * Adds a {@link GlobalSearchManagerListener} which should be informed of all changes to the search manager. To remove it
	 * again, call {@link #removeEventListener(GlobalSearchManagerListener)}.
	 *
	 * @param listener
	 * 		the listener instance to add
	 */
	public void addEventListener(final GlobalSearchManagerListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		listeners.add(listener);
	}

	/**
	 * Removes the {@link GlobalSearchManagerListener} from this registry.
	 *
	 * @param listener
	 * 		the listener instance to remove
	 */
	public void removeEventListener(final GlobalSearchManagerListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		listeners.remove(listener);
	}

	/**
	 * Notifies all registered {@link GlobalSearchManagerListener}s that new {@link Document}s have been added. See
	 * {@link GlobalSearchUtilities} for helper methods to create documents.
	 *
	 * @param addedDocuments
	 * 		the documents that have been added
	 */
	public void fireDocumentsAdded(Collection<Document> addedDocuments) {
		synchronized (listeners) {
			for (GlobalSearchManagerListener listener : listeners) {
				listener.documentsAdded(categoryId, addedDocuments);
			}
		}
	}

	/**
	 * Notifies all registered {@link GlobalSearchManagerListener}s that indexed {@link Document}s have been updated. See
	 * {@link GlobalSearchUtilities} for helper methods to create documents.
	 *
	 * @param updatedDocuments
	 * 		the documents that have been updated
	 */
	public void fireDocumentsUpdated(Collection<Document> updatedDocuments) {
		synchronized (listeners) {
			for (GlobalSearchManagerListener listener : listeners) {
				listener.documentsUpdated(categoryId, updatedDocuments);
			}
		}
	}

	/**
	 * Notifies all registered {@link GlobalSearchManagerListener}s that indexed {@link Document}s have been removed. See
	 * {@link GlobalSearchUtilities} for helper methods to create documents.
	 *
	 * @param removedDocuments
	 * 		the documents that have been removed
	 */
	public void fireDocumentsRemoved(Collection<Document> removedDocuments) {
		synchronized (listeners) {
			for (GlobalSearchManagerListener listener : listeners) {
				listener.documentsRemoved(categoryId, removedDocuments);
			}
		}
	}
}
