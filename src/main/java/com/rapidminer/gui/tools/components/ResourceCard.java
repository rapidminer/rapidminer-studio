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

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 * Data container used in button bard card panels. The title, tooltip, and icon are loaded from the GUI.properties. I18N
 * keys should look like this: 'gui.cards.I18N_KEY.title', 'gui.cards.I18N_KEY.tip', 'gui.cards.I18N_KEY.icon'.
 * <p>
 * Since 9.2.0: If the card should display a BETA tag, add a 'gui.cards.I18N_KEY.beta = true' entry to the i18n properties.
 * </p>
 *
 * @author Nils Woehler
 */
public class ResourceCard implements Card {

	private String title;
	private final String key;
	private final boolean isBeta;
	private String tip;
	private ImageIcon icon;

	public ResourceCard(String key, String i18nKey) {

		this.key = key;
		String titleName = I18N.getGUIMessageOrNull("gui.cards." + i18nKey + ".title");
		if (titleName != null) {
			this.title = "<html><div style=\"text-align: center;\"><body>" + titleName.replaceFirst(" ", "<br>")
					+ "</html></body>";
			this.tip = I18N.getGUIMessage("gui.cards." + i18nKey + ".tip");
		} else {
			// default case if no name and icon are defined:
			key = key.replace("_", " ");
			char[] stringArray = key.toCharArray();
			stringArray[0] = Character.toUpperCase(stringArray[0]);
			String defaultName = new String(stringArray);

			this.title = "<html><div style=\"text-align: center;\"><body>" + defaultName.replaceFirst(" ", "<br>")
					+ "</html></body>";
			this.tip = defaultName;
		}

		String iconName = I18N.getGUIMessageOrNull("gui.cards." + i18nKey + ".icon");
		if (iconName != null) {
			this.icon = SwingTools.createIcon("32/" + iconName);
		} else {
			this.icon = SwingTools.createIcon("32/data_information.png"); // default icon

		}

		this.isBeta = Boolean.parseBoolean(I18N.getGUIMessage("gui.cards." + i18nKey + ".beta"));
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getTip() {
		return tip;
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	@Override
	public String getFooter() {
		return null; // no caption
	}

	@Override
	public boolean isBeta() {
		return isBeta;
	}
}
