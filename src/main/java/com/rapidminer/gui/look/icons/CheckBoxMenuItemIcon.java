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
package com.rapidminer.gui.look.icons;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.io.Serializable;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;

import com.rapidminer.gui.look.Colors;


/**
 * The menu item check box icon.
 *
 * @author Ingo Mierswa
 */
public class CheckBoxMenuItemIcon implements Icon, UIResource, Serializable {

	private static final long serialVersionUID = 1L;

	private static final Stroke CHECKBOX_STROKE = new BasicStroke(2f);

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		AbstractButton b = (AbstractButton) c;
		ButtonModel model = b.getModel();

		g.translate(x, y);

		boolean isSelected = model.isSelected();
		boolean isEnabled = model.isEnabled();

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// draw check mark
		if (isSelected) {
			g2.setStroke(CHECKBOX_STROKE);
			if (isEnabled) {
				g2.setColor(Colors.CHECKBOX_CHECKED);
			} else {
				g2.setColor(Colors.CHECKBOX_CHECKED_DISABLED);
			}
			g2.drawLine(2, 6, 5, 8);
			g2.drawLine(5, 8, 9, 1);
		}
		g.translate(-x, -y);
	}

	@Override
	public int getIconWidth() {
		return (int) (IconFactory.MENU_ICON_SIZE.width * 1.5);
	}

	@Override
	public int getIconHeight() {
		return IconFactory.MENU_ICON_SIZE.height;
	}
}
