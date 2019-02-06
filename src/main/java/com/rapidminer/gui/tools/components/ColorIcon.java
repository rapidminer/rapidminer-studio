/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package com.rapidminer.gui.tools.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;


/**
 * This is an icon, that shows just a single color. This can be used for demonstrating selected
 * colors.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public class ColorIcon implements Icon {

	private Color color;
	private Color borderColor = Color.DARK_GRAY;

	private int width = 20;
	private int height = 8;

	public ColorIcon(Color color) {
		this.color = color;
	}

	public ColorIcon(Color color, int width, int height) {
		this.color = color;
		this.width = width;
		this.height = height;
	}

	public Color getColor() {
		return this.color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Color getBorderColor() {
		return this.borderColor;
	}

	public void setBorderColor(Color borderColor) {
		this.borderColor = borderColor;
	}

	@Override
	public int getIconWidth() {
		return width;
	}

	@Override
	public int getIconHeight() {
		return height;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();
		if (color != null) {
			g2.setColor(color);
			g2.fillRoundRect(x, y, getIconWidth() - 1, getIconHeight(), 2, 2);
		} else {
			// indicate null color
			g2.setColor(borderColor);
			int iconWidthQuarter = getIconWidth() / 4;
			int iconHeightHalf = getIconHeight() / 2;
			g2.drawLine(x, y + iconHeightHalf, x + iconWidthQuarter, y + getIconHeight());
			g2.drawLine(x, y, x + 2 * iconWidthQuarter, y + getIconHeight());
			g2.drawLine(x + iconWidthQuarter, y, x + 3 * iconWidthQuarter, y + getIconHeight());
			g2.drawLine(x + 2 * iconWidthQuarter, y, x + 4 * iconWidthQuarter - 1, y + getIconHeight());
			g2.drawLine(x + 3 * iconWidthQuarter, y, x + 4 * iconWidthQuarter - 1, y + getIconHeight() / 2);
		}

		g2.setColor(borderColor);
		g2.drawRoundRect(x, y, getIconWidth() - 1, getIconHeight(), 2, 2);

		g2.dispose();
	}
}
