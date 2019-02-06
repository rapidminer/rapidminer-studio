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
import javax.swing.JRadioButton;
import javax.swing.plaf.UIResource;

import com.rapidminer.gui.look.Colors;


/**
 * The radio button icon.
 *
 * @author Ingo Mierswa, Marco Boeck
 */
public class RadioButtonIcon implements Icon, UIResource, Serializable {

	private static final long serialVersionUID = -2576744883403903818L;

	private static final Stroke RADIO_STROKE = new BasicStroke(2f);

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		JRadioButton radioButton = (JRadioButton) c;
		ButtonModel bm = radioButton.getModel();
		int w = c.getWidth();
		int h = c.getHeight();
		if (h < 0 || w < 0) {
			return;
		}

		g.translate(x, y);

		boolean isSelected = bm.isSelected();
		boolean isEnabled = bm.isEnabled();
		boolean isPressed = bm.isPressed();

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		// drawing background section
		if (!isEnabled) {
			g2.setColor(Colors.RADIOBUTTON_BACKGROUND_DISABLED);
		} else {
			g2.setColor(Colors.RADIOBUTTON_BACKGROUND);
		}
		Shape circle = new Ellipse2D.Double(2, 2, 12, 12);
		g2.fill(circle);

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
		circle = new Ellipse2D.Double(1, 1, 14, 14);
		g2.draw(circle);

		// drawing sphere
		if (isSelected) {
			if (isEnabled) {
				g2.setColor(Colors.RADIOBUTTON_CHECKED);
			} else {
				g2.setColor(Colors.RADIOBUTTON_CHECKED_DISABLED);
			}
			circle = new Ellipse2D.Double(4, 4, 9, 9);
			g2.fill(circle);
		}

		g.translate(-x, -y);
	}

	@Override
	public int getIconWidth() {
		return 16;
	}

	@Override
	public int getIconHeight() {
		return 16;
	}
}
