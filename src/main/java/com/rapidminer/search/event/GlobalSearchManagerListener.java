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

import java.util.Collection;
import java.util.EventListener;

import org.apache.lucene.document.Document;

import com.rapidminer.search.GlobalSearchManager;


/**
 * Listens for events on a {@link GlobalSearchManager}.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public interface GlobalSearchManagerListener extends EventListener {

	/**
	 * Fired when {@link Document}s have been added.
	 *
	 * @param categoryId
	 * 		the id of the {@link GlobalSearchManager} search category
	 * @param addedDocuments
	 * 		the added documents, never {@code null}
	 */
	void documentsAdded(final String categoryId, final Collection<Document> addedDocuments);

	/**
	 * Fired when {@link Document}s have been updated.
	 *
	 * @param categoryId
	 * 		the id of the {@link GlobalSearchManager} search category
	 * @param updatedDocuments
	 * 		the updated documents, never {@code null}
	 */
	void documentsUpdated(final String categoryId, final Collection<Document> updatedDocuments);

	/**
	 * Fired when {@link Document}s have been removed.
	 *
	 * @param categoryId
	 * 		the id of the {@link GlobalSearchManager} search category
	 * @param removedDocuments
	 * 		the removed documents, never {@code null}
	 */
	void documentsRemoved(final String categoryId, final Collection<Document> removedDocuments);
}
