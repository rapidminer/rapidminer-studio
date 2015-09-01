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
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;


/**
 * The list view icon.
 * 
 * @author Ingo Mierswa
 */
public class ListViewIcon implements Icon, UIResource, Serializable {

	private static final long serialVersionUID = -5084915669804332770L;

	public static final Dimension ICON_SIZE = new Dimension(14, 10);

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		g.setColor(Color.red);
		g.fillRect(0, 0, 14, 10);
		g.setColor(new ColorUIResource(225, 235, 245));
		g.fillRect(0, 0, 5, 4);
		g.setColor(new ColorUIResource(160, 210, 250));
		g.fillRect(1, 0, 3, 4);
		g.fillRect(0, 1, 5, 2);
		g.setColor(new ColorUIResource(70, 170, 255));
		g.fillRect(1, 1, 3, 2);
		g.translate(8, 0);
		g.setColor(new ColorUIResource(225, 235, 245));
		g.fillRect(0, 0, 5, 4);
		g.setColor(new ColorUIResource(160, 210, 250));
		g.fillRect(1, 0, 3, 4);
		g.fillRect(0, 1, 5, 2);
		g.setColor(new ColorUIResource(70, 170, 255));
		g.fillRect(1, 1, 3, 2);
		g.translate(-8, 0);
		g.translate(0, 6);
		g.setColor(new ColorUIResource(225, 235, 245));
		g.fillRect(0, 0, 5, 4);
		g.setColor(new ColorUIResource(160, 210, 250));
		g.fillRect(1, 0, 3, 4);
		g.fillRect(0, 1, 5, 2);
		g.setColor(new ColorUIResource(70, 170, 255));
		g.fillRect(1, 1, 3, 2);
		g.translate(8, 0);
		g.setColor(new ColorUIResource(225, 235, 245));
		g.fillRect(0, 0, 5, 4);
		g.setColor(new ColorUIResource(160, 210, 250));
		g.fillRect(1, 0, 3, 4);
		g.fillRect(0, 1, 5, 2);
		g.setColor(new ColorUIResource(70, 170, 255));
		g.fillRect(1, 1, 3, 2);
		g.translate(-8, -6);
	}

	@Override
	public int getIconWidth() {
		return ICON_SIZE.width;
	}

	@Override
	public int getIconHeight() {
		return ICON_SIZE.height;
	}
}
