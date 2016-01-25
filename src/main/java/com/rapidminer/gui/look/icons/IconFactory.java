/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.gui.look.icons;

import java.awt.Dimension;

import javax.swing.Icon;


/**
 * The factory used for creating and holding icon objects. All icons are singletons delivered by the
 * methods of this class.
 *
 * @author Ingo Mierswa
 */
public class IconFactory {

	public static final Dimension MENU_ICON_SIZE = new Dimension(10, 10);

	private final static Icon RADIO_BUTTON_ICON = new RadioButtonIcon();

	private final static Icon CHECK_BOX_ICON = new CheckBoxIcon();

	private final static Icon CHECK_BOX_MENU_ITEM_ICON = new CheckBoxMenuItemIcon();

	private final static Icon RADIO_BUTTON_MENU_ITEM_ICON = new RadioButtonMenuItemIcon();

	public static Icon getRadioButtonIcon() {
		return RADIO_BUTTON_ICON;
	}

	public static Icon getCheckBoxIcon() {
		return CHECK_BOX_ICON;
	}

	public static Icon getCheckBoxMenuItemIcon() {
		return CHECK_BOX_MENU_ITEM_ICON;
	}

	public static Icon getRadioButtonMenuItemIcon() {
		return RADIO_BUTTON_MENU_ITEM_ICON;
	}

}
