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

import javax.swing.Action;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;


/**
 * A {@link JToggleButton} that can be used to define <em>horizontal</em> composite elements such as
 * button groups or split buttons.
 * <p>
 * The button overrides the default paint methods to allow for a composition without gaps between
 * the individual button elements. As a consequence, {@code CompositeButton}s should never be used
 * as stand alone component.
 * <p>
 * Whether the {@code CompositeButton} is the left-most, a center, or the right-most element of the
 * composition can be specified in the constructors via the Swing constants
 * {@link SwingConstants#LEFT}, {@link SwingConstants#CENTER}, and {@link SwingConstants#RIGHT}
 * respectively.
 *
 * @author Michael Knopf
 * @since 7.0.0
 */
class CompositeToggleButton extends JToggleButton {

	private static final long serialVersionUID = 1L;

	/** Paints the component background and border. */
	private final CompositeButtonPainter painter;

	/**
	 * Creates a new {@code CompositeToggleButton} with the given label to be used at the given
	 * position.
	 *
	 * @param label
	 *            the button label
	 * @param position
	 *            the position in the composite element ({@link SwingConstants#LEFT},
	 *            {@link SwingConstants#CENTER}, or {@link SwingConstants#RIGHT})
	 */
	public CompositeToggleButton(String label, int position) {
		super(label);
		// the parent should never draw its background
		super.setContentAreaFilled(false);
		super.setBorderPainted(false);
		painter = new CompositeButtonPainter(this, position);
	}

	/**
	 * Creates a new {@code CompositeToggleButton} with the given {@link Action} to be used at the
	 * given position.
	 *
	 * @param action
	 *            the button action
	 * @param position
	 *            the position in the composite element ({@link SwingConstants#LEFT},
	 *            {@link SwingConstants#CENTER}, or {@link SwingConstants#RIGHT})
	 */
	public CompositeToggleButton(Action action, int position) {
		super(action);
		// the parent should never draw its background
		super.setContentAreaFilled(false);
		super.setBorderPainted(false);
		painter = new CompositeButtonPainter(this, position);
	}

	@Override
	protected void paintComponent(Graphics g) {
		painter.paintComponent(g);
		super.paintComponent(g);
	}

	@Override
	protected void paintBorder(Graphics g) {
		painter.paintBorder(g);
	}
}
