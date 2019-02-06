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
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.io.Serializable;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.plaf.UIResource;

import com.rapidminer.gui.look.Colors;


/**
 * The menu item radio button icon.
 *
 * @author Ingo Mierswa
 */
public class RadioButtonMenuItemIcon implements Icon, UIResource, Serializable {

	private static final long serialVersionUID = 1L;

	private static final Stroke RADIO_STROKE = new BasicStroke(1f);

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		JMenuItem b = (JMenuItem) c;
		ButtonModel bm = b.getModel();

		g.translate(x, y);

		boolean isSelected = bm.isSelected();
		boolean isEnabled = bm.isEnabled();
		boolean isPressed = bm.isPressed();

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		// drawing background section
		if (!isEnabled) {
			g2.setColor(Colors.RADIOBUTTON_BORDER_DISABLED);
		} else {
			if (isPressed) {
				g2.setColor(Colors.RADIOBUTTON_BORDER_FOCUS);
			} else {
				g2.setColor(Colors.RADIOBUTTON_BORDER);
			}
		}
		g2.setStroke(RADIO_STROKE);
		Shape circle = new Ellipse2D.Double(0, 0, 9, 9);
		g2.draw(circle);

		// drawing sphere
		if (isSelected) {
			if (isEnabled) {
				g2.setColor(Colors.RADIOBUTTON_CHECKED);
			} else {
				g2.setColor(Colors.RADIOBUTTON_CHECKED_DISABLED);
			}
			circle = new Ellipse2D.Double(3, 3, 4, 4);
			g2.fill(circle);
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
