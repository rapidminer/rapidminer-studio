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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.net.URL;
import javax.swing.Action;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.Tools;


/**
 * Abstract super class for both the {@link LinkLocalButton} and the {@link LinkRemoteButton}. It
 * holds the template for the plain link HTML and the icon link HTML. Furthermore it holds a
 * {@link HyperlinkListener} which invokes the provided {@link Action} in case the link has been
 * clicked.
 *
 * @author Nils Woehler
 * @since 7.0.0
 *
 */
public class AbstractLinkButton extends ExtendedHTMLJEditorPane {

	private static final long serialVersionUID = 1L;

	private final static String TEMPLATE_HTML = "<a href=\"#\">%s</a>";

	private final static String TEMPLATE_HTML_BOLD = "<a style=\"font-weight:bold;\" href=\"#\">%s</a>";

	private final static String TEMPLATE_ICON_HTML = "<table cellpadding=\"1px\"><tr><td><img width=\"16\" height=\"16\" src=\"%s\"></td><td><a href=\"#\">%s</a></td></tr></table>";

	/**
	 * if the {@link Action} contains this property and it is set to {@code true}, the link will be
	 * bold
	 */
	public static final String PROPERTY_BOLD = "isLinkBold";

	private Action action;

	private HyperlinkListener actionLinkListener;

	public AbstractLinkButton(final Action action, Color linkTextColor) {
		super("text/html", makeHTML(action));
		this.action = action;

		installDefaultStylesheet();
		setEditable(false);
		setOpaque(false);
		setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));

		HTMLEditorKit htmlKit = (HTMLEditorKit) getEditorKit();
		String hexColor = String.format(SwingTools.getColorHexValue(linkTextColor));
		htmlKit.getStyleSheet().addRule("a {color:" + hexColor + ";}");

		makeHyperLinkListener(action);
	}

	/**
	 * Replaces the current action of the link button by another action. The current
	 * {@link HyperlinkListener} will be unregistered and a new one will be created. Furthermore the
	 * button text will be exchanged.
	 *
	 * @param action
	 *            the new action
	 */
	public void setAction(Action action) {
		if (action == null) {
			throw new IllegalArgumentException("Action must not be null");
		}
		if (actionLinkListener != null) {
			removeHyperlinkListener(actionLinkListener);
		}
		this.action = action;
		makeHyperLinkListener(action);
		setText(makeHTML(action));
		setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
	}

	/**
	 * Returns the main action for this link button
	 *
	 * @return the main action for this link button
	 */
	public Action getAction() {
		return action;
	}

	/**
	 * Creates the hyperlink listener for the given action.
	 *
	 * @param action
	 */
	private void makeHyperLinkListener(final Action action) {

		actionLinkListener = new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == EventType.ACTIVATED) {
					action.actionPerformed(
							new ActionEvent(AbstractLinkButton.this, ActionEvent.ACTION_PERFORMED, e.getDescription()));
				}
			}
		};
		addHyperlinkListener(actionLinkListener);
	}

	/**
	 * Creates the HTML text which will represent the link button.
	 *
	 * @param action
	 * @return
	 */
	private static String makeHTML(final Action action) {
		if (action == null) {
			throw new IllegalArgumentException("action must not be null!");
		}

		String name = (String) action.getValue(Action.NAME);
		if (name == null || name.trim().isEmpty()) {
			return "";
		}
		// if only part of the text should appear as link, <a href> tag is defined in i18n
		if (name.contains("<") || name.contains(">")) {
			return name;
		}

		URL iconUrl = null;
		if (action instanceof ResourceAction) {
			String iconName = ((ResourceAction) action).getIconName();
			if (iconName != null) {
				String regularIconPath = "icons/16/" + iconName;
				String retinaIconPath = "icons/16/@2x/" + iconName;
				boolean isRetina = SwingTools.getGUIScaling() == SwingTools.Scaling.RETINA;
				String iconLookup = isRetina ? retinaIconPath : regularIconPath;
				try {
					iconUrl = Tools.getResource(iconLookup);
					if (iconUrl == null && isRetina) {
						// fallback if @2x icon is missing on retina displays
						iconUrl = Tools.getResource(regularIconPath);
					}
				} catch (NullPointerException e) {
					// this can occur if no @2x icon exists on OS X and the security manager throws an NPE
					iconUrl = Tools.getResource(regularIconPath);
				}
			}
		}

		if (iconUrl != null) {
			return String.format(TEMPLATE_ICON_HTML, iconUrl.toString(), name);
		} else {
			if (Boolean.parseBoolean(String.valueOf(action.getValue(PROPERTY_BOLD)))) {
				return String.format(TEMPLATE_HTML_BOLD, name);
			} else {
				return String.format(TEMPLATE_HTML, name);
			}
		}
	}

}
