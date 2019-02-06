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
package com.rapidminer.gui.tools;

import java.awt.Container;

import javax.swing.SwingUtilities;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.vlsolutions.swing.docking.AutoHideExpandPanel;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableContainer;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.RelativeDockablePosition;
import com.vlsolutions.swing.docking.TabbedDockView;
import com.vlsolutions.swing.docking.TabbedDockableContainer;


/**
 * This helper class provides some static methods and properties which might be useful for the
 * docking framework.
 *
 * @author Marcel Michel
 * @since 6.2.0
 *
 */
public class DockingTools {

	/**
	 * Tries to resolve a {@link DockableState} with the given dockKey.
	 *
	 * @param dockKey
	 *            The dock key of the desired {@link DockableState}
	 * @return If successful the found {@link DockableState}, otherwise <code>null</code>.
	 * @since 6.2.0
	 */
	public static DockableState getDockableState(String dockKey) {
		MainFrame mainFrame = RapidMinerGUI.getMainFrame();
		for (DockableState dockableState : mainFrame.getDockingDesktop().getDockables()) {
			Dockable dockable = dockableState.getDockable();
			if (dockable.getDockKey().getKey().equals(dockKey)) {
				return dockableState;
			}
		}
		return null;
	}

	/**
	 * Opens a {@link Dockable} if closed or brings it to foreground.
	 *
	 * @param dockKey
	 *            The dock key of the desired {@link Dockable}
	 * @since 6.2.0
	 */
	public static void openDockable(String dockKey) {
		openDockable(dockKey, null);
	}

	/**
	 * Opens a {@link Dockable} if closed or brings it to foreground in regard to the defined
	 * dockKeyContainer.
	 *
	 * @param dockKey
	 *            The dock key of the desired {@link Dockable}
	 * @param dockKeyContainer
	 *            The desired container of the {@link Dockable}
	 * @since 6.2.0
	 */
	public static void openDockable(String dockKey, String dockKeyContainer) {
		openDockable(dockKey, dockKeyContainer, null);
	}

	/**
	 * Opens a {@link Dockable} if closed or brings it to foreground in regard to the defined
	 * dockKeyContainer. If no container could be found the {@link Dockable} will be added in
	 * respect to the dockablePosition.
	 *
	 * @param dockKey
	 *            The dock key of the desired {@link Dockable}
	 * @param dockKeyContainer
	 *            The desired container of the {@link Dockable}, can be <code>null</code>
	 * @param dockablePosition
	 *            The desired position of the {@link Dockable} if no container could be found, can
	 *            be <code>null</code>
	 * @since 6.2.0
	 */
	public static void openDockable(String dockKey, String dockKeyContainer, RelativeDockablePosition dockablePosition) {
		if (dockKey == null) {
			throw new IllegalArgumentException("dockKey cannot be null");
		}
		DockableState dockableState = getDockableState(dockKey);
		if (dockableState == null) {
			return;
		}
		MainFrame mainFrame = RapidMinerGUI.getMainFrame();
		Dockable dockable = dockableState.getDockable();
		if (dockableState.isClosed()) {
			if (dockKeyContainer != null) {
				DockableState dockableStateContainer = getDockableState(dockKeyContainer);
				if (dockableStateContainer != null) {
					Dockable dockableContainer = dockableStateContainer.getDockable();
					// Dockable is not closed try to find the container
					TabbedDockableContainer tabbedContainer = null;
					Container container = SwingUtilities.getAncestorOfClass(DockableContainer.class,
							dockableContainer.getComponent());
					// The container might be null, if the dockable is not in a tab
					// environment (single dockable displayed)
					if (container instanceof TabbedDockView) {
						tabbedContainer = (TabbedDockableContainer) SwingUtilities
								.getAncestorOfClass(TabbedDockableContainer.class, dockableContainer.getComponent());
					} else if (container instanceof AutoHideExpandPanel) {
						// This kind of instantiation does not support the following
						// operations. Cancel at this point and just add the dockable to the
						// mainframe.
						mainFrame.getDockableMenu().getDockingContext().getDesktopList().get(0).addDockable(dockable);
						return;
					}
					// Add new tab to the container, in regard to the found tabbedContainer
					mainFrame.getDockingDesktop().createTab(dockableContainer, dockable,
							tabbedContainer != null ? tabbedContainer.getTabCount() : 0);

					// Bring new tab to front
					if (tabbedContainer != null) {
						tabbedContainer.setSelectedDockable(dockable);
					} else {
						container = SwingUtilities.getAncestorOfClass(TabbedDockableContainer.class,
								dockableContainer.getComponent());
						if (container instanceof TabbedDockableContainer) {
							((TabbedDockableContainer) container).setSelectedDockable(dockable);
						}
					}
					// All went fine, dockable successfully added
					return;
				}
			}
			// DockableContainer could not be resolved. Add dockable to a magic position.
			if (dockablePosition == null) {
				mainFrame.getDockableMenu().getDockingContext().getDesktopList().get(0).addDockable(dockable,
						RelativeDockablePosition.BOTTOM_CENTER);
			} else {
				mainFrame.getDockableMenu().getDockingContext().getDesktopList().get(0).addDockable(dockable,
						dockablePosition);
			}
		} else {
			// Dockable is not closed: show it in the active tab
			Container container = SwingUtilities.getAncestorOfClass(TabbedDockableContainer.class, dockable.getComponent());
			if (container instanceof TabbedDockableContainer) {
				((TabbedDockableContainer) container).setSelectedDockable(dockable);
			}
		}
	}
}
