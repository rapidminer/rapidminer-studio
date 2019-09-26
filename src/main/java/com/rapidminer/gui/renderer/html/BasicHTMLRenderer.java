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

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;

import com.rapidminer.tools.RMUrlHandler;


/**
 * JEditor Pane based renderer which supports the RMUrlHandler.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.4.1
 */
class BasicHTMLRenderer implements HTMLRenderer {

	private final JEditorPane renderer;

	/**
	 * Creates a new JEditorPane with the given text
	 *
	 * @param text
	 * 		the html text
	 */
	BasicHTMLRenderer(String text) {
		renderer = new JEditorPane("text/html", text);
		renderer.addHyperlinkListener(e -> {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				RMUrlHandler.handleUrl(e.getDescription());
			}
		});
		renderer.setOpaque(false);
		renderer.setEditable(false);
		renderer.setBorder(null);
	}

	@Override
	public JComponent getComponent() {
		return renderer;
	}

	@Override
	public void registerJavascriptCallback(String name, Object callbackHandler) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean executeJavascript(String javascript) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void loadHTML(String html) {
		if (html == null) {
			throw new IllegalArgumentException("html must not be null!");
		}

		renderer.setText(html);
	}

	@Override
	public void loadHTMLAndWait(String html) {
		loadHTML(html);
	}
}