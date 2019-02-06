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
package com.rapidminer.gui.tools.components.composite;

import java.awt.Graphics;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.io.Serializable;
import javax.swing.AbstractButton;
import javax.swing.SwingConstants;

import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.RapidLookTools;


/**
 * Provides paint methods for {@link CompositeButton} and related classes.
 *
 * @author Michael Knopf
 * @since 7.0.0
 */
public class CompositeButtonPainter implements Serializable {

	/** The (composite) button */
	private final AbstractButton button;

	/** The button's position within the composition. */
	private final int position;

	/**
	 * Creates a new {@code CompositeButtonPainter} for the given button. The position indicates the
	 * painting style.
	 *
	 * @param button
	 *            the button to paint
	 * @param position
	 *            the button's position
	 */
	public CompositeButtonPainter(AbstractButton button, int position) {
		// button must not be null
		if (button == null) {
			throw new IllegalArgumentException("Button must not be null!");
		}
		this.button = button;

		// the painter only support positions left, center, and right
		switch (position) {
			case SwingConstants.LEFT:
			case SwingConstants.CENTER:
			case SwingConstants.RIGHT:
				this.position = position;
				break;
			default:
				throw new IllegalArgumentException("Position (swing constant) not supported.");
		}
	}

	/**
	 * Draws the component background.
	 *
	 * @param g
	 *            the graphics context
	 */
	public void paintComponent(Graphics g) {
		RectangularShape rectangle;
		int radius = RapidLookAndFeel.CORNER_DEFAULT_RADIUS;
		switch (position) {
			case SwingConstants.LEFT:
				rectangle = new RoundRectangle2D.Double(0, 0, button.getWidth() + radius, button.getHeight(), radius,
						radius);
				break;
			case SwingConstants.CENTER:
				rectangle = new Rectangle2D.Double(0, 0, button.getWidth(), button.getHeight());
				break;
			default:
				rectangle = new RoundRectangle2D.Double(-radius, 0, button.getWidth() + radius, button.getHeight(), radius,
						radius);
				break;
		}
		RapidLookTools.drawButton(button, g, rectangle);
	}

	/**
	 * Draws the component border.
	 *
	 * @param g
	 *            the graphics context
	 */
	public void paintBorder(Graphics g) {
		Shape borderShape;
		int radius = RapidLookAndFeel.CORNER_DEFAULT_RADIUS;
		switch (position) {
			case SwingConstants.LEFT:
				borderShape = new RoundRectangle2D.Double(0, 0, button.getWidth() + radius, button.getHeight() - 1, radius, radius);
				RapidLookTools.drawButtonBorder(button, g, borderShape);
				break;
			case SwingConstants.CENTER:
				borderShape = new Rectangle2D.Double(0, 0, button.getWidth() + radius, button.getHeight() - 1);
				RapidLookTools.drawButtonBorder(button, g, borderShape);
				break;
			default:
				borderShape = new RoundRectangle2D.Double(-radius, 0, button.getWidth() + radius - 1, button.getHeight() - 1, radius, radius);
				RapidLookTools.drawButtonBorder(button, g, borderShape);
				// special case, right button has a left border
				borderShape = new Line2D.Double(0, 0, 0, button.getHeight());
				RapidLookTools.drawButtonBorder(button, g, borderShape);
				break;
		}

	}
}
