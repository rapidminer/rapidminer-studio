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
import javax.swing.plaf.UIResource;

import com.rapidminer.gui.look.Colors;


/**
 * The UIResource for empty combo box borders.
 *
 * @author Ingo Mierswa
 */
public class EmptyComboBoxBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = -1398214714584187754L;

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		g.translate(x, y);
		g.setColor(Colors.WHITE);
		g.drawLine(1, 6, 1, h - 7);
		g.drawLine(2, 4, 2, h - 5);
		g.drawLine(3, 3, 3, h - 4);
		g.drawLine(4, 2, 4, h - 3);
		g.translate(-x, -y);
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(3, 5, 3, 1);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.top = 3;
		insets.bottom = 3;
		insets.left = 5;
		insets.right = 1;
		return insets;
	}
}
