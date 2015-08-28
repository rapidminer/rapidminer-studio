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

import com.rapidminer.gui.look.Colors;

import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.plaf.ColorUIResource;


/**
 * This is a cached painter for the menu backgrounds.
 * 
 * @author Ingo Mierswa
 */
public class MenuBackgroundPainter extends AbstractCachedPainter {

	public static final MenuBackgroundPainter SINGLETON = new MenuBackgroundPainter(7);

	MenuBackgroundPainter(int count) {
		super(count);
	}

	public synchronized void paint(Component c, Graphics g, int x, int y, int w, int h) {
		paint(c, g, x, y, w, h, new Object[] {});
	}

	@Override
	protected void paintToImage(Component c, Graphics g, int w, int h, Object[] args) {
		g.setColor(Colors.getWhite());
		g.drawLine(0, 0, w - 1, 0);

		g.setColor(new ColorUIResource(140, 140, 140));
		g.drawLine(3, 1, w - 4, 1);
		g.drawLine(0, 4, 0, h - 1);
		g.drawLine(w - 1, 4, w - 1, h - 1);

		g.setColor(new ColorUIResource(170, 170, 170));
		g.drawLine(0, 3, 2, 1);
		g.drawLine(w - 1, 3, w - 3, 1);

		g.setColor(new ColorUIResource(220, 220, 220));
		g.drawLine(0, 1, 1, 2);
		g.drawLine(w - 1, 1, w - 2, 2);

		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(new GradientPaint(1, 2, new ColorUIResource(233, 233, 233), 1, h, Colors.getWhite()));
		g2.fillRect(1, 2, w - 2, h);

		g.setColor(new ColorUIResource(190, 190, 190));
		g.drawLine(0, 2, 1, 1);
		g.drawLine(1, 2, 1, 2);
		g.drawLine(w - 1, 2, w - 2, 1);
		g.drawLine(w - 2, 2, w - 2, 2);
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
