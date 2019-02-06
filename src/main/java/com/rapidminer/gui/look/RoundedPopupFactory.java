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
package com.rapidminer.gui.look;

import java.awt.Component;
import java.awt.image.BufferedImage;

import javax.swing.Popup;
import javax.swing.PopupFactory;


/**
 * The factory for popups in form of rounded rectangles.
 * 
 * @author Ingo Mierswa
 */
public final class RoundedPopupFactory extends PopupFactory {

	public static final BufferedImage TOP_LEFT_PIC = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);

	public static final BufferedImage TOP_RIGHT_PIC = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);

	public static final BufferedImage BOTTOM_LEFT_PIC = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);

	public static final BufferedImage BOTTOM_RIGHT_PIC = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);

	public static final BufferedImage RIGHT_PIC = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);

	public static final BufferedImage BOTTOM_PIC = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);

	private final PopupFactory storedFactory;

	private RoundedPopupFactory(PopupFactory storedFactory) {
		this.storedFactory = storedFactory;
	}

	public static void install() {
		PopupFactory factory = PopupFactory.getSharedInstance();
		if (factory instanceof RoundedPopupFactory) {
			return;
		}
		PopupFactory.setSharedInstance(new RoundedPopupFactory(factory));
	}

	public static void uninstall() {
		PopupFactory factory = PopupFactory.getSharedInstance();
		if (!(factory instanceof RoundedPopupFactory)) {
			return;
		}
		PopupFactory stored = ((RoundedPopupFactory) factory).storedFactory;
		PopupFactory.setSharedInstance(stored);
	}

	@Override
	public Popup getPopup(Component owner, Component contents, int x, int y) throws IllegalArgumentException {
		Popup popup = super.getPopup(owner, contents, x, y);
		return RoundedRectanglePopup.getInstance(owner, contents, x, y, popup);
	}
}
