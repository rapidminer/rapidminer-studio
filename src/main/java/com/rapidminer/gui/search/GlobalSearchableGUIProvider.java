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
package com.rapidminer.gui.search;

import java.awt.dnd.DragGestureListener;
import javax.swing.JComponent;

import org.apache.lucene.document.Document;

import com.rapidminer.search.GlobalSearchManager;
import com.rapidminer.search.GlobalSearchResult;
import com.rapidminer.search.GlobalSearchUtilities;
import com.rapidminer.search.GlobalSearchable;


/**
 * Implement this to provide a GUI component to visualize search results provided by {@link com.rapidminer.search.GlobalSearchHandler}.
 * <p>Register your implementation to {@link GlobalSearchGUIRegistry} so it is automatically found for your {@link
 * GlobalSearchManager#getSearchCategoryId()}.
 * </p>
 * <p>
 * Do so when #initFinalChecks() is called for plugins, because afterwards the UI for the Global Search will be initialized.
 * </p>
 *
 * @author Marco Boeck
 * @since 8.1
 */
public interface GlobalSearchableGUIProvider {

	/**
	 * Can be used to veto closing the Global Search GUI upon a triggered search result (e.g. because the click is not supposed to do anything).
	 * @since 8.1
	 */
	class Veto {

		private boolean vetoGiven;

		/**
		 * Exercises the veto power.
		 */
		public void veto() {
			this.vetoGiven = true;
		}

		/**
		 * Returns if provider did veto the interaction.
		 *
		 * @return {@code true} if provider did veto; {@code false} otherwise
		 */
		protected boolean isVeto() {
			return vetoGiven;
		}
	}


	/**
	 * Creates a GUI Swing component for the provided document for visualizing {@link
	 * GlobalSearchResult}s. This one is displayed in the search result list, so:
	 * <ul>
	 * <li>It must not exceed {@value GlobalSearchGUIUtilities#MAX_HEIGHT}px in height - otherwise it will be rejected</li>
	 * <li>It must also not be opaque, so that {@link JComponent#isOpaque()} returns false</li>
	 * <li>Finally, it should try not exceed the width of the {@link GlobalSearchPanel}, otherwise it will simply be cut off</li>
	 * </ul>
	 * <p>
	 * <strong>Attention: This method must not block! If you need more time to compute something, then usage of a {@link com.rapidminer.gui.tools.MultiSwingWorker} is encouraged.
	 * In that case, this method should return with a placeholder UI first and then update it once the computation is done.</strong>
	 * </p>
	 *
	 * @param document
	 * 		the document for which a GUI list component should be created, never {@code null}
	 * @param bestFragments
	 * 		the best fragments with HTML-formatted highlighting for the match on the {@link
	 * 		GlobalSearchUtilities#FIELD_NAME} of the document. Can be {@code null}.
	 * 		Use {@link GlobalSearchGUIUtilities#createHTMLHighlightFromString(String, String[])} to create an HTML snippet highlighting the best matches.
	 * @return the component, never {@code null}
	 */
	JComponent getGUIListComponentForDocument(final Document document, final String[] bestFragments);


	/**
	 * The I18N name for your search category. Displayed in the search filter UI. Should not exceed 10 characters, will be shortened otherwise.
	 *
	 * @return the internationalized name, must not be {@code null} or empty
	 */
	String getI18nNameForSearchable();

	/**
	 * This method is called when a single search result from this {@link GlobalSearchable} has been activated (e.g. clicked
	 * on by the user). This should take the user to the result, e.g. by opening it. The implementation can veto the interaction, and by doing so veto the closing of the search results.
	 * This is useful if a result cannot be directly activated (either in general or because of the circumstances).
	 * <p>
	 * Note that this is fired on the EDT, so don't do computationally intensive tasks here. Spawn another thread if need be.
	 * </p>
	 *
	 * @param document
	 * 		the document which was activated
	 * @param veto
	 * 		Call {@link Veto#veto()} if the implementation vetoes this search result activation (e.g. because the click is not supposed to do anything)
	 */
	void searchResultTriggered(final Document document, final Veto veto);

	/**
	 * This method is called when a single search result from this {@link GlobalSearchable} is being browsed (e.g. user is hovering over it).
	 * This should show a preview of some description to the user. It will almost always be a pre-step to {@link #searchResultTriggered(Document, Veto)}.
	 * <p>
	 *     Note that this is fired on the EDT, so don't do computationally intensive tasks here! This must return immediately.
	 * </p>
	 *
	 * @param document
	 * 		the document which is being browsed
	 */
	void searchResultBrowsed(final Document document);

	/**
	 * This is used to determine if drag&drop is supported by this {@link GlobalSearchable}.
	 * This means that the {@link #getGUIListComponentForDocument(Document, String[])} can be dragged somewhere, e.g. the process canvas.
	 *
	 * If this is {@code true}, {@link #getDragAndDropSupport(Document)} will be used to register the drag source.
	 *
	 * @param document
	 * 		the document which should be dragged and dropped
	 * @return {@code true} if drag&drop is supported; {@code false} otherwise.
	 */
	boolean isDragAndDropSupported(final Document document);

	/**
	 * This is used to register the GUI component as a drag source to the UI. Only called if {@link #isDragAndDropSupported(Document)} returns {@code true}.
	 *
	 * @param document
	 * 		the document which should be dragged and dropped
	 * @return the drag gesture listener if {@link #isDragAndDropSupported(Document)} is {@code true}, otherwise this can return {@code null}
	 */
	DragGestureListener getDragAndDropSupport(final Document document);
}
