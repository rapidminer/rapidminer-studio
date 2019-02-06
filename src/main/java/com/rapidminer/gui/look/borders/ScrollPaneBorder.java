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
package com.rapidminer.gui.look.borders;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;


/**
 * The UIResource for scroll pane borders.
 * 
 * @author Ingo Mierswa
 */
public class ScrollPaneBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = 8716985175035922340L;

	// private static final Insets INSETS = new Insets(1, 1, 1, 1);
	private static final Insets INSETS = new Insets(1, 0, 0, 0);

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		g.translate(x, y);
		g.setColor(new ColorUIResource(190, 190, 190));
		g.drawLine(0, 0, w - 1, 0);
		g.drawLine(0, 0, 0, h - 1);
		g.setColor(new ColorUIResource(220, 220, 220));
		g.drawLine(w - 1, 1, w - 1, h - 1);
		g.drawLine(1, h - 1, w - 1, h - 1);
		g.translate(-x, -y);
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return INSETS;
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.top = INSETS.top;
		insets.left = INSETS.left;
		insets.bottom = INSETS.bottom;
		insets.right = INSETS.right;
		return insets;
	}
}
