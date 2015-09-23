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
 * The UIResource for menu borders.
 * 
 * @author Ingo Mierswa
 */
public class MenuBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = 4220942363580919917L;

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		g.translate(x, y);
		g.setColor(new ColorUIResource(175, 215, 255));
		g.drawLine(0, 0, w - 3, 0);
		g.drawLine(0, 0, 0, h - 3);
		g.drawLine(w - 2, 0, w - 2, h);
		g.drawLine(0, h - 2, w, h - 2);
		g.setColor(new ColorUIResource(200, 225, 250));
		g.drawLine(w - 3, 0, w - 3, h);
		g.setColor(new ColorUIResource(160, 200, 255));
		g.drawLine(w - 1, 0, w - 1, h);
		g.drawLine(0, h - 1, w, h - 1);
		g.setColor(new ColorUIResource(190, 220, 255));
		g.translate(-x, -y);
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(3, 3, 4, 5);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.top = insets.left = 3;
		insets.bottom = 4;
		insets.right = 5;
		return insets;
	}
}
