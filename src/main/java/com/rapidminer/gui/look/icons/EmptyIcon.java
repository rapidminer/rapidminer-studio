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
package com.rapidminer.gui.look.icons;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;


/**
 * Create a variable sized empty icon. Use prepared instances from {@link IconFactory}.
 *
 * @author Andreas Timm
 * @since 8.2
 */
public class EmptyIcon implements Icon {

	/**
	 * size of the icon
	 */
	private int width;
	private int height;

	/**
	 * Create an empty icon with this size
	 *
	 * @param width
	 * 		of the icon
	 * @param height
	 * 		of the icon
	 */
	public EmptyIcon(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		// intentionally left empty
	}

	/**
	 * Get the icons height
	 *
	 * @return height of the icon in pixels
	 */
	public int getIconHeight() {
		return height;
	}

	/**
	 * Get the icons width
	 *
	 * @return width of the icon in pixels
	 */
	public int getIconWidth() {
		return width;
	}
}
