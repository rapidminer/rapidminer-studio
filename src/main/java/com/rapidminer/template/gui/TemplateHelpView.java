/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.template.gui;

import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.template.Step;
import com.rapidminer.template.Template;
import com.rapidminer.template.TemplateState;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.RMUrlHandler;

import java.awt.Desktop;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;


/**
 * Used to display one of the help texts of a template (see {@link Template#getHelpText(Step)}. The
 * view registers as {@link Observer} with the {@link Template} and updates when the template
 * changes. It displays the help text for a particular {@link Step} defined during construction.
 * 
 * @author Simon Fischer
 * 
 */
public class TemplateHelpView extends ExtendedHTMLJEditorPane {

	private static final long serialVersionUID = 1L;

	public TemplateHelpView(final TemplateState state, final String defaultText, final Step forStep) {
		super("text/html", defaultText);

		setEditable(false);
		installDefaultStylesheet();
		decorateStyleSheet(this);

		state.addObserver(new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				if (TemplateState.OBSERVER_EVENT_TEMPLATE.equals(arg)) {
					Template template = state.getTemplate();
					if (template != null) {
						String text = template.getHelpText(forStep);
						if (template.getLearnMoreURL() != null) {
							// TODO: reactivate once the content is actually there
							// text +=
							// "<div style=\"margin-top:10px;\"><a href=\""+template.getLearnMoreURL()+"\"/>Tell me more...</div>";
						}
						setText(text);
					} else {
						setText(defaultText);
					}
					setCaretPosition(0);
				}
			}
		});

		addHyperlinkListener(new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (!RMUrlHandler.handleUrl(e.getDescription())) {
						try {
							Desktop.getDesktop().browse(e.getURL().toURI());
						} catch (Exception e1) {
							// LogService.getRoot().log(Level.WARNING,
							// "Cannot display news site "+e.getDescription()+" ("+e1.getMessage()+"). Network may be down.",
							// e1);
							LogService.getRoot().log(Level.WARNING, "Failed to open browser: " + e1, e1);
						}
					}
				}
			}
		});
	}

	public static void decorateStyleSheet(JEditorPane editor) {
		StyleSheet css = ((HTMLEditorKit) editor.getEditorKit()).getStyleSheet();
		css.addRule(".image-wrapper {margin-top:5px;margin-bottom:5px;text-align:center;}");
		css.addRule("quote { text-align:center;font-style:italic;}");
	}
}
