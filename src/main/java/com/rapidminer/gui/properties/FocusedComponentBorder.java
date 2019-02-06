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
package com.rapidminer.gui.properties;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.border.AbstractBorder;
import javax.swing.plaf.UIResource;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookAndFeel;


/**
 * A border for focused components
 *
 * @author Nils Woehler
 */
public class FocusedComponentBorder extends AbstractBorder implements UIResource {

	private static final FocusedComponentBorder INSTANCE = new FocusedComponentBorder();

	public static FocusedComponentBorder getInstance() {
		return INSTANCE;
	}

	private static final long serialVersionUID = -7844804073270123279L;

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.translate(x, y);
		g2.setColor(Colors.TEXTFIELD_BORDER_FOCUS);
		g2.drawRoundRect(0, 0, w - 1, h - 1, RapidLookAndFeel.CORNER_DEFAULT_RADIUS, RapidLookAndFeel.CORNER_DEFAULT_RADIUS);

		g2.translate(-x, -y);
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(2, 2, 2, 2);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.right = insets.left = 2;
		insets.top = insets.bottom = 2;
		return insets;
	}
}
