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
 * This is a cached painter for the checkboxes.
 * 
 * @author Ingo Mierswa
 */
public class CheckboxPainter extends AbstractCachedPainter {

	public static final CheckboxPainter SINGLETON = new CheckboxPainter(7);

	CheckboxPainter(int count) {
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

		// drawing outer
		Color c1 = new ColorUIResource(0);
		Color c2 = new ColorUIResource(0);
		Color c3 = new ColorUIResource(0);

		if (!isEnabled) {
			c1 = RapidLookTools.getColors().getCheckBoxButtonColors()[2][0];
			c2 = RapidLookTools.getColors().getCheckBoxButtonColors()[2][1];
			c3 = RapidLookTools.getColors().getCheckBoxButtonColors()[2][2];
		} else {
			if (isRollover) {
				c1 = RapidLookTools.getColors().getCheckBoxButtonColors()[1][0];
				c2 = RapidLookTools.getColors().getCheckBoxButtonColors()[1][1];
				c3 = RapidLookTools.getColors().getCheckBoxButtonColors()[1][2];
			} else {
				c1 = RapidLookTools.getColors().getCheckBoxButtonColors()[0][0];
				c2 = RapidLookTools.getColors().getCheckBoxButtonColors()[0][1];
				c3 = RapidLookTools.getColors().getCheckBoxButtonColors()[0][2];
			}
		}
		g.setColor(c3);
		g.drawRect(1, 1, 13, 13);
		g.setColor(c2);
		g.drawLine(1, 1, 1, 1);
		g.drawLine(14, 1, 14, 1);
		g.drawLine(1, 14, 1, 14);
		g.drawLine(14, 14, 14, 14);
		g.setColor(c1);
		g.drawLine(0, 2, 0, 13);
		g.drawLine(2, 0, 13, 0);
		g.drawLine(15, 2, 15, 13);
		g.drawLine(2, 15, 13, 15);

		// drawing background section
		if (!isEnabled) {
			g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[2][3]);
			g.fillRect(2, 2, 12, 12);
		} else {
			if (isPressed) {
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[1][4]);
				g.fillRect(2, 2, 12, 12);
			} else if (isSelected) {
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[1][3]);
				g.fillRect(2, 2, 12, 12);
			} else {
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[0][3]);
				g.drawLine(2, 2, 13, 2);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[0][3]);
				g.drawLine(2, 3, 13, 3);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[0][3]);
				g.drawLine(2, 4, 13, 4);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[0][3]);
				g.drawLine(2, 5, 13, 5);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[0][4]);
				g.drawLine(2, 6, 13, 6);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[0][5]);
				g.drawLine(2, 7, 13, 7);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[0][6]);
				g.drawLine(2, 8, 13, 8);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[0][7]);
				g.drawLine(2, 9, 13, 9);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[0][8]);
				g.drawLine(2, 10, 13, 10);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[0][8]);
				g.drawLine(2, 11, 13, 11);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[0][5]);
				g.drawLine(2, 12, 13, 12);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[0][4]);
				g.drawLine(2, 13, 13, 13);
			}
		}

		if (isSelected) {
			g.translate(2, 3);
			if (isEnabled) {
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[3][0]);
				g.drawLine(4, 6, 4, 9);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[3][1]);
				g.drawLine(9, 1, 9, 1);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[3][2]);
				g.drawLine(9, 3, 9, 3);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[3][3]);
				g.drawLine(9, 2, 5, 6);
				g.drawLine(3, 6, 3, 6);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[3][4]);
				g.drawLine(8, 4, 6, 6);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[3][5]);
				g.drawLine(7, 6, 6, 7);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[3][6]);
				g.drawLine(8, 5, 8, 5);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[3][7]);
				g.drawLine(2, 7, 5, 7);
				g.drawLine(3, 8, 5, 8);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[3][8]);
				g.fillRect(1, 5, 2, 2);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[3][9]);
				g.fillRect(1, 4, 1, 1);
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[3][10]);
				g.drawLine(2, 6, 2, 7);
			} else {
				g.setColor(RapidLookTools.getColors().getCheckBoxButtonColors()[2][4]);
				g.drawLine(4, 6, 4, 9);
				g.drawLine(9, 1, 9, 1);
				g.drawLine(9, 3, 9, 3);
				g.drawLine(9, 2, 5, 6);
				g.drawLine(8, 4, 6, 6);
				g.drawLine(7, 6, 6, 7);
				g.drawLine(8, 5, 8, 5);
				g.drawLine(2, 7, 5, 7);
				g.drawLine(3, 8, 5, 8);
				g.fillRect(1, 5, 2, 2);
				g.fillRect(1, 4, 1, 1);
				g.drawLine(2, 6, 2, 7);
				g.drawLine(3, 6, 3, 6);
			}
			g.translate(-2, -3);
		}
	}

	@Override
	protected void paintImage(Component c, Graphics g, int x, int y, int imageW, int imageH, Image image, Object[] args) {
		g.drawImage(image, 0, 0, null);
	}
}
