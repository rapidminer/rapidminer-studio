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
package com.rapidminer.gui.look.icons;

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.plaf.ColorUIResource;


/**
 * The slider thumb icon.
 * 
 * @author Ingo Mierswa
 */
public class SliderThumb implements Icon, Serializable {

	private static final long serialVersionUID = 398190943228862938L;

	protected static final int SIZE = 9;

	protected static final int HALF_SIZE = 4;

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		g.translate(x, y);
		g.setColor(new ColorUIResource(174, 196, 238));
		g.drawLine(9, 1, 14, 6);
		g.drawLine(6, 1, 1, 6);
		g.setColor(new ColorUIResource(224, 231, 246));
		g.drawLine(10, 1, 15, 6);
		g.drawLine(5, 1, 0, 6);
		g.setColor(new ColorUIResource(145, 180, 242));
		g.drawLine(1, 7, 1, 14);
		g.drawLine(14, 7, 14, 14);
		g.setColor(new ColorUIResource(180, 200, 235));
		g.drawLine(0, 7, 0, 13);
		g.drawLine(15, 7, 15, 13);
		g.setColor(new ColorUIResource(225, 232, 245));
		g.drawLine(0, 14, 1, 15);
		g.drawLine(14, 15, 15, 14);
		g.setColor(new ColorUIResource(220, 230, 250));
		g.drawLine(7, 0, 8, 0);
		g.setColor(new ColorUIResource(175, 195, 238));
		g.drawLine(7, 1, 8, 1);
		g.setColor(new ColorUIResource(254, 255, 255));
		g.drawLine(6, 2, 9, 2);
		g.setColor(new ColorUIResource(242, 247, 255));
		g.drawLine(5, 3, 10, 3);
		g.setColor(new ColorUIResource(228, 238, 255));
		g.drawLine(4, 4, 11, 4);
		g.setColor(new ColorUIResource(215, 230, 254));
		g.drawLine(3, 5, 12, 5);
		g.setColor(new ColorUIResource(200, 220, 254));
		g.drawLine(2, 6, 13, 6);
		g.setColor(new ColorUIResource(186, 212, 254));
		g.drawLine(2, 7, 13, 7);
		g.setColor(new ColorUIResource(172, 203, 253));
		g.drawLine(2, 8, 13, 8);
		g.setColor(new ColorUIResource(158, 195, 253));
		g.drawLine(2, 9, 13, 9);
		g.setColor(new ColorUIResource(148, 188, 253));
		g.drawLine(2, 10, 13, 10);
		g.setColor(new ColorUIResource(161, 196, 253));
		g.drawLine(2, 11, 13, 11);
		g.setColor(new ColorUIResource(174, 205, 254));
		g.drawLine(2, 12, 13, 12);
		g.setColor(new ColorUIResource(188, 213, 254));
		g.drawLine(2, 13, 13, 13);
		g.setColor(new ColorUIResource(208, 223, 249));
		g.drawLine(2, 14, 13, 14);
		g.setColor(new ColorUIResource(180, 200, 234));
		g.drawLine(2, 15, 13, 15);
		g.translate(-x, -y);
	}

	@Override
	public int getIconWidth() {
		return 9;
	}

	@Override
	public int getIconHeight() {
		return 9;
	}
}
