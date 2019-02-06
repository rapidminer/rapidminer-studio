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
package com.rapidminer.gui.look.painters;

import java.awt.Component;
import java.awt.Graphics;


/**
 * This class provides static methods for cached painting of GUI elements.
 *
 * @author Ingo Mierswa
 */
public class CachedPainter {

	public static void clearMenuCache() {
		AbstractCachedPainter.clearCache();
	}

	public static void clearCashedImages() {
		AbstractCachedPainter.clearCache();
	}

	public static boolean drawMenuBackground(Component c, Graphics g, int x, int y, int w, int h) {
		if (h < 0 || w < 0) {
			return true;
		}
		MenuBackgroundPainter.SINGLETON.paint(c, g, x, y, w, h);
		return true;
	}

	public static boolean drawMenuBarBackground(Component c, Graphics g, int x, int y, int w, int h) {
		if (h < 0 || w < 0) {
			return true;
		}
		MenuBarBackgroundPainter.SINGLETON.paint(c, g, x, y, w, h);
		return true;
	}

	public static boolean drawMenuSeparator(Component c, Graphics g) {
		int w = c.getWidth();
		int h = c.getHeight();
		if (h < 0 || w < 0) {
			return true;
		}
		MenuSeparatorPainter.SINGLETON.paint(c, g, c.getX(), c.getY(), w, h);
		return true;
	}

}
