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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;

import com.rapidminer.gui.look.RoundedPopupFactory;


/**
 * The UIResource for popup borders.
 *
 * @author Ingo Mierswa
 */
public class PopupBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = -6179692340222478237L;

	private Image bottomLeft;
	private Image bottomRight;
	private Image right;
	private Image bottom;

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		JComponent popup = (JComponent) c;
		g.translate(x, y);
		try {
			this.bottomLeft = (Image) popup.getClientProperty(RoundedPopupFactory.BOTTOM_LEFT_PIC);
			if (this.bottomLeft != null) {
				g.drawImage(this.bottomLeft, 0, h - 5, c);
			}

			this.bottomRight = (Image) popup.getClientProperty(RoundedPopupFactory.BOTTOM_RIGHT_PIC);
			if (this.bottomRight != null) {
				g.drawImage(this.bottomRight, w - 5, h - 5, c);
			}

			this.right = (Image) popup.getClientProperty(RoundedPopupFactory.RIGHT_PIC);
			if (this.right != null) {
				g.drawImage(this.right, w - 1, 0, c);
			}

			this.bottom = (Image) popup.getClientProperty(RoundedPopupFactory.BOTTOM_PIC);
			if (this.bottom != null) {
				g.drawImage(this.bottom, 5, h - 1, c);
			}
		} catch (Exception exp) {
			// do nothing
		}

		if (ShadowedPopupMenuBorder.POPUP_BOTTOM_LEFT != null) {
			g.drawImage(ShadowedPopupMenuBorder.POPUP_BOTTOM_LEFT, 0, h - 5, popup);
		}
		if (ShadowedPopupMenuBorder.POPUP_BOTTOM_RIGHT != null) {
			g.drawImage(ShadowedPopupMenuBorder.POPUP_BOTTOM_RIGHT, w - 5, h - 5, popup);
		}

		ColorUIResource c1 = new ColorUIResource(150, 150, 150);
		Color c4 = new Color(160, 160, 160, 100);
		ColorUIResource c2 = new ColorUIResource(135, 135, 135);

		g.setColor(UIManager.getColor("MenuItem.background"));
		g.drawLine(1, 0, w - 4, 0);
		g.drawLine(1, 1, w - 4, 1);
		g.drawLine(1, 0, 1, h - 7);
		g.drawLine(5, h - 3, w - 6, h - 3);
		g.setColor(UIManager.getColor("MenuItem.fadingColor"));
		g.drawLine(w - 3, 2, w - 3, h - 5);

		g.setColor(c1);
		g.drawLine(0, 0, 0, h - 6);
		g.setColor(c4);
		g.drawLine(5, h - 1, w - 6, h - 1);
		g.drawLine(w - 1, 2, w - 1, h - 6);

		g.setColor(c2);
		g.drawLine(w - 2, 0, w - 2, h - 6);
		g.drawLine(5, h - 2, w - 6, h - 2);

		g.translate(-x, -y);
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(1, 1, 1, 1);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.left = 1;
		insets.top = 1;
		insets.right = insets.bottom = 1;
		return insets;
	}
}
