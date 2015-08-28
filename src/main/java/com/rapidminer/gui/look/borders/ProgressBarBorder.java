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
 * The UIResource for progress bar borders.
 * 
 * @author Ingo Mierswa
 */
public class ProgressBarBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = 4150602481439529878L;

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		g.translate(x, y);
		g.setColor(new ColorUIResource(252, 252, 252));
		g.drawLine(4, 1, w - 5, 1);
		g.drawLine(2, 2, w - 3, 2);
		g.drawLine(4, h - 2, w - 5, h - 2);
		g.drawLine(2, h - 3, w - 3, h - 3);
		g.drawLine(1, 2, 1, h - 3);
		g.drawLine(2, 2, 2, h - 3);
		g.drawLine(w - 2, 2, w - 2, h - 3);
		g.drawLine(w - 3, 2, w - 3, h - 3);

		g.setColor(new ColorUIResource(140, 140, 150));
		g.drawLine(3, 0, w - 4, 0);
		g.drawLine(3, h - 1, w - 4, h - 1);
		g.drawLine(0, 2, 0, h - 3);
		g.drawLine(w - 1, 2, w - 1, h - 3);
		g.drawLine(1, 1, 2, 1);
		g.drawLine(0, 2, 1, 2);
		g.drawLine(1, h - 2, 2, h - 2);
		g.drawLine(0, h - 3, 1, h - 3);
		g.drawLine(w - 2, 1, w - 3, 1);
		g.drawLine(w - 1, 2, w - 2, 2);
		g.drawLine(w - 2, h - 2, w - 3, h - 2);
		g.drawLine(w - 1, h - 3, w - 2, h - 3);

		g.setColor(new ColorUIResource(235, 235, 235));
		g.drawLine(2, 0, 3, 1); // topleft
		g.drawLine(0, 2, 1, 3);
		g.drawLine(2, h - 1, 3, h - 2);
		g.drawLine(0, h - 3, 1, h - 4);
		g.drawLine(w - 3, 0, w - 4, 1);
		g.drawLine(w - 1, 2, w - 2, 3);
		g.drawLine(w - 3, h - 1, w - 4, h - 2);
		g.drawLine(w - 1, h - 3, w - 2, h - 4);
		g.translate(-x, -y);
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(3, 3, 3, 3);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.top = insets.left = insets.bottom = insets.right = 3;
		return insets;
	}
}
