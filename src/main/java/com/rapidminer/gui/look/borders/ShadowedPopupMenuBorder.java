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

import com.rapidminer.gui.look.RoundedPopupFactory;
import com.rapidminer.gui.tools.SwingTools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;


/**
 * The UIResource for shadowed popup menu borders.
 * 
 * @author Ingo Mierswa
 */
public class ShadowedPopupMenuBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = 1446180576173319227L;

	public final static Image POPUP_TOP_RIGHT = SwingTools.createImage("plaf/popupShadowTopRight.png").getImage();

	public final static Image POPUP_TOP_LEFT = SwingTools.createImage("plaf/popupShadowTopLeft.png").getImage();

	public final static Image POPUP_BOTTOM_LEFT = SwingTools.createImage("plaf/popupShadowBottomLeft.png").getImage();

	public final static Image POPUP_BOTTOM_RIGHT = SwingTools.createImage("plaf/popupShadowBottomRight.png").getImage();

	private Image topRight;
	private Image topLeft;
	private Image bottomRight;
	private Image bottomLeft;
	private Image right;
	private Image bottom;

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		JComponent popup = (JComponent) c;
		g.translate(x, y);
		try {
			this.topLeft = (Image) popup.getClientProperty(RoundedPopupFactory.TOP_LEFT_PIC);
			if (this.topLeft != null) {
				g.drawImage(this.topLeft, 0, 0, c);
			}

			this.topRight = (Image) popup.getClientProperty(RoundedPopupFactory.TOP_RIGHT_PIC);
			if (this.topRight != null) {
				g.drawImage(this.topRight, w - 5, 0, c);
			}

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

		if (POPUP_TOP_LEFT != null) {
			g.drawImage(POPUP_TOP_LEFT, 0, 0, popup);
		}
		if (POPUP_TOP_RIGHT != null) {
			g.drawImage(POPUP_TOP_RIGHT, w - 5, 0, popup);
		}
		if (POPUP_BOTTOM_LEFT != null) {
			g.drawImage(POPUP_BOTTOM_LEFT, 0, h - 5, popup);
		}
		if (POPUP_BOTTOM_RIGHT != null) {
			g.drawImage(POPUP_BOTTOM_RIGHT, w - 5, h - 5, popup);
		}

		ColorUIResource c1 = new ColorUIResource(150, 150, 150);
		Color c4 = new Color(160, 160, 160, 100);
		ColorUIResource c2 = new ColorUIResource(135, 135, 135);

		g.setColor(c1);
		g.drawLine(5, 0, w - 6, 0);
		g.drawLine(0, 5, 0, h - 6);
		g.setColor(c4);
		g.drawLine(5, h - 1, w - 6, h - 1);
		g.drawLine(w - 1, 5, w - 1, h - 6);

		g.setColor(c2);
		g.drawLine(w - 2, 5, w - 2, h - 6);
		g.drawLine(5, h - 2, w - 6, h - 2);

		g.translate(-x, -y);
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(2, 2, 3, 3);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.left = 2;
		insets.top = 2;
		insets.right = insets.bottom = 3;
		return insets;
	}
}
