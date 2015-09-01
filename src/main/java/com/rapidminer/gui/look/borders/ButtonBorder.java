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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;


/**
 * The UIResource for button borders.
 * 
 * @author Ingo Mierswa
 */
public class ButtonBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = 191853543634535781L;

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		g.translate(x, y);
		JComponent jc = (JComponent) c;

		boolean isRollover = false;
		boolean isFocused = false;

		if (jc instanceof JButton) {
			isRollover = ((JButton) jc).getModel().isRollover();
			isFocused = ((JButton) jc).hasFocus();
		} else if (jc instanceof JComboBox) {
			isFocused = ((JComboBox) jc).hasFocus();
		}

		Color c1 = new ColorUIResource(0);
		Color c2 = new ColorUIResource(0);
		Color c3 = new ColorUIResource(0);
		Color c4 = new ColorUIResource(0);
		Color c5 = new ColorUIResource(0);
		Color c6 = new ColorUIResource(0);

		if (isFocused) {
			c1 = new ColorUIResource(105, 150, 225);
			c2 = new ColorUIResource(92, 120, 166);
			c3 = new ColorUIResource(130, 165, 220);
			c4 = new ColorUIResource(210, 220, 240);
			c5 = new ColorUIResource(163, 190, 230);
			c6 = new ColorUIResource(120, 150, 205);
		} else if (isRollover) {
			c1 = new ColorUIResource(105, 190, 105);
			c2 = new ColorUIResource(85, 140, 85);
			c3 = new ColorUIResource(105, 165, 105);
			c4 = new ColorUIResource(160, 210, 160);
			c5 = new ColorUIResource(145, 200, 145);
			c6 = new ColorUIResource(140, 190, 140);
		} else {
			c1 = new ColorUIResource(140, 140, 140);
			c2 = new ColorUIResource(130, 130, 130);
			c3 = new ColorUIResource(170, 170, 170);
			c4 = new ColorUIResource(210, 210, 210);
			c5 = new ColorUIResource(165, 165, 165);
			c6 = new ColorUIResource(155, 155, 155);
		}

		g.setColor(c1);
		g.drawLine(5, 0, w - 7, 0);
		g.drawLine(0, 6, 0, h - 7);
		g.drawLine(w - 1, 6, w - 1, h - 7);
		g.setColor(c2);
		g.drawLine(6, h - 1, w - 7, h - 1);

		g.setColor(c5);
		g.drawLine(0, 5, 1, 5);
		g.drawLine(5, 1, 5, 0);

		g.setColor(c1);
		g.drawLine(1, 4, 1, 3);
		g.drawLine(4, 1, 3, 1);
		g.drawLine(3, 1, 1, 3);

		g.setColor(c4);
		g.drawLine(4, 0, 5, 1);
		g.drawLine(0, 4, 1, 5);
		g.drawLine(2, 3, 3, 2);

		g.setColor(c5);
		g.drawLine(w - 1, 5, w - 2, 5);
		g.drawLine(w - 6, 0, w - 6, 1);

		g.setColor(c1);
		g.drawLine(w - 2, 4, w - 2, 3);
		g.drawLine(w - 5, 1, w - 4, 1);
		g.drawLine(w - 4, 1, w - 2, 3);

		g.setColor(c4);
		g.drawLine(w - 5, 0, w - 6, 1);
		g.drawLine(w - 1, 4, w - 2, 5);
		g.drawLine(w - 4, 2, w - 3, 3);

		g.setColor(c3);
		g.drawLine(2, h - 4, 3, h - 3);
		g.drawLine(5, h - 2, 5, h - 1);

		g.setColor(c5);
		g.drawLine(0, h - 6, 1, h - 6);

		g.setColor(c6);
		g.drawLine(1, h - 5, 1, h - 4);
		g.drawLine(4, h - 2, 3, h - 2);
		g.drawLine(3, h - 2, 1, h - 4);

		g.setColor(c3);
		g.drawLine(w - 3, h - 4, w - 4, h - 3);
		g.drawLine(w - 6, h - 2, w - 6, h - 1);

		g.setColor(c5);
		g.drawLine(w - 1, h - 6, w - 2, h - 6);

		g.setColor(c6);
		g.drawLine(w - 2, h - 5, w - 2, h - 4);
		g.drawLine(w - 5, h - 2, w - 4, h - 2);
		g.drawLine(w - 4, h - 2, w - 2, h - 4);

		g.translate(-x, -y);
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(2, 2, 2, 2);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.top = insets.left = insets.right = 2;
		insets.bottom = 5;
		return insets;
	}
}
