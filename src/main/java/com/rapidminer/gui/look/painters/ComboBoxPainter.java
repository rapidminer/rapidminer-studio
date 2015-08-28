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

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.plaf.ColorUIResource;


/**
 * This is a cached painter for combo boxes.
 * 
 * @author Ingo Mierswa
 */
public class ComboBoxPainter extends AbstractCachedPainter {

	public static final ComboBoxPainter SINGLETON = new ComboBoxPainter(15);

	ComboBoxPainter(int count) {
		super(count);
	}

	public synchronized void paint(Component c, Graphics g, int x, int y, int w, int h) {
		paint(c, g, x, y, w, h, new Object[] {});
	}

	@Override
	protected void paintToImage(Component c, Graphics g, int w, int h, Object[] args) {
		h -= 1;
		boolean down = ((Boolean) args[0]).booleanValue();
		boolean enable = ((Boolean) args[1]).booleanValue();

		if (enable) {
			if (down) {
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[0]);
				g.drawLine(2, 1, w - 3, 1);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[1]);
				g.drawLine(1, 2, w - 2, 2);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[2]);
				g.drawLine(1, 3, w - 2, 3);
				g.drawLine(1, 4, w - 2, 4);
				g.drawLine(1, 5, w - 2, 5);

				Graphics2D g2 = (Graphics2D) g;
				g2.setPaint(new GradientPaint(0, 0, RapidLookTools.getColors().getButtonSkinColors()[3], 0, h,
						RapidLookTools.getColors().getButtonSkinColors()[4]));
				g.fillRect(1, 6, w - 2, h - 11);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[5]);
				g.drawLine(1, h - 5, w - 2, h - 5);
				g.drawLine(1, h - 4, w - 2, h - 4);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[6]);
				g.drawLine(1, h - 3, w - 2, h - 3);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[7]);
				g.drawLine(1, h - 2, w - 2, h - 2);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[8]);
				g.drawLine(2, h - 1, w - 3, h - 1);
			} else {
				g.setColor(Color.white);
				g.drawLine(2, 1, w - 3, 1);
				g.drawLine(1, 2, w - 2, 2);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[9]);
				g.drawLine(1, 3, w - 2, 3);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[4]);
				g.drawLine(1, 4, w - 2, 4);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[1]);
				g.drawLine(1, 5, w - 2, 5);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[10]);
				g.drawLine(1, 6, w - 2, 6);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[2]);
				g.drawLine(1, 7, w - 2, 7);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[18]);
				g.drawLine(1, 8, w - 2, 8);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[3]);
				g.fillRect(1, 9, w - 2, h - 15);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[11]);
				g.drawLine(1, h - 6, w - 2, h - 6);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[12]);
				g.drawLine(1, h - 5, w - 2, h - 5);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[13]);
				g.drawLine(1, h - 4, w - 2, h - 4);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[14]);
				g.drawLine(1, h - 3, w - 2, h - 3);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[15]);
				g.drawLine(1, h - 2, w - 2, h - 2);
				g.setColor(RapidLookTools.getColors().getButtonSkinColors()[16]);
				g.drawLine(2, h - 1, w - 3, h - 1);
			}
		} else { // disabled button
			g.setColor(RapidLookTools.getColors().getButtonSkinColors()[17]);
			g.drawLine(2, 1, w - 3, 1);
			g.drawLine(1, 2, w - 2, 2);
			g.drawLine(1, 3, w - 2, 3);
			g.drawLine(1, 4, w - 2, 4);
			g.drawLine(1, 5, w - 2, 5);
			g.drawLine(1, 6, w - 2, 6);
			g.drawLine(1, 7, w - 2, 7);
			g.fillRect(1, 8, w - 2, h - 14);
			g.drawLine(1, h - 6, w - 2, h - 6);
			g.drawLine(1, h - 5, w - 2, h - 5);
			g.drawLine(1, h - 4, w - 2, h - 4);
			g.drawLine(1, h - 3, w - 2, h - 3);
			g.drawLine(1, h - 2, w - 2, h - 2);
			g.drawLine(2, h - 1, w - 3, h - 1);
		}

		// arrow
		int ny = (c.getSize().height / 2) - 3;
		int nx = c.getWidth() - 15;
		if (down && enable) {
			nx++;
			ny++;
		}
		g.translate(nx, ny);
		if (enable) {
			g.setColor(new ColorUIResource(195, 195, 195));
			g.drawLine(0, 0, 8, 0);
			g.drawLine(1, 2, 4, 5);
			g.drawLine(7, 2, 4, 5);
			g.setColor(new ColorUIResource(100, 100, 100));
			g.drawLine(1, 1, 7, 1);
			g.drawLine(2, 2, 6, 2);
			g.drawLine(3, 3, 5, 3);
			g.drawLine(4, 4, 4, 4);
		} else {
			g.setColor(new ColorUIResource(220, 220, 220));
			g.drawLine(0, 0, 8, 0);
			g.drawLine(1, 2, 4, 5);
			g.drawLine(7, 2, 4, 5);
			g.setColor(new ColorUIResource(190, 190, 190));
			g.drawLine(1, 1, 7, 1);
			g.drawLine(2, 2, 6, 2);
			g.drawLine(3, 3, 5, 3);
			g.drawLine(4, 4, 4, 4);
		}
		g.translate(-nx, -ny);
	}

	@Override
	protected void paintImage(Component c, Graphics g, int x, int y, int imageW, int imageH, Image image, Object[] args) {
		g.translate(x, y);
		g.drawImage(image, 0, 0, null);
		g.translate(-x, -y);
	}
}
