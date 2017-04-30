/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
import javax.swing.plaf.UIResource;

import com.rapidminer.gui.look.Colors;


/**
 * The UIResource for menu bar borders.
 *
 * @author Ingo Mierswa
 */
public class MenuBarBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = -2583591973991105007L;

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		g.setColor(Colors.MENUBAR_BORDER);
		g.drawLine(0, h - 1, w, h - 1);
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(2, 5, 2, 0);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.top = 2;
		insets.bottom = 2;
		insets.left = 5;
		insets.right = 0;
		return insets;
	}
}
