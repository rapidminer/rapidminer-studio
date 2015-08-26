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

import com.rapidminer.gui.look.RapidLookListCellRenderer;
import com.rapidminer.gui.look.RapidLookTools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;
import javax.swing.plaf.UIResource;


/**
 * The UIResource for combo box list cell renderer focus borders.
 * 
 * @author Ingo Mierswa
 */
public class ComboBoxListCellRendererFocusBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = -7891513529402201022L;

	private Color selectionBackground = RapidLookTools.getColors().getTextHighlightBackColor();

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		if (c instanceof RapidLookListCellRenderer) {
			this.selectionBackground = ((RapidLookListCellRenderer) c).getParentList().getSelectionBackground();
		}

		g.translate(x, y);
		g.drawLine(0, 0, 0, 0);
		g.drawLine(w - 1, 0, w - 1, 0);
		g.drawLine(0, h - 1, 0, h - 1);
		g.drawLine(w - 1, h - 1, w - 1, h - 1);

		g.setColor(this.selectionBackground.brighter());
		g.drawLine(1, 0, 2, 0);
		g.drawLine(w - 2, 0, w - 3, 0);

		g.drawLine(1, h - 1, 2, h - 1);
		g.drawLine(w - 2, h - 1, w - 3, h - 1);

		g.drawLine(0, 1, 0, 2);
		g.drawLine(0, h - 2, 0, h - 3);

		g.drawLine(w - 1, 1, w - 1, 2);
		g.drawLine(w - 1, h - 2, w - 1, h - 3);

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
		insets.right = 1;
		insets.bottom = 1;
		return insets;
	}
}
