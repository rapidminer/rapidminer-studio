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
package com.rapidminer.gui.processeditor.results;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.GeneralPath;

import javax.swing.border.Border;


/**
 * 
 * @author Simon Fischer
 */
public class RapidBorder implements Border {

	private static final Insets INSETS = new Insets(20, 5, 5, 5);
	private final Color color;
	private final int cornerSize;
	private final int headerHeight;

	protected RapidBorder(Color color, int cornerSize, int headerHeight) {
		super();
		this.color = color;
		this.cornerSize = cornerSize;
		this.headerHeight = headerHeight;
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return INSETS;
	}

	@Override
	public boolean isBorderOpaque() {
		return true;
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {

		width--;
		height--;

		GeneralPath border = new GeneralPath();
		border.moveTo(x + cornerSize, y); // top left
		border.lineTo(x + width - cornerSize, y);
		border.curveTo(x + width, y, y + width, y, x + width, y + cornerSize); // top right
		border.lineTo(x + width, y + height - cornerSize);
		border.curveTo(x + width, y + height, x + width, y + height, x + width - cornerSize, y + height); // bottom
																											// right
		border.lineTo(x + cornerSize, y + height);
		border.curveTo(x, y + height, x, y + height, x, y + height - cornerSize); // bottom left
		border.lineTo(x, y + cornerSize);
		border.curveTo(x, y, x, y, x + cornerSize, y); // top left again

		if (headerHeight > 0) {
			if (height <= headerHeight + cornerSize) {
				g.setColor(color);
				((Graphics2D) g).fill(border);
			} else {
				g.setColor(c.getBackground());
				((Graphics2D) g).fill(border);

				GeneralPath header = new GeneralPath();
				header.moveTo(x + cornerSize, y); // top left
				header.lineTo(x + width - cornerSize, y);
				header.curveTo(x + width, y, y + width, y, x + width, y + cornerSize); // top right
				header.lineTo(x + width, y + headerHeight);
				header.lineTo(x, y + headerHeight);
				header.lineTo(x, y + cornerSize);
				header.curveTo(x, y, x, y, x + cornerSize, y); // top left again
				g.setColor(color);
				((Graphics2D) g).fill(header);
			}
		}

		g.setColor(color);
		((Graphics2D) g).draw(border);
	}
}
