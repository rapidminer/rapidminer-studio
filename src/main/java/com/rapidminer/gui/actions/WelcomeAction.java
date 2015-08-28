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
package com.rapidminer.gui.actions;

import java.awt.Color;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.WelcomeScreen;
import com.rapidminer.tools.I18N;


/**
 * Base class for {@link WelcomeScreen} actions.
 *
 *
 * @author Nils Woehler
 *
 */
public abstract class WelcomeAction extends AbstractAction {

	private static final long serialVersionUID = -6132881402090898497L;

	private static String generateHTML(String i18nKey, Color textCol, boolean textAlignRight) {
		String textAlign = "left";
		if (textAlignRight) {
			textAlign = "right";
		}

		return "<html><body>" + "<div style=\"width: 195px; margin-" + textAlign + ": 5px; text-align: " + textAlign
				+ "; font-family: 'Open Sans Light'; font-size: 28px;\">"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.welcome." + i18nKey + ".label") + "<br\\>"
				+ "<span style=\"font-size: 12px;font-family: 'Open Sans';\">"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.welcome." + i18nKey + ".synopsis") + "</span>" + "</div>"
				+ "</body></html>";
	}

	private static Icon getIcon(String i18nKey) {
		return SwingTools.createIcon("welcome/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.welcome." + i18nKey + ".icon"));
	}

	public WelcomeAction(String i18nKey, Color textCol, boolean textAlignRight) {
		super(generateHTML(i18nKey, textCol, textAlignRight), getIcon(i18nKey));
		putValue(SHORT_DESCRIPTION, I18N.getMessage(I18N.getGUIBundle(), "gui.action.welcome." + i18nKey + ".tip"));
	}

	/**
	 * Creates a left-aligned text.
	 *
	 * @param i18nKey
	 * @param textCol
	 */
	public WelcomeAction(String i18nKey, Color textCol) {
		this(i18nKey, textCol, false);
	}

}
