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
package com.rapidminer.gui;

import java.util.logging.Level;

import javax.swing.JMenu;
import javax.swing.JToolBar;

import com.rapidminer.tools.LogService;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockingContext;


/**
 * Collection of {@link Perspective}s that can be applied, saved, created.
 *
 * @author Simon Fischer
 * @deprecated Since 7.0.0. Use {@link PerspectiveController} instead.
 */
@Deprecated
public abstract class ApplicationPerspectives {

	protected final PerspectiveController perspectiveController;

	public ApplicationPerspectives(final PerspectiveController perspectiveController) {
		this.perspectiveController = perspectiveController;
	}

	public ApplicationPerspectives(final DockingContext context) {
		perspectiveController = new PerspectiveController(context);
	}

	public void showPerspective(final Perspective perspective) {
		perspectiveController.showPerspective(perspective);
	}

	public JMenu getWorkspaceMenu() {
		LogService.getRoot().log(Level.WARNING,
				"The access of the WorkspaceMenu deprecated. Use the PerspectiveController instead.");
		return new JMenu();
	}

	public JToolBar getWorkspaceToolBar() {
		LogService.getRoot().log(Level.WARNING,
				"The access of the WorkspaceToolBar is deprecated. Use the PerspectiveController instead.");
		return new JToolBar();
	}

	/**
	 * Checks if the given string is valid as name of a new perspective.
	 *
	 * @param name
	 * @return validity
	 */
	public boolean isValidName(final String name) {
		return perspectiveController.getModel().isValidName(name);
	}

	/**
	 *
	 * @throws IllegalArgumentException
	 *             if name is already used
	 */
	public Perspective addPerspective(final String name, final boolean userDefined) {
		return perspectiveController.getModel().addPerspective(name, userDefined);
	}

	/** Saves all perspectives to the users config directory. */
	public void saveAll() {
		perspectiveController.saveAll();
	}

	/** Loads all perspectives from the users config directory. */
	public void loadAll() {
		perspectiveController.loadAll();
	}

	public Perspective getCurrentPerspective() {
		return perspectiveController.getModel().getSelectedPerspective();
	}

	/** Switches to the given perspective, storing the current one. */
	public void showPerspective(final String name) {
		perspectiveController.showPerspective(name);
	}

	/**
	 * Creates a user-defined perspectives, and possibly switches to this new perspective
	 * immediately. The new perspective will be a copy of the current one.
	 */
	public Perspective createUserPerspective(final String name, final boolean show) {
		return perspectiveController.createUserPerspective(name, show);
	}

	/** Shows the tab as a child of the given dockable in all perspectives. */
	public void showTabInAllPerspectives(final Dockable dockable, final Dockable parent) {
		perspectiveController.showTabInAllPerspectives(dockable, parent);
	}

	public void removeFromAllPerspectives(final Dockable dockable) {
		perspectiveController.removeFromAllPerspectives(dockable);
	}

	protected abstract void makePredefined();

	protected abstract void restoreDefault(String perspectiveName);

	protected Perspective getPerspective(final String name) {
		return perspectiveController.getModel().getPerspective(name);
	}

	public void addPerspectiveChangeListener(final PerspectiveChangeListener listener) {
		perspectiveController.getModel().addPerspectiveChangeListener(listener);
	}

	public boolean removePerspectiveChangeListener(final PerspectiveChangeListener listener) {
		return perspectiveController.getModel().removePerspectiveChangeListener(listener);
	}

	public void notifyChangeListener() {
		perspectiveController.getModel().notifyChangeListener();
	}

}
