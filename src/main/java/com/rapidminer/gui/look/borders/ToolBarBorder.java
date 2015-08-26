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

import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.look.ToolbarHandlerIcon;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;


/**
 * The UIResource for tool bar borders.
 * 
 * @author Ingo Mierswa
 */
public class ToolBarBorder extends AbstractBorder implements UIResource, SwingConstants {

	private static final long serialVersionUID = 6850360226068674391L;

	private final static ToolbarHandlerIcon TOOLBAR_HANDLER = new ToolbarHandlerIcon(10, 10,
			RapidLookAndFeel.getPrimaryControlHighlight(), RapidLookAndFeel.getPrimaryControlDarkShadow());

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		g.translate(x, y);
		if (((JToolBar) c).isFloatable()) {
			if (((JToolBar) c).getOrientation() == HORIZONTAL) {
				TOOLBAR_HANDLER.setBumpArea(8, c.getSize().height - 10);
				if (RapidLookTools.isLeftToRight(c)) {
					TOOLBAR_HANDLER.paintIcon(c, g, 4, 5);
				} else {
					TOOLBAR_HANDLER.paintIcon(c, g, c.getBounds().width - 12, 5);
				}
			} else {
				TOOLBAR_HANDLER.setBumpArea(c.getSize().width - 10, 8);
				TOOLBAR_HANDLER.paintIcon(c, g, 5, 4);
			}
		}

		if (((JToolBar) c).getOrientation() == HORIZONTAL) {
			g.setColor(new ColorUIResource(152, 152, 152));
			g.drawLine(0, 0, w - 1, 0);
			g.setColor(new ColorUIResource(252, 252, 252));
			g.drawLine(0, 1, w - 1, 1);
			g.setColor(new ColorUIResource(249, 249, 249));
			g.drawLine(0, 2, w - 1, 2);
			g.setColor(new ColorUIResource(245, 245, 245));
			g.drawLine(0, 3, w - 1, 3);

			g.setColor(new ColorUIResource(205, 205, 205));
			g.drawLine(0, h - 2, w - 1, h - 2);
			g.setColor(new ColorUIResource(180, 180, 180));
			g.drawLine(0, h - 1, w - 1, h - 1);
		} else {
			g.setColor(new ColorUIResource(152, 152, 152));
			g.drawLine(0, 0, 0, h - 1);
			g.setColor(new ColorUIResource(252, 252, 252));
			g.drawLine(1, 0, 1, h - 1);
			g.setColor(new ColorUIResource(249, 249, 249));
			g.drawLine(2, 0, 2, h - 1);
			g.setColor(new ColorUIResource(245, 245, 245));
			g.drawLine(3, 0, 3, h - 1);

			g.setColor(new ColorUIResource(205, 205, 205));
			g.drawLine(w - 3, 0, w - 3, h - 1);
			g.setColor(new ColorUIResource(180, 180, 180));
			g.drawLine(w - 2, 0, w - 2, h - 1);
			g.setColor(new ColorUIResource(145, 145, 145));
			g.drawLine(w - 1, 0, w - 1, h - 1);
		}
		g.translate(-x, -y);
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return getBorderInsets(c, new Insets(4, 0, 3, 0));
	}

	@Override
	public Insets getBorderInsets(Component c, Insets newInsets) {
		newInsets.top = newInsets.left = newInsets.bottom = newInsets.right = 4;

		if (((JToolBar) c).isFloatable()) {
			if (((JToolBar) c).getOrientation() == HORIZONTAL) {
				newInsets.top = 4;
				if (c.getComponentOrientation().isLeftToRight()) {
					newInsets.left = 16;
				} else {
					newInsets.right = 16;
				}
			} else {
				newInsets.left = 4;
				newInsets.top = 16;
			}
		}

		Insets margin = ((JToolBar) c).getMargin();
		if (margin != null) {
			newInsets.left += margin.left;
			newInsets.top += margin.top;
			newInsets.right += margin.right;
			newInsets.bottom += margin.bottom;
		}
		return newInsets;
	}
}
