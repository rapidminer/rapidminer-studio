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
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.plaf.ColorUIResource;


/**
 * This is a cached painter for borders of buttons.
 * 
 * @author Ingo Mierswa
 */
public class ButtonBorderPainter extends AbstractCachedPainter {

	public static final ButtonBorderPainter SINGLETON = new ButtonBorderPainter(15);

	ButtonBorderPainter(int count) {
		super(count);
	}

	public synchronized void paint(Component c, Graphics g, int x, int y, int w, int h) {
		paint(c, g, x, y, w, h, new Object[] {});
	}

	@Override
	protected void paintToImage(Component c, Graphics g, int w, int h, Object[] args) {
		boolean roundBorder = Boolean.parseBoolean(args[1].toString());
		String type = args[2].toString();

		if (roundBorder) {
			Color c1 = new ColorUIResource(0);
			Color c2 = new ColorUIResource(0);
			Color c3 = new ColorUIResource(0);
			Color c4 = new ColorUIResource(0);
			Color c5 = new ColorUIResource(0);
			Color c6 = new ColorUIResource(0);
			Color c7 = new ColorUIResource(0);
			Color c11 = new ColorUIResource(0);
			Color c12 = new ColorUIResource(0);

			if (type.equals("NORMAL")) {
				c1 = RapidLookTools.getColors().getButtonBorderColors()[0][0];
				c2 = RapidLookTools.getColors().getButtonBorderColors()[0][1];
				c3 = RapidLookTools.getColors().getButtonBorderColors()[0][2];
				c4 = RapidLookTools.getColors().getButtonBorderColors()[0][3];
				c5 = RapidLookTools.getColors().getButtonBorderColors()[0][4];
				c6 = RapidLookTools.getColors().getButtonBorderColors()[0][5];
				c7 = RapidLookTools.getColors().getButtonBorderColors()[0][6];
				c11 = RapidLookTools.getColors().getButtonBorderColors()[0][9];
				c12 = RapidLookTools.getColors().getButtonBorderColors()[0][10];
			} else if (type.equals("ROLLOVER")) {
				c1 = RapidLookTools.getColors().getButtonBorderColors()[1][0];
				c2 = RapidLookTools.getColors().getButtonBorderColors()[1][1];
				c3 = RapidLookTools.getColors().getButtonBorderColors()[1][2];
				c4 = RapidLookTools.getColors().getButtonBorderColors()[1][3];
				c5 = RapidLookTools.getColors().getButtonBorderColors()[1][4];
				c6 = RapidLookTools.getColors().getButtonBorderColors()[1][5];
				c7 = RapidLookTools.getColors().getButtonBorderColors()[1][6];
				c11 = RapidLookTools.getColors().getButtonBorderColors()[1][9];
				c12 = RapidLookTools.getColors().getButtonBorderColors()[1][10];
			} else if (type.equals("FOCUS")) {
				c1 = RapidLookTools.getColors().getButtonBorderColors()[2][0];
				c2 = RapidLookTools.getColors().getButtonBorderColors()[2][1];
				c3 = RapidLookTools.getColors().getButtonBorderColors()[2][2];
				c4 = RapidLookTools.getColors().getButtonBorderColors()[2][3];
				c5 = RapidLookTools.getColors().getButtonBorderColors()[2][4];
				c6 = RapidLookTools.getColors().getButtonBorderColors()[2][5];
				c7 = RapidLookTools.getColors().getButtonBorderColors()[2][6];
				c11 = RapidLookTools.getColors().getButtonBorderColors()[2][9];
				c12 = RapidLookTools.getColors().getButtonBorderColors()[2][10];
			} else if (type.equals("DISABLE")) {
				c1 = RapidLookTools.getColors().getButtonBorderColors()[3][0];
				c2 = RapidLookTools.getColors().getButtonBorderColors()[3][1];
				c3 = RapidLookTools.getColors().getButtonBorderColors()[3][2];
				c4 = RapidLookTools.getColors().getButtonBorderColors()[3][3];
				c5 = RapidLookTools.getColors().getButtonBorderColors()[3][4];
				c6 = RapidLookTools.getColors().getButtonBorderColors()[3][5];
				c7 = RapidLookTools.getColors().getButtonBorderColors()[3][6];
				c11 = RapidLookTools.getColors().getButtonBorderColors()[3][9];
				c12 = RapidLookTools.getColors().getButtonBorderColors()[3][10];
			} else if (type.equals("DEFAULT")) {
				c1 = RapidLookTools.getColors().getButtonBorderColors()[4][0];
				c2 = RapidLookTools.getColors().getButtonBorderColors()[4][1];
				c3 = RapidLookTools.getColors().getButtonBorderColors()[4][2];
				c4 = RapidLookTools.getColors().getButtonBorderColors()[4][3];
				c5 = RapidLookTools.getColors().getButtonBorderColors()[4][4];
				c6 = RapidLookTools.getColors().getButtonBorderColors()[4][5];
				c7 = RapidLookTools.getColors().getButtonBorderColors()[4][6];
				c11 = RapidLookTools.getColors().getButtonBorderColors()[4][9];
				c12 = RapidLookTools.getColors().getButtonBorderColors()[4][10];
			}

			g.setColor(c11);
			g.drawLine(7, 0, w - 8, 0);
			g.drawLine(1, 4, 4, 1);
			g.drawLine(w - 5, 1, w - 2, 4);
			g.drawLine(0, 7, 0, h - 9);
			g.drawLine(w - 1, 7, w - 1, h - 9);
			g.drawLine(1, h - 6, 1, h - 5);
			g.drawLine(2, h - 4, 3, h - 3);
			g.drawLine(4, h - 2, 5, h - 2);
			g.drawLine(w - 2, h - 6, w - 2, h - 5);
			g.drawLine(w - 3, h - 4, w - 4, h - 3);
			g.drawLine(w - 5, h - 2, w - 6, h - 2);
			g.setColor(c12);
			g.drawLine(5, 0, 6, 0);
			g.drawLine(w - 6, 0, w - 7, 0);
			g.drawLine(0, 5, 0, 6);
			g.drawLine(0, h - 7, 0, h - 8);
			g.drawLine(w - 1, 5, w - 1, 6);
			g.drawLine(w - 1, h - 7, w - 1, h - 8);
			g.translate(1, 1);
			w -= 2;
			h -= 2;

			g.setColor(c1);
			g.drawLine(5, 0, w - 7, 0);
			g.drawLine(0, 6, 0, h - 7);
			g.drawLine(w - 1, 6, w - 1, h - 7);
			g.setColor(c2);
			g.drawLine(6, h - 1, w - 7, h - 1);

			g.setColor(c5);
			g.drawLine(0, 5, 1, 5);
			g.drawLine(5, 1, 5, 0);

			g.setColor(c7);
			g.drawLine(1, 4, 1, 3);
			g.drawLine(4, 1, 3, 1);
			g.drawLine(3, 1, 1, 3);

			g.setColor(c4);
			g.drawLine(4, 0, 5, 1);
			g.drawLine(0, 4, 1, 5);
			g.drawLine(2, 3, 3, 2);

			g.setColor(c5);
			g.drawLine(w - 1, 5, w - 2, 5);
			g.drawLine(w - 6, 0, w - 6, 1);

			g.setColor(c7);
			g.drawLine(w - 2, 4, w - 2, 3);
			g.drawLine(w - 5, 1, w - 4, 1);
			g.drawLine(w - 4, 1, w - 2, 3);

			g.setColor(c4);
			g.drawLine(w - 5, 0, w - 6, 1);
			g.drawLine(w - 1, 4, w - 2, 5);
			g.drawLine(w - 4, 2, w - 3, 3);

			g.setColor(c3);
			g.drawLine(2, h - 4, 3, h - 3);
			g.drawLine(5, h - 2, 5, h - 1);

			g.setColor(c5);
			g.drawLine(0, h - 6, 1, h - 6);

			g.setColor(c6);
			g.drawLine(1, h - 5, 1, h - 4);
			g.drawLine(4, h - 2, 3, h - 2);
			g.drawLine(3, h - 2, 1, h - 4);

			g.setColor(c3);
			g.drawLine(w - 3, h - 4, w - 4, h - 3);
			g.drawLine(w - 6, h - 2, w - 6, h - 1);

			g.setColor(c5);
			g.drawLine(w - 1, h - 6, w - 2, h - 6);

			g.setColor(c6);
			g.drawLine(w - 2, h - 5, w - 2, h - 4);
			g.drawLine(w - 5, h - 2, w - 4, h - 2);
			g.drawLine(w - 4, h - 2, w - 2, h - 4);

			g.setColor(c4);
			g.drawLine(7, h, w - 8, h);

		} else { // square border
			Color c1 = new ColorUIResource(0);
			Color c2 = new ColorUIResource(0);
			Color c11 = new ColorUIResource(0);
			Color c12 = new ColorUIResource(0);

			if (type.equals("NORMAL")) {
				c1 = RapidLookTools.getColors().getButtonBorderColors()[0][8];
				c2 = RapidLookTools.getColors().getButtonBorderColors()[0][7];
				c11 = RapidLookTools.getColors().getButtonBorderColors()[0][9];
				c12 = RapidLookTools.getColors().getButtonBorderColors()[0][10];
			} else if (type.equals("ROLLOVER")) {
				c1 = RapidLookTools.getColors().getButtonBorderColors()[1][8];
				c2 = RapidLookTools.getColors().getButtonBorderColors()[1][7];
				c11 = RapidLookTools.getColors().getButtonBorderColors()[1][9];
				c12 = RapidLookTools.getColors().getButtonBorderColors()[1][10];
			} else if (type.equals("FOCUS")) {
				c1 = RapidLookTools.getColors().getButtonBorderColors()[2][8];
				c2 = RapidLookTools.getColors().getButtonBorderColors()[2][7];
				c11 = RapidLookTools.getColors().getButtonBorderColors()[2][9];
				c12 = RapidLookTools.getColors().getButtonBorderColors()[2][10];
			} else if (type.equals("DISABLE")) {
				c1 = RapidLookTools.getColors().getButtonBorderColors()[3][8];
				c2 = RapidLookTools.getColors().getButtonBorderColors()[3][7];
				c11 = RapidLookTools.getColors().getButtonBorderColors()[3][9];
				c12 = RapidLookTools.getColors().getButtonBorderColors()[3][10];
			} else if (type.equals("DEFAULT")) {
				c1 = RapidLookTools.getColors().getButtonBorderColors()[4][8];
				c2 = RapidLookTools.getColors().getButtonBorderColors()[4][7];
				c11 = RapidLookTools.getColors().getButtonBorderColors()[4][9];
				c12 = RapidLookTools.getColors().getButtonBorderColors()[4][10];
			}

			g.setColor(c11);
			g.drawLine(0, 3, 0, h - 4);
			g.drawLine(w - 1, 3, w - 1, h - 4);
			g.drawLine(3, 0, w - 4, 0);
			g.drawLine(3, h - 1, w - 4, h - 1);
			g.setColor(c12);
			g.drawLine(0, 2, 2, 0);
			g.drawLine(w - 1, 2, w - 3, 0);
			g.drawLine(0, h - 3, 2, h - 1);
			g.drawLine(w - 1, h - 3, w - 3, h - 1);
			g.translate(1, 1);
			w -= 2;
			h -= 2;

			g.setColor(c1);
			g.drawLine(2, 0, w - 3, 0);
			g.drawLine(0, 2, 0, h - 3);
			g.drawLine(w - 1, 2, w - 1, h - 3);
			g.drawLine(2, h - 1, w - 3, h - 1);

			g.drawLine(1, 1, 1, 1);
			g.drawLine(w - 2, 1, w - 2, 1);
			g.drawLine(1, h - 2, 1, h - 2);
			g.drawLine(w - 2, h - 2, w - 2, h - 2);

			g.setColor(c2);
			g.drawLine(1, 0, 0, 1);
			g.drawLine(w - 2, 0, w - 1, 1);
			g.drawLine(w - 2, h - 1, w - 1, h - 2);
			g.drawLine(1, h - 1, 0, h - 2);
		}
	}

	@Override
	protected void paintImage(Component c, Graphics g, int x, int y, int imageW, int imageH, Image image, Object[] args) {
		g.translate(x, y);
		g.drawImage(image, 0, 0, null);
		g.translate(-x, -y);
	}
}
