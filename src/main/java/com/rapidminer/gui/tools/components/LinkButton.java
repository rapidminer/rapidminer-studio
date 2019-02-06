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

import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.Action;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.tools.Tools;


/**
 * Can be used as a label that triggers an action event on every link activation click. The
 * {@link Action#NAME} property of the action will be the label text. Note that it must contain a
 * &lt;a&gt;> tag for this class to do something useful. The icon property of the action is not
 * interpreted.
 *
 * @author Simon Fischer
 * @deprecated replaced by either {@link LinkLocalButton} for actions that only trigger
 *             in-application actions or by {@link LinkRemoteButton} for actions that open a website
 *             in the browser.
 *
 */
@Deprecated
public class LinkButton extends ExtendedHTMLJEditorPane {

	private static final long serialVersionUID = 1L;

	private HyperlinkListener actionLinkListener;

	private final boolean addLinkTag;
	private final boolean normalFont;

	public LinkButton(final Action action) {
		this(action, false);
	}

	public LinkButton(final Action action, boolean addLinkTag) {
		this(action, addLinkTag, false);
	}

	public LinkButton(final Action action, boolean addLinkTag, boolean normalFont) {
		super("text/html", makeHTML(action, addLinkTag, normalFont));
		this.addLinkTag = addLinkTag;
		this.normalFont = normalFont;
		installDefaultStylesheet();
		setEditable(false);
		setOpaque(false);
		if (action != null) {
			setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
			makeHyperLinkListener(action);
		}
	}

	private void makeHyperLinkListener(final Action action) {
		actionLinkListener = new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == EventType.ACTIVATED) {
					action.actionPerformed(
							new ActionEvent(LinkButton.this, ActionEvent.ACTION_PERFORMED, e.getDescription()));
				}
			}
		};
		addHyperlinkListener(actionLinkListener);
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
		makeHyperLinkListener(action);
		setText(makeHTML(action, addLinkTag, normalFont));
		setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
	}

	private static String makeHTML(final Action action, boolean addLinkTag, boolean normalFont) {
		if (action == null) {
			return "";
		}

		String html = (String) action.getValue(Action.NAME);
		if (addLinkTag) {
			if (action instanceof ResourceAction) {
				String iconName = ((ResourceAction) action).getIconName();
				if (iconName != null) {
					URL iconUrl = Tools.getResource("icons/16/" + iconName);
					if (iconUrl != null) {
						html = "<img src=\"" + iconUrl.toString()
								+ "\" border=\"0\" style=\"border:none;vertical-align:middle;\"/>&nbsp;" + html;
					}
				}
			}
			html = "<a href=\"#\"" + (normalFont ? " style=\"text-decoration:none;color:#222244;\"" : "") + ">" + html
					+ "</a>";
		}
		return html;
	}

}
