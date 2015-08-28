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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;


/**
 * The menu item radio button icon.
 * 
 * @author Ingo Mierswa
 */
public class RadioButtonMenuItemIcon implements Icon, UIResource, Serializable {

	private static final long serialVersionUID = -7415345504361964833L;

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		JMenuItem b = (JMenuItem) c;
		ButtonModel model = b.getModel();
		boolean isSelected = model.isSelected();
		boolean isEnabled = model.isEnabled();
		boolean isPressed = model.isPressed();
		boolean isArmed = model.isArmed();

		g.translate(x, y);
		if (isEnabled) {
			if (isPressed || isArmed) {
				drawCircles(g, new ColorUIResource(250, 250, 250), new ColorUIResource(162, 188, 241), new ColorUIResource(
						206, 220, 245));
			} else {
				drawCircles(g, new ColorUIResource(150, 150, 150), new ColorUIResource(230, 230, 230), new ColorUIResource(
						190, 190, 190));
			}
		} else {
			drawCircles(g, new ColorUIResource(160, 160, 160), new ColorUIResource(210, 210, 210), new ColorUIResource(190,
					190, 190));
		}

		if (isSelected) {
			Color c1;
			Color c2;
			if (isEnabled) {
				if (model.isArmed() || ((c instanceof JMenu) && model.isSelected())) {
					c1 = new ColorUIResource(162, 188, 241);
					c2 = Color.white;
				} else {
					c1 = new ColorUIResource(200, 200, 200);
					c2 = new ColorUIResource(120, 120, 120);
				}
			} else {
				c1 = new ColorUIResource(200, 200, 200);
				c2 = new ColorUIResource(150, 150, 150);
			}
			g.setColor(c1);
			g.drawLine(5, 3, 5, 7);
			g.drawLine(3, 5, 7, 5);

			g.setColor(c2);
			g.fillRect(4, 4, 3, 3);
		}
		g.translate(-x, -y);
	}

	static void drawCircles(Graphics g, Color c1, Color c2, Color c3) {
		g.setColor(c1);
		g.drawLine(3, 1, 7, 1);
		g.drawLine(9, 3, 9, 7);
		g.drawLine(3, 9, 7, 9);
		g.drawLine(1, 3, 1, 7);
		g.drawLine(2, 2, 2, 2);
		g.drawLine(8, 2, 8, 2);
		g.drawLine(8, 8, 8, 8);
		g.drawLine(2, 8, 2, 8);

		g.setColor(c2);
		g.drawLine(1, 2, 2, 1);
		g.drawLine(4, 0, 6, 0);
		g.drawLine(8, 1, 9, 2);
		g.drawLine(10, 4, 10, 6);
		g.drawLine(9, 8, 8, 9);
		g.drawLine(4, 10, 6, 10);
		g.drawLine(1, 8, 2, 9);
		g.drawLine(0, 4, 0, 6);

		g.setColor(c3);
		g.drawLine(5, 1, 5, 1);
		g.drawLine(5, 9, 5, 9);
		g.drawLine(1, 5, 1, 5);
		g.drawLine(9, 5, 9, 5);
	}

	@Override
	public int getIconWidth() {
		return IconFactory.MENU_ICON_SIZE.width;
	}

	@Override
	public int getIconHeight() {
		return IconFactory.MENU_ICON_SIZE.height;
	}
}
