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

import com.rapidminer.gui.look.RapidLookTools;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;


/**
 * The UIResource for text field borders.
 * 
 * @author Ingo Mierswa
 */
public class TextFieldBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = -7844804073270123279L;

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		g.translate(x, y);
		if (c.isOpaque()) {
			g.setColor(RapidLookTools.getColors().getCommonBackground());
			g.drawRect(0, 0, w - 1, h - 1);
		}
		g.setColor(c.getBackground());
		g.drawRect(3, 3, w - 7, h - 7);
		g.drawLine(3, 2, w - 4, 2);
		g.drawLine(3, h - 3, w - 4, h - 3);
		g.drawLine(2, 3, 2, h - 4);
		g.drawLine(w - 3, 3, w - 3, h - 4);

		ColorUIResource c1 = new ColorUIResource(0);
		ColorUIResource c2 = new ColorUIResource(0);
		ColorUIResource c3 = new ColorUIResource(0);
		ColorUIResource c4 = new ColorUIResource(0);

		if (c.isEnabled()) {
			if (c.hasFocus()) {
				c1 = RapidLookTools.getColors().getTextFieldBorderColors()[0][0];
				c2 = RapidLookTools.getColors().getTextFieldBorderColors()[0][1];
				c3 = RapidLookTools.getColors().getTextFieldBorderColors()[0][2];
				c4 = RapidLookTools.getColors().getTextFieldBorderColors()[0][3];
			} else {
				c1 = RapidLookTools.getColors().getTextFieldBorderColors()[1][0];
				c2 = RapidLookTools.getColors().getTextFieldBorderColors()[1][1];
				c3 = RapidLookTools.getColors().getTextFieldBorderColors()[1][2];
				c4 = RapidLookTools.getColors().getTextFieldBorderColors()[1][3];
			}
		} else {
			c1 = RapidLookTools.getColors().getTextFieldBorderColors()[2][0];
			c2 = RapidLookTools.getColors().getTextFieldBorderColors()[2][1];
			c3 = RapidLookTools.getColors().getTextFieldBorderColors()[2][2];
			c4 = RapidLookTools.getColors().getTextFieldBorderColors()[2][3];
		}

		g.setColor(c1);
		g.drawLine(3, 1, w - 4, 1);
		g.drawLine(3, h - 2, w - 4, h - 2);
		g.drawLine(1, 3, 1, h - 4);
		g.drawLine(w - 2, 3, w - 2, h - 4);
		g.drawLine(2, 2, 2, 2);
		g.drawLine(2, h - 3, 2, h - 3);
		g.drawLine(w - 3, 2, w - 3, 2);
		g.drawLine(w - 3, h - 3, w - 3, h - 3);

		// drawing corners outer
		g.setColor(c2);
		g.drawLine(3, 0, 3, 0);
		g.drawLine(0, 3, 0, 3);
		g.drawLine(w - 4, 0, w - 4, 0);
		g.drawLine(w - 1, 3, w - 1, 3);
		g.drawLine(3, h - 1, 3, h - 1);
		g.drawLine(0, h - 4, 0, h - 4);
		g.drawLine(w - 4, h - 1, w - 4, h - 1);
		g.drawLine(w - 1, h - 4, w - 1, h - 4);
		g.drawLine(1, 1, 1, 1);
		g.drawLine(1, h - 2, 1, h - 2);
		g.drawLine(w - 2, 1, w - 2, 1);
		g.drawLine(w - 2, h - 2, w - 2, h - 2);

		g.setColor(c3);
		g.drawLine(4, 0, w - 5, 0);
		g.drawLine(4, h - 1, w - 5, h - 1);
		g.drawLine(0, 4, 0, h - 5);
		g.drawLine(w - 1, 4, w - 1, h - 5);

		g.setColor(c4);
		g.drawLine(1, 2, 2, 1);
		g.drawLine(1, h - 3, 2, h - 2);
		g.drawLine(w - 2, 2, w - 3, 1);
		g.drawLine(w - 2, h - 3, w - 3, h - 2);

		g.translate(-x, -y);
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(3, 4, 3, 4);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.right = insets.left = 4;
		insets.top = insets.bottom = 3;
		return insets;
	}
}
