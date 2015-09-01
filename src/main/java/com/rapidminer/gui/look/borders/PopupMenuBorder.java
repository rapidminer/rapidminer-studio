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
package com.rapidminer.gui.look.borders;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;


/**
 * The UIResource for popup menu borders.
 * 
 * @author Ingo Mierswa
 */
public class PopupMenuBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = 9188560188019884562L;

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		g.translate(x, y);

		g.setColor(new ColorUIResource(165, 165, 175));
		g.drawLine(0, 0, 0, h - 1);
		g.drawLine(0, h - 1, w - 1, h - 1);
		g.drawLine(0, 0, w - 1, 0);
		g.drawLine(w - 1, 0, w - 1, h - 1);

		g.setColor(new ColorUIResource(245, 245, 245));
		g.drawLine(1, 1, 1, h - 2);
		g.drawLine(1, h - 2, w - 2, h - 2);
		g.drawLine(1, 1, w - 2, 1);
		g.drawLine(w - 2, 1, w - 2, h - 2);

		g.translate(-x, -y);
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(2, 2, 2, 2);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.top = 2;
		insets.bottom = insets.right = 2;
		insets.left = 2;
		return insets;
	}
}
