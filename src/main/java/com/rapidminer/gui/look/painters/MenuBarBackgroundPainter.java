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
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.image.BufferedImage;

import com.rapidminer.gui.look.Colors;


/**
 * This is a cached painter for the menu bar backgrounds.
 *
 * @author Ingo Mierswa
 */
public class MenuBarBackgroundPainter extends AbstractCachedPainter {

	public static final MenuBarBackgroundPainter SINGLETON = new MenuBarBackgroundPainter(15);

	MenuBarBackgroundPainter(int count) {
		super(count);
	}

	public synchronized void paint(Component c, Graphics g, int x, int y, int w, int h) {
		paint(c, g, x, y, w, h, new Object[] {});
	}

	@Override
	protected void paintToImage(Component c, Graphics g, int w, int h, Object[] args) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(Colors.WHITE);
		g2.fillRect(0, 0, w, h - 3);
	}

	@Override
	protected void paintImage(Component c, Graphics g, int x, int y, int imageW, int imageH, Image image, Object[] args) {
		g.translate(x, y);
		g.drawImage(image, 0, 0, null);
		g.translate(-x, -y);
	}

	@Override
	protected Image createImage(Component c, int w, int h, GraphicsConfiguration config) {
		if (config == null) {
			return new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
		}
		return config.createCompatibleVolatileImage(w, h);
	}
}
