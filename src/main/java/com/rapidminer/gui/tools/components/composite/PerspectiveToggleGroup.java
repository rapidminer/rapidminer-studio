/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.awt.Dimension;

import javax.swing.Action;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.PerspectiveController;


/**
 * Group of {@link JToggleButton}s with a behavior similar to a radio button group. Used to manage
 * the application {@link Perspective}s.
 *
 * @author Marcel Michel
 * @since 7.0.0
 */
public class PerspectiveToggleGroup extends ToggleButtonGroup {

	private static final long serialVersionUID = 1L;

	private final PerspectiveController perspectiveController;

	/**
	 * Creates a new {@link PerspectiveToggleGroup} from the given Actions (requires at least two
	 * actions).
	 *
	 * @param perspectiveController
	 *            the perspective controller which should be used
	 * @param preferredSize
	 *            the preferredSize of the nested {@link CompositeToggleButton}s or {@code null}
	 * @param actions
	 *            the action
	 */
	public PerspectiveToggleGroup(PerspectiveController perspectiveController, Dimension preferredSize, Action... actions) {
		super(preferredSize, actions);
		this.perspectiveController = perspectiveController;
	}

	@Override
	protected CompositeMenuToggleButton createCompositeMenuToggleButton(Action... actions) {
		return new PerspectiveMenuToggleButton(perspectiveController, SwingConstants.RIGHT, actions);
	}
}
