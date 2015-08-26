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
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.plaf.ColorUIResource;


/**
 * This is a cached painter for radio buttons.
 * 
 * @author Ingo Mierswa
 */
public class RadioButtonPainter extends AbstractCachedPainter {

	public static final RadioButtonPainter SINGLETON = new RadioButtonPainter(7);

	RadioButtonPainter(int count) {
		super(count);
	}

	public synchronized void paint(Component c, Graphics g, int x, int y, int w, int h) {
		paint(c, g, x, y, w, h, new Object[] {});
	}

	@Override
	protected void paintToImage(Component c, Graphics g, int w, int h, Object[] args) {
		boolean isSelected = ((Boolean) args[0]).booleanValue();
		boolean isEnabled = ((Boolean) args[1]).booleanValue();
		boolean isPressed = ((Boolean) args[2]).booleanValue();
		boolean isRollover = ((Boolean) args[4]).booleanValue();

		ColorUIResource c1 = new ColorUIResource(0);
		ColorUIResource c2 = new ColorUIResource(0);
		ColorUIResource c3 = new ColorUIResource(0);
		ColorUIResource c4 = new ColorUIResource(0);
		ColorUIResource c5 = new ColorUIResource(0);

		if (isEnabled) {
			if (isRollover) {
				c1 = RapidLookTools.getColors().getRadioButtonColors()[1][0];
				c2 = RapidLookTools.getColors().getRadioButtonColors()[1][1];
				c3 = RapidLookTools.getColors().getRadioButtonColors()[1][2];
				c4 = RapidLookTools.getColors().getRadioButtonColors()[1][3];
				c5 = RapidLookTools.getColors().getRadioButtonColors()[1][4];
			} else {
				c1 = RapidLookTools.getColors().getRadioButtonColors()[0][0];
				c2 = RapidLookTools.getColors().getRadioButtonColors()[0][1];
				c3 = RapidLookTools.getColors().getRadioButtonColors()[0][2];
				c4 = RapidLookTools.getColors().getRadioButtonColors()[0][3];
				c5 = RapidLookTools.getColors().getRadioButtonColors()[0][4];
			}
		} else {
			c1 = RapidLookTools.getColors().getRadioButtonColors()[0][0];
			c2 = RapidLookTools.getColors().getRadioButtonColors()[0][1];
			c3 = RapidLookTools.getColors().getRadioButtonColors()[0][2];
			c4 = RapidLookTools.getColors().getRadioButtonColors()[0][3];
			c5 = RapidLookTools.getColors().getRadioButtonColors()[0][4];
		}

		g.setColor(c1);
		g.drawLine(4, 0, 11, 0);
		g.drawLine(4, 15, 11, 15);
		g.drawLine(0, 4, 0, 11);
		g.drawLine(15, 4, 15, 11);

		g.setColor(c2);
		g.drawLine(5, 0, 6, 1);
		g.drawLine(10, 0, 9, 1);
		g.drawLine(5, 15, 6, 14);
		g.drawLine(10, 15, 9, 14);
		g.drawLine(0, 5, 1, 6);
		g.drawLine(0, 10, 1, 9);
		g.drawLine(15, 5, 14, 6);
		g.drawLine(15, 10, 14, 9);

		g.drawLine(1, 3, 3, 1);
		g.drawLine(1, 12, 3, 14);
		g.drawLine(12, 1, 14, 3);
		g.drawLine(12, 14, 14, 12);

		g.setColor(c3);
		g.drawLine(6, 0, 5, 1);
		g.drawLine(9, 0, 10, 1);
		g.drawLine(6, 15, 5, 14);
		g.drawLine(9, 15, 10, 14);
		g.drawLine(0, 6, 1, 5);
		g.drawLine(0, 9, 1, 10);
		g.drawLine(15, 6, 14, 5);
		g.drawLine(15, 9, 14, 10);

		g.setColor(c4);
		g.drawLine(7, 0, 8, 0);
		g.drawLine(7, 15, 8, 15);
		g.drawLine(0, 7, 0, 8);
		g.drawLine(15, 7, 15, 8);

		g.drawLine(1, 4, 4, 1);
		g.drawLine(1, 11, 4, 14);
		g.drawLine(11, 1, 14, 4);
		g.drawLine(11, 14, 14, 11);

		g.setColor(c5);
		g.drawLine(7, 1, 8, 1);
		g.drawLine(7, 14, 8, 14);
		g.drawLine(1, 7, 1, 8);
		g.drawLine(14, 7, 14, 8);

		g.drawLine(2, 4, 4, 2);
		g.drawLine(11, 2, 13, 4);
		g.drawLine(2, 11, 4, 13);
		g.drawLine(11, 13, 13, 11);

		// -- drawing inner section
		if (isEnabled) {
			if (isSelected || isPressed) {
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][0]);
				g.drawLine(5, 2, 10, 2);
				g.drawLine(4, 3, 11, 3);
				g.drawLine(3, 4, 12, 4);
				g.drawLine(2, 5, 13, 5);
				g.drawLine(2, 6, 13, 6);
				g.drawLine(2, 7, 13, 7);
				g.drawLine(2, 8, 13, 8);
				g.drawLine(2, 9, 13, 9);
				g.drawLine(2, 10, 13, 10);
				g.drawLine(3, 11, 12, 11);
				g.drawLine(4, 12, 11, 12);
				g.drawLine(5, 13, 10, 13);
			} else {
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][1]);
				g.drawLine(5, 2, 10, 2);
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][2]);
				g.drawLine(4, 3, 11, 3);
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][3]);
				g.drawLine(3, 4, 12, 4);
				// --
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][4]);
				g.drawLine(2, 5, 13, 5);
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][5]);
				g.drawLine(2, 6, 13, 6);
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][6]);
				g.drawLine(2, 7, 13, 7);
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][7]);
				g.drawLine(2, 8, 13, 8);
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][8]);
				g.drawLine(2, 9, 13, 9);
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][9]);
				g.drawLine(2, 10, 13, 10);
				// --
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][8]);
				g.drawLine(3, 11, 12, 11);
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][4]);
				g.drawLine(4, 12, 11, 12);
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][1]);
				g.drawLine(5, 13, 10, 13);
			}
		} else {
			g.setColor(RapidLookTools.getColors().getRadioButtonColors()[2][8]);
			g.drawLine(5, 2, 10, 2);
			g.drawLine(4, 3, 11, 3);
			g.drawLine(3, 4, 12, 4);
			g.drawLine(2, 5, 13, 5);
			g.drawLine(2, 6, 13, 6);
			g.drawLine(2, 7, 13, 7);
			g.drawLine(2, 8, 13, 8);
			g.drawLine(2, 9, 13, 9);
			g.drawLine(2, 10, 13, 10);
			g.drawLine(3, 11, 12, 11);
			g.drawLine(4, 12, 11, 12);
			g.drawLine(5, 13, 10, 13);
		}

		// drawing sphere
		g.translate(3, 3);
		if (isSelected) {
			if (isEnabled) {
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[3][0]);
				g.drawLine(1, 3, 3, 1);
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[3][1]);
				g.drawLine(1, 4, 4, 1);
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[3][2]);
				g.drawLine(1, 5, 5, 1);
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[3][3]);
				g.drawLine(1, 6, 6, 1);

				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[3][4]);
				g.drawLine(2, 6, 6, 2);
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[3][5]);
				g.drawLine(2, 7, 7, 2);

				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[3][6]);
				g.drawLine(3, 7, 7, 3);
				g.drawLine(3, 8, 8, 3);
				g.drawLine(4, 8, 8, 4);
				g.drawLine(5, 8, 8, 5);
				g.drawLine(6, 8, 8, 6);
			} else {
				g.setColor(RapidLookTools.getColors().getRadioButtonColors()[3][7]);
				g.drawLine(1, 3, 3, 1);
				g.drawLine(1, 4, 4, 1);
				g.drawLine(1, 5, 5, 1);
				g.drawLine(1, 6, 6, 1);
				g.drawLine(2, 6, 6, 2);
				g.drawLine(2, 7, 7, 2);
				g.drawLine(3, 7, 7, 3);
				g.drawLine(3, 8, 8, 3);
				g.drawLine(4, 8, 8, 4);
				g.drawLine(5, 8, 8, 5);
				g.drawLine(6, 8, 8, 6);
			}
		}
		g.translate(-3, -3);
	}

	@Override
	protected void paintImage(Component c, Graphics g, int x, int y, int imageW, int imageH, Image image, Object[] args) {
		g.drawImage(image, 0, 0, null);
	}
}
