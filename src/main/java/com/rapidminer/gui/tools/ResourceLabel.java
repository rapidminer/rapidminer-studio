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
package com.rapidminer.gui.tools;

import com.rapidminer.tools.I18N;

import java.text.MessageFormat;

import javax.swing.ImageIcon;
import javax.swing.JLabel;


/**
 * 
 * @author Simon Fischer
 */
public class ResourceLabel extends JLabel {

	private static final long serialVersionUID = 1L;

	/**
	 * This will construct a JLabel with the label, tooltip, icon and mnemonic set by a resource
	 * identifier. The following properties from the gui resource bundles are used:
	 * <ul>
	 * <li>gui.label.-key-.label as label</li>
	 * <li>gui.label.-key-.tip as tooltip</li>
	 * <li>gui.label.-key-.mne as mnemonic</li>
	 * <li>gui.label.-key-.icon as icon. Here the size must be assigned by leading 24/ or something
	 * like this</li>
	 * </ul>
	 */
	public ResourceLabel(String i18nKey, Object... i18nArgs) {
		super((i18nArgs == null) || (i18nArgs.length == 0) ? getMessage(i18nKey + ".label") : MessageFormat.format(
				getMessage(i18nKey + ".label"), i18nArgs));
		setToolTipText(getMessageOrNull(i18nKey + ".tip"));
		String mne = getMessageOrNull(i18nKey + ".mne");
		String icon = getMessageOrNull(i18nKey + ".icon");
		if (icon != null) {
			if (!icon.contains("/")) {
				icon = "16/" + icon;
			}
			ImageIcon iicon = SwingTools.createIcon(icon);
			setIcon(iicon);
		}
		if (mne != null && !mne.isEmpty()) {
			setDisplayedMnemonic(mne.charAt(0));
		}
	}

	private static String getMessage(String key) {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.label." + key);
	}

	private static String getMessageOrNull(String key) {
		return I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.label." + key);
	}
}
