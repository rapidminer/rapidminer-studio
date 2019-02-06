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
package com.rapidminer.gui.docking;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.UIManager;

import com.rapidminer.gui.LoggedAbstractAction;
import com.vlsolutions.swing.docking.DockGroup;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.DockViewAsTab;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.TabbedDockableContainer;
import com.vlsolutions.swing.docking.event.DockDragEvent;
import com.vlsolutions.swing.docking.event.DockDropEvent;
import com.vlsolutions.swing.docking.event.DockEvent;
import com.vlsolutions.swing.docking.event.DockingActionCreateTabEvent;
import com.vlsolutions.swing.tabbedpane.JTabbedPaneSmartIcon;
import com.vlsolutions.swing.tabbedpane.SmartIconJButton;


/**
 * Detached dock view, that also shows a tab in the floating dialog.
 *
 * @author Tobias Malbrecht
 */
public class DetachedDockViewAsTab extends DockViewAsTab {

	private static final long serialVersionUID = -2316449349513873264L;

	protected Action attachAction;

	protected SmartIconJButton attachSmartIcon;

	public DetachedDockViewAsTab(Dockable dockable) {
		super(dockable);
	}

	@Override
	public void resetTabIcons() {
		// configure attach button
		attachAction = new LoggedAbstractAction("Attach") {

			private static final long serialVersionUID = 390635147992456838L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				desktop.setFloating(getDockable(), false);
			}
		};
		attachSmartIcon = new SmartIconJButton(attachAction);
		attachAction.putValue(Action.SHORT_DESCRIPTION, UIManager.get("DockViewTitleBar.attachButtonText"));
		attachSmartIcon.setIcon(UIManager.getIcon("DockViewTitleBar.attach"));
		attachSmartIcon.setPressedIcon(UIManager.getIcon("DockViewTitleBar.attach.pressed"));
		attachSmartIcon.setRolloverIcon(UIManager.getIcon("DockViewTitleBar.attach.rollover"));

		ArrayList<SmartIconJButton> icons = new ArrayList<SmartIconJButton>();
		DockKey dockKey = getDockable().getDockKey();
		if (dockKey.isCloseEnabled()) {
			icons.add(closeSmartIcon);
		}
		if (dockKey.isFloatEnabled()) {
			icons.add(attachSmartIcon);
		}
		if (icons.size() > 0) {
			SmartIconJButton[] iconsArray = icons.toArray(new SmartIconJButton[0]);
			smartIcon = new JTabbedPaneSmartIcon(dockKey.getIcon(), dockKey.getName(), null, null, true, iconsArray);
			smartIcon.setIconForTabbedPane(tabHeader);
			tabHeader.addTab("", smartIcon, getDockable().getComponent(), dockKey.getTooltip());
		} else {
			tabHeader.addTab(dockKey.getName(), dockKey.getIcon(), getDockable().getComponent(), dockKey.getTooltip());
		}

	}

	@Override
	protected void scanDrop(DockEvent event, boolean drop) {
		if (getParent() instanceof TabbedDockableContainer) {
			if (drop) {
				((DockDropEvent) event).rejectDrop();
			} else {
				((DockDragEvent) event).delegateDrag();
			}
			return;
		}
		if (event.getDragSource().getDockable() == dockable) {
			if (drop) {
				((DockDropEvent) event).rejectDrop();
			} else {
				((DockDragEvent) event).rejectDrag();
			}
			return;
		}
		if (event.getDragSource().getDockableContainer() instanceof TabbedDockableContainer) {
			if (drop) {
				((DockDropEvent) event).rejectDrop();
			} else {
				((DockDragEvent) event).rejectDrag();
			}
			return;
		}
		Rectangle bounds = getBounds();
		DockGroup sourceGroup = event.getDragSource().getDockable().getDockKey().getDockGroup();
		DockGroup destinationGroup = dockable.getDockKey().getDockGroup();
		if (!DockGroup.areGroupsCompatible(destinationGroup, sourceGroup)) {
			if (drop) {
				((DockDropEvent) event).rejectDrop();
			} else {
				((DockDragEvent) event).rejectDrag();
			}
			return;
		}
		Dockable sourceDockable = event.getDragSource().getDockable();
		DockableState.Location dockableLocation = sourceDockable.getDockKey().getLocation();
		DockableState.Location viewLocation = dockable.getDockKey().getLocation();
		if (drop) {
			event.setDockingAction(new DockingActionCreateTabEvent(event.getDesktop(), sourceDockable, dockableLocation,
					viewLocation, dockable, 0));
			((DockDropEvent) event).acceptDrop(false);
			desktop.createTab(dockable, event.getDragSource().getDockable(), 0, true);
		} else {
			Rectangle2D r2d = new Rectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height);
			event.setDockingAction(new DockingActionCreateTabEvent(event.getDesktop(), sourceDockable, dockableLocation,
					viewLocation, dockable, 0));
			if (r2d.equals(lastDropShape)) {
				((DockDragEvent) event).acceptDrag(lastDropGeneralPath);
			} else {
				GeneralPath path = buildPathForTab(bounds);
				lastDropShape = r2d;
				lastDropGeneralPath = path;
				((DockDragEvent) event).acceptDrag(lastDropGeneralPath);
			}
		}
	}
}
