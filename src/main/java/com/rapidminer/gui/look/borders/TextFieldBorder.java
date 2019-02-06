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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.UIResource;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.RapidLookTools;


/**
 * The UIResource for text field borders.
 *
 * @author Ingo Mierswa
 */
public class TextFieldBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = -7844804073270123279L;

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.translate(x, y);

		boolean darkBorder = c instanceof JComponent && Boolean.parseBoolean(String.valueOf(((JComponent) c).getClientProperty(RapidLookTools.PROPERTY_INPUT_DARK_BORDER)));
		if (darkBorder) {
			if (c.isEnabled()) {
				if (c.isFocusOwner()) {
					g2.setColor(Colors.TEXTFIELD_BORDER_DARK_FOCUS);
				} else {
					g2.setColor(Colors.TEXTFIELD_BORDER_DARK);
				}
			} else {
				g2.setColor(Colors.TEXTFIELD_BORDER_DARK_DISABLED);
			}
		} else {
			if (c.isEnabled()) {
				if (c.isFocusOwner()) {
					g2.setColor(Colors.TEXTFIELD_BORDER_FOCUS);
				} else {
					g2.setColor(Colors.TEXTFIELD_BORDER);
				}
			} else {
				g2.setColor(Colors.TEXTFIELD_BORDER_DISABLED);
			}
		}

		// composite field, aka part of other components directly adjacent to it?
		int position = -1;
		if (c instanceof JComponent) {
			Object composite = ((JComponent) c).getClientProperty(RapidLookTools.PROPERTY_INPUT_TYPE_COMPOSITE);
			if (composite != null) {
				try {
					position = Integer.valueOf(String.valueOf(composite));
				} catch (NumberFormatException e) {
					// stay with -1 aka standalone
				}
			}

		}

		int radius = RapidLookAndFeel.CORNER_DEFAULT_RADIUS;
		Shape borderShape;
		switch (position) {
			case SwingConstants.LEFT:
				borderShape = new RoundRectangle2D.Double(0, 0, w + radius, h - 1, radius, radius);
				g2.draw(borderShape);
				break;
			case SwingConstants.CENTER:
				borderShape = new Rectangle2D.Double(0, 0, w + radius, h - 1);
				g2.draw(borderShape);
				break;
			case SwingConstants.RIGHT:
				borderShape = new RoundRectangle2D.Double(-radius, 0, w + radius - 1, h - 1, radius, radius);
				g2.draw(borderShape);
				// special case, right field has a left border
				borderShape = new Line2D.Double(0, 0, 0, h);
				g2.draw(borderShape);
				break;
			default:
				borderShape = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, radius, radius);
				g2.draw(borderShape);
				break;
		}

		g2.translate(-x, -y);
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(4, 4, 4, 4);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.right = insets.left = 4;
		insets.top = insets.bottom = 4;
		return insets;
	}
}
