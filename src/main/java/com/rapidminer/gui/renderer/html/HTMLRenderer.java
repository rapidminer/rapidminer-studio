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
package com.rapidminer.gui.renderer.html;

import java.io.Closeable;
import javax.swing.JComponent;


/**
 * Interface for HTML Renderer implementations
 *
 * @author Jonas Wilms-Pfau
 * @since 9.4.1
 */
public interface HTMLRenderer extends Closeable {

	/**
	 * Returns the HTML rendering component
	 *
	 * @return the component
	 */
	JComponent getComponent();

	/**
	 * Registers a POJO as javascript callback, which is bound to the given JS name and can be accessed from JavaScript.
	 * May not be supported.
	 * <p>
	 * Note: After this has been called, {@link #loadHTML(String)} needs to be called! Otherwise, this call has no
	 * effect as it is essentially behaving like JQueries document.ready functionality.
	 * </p>
	 *
	 * @param name            the name of the Javascript object
	 * @param callbackHandler the pojo
	 * @throws UnsupportedOperationException if the HTML renderer does not support this feature
	 */
	void registerJavascriptCallback(String name, Object callbackHandler);

	/**
	 * Executes javascript on the displayed HTML content. May not be supported.
	 *
	 * @param javascript the javascript to execute
	 * @return {@code true} if the javascript could be executed
	 * @throws UnsupportedOperationException if the HTML renderer does not support this feature
	 */
	boolean executeJavascript(String javascript);

	/**
	 * Loads the given html content. May return immediately and load asynchronously.
	 *
	 * @param html the html, must not be {@code null}
	 */
	void loadHTML(String html);

	/**
	 * Loads the given html content. Will not return before loading finished.
	 *
	 * @param html the html, must not be {@code null}
	 */
	void loadHTMLAndWait(String html);

	/**
	 * Must be called if the HTML renderer is not used anymore.
	 */
	default void close() {
		// does nothing by default
	}
}
