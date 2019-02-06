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
package com.rapidminer.gui.tools.components;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.tools.RMUrlHandler;


/**
 * A {@link ExtendedHTMLJEditorPane} that works analogously to {@link FixedWidthLabel}, breaking the
 * lines according to the width in pixel. In contrast to {@link FixedWidthLabel} a
 * {@link FixedWidthEditorPanel} can also handle hyperlinks.
 *
 *
 * @author Gisa Schaefer
 *
 */
public class FixedWidthEditorPane extends ExtendedHTMLJEditorPane {

	private static final long serialVersionUID = -5163308718873620492L;

	private int width;

	private String rootlessHTML;

	/**
	 * Creates a pane with the given rootlessHTML as text with the given width.
	 *
	 * @param width
	 *            the desired width
	 * @param rootlessHTML
	 *            the text, can contain hyperlinks that will be clickable
	 */
	public FixedWidthEditorPane(int width, String rootlessHTML) {
		super("text/html", "");
		this.width = width;
		this.rootlessHTML = rootlessHTML;
		updateLabel();

		setEditable(false);
		setFocusable(false);
		installDefaultStylesheet();
		addHyperlinkListener(new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					RMUrlHandler.handleUrl(e.getDescription());
				}
			}
		});

	}

	@Override
	public void setText(String text) {
		this.rootlessHTML = text;
		updateLabel();
	}

	/**
	 * Sets the width of the text.
	 *
	 * @param width
	 *            the width of the text
	 */
	public void setWidth(int width) {
		this.width = width;
		updateLabel();
	}

	/**
	 * Updates the text with the given width.
	 */
	public void updateLabel() {
		try {
			super.setText("<html><body><div style=\"width:" + width + "pt\">" + rootlessHTML + "</div></body></html>");
		} catch (RuntimeException e) {
			// ignore
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		super.paintComponent(g);
	}

}
