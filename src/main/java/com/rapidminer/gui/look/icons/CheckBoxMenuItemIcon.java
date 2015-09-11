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
 * The menu item check box icon.
 * 
 * @author Ingo Mierswa
 */
public class CheckBoxMenuItemIcon implements Icon, UIResource, Serializable {

	private static final long serialVersionUID = -3035362898629322365L;

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		JMenuItem b = (JMenuItem) c;
		ButtonModel model = b.getModel();

		boolean isSelected = model.isSelected();
		boolean isEnabled = model.isEnabled();

		g.translate(x, y);
		if (isSelected) {
			if (isEnabled) {
				if (model.isArmed() || ((c instanceof JMenu) && model.isSelected())) {
					g.setColor(new Color(255, 255, 255, 150));
					g.drawLine(4, 6, 4, 9);
					g.setColor(new Color(255, 255, 255, 120));
					g.drawLine(9, 1, 9, 1);
					g.setColor(new Color(255, 255, 255, 160));
					g.drawLine(9, 3, 9, 3);
					g.setColor(new Color(255, 255, 255, 200));
					g.drawLine(9, 2, 5, 6);
					g.setColor(new Color(255, 255, 255, 190));
					g.drawLine(8, 4, 6, 6);
					g.setColor(new Color(255, 255, 255, 205));
					g.drawLine(7, 6, 6, 7);
					g.setColor(new Color(255, 255, 255, 150));
					g.drawLine(8, 5, 8, 5);
					g.setColor(new Color(255, 255, 255, 255));
					g.drawLine(2, 7, 5, 7);
					g.setColor(new Color(255, 255, 255, 230));
					g.drawLine(3, 8, 5, 8);
					g.setColor(new Color(255, 255, 255, 200));
					g.fillRect(1, 5, 2, 2);
					g.setColor(new Color(255, 255, 255, 150));
					g.fillRect(1, 4, 1, 1);
					g.setColor(new Color(255, 255, 255, 245));
					g.drawLine(2, 6, 2, 7);
					g.setColor(new Color(255, 255, 255, 230));
					g.drawLine(3, 6, 3, 6);
				} else {
					g.setColor(new ColorUIResource(176, 176, 176));
					g.drawLine(4, 6, 4, 9);
					g.setColor(new ColorUIResource(212, 212, 212));
					g.drawLine(9, 1, 6, 4);
					g.setColor(new ColorUIResource(200, 200, 200));
					g.drawLine(9, 3, 9, 3);
					g.setColor(new ColorUIResource(150, 150, 150));
					g.drawLine(9, 2, 5, 6);
					g.setColor(new ColorUIResource(130, 130, 130));
					g.drawLine(8, 4, 6, 6);
					g.setColor(new ColorUIResource(150, 150, 150));
					g.drawLine(7, 6, 6, 7);
					g.setColor(new ColorUIResource(195, 195, 195));
					g.drawLine(8, 5, 8, 5);
					g.setColor(new ColorUIResource(110, 110, 110));
					g.drawLine(2, 7, 5, 7);
					g.setColor(new ColorUIResource(145, 145, 145));
					g.drawLine(3, 8, 5, 8);
					g.setColor(new ColorUIResource(150, 150, 150));
					g.fillRect(1, 5, 2, 2);
					g.setColor(new ColorUIResource(180, 180, 180));
					g.fillRect(1, 4, 1, 1);
					g.setColor(new ColorUIResource(125, 125, 125));
					g.drawLine(2, 6, 2, 7);
					g.setColor(new ColorUIResource(150, 150, 150));
					g.drawLine(3, 6, 3, 6);
				}
			} else {
				g.setColor(new ColorUIResource(216, 216, 216));
				g.drawLine(4, 6, 4, 9);
				g.setColor(new ColorUIResource(232, 232, 232));
				g.drawLine(9, 1, 6, 4);
				g.setColor(new ColorUIResource(210, 210, 210));
				g.drawLine(9, 3, 9, 3);
				g.setColor(new ColorUIResource(180, 180, 180));
				g.drawLine(9, 2, 5, 6);
				g.setColor(new ColorUIResource(170, 170, 170));
				g.drawLine(8, 4, 6, 6);
				g.setColor(new ColorUIResource(190, 190, 190));
				g.drawLine(7, 6, 6, 7);
				g.setColor(new ColorUIResource(220, 220, 220));
				g.drawLine(8, 5, 8, 5);
				g.setColor(new ColorUIResource(170, 170, 170));
				g.drawLine(2, 7, 5, 7);
				g.setColor(new ColorUIResource(185, 185, 185));
				g.drawLine(3, 8, 5, 8);
				g.setColor(new ColorUIResource(190, 190, 190));
				g.fillRect(1, 5, 2, 2);
				g.setColor(new ColorUIResource(220, 220, 220));
				g.fillRect(1, 4, 1, 1);
				g.setColor(new ColorUIResource(175, 175, 175));
				g.drawLine(2, 6, 2, 7);
				g.setColor(new ColorUIResource(190, 190, 190));
				g.drawLine(3, 6, 3, 6);
			}
		}
		g.translate(-x, -y);
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
