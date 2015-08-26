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
package com.rapidminer.gui.look.painters;

import com.rapidminer.gui.look.RapidLookTools;

import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.plaf.ColorUIResource;


/**
 * This is a cached painter for the faded menu items.
 * 
 * @author Ingo Mierswa
 */
public class MenuItemFadingPainter extends AbstractCachedPainter {

	public static final MenuItemFadingPainter SINGLETON = new MenuItemFadingPainter(7);

	private int width;

	MenuItemFadingPainter(int count) {
		super(count);
	}

	public synchronized void paint(Component c, Graphics g, int x, int y, int w, int h) {
		this.width = w;
		paint(c, g, x, y, 50, h, new Object[] {});
	}

	@Override
	protected void paintToImage(Component c, Graphics g, int w, int h, Object[] args) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(new GradientPaint(0, 0, new ColorUIResource(RapidLookTools.getColors().getMenuItemBackground()), 50, 0,
				RapidLookTools.getColors().getMenuItemFadingColor()));
		g2.fillRect(0, 0, 50, h);
	}

	@Override
	protected void paintImage(Component c, Graphics g, int x, int y, int imageW, int imageH, Image image, Object[] args) {
		g.drawImage(image, (this.width >= 50 ? this.width - 50 : 0), 0, null);
	}

	@Override
	protected Image createImage(Component c, int w, int h, GraphicsConfiguration config) {
		if (config == null) {
			return new BufferedImage(50, h, BufferedImage.TYPE_3BYTE_BGR);
		}
		return config.createCompatibleVolatileImage(50, h);
	}
}
