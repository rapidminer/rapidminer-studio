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

import java.awt.Dimension;
import javax.swing.Action;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.PerspectiveController;
import com.rapidminer.tools.I18N;


/**
 * Group of {@link JToggleButton}s with a behavior similar to a radio button group. Used to manage
 * the application {@link Perspective}s.
 *
 * @author Marcel Michel, Jan Czogalla
 * @since 7.0.0
 */
public class PerspectiveToggleGroup extends ToggleButtonGroup {

	private static final long serialVersionUID = 1L;
	private static final Action[] EMPTY_ACTIONS = new Action[0];
	private static final Action[] TWO_NULL_ACTIONS = new Action[2];

	private static final String SECONDARY_BUTTON_LABEL = I18N.getGUILabel("workspace_more");

	private static Dimension preferredMoreSize;
	private static Dimension minimizedMoreSize;

	private final PerspectiveController perspectiveController;

	/**
	 * Creates a new {@link PerspectiveToggleGroup} from the given Actions (requires at least two
	 * actions).
	 *
	 * @param perspectiveController
	 * 		the perspective controller which should be used
	 * @param preferredSize
	 * 		the preferredSize of the nested {@link CompositeToggleButton}s or {@code null}
	 * @param actions
	 * 		the action
	 */
	public PerspectiveToggleGroup(PerspectiveController perspectiveController, Dimension preferredSize, Action... actions) {
		super(preferredSize, actions);
		this.perspectiveController = perspectiveController;
	}

	@Override
	protected CompositeMenuToggleButton createCompositeMenuToggleButton(Action... actions) {
		return new PerspectiveMenuToggleButton(perspectiveController, SwingConstants.RIGHT, actions);
	}

	/**
	 * Minimizes the "More" button to only show the icon. Is used to make the perspectives panel smaller. Will do
	 * nothing if no secondary button was created yet or the button is already minimized.
	 *
	 * @since 8.1
	 */
	public void minimizeSecondaryButton() {
		if (secondaryButton == null || secondaryButton.getText().isEmpty()) {
			return;
		}
		secondaryButton.setText("");
		secondaryButton.setPreferredSize(getMinimizedSecondaryButtonSize());
	}

	/**
	 * Restores the "More" button to show also text. Is used in resizing the perspectives panel. Will do nothing if no
	 * secondary button was created yet or the button is already maximized.
	 *
	 * @since 8.1
	 */
	public void maximizeSecondaryButton() {
		if (secondaryButton == null || !secondaryButton.getText().isEmpty()) {
			return;
		}
		secondaryButton.setText(SECONDARY_BUTTON_LABEL);
		secondaryButton.setPreferredSize(getDefaultSecondaryButtonSize());
	}

	/**
	 * Initializes both {@link #preferredMoreSize} and {@link #minimizedMoreSize}. Should be called before first usages
	 * of {@link #getDefaultSecondaryButtonSize()} and {@link #getMinimizedSecondaryButtonSize()}. Will not change anything
	 * if called again.
	 *
	 * @since 8.1
	 */
	public static void init(){
		if (preferredMoreSize == null) {
			PerspectiveToggleGroup group = new PerspectiveToggleGroup(null, new Dimension(), TWO_NULL_ACTIONS);
			group.addSeconderyActions(EMPTY_ACTIONS);
			preferredMoreSize = new Dimension(group.secondaryButton.getPreferredSize());
		}
		if (minimizedMoreSize == null){
			PerspectiveToggleGroup group = new PerspectiveToggleGroup(null, new Dimension(), TWO_NULL_ACTIONS);
			group.addSeconderyActions(EMPTY_ACTIONS);
			CompositeMenuToggleButton secondaryButton = group.secondaryButton;
			secondaryButton.setPreferredSize(null);
			secondaryButton.setText("");
			minimizedMoreSize = secondaryButton.getPreferredSize();
			minimizedMoreSize.width += 10;
			minimizedMoreSize.height = preferredMoreSize.height;
		}
	}

	/**
	 * Returns the default {@link Dimension} for the secondary button (including label text). Should be initialized
	 * with {@link #init()} before first use.
	 *
	 * @return the default secondary button size
	 * @since 8.1
	 */
	public static Dimension getDefaultSecondaryButtonSize() {
		return preferredMoreSize;
	}

	/**
	 * Returns the minimized {@link Dimension} for the secondary button (empty label text). Should be initialized
	 * with {@link #init()} before first use.
	 *
	 * @return the minimized secondary button size
	 * @since 8.1
	 */
	public static Dimension getMinimizedSecondaryButtonSize() {
		return minimizedMoreSize;
	}
}
