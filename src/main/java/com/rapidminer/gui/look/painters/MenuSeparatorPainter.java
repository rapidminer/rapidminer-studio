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
 * This is a cached painter for the menu separators.
 * 
 * @author Ingo Mierswa
 */
public class MenuSeparatorPainter extends AbstractCachedPainter {

	public static final MenuSeparatorPainter SINGLETON = new MenuSeparatorPainter(7);

	MenuSeparatorPainter(int count) {
		super(count);
	}

	public synchronized void paint(Component c, Graphics g, int x, int y, int w, int h) {
		paint(c, g, x, y, w, h, new Object[] {});
	}

	@Override
	protected void paintToImage(Component c, Graphics g, int w, int h, Object[] args) {
		g.setColor(RapidLookTools.getColors().getMenuItemBackground());
		g.fillRect(0, 0, w, h);

		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(new GradientPaint((w > 50 ? w - 50 : 0), 0, RapidLookTools.getColors().getMenuItemBackground(), w, 0,
				RapidLookTools.getColors().getMenuItemFadingColor()));
		g2.fillRect((w > 50 ? w - 50 : 0), 0, w, h);

		ColorUIResource c1 = new ColorUIResource(140, 140, 140);
		g.setColor(c1);
		g.drawLine(0, 1, w, 1);
		g2.setPaint(new GradientPaint(w - 20, 0, c1, w, 0, RapidLookTools.getColors().getMenuItemFadingColor()));
		g2.fillRect(w - 20, 1, 20, 1);

		g2.setPaint(new GradientPaint(0, 0, RapidLookTools.getColors().getMenuItemBackground(), 20, 0, c1));
		g2.fillRect(0, 1, 20, 1);
	}

	@Override
	protected void paintImage(Component c, Graphics g, int x, int y, int imageW, int imageH, Image image, Object[] args) {
		g.drawImage(image, 0, 0, null);
	}

	@Override
	protected Image createImage(Component c, int w, int h, GraphicsConfiguration config) {
		if (config == null) {
			return new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		}
		return config.createCompatibleVolatileImage(w, h);
	}
}
